package com.hello.jeipoppu.algorithms;


import java.util.List;

public interface Algorithm {

  public Double[] compute(final List<Double[]> features);

  public Double[] computeAggregate(final List<Double[]> values);

  public String getModelName();
}
