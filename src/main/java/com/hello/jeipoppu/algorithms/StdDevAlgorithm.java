package com.hello.jeipoppu.algorithms;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class StdDevAlgorithm implements Algorithm {

  private Map<String, Double[]> models;

  public StdDevAlgorithm() {
    models = Maps.newHashMap();

    //Windowed model
    models.put("Noise", new Double[]{2.519247433092869, 2.0307077916411505, 2.240904974905877, 1.9964355935821132, 1.7919166016792782, 1.786374784530102, 1.5898494738345077, 1.4717945393600431, 1.3465761418434183, 1.188923955837805, 0.9764227657830212, 0.8729976360866417, 0.5834331973125929, 0.4474551870403379, 0.14606349979574754, 0.0});
    models.put("Snoring Multi", new Double[]{2.08443731337729, 1.7777525473610318, 2.017710304086148, 1.9755263965027186, 1.879813612934076, 1.817944631385272, 1.698245558219341, 1.4971665839159634, 1.4069981633956254, 1.2320098503748675, 1.0467458894643416, 0.8762018268140126, 0.6999947371656364, 0.5205247313333029, 0.19861803567534603, 0.0});
    models.put("Snoring Only", new Double[]{2.393977748534015, 1.8911898810178762, 2.1536440672214474, 2.14164226562326, 2.120140756328995, 1.9860912488520033, 1.8242727038465032, 1.6192571801084923, 1.4783543879452474, 1.3020095325591332, 1.1333979930704936, 0.9458464007979065, 0.7355344116746965, 0.5235501565244947, 0.23250859931576462, 0.0});
    models.put("Snoring C", new Double[]{2.1729494106001117, 2.227784877426436, 2.3215575694653223, 2.1938315925902594, 2.0859498238378804, 2.003189274433817, 1.8296980915245047, 1.7528328469846792, 1.612624855569739, 1.387439588839096, 1.2325109145560678, 1.0182058500278985, 0.8412412506396383, 0.6042994063906901, 0.2975946938710059, 0.0});
    models.put("Speech", new Double[]{2.7791172372702473, 2.48487264756004, 2.690103160039592, 2.692075192536201, 2.4110395865916163, 2.3178733588220237, 2.1676329206377343, 1.9158389821274129, 1.9017280409063637, 1.7374681720237821, 1.361126413183254, 1.1858004748366227, 0.9361014561926182, 0.6441385952685407, 0.34351580458998954, 0.0});
    models.put("Noise(white)", new Double[]{2.3603018381463263, 2.7118676626371965, 2.6148375434570923, 2.521471430218959, 2.4572883320046124, 2.3710001473633935, 2.3141975482606805, 1.9719304783472642, 1.8892154827033314, 1.6855065070690596, 1.4675363310369829, 1.1975488339047173, 0.9794405109351182, 0.6350795618671664, 0.36751744428519706, 0.0});

  }

  public Map<String, Double[]> getModels () { return models; }

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
