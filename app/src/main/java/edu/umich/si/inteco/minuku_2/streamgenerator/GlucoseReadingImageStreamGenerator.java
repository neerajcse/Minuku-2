/*
 * Copyright (c) 2016.
 *
 * DReflect and Minuku Libraries by Shriti Raj (shritir@umich.edu) and Neeraj Kumar(neerajk@uci.edu) is licensed under a Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International License.
 * Based on a work at https://github.com/Shriti-UCI/Minuku-2.
 *
 *
 * You are free to (only if you meet the terms mentioned below) :
 *
 * Share — copy and redistribute the material in any medium or format
 * Adapt — remix, transform, and build upon the material
 *
 * The licensor cannot revoke these freedoms as long as you follow the license terms.
 *
 * Under the following terms:
 *
 * Attribution — You must give appropriate credit, provide a link to the license, and indicate if changes were made. You may do so in any reasonable manner, but not in any way that suggests the licensor endorses you or your use.
 * NonCommercial — You may not use the material for commercial purposes.
 * ShareAlike — If you remix, transform, or build upon the material, you must distribute your contributions under the same license as the original.
 * No additional restrictions — You may not apply legal terms or technological measures that legally restrict others from doing anything the license permits.
 */

package edu.umich.si.inteco.minuku_2.streamgenerator;

import android.content.Context;

import edu.umich.si.inteco.minuku.config.Constants;
import edu.umich.si.inteco.minuku.config.UserPreferences;
import edu.umich.si.inteco.minuku.logger.Log;
import edu.umich.si.inteco.minuku.manager.MinukuStreamManager;
import edu.umich.si.inteco.minuku.streamgenerator.AnnotatedImageStreamGenerator;
import edu.umich.si.inteco.minuku_2.model.GlucoseReadingImage;
import edu.umich.si.inteco.minukucore.event.NoDataChangeEvent;

/**
 * Created by shriti on 7/31/16.
 */
public class GlucoseReadingImageStreamGenerator extends
        AnnotatedImageStreamGenerator<GlucoseReadingImage> {


    public GlucoseReadingImageStreamGenerator(Context applicationContext) {
        super(applicationContext, GlucoseReadingImage.class);
        TAG = "GlucoseReadingImageStreamGenerator";
    }

    @Override
    public boolean updateStream() {
        Log.d(TAG,
                "Update Stream called: The preference values are - \n" +
                        UserPreferences.getInstance().getPreferenceSet("gmTimes") + "\n" +
                        UserPreferences.getInstance().getPreference("endTime"));
        MinukuStreamManager.getInstance().handleNoDataChangeEvent(
                new NoDataChangeEvent(GlucoseReadingImage.class));
        return true;
    }

    @Override
    public long getUpdateFrequency() {
        return Constants.IMAGE_STREAM_GENERATOR_UPDATE_FREQUENCY_MINUTES;
    }
}
