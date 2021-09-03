package de.adito.aditoweb.nbm.metrics.impl.collectors.defaults;

import de.adito.aditoweb.nbm.metrics.api.collectors.*;
import de.adito.aditoweb.nbm.metrics.api.types.Histogram;

/**
 * @author w.glanzer, 14.07.2021
 */
@StaticMetricCollector(name = "defaults.memory", type = EStaticMetricCollectorType.INTERVAL)
public class MemoryMetricCollector implements IStaticMetricCollector
{

  /**
   * Collects the free memory and exports it as a metric
   *
   * @return free memory in bytes
   */
  @Histogram(name = "defaults.memory.free")
  public long collectFreeMemory()
  {
    return Runtime.getRuntime().freeMemory();
  }

  /**
   * Collects the overall available memory and exports it as a metric
   *
   * @return max. available memory ("upper limit") in bytes
   */
  @Histogram(name = "defaults.memory.available")
  public long collectAvailableMemory()
  {
    return Runtime.getRuntime().maxMemory();
  }

  /**
   * Collects the overall available memory and exports it as a metric
   *
   * @return max. available memory ("upper limit") in bytes
   */
  @Histogram(name = "defaults.memory.allocated")
  public long collectAllocatedMemory()
  {
    return Runtime.getRuntime().totalMemory();
  }

}
