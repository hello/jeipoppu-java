package com.hello.jeipoppu.classifiers;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.hello.jeipoppu.algorithms.Algorithm;
import com.hello.jeipoppu.models.Classification;
import com.hello.suripu.api.audio.MatrixProtos.Matrix;
import com.hello.suripu.api.audio.MatrixProtos.MatrixClientMessage;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class BasicClassifier implements Classifier {

  private final static org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(BasicClassifier.class);
  private final static String MODEL_FILENAME = "/Users/jnorgan/HelloCode/scripts/data/audio_features/model_stddev.csv";

  private MatrixClientMessage message;
  private Algorithm algorithm;
  public List<Double[]> features;
  public Double[] processedFeatures;

  public BasicClassifier(final Algorithm algorithm) {
    this.algorithm = algorithm;
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

    features = getFeaturesList(data);
    if (features.isEmpty()) {
      return Collections.EMPTY_LIST;
    }

    processedFeatures = algorithm.compute(features);

    //Load models
    final Map<String, List<Double>> models = loadModels();

    //Compare vector to models to determine classification
    Double lowestDistance = 3000.0;
    String nearestModel = "";
    Map<String, Double> modelDistances = Maps.newHashMap();
    for (Map.Entry<String, List<Double>> model : models.entrySet()) {
      final String modelName = model.getKey();
      final Double distance = getDistance(processedFeatures, model.getValue());
      modelDistances.put(modelName, distance);
      if (distance < lowestDistance) {
        lowestDistance = distance;
        nearestModel = modelName;
      }
      //LOGGER.debug("Model: {} ; Distance: {}", modelName, distance);
    }
    modelDistances = sortByValue(modelDistances);
    LOGGER.debug("Sorted Models: {}", modelDistances.toString());
    //LOGGER.debug("Matrix processed for: {} with {} : {}", message.getDeviceId(), message.getUnixTime(), featureVectors.size());
    //LOGGER.debug("{}", toOctaveMatrixString(featureVectors));
    //LOGGER.debug("Processed featureVectors: {} via {}", processedFeatures, algorithm.getClass().getName());

    String selectedModel = modelDistances.keySet().toArray()[0].toString();
    String secondModel = modelDistances.keySet().toArray()[1].toString();

    Double modelDiff = Math.abs(modelDistances.get(selectedModel) - modelDistances.get(secondModel));
    if (modelDiff < 0.2) {
      LOGGER.debug("{} MAYBE {}", selectedModel, secondModel);
    } else {
      LOGGER.debug("{} FAIRLY CERTAIN", selectedModel);
    }

    final Classification classification = new Classification(selectedModel, modelDistances.get(selectedModel));
    return Lists.newArrayList(classification);
  }

  public Double[] getProcessedFeatures() {
    return processedFeatures;
  }

  public static <K, V extends Comparable<? super V>> Map<K, V>
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

    Map<K, V> result = new LinkedHashMap<>();
    for (Map.Entry<K, V> entry : list)
    {
      result.put( entry.getKey(), entry.getValue() );
    }
    return result;
  }

  public Algorithm getAlgorithm() {
    return algorithm;
  }

  private double getDistance(final Double[] processedFeatures, final List<Double> modelFeatures) {
    //int[] distanceVector = new int[processedFeatures.length];
    double distance = 0;
    for (int x=0; x<processedFeatures.length; x++) {
      distance += Math.pow((modelFeatures.get(x) - processedFeatures[x]), 2.0);
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


  private List<Double[]> getFeaturesList(List<Integer> data) {

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

  //TODO: abtract this method out to handle model loading from other sources
  public Map<String, List<Double>> loadModels() {

    BufferedReader br = null;
    Map<String, List<Double>> returnModels = Maps.newHashMap();

    try {

      String line;
      String MODEL_FILENAME = "/Users/jnorgan/HelloCode/scripts/data/audio_features/" + algorithm.getModelName();

      br = new BufferedReader(new FileReader(MODEL_FILENAME));
      while ((line = br.readLine()) != null) {
        List<Double> returnFeatures = Lists.newArrayList();
        //final String line = br.readLine();
        String[] parts = line.split(":");
        String[] featureStrings = parts[1].split(",");

        for (final String feature : featureStrings) {
          returnFeatures.add(Double.parseDouble(feature.trim()));
        }
        returnModels.put(parts[0], returnFeatures);
      }


    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (br != null) {
        try {
          br.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    return returnModels;
  }
}
