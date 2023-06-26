package de.adito.aditoweb.nbm.metrics.impl.eventlogger;

import de.adito.aditoweb.nbm.metrics.impl.bugreports.IBugReport;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.openide.util.Lookup;

import java.io.File;
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
  void captureRegularException(@NonNull Throwable pException);

  /**
   * Captures a thread deadlock
   *
   * @param pDeadLockedThreads Threads that deadlocked
   * @param pAllThreadInfos    All Threads
   */
  void captureThreadDeadlock(@NonNull List<ThreadInfo> pDeadLockedThreads, @NonNull ThreadInfo[] pAllThreadInfos);

  /**
   * Captured, that the EDT is under heavy load
   *
   * @param pThreadInfo     EDT-Info
   * @param pAllThreadInfos Supplier for retrieving all threads infos (stacktrace etc.)
   */
  void captureEDTStress(@NonNull ThreadInfo pThreadInfo, @NonNull Supplier<ThreadInfo[]> pAllThreadInfos);

  /**
   * Captured, that a bug report should be submitted.
   * Will be outputted to the given file and not submitted to the logger, if not null
   *
   * @param pReport     Report that should be submitted
   * @param pOutputFile File to output the report to, if it should not be sent to the logger
   * @return the ID of the bug report that was submitted, or null if the report could not be sent
   */
  @Nullable
  String captureBugReport(@NonNull IBugReport pReport, @Nullable File pOutputFile);

}
