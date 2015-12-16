package com.hello.jeipoppu.algorithms;

import java.util.Arrays;
import java.util.List;

public class SumAlgorithm implements Algorithm {

  private String modelName;

  public SumAlgorithm(final String modelName) {
    this.modelName = modelName;
  }

  public String getModelName() {
    return modelName;
  }

  public Double[] compute(final List<Double[]> features) {
    return sumFeatures(features);
  }

  public Double[] computeAggregate(List<Double[]> listOfSumArrays) {
    return sumFeatures(listOfSumArrays);
  }

  private Double[] sumFeatures(final List<Double[]> features) {
    Double[] summedArray = new Double[features.get(0).length];

    Arrays.fill(summedArray, new Double(0.0));
    for(final Double[] feature : features) {
      for (int i=0; i<feature.length; i++) {
        summedArray[i] += feature[i];
      }
    }

    return summedArray;
  }
}
