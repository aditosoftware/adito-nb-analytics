package de.adito.aditoweb.nbm.metrics.impl.detectors;

import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import javax.management.openmbean.*;
import java.lang.management.ThreadInfo;
import java.util.Collection;
import java.util.concurrent.*;

import static org.mockito.Mockito.*;

/**
 * @author m.kaspera, 16.12.2021
 */
class DeadLockDetectorRunnerTest
{
  @Test
  void isDeadlockDetected()
  {
    final Object lock1 = new Object();
    final Object lock2 = new Object();
    Thread thread1 = new Thread(() -> {
      synchronized (lock1)
      {
        try
        {
          Thread.sleep(500);
        }
        catch (InterruptedException pE)
        {
          Assertions.fail(pE);
        }
        synchronized (lock2)
        {
          System.out.println("thread 1 has lock 2");
        }
      }
    });
    Thread thread2 = new Thread(() -> {
      synchronized (lock2)
      {
        try
        {
          Thread.sleep(500);
        }
        catch (InterruptedException pE)
        {
          Assertions.fail(pE);
        }
        synchronized (lock1)
        {
          System.out.println("thread 2 has lock 1");
        }
      }
    });
    thread1.start();
    thread2.start();
    DeadlockDetectorRunner deadLockDetector = new DeadlockDetectorRunner();
    DeadlockDetectorRunner spy = Mockito.spy(deadLockDetector);
    spy.run();
    try
    {
      Thread.sleep(1000L);
    }
    catch (InterruptedException pE)
    {
      pE.printStackTrace();
    }
    spy.run();
    verify(spy, Mockito.times(1)).getThreaddump();
    thread1.interrupt();
    thread2.interrupt();
  }

  @Test
  void isDeadlockDetectedCompletableFuturePossibleDetection()
  {
    final Object lock1 = new Object();
    CompletableFuture<Object> completableFuture = new CompletableFuture<>();
    Thread thread1 = new Thread(() -> {
      synchronized (lock1)
      {
        try
        {
          completableFuture.get();
        }
        catch (InterruptedException | ExecutionException pE)
        {
          Assertions.fail(pE);
        }
      }
    });
    Thread thread2 = new Thread(() -> {
        try
        {
          Thread.sleep(500);
        }
        catch (InterruptedException pE)
        {
          Assertions.fail(pE);
        }
        synchronized (lock1)
        {
          System.out.println("thread 2 has lock 1");
        }
        completableFuture.complete(new Object());
    });
    thread1.start();
    thread2.start();
    DeadlockDetectorRunner deadLockDetector = new DeadlockDetectorRunner();
    DeadlockDetectorRunner spy = Mockito.spy(deadLockDetector);
    spy.run();
    try
    {
      Thread.sleep(1000L);
    }
    catch (InterruptedException pE)
    {
      pE.printStackTrace();
    }
    spy.run();
    verify(spy, Mockito.times(1)).getThreaddump();
    thread1.interrupt();
    thread2.interrupt();
  }

  @Test
  void isNoDeadlockIgnored()
  {
    DeadlockDetectorRunner deadLockDetector = new DeadlockDetectorRunner();
    DeadlockDetectorRunner spy = Mockito.spy(deadLockDetector);
    spy.run();
    try
    {
      Thread.sleep(1000L);
    }
    catch (InterruptedException pE)
    {
      pE.printStackTrace();
    }
    spy.run();
    verify(spy, Mockito.times(0)).getThreaddump();
  }
}