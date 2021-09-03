package de.adito.aditoweb.nbm.metrics.impl.handlers.simple;

import de.adito.aditoweb.nbm.metrics.api.types.Gauge;
import de.adito.aditoweb.nbm.metrics.impl.handlers.MetricHandler;
import org.jetbrains.annotations.*;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implements a metricHandler for {@link Gauge} annotation
 *
 * @author w.glanzer, 14.07.2021
 */
@MetricHandler(metric = Gauge.class)
class GaugeMetricHandler extends AbstractAsyncCodahaleMetricHandler<Gauge>
{

  @Override
  protected void beforeMethod0(@NotNull Gauge pAnnotation, @NotNull Object pObject, @NotNull Method pMethod, @NotNull Object[] pArgs)
  {
    // not used
  }

  @Override
  protected void afterMethod0(@NotNull Gauge pAnnotation, @NotNull Object pObject, @NotNull Method pMethod, @NotNull Object[] pArgs,
                              @Nullable Object pResult)
  {
    ((_SimpleValueGauge) getRegistry().gauge(getMetricName(pAnnotation.name(), pMethod), _SimpleValueGauge::new)).setValue(pResult);
  }

  /**
   * Gauge-Impl, to set the value directly
   */
  private static class _SimpleValueGauge implements com.codahale.metrics.Gauge<Object>
  {
    private final AtomicReference<Object> valueRef = new AtomicReference<>();

    @Override
    public Object getValue()
    {
      return valueRef.get();
    }

    public void setValue(@Nullable Object pValue)
    {
      valueRef.set(pValue);
    }
  }

}
