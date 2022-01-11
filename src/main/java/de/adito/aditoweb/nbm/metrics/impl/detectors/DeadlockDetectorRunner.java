package de.adito.aditoweb.nbm.metrics.impl.detectors;

import com.google.common.cache.*;
import de.adito.aditoweb.nbm.metrics.api.IMetricProxyFactory;
import de.adito.aditoweb.nbm.metrics.api.types.Sampled;
import org.jetbrains.annotations.*;

import java.lang.management.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

/**
 * @author m.kaspera, 17.12.2021
 */
class DeadlockDetectorRunner implements Runnable
{

  private final Map<Long, Long> lastCPUTimeMap = new HashMap<>();
  private final Cache<StacktraceKey, StackTraceElement[]> stackTraceCache = CacheBuilder.newBuilder().build();
  private final Set<Long> systemThreads = new HashSet<>();
  private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
  private DeadlockDetectorRunner metricProxy;

  @Override
  public void run()
  {
    if (metricProxy == null)
      metricProxy = IMetricProxyFactory.proxy(this);
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
        ThreadInfo[] threadInfos = getThreaddump();
        metricProxy.logDeadLock(deadlockedThreads, threadInfos);
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
  void checkForDeadlock(@NotNull List<ThreadInfo> pDeadlockedThreads, long pCurrentThreadId) throws ExecutionException
  {
    Long lastCPUTime = lastCPUTimeMap.getOrDefault(pCurrentThreadId, -1L);
    long currentThreadTime = threadMXBean.getThreadCpuTime(pCurrentThreadId);
    lastCPUTimeMap.put(pCurrentThreadId, currentThreadTime);
    if (currentThreadTime == lastCPUTime)
    {
      ThreadInfo currentThreadInfo = threadMXBean.getThreadInfo(pCurrentThreadId, Integer.MAX_VALUE);
      if (isDeadlockedThread(currentThreadInfo, pCurrentThreadId, currentThreadTime))
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
  boolean isDeadlockedThread(@NotNull ThreadInfo pCurrentThreadInfo, long pCurrentThreadId, long pCurrentThreadTime) throws ExecutionException
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

  ThreadInfo[] getThreaddump()
  {
    return threadMXBean.dumpAllThreads(true, true);
  }

  @Sampled(name = "DeadlockDetector", argumentConverter = ArgumentConverter.class)
  public void logDeadLock(@NotNull List<ThreadInfo> pDeadLockedThreads, @NotNull ThreadInfo[] pAllThreadInfos)
  {
    DeadLockDetector.LOGGER.log(Level.SEVERE, "Deadlock detected:\n" + ThreadUtility.getDeadlockedThreadsAsString(pDeadLockedThreads) + "\nThreaddump:\n"
        + ThreadUtility.getThreadDump(pAllThreadInfos));
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

  private static class ArgumentConverter implements Sampled.IArgumentConverter
  {

    @Nullable
    @Override
    public String toString(@Nullable Object pArgumentValue)
    {
      if (pArgumentValue instanceof ThreadInfo[])
      {
        return ThreadUtility.getThreadDump((ThreadInfo[]) pArgumentValue);
      }
      else if (pArgumentValue instanceof List)
      {
        return ThreadUtility.getDeadlockedThreadsAsString((List<?>) pArgumentValue);
      }
      return null;
    }
  }
}
