package de.adito.aditoweb.nbm.metrics.impl.eventlogger;

import de.adito.aditoweb.nbm.metrics.impl.InstallationID;
import de.adito.aditoweb.nbm.metrics.impl.detectors.ThreadUtility;
import de.adito.aditoweb.nbm.metrics.impl.user.IUserAgreement;
import io.reactivex.rxjava3.disposables.Disposable;
import io.sentry.*;
import io.sentry.protocol.*;
import org.jetbrains.annotations.*;
import org.openide.modules.*;
import org.openide.windows.OnShowing;

import java.lang.management.ThreadInfo;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Supplier;
import java.util.logging.*;
import java.util.stream.Collectors;

/**
 * @author w.glanzer, 23.06.2022
 */
class SentryEventLogger implements IEventLogger
{

  protected static final SentryEventLogger INSTANCE = new SentryEventLogger();
  private static final String ANALYTICS_VERSION = Modules.getDefault().ownerOf(SentryEventLogger.class).getImplementationVersion();
  private static final Logger LOGGER = Logger.getLogger(SentryEventLogger.class.getName());
  private static final String SENTRY_DSN = "http://ae97332f81694a3e81891dcce06e31a7@157.90.233.96:9000/2";

  private static final Set<String> IGNORED_EXCEPTIONS = Set.of("de.adito.aditoweb.nbm.designertunnel.connection.TunnelDisconnectionException");

  private static final Set<String> IGNORE_EDT_IF_IN_CLASS = Set.of("org.netbeans.modules.progress.ui.RunOffEDTImpl",
                                                                   "de.adito.git.nbm.sidebar.EditorColorizer");

  @SuppressWarnings({"FieldCanBeLocal", "unused"}) // only once inited
  private Disposable disposable;

  @Override
  public void captureRegularException(@NotNull Throwable pException)
  {
    if (!IGNORED_EXCEPTIONS.contains(pException.getClass().getName()))
      _catchException(() -> Sentry.captureEvent(_createEvent(SentryLevel.ERROR, null, pException, null)));
  }

  @Override
  public void captureThreadDeadlock(@NotNull List<ThreadInfo> pDeadLockedThreads, @NotNull ThreadInfo[] pAllThreadInfos)
  {
    _catchException(() -> Sentry.captureEvent(_createEvent(SentryLevel.ERROR, pDeadLockedThreads, null, "Deadlocked Threads"),
                                              Hint.withAttachment(new Attachment(ThreadUtility.getThreadDump(pAllThreadInfos).getBytes(StandardCharsets.UTF_8),
                                                                                 "threaddump.tdump"))));
  }

  @Override
  public void captureEDTStress(@NotNull ThreadInfo pEdtInfo, @NotNull Supplier<ThreadInfo[]> pAllThreadInfos)
  {
    boolean ignoreEDT = false;
    boolean containsAditoTrace = false;
    for (StackTraceElement stackTraceElement : pEdtInfo.getStackTrace())
    {
      if (IGNORE_EDT_IF_IN_CLASS.contains(stackTraceElement.getClassName()))
      {
        ignoreEDT = true;
        // break here because if ignoreEDT is true, the event should not be sent regardless of the contents of the other strackTraceElements
        break;
      }
      if (stackTraceElement.getClassName().contains("adito"))
      {
        containsAditoTrace = true;
        // no break here, since it is possible that one of the remaining stackTraceElements contains an ignored class
      }
    }
    // only send the event if 1) none of the ignored classes is in the stacktrace and 2) at least one stackTraceElement contains a class with adito in its full class name
    if (!ignoreEDT && containsAditoTrace)
    {
      _catchException(() -> Sentry.captureEvent(_createEvent(SentryLevel.FATAL, List.of(pEdtInfo), null, "EDT Stress"),
                                                Hint.withAttachment(new Attachment(ThreadUtility.getThreadDump(pAllThreadInfos.get())
                                                                                       .getBytes(StandardCharsets.UTF_8),
                                                                                   "threaddump.tdump"))));
    }
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
            });
            Sentry.startSession();
            Sentry.configureScope(pScope -> {
              User user = new User();
              user.setId(InstallationID.get().getID());
              pScope.setUser(user);
              pScope.setTag("os", System.getProperty("os.name"));
              pScope.setTag("plugins.analytics", ANALYTICS_VERSION);
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
   * Catches all exceptions happening inside pRunnable and logs it to console
   *
   * @param pRunnable Runnable to execute
   */
  private void _catchException(@NotNull Runnable pRunnable)
  {
    try
    {
      pRunnable.run();
    }
    catch (Throwable e)
    {
      LOGGER.log(Level.WARNING, "", e);
    }
  }

  @NotNull
  private SentryEvent _createEvent(@NotNull SentryLevel pLevel, @Nullable Collection<ThreadInfo> pThreadInfos, @Nullable Throwable pThrowable, @Nullable String pTitle)
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

  @OnShowing
  public static class StartHook implements Runnable
  {
    @Override
    public void run()
    {
      SentryEventLogger.INSTANCE._init();
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

}
