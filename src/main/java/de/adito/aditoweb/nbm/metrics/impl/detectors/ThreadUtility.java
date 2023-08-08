package de.adito.aditoweb.nbm.metrics.impl.detectors;

import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

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
  @NonNull
  static String getThreadInfoStacktrace(@NonNull ThreadInfo pThreadInfo)
  {
    Thread.State state = getThreadState(pThreadInfo);
    if (state == null)
      state = Thread.State.TERMINATED;

    StringBuilder sb = new StringBuilder("\"" + pThreadInfo.getThreadName() + "\"" +
                                             (pThreadInfo.isDaemon() ? " daemon" : "") +
                                             " prio=" + pThreadInfo.getPriority() +
                                             " tid=0x" + String.format("%X", pThreadInfo.getThreadId()) +
                                             " nid=NA " +
                                             state.toString().toLowerCase());
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
    sb.append("  java.lang.Thread.State: ").append(state).append("\n");
    StackTraceElement[] stackTrace = pThreadInfo.getStackTrace();
    int i = 0;
    for (; i < stackTrace.length; i++)
    {
      StackTraceElement ste = stackTrace[i];
      sb.append("\tat " + ste.toString());
      sb.append('\n');
      if (i == 0 && pThreadInfo.getLockInfo() != null)
      {
        Thread.State ts = state;
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
  @NonNull
  public static String getThreadDump(@NonNull ThreadInfo[] pAllThreadInfos)
  {
    return Arrays.stream(pAllThreadInfos)
        .map(ThreadUtility::getThreadInfoStacktrace)
        .collect(Collectors.joining("\n"));
  }

  @NonNull
  public static String getDeadlockedThreadsAsString(@NonNull List<?> pDeadlockedThreads)
  {
    return "Deadlocked Threads:\n" + pDeadlockedThreads.stream()
        .filter(pObj -> pObj instanceof ThreadInfo)
        .map(ThreadInfo.class::cast)
        .map(ThreadInfo::toString)
        .map(String::valueOf)
        .collect(Collectors.joining("\n")) + "\n";
  }

  /**
   * Extracts the thread state of the given info.
   * This method is necessary, because IntelliJ should interpret TIMED_WAITING and WAITING the same way
   *
   * @param pInfo Info to read the state from
   * @return the state
   */
  @Nullable
  private static Thread.State getThreadState(@NonNull ThreadInfo pInfo)
  {
    Thread.State state = pInfo.getThreadState();
    if (state == Thread.State.TIMED_WAITING)
      state = Thread.State.WAITING;
    return state;
  }
}
