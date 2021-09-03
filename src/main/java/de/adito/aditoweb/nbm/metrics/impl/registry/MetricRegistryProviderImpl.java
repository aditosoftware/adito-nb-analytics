package de.adito.aditoweb.nbm.metrics.impl.registry;

import com.codahale.metrics.MetricRegistry;
import org.jetbrains.annotations.NotNull;

/**
 * Singleton Metric-Registry Impl
 *
 * @author w.glanzer, 16.07.2021
 */
class MetricRegistryProviderImpl implements IMetricRegistryProvider
{

  private static final MetricRegistryProviderImpl _INSTANCE = new MetricRegistryProviderImpl();
  private final MetricRegistry registry = new MetricRegistry();

  @NotNull
  public static MetricRegistryProviderImpl getInstance()
  {
    return _INSTANCE;
  }

  @NotNull
  @Override
  public MetricRegistry getRegistry()
  {
    return registry;
  }

}
