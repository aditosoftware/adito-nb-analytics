package de.adito.aditoweb.nbm.metrics.impl.detectors;

import com.google.common.cache.*;
import de.adito.aditoweb.nbm.metrics.impl.eventlogger.IEventLogger;
import org.jetbrains.annotations.NotNull;

import java.lang.management.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * @author m.kaspera, 17.12.2021
 */
class DeadlockDetectorRunner implements Runnable
{

  private final Map<Long, Long> lastCPUTimeMap = new HashMap<>();
  private final Cache<StacktraceKey, StackTraceElement[]> stackTraceCache = CacheBuilder.newBuilder().build();
  private final Set<Long> systemThreads = new HashSet<>();
  private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
  private Set<String> lastDeadlockedThreads = Set.of();

  @Override
  public void run()
  {
    try
    {
      long runnerThreadId = Thread.currentThread().getId();
      List<ThreadInfo> deadlockedThreads = new ArrayList<>();
      long[] liveThreadIds = threadMXBean.getAllThreadIds();
      for (long currentThreadId : liveThreadIds)
      {
        // threads declared as "system threads" are excluded from the detection, and we also do not want to check our own thread
        if (!systemThreads.contains(currentThreadId) && runnerThreadId != currentThreadId)
        {
          checkForDeadlock(deadlockedThreads, currentThreadId);
        }
      }
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

  /**
   * @param pDeadlockedThreads list of deadlocked threads, the current thread should be added to this list if it is deemed threadlocked
   * @param pCurrentThreadId   thread id to check for a potential deadlock
   * @throws ExecutionException if an exception is thrown when loading an entry of the stackTraceCache
   */
  private void checkForDeadlock(@NotNull List<ThreadInfo> pDeadlockedThreads, long pCurrentThreadId) throws ExecutionException
  {
    Long lastCPUTime = lastCPUTimeMap.getOrDefault(pCurrentThreadId, -1L);
    long currentThreadTime = threadMXBean.getThreadCpuTime(pCurrentThreadId);
    lastCPUTimeMap.put(pCurrentThreadId, currentThreadTime);
    if (currentThreadTime == lastCPUTime)
    {
      ThreadInfo currentThreadInfo = threadMXBean.getThreadInfo(pCurrentThreadId, Integer.MAX_VALUE);
      if (currentThreadInfo != null && isDeadlockedThread(currentThreadInfo, pCurrentThreadId, currentThreadTime))
      {
        pDeadlockedThreads.add(currentThreadInfo);
      }
    }
    else
    {
      stackTraceCache.invalidate(new StacktraceKey(lastCPUTime, pCurrentThreadId));
    }
  }

  /**
   * @param pCurrentThreadInfo ThreadInfo about the thread to check
   * @param pCurrentThreadId   id of the thread to check
   * @param pCurrentThreadTime current execution time of the thread to check
   * @return true if the thread should be considered deadlocked, false otherwise
   * @throws ExecutionException if an exception is thrown when loading an entry of the stackTraceCache
   */
  private boolean isDeadlockedThread(@NotNull ThreadInfo pCurrentThreadInfo, long pCurrentThreadId, long pCurrentThreadTime) throws ExecutionException
  {
    if (!DeadLockDetector.SYSTEM_THREADS.contains(pCurrentThreadInfo.getThreadName()))
    {
      StackTraceElement[] stackTraceElements = stackTraceCache.get(new StacktraceKey(pCurrentThreadTime, pCurrentThreadId),
                                                                   pCurrentThreadInfo::getStackTrace);
      for (StackTraceElement stackTraceElement : stackTraceElements)
      {
        if (isMonitoredClass(stackTraceElement.getClassName()))
        {
          return true;
        }
      }
    }
    else
    {
      // the thread is listed as a system thread, add its id to the list of systemThreads so we do not have to load its threadInfo in the future
      systemThreads.add(pCurrentThreadId);
    }
    return false;
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

  private boolean isMonitoredClass(@NotNull String pClassName)
  {
    for (String monitoredClassInfix : DeadLockDetector.MONITORED_CLASS_INFIXES)
    {
      if (pClassName.contains(monitoredClassInfix))
        return true;
    }
    return false;
  }
}
