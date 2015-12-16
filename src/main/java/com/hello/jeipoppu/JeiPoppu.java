package com.hello.jeipoppu;

import com.hello.jeipoppu.commands.AudioFeaturesCommand;
import com.hello.jeipoppu.commands.TrainAudioFeaturesCommand;
import com.hello.jeipoppu.configuration.JeiPoppuConfiguration;

import org.joda.time.DateTimeZone;

import java.util.TimeZone;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

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
      bootstrap.addCommand(new AudioFeaturesCommand("audio_features", "Analyzing incoming audio featureVectors."));
      bootstrap.addCommand(new TrainAudioFeaturesCommand("train_features", "Train a model on test set of audio featureVectors."));
    }

    @Override
    public void run(JeiPoppuConfiguration configuration,
                    Environment environment) {
        // DO NOTHING HERE
    }
}
