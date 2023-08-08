package de.adito.aditoweb.nbm.metrics.impl.eventlogger.sentry;

import com.google.common.net.MediaType;
import de.adito.aditoweb.nbm.metrics.impl.InstallationID;
import de.adito.aditoweb.nbm.metrics.impl.bugreports.IBugReport;
import de.adito.aditoweb.nbm.metrics.impl.detectors.ThreadUtility;
import de.adito.aditoweb.nbm.metrics.impl.eventlogger.IEventLogger;
import de.adito.aditoweb.nbm.metrics.impl.user.IUserAgreement;
import io.reactivex.rxjava3.disposables.Disposable;
import io.sentry.*;
import io.sentry.protocol.*;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.netbeans.api.autoupdate.*;
import org.openide.modules.*;
import org.openide.util.lookup.ServiceProvider;
import org.openide.windows.OnShowing;

import java.io.*;
import java.lang.management.ThreadInfo;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Supplier;
import java.util.logging.*;
import java.util.stream.*;
import java.util.zip.*;

/**
 * @author w.glanzer, 23.06.2022
 */
@SuppressWarnings("UnstableApiUsage")
@ServiceProvider(service = IEventLogger.class)
public class SentryEventLogger implements IEventLogger
{

  private static final Logger LOGGER = Logger.getLogger(SentryEventLogger.class.getName());
  private static final String SENTRY_DSN = "http://ae97332f81694a3e81891dcce06e31a7@157.90.233.96:9000/2";

  private static final Set<String> IGNORED_EXCEPTIONS = Set.of("de.adito.aditoweb.nbm.designertunnel.connection.TunnelDisconnectionException");

  // lowercase -> if the content of a contains call is also first converted to lowerCase a case-insensitive contains call is possible
  private static final Set<String> IGNORE_EDT_IF_IN_CLASS = Set.of("org.netbeans.modules.progress.ui.RunOffEDTImpl".toLowerCase(),
                                                                   "de.adito.git.nbm.sidebar.EditorColorizer".toLowerCase());

  @SuppressWarnings({"FieldCanBeLocal", "unused"}) // only once inited
  private Disposable disposable;
  // keep a hash of the last sent EDT stacktrace hash to check if the stacktrace changed -> no change no event
  private int lastSentEdtStackTraceHash = 0;

  @Override
  public void captureRegularException(@NonNull Throwable pException)
  {
    if (!IGNORED_EXCEPTIONS.contains(pException.getClass().getName()))
      _catchException(() -> Sentry.captureEvent(_createEvent(SentryLevel.ERROR, null, pException, null)));
  }

  @Override
  public void captureThreadDeadlock(@NonNull List<ThreadInfo> pDeadLockedThreads, ThreadInfo @NonNull [] pAllThreadInfos)
  {
    _catchException(() -> Sentry.captureEvent(_createEvent(SentryLevel.ERROR, pDeadLockedThreads, null, "Deadlocked Threads"),
                                              Hint.withAttachment(new Attachment(ThreadUtility.getThreadDump(pAllThreadInfos).getBytes(StandardCharsets.UTF_8),
                                                                                 "threaddump.tdump"))));
  }

  @Override
  public void captureEDTStress(@NonNull ThreadInfo pEdtInfo, @NonNull Supplier<ThreadInfo[]> pAllThreadInfos)
  {
    boolean ignoreEDT = false;
    boolean containsAditoTrace = false;
    StackTraceElement[] edtStackTrace = pEdtInfo.getStackTrace();
    int currentStackTraceHash = Arrays.hashCode(edtStackTrace);
    if (currentStackTraceHash != lastSentEdtStackTraceHash)
    {
      for (StackTraceElement stackTraceElement : edtStackTrace)
      {
        if (IGNORE_EDT_IF_IN_CLASS.contains(stackTraceElement.getClassName().toLowerCase()))
        {
          ignoreEDT = true;
          // break here because if ignoreEDT is true, the event should not be sent regardless of the contents of the other strackTraceElements
          break;
        }
        if (stackTraceElement.getClassName().toLowerCase().contains("adito"))
        {
          containsAditoTrace = true;
          // no break here, since it is possible that one of the remaining stackTraceElements contains an ignored class
        }
      }
      // only send the event if 1) none of the ignored classes is in the stacktrace and 2) at least one stackTraceElement contains a class with adito in its full class name
      if (!ignoreEDT && containsAditoTrace)
      {
        lastSentEdtStackTraceHash = currentStackTraceHash;
        _catchException(() -> Sentry.captureEvent(_createEvent(SentryLevel.FATAL, List.of(pEdtInfo), null, "EDT Stress"),
                                                  Hint.withAttachment(getThreadDumpAttachment(ThreadUtility.getThreadDump(pAllThreadInfos.get())))));
      }
    }
  }

