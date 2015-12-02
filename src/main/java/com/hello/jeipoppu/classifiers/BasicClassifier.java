package com.hello.jeipoppu.classifiers;

import com.google.common.collect.Lists;
import com.google.common.base.Joiner;

import com.hello.jeipoppu.models.Classification;
import com.hello.suripu.api.audio.MatrixProtos;
import com.hello.suripu.api.audio.MatrixProtos.Matrix;
import com.hello.suripu.api.audio.MatrixProtos.MatrixClientMessage;

import org.slf4j.LoggerFactory;

import java.util.List;

public class BasicClassifier implements Classifier {

  private final static org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(BasicClassifier.class);

  private MatrixClientMessage message;

  public BasicClassifier(final MatrixClientMessage message) {
    this.message = message;
  }

  public Classifier create(final MatrixClientMessage message) {
    return new BasicClassifier(message);
  }

  public Classification run() {
    final List<Integer> data = message.getMatrixList(0).getIdataList();
    final List<Integer[]> features = getFeaturesList(data);

    final Integer energy = getEnergySum(data);

    LOGGER.debug("Matrix processed for: {} with {} : {}", message.getDeviceId(), message.getUnixTime(), features.size());
    LOGGER.debug("{}", toOctaveMatrixString(features));
    LOGGER.debug("Energy Sum: {}", energy.toString());

    return new Classification("Test Classification");
  }

  public Boolean isEmpty() {
    if (!(message.getMatrixListCount() > 0)) {
      return true;
    }
    return false;
  }


  private Integer getEnergySum(List<Integer> data) {
    Integer energySum = 0;
    for (final Integer feat : data) {
      energySum += feat.intValue();
    }
    return energySum;
  }

  private List<Integer[]> getFeaturesList(List<Integer> data) {

    final Matrix matrix = message.getMatrixList(0);
    Integer row = 0;
    Integer col = 0;
    final Integer maxCols = matrix.getCols();
    final Integer maxRows = matrix.getRows();
    Integer[] featureArray = new Integer[maxCols];
    List<Integer[]> features = Lists.newArrayList();
    for (final Integer feat : data) {
      featureArray[col] = feat;
      col++;
      if (col >= maxCols) {
        col = 0;
        features.add(featureArray.clone());
      }
    }

    return features;
  }

  private String toOctaveMatrixString(List<Integer[]> featureList) {
    String featureMatrix = "";
    for (final Integer[] feature : featureList) {
      featureMatrix = featureMatrix.concat(Joiner.on(",").join(feature).concat("; "));
    }
    return featureMatrix;

  }

}
