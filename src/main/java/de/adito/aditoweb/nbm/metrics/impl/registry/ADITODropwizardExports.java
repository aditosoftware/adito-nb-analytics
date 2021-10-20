package de.adito.aditoweb.nbm.metrics.impl.registry;

import com.codahale.metrics.Timer;
import com.codahale.metrics.*;
import de.adito.aditoweb.nbm.metrics.impl.handlers.MultiValueGauge;
import io.prometheus.client.dropwizard.samplebuilder.SampleBuilder;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.*;
import java.util.stream.Collectors;

/**
 * Collect Dropwizard metrics from a MetricRegistry.
 * This class is similar to the original {@link io.prometheus.client.dropwizard.DropwizardExports} class, but with extended gauge label functionality
 *
 * @author w.glanzer, 20.10.2021
 */
class ADITODropwizardExports extends io.prometheus.client.Collector implements io.prometheus.client.Collector.Describable
{
  private static final Logger LOGGER = Logger.getLogger(ADITODropwizardExports.class.getName());
  private final MetricRegistry registry;
  private final SampleBuilder sampleBuilder;

  /**
   * @param pRegistry      a metric registry to export in prometheus.
   * @param pSampleBuilder sampleBuilder to use to create prometheus samples.
   */
  public ADITODropwizardExports(MetricRegistry pRegistry, SampleBuilder pSampleBuilder)
  {
    registry = pRegistry;
    sampleBuilder = pSampleBuilder;
  }

  @Override
  public List<MetricFamilySamples> collect()
  {
    Map<String, MetricFamilySamples> mfSamplesMap = new HashMap<>();

    //noinspection rawtypes
    for (SortedMap.Entry<String, Gauge> entry : registry.getGauges().entrySet())
    {
      if (entry.getValue() instanceof MultiValueGauge)
        _addToMap(mfSamplesMap, _fromMultiValueGauge(entry.getKey(), (MultiValueGauge) entry.getValue()));
      else
        _addToMap(mfSamplesMap, _fromGauge(entry.getKey(), entry.getValue()));
    }
    for (SortedMap.Entry<String, Counter> entry : registry.getCounters().entrySet())
      _addToMap(mfSamplesMap, _fromCounter(entry.getKey(), entry.getValue()));
    for (SortedMap.Entry<String, Histogram> entry : registry.getHistograms().entrySet())
      _addToMap(mfSamplesMap, _fromHistogram(entry.getKey(), entry.getValue()));
    for (SortedMap.Entry<String, Timer> entry : registry.getTimers().entrySet())
      _addToMap(mfSamplesMap, _fromTimer(entry.getKey(), entry.getValue()));
    for (SortedMap.Entry<String, Meter> entry : registry.getMeters().entrySet())
      _addToMap(mfSamplesMap, _fromMeter(entry.getKey(), entry.getValue()));
    return new ArrayList<>(mfSamplesMap.values());
  }

  @Override
  public List<MetricFamilySamples> describe()
  {
    return new ArrayList<>();
  }

  /**
   * Export counter as Prometheus <a href="https://prometheus.io/docs/concepts/metric_types/#gauge">Gauge</a>.
   */
  private MetricFamilySamples _fromCounter(String pDropwizardName, Counter pCounter)
  {
    MetricFamilySamples.Sample sample = sampleBuilder.createSample(pDropwizardName, "", new ArrayList<>(), new ArrayList<>(),
                                                                   Long.valueOf(pCounter.getCount()).doubleValue());
    return new MetricFamilySamples(sample.name, Type.GAUGE, getHelpMessage(pDropwizardName, pCounter), List.of(sample));
  }

  private Double _getGaugeValue(@NotNull String pDropwizardName, @Nullable Object pValue)
  {
    if (pValue instanceof Number)
      return ((Number) pValue).doubleValue();
    else if (pValue instanceof Boolean)
      return ((Boolean) pValue) ? 1D : 0D;
    LOGGER.log(Level.FINE, String.format("Invalid type for Gauge %s: %s", sanitizeMetricName(pDropwizardName),
                                         pValue == null ? "null" : pValue.getClass().getName()));
    return null;
  }

  private MetricFamilySamples _fromMultiValueGauge(@NotNull String pDropwizardName, @NotNull MultiValueGauge pGauge)
  {
    List<MetricFamilySamples.Sample> samples = new ArrayList<>();
    for (Object gaugeValue : pGauge.getValue())
    {
      if (gaugeValue instanceof MultiValueGauge.ILabelProvider)
      {
        // Add a sample, where labels are given
        Double value = _getGaugeValue(pDropwizardName, ((MultiValueGauge.ILabelProvider) gaugeValue).getValue());
        if (value != null)
        {
          Map<String, String> labels = ((MultiValueGauge.ILabelProvider) gaugeValue).getLabels();
          List<String> labelNames = new ArrayList<>(labels.keySet());
          List<String> labelValues = labelNames.stream()
              .map(labels::get)
              .collect(Collectors.toList());
          samples.add(sampleBuilder.createSample(pDropwizardName, "", labelNames, labelValues, value));
        }
      }
      else
      {
        // Add plain sample
        Double value = _getGaugeValue(pDropwizardName, gaugeValue);
        if (value != null)
          samples.add(sampleBuilder.createSample(pDropwizardName, "", List.of(), List.of(), value));
      }
    }

    if (samples.isEmpty())
      return null;

    return new MetricFamilySamples(samples.get(0).name, Type.GAUGE, getHelpMessage(pDropwizardName, pGauge), samples);
  }