  @Nullable
  @Override
  public String captureBugReport(@NonNull IBugReport pReport, @Nullable File pOutputFile)
  {
    return _catchException(() -> {
      // Prepare event
      SentryEvent ev = _createEvent(SentryLevel.INFO, null, new UserFeedbackEvent(), null);
      ev.setExtras(extractExtraInformation(pReport));
      SentryId eventId = ev.getEventId();
      assert eventId != null;
      UserFeedback feedback = new UserFeedback(eventId);
      feedback.setComments(pReport.getComment());
      feedback.setEmail(pReport.getMail());
      List<Attachment> attachments = extractAttachments(pReport);

      if (pOutputFile == null)
      {
        // send to sentry
        Sentry.captureEvent(ev, Hint.withAttachments(attachments));
        Sentry.captureUserFeedback(feedback);
        return eventId.toString();
      }
      else
      {
        // output to file
        try (FileWriter writer = new FileWriter(pOutputFile, true))
        {
          String separator = "\n\n---\n\n";

          // Event itself
          ev.serialize(new JsonObjectWriter(writer, Integer.MAX_VALUE), NoOpLogger.getInstance());
          writer.write(separator);

          // Feedback object
          feedback.serialize(new JsonObjectWriter(writer, Integer.MAX_VALUE), NoOpLogger.getInstance());
          writer.write(separator);

          // Attachments
          for (Attachment attachment : attachments)
          {
            new JsonObjectWriter(writer, Integer.MAX_VALUE).beginObject()
                .name("name").value(attachment.getFilename())
                .name("bytes").value(Base64.getEncoder().encodeToString(attachment.getBytes()))
                .endObject();
            writer.write(separator);
          }

          return null;
        }
      }
    });
  }

  /**
   * Extracts all attachments from a bug report
   *
   * @param pReport Report to extract the data from
   * @return all attachments to send
   */
  @NonNull
  private List<Attachment> extractAttachments(@NonNull IBugReport pReport)
  {
    // Prepare attachments
    List<Attachment> attachments = new ArrayList<>();

    // Append ThreadDump
    Optional.ofNullable(pReport.getThreadDump())
        .map(this::getThreadDumpAttachment)
        .ifPresent(attachments::add);

    // Append Screenshots
    Optional.ofNullable(pReport.getScreenshots())
        .map(pScreenshots -> pScreenshots.stream()
            .map(pScreenshot -> new Attachment(pScreenshot.getData(), "screenshot_" + pScreenshot.getName()))
            .collect(Collectors.toList()))
        .ifPresent(attachments::addAll);

    // Append Logs
    Optional.ofNullable(pReport.getLogs())
        .map(pLogFiles -> {
          List<Attachment> result = new ArrayList<>();
          List<IBugReport.LogFile> remainingFiles = new ArrayList<>(pLogFiles);

          // Add a "standalone" messages.log, if possible - so it will be accessable more quickly
          for (Iterator<IBugReport.LogFile> iterator = remainingFiles.iterator(); iterator.hasNext(); )
          {
            IBugReport.LogFile file = iterator.next();
            if ("messages.log".equalsIgnoreCase(file.getName()))
            {
              result.add(new Attachment(file.getData(), "log_" + file.getName(), MediaType.PLAIN_TEXT_UTF_8.toString()));
              iterator.remove();
            }
          }

          // Combine all other log files into one large zip file
          if (!remainingFiles.isEmpty())
          {
            Attachment others = compress(remainingFiles.stream().collect(Collectors.toMap(pLogFile -> "log_" + pLogFile.getName(), IBugReport.LogFile::getData)));
            if (others != null)
              result.add(others);
          }

          return result;
        })
        .ifPresent(attachments::addAll);

    return attachments;
  }

