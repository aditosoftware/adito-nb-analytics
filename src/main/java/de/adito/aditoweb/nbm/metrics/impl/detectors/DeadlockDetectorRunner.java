package de.adito.aditoweb.nbm.metrics.impl.detectors;

import de.adito.aditoweb.nbm.metrics.impl.eventlogger.IEventLogger;
import org.jetbrains.annotations.NotNull;

import java.lang.management.*;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * @author m.kaspera, 17.12.2021
 */
class DeadlockDetectorRunner implements Runnable
{

  private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
  private Set<String> lastDeadlockedThreads = Set.of();

  @Override
  public void run()
  {
    try
    {
      List<ThreadInfo> deadlockedThreads;
      deadlockedThreads = Arrays.stream(Optional.ofNullable(threadMXBean.findDeadlockedThreads()).orElse(new long[0]))
          .mapToObj(threadMXBean::getThreadInfo)
          .collect(Collectors.toList());
      // if the list of deadlockedThreads contains entries deadlock(s) were detected -> go and log them in combination with a complete threaddump
      if (!deadlockedThreads.isEmpty())
      {
        logDeadLock(deadlockedThreads, threadMXBean.dumpAllThreads(true, true));
      }
    }
    // catch all exceptions since the deadlock detection thread should continue operating, except if it is cancelled
    catch (Throwable pE)
    {
      DeadLockDetector.LOGGER.log(Level.WARNING, "", pE);
    }
  }

  void logDeadLock(@NotNull List<ThreadInfo> pDeadLockedThreads, @NotNull ThreadInfo[] pAllThreadInfos)
  {
    Set<String> currentDeadlockedThreadNames = pDeadLockedThreads.stream()
        .map(ThreadInfo::getThreadName)
        .collect(Collectors.toSet());

    try
    {
      if (!Objects.equals(lastDeadlockedThreads, currentDeadlockedThreadNames))
        IEventLogger.getInstance().captureThreadDeadlock(pDeadLockedThreads, pAllThreadInfos);
    }
    finally
    {
      lastDeadlockedThreads = currentDeadlockedThreadNames;
    }
  }
}
