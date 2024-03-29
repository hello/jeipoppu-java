package com.hello.jeipoppu.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import io.dropwizard.Configuration;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * Created by jnorgan on 6/29/15.
 */
public class JeiPoppuConfiguration extends Configuration {
    @Valid
    @NotNull
    @JsonProperty("metrics_enabled")
    private Boolean metricsEnabled;

    public Boolean getMetricsEnabled() {
        return metricsEnabled;
    }

    @Valid
    @NotNull
    @JsonProperty("app_names")
    private Map<String, String> appNames;

    public ImmutableMap<String, String> getAppNames() {
        return ImmutableMap.copyOf(appNames);
    }

    @Valid
    @JsonProperty("debug")
    private Boolean debug = Boolean.FALSE;

    public Boolean getDebug() { return debug; }


    @Valid
    @NotNull
    @JsonProperty("kinesis")
    private KinesisConfiguration kinesisConfiguration;

    public Map<String, String> getKinesisEndpoints() {
        return kinesisConfiguration.getEndpoints();
    }

    public Map<String, String> getKinesisStreams() {
        return kinesisConfiguration.getStreams();
    }


    @Valid
    @NotNull
    @Max(2500)
    @JsonProperty("max_records")
    private Integer maxRecords;

    public Integer getMaxRecords() {
        return maxRecords;
    }

    @Valid
    @NotNull
    @JsonProperty("graphite")
    private GraphiteConfiguration graphite;

    public GraphiteConfiguration getGraphite() {
        return graphite;
    }
}
