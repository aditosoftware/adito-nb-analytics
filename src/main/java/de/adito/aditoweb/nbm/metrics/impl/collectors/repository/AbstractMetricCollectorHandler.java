package de.adito.aditoweb.nbm.metrics.impl.collectors.repository;

import com.google.common.collect.*;
import de.adito.aditoweb.nbm.metrics.api.IMetricProxyFactory;
import de.adito.aditoweb.nbm.metrics.api.collectors.*;
import de.adito.aditoweb.nbm.metrics.api.types.MetricType;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.logging.*;

/**
 * @author w.glanzer, 14.07.2021
 */
abstract class AbstractMetricCollectorHandler
{

  private static final Logger _LOGGER = Logger.getLogger(AbstractMetricCollectorHandler.class.getName());

  /**
   * Registers a new collector
   *
   * @param pCollector  Collector that should be registered
   * @param pDefinition The annotation that resultet in this registration
   */
  public final void addCollector(@NotNull IStaticMetricCollector pCollector, @NotNull StaticMetricCollector pDefinition)
  {
    IStaticMetricCollector collectorProxy = IMetricProxyFactory.proxy(pCollector);
    Multimap<Method, Annotation> methods = ArrayListMultimap.create();

    // Find methods to metrify
    for (Method method : collectorProxy.getClass().getDeclaredMethods())
      for (Annotation annotation : method.getAnnotations())
        if (annotation.annotationType().isAnnotationPresent(MetricType.class))
          methods.put(method, annotation);

    // Call implentation
    addCollector0(collectorProxy, pDefinition, methods);
  }

  /**
   * Gets called if a new collector should be registered
   *
   * @param pCollector        Collector that should be registered
   * @param pDefinition       The annotation that resultet in this registration
   * @param pMetrifiedMethods Methods that has to be be handeld inside the collector
   */
  protected abstract void addCollector0(@NotNull IStaticMetricCollector pCollector, @NotNull StaticMetricCollector pDefinition,
                                        @NotNull Multimap<Method, Annotation> pMetrifiedMethods);

  /**
   * Calls a specific method, if necessary
   */
  protected static class MethodCaller implements Runnable
  {
    private final IStaticMetricCollector collector;
    private final StaticMetricCollector definition;
    private final Method method;

    public MethodCaller(@NotNull IStaticMetricCollector pCollector, @NotNull StaticMetricCollector pDefinition, @NotNull Method pMethod)
    {
      collector = pCollector;
      definition = pDefinition;
      method = pMethod;
    }

    @Override
    public void run()
    {
      try
      {
        method.setAccessible(true);
        method.invoke(collector);
      }
      catch (Exception e)
      {
        _LOGGER.log(Level.WARNING, "Failed to invoke metric collector " + collector + " with ID " + definition.name(), e);
      }
    }
  }

}
