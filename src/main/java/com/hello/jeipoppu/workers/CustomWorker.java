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

/**
 * This custom worker loads in a number of locally-stored serialized protobufs, stuffs them into
 * a kinesis Record object and feeds them to a record processor of some sort. This is useful for
 * creating training / validation data sets.
 */
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
          List<String> featureSets = Lists.newArrayList();

//          //Office Noise
//          featureSets.add("features_office_a_1290");
//          featureSets.add("features_office_a_1291");
//          featureSets.add("features_office_a_1292");
//          featureSets.add("features_office_a_1293");
//
//          //Speech
//          featureSets.add("features_counting_a_1296");
//          featureSets.add("features_counting_a_1298");
//          featureSets.add("features_counting_a_1299");
//
//          //Only Snores
//          featureSets.add("features_onlysnoring_1249");
//          featureSets.add("features_onlysnoring_1250");
//          featureSets.add("features_onlysnoring_1251");
//          featureSets.add("features_onlysnoring_1252");
//          featureSets.add("features_onlysnoring_1253");
//
//          //Snore C
//          featureSets.add("features_snoring_c_1351");
//          featureSets.add("features_snoring_c_1352");
//          featureSets.add("features_snoring_c_1353");
//          featureSets.add("features_snoring_c_1354");
//          featureSets.add("features_snoring_c_1355");
//          featureSets.add("features_snoring_c_1356");
//
//          //Music (Bing Crosby - White Christmas)
//          featureSets.add("features_music_a_1397");
//          featureSets.add("features_music_a_1398");
//          featureSets.add("features_music_a_1399");
//
//          //Noise (white)
//          featureSets.add("features_noise_1408");
//          featureSets.add("features_noise_1409");

          //Test (Snore, Noise, quiet snore, speech)
          featureSets.add("features_test_306");
          featureSets.add("features_test_307");
          featureSets.add("features_test_308");

//          //Snoring A
//          featureSets.add("features_snoring_a_1256");
//          featureSets.add("features_snoring_a_1257");
//          featureSets.add("features_snoring_a_1258");
//          featureSets.add("features_snoring_a_1259");
//          featureSets.add("features_snoring_a_1260");
//          featureSets.add("features_snoring_a_1261");
//          featureSets.add("features_snoring_a_1262");
//          featureSets.add("features_snoring_a_1263");
//
//          //Snoring B
//          featureSets.add("features_snoring_b_1265");
//          featureSets.add("features_snoring_b_1266");
//          featureSets.add("features_snoring_b_1267");
//          featureSets.add("features_snoring_b_1268");
//          featureSets.add("features_snoring_b_1269");
//          featureSets.add("features_snoring_b_1270");
//          featureSets.add("features_snoring_b_1271");
//          featureSets.add("features_snoring_b_1272");
//          featureSets.add("features_snoring_b_1273");

          for (final String featureSet : featureSets) {
            LOG.debug(featureSet);

            final String fileName = "/Users/jnorgan/HelloCode/scripts/data/audio_features/" + featureSet + "_pb";

            List<Record> records = Lists.newArrayList();
            List<MatrixClientMessage> clientMsgs = Lists.newArrayList();
            try {
              FileInputStream fileIn = new FileInputStream(fileName);
              ObjectInputStream in = new ObjectInputStream(fileIn);
              MatrixClientMessage msg;
              while ((msg = MatrixClientMessage.parseDelimitedFrom(in)) != null) {
                clientMsgs.add(msg);
              }
              //clientMsgs = (List<MatrixClientMessage>) in.readObject();

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
                java.io.FileOutputStream fileOut = new java.io.FileOutputStream(fileName + "_pb");
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