  /**
   * Combines the given data into a large zip file
   *
   * @param pFiles Files to compress to a zip file
   * @return the zip file as attachment or null, if it could not be created
   */
  @Nullable
  private Attachment compress(@NonNull Map<String, byte[]> pFiles)
  {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
         ZipOutputStream zos = new ZipOutputStream(baos))
    {
      // Add everything to the ZipOutputStream
      for (Map.Entry<String, byte[]> file : pFiles.entrySet())
      {
        zos.putNextEntry(new ZipEntry(file.getKey()));
        zos.write(file.getValue());
        zos.closeEntry();
      }

      // close the zip, because we finished
      zos.close();

      // return a new attachment
      return new Attachment(baos.toByteArray(), "log_others.zip", MediaType.ZIP.toString());
    }
    catch (IOException e)
    {
      LOGGER.log(Level.WARNING, "", e);
      return null;
    }
  }

  /**
   * Extracts all extra information to send
   *
   * @param pReport Report to get the data from
   * @return the extra information to append to the sentry event
   */
  @NonNull
  private Map<String, Object> extractExtraInformation(@NonNull IBugReport pReport)
  {
    Map<String, Object> infos = new HashMap<>();

    // Append System Details
    IBugReport.OperatingSystemState systemState = pReport.getSystemState();
    if (systemState != null)
    {
      // System - infos read via MXBean
      infos.put("System", Map.of(
          "CPU Count", systemState.getSystemCPUCount(),
          "Load Average", systemState.getSystemLoadAverage()
      ));

      // Runtime - infos read via Runtime.getDefault()
      infos.put("Runtime", Map.of(
          "CPU Count", systemState.getRuntimeCPUCount(),
          "Memory", Map.of(
              "free", systemState.getFreeMemory(),
              "total", systemState.getTotalMemory(),
              "max", systemState.getMaxMemory()
          )
      ));

      // System-Properties
      infos.put("JVM Properties", systemState.getSystemProperties());
    }

    // Append Project Details
    List<IBugReport.ProjectDetails> projectDetails = pReport.getProjectDetails();
    if (projectDetails != null)
      projectDetails.forEach(pDetail -> infos.put("Open Project: " + pDetail.getName(), pDetail.getDetails()));

    return infos;
  }

  /**
   * Creates an attachment from a given thread dump
   *
   * @param pThreadDump Dump as string
   * @return the attachment
   */
  @NonNull
  private Attachment getThreadDumpAttachment(@NonNull String pThreadDump)
  {
    return new Attachment(pThreadDump.getBytes(StandardCharsets.UTF_8), "threaddump.tdump", MediaType.PLAIN_TEXT_UTF_8.toString());
  }

  /**
   * Initializes Sentry and its observables
   */
  private void _init()
  {
    disposable = IUserAgreement.getInstance().sendingAnalyticsAllowed()
        .distinctUntilChanged()
        .subscribe(pSendingAnalyticsAllowed -> {
          if (pSendingAnalyticsAllowed && !Sentry.isEnabled())
          {
            Sentry.init(pOptions -> {
              pOptions.setDsn(SENTRY_DSN);
              pOptions.setEnvironment(InstallationID.get().isUnknownVersion() ? "dev" : "production");
              pOptions.setRelease(InstallationID.get().getVersion());
              pOptions.setAttachServerName(false);
              pOptions.setCacheDirPath(Places.getCacheSubdirectory("analytics").getAbsolutePath());
              pOptions.setEnableAutoSessionTracking(true);
              pOptions.setSessionTrackingIntervalMillis(30000);
              pOptions.setTracesSampleRate(1.0);
            });
            Sentry.startSession();
            Sentry.configureScope(pScope -> {
              User user = new User();
              user.setId(InstallationID.get().getID());
              pScope.setTag("adito.version", InstallationID.get().getFullVersion());
              pScope.setUser(user);
              pScope.setTag("os", System.getProperty("os.name"));
              getInstalledPlugins().forEach(pScope::setTag);
            });
          }
          else if (!pSendingAnalyticsAllowed && Sentry.isEnabled())
          {
            Sentry.endSession();
            Sentry.close();
          }
        });
  }

  /**
   * Catches all exceptions happening inside pSupplier and logs it to console
   *
   * @param pSupplier Function to execute
   */
  @Nullable
  private <T, Ex extends Throwable> T _catchException(@NonNull ISupEx<T, Ex> pSupplier)
  {
    try
    {
      return pSupplier.get();
    }
    catch (Throwable e)
    {
      LOGGER.log(Level.WARNING, "", e);
      return null;
    }
  }

  @NonNull
  private SentryEvent _createEvent(@NonNull SentryLevel pLevel, @Nullable Collection<ThreadInfo> pThreadInfos, @Nullable Throwable pThrowable, @Nullable String pTitle)
  {
    SentryEvent event = new SentryEvent();
    event.setLevel(pLevel);
    event.setRelease(InstallationID.get().getVersion());

    // Message
    if (pTitle != null)
    {
      Message message = new Message();
      message.setMessage(pTitle);
      event.setMessage(message);
    }

    // Threads
    if (pThreadInfos != null)
      event.setThreads(pThreadInfos.stream()
                           .map(SentryThreadUtility::getSentryThread)
                           .collect(Collectors.toList()));

    // Throwable
    if (pThrowable != null)
      event.setThrowable(pThrowable);

    return event;
  }

  /**
   * @return all installed ADITO plugins with the name as key and the version as value
   */
  @NonNull
  private Map<String, String> getInstalledPlugins()
  {
    return UpdateManager.getDefault().getUpdateUnits(UpdateManager.TYPE.KIT_MODULE).stream()
        .map(UpdateUnit::getInstalled)
        .filter(Objects::nonNull)
        .filter(pElement -> pElement.getCategory() != null && pElement.getCategory().startsWith("ADITO"))
        .collect(Collectors.toMap(this::getPluginID, UpdateElement::getSpecificationVersion));
  }

  /**
   * Evaluates the ID of a plugin to be included in every sent object
   *
   * @param pElement Element to get the ID for
   * @return the ID
   */
  @NonNull
  private String getPluginID(@NonNull UpdateElement pElement)
  {
    // ID based on codeName
    String id = pElement.getCodeName();

    // skip every parent package, so only the last package will be present
    id = id.substring(id.lastIndexOf('.') + 1);

    // lowercase everything
    id = id.toLowerCase(Locale.ROOT);

    // add prefix, so we know its from a plugin
    return "plugins." + id;
  }

  @OnShowing
  public static class StartHook implements Runnable
  {
    @Override
    public void run()
    {
      IEventLogger instance = IEventLogger.getInstance();
      if (instance instanceof SentryEventLogger)
        ((SentryEventLogger) instance)._init();
    }
  }

  @OnStop
  public static class StopHook implements Runnable
  {
    @Override
    public void run()
    {
      if (Sentry.isEnabled())
        Sentry.endSession();
    }
  }

  /**
   * Exception that gets transmitted to sentry, if a userfeedback should be sent
   */
  private static class UserFeedbackEvent extends Exception
  {
    /**
     * Length of the random trace generation
     */
    private static final int RANDOM_TRACE_DEPTH = 3;

    public UserFeedbackEvent()
    {
      setStackTrace(appendRandomStackTraceElements(getStackTrace()));
    }

    /**
     * Appends random stacktrace elements to the given element array,
     * so Sentry is forced to not combining those exceptions
     *
     * @param pOriginalElements Original elements
     * @return the elements, with the appended generated ones
     */
    @NonNull
    private StackTraceElement[] appendRandomStackTraceElements(StackTraceElement @NonNull [] pOriginalElements)
    {
      List<StackTraceElement> elements = new ArrayList<>();

      // Add random stacktrace elements
      IntStream.range(0, RANDOM_TRACE_DEPTH)
          .mapToObj(pIdx -> new StackTraceElement(getClass().getName() + ".Dyn_" + new Random().nextInt(Integer.MAX_VALUE),
                                                  "init", "Dynamic_" + new Random().nextInt(Integer.MAX_VALUE) + ".java", -1))
          .forEachOrdered(elements::add);

      // Append all current stacktrace elements, so it stays human readable in sentry logging
      elements.addAll(Arrays.asList(pOriginalElements));

      return elements.toArray(new StackTraceElement[0]);
    }
  }

  /**
   * Supplier that is capable of handling exceptions
   *
   * @param <T>  type of the returning object
   * @param <Ex> exception
   */
  private interface ISupEx<T, Ex extends Throwable>
  {
    /**
     * @return the calculated object
     * @throws Ex exception, if any thrown
     */
    T get() throws Ex;
  }

}
