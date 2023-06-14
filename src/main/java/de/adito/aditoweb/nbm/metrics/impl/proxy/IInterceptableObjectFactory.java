package de.adito.aditoweb.nbm.metrics.impl.proxy;

import lombok.NonNull;

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
  @NonNull
  <T> T createInterceptableProxy(@NonNull T pObject, @NonNull InvocationHandler pHandler) throws IllegalAccessException;

}
