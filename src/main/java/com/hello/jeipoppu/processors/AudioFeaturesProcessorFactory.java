package com.hello.jeipoppu.processors;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.codahale.metrics.MetricRegistry;
import com.hello.suripu.core.logging.DataLogger;


public class AudioFeaturesProcessorFactory implements IRecordProcessorFactory {

    private final MetricRegistry metricRegistry;
    private final DataLogger productsLogger;

    public AudioFeaturesProcessorFactory(final String streamName, final DataLogger productsLogger, MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
        this.productsLogger = productsLogger;
    }

    public IRecordProcessor createProcessor() {
        return new AudioFeaturesProcessor(productsLogger, metricRegistry);
    }
}
