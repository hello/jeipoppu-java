package com.hello.jeipoppu.commands;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;
import com.hello.jeipoppu.configuration.JeiPoppuConfiguration;
import com.hello.jeipoppu.framework.AudioFeaturesEnvironmentCommand;
import com.hello.jeipoppu.processors.AudioFeaturesTrainerFactory;
import com.hello.jeipoppu.workers.CustomWorker;

import net.sourceforge.argparse4j.inf.Namespace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;

import io.dropwizard.setup.Environment;

/**
 * Created by jnorgan on 6/29/15.
 */
public class TrainAudioFeaturesCommand extends AudioFeaturesEnvironmentCommand<JeiPoppuConfiguration> {
    private final static String COMMAND_APP_NAME = "audio_features_processor";
    private final static String COMMAND_STREAM_NAME = "audio_features";
    private final static Logger LOGGER = LoggerFactory.getLogger(TrainAudioFeaturesCommand.class);
    private final static ClientConfiguration DEFAULT_CLIENT_CONFIGURATION = new ClientConfiguration().withConnectionTimeout(200).withMaxErrorRetry(1);

    public TrainAudioFeaturesCommand(final String name, final String description) {
        super(name, description);
    }

    @Override
    protected void run(Environment environment, Namespace namespace, JeiPoppuConfiguration configuration) throws Exception {

        final AWSCredentialsProvider awsCredentialsProvider = new DefaultAWSCredentialsProviderChain();

        final String workerId = InetAddress.getLocalHost().getCanonicalHostName();
        final KinesisClientLibConfiguration kinesisConfig = new KinesisClientLibConfiguration(
                configuration.getAppNames().get(COMMAND_APP_NAME),
                configuration.getKinesisStreams().get(COMMAND_STREAM_NAME),
                awsCredentialsProvider,
                workerId);
        kinesisConfig.withInitialPositionInStream(InitialPositionInStream.LATEST);
        kinesisConfig.withMaxRecords(configuration.getMaxRecords());
        kinesisConfig.withKinesisEndpoint(configuration.getKinesisEndpoints().get(COMMAND_STREAM_NAME));
//        kinesisConfig.withIdleTimeBetweenReadsInMillis(10000);

        final IRecordProcessorFactory trainerFactory = new AudioFeaturesTrainerFactory(
                configuration.getKinesisStreams().get(COMMAND_STREAM_NAME),
                environment.metrics()
        );


        //Run a worker that reads in all test data, runs training algorithm, & creates and stores a model.
        final CustomWorker fileWorker = new CustomWorker(trainerFactory, kinesisConfig);
        fileWorker.run();
    }
}
