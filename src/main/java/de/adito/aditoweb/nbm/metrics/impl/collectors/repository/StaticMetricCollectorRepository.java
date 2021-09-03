package de.adito.aditoweb.nbm.metrics.impl.collectors.repository;

import de.adito.aditoweb.nbm.metrics.api.collectors.*;
import de.adito.picoservice.IPicoRegistry;
import org.jetbrains.annotations.NotNull;
import org.openide.modules.OnStart;
import org.openide.util.Pair;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.*;

/**
 * @author w.glanzer, 14.07.2021
 */
@SuppressWarnings("unused")
    // used in Initializer
class StaticMetricCollectorRepository
{

  private static final Logger _LOGGER = Logger.getLogger(StaticMetricCollectorRepository.class.getName());
  private static final StaticMetricCollectorRepository _INSTANCE = new StaticMetricCollectorRepository();
  private final AtomicBoolean started = new AtomicBoolean(false);
  private final Map<EStaticMetricCollectorType, AbstractMetricCollectorHandler> handlers = Map.of(
      EStaticMetricCollectorType.INTERVAL, new IntervalMetricCollectorHandler(),
      EStaticMetricCollectorType.START, new StartMetricCollectorHandler());

  @NotNull
  public static StaticMetricCollectorRepository getInstance()
  {
    return _INSTANCE;
  }

  private StaticMetricCollectorRepository()
  {
  }

  /**
   * Starts the repository, so that it collects metrics from metricCollectors automatically
   */
  public synchronized void start()
  {
    if (started.get())
      return;

    // register collectors
    IPicoRegistry.INSTANCE.find(IStaticMetricCollector.class, StaticMetricCollector.class).entrySet().stream()
        .map(pEntry -> {
          try
          {
            Constructor<? extends IStaticMetricCollector> constructor = pEntry.getKey().getDeclaredConstructor();
            constructor.setAccessible(true);
            return Pair.of(constructor.newInstance(), pEntry.getValue());
          }
          catch (Exception e)
          {
            _LOGGER.log(Level.WARNING, "Failed to instantiate collector " + pEntry.getKey().getName() + " for name " + pEntry.getValue().name(), e);
            return null;
          }
        })
        .filter(Objects::nonNull)
        .forEach(pPair -> {
          AbstractMetricCollectorHandler handler = handlers.get(pPair.second().type());
          if (handler == null)
            throw new IllegalStateException();
          handler.addCollector(pPair.first(), pPair.second());
        });

    started.set(true);
  }

  /**
   * Initializes the repository at startup
   */
  @OnStart
  public static class Initializer implements Runnable
  {
    @Override
    public void run()
    {
      StaticMetricCollectorRepository.getInstance().start();
    }
  }

}