  /**
   * Export gauge as a prometheus gauge.
   */
  private MetricFamilySamples _fromGauge(@NotNull String pDropwizardName, @NotNull Gauge<?> pGauge)
  {
    Double value = _getGaugeValue(pDropwizardName, pGauge.getValue());
    if (value == null)
      return null;
    MetricFamilySamples.Sample sample = sampleBuilder.createSample(pDropwizardName, "", new ArrayList<>(), new ArrayList<>(), value);
    return new MetricFamilySamples(sample.name, Type.GAUGE, getHelpMessage(pDropwizardName, pGauge), List.of(sample));
  }

  /**
   * Export a histogram snapshot as a prometheus SUMMARY.
   *
   * @param pDropwizardName metric name.
   * @param pSnapshot       the histogram snapshot.
   * @param pCount          the total sample count for this snapshot.
   * @param pFactor         a factor to apply to histogram values.
   */
  private MetricFamilySamples _fromSnapshotAndCount(@NotNull String pDropwizardName, Snapshot pSnapshot, long pCount, double pFactor, String pHelp)
  {
    List<MetricFamilySamples.Sample> samples = Arrays.asList(
        sampleBuilder.createSample(pDropwizardName, "", List.of("quantile"), List.of("0.5"), pSnapshot.getMedian() * pFactor),
        sampleBuilder.createSample(pDropwizardName, "", List.of("quantile"), List.of("0.75"), pSnapshot.get75thPercentile() * pFactor),
        sampleBuilder.createSample(pDropwizardName, "", List.of("quantile"), List.of("0.95"), pSnapshot.get95thPercentile() * pFactor),
        sampleBuilder.createSample(pDropwizardName, "", List.of("quantile"), List.of("0.98"), pSnapshot.get98thPercentile() * pFactor),
        sampleBuilder.createSample(pDropwizardName, "", List.of("quantile"), List.of("0.99"), pSnapshot.get99thPercentile() * pFactor),
        sampleBuilder.createSample(pDropwizardName, "", List.of("quantile"), List.of("0.999"), pSnapshot.get999thPercentile() * pFactor),
        sampleBuilder.createSample(pDropwizardName, "_count", new ArrayList<>(), new ArrayList<>(), pCount)
    );
    return new MetricFamilySamples(samples.get(0).name, Type.SUMMARY, pHelp, samples);
  }

  /**
   * Convert histogram snapshot.
   */
  private MetricFamilySamples _fromHistogram(@NotNull String pDropwizardName, Histogram pHistogram)
  {
    return _fromSnapshotAndCount(pDropwizardName, pHistogram.getSnapshot(), pHistogram.getCount(), 1.0,
                                 getHelpMessage(pDropwizardName, pHistogram));
  }

  /**
   * Export Dropwizard Timer as a histogram. Use TIME_UNIT as time unit.
   */
  private MetricFamilySamples _fromTimer(@NotNull String pDropwizardName, Timer pTimer)
  {
    return _fromSnapshotAndCount(pDropwizardName, pTimer.getSnapshot(), pTimer.getCount(),
                                 1.0D / TimeUnit.SECONDS.toNanos(1L), getHelpMessage(pDropwizardName, pTimer));
  }

  /**
   * Export a Meter as as prometheus COUNTER.
   */
  private MetricFamilySamples _fromMeter(@NotNull String pDropwizardName, Meter pMeter)
  {
    final MetricFamilySamples.Sample sample = sampleBuilder.createSample(pDropwizardName, "_total",
                                                                         new ArrayList<>(),
                                                                         new ArrayList<>(),
                                                                         pMeter.getCount());
    return new MetricFamilySamples(sample.name, Type.COUNTER, getHelpMessage(pDropwizardName, pMeter),
                                   List.of(sample));
  }


  private void _addToMap(Map<String, MetricFamilySamples> mfSamplesMap, MetricFamilySamples newMfSamples)
  {
    if (newMfSamples != null)
    {
      MetricFamilySamples currentMfSamples = mfSamplesMap.get(newMfSamples.name);
      if (currentMfSamples == null)
      {
        mfSamplesMap.put(newMfSamples.name, newMfSamples);
      }
      else
      {
        List<MetricFamilySamples.Sample> samples = new ArrayList<>(currentMfSamples.samples);
        samples.addAll(newMfSamples.samples);
        mfSamplesMap.put(newMfSamples.name, new MetricFamilySamples(newMfSamples.name, currentMfSamples.type, currentMfSamples.help, samples));
      }
    }
  }

  private static String getHelpMessage(String metricName, Metric metric)
  {
    return String.format("Generated from ADITO dropwizard metric export (metric=%s, type=%s)", metricName, metric.getClass().getName());
  }
}