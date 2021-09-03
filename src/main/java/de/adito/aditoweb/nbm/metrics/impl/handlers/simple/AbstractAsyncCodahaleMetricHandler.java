package de.adito.aditoweb.nbm.metrics.impl.handlers.simple;

import com.codahale.metrics.MetricRegistry;
import de.adito.aditoweb.nbm.metrics.impl.registry.IMetricRegistryProvider;
import org.jetbrains.annotations.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * Implements all abstract basic stuff for codahale metrics
 *
 * @author w.glanzer, 13.07.2021
 */
abstract class AbstractAsyncCodahaleMetricHandler<T extends Annotation> extends AbstractAsyncMetricHandler<T>
{

  /**
   * Returns the name of the metric of an annotated method
   *
   * @param pAnnotationName Name of the annotation
   * @param pCalledMethod   Method that was annotated
   * @return Name of the metric
   */
  @NotNull
  protected String getMetricName(@Nullable String pAnnotationName, @NotNull Method pCalledMethod)
  {
    String name = pAnnotationName;

    // Empty string should not be used, but we should have implemented a fallback..
    if (name == null || name.trim().isEmpty())
      name = pCalledMethod.getDeclaringClass().getName() + "." + pCalledMethod.getName();

    return name;
  }

  @NotNull
  protected final MetricRegistry getRegistry()
  {
    return IMetricRegistryProvider.getDefault().getRegistry();
  }

}
