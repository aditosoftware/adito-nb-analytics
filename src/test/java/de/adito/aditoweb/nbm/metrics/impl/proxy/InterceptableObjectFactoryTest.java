package de.adito.aditoweb.nbm.metrics.impl.proxy;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;

import java.lang.reflect.*;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author w.glanzer, 12.07.2021
 */
class InterceptableObjectFactoryTest
{

  private IInterceptableObjectFactory objectFactory;

  @BeforeEach
  void setUp()
  {
    objectFactory = new InterceptableObjectFactory();
  }

  @Test
  void shouldCreateInterceptableProxy() throws Throwable
  {
    // Create Mock
    InvocationHandler handler = mock(InvocationHandler.class);
    _MyProxyObject object = spy(new _MyProxyObject());
    when(handler.invoke(any(), any(), any())).thenAnswer(pInv -> pInv.getArgument(1, Method.class).invoke(object, pInv.getArgument(2)));

    // Create object with factory
    _MyProxyObject proxyObject = objectFactory.createInterceptableProxy(object, handler);
    assertNotNull(proxyObject);

    // Invoke method on proxy
    proxyObject.test();

    // Verify
    verify(handler, times(1)).invoke(proxyObject, _MyProxyObject.class.getDeclaredMethod("test"), null);
    verify(object, times(1)).test();
  }

  @Test
  void shouldCreateInterceptableProxyCorrectAnnotations() throws Throwable
  {
    // Create Mock
    _MyProxyObjectAnnotations object = new _MyProxyObjectAnnotations();

    // Create object with factory
    _MyProxyObjectAnnotations proxyObject = objectFactory.createInterceptableProxy(object, mock(InvocationHandler.class));
    assertNotNull(proxyObject);
    assertArrayEquals(object.getClass().getDeclaredMethod("test").getAnnotations(), proxyObject.getClass().getDeclaredMethod("test").getAnnotations());
  }

  @Test
  void shouldCreateInterceptableProxy_withConstructor() throws Throwable
  {
    // Create Mock
    InvocationHandler handler = mock(InvocationHandler.class);
    _MyProxyObjectWithConstructor object = spy(new _MyProxyObjectWithConstructor("myValue"));
    when(handler.invoke(any(), any(), any())).thenAnswer(pInv -> pInv.getArgument(1, Method.class).invoke(object, pInv.getArgument(2)));

    // Create object with factory
    _MyProxyObjectWithConstructor proxyObject = objectFactory.createInterceptableProxy(object, handler);
    assertNotNull(proxyObject);

    // Invoke method on proxy
    assertEquals("myValue", proxyObject.getTest());
    assertEquals("myValue", proxyObject.test);

    // Verify
    verify(handler, times(1)).invoke(proxyObject, _MyProxyObjectWithConstructor.class.getDeclaredMethod("getTest"), null);
    verify(object, times(1)).getTest();
  }

  @Test
  void shouldCreateInterceptableProxy_withInterface() throws Throwable
  {
    // Create Mock
    InvocationHandler handler = mock(InvocationHandler.class);
    _MyProxyObjectWithInterface object = spy(new _MyProxyObjectWithInterface());
    when(handler.invoke(any(), any(), any())).thenAnswer(pInv -> pInv.getArgument(1, Method.class).invoke(object, pInv.getArgument(2)));

    // Create object with factory
    _MyProxyObjectWithInterface proxyObject = objectFactory.createInterceptableProxy(object, handler);
    assertNotNull(proxyObject);

    // Invoke method on proxy
    proxyObject.test();
    proxyObject.run();

    // Verify
    verify(handler, times(1)).invoke(proxyObject, _MyProxyObjectWithInterface.class.getDeclaredMethod("test"), null);
    verify(handler, times(1)).invoke(proxyObject, _MyProxyObjectWithInterface.class.getDeclaredMethod("run"), null);
    verify(object, times(1)).test();
    verify(object, times(1)).run();
  }

  @Test
  void shouldCreateInterceptableProxy_withSuperclass() throws Throwable
  {
    // Create Mock
    InvocationHandler handler = mock(InvocationHandler.class);
    _MyProxyObjectWithSuperclass object = spy(new _MyProxyObjectWithSuperclass());
    when(handler.invoke(any(), any(), any())).thenAnswer(pInv -> pInv.getArgument(1, Method.class).invoke(object, pInv.getArgument(2)));

    // Create object with factory
    _MyProxyObjectWithSuperclass proxyObject = objectFactory.createInterceptableProxy(object, handler);
    assertNotNull(proxyObject);

    // Invoke method on proxy
    proxyObject.test();
    proxyObject.run();

    // Verify
    verify(handler, times(1)).invoke(proxyObject, _MyProxyObjectWithSuperclass.class.getSuperclass().getDeclaredMethod("test"), null);
    verify(handler, times(1)).invoke(proxyObject, _MyProxyObjectWithSuperclass.class.getSuperclass().getDeclaredMethod("run"), null);
    verify(object, times(1)).test();
    verify(object, times(1)).run();
  }

  @Test
  void shouldCreateInterceptableProxy_withSuperclassAndInterface() throws Throwable
  {
    // Create Mock
    InvocationHandler handler = mock(InvocationHandler.class);
    _MyProxyObjectWithSuperclassAndInterface object = spy(new _MyProxyObjectWithSuperclassAndInterface());
    when(handler.invoke(any(), any(), any())).thenAnswer(pInv -> pInv.getArgument(1, Method.class).invoke(object, pInv.getArgument(2)));

    // Create object with factory
    _MyProxyObjectWithSuperclassAndInterface proxyObject = objectFactory.createInterceptableProxy(object, handler);
    assertNotNull(proxyObject);

    // Invoke method on proxy
    proxyObject.test();
    proxyObject.run();
    proxyObject.call();

    // Verify
    verify(handler, times(1)).invoke(proxyObject, _MyProxyObjectWithSuperclassAndInterface.class.getSuperclass().getDeclaredMethod("test"), null);
    verify(handler, times(1)).invoke(proxyObject, _MyProxyObjectWithSuperclassAndInterface.class.getSuperclass().getDeclaredMethod("run"), null);
    verify(handler, times(1)).invoke(proxyObject, _MyProxyObjectWithSuperclassAndInterface.class.getDeclaredMethod("call"), null);
    verify(object, times(1)).test();
    verify(object, times(1)).run();
    verify(object, times(1)).call();
  }

  private static class _MyProxyObject
  {
    @NotNull
    public String test()
    {
      return getClass().getName();
    }
  }

  private static class _MyProxyObjectAnnotations
  {
    @MyAnnotation
    @NotNull
    public String test()
    {
      return getClass().getName();
    }
  }

  private static class _MyProxyObjectWithConstructor
  {
    private final String test;

    public _MyProxyObjectWithConstructor(@NotNull String pTest)
    {
      test = pTest;
    }

    @NotNull
    public String getTest()
    {
      return test;
    }
  }

  private static class _MyProxyObjectWithInterface implements Runnable
  {
    @Override
    public void run()
    {

    }

    @NotNull
    public String test()
    {
      return getClass().getName();
    }
  }

  private static class _MyProxyObjectWithSuperclass extends _MyProxyObjectWithInterface
  {
  }

  private static class _MyProxyObjectWithSuperclassAndInterface extends _MyProxyObjectWithInterface
      implements Callable<Integer>
  {
    @Override
    public Integer call()
    {
      return 5;
    }
  }

  public @interface MyAnnotation
  {

  }

}