package de.adito.aditoweb.nbm.metrics.impl.detectors;

import org.junit.jupiter.api.Test;

import java.lang.management.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author m.kaspera, 20.12.2021
 */
class ThreadUtilityTest
{

  @Test
  void name()
  {
    ThreadInfo threadInfo = mock(ThreadInfo.class);
    when(threadInfo.getThreadId()).thenReturn(1L);
    when(threadInfo.getPriority()).thenReturn(1);
    when(threadInfo.getThreadName()).thenReturn("test");
    when(threadInfo.isDaemon()).thenReturn(false);
    when(threadInfo.getLockedMonitors()).thenReturn(new MonitorInfo[0]);
    when(threadInfo.getLockedSynchronizers()).thenReturn(new LockInfo[0]);
    when(threadInfo.getThreadState()).thenReturn(Thread.State.WAITING);
    when(threadInfo.getLockName()).thenReturn("lockname");
    when(threadInfo.getLockInfo()).thenReturn(new LockInfo("Lockclass", 1234));
    when(threadInfo.getLockOwnerId()).thenReturn(1L);
    when(threadInfo.isSuspended()).thenReturn(true);
    when(threadInfo.isInNative()).thenReturn(false);
    StackTraceElement[] stackTraceElements = {
        new StackTraceElement("test1", "line", "file.class", 1),
        new StackTraceElement("test2", "method", "file.class", 1),
        new StackTraceElement("test3", "comment", "file.class", 1),
        new StackTraceElement("test4", "test", "file.class", 1),
        new StackTraceElement("test5", "line", "file.class", 1),
        new StackTraceElement("test6", "file", "file.class", 1),
        new StackTraceElement("test7", "static", "file.class", 1),
        new StackTraceElement("test8", "final", "file.class", 1),
        new StackTraceElement("test9", "class", "file.class", 1),
        new StackTraceElement("test10", "number", "file.class", 1),
        new StackTraceElement("test11", "letter", "file.class", 1),
        new StackTraceElement("test12", "string", "file.class", 1)
        };
    when(threadInfo.getStackTrace()).thenReturn(stackTraceElements);
    assertEquals("\"test\" prio=1 Id=1 WAITING on lockname (suspended)\n" +
                     "\tat test1.line(file.class:1)\n" +
                     "\t-  waiting on Lockclass@4d2\n" +
                     "\tat test2.method(file.class:1)\n" +
                     "\tat test3.comment(file.class:1)\n" +
                     "\tat test4.test(file.class:1)\n" +
                     "\tat test5.line(file.class:1)\n" +
                     "\tat test6.file(file.class:1)\n" +
                     "\tat test7.static(file.class:1)\n" +
                     "\tat test8.final(file.class:1)\n" +
                     "\tat test9.class(file.class:1)\n" +
                     "\tat test10.number(file.class:1)\n" +
                     "\tat test11.letter(file.class:1)\n" +
                     "\tat test12.string(file.class:1)\n\n", ThreadUtility.getThreadDump(new ThreadInfo[] {threadInfo}));
  }
}