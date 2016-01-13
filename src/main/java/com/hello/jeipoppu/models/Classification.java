package com.hello.jeipoppu.models;


public class Classification {

  public String name;
  private double confidence;

  public Classification(final String classificationName, final double confidence) {
    this.name = classificationName;
    this.confidence = confidence;
  }

  public String toString() {
    return name;
  }

  public double getConfidence() {
    return confidence;
  }

}
