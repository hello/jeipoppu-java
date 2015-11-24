package com.hello.jeipoppu.processors;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.codahale.metrics.MetricRegistry;

/**
 * Created by jnorgan on 6/29/15.
 */
public class AudioFeaturesProcessorFactory implements IRecordProcessorFactory {

    private final String streamName;
    private final MetricRegistry metricRegistry;

    public AudioFeaturesProcessorFactory(final String streamName, final MetricRegistry metricRegistry) {
        this.streamName = streamName;
        this.metricRegistry = metricRegistry;
    }

    public IRecordProcessor createProcessor() {
        return new AudioFeaturesProcessor(metricRegistry);
    }
}
