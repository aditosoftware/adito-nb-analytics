package de.adito.aditoweb.nbm.metrics.impl.detectors;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.metrics.IRunnableDetector;
import io.reactivex.rxjava3.disposables.Disposable;

import java.lang.management.ManagementFactory;
import java.util.concurrent.*;
import java.util.logging.*;

/**
 * @author m.kaspera, 05.01.2022
 */
public abstract class ARunnableDetector implements IRunnableDetector, Disposable
{

  ScheduledFuture<?> scheduledFuture;

  @Override
  public void start()
  {
    if (scheduledFuture == null || scheduledFuture.isCancelled())
    {
      ManagementFactory.getThreadMXBean().setThreadCpuTimeEnabled(true);
      scheduledFuture = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder()
          .setNameFormat(getThreadNameFormat())
          .setDaemon(true)
          .build())
          .scheduleAtFixedRate(getRunner(), 0, getTimeInterval(), TimeUnit.SECONDS);
    }
    else
    {
      getLogger().log(Level.WARNING, "Detector is already running");
    }
  }

  @Override
  public void dispose()
  {
    scheduledFuture.cancel(true);
  }

  @Override
  public boolean isDisposed()
  {
    return scheduledFuture.isCancelled();
  }

  /**
   *
   * @return NameFormat for the ThreadFactoryBuilder
   */
  abstract String getThreadNameFormat();

  /**
   * Determines in which time interval the runner is started
   *
   * @return interval in seconds
   */
  abstract long getTimeInterval();

  /**
   *
   * @return The runner that is started every getTimeInterval() seconds
   */
  abstract Runnable getRunner();

  /**
   *
   * @return Logger used for logging
   */
  abstract Logger getLogger();
}
