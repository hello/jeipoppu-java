package com.hello.jeipoppu.processors;

import com.google.common.collect.Lists;
import com.google.protobuf.InvalidProtocolBufferException;

import com.amazonaws.services.kinesis.clientlibrary.exceptions.InvalidStateException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ShutdownException;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason;
import com.amazonaws.services.kinesis.model.Record;
import com.codahale.metrics.MetricRegistry;
import com.hello.jeipoppu.algorithms.StdDevAlgorithm;
import com.hello.jeipoppu.classifiers.BasicClassifier;
import com.hello.jeipoppu.classifiers.Classifier;
import com.hello.jeipoppu.classifiers.WindowClassifier;
import com.hello.jeipoppu.models.Classification;
import com.hello.suripu.api.audio.MatrixProtos.MatrixClientMessage;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;

public class AudioFeaturesTrainer implements IRecordProcessor {

    private final static Logger LOGGER = LoggerFactory.getLogger(AudioFeaturesTrainer.class);

    private Long lastFilterTimestamp;
    private String shardId = "No Lease Key";
    private Classifier classifier;
    private List<Double[]> listOfProcessed;


    public AudioFeaturesTrainer(final MetricRegistry metricRegistry){
      classifier = new WindowClassifier(new StdDevAlgorithm("model_window_stddev"), 3);
      listOfProcessed = Lists.newArrayList();
    }

    public void initialize(String shardId) {
        this.shardId = shardId;
        }


    public void processRecords(List<Record> records, IRecordProcessorCheckpointer iRecordProcessorCheckpointer) {

      List<MatrixClientMessage> nonEmptyRecords = Lists.newArrayList();

        for(final Record record : records) {

            final String sequenceNumber = record.getSequenceNumber();
            MatrixClientMessage matrixClientMessage = MatrixClientMessage.getDefaultInstance();

            try {
              matrixClientMessage =  MatrixClientMessage.parseFrom(record.getData().array());
            } catch (InvalidProtocolBufferException e) {
                LOGGER.error("Failed parsing protobuf: {}", e.getMessage());
                continue;
            }

//          if (classifier.isEmpty()) {
//            continue;
//          }
            final List<Classification> classifications = classifier.run(matrixClientMessage);

            listOfProcessed.add(classifier.getProcessedFeatures());
            Double[] aggregateValues = classifier.getAlgorithm().computeAggregate(listOfProcessed);
          LOGGER.debug("Aggregate Values: {}", java.util.Arrays.deepToString(aggregateValues));
          //LOGGER.debug("Classification: {}", classification.name);

        }

      //Perform aggregate analysis on records


      //Store in model

      //Serialize nonEmptyRecords for future use
      LOGGER.debug("Non-empty records: {}", nonEmptyRecords.size());

      final Boolean shouldWriteRecords = false;
      final String fileName = "/Users/jnorgan/HelloCode/scripts/data/audio_features_" + Integer.toString(DateTime.now().minuteOfDay().get());

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
        } catch (IOException e) {
          e.printStackTrace();
        }


        List<MatrixClientMessage> clientMsg = Lists.newArrayList();
        try {
          FileInputStream fileIn = new FileInputStream(fileName);
          java.io.ObjectInputStream in = new java.io.ObjectInputStream(fileIn);
          clientMsg = (List<MatrixClientMessage>) in.readObject();
          System.out.println("Deserialized Data: \n" + clientMsg.toString());
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

    }

    public void shutdown(IRecordProcessorCheckpointer iRecordProcessorCheckpointer, ShutdownReason shutdownReason) {

    }
}
