package com.hello.jeipoppu.processors;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.codahale.metrics.MetricRegistry;


public class AudioFeaturesProcessorFactory implements IRecordProcessorFactory {

    private final MetricRegistry metricRegistry;

    public AudioFeaturesProcessorFactory(final String streamName, final MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

    public IRecordProcessor createProcessor() {
        return new AudioFeaturesProcessor(metricRegistry);
    }
}
