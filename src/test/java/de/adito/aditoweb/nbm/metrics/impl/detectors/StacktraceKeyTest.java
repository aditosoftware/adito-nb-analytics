package de.adito.aditoweb.nbm.metrics.impl.detectors;

import com.google.common.cache.*;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author m.kaspera, 17.12.2021
 */
class StacktraceKeyTest
{

  @Test
  void isSameKeyEqual()
  {
    assertEquals(new StacktraceKey(0, 10), new StacktraceKey(0, 10));
    assertEquals(new StacktraceKey(1, 11), new StacktraceKey(1, 11));
    assertEquals(new StacktraceKey(2, 12), new StacktraceKey(2, 12));
    assertEquals(new StacktraceKey(3, 13), new StacktraceKey(3, 13));
    assertEquals(new StacktraceKey(4, 10), new StacktraceKey(4, 10));
    assertEquals(new StacktraceKey(5, 11), new StacktraceKey(5, 11));
    assertEquals(new StacktraceKey(6, 12), new StacktraceKey(6, 12));
  }

  @Test
  void isDifferentKeyNotEquals()
  {
    assertNotEquals(new StacktraceKey(0, 10), new StacktraceKey(5, 10));
  }

  @Test
  void isKeyWorksInCache()
  {
    Cache<StacktraceKey, StackTraceElement[]> stackTraceCache = CacheBuilder.newBuilder().build();
    stackTraceCache.put(new StacktraceKey(0, 10), new StackTraceElement[0]);
    Assertions.assertNotNull(stackTraceCache.getIfPresent(new StacktraceKey(0, 10)));
    Assertions.assertNull(stackTraceCache.getIfPresent(new StacktraceKey(1, 10)));
  }
}