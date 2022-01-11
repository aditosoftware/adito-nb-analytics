package de.adito.aditoweb.nbm.metrics.impl.detectors;

import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.lang.management.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author m.kaspera, 20.12.2021
 */
class EDTStressDetectorTest
{

  @Test
  void isDumpThreadsIfStressAboveThreshhold()
  {
    try(MockedStatic<ManagementFactory> managementFactoryMockedStatic = mockStatic(ManagementFactory.class))
    {
      ThreadMXBean mockedThreadBean = mock(ThreadMXBean.class);
      managementFactoryMockedStatic.when(ManagementFactory::getThreadMXBean).thenReturn(mockedThreadBean);
      when(mockedThreadBean.getAllThreadIds()).thenReturn(new long[]{1L});
      ThreadInfo mockedThreadInfo = mock(ThreadInfo.class);
      when(mockedThreadInfo.getThreadId()).thenReturn(1L);
      when(mockedThreadInfo.getPriority()).thenReturn(1);
      when(mockedThreadInfo.getThreadName()).thenReturn("test");
      when(mockedThreadInfo.isDaemon()).thenReturn(false);
      when(mockedThreadInfo.getLockedMonitors()).thenReturn(new MonitorInfo[0]);
      when(mockedThreadInfo.getLockedSynchronizers()).thenReturn(new LockInfo[0]);
      when(mockedThreadInfo.getThreadState()).thenReturn(Thread.State.WAITING);
      when(mockedThreadInfo.getLockName()).thenReturn("lockname");
      when(mockedThreadInfo.getLockInfo()).thenReturn(new LockInfo("Lockclass", 1234));
      when(mockedThreadInfo.getLockOwnerId()).thenReturn(1L);
      when(mockedThreadInfo.isSuspended()).thenReturn(true);
      when(mockedThreadInfo.isInNative()).thenReturn(false);
      StackTraceElement[] stackTraceElements = {
          new StackTraceElement("test1", "line", "file.class", 1),
      };
      when(mockedThreadInfo.getStackTrace()).thenReturn(stackTraceElements);
      when(mockedThreadBean.getThreadInfo(1L)).thenReturn(mockedThreadInfo);
      when(mockedThreadBean.getThreadInfo(1L, Integer.MAX_VALUE)).thenReturn(mockedThreadInfo);
      when(mockedThreadInfo.getThreadName()).thenReturn("AWT-EventQueue");
      when(mockedThreadBean.getThreadCpuTime(1L)).thenReturn(0L).thenReturn(700_000_000L);
      when(mockedThreadBean.getThreadUserTime(1L)).thenReturn(0L).thenReturn(700_000_000L);
      when(mockedThreadBean.dumpAllThreads(true, true)).thenReturn(new ThreadInfo[0]);
      EDTStressDetector._Executor executor = new EDTStressDetector._Executor();
      executor.run();
      executor.run();
      verify(mockedThreadBean, atLeast(1)).getThreadInfo(1L, Integer.MAX_VALUE);
    }
  }

  @Test
  void isNoDumpThreadsIfStressBelowThreshhold()
  {
    try(MockedStatic<ManagementFactory> managementFactoryMockedStatic = mockStatic(ManagementFactory.class))
    {
      ThreadMXBean mockedThreadBean = mock(ThreadMXBean.class);
      managementFactoryMockedStatic.when(ManagementFactory::getThreadMXBean).thenReturn(mockedThreadBean);
      when(mockedThreadBean.getAllThreadIds()).thenReturn(new long[]{1L});
      ThreadInfo mockedThreadInfo = mock(ThreadInfo.class);
      when(mockedThreadBean.getThreadInfo(1L)).thenReturn(mockedThreadInfo);
      when(mockedThreadInfo.getThreadName()).thenReturn("AWT-EventQueue");
      when(mockedThreadBean.getThreadCpuTime(1L)).thenReturn(0L).thenReturn(5_000_000L);
      when(mockedThreadBean.getThreadUserTime(1L)).thenReturn(0L).thenReturn(700_000L);
      when(mockedThreadBean.dumpAllThreads(true, true)).thenReturn(new ThreadInfo[0]);
      EDTStressDetector._Executor executor = new EDTStressDetector._Executor();
      executor.run();
      executor.run();
      verify(mockedThreadBean, never()).dumpAllThreads(true, true);
    }
  }
}