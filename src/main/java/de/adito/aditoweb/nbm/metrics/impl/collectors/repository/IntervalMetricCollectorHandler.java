package de.adito.aditoweb.nbm.metrics.impl.collectors.repository;

import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.adito.aditoweb.nbm.metrics.api.collectors.*;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.concurrent.*;

/**
 * Handler that cares about MetricCollectors which
 * have to be called in a specific interval
 *
 * @author w.glanzer, 14.07.2021
 */
class IntervalMetricCollectorHandler extends AbstractMetricCollectorHandler
{

  private static final long _INTERVAL_RATE_MS = Long.parseLong(System.getProperty("adito.metrics.staticcollector.interval", "30000"));
  private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors(),
                                                                                     new ThreadFactoryBuilder()
                                                                                         .setDaemon(true)
                                                                                         .setNameFormat("tIntervalMetricCollector-%d")
                                                                                         .build());

  @Override
  protected void addCollector0(@NotNull IStaticMetricCollector pCollector, @NotNull StaticMetricCollector pDefinition,
                               @NotNull Multimap<Method, Annotation> pMetrifiedMethods)
  {
    // Just use the key set, because we want to trigger a method only one - even it is annotated twice
    pMetrifiedMethods.keySet().forEach(pMethod -> executor.scheduleAtFixedRate(new MethodCaller(pCollector, pDefinition, pMethod), 0,
                                                                               _INTERVAL_RATE_MS, TimeUnit.MILLISECONDS));
  }

}
