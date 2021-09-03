package de.adito.aditoweb.nbm.metrics.impl.proxy;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationHandler;

/**
 * Factory to provide proxies around given objects to proxy method calls.
 *
 * @author w.glanzer, 09.07.2021
 */
public interface IInterceptableObjectFactory
{

  /**
   * Creates a new proxy object to give access to intercept every methods
   *
   * @param pObject  Object that gets proxied
   * @param pHandler Handler to intercept methods
   * @return A proxy object that delegates every method call to pHandler
   */
  @NotNull
  <T> T createInterceptableProxy(@NotNull T pObject, @NotNull InvocationHandler pHandler) throws IllegalAccessException;

}
