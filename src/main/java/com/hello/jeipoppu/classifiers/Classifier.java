package com.hello.jeipoppu.classifiers;

import com.hello.jeipoppu.algorithms.Algorithm;
import com.hello.jeipoppu.models.Classification;
import com.hello.suripu.api.audio.MatrixProtos;

import java.util.List;

public interface Classifier {

  public List<Classification> run(final MatrixProtos.MatrixClientMessage message);
  public Algorithm getAlgorithm();
  Double[] getProcessedFeatures();
  Integer getFeatureCount();
}
