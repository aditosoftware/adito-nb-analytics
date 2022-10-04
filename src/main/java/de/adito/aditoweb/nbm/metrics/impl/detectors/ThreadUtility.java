package de.adito.aditoweb.nbm.metrics.impl.detectors;

import org.jetbrains.annotations.NotNull;

import java.lang.management.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author m.kaspera, 20.12.2021
 */
public class ThreadUtility
{
  /**
   * This is the toString method of the ThreadInfo class as it is in jkd13, with the limit to the printed stacktrace size of 8 elements removed
   *
   * @param pThreadInfo ThreadInfo for which to get a string representation with a full stackTrace
   * @return String represenation of the ThreadInfo including a full strackTrace
   */
  @SuppressWarnings({"StringConcatenationInsideStringBufferAppend", "DuplicateBranchesInSwitch"}) // Method copied from ThreadInfo,
  // keep intact as much as possible
  @NotNull
  static String getThreadInfoStacktrace(@NotNull ThreadInfo pThreadInfo)
  {
    StringBuilder sb = new StringBuilder("\"" + pThreadInfo.getThreadName() + "\"" +
                                             (pThreadInfo.isDaemon() ? " daemon" : "") +
                                             " prio=" + pThreadInfo.getPriority() +
                                             " tid=0x" + String.format("%X", pThreadInfo.getThreadId()) +
                                             " nid=NA " +
                                             pThreadInfo.getThreadState().toString().toLowerCase());
    if (pThreadInfo.getLockName() != null)
    {
      sb.append(" on " + pThreadInfo.getLockName());
    }
    if (pThreadInfo.getLockOwnerName() != null)
    {
      sb.append(" owned by \"" + pThreadInfo.getLockOwnerName() +
                    "\" Id=" + pThreadInfo.getLockOwnerId());
    }
    if (pThreadInfo.isSuspended())
    {
      sb.append(" (suspended)");
    }
    if (pThreadInfo.isInNative())
    {
      sb.append(" (in native)");
    }
    sb.append('\n');
    sb.append("  java.lang.Thread.State: ").append(pThreadInfo.getThreadState()).append("\n");
    StackTraceElement[] stackTrace = pThreadInfo.getStackTrace();
    int i = 0;
    for (; i < stackTrace.length; i++)
    {
      StackTraceElement ste = stackTrace[i];
      sb.append("\tat " + ste.toString());
      sb.append('\n');
      if (i == 0 && pThreadInfo.getLockInfo() != null)
      {
        Thread.State ts = pThreadInfo.getThreadState();
        switch (ts)
        {
          case BLOCKED:
            sb.append("\t-  blocked on " + pThreadInfo.getLockInfo());
            sb.append('\n');
            break;
          case WAITING:
            sb.append("\t-  waiting on " + pThreadInfo.getLockInfo());
            sb.append('\n');
            break;
          case TIMED_WAITING:
            sb.append("\t-  waiting on " + pThreadInfo.getLockInfo());
            sb.append('\n');
            break;
          default:
        }
      }

      for (MonitorInfo mi : pThreadInfo.getLockedMonitors())
      {
        if (mi.getLockedStackDepth() == i)
        {
          sb.append("\t-  locked " + mi);
          sb.append('\n');
        }
      }
    }

    LockInfo[] locks = pThreadInfo.getLockedSynchronizers();
    if (locks.length > 0)
    {
      sb.append("\n\tNumber of locked synchronizers = " + locks.length);
      sb.append('\n');
      for (LockInfo li : locks)
      {
        sb.append("\t- " + li);
        sb.append('\n');
      }
    }
    return sb.toString();
  }

  /**
   * Formats the ThreadInfos in the way a threadDump normally looks
   *
   * @param pAllThreadInfos ThreadInfos, their depth for the stacktraceElements should be Integer.MAX_VALUE
   * @return ThreadDump as String, formatted similar to a Threaddump that would be acquired via VisualVM
   */
  @NotNull
  public static String getThreadDump(@NotNull ThreadInfo[] pAllThreadInfos)
  {
    return Arrays.stream(pAllThreadInfos)
        .map(ThreadUtility::getThreadInfoStacktrace)
        .collect(Collectors.joining("\n"));
  }

  @NotNull
  public static String getDeadlockedThreadsAsString(@NotNull List<?> pDeadlockedThreads)
  {
    return "Deadlocked Threads:\n" + pDeadlockedThreads.stream()
        .filter(pObj -> pObj instanceof ThreadInfo)
        .map(ThreadInfo.class::cast)
        .map(ThreadInfo::toString)
        .map(String::valueOf)
        .collect(Collectors.joining("\n")) + "\n";
  }
}
