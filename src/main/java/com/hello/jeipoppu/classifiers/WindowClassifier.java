package com.hello.jeipoppu.classifiers;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.hello.jeipoppu.algorithms.Algorithm;
import com.hello.jeipoppu.models.Classification;
import com.hello.suripu.api.audio.AudioClassificationProtos.audio_class_result.audio_class;
import com.hello.suripu.api.audio.MatrixProtos.Matrix;
import com.hello.suripu.api.audio.MatrixProtos.MatrixClientMessage;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class WindowClassifier implements Classifier {

  private final static org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(WindowClassifier.class);

  private MatrixClientMessage message;
  private Algorithm algorithm;
  public List<Double[]> featureVectors;
  public Double[] processedFeatures;
  private Map<String, Double[]> models;
  public List<Double[]> processedWindows;
  private Integer windowSize;

  public WindowClassifier(final Algorithm algorithm, final Integer windowSize) {
    this.algorithm = algorithm;
    models = algorithm.getModels();
    processedWindows = Lists.newArrayList();
    this.windowSize = windowSize;
  }

  public List<Classification> run(final MatrixClientMessage message) {

    this.message = message;

    List<Integer> data = Lists.newArrayList();
    //Process incoming data through algorithm to produce vector
    for (final Matrix matrix : message.getMatrixListList()) {
      if (matrix.getId().equals("feature_chunk")) {
        data.addAll(matrix.getIdataList());
      }
    }

    featureVectors = getFeatureVectorsList(data);

    if (featureVectors.isEmpty()) {
      return Collections.EMPTY_LIST;
    }

    Integer adaptiveWindowSize = windowSize;

    if (featureVectors.size() <= windowSize) {
      adaptiveWindowSize = featureVectors.size() / 2;
    }

    List<Classification> classifications = Lists.newArrayList();
    for (int x=0; x < featureVectors.size() - (adaptiveWindowSize - 1); x++) {
      List<Double[]> windowVectors = Lists.newArrayList();
      for(int y =0; y < adaptiveWindowSize; y++) {
        windowVectors.add(featureVectors.get(x + y));
      }
      final Double[] processedWindow = algorithm.compute(windowVectors);
      processedWindows.add(processedWindow);
      final Classification windowClassification = determineClassification(processedWindow);
      classifications.add(windowClassification);
    }

    //roll through feature vectors and determine classification
    processedFeatures = algorithm.computeAggregate(processedWindows);

    final Classification featureClassification = determineClassification(processedFeatures);

    return classifications;
  }

  public Double[] getProcessedFeatures() {
    return processedFeatures;
  }

  public Integer getFeatureCount() { return featureVectors.size(); }

  public Classification determineClassification(final Double[] features) {
    //Compare vector to models to determine classification
    Double lowestDistance = 3000.0;
    String nearestModel = "";
    Map<String, Double> modelDistances = Maps.newHashMap();
    for (Map.Entry<String, Double[]> model : models.entrySet()) {
      final String modelName = model.getKey();
      final Double distance = getDistance(features, model.getValue());
      modelDistances.put(modelName, distance);
      if (distance < lowestDistance) {
        lowestDistance = distance;
        nearestModel = modelName;
      }
      //LOGGER.debug("Model: {} ; Distance: {}", modelName, distance);
    }
    modelDistances = sortByValue(modelDistances);
//    System.out.println(modelDistances.toString());
//    System.out.println(lowestDistance);
    //LOGGER.debug("Sorted Models: {}", modelDistances.toString());
    //LOGGER.debug("Matrix processed for: {} with {} : {}", message.getDeviceId(), message.getUnixTime(), featureVectors.size());
    //LOGGER.debug("Processed featureVectors: {} via {}", processedFeatures, algorithm.getClass().getName());

    String selectedModel = modelDistances.keySet().toArray()[0].toString();
    String secondModel = modelDistances.keySet().toArray()[1].toString();

    Double modelDiff = Math.abs(modelDistances.get(selectedModel) - modelDistances.get(secondModel));
//    if (modelDiff < 0.2 && !(selectedModel.contains("Snor") && secondModel.contains("Snor"))) {
//      //LOGGER.debug("{} MAYBE {}", selectedModel, secondModel);
//      return new Classification("Uncertain", 0.0f);
//    }

    if (lowestDistance > 3.5f) {
      return new Classification(audio_class.UNKNOWN, 0.0f);
    }

    if(selectedModel.startsWith("Snoring")){
      return new Classification(audio_class.SNORING, modelDistances.get(selectedModel));
    }
    if(selectedModel.startsWith("Speech")){
      return new Classification(audio_class.TALKING, modelDistances.get(selectedModel));
    }
    if(selectedModel.startsWith("Noise")){
      return new Classification(audio_class.UNKNOWN, modelDistances.get(selectedModel));
    }

    return new Classification(audio_class.NULL, modelDistances.get(selectedModel));
  }

  public static <K, V extends Comparable<? super V>> LinkedHashMap<K, V>
  sortByValue( Map<K, V> map )
  {
    List<Map.Entry<K, V>> list =
        new LinkedList<>( map.entrySet() );
    Collections.sort( list, new Comparator<Map.Entry<K, V>>()
    {
      @Override
      public int compare( Map.Entry<K, V> o1, Map.Entry<K, V> o2 )
      {
        return (o1.getValue()).compareTo( o2.getValue() );
      }
    } );

    LinkedHashMap<K, V> result = new LinkedHashMap<>();
    for (Map.Entry<K, V> entry : list)
    {
      result.put( entry.getKey(), entry.getValue() );
    }
    return result;
  }

  public Algorithm getAlgorithm() {
    return algorithm;
  }

  private double getDistance(final Double[] processedFeatures, final Double[] modelFeatures) {
    //int[] distanceVector = new int[processedFeatures.length];
    double distance = 0;
    for (int x=0; x<processedFeatures.length; x++) {
      distance += Math.pow((modelFeatures[x] - processedFeatures[x]), 2.0);
    }
    return Math.sqrt(distance);
  }

  public Boolean isEmpty() {
    if (!(message.getMatrixListCount() > 0)) {
      return true;
    }
    return false;
  }

  private int[] trimValuesBelow(int[] inputArray, final Integer trimValue) {
    int[] trimmedArray = new int[inputArray.length];
    for(int i=0;i<inputArray.length; i++) {
      if (inputArray[i] < trimValue) {
        trimmedArray[i] = trimValue;
      } else {
        trimmedArray[i] = inputArray[i];
      }
    }

    return trimmedArray;
  }


  private List<Double[]> getFeatureVectorsList(List<Integer> data) {

    if(message.getMatrixListCount() < 1) {
      return Collections.emptyList();
    }
    final Matrix matrix = message.getMatrixList(0);
    Integer col = 0;
    final Integer maxCols = matrix.getCols();
    Double[] featureArray = new Double[maxCols];
    List<Double[]> features = Lists.newArrayList();
    for (final Integer feat : data) {
      featureArray[col] = feat.doubleValue();
      col++;
      if (col >= maxCols) {
        col = 0;
        features.add(featureArray.clone());
      }
    }

    return features;
  }

  public String toOctaveMatrixString(List<Double[]> featureList) {
    String featureMatrix = "";
    for (final Double[] feature : featureList) {
      featureMatrix = featureMatrix.concat(Joiner.on(",").join(feature).concat("; "));
    }
    return featureMatrix;

  }
}
