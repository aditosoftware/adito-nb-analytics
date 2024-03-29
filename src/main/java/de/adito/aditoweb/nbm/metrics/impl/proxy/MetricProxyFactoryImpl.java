package de.adito.aditoweb.nbm.metrics.impl.proxy;

import de.adito.aditoweb.nbm.metrics.api.IMetricProxyFactory;
import de.adito.aditoweb.nbm.metrics.impl.handlers.IMetricHandler;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.openide.util.lookup.ServiceProvider;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.*;

/**
 * @author w.glanzer, 09.07.2021
 */
@ServiceProvider(service = IMetricProxyFactory.class)
public class MetricProxyFactoryImpl implements IMetricProxyFactory
{

  private static final Logger _LOGGER = Logger.getLogger(MetricProxyFactoryImpl.class.getName());
  private final IInterceptableObjectFactory objectFactory = new InterceptableObjectFactory();

  @Override
  public boolean canCreateProxy(@NonNull Object pObject)
  {
    // proxy everything, if enabled
    return true;
  }

  @NonNull
  @Override
  public <T> T createProxy(@NonNull T pObject)
  {
    try
    {
      return objectFactory.createInterceptableProxy(pObject, new _MetricInvocationHandler<>(pObject));
    }
    catch (Throwable e) //NOSONAR catch everything, including NoSuchMethodError
    {
      _LOGGER.log(Level.WARNING, "Failed to create a metric proxy for object " + pObject, e);

      // return the original value, if an error happens..
      return pObject;
    }
  }

  /**
   * Invocation-Handler that delegates all method calls to the appropriate handlers
   * and invokes the original method afterwards
   */
  private static class _MetricInvocationHandler<T> implements InvocationHandler
  {
    private final IMetricHandler.Accessor metricHandlerAccessor = new IMetricHandler.Accessor();
    private final T object;

    public _MetricInvocationHandler(@NonNull T pObject)
    {
      object = pObject;
    }

    @Override
    public Object invoke(Object pProxy, Method pMethod, @Nullable Object[] pArgs) throws Throwable
    {
      Map<String, Object> hints = new HashMap<>();
      AtomicReference<Object> methodResult = new AtomicReference<>();
      AtomicReference<Throwable> methodException = new AtomicReference<>();
      Object[] arguments = pArgs == null ? new Object[0] : pArgs;

      try
      {
        // trigger handlers
        metricHandlerAccessor.beforeMethodCall(pProxy, pMethod, pMethod, arguments, hints);

        // call method itself and track exception, if any occured
        pMethod.setAccessible(true);
        methodResult.set(pMethod.invoke(object, pArgs));
        return methodResult.get();
      }
      catch (Throwable t)
      {
        methodException.set(t);
        throw t;
      }
      finally
      {
        // trigger handlers
        metricHandlerAccessor.afterMethodCall(pProxy, pMethod, pMethod, arguments, hints, methodResult.get(), methodException.get());
      }
    }
  }
}
