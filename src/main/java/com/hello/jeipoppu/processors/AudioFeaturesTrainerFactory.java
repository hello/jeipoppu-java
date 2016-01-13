package com.hello.jeipoppu.processors;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.codahale.metrics.MetricRegistry;


public class AudioFeaturesTrainerFactory implements IRecordProcessorFactory {

    private final String streamName;
    private final MetricRegistry metricRegistry;

    public AudioFeaturesTrainerFactory(final String streamName, final MetricRegistry metricRegistry) {
        this.streamName = streamName;
        this.metricRegistry = metricRegistry;
    }

    public IRecordProcessor createProcessor() {
        return new AudioFeaturesTrainer(metricRegistry);
    }
}
