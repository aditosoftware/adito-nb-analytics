package de.adito.aditoweb.nbm.metrics.impl.detectors;

import java.util.Objects;

/**
 * Key combining the execution time of a thread with its thread id
 *
 * @author m.kaspera, 17.12.2021
 */
class StacktraceKey
{
  private final long cpuTime;
  private final long threadId;

  public StacktraceKey(long pCpuTime, long pThreadId)
  {
    cpuTime = pCpuTime;
    threadId = pThreadId;
  }

  @Override
  public boolean equals(Object pO)
  {
    if (this == pO) return true;
    if (pO == null || getClass() != pO.getClass()) return false;
    StacktraceKey that = (StacktraceKey) pO;
    return cpuTime == that.cpuTime && threadId == that.threadId;
  }

  @Override
  public int hashCode()
  {
    return Objects.hash(cpuTime, threadId);
  }
}
