package com.hello.jeipoppu.algorithms;

import com.google.common.collect.Lists;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import java.util.Arrays;
import java.util.List;

public class StdDevAlgorithm implements Algorithm {

  private String modelName;

  public StdDevAlgorithm(final String modelName) {
    this.modelName = modelName;
  }

  public String getModelName() {
    return modelName;
  }

  public Double[] compute(final List<Double[]> features) {

    final Double[] stdDevOfFeatures = stdDevOfFeatures(features);

    return stdDevOfFeatures;
  }

  public Double[] computeAggregate(List<Double[]> stdDevs) {

    //Compute average standard deviation over List of stdDev arrays
    Double[] avgStdDevs = new Double[stdDevs.get(0).length];
    Arrays.fill(avgStdDevs, new Double(0.0));
    Double[] varianceSums = new Double[stdDevs.get(0).length];
    Arrays.fill(varianceSums, new Double(0.0));

    for(final Double[] stdDevArray : stdDevs) {
      for (int i=0; i<stdDevArray.length; i++) {
        varianceSums[i] += Math.pow(stdDevArray[i], 2.0);
      }
    }

    //Take the square root of the averaged variances
    for (int i=0; i<varianceSums.length; i++) {
      avgStdDevs[i] = Math.sqrt(varianceSums[i] / stdDevs.size());
    }

    return avgStdDevs;
  }

  private Double[] stdDevOfFeatures(final List<Double[]> features) {
    List<SummaryStatistics> statList = Lists.newArrayList();
    for(final Double[] feature : features) {
      for (int i=0; i<feature.length; i++) {
        if(statList.size() == i) {
          statList.add(new SummaryStatistics());
        }
        statList.get(i).addValue(feature[i]);
      }
    }

    Double[] stdDevs = new Double[features.get(0).length];
    Arrays.fill(stdDevs, new Double(0.0));
    int i = 0;
    for(final SummaryStatistics stats : statList) {
        stdDevs[i] = stats.getStandardDeviation();
        i++;
    }

    return stdDevs;
  }
}
