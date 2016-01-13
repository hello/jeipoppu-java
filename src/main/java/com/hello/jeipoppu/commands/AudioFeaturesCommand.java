package com.hello.jeipoppu.commands;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import com.hello.jeipoppu.configuration.JeiPoppuConfiguration;
import com.hello.jeipoppu.framework.AudioFeaturesEnvironmentCommand;
import com.hello.jeipoppu.processors.AudioFeaturesProcessorFactory;
import com.hello.jeipoppu.workers.CustomWorker;

import net.sourceforge.argparse4j.inf.Namespace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import io.dropwizard.setup.Environment;

/**
 * Created by jnorgan on 6/29/15.
 */
public class AudioFeaturesCommand extends AudioFeaturesEnvironmentCommand<JeiPoppuConfiguration> {
    private final static String COMMAND_APP_NAME = "audio_features_processor";
    private final static String COMMAND_STREAM_NAME = "audio_features";
    private final static Logger LOGGER = LoggerFactory.getLogger(AudioFeaturesCommand.class);
    private final static ClientConfiguration DEFAULT_CLIENT_CONFIGURATION = new ClientConfiguration().withConnectionTimeout(200).withMaxErrorRetry(1);

    public AudioFeaturesCommand(final String name, final String description) {
        super(name, description);
    }

    @Override
    protected void run(Environment environment, Namespace namespace, JeiPoppuConfiguration configuration) throws Exception {

        if(configuration.getMetricsEnabled()) {
            final String graphiteHostName = configuration.getGraphite().getHost();
            final String apiKey = configuration.getGraphite().getApiKey();
            final Integer interval = configuration.getGraphite().getReportingIntervalInSeconds();

            final String env = (configuration.getDebug()) ? "dev" : "prod";
            final String prefix = String.format("%s.%s.jeipoppu", apiKey, env);

            final Graphite graphite = new Graphite(new InetSocketAddress(graphiteHostName, 2003));

            final GraphiteReporter reporter = GraphiteReporter.forRegistry(environment.metrics())
                    .prefixedWith(prefix)
                    .convertRatesTo(TimeUnit.SECONDS)
                    .convertDurationsTo(TimeUnit.MILLISECONDS)
                    .filter(MetricFilter.ALL)
                    .build(graphite);
            reporter.start(interval, TimeUnit.SECONDS);

            LOGGER.info("Metrics enabled.");
        } else {
            LOGGER.warn("Metrics not enabled.");
        }

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

        final IRecordProcessorFactory processorFactory = new AudioFeaturesProcessorFactory(
                configuration.getKinesisStreams().get(COMMAND_STREAM_NAME),
                environment.metrics()
        );

        final Worker kinesisWorker = new Worker(processorFactory, kinesisConfig);
        kinesisWorker.run();
        final CustomWorker fileWorker = new CustomWorker(processorFactory, kinesisConfig);
        //fileWorker.run();
    }
}
