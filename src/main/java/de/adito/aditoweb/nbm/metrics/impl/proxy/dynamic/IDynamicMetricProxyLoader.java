package de.adito.aditoweb.nbm.metrics.impl.proxy.dynamic;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.jetbrains.annotations.*;

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
   * Injects the given annotation into all methods (which are not abstract) of the given class.
   * All inner classes of the given one will be touched too.
   *
   * @param pClass      Class to inject the annotations to
   * @param pAnnotation Annotation to be injected
   * @param pMatcher    additional matcher to add, null will be ignored
   */
  void loadDynamicProxy(@NotNull Class<?> pClass, @NotNull Annotation pAnnotation, @Nullable ElementMatcher<MethodDescription> pMatcher);

}
