package com.hello.jeipoppu;

import com.hello.jeipoppu.configuration.JeiPoppuConfiguration;
import com.hello.jeipoppu.processors.AudioFeaturesCommand;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.joda.time.DateTimeZone;

import java.util.TimeZone;

/**
 * A dropwizard application that essentially mimics the same/similar structure as the
 * Suripu workers.
 *
 */
public class JeiPoppu extends Application<JeiPoppuConfiguration>
{

    public static void main( String[] args ) throws Exception
    {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        DateTimeZone.setDefault(DateTimeZone.UTC);
        new JeiPoppu().run(args);
    }

    @Override
    public String getName() {
        return "jeipoppu";
    }

    @Override
    public void initialize(Bootstrap<JeiPoppuConfiguration> bootstrap) {
        bootstrap.addCommand(new AudioFeaturesCommand("audio_features", "Analyzing incoming audio features."));
    }

    @Override
    public void run(JeiPoppuConfiguration configuration,
                    Environment environment) {
        // DO NOTHING HERE
    }
}
