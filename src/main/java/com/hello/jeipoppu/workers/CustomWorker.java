package com.hello.jeipoppu.workers;

import com.google.common.collect.Lists;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;
import com.amazonaws.services.kinesis.model.Record;
import com.hello.suripu.api.audio.MatrixProtos.MatrixClientMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.List;

public class CustomWorker implements Runnable {

  private static final Log LOG = LogFactory.getLog(CustomWorker.class);
  private final String applicationName;
  private final IRecordProcessorFactory recordProcessorFactory;
  private volatile boolean shutdown;
  private IRecordProcessor recordProcessor;
  private final long idleTimeInMilliseconds;

  public CustomWorker (final IRecordProcessorFactory recordProcessorFactory,
                       final KinesisClientLibConfiguration config) {
    this.applicationName = config.getApplicationName();
    this.recordProcessorFactory = recordProcessorFactory;
    this.idleTimeInMilliseconds = config.getIdleTimeBetweenReadsInMillis();
  }

  public String getApplicationName() {
    return applicationName;
  }

  public void run() {
    try {
            initialize();
            LOG.info("Initialization complete. Starting worker loop.");
        } catch (RuntimeException e1) {
            LOG.error("Unable to initialize. Shutting down.", e1);
            shutdown();
        }

        while (!shutdown) {
          //Pull records
          // fetch serialized list of Records from disk
//          List<String> featureSets = Lists.newArrayList(
//              "features_office_a_1290",
//              "features_office_a_1291",
//              "features_office_a_1292",
//              "features_office_a_1293"
//          );
          //Speech
//          List<String> featureSets = Lists.newArrayList(
//              "features_counting_a_1296",
//              "features_counting_a_1298",
//              "features_counting_a_1299"
//          );

          //Only Snores
//          List<String> featureSets = Lists.newArrayList(
//              "features_onlysnoring_1249",
//              "features_onlysnoring_1250",
//              "features_onlysnoring_1251",
//              "features_onlysnoring_1252",
//              "features_onlysnoring_1253"
//          );

          //Snore C
//          List<String> featureSets = Lists.newArrayList(
//              "features_snoring_c_1351",
//              "features_snoring_c_1352",
//              "features_snoring_c_1353",
//              "features_snoring_c_1354",
//              "features_snoring_c_1355",
//              "features_snoring_c_1356"
//          );


          //Music (Bing Crosby - White Christmas)
//          List<String> featureSets = Lists.newArrayList(
//              "features_music_a_1397",
//              "features_music_a_1398",
//              "features_music_a_1399"
//          );
          //Noise (white)
//          List<String> featureSets = Lists.newArrayList(
//              "features_noise_1408",
//              "features_noise_1409"
//          );

          //Test (Snore, Noise, quiet snore, speech)
          List<String> featureSets = Lists.newArrayList(
              "features_test_306",
              "features_test_307",
              "features_test_308"
          );


//          List<String> featureSets = Lists.newArrayList(
//              "features_snoring_a_1256",
//              "features_snoring_a_1257",
//              "features_snoring_a_1258",
//              "features_snoring_a_1259",
//              "features_snoring_a_1260",
//              "features_snoring_a_1261",
//              "features_snoring_a_1262",
//              "features_snoring_a_1263",
//              "features_snoring_b_1265",
//              "features_snoring_b_1266",
//              "features_snoring_b_1267",
//              "features_snoring_b_1268",
//              "features_snoring_b_1269",
//              "features_snoring_b_1270",
//              "features_snoring_b_1271",
//              "features_snoring_b_1272",
//              "features_snoring_b_1273"
//              );

          for (final String featureSet : featureSets) {
            //LOG.debug(featureSet);

            final String fileName = "/Users/jnorgan/HelloCode/scripts/data/audio_features/" + featureSet;

            List<Record> records = Lists.newArrayList();
            List<MatrixClientMessage> clientMsgs = Lists.newArrayList();
            try {
              FileInputStream fileIn = new FileInputStream(fileName);
              ObjectInputStream in = new ObjectInputStream(fileIn);
              clientMsgs = (List<MatrixClientMessage>) in.readObject();

              //System.out.println("Deserialized Data: \n" + clientMsg.toString());
              in.close();
              fileIn.close();
            } catch (FileNotFoundException e) {
              e.printStackTrace();
            } catch (IOException e) {
              e.printStackTrace();
            } catch (Exception e) {
              e.printStackTrace();
            }

            final Boolean shouldWriteRecords = false;

            if (shouldWriteRecords) {
              try {
                java.io.FileOutputStream fileOut = new java.io.FileOutputStream(fileName);
                ObjectOutputStream out = new ObjectOutputStream(fileOut);
                //out.writeDelimitedTo(nonEmptyRecords);
                for (final MatrixClientMessage msg : clientMsgs) {
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
            }

            //Stuff MatrixClientMessages into a Record list
            for(final MatrixClientMessage msg : clientMsgs) {
              Record newRecord = new Record().withData(ByteBuffer.wrap(msg.toByteArray()));
              records.add(newRecord);
            }

            FakeRecordProcessorCheckpointer checkpointer = new FakeRecordProcessorCheckpointer();

            try {
              recordProcessor.processRecords(records, checkpointer);
              Thread.sleep(idleTimeInMilliseconds);
            } catch (Exception e) {
              LOG.error("ShardId: Application processRecords() threw an exception when processing shard ", e);
            }
          }

          shutdown();

        }

  }

  private void initialize() {
    this.recordProcessor = recordProcessorFactory.createProcessor();
  }

  public void shutdown() {
    this.shutdown = true;
  }


  public static class Builder {

    private IRecordProcessorFactory recordProcessorFactory;
    private KinesisClientLibConfiguration config;

    public Builder() {
    }

    public Builder config(final KinesisClientLibConfiguration config) {
      this.config = config;
      return this;
    }

    public Builder recordProcessorFactory(IRecordProcessorFactory recordProcessorFactory) {
      this.recordProcessorFactory = recordProcessorFactory;
      return this;
    }

    public CustomWorker build() {
      if (config == null) {
        throw new IllegalArgumentException(
            "A kinesis client config needs to be provided to build Worker");
      }

      if (recordProcessorFactory == null) {
        throw new IllegalArgumentException(
            "A Record Processor Factory needs to be provided to build Worker");
      }

      return new CustomWorker(recordProcessorFactory, config);
    }

  }
}
