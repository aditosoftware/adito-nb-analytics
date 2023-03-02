package de.adito.aditoweb.nbm.metrics.impl.eventlogger;

import org.jetbrains.annotations.NotNull;
import org.openide.util.Lookup;

import java.lang.management.ThreadInfo;
import java.util.*;
import java.util.function.Supplier;

/**
 * @author w.glanzer, 23.06.2022
 */
public interface IEventLogger
{

  static IEventLogger getInstance()
  {
    return Objects.requireNonNull(Lookup.getDefault().lookup(IEventLogger.class));
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
   * @param pThreadInfo     EDT-Info
   * @param pAllThreadInfos Supplier for retrieving all threads infos (stacktrace etc.)
   */
  void captureEDTStress(@NotNull ThreadInfo pThreadInfo, @NotNull Supplier<ThreadInfo[]> pAllThreadInfos);

}
