package de.adito.aditoweb.nbm.metrics.impl.handlers.simple;

import de.adito.aditoweb.nbm.metrics.api.types.Counted;
import de.adito.aditoweb.nbm.metrics.impl.handlers.MetricHandler;
import org.jetbrains.annotations.*;

import java.lang.reflect.Method;

/**
 * Implements a metricHandler for {@link Counted} annotation
 *
 * @author w.glanzer, 12.07.2021
 */
@MetricHandler(metric = Counted.class)
class CountedMetricHandler extends AbstractAsyncCodahaleMetricHandler<Counted>
{

  @Override
  protected void beforeMethod0(@NotNull Counted pAnnotation, @NotNull Object pObject, @NotNull Method pMethod, @NotNull Object[] pArgs)
  {
    getRegistry().counter(getMetricName(pAnnotation.name(), pAnnotation.nameFactory(), pMethod, pArgs)).inc();
  }

  @Override
  protected void afterMethod0(@NotNull Counted pAnnotation, @NotNull Object pObject, @NotNull Method pMethod, @NotNull Object[] pArgs,
                              @Nullable Object pResult)
  {
    // not used
  }

}
