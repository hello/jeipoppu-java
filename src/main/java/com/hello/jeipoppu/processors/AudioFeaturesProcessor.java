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
import com.hello.suripu.api.audio.AudioClassificationProtos.audio_class_result;
import com.hello.suripu.api.audio.AudioClassificationProtos.audio_class_result.audio_class;
import com.hello.suripu.api.audio.AudioClassificationProtos.audio_classifcation_message;
import com.hello.suripu.api.audio.MatrixProtos.MatrixClientMessage;
import com.hello.suripu.core.logging.DataLogger;

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
  private final static Boolean SHOULD_WRITE_RECORDS = false;
  private final static String SERIALIZED_RECORD_PATH = "/Users/jnorgan/HelloCode/scripts/data/audio_features/";


    private final Meter messagesProcessed;
    private Long lastFilterTimestamp;
    private String shardId = "No Lease Key";
    private String lastWriteMinute = "";
    private List<MatrixClientMessage> nonEmptyRecords;
    private StringBuilder matrixBuilder;
    private DataLogger productsLogger;


    public AudioFeaturesProcessor(final DataLogger productsLogger, final MetricRegistry metricRegistry){
        this.metrics= metricRegistry;
        messagesProcessed = metrics.meter(name(AudioFeaturesProcessor.class, "messages-processed"));
        nonEmptyRecords = Lists.newArrayList();
        matrixBuilder = new StringBuilder();
        this.productsLogger = productsLogger;
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
          //final long unixSeconds = matrixClientMessage.getUnixTime() - 313; //Realignment for Test Data set 1
          //final long unixSeconds = matrixClientMessage.getUnixTime() - 2702; //Realignment for Test Data set 2

          final long unixSeconds = matrixClientMessage.getUnixTime(); //Realignment for Test Data set
          final Date date = new Date(unixSeconds*1000L);
          final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
          sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
          String formattedDate = sdf.format(date);

//          if (!matrixClientMessage.getDeviceId().equals("89BE44968F02BCB0")) {
//            continue;
//          }

           nonEmptyRecords.add(matrixClientMessage);

          final List<Classification> classifications = classifier.run(matrixClientMessage);
          //matrixBuilder.append(classifier.toOctaveMatrixString(classifier.featureVectors));

          LinkedHashMap<audio_class, Integer> classCounts = Maps.newLinkedHashMap();
          for(final Classification classification : classifications) {
            if (!classCounts.containsKey(classification.type)) {
              classCounts.put(classification.type, 1);
            } else {
              classCounts.put(classification.type, classCounts.get(classification.type) + 1);
            }
          }
          classCounts = WindowClassifier.sortByValue(classCounts);

          if (classCounts.size() < 1) {
            continue;
          }
          //LOGGER.debug("Classification: {}", classCounts.toString());

          final String deviceId = matrixClientMessage.getDeviceId();
          final Integer featureCount = classifications.size();

          final audio_class winningClassification = Lists.newArrayList(classCounts.keySet()).get(classCounts.size()-1);
          final Integer winningValue = Lists.newArrayList(classCounts.values()).get(classCounts.size()-1);
          //Basically, what percentage of the total feature vectors for this upload where classified as the winner
          final Float winnerPercentage = (float)winningValue / (float)featureCount;

          //System.out.println(classCounts.toString());

          LOGGER.debug("{}, {}, {}, {}, {}, {}", deviceId, formattedDate, winningClassification, winningValue, featureCount.toString(), winnerPercentage);

          final audio_class_result classResult = audio_class_result.newBuilder()
              .addClasses(winningClassification)
              .addProbability(winnerPercentage)
              .build();
          final audio_classifcation_message classificationMsg = audio_classifcation_message.newBuilder()
              .setDeviceId(deviceId)
              .setUnixTime((int)unixSeconds)
              .setClasses(classResult)
              .build();

          //Store the audio_classification_message in Kinesis
          try {
            productsLogger.put(deviceId, classificationMsg.toByteArray());
          } catch (Exception e) {
            LOGGER.error("error=kinesis-insert-audio_products {}", e.getMessage());
          }
        }

      //Serialize nonEmptyRecords for future use
      final String minuteOfDay = Integer.toString(DateTime.now().minuteOfDay().get());
      if (!minuteOfDay.equals(lastWriteMinute)) {
        if (SHOULD_WRITE_RECORDS) {
          writeRecordsToFile(nonEmptyRecords, SERIALIZED_RECORD_PATH + "features_snoring_d_" + minuteOfDay + "_pb");
        }
        lastWriteMinute = minuteOfDay;
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

    private void writeRecordsToFile(final List<MatrixClientMessage> records, String filename) {
      try {
        java.io.FileOutputStream fileOut = new java.io.FileOutputStream(filename);
        ObjectOutputStream out = new ObjectOutputStream(fileOut);
        //out.writeObject(nonEmptyRecords);
        for (final MatrixClientMessage msg : records) {
          msg.writeDelimitedTo(out);
        }
        out.close();
        fileOut.close();
        System.out.println("\nSerialization Successful... Checkout your specified output file..\n");

      } catch (java.io.FileNotFoundException e) {
        e.printStackTrace();
      } catch (java.io.IOException e) {
        e.printStackTrace();
      }


      List<MatrixClientMessage> clientMsgs = Lists.newArrayList();
      try {
        java.io.FileInputStream fileIn = new FileInputStream(filename);
        java.io.ObjectInputStream in = new java.io.ObjectInputStream(fileIn);
        //clientMsg = (List<MatrixClientMessage>) in.readObject();
        MatrixClientMessage msg;
        while ((msg = MatrixClientMessage.parseDelimitedFrom(in)) != null) {
          clientMsgs.add(msg);
        }
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


      LOGGER.debug("Deserialized record count: {}", clientMsgs.size());

    }

    public void shutdown(IRecordProcessorCheckpointer iRecordProcessorCheckpointer, ShutdownReason shutdownReason) {
    }
}
