package com.hello.jeipoppu.models;


import com.hello.suripu.api.audio.AudioClassificationProtos;

public class Classification {

  public AudioClassificationProtos.audio_class_result.audio_class type;
  private double confidence;

  public Classification(final AudioClassificationProtos.audio_class_result.audio_class classificationType, final double confidence) {
    this.type = classificationType;
    this.confidence = confidence;
  }

  public String toString() {
    return type.toString();
  }

  public double getConfidence() {
    return confidence;
  }

}
