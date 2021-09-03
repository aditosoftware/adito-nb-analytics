package de.adito.aditoweb.nbm.metrics.impl.collectors.defaults;

import de.adito.aditoweb.nbm.metrics.api.collectors.*;
import de.adito.aditoweb.nbm.metrics.api.types.Gauge;

/**
 * @author w.glanzer, 14.07.2021
 */
@StaticMetricCollector(name = "defaults.vm", type = EStaticMetricCollectorType.START)
public class StaticVMMetricCollector implements IStaticMetricCollector
{

  /**
   * Collects the available processors and exports it as metric
   *
   * @return the available cpu count
   */
  @Gauge(name = "defaults.vm.cpu.count")
  public int collectCPUCount()
  {
    return Runtime.getRuntime().availableProcessors();
  }

  /**
   * Collectors the OS type and exports it as metric
   *
   * @return the os type (Linux, Windows, Mac)
   */
  @Gauge(name = "defaults.vm.os")
  public String collectOSType()
  {
    return System.getProperty("os.name");
  }

}
