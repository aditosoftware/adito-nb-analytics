package de.adito.aditoweb.nbm.metrics.impl.proxy.dynamic;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;

/**
 * Injects dynamic metric annotations into already defined
 * objects during runtime and byte code manipulation
 *
 * @author w.glanzer, 03.03.2023
 */
public interface IDynamicMetricProxyLoader
{

  /**
   * Injects the given annotation into all instance
   * methods (which are not static and not abstract) of the given class.
   *
   * @param pClass      Class to inject the annotations to
   * @param pAnnotation Annotation to be injected
   */
  void loadDynamicProxy(@NotNull Class<?> pClass, @NotNull Annotation pAnnotation);

}
