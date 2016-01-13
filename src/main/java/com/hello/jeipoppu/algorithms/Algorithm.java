package com.hello.jeipoppu.algorithms;


import java.util.List;
import java.util.Map;

public interface Algorithm {

  public Double[] compute(final List<Double[]> features);

  public Double[] computeAggregate(final List<Double[]> values);

  Map<String, Double[]> getModels ();
}
