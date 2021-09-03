package de.adito.aditoweb.nbm.metrics.impl.registry;

import com.codahale.metrics.MetricRegistry;
import org.jetbrains.annotations.NotNull;

/**
 * Provides the metric registry (in a static way)
 *
 * @author w.glanzer, 16.07.2021
 */
public interface IMetricRegistryProvider
{

  /**
   * Returns the global metric registry provider
   *
   * @return global registry provider
   */
  @NotNull
  static IMetricRegistryProvider getDefault()
  {
    return MetricRegistryProviderImpl.getInstance();
  }

  /**
   * Returns the specific instance of the metricregistry
   *
   * @return the registry
   */
  @NotNull
  MetricRegistry getRegistry();

}
