package de.adito.aditoweb.nbm.metrics.impl.collectors.repository;

import com.google.common.collect.Multimap;
import de.adito.aditoweb.nbm.metrics.api.collectors.*;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * Cares about calling collectors at startup
 *
 * @author w.glanzer, 14.07.2021
 */
class StartMetricCollectorHandler extends AbstractMetricCollectorHandler
{

  @Override
  protected void addCollector0(@NotNull IStaticMetricCollector pCollector, @NotNull StaticMetricCollector pDefinition,
                               @NotNull Multimap<Method, Annotation> pMetrifiedMethods)
  {
    // Just use the key set, because we want to trigger a method only one - even it is annotated twice
    // All methods have to be called instantly!
    pMetrifiedMethods.keySet().forEach(pMethod -> new MethodCaller(pCollector, pDefinition, pMethod).run());
  }

}
