package com.hello.jeipoppu.processors;

import com.amazonaws.services.kinesis.clientlibrary.exceptions.InvalidStateException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ShutdownException;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason;
import com.amazonaws.services.kinesis.model.Record;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.google.protobuf.InvalidProtocolBufferException;

import com.hello.suripu.api.audio.MatrixProtos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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


          //Perform classification here
          final MatrixProtos.Matrix matrixPayload = matrixClientMessage.getMatrixPayload();
          if (!(matrixClientMessage.getMatrixListCount() > 0)) {
            continue;
          }
          final MatrixProtos.Matrix matrix = matrixClientMessage.getMatrixList(0);
          LOGGER.debug("Matrix processed for: {} with {}", matrixClientMessage.getDeviceId(), matrix.getId());
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
