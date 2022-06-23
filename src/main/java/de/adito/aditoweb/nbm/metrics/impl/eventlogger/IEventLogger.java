package de.adito.aditoweb.nbm.metrics.impl.eventlogger;

import org.jetbrains.annotations.NotNull;

import java.lang.management.ThreadInfo;
import java.util.List;

/**
 * @author w.glanzer, 23.06.2022
 */
public interface IEventLogger
{

  static IEventLogger getInstance()
  {
    return SentryEventLogger.INSTANCE;
  }

  /**
   * Captures a regular exception
   *
   * @param pException Exception that was catched
   */
  void captureRegularException(@NotNull Throwable pException);

  /**
   * Captures a thread deadlock
   *
   * @param pDeadLockedThreads Threads that deadlocked
   * @param pAllThreadInfos    All Threads
   */
  void captureThreadDeadlock(@NotNull List<ThreadInfo> pDeadLockedThreads, @NotNull ThreadInfo[] pAllThreadInfos);

  /**
   * Captured, that the EDT is under heavy load
   *
   * @param pThreadInfo EDT-Info
   */
  void captureEDTStress(@NotNull ThreadInfo pThreadInfo);

}
