package de.adito.aditoweb.nbm.metrics.impl.detectors;

import de.adito.aditoweb.nbm.metrics.api.IMetricProxyFactory;
import de.adito.aditoweb.nbm.metrics.api.types.Histogram;
import de.adito.aditoweb.nbm.metrics.impl.eventlogger.IEventLogger;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.metrics.IEDTStressDetector;
import org.openide.util.lookup.ServiceProvider;

import java.lang.management.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * @author m.kaspera, 20.12.2021
 */
@ServiceProvider(service = IEDTStressDetector.class)
public class EDTStressDetector extends ARunnableDetector implements IEDTStressDetector
{

  private static final double STRESS_PERCENTAGE = Double.parseDouble(System.getProperty(IEDTStressDetector.ALERT_LEVEL, "66"));
  private static final Logger LOGGER = Logger.getLogger(EDTStressDetector.class.getName());

  @Override
  String getThreadNameFormat()
  {
    return "Analytics-EDTStressDetector-%d";
  }

  @Override
  long getTimeInterval()
  {
    return 1;
  }

  @Override
  Runnable getRunner()
  {
    return new _Executor();
  }

  @Override
  Logger getLogger()
  {
    return LOGGER;
  }

  static class _Executor implements Runnable
  {
    private static final long NANOS_IN_SECONDS = 1_000_000_000L;
    private long lastEDTCPUTime = -1;
    private long lastEDTUserTime = -1;
    private final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
    private final long edtID = Arrays.stream(threadBean.getAllThreadIds())
        .filter(pID -> threadBean.getThreadInfo(pID).getThreadName().startsWith("AWT-EventQueue"))
        .findFirst()
        .orElseThrow();
    private _Executor metricsProxy;
    private boolean lastThresholdExceeded = false;

    @Override
    public void run()
    {
      if (metricsProxy == null)
        metricsProxy = IMetricProxyFactory.proxy(this);
      long edtCPUTime = threadBean.getThreadCpuTime(edtID);
      long edtUserTime = threadBean.getThreadUserTime(edtID);
      if (lastEDTCPUTime > -1)
      {
        long diff = edtCPUTime - lastEDTCPUTime;
        long userDiff = edtUserTime - lastEDTUserTime;
        long diffPercentage = diff * 100 / NANOS_IN_SECONDS;
        long userDiffPercentage = userDiff * 100 / NANOS_IN_SECONDS;

        long maxPercentage = Math.max(diffPercentage, userDiffPercentage);
        metricsProxy.logStress(maxPercentage);

        if (maxPercentage > STRESS_PERCENTAGE && !lastThresholdExceeded && !isCurrentlyIdle(edtID))
        {
          ThreadInfo edtThreadInfo = threadBean.getThreadInfo(edtID, Integer.MAX_VALUE);
          IEventLogger.getInstance().captureEDTStress(edtThreadInfo, () -> threadBean.dumpAllThreads(true, true));
        }
        lastThresholdExceeded = maxPercentage > STRESS_PERCENTAGE;
      }

      lastEDTCPUTime = edtCPUTime;
      lastEDTUserTime = edtUserTime;
    }

    /**
     * Check if the edt thread is currently idle
     *
     * @param pProcessId id of the process to check
     * @return true if the thread is currently parked and idle, false otherwise (also returns false if in waiting status)
     */
    private boolean isCurrentlyIdle(long pProcessId)
    {
      StackTraceElement[] stackTrace = threadBean.getThreadInfo(pProcessId, 1).getStackTrace();
      if (stackTrace != null && stackTrace.length > 0)
        return "jdk.internal.misc.Unsafe".equals(stackTrace[0].getClassName()) && "park".equals(stackTrace[0].getMethodName());
      return false;
    }

    @Histogram(name = "EDTStressPercentage")
    private double logStress(double pStressPercentage)
    {
      return pStressPercentage;
    }
  }

}
