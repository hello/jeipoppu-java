package com.hello.jeipoppu.processors;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.protobuf.InvalidProtocolBufferException;

import com.amazonaws.services.kinesis.clientlibrary.exceptions.InvalidStateException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ShutdownException;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason;
import com.amazonaws.services.kinesis.model.Record;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.hello.jeipoppu.algorithms.StdDevAlgorithm;
import com.hello.jeipoppu.classifiers.BasicClassifier;
import com.hello.jeipoppu.classifiers.WindowClassifier;
import com.hello.jeipoppu.models.Classification;
import com.hello.suripu.api.audio.MatrixProtos.MatrixClientMessage;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import static com.codahale.metrics.MetricRegistry.name;

public class AudioFeaturesProcessor implements IRecordProcessor {

    private final MetricRegistry metrics;
    private final static Logger LOGGER = LoggerFactory.getLogger(AudioFeaturesProcessor.class);


    private final Meter messagesProcessed;
    private Long lastFilterTimestamp;
    private String shardId = "No Lease Key";
    private String lastWriteMinute = "";
    private List<MatrixClientMessage> nonEmptyRecords;
    private StringBuilder matrixBuilder;


    public AudioFeaturesProcessor(final MetricRegistry metricRegistry){
        this.metrics= metricRegistry;
        messagesProcessed = metrics.meter(name(AudioFeaturesProcessor.class, "messages-processed"));
        nonEmptyRecords = Lists.newArrayList();
        matrixBuilder = new StringBuilder();
    }

    public void initialize(String shardId) {
        this.shardId = shardId;
        }


    public void processRecords(List<Record> records, IRecordProcessorCheckpointer iRecordProcessorCheckpointer) {


        for(final Record record : records) {

            final String sequenceNumber = record.getSequenceNumber();
            MatrixClientMessage matrixClientMessage = MatrixClientMessage.getDefaultInstance();

            try {
              matrixClientMessage =  MatrixClientMessage.parseFrom(record.getData().array());
            } catch (InvalidProtocolBufferException e) {
                LOGGER.error("Failed parsing protobuf: {}", e.getMessage());
                continue;
            }

          final WindowClassifier classifier = new WindowClassifier(new StdDevAlgorithm(), 32);
          //final BasicClassifier classifier = new BasicClassifier(new StdDevAlgorithm("model_stddev"));

//          if (classifier.isEmpty()) {
//            continue;
//          }

// Home unit: 89BE44968F02BCB0
// CANARY 8:  774BDC5C040473F9

          //LOGGER.debug(matrixClientMessage.getDeviceId());
          //final long unixSeconds = matrixClientMessage.getUnixTime() - 313; //Realignment for Test Data set
          final long unixSeconds = matrixClientMessage.getUnixTime(); //Realignment for Test Data set
          final Date date = new Date(unixSeconds*1000L);
          final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
          sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
          String formattedDate = sdf.format(date);

//          if (!matrixClientMessage.getDeviceId().equals("774BDC5C040473F9")) {
//            continue;
//          }

           nonEmptyRecords.add(matrixClientMessage);

          final List<Classification> classifications = classifier.run(matrixClientMessage);
          //matrixBuilder.append(classifier.toOctaveMatrixString(classifier.featureVectors));

          LinkedHashMap<String, Integer> classCounts = Maps.newLinkedHashMap();
          for(final Classification classification : classifications) {
            if (!classCounts.containsKey(classification.name)) {
              classCounts.put(classification.name, 1);
            } else {
              classCounts.put(classification.name, classCounts.get(classification.name) + 1);
            }
          }
          classCounts = WindowClassifier.sortByValue(classCounts);

          if (classCounts.size() < 1) {
            continue;
          }
          //LOGGER.debug("Classification: {}", classCounts.toString());

          final String deviceId = matrixClientMessage.getDeviceId();
          final Integer featureCount = classifier.getFeatureCount();

          final String winningClassification = Lists.newArrayList(classCounts.keySet()).get(classCounts.size()-1);
          final Integer winningValue = Lists.newArrayList(classCounts.values()).get(classCounts.size()-1);
          //Basically, what percentage of the total feature vectors for this upload where classified as the winner
          final Float winnerPercentage = (float)winningValue / (float)featureCount;

          //System.out.println(classCounts.toString());

          System.out.print(deviceId + ", ");
          System.out.print(formattedDate + ", ");
          System.out.print(winningClassification + ", ");
          System.out.print(winningValue + ", ");
          System.out.print(featureCount.toString() + ", ");
          System.out.printf("%.3f\n", winnerPercentage);

        }

      //Serialize nonEmptyRecords for future use
     // LOGGER.debug("Non-empty records: {}", nonEmptyRecords.size());

      final Boolean shouldWriteRecords = false;
      final String minuteOfDay = Integer.toString(DateTime.now().minuteOfDay().get());
      if (!minuteOfDay.equals(lastWriteMinute)) {
        lastWriteMinute = minuteOfDay;
        final String fileName = "/Users/jnorgan/HelloCode/scripts/data/audio_features/features_test_" + minuteOfDay;

        if (shouldWriteRecords) {
          try {
            java.io.FileOutputStream fileOut = new java.io.FileOutputStream(fileName);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(nonEmptyRecords);
            out.close();
            fileOut.close();
            System.out.println("\nSerialization Successful... Checkout your specified output file..\n");

          } catch (java.io.FileNotFoundException e) {
            e.printStackTrace();
          } catch (java.io.IOException e) {
            e.printStackTrace();
          }


          List<MatrixClientMessage> clientMsg = Lists.newArrayList();
          try {
            java.io.FileInputStream fileIn = new FileInputStream(fileName);
            java.io.ObjectInputStream in = new java.io.ObjectInputStream(fileIn);
            clientMsg = (List<MatrixClientMessage>) in.readObject();
            //System.out.println("Deserialized Data: \n" + clientMsg.toString());
            in.close();
            fileIn.close();
          } catch (java.io.FileNotFoundException e) {
            e.printStackTrace();
          } catch (IOException e) {
            e.printStackTrace();
          } catch (Exception e) {
            e.printStackTrace();
          }


          LOGGER.debug("Deserialized record count: {}", clientMsg.size());
        }

        nonEmptyRecords = Lists.newArrayList();
      }




      //Batch dump classifier products to another kinesis stream
      //OR
      //Write directly to DynamoDB / something else


        try {
            iRecordProcessorCheckpointer.checkpoint();
        } catch (InvalidStateException e) {
            LOGGER.error("checkpoint {}", e.getMessage());
        } catch (ShutdownException e) {
            LOGGER.error("Received shutdown command at checkpoint, bailing. {}", e.getMessage());
        }

      final String octaveMatrix = matrixBuilder.toString();
      //LOGGER.debug("Matrix: t = [{}]", octaveMatrix);
    }

    public void shutdown(IRecordProcessorCheckpointer iRecordProcessorCheckpointer, ShutdownReason shutdownReason) {
    }
}
