package com.hello.jeipoppu.processors;

import com.amazonaws.services.kinesis.clientlibrary.exceptions.InvalidStateException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ShutdownException;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason;
import com.amazonaws.services.kinesis.model.Record;
import com.amazonaws.util.StringUtils;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.protobuf.InvalidProtocolBufferException;

import com.hello.suripu.api.audio.MatrixProtos;
import com.hello.jeipoppu.classifiers.BasicClassifier;
import com.hello.jeipoppu.models.Classification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;

import static com.codahale.metrics.MetricRegistry.name;

public class AudioFeaturesProcessor implements IRecordProcessor {

    private final MetricRegistry metrics;
    private final static Logger LOGGER = LoggerFactory.getLogger(AudioFeaturesProcessor.class);


    private final Meter messagesProcessed;
    private Long lastFilterTimestamp;
    private String shardId = "No Lease Key";


    public AudioFeaturesProcessor(final MetricRegistry metricRegistry){
        this.metrics= metricRegistry;
        messagesProcessed = metrics.meter(name(AudioFeaturesProcessor.class, "messages-processed"));
    }

    public void initialize(String shardId) {
        this.shardId = shardId;
        }


    public void processRecords(List<Record> records, IRecordProcessorCheckpointer iRecordProcessorCheckpointer) {


        for(final Record record : records) {

            final String sequenceNumber = record.getSequenceNumber();
            MatrixProtos.MatrixClientMessage matrixClientMessage = MatrixProtos.MatrixClientMessage.getDefaultInstance();

            try {
              matrixClientMessage =  MatrixProtos.MatrixClientMessage.parseFrom(record.getData().array());
            } catch (InvalidProtocolBufferException e) {
                LOGGER.error("Failed parsing protobuf: {}", e.getMessage());
                continue;
            }

          final BasicClassifier classifier = new BasicClassifier(matrixClientMessage);

          if (classifier.isEmpty()) {
            continue;
          }

          final Classification classification = classifier.run();

          LOGGER.debug("Classification: ", classification.toString());
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
