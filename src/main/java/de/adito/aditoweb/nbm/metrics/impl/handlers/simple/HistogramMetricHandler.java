package de.adito.aditoweb.nbm.metrics.impl.handlers.simple;

import de.adito.aditoweb.nbm.metrics.api.types.Histogram;
import de.adito.aditoweb.nbm.metrics.impl.handlers.MetricHandler;
import org.jetbrains.annotations.*;

import java.lang.reflect.Method;

/**
 * Implements a metricHandler for {@link Histogram} annotation
 *
 * @author w.glanzer, 14.07.2021
 */
@MetricHandler(metric = Histogram.class)
class HistogramMetricHandler extends AbstractAsyncCodahaleMetricHandler<Histogram>
{

  @Override
  protected void beforeMethod0(@NotNull Histogram pAnnotation, @NotNull Object pObject, @NotNull Method pMethod, @NotNull Object[] pArgs)
  {
    // not used
  }

  @Override
  protected void afterMethod0(@NotNull Histogram pAnnotation, @NotNull Object pObject, @NotNull Method pMethod, @NotNull Object[] pArgs,
                              @Nullable Object pResult)
  {
    if (pResult instanceof Number)
      getRegistry().histogram(getMetricName(pAnnotation.name(), pMethod)).update(((Number) pResult).longValue());
    else
      throw new IllegalStateException("Method " + pMethod.getName() + " in class " + pMethod.getDeclaringClass().getName() +
                                          " was annoted with @Histogram, but did not return a valid histogram datatype (" + pObject + ")");
  }

}
