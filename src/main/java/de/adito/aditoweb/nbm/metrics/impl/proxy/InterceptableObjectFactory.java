package de.adito.aditoweb.nbm.metrics.impl.proxy;

import lombok.NonNull;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.implementation.attribute.MethodAttributeAppender;
import net.bytebuddy.matcher.ElementMatchers;
import org.objenesis.ObjenesisStd;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.*;
import java.util.logging.*;

/**
 * @author w.glanzer, 09.07.2021
 */
class InterceptableObjectFactory implements IInterceptableObjectFactory
{

  private static final Logger _LOGGER = Logger.getLogger(InterceptableObjectFactory.class.getName());

  @NonNull
  @Override
  public <T> T createInterceptableProxy(@NonNull T pObject, @NonNull InvocationHandler pHandler) throws IllegalAccessException
  {
    Class<?> type = new ByteBuddy()
        .subclass(pObject.getClass())
        .method(ElementMatchers.any())
        .intercept(InvocationHandlerAdapter.of(pHandler))
        .attribute(MethodAttributeAppender.ForInstrumentedMethod.INCLUDING_RECEIVER)
        .make()
        .load(pObject.getClass().getClassLoader(), ClassLoadingStrategy.UsingLookup.of(MethodHandles.privateLookupIn(pObject.getClass(), MethodHandles.lookup())))
        .getLoaded();

    //noinspection unchecked
    return _copyProperties(pObject, (T) new ObjenesisStd().getInstantiatorOf(type).newInstance());
  }

  /**
   * Copies all properties from the original objekt to the target object.
   * Only properties will be copied, that are bound to pOriginal - e.g. STATIC properties wont be copied.
   *
   * @param pOriginal    Original object
   * @param pDestination Target object
   * @return the target object after it was modified
   */
  @NonNull
  private <T> T _copyProperties(@NonNull T pOriginal, @NonNull T pDestination)
  {
    Class<?> clsType = pOriginal.getClass();
    while (clsType != null)
    {
      Field[] fields = clsType.getDeclaredFields();
      for (Field field : fields)
      {
        // ignore static fields, because they are not bound to the object itself
        if (!Modifier.isStatic(field.getModifiers()))
        {
          try
          {
            field.setAccessible(true);
            field.set(pDestination, field.get(pOriginal));
          }
          catch (Throwable e) //NOSONAR catch everything, including NoSuchMethodError
          {
            _LOGGER.log(Level.WARNING, "", e);
          }
        }
      }
      clsType = clsType.getSuperclass();
    }

    return pDestination;
  }

}
