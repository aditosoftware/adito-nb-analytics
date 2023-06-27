package de.adito.aditoweb.nbm.metrics.impl.eventlogger.sentry;

import io.sentry.protocol.*;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

import java.lang.management.ThreadInfo;
import java.util.*;

/**
 * @author w.glanzer, 23.06.2022
 */
class SentryThreadUtility
{

  private static final Set<String> INAPP_PACKAGES = Set.of("de.adito.");

  /**
   * Converts a current thread to a SentryThread
   *
   * @param thread the thread to be converted
   * @return a SentryThread
   */
  public static @NonNull SentryThread getSentryThread(@NonNull ThreadInfo thread)
  {
    SentryThread sentryThread = new SentryThread();

    sentryThread.setName(thread.getThreadName());
    sentryThread.setPriority(thread.getPriority());
    sentryThread.setId(thread.getThreadId());
    sentryThread.setDaemon(thread.isDaemon());
    sentryThread.setState(thread.getThreadState().name());
    sentryThread.setCrashed(false);

    List<SentryStackFrame> frames = getStackFrames(thread.getStackTrace());

    if (frames != null && !frames.isEmpty())
    {
      SentryStackTrace sentryStackTrace = new SentryStackTrace(frames);

      // threads are always gotten either via Thread.currentThread() or Thread.getAllStackTraces()
      sentryStackTrace.setSnapshot(true);
      sentryThread.setStacktrace(sentryStackTrace);
    }

    return sentryThread;
  }

  /**
   * convert an Array of Java StackTraceElements to a list of SentryStackFrames
   *
   * @param elements Array of Java StackTraceElements
   * @return list of SentryStackFrames or null if none
   */
  @Nullable
  private static List<SentryStackFrame> getStackFrames(@Nullable StackTraceElement[] elements)
  {
    List<SentryStackFrame> sentryStackFrames = null;

    if (elements != null && elements.length > 0)
    {
      sentryStackFrames = new ArrayList<>();
      for (StackTraceElement item : elements)
      {
        if (item != null)
        {
          // we don't want to add our own frames
          String className = item.getClassName();
          if (className.startsWith("io.sentry.")
              && !className.startsWith("io.sentry.samples.")
              && !className.startsWith("io.sentry.mobile."))
          {
            continue;
          }

          SentryStackFrame sentryStackFrame = new SentryStackFrame();
          sentryStackFrame.setInApp(INAPP_PACKAGES.stream().anyMatch(className::startsWith));
          sentryStackFrame.setModule(className);
          sentryStackFrame.setFunction(item.getMethodName());
          sentryStackFrame.setFilename(item.getFileName());
          // Protocol doesn't accept negative line numbers.
          // The runtime seem to use -2 as a way to signal a native method
          if (item.getLineNumber() >= 0)
          {
            sentryStackFrame.setLineno(item.getLineNumber());
          }
          sentryStackFrame.setNative(item.isNativeMethod());
          sentryStackFrames.add(sentryStackFrame);
        }
      }
      Collections.reverse(sentryStackFrames);
    }

    return sentryStackFrames;
  }

}
