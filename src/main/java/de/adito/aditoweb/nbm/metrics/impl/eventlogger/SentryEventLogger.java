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
import java.util.logging.*;
import java.util.stream.Collectors;

/**
 * @author w.glanzer, 23.06.2022
 */
class SentryEventLogger implements IEventLogger
{

  protected static final SentryEventLogger INSTANCE = new SentryEventLogger();
  private static final Logger LOGGER = Logger.getLogger(SentryEventLogger.class.getName());
  private static final String SENTRY_DSN = "http://ae97332f81694a3e81891dcce06e31a7@157.90.233.96:9000/2";

  @SuppressWarnings({"FieldCanBeLocal", "unused"}) // only once inited
  private Disposable disposable;

  @Override
  public void captureRegularException(@NotNull Throwable pException)
  {
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
  public void captureEDTStress(@NotNull ThreadInfo pThreadInfo, @NotNull ThreadInfo[] pAllThreadInfos)
  {
    _catchException(() -> Sentry.captureEvent(_createEvent(SentryLevel.FATAL, List.of(pThreadInfo), null, "EDT Stress"),
                                              Hint.withAttachment(new Attachment(ThreadUtility.getThreadDump(pAllThreadInfos).getBytes(StandardCharsets.UTF_8),
                                                                                 "threaddump.tdump"))));
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

    // Custom Tags
    event.setTag("os", System.getProperty("os.name"));

    // User
    User user = new User();
    user.setId(InstallationID.get().getID());
    event.setUser(user);

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
