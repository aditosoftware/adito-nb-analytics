package de.adito.aditoweb.nbm.metrics.impl.handlers;

import org.jetbrains.annotations.*;

import java.util.*;

/**
 * A MultiValueGauge act like a value-container-hack, so
 * that all values get sent to the prometheus endpoint
 *
 * @author w.glanzer, 20.10.2021
 */
public class MultiValueGauge implements com.codahale.metrics.Gauge<List<Object>>
{

  private final List<Object> values = new ArrayList<>();
  private final boolean clearOnRead;

  public MultiValueGauge(boolean pClearOnRead)
  {
    clearOnRead = pClearOnRead;
  }

  @Override
  public List<Object> getValue()
  {
    synchronized (values)
    {
      List<Object> currentValues = List.copyOf(values);
      if (clearOnRead)
        values.clear();
      return currentValues;
    }
  }

  public void addValue(@Nullable Map<String, String> pAdditionalLabels, @NotNull Object pValue)
  {
    synchronized (values)
    {
      Object value = pValue;
      if (pAdditionalLabels != null)
        value = new ILabelProvider()
        {
          @Override
          public Map<String, String> getLabels()
          {
            return pAdditionalLabels;
          }

          @Override
          public Object getValue()
          {
            return pValue;
          }
        };
      values.add(value);
    }
  }

  public interface ILabelProvider
  {
    Map<String, String> getLabels();

    Object getValue();
  }

}
