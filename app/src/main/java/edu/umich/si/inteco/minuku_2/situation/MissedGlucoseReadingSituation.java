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

package edu.umich.si.inteco.minuku_2.situation;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.umich.si.inteco.minuku.config.Constants;
import edu.umich.si.inteco.minuku.config.UserPreferences;
import edu.umich.si.inteco.minuku.logger.Log;
import edu.umich.si.inteco.minuku.manager.MinukuSituationManager;
import edu.umich.si.inteco.minuku_2.event.MissedGlucoseReadingEvent;
import edu.umich.si.inteco.minuku_2.model.GlucoseReadingImage;
import edu.umich.si.inteco.minukucore.event.ActionEvent;
import edu.umich.si.inteco.minukucore.event.MinukuEvent;
import edu.umich.si.inteco.minukucore.event.NoDataChangeEvent;
import edu.umich.si.inteco.minukucore.exception.DataRecordTypeNotFound;
import edu.umich.si.inteco.minukucore.model.DataRecord;
import edu.umich.si.inteco.minukucore.model.StreamSnapshot;
import edu.umich.si.inteco.minukucore.situation.Situation;

/**
 * Created by shriti on 7/31/16.
 */
public class MissedGlucoseReadingSituation implements Situation {

    String TAG = "MissedGlucoseReadingSituation";

    public MissedGlucoseReadingSituation() {
        try {
            MinukuSituationManager.getInstance().register(this);
            Log.d(TAG, "Registered successfully.");
        } catch (DataRecordTypeNotFound dataRecordTypeNotFound) {
            Log.e(TAG, "Registration failed.");
            dataRecordTypeNotFound.printStackTrace();
        }
    }

    @Override
    public <T extends ActionEvent> T assertSituation(StreamSnapshot snapshot, MinukuEvent aMinukuEvent) {
        List<DataRecord> dataRecords = new ArrayList<>();
        dataRecords.add(snapshot.getCurrentValue(GlucoseReadingImage.class));
        if(aMinukuEvent instanceof NoDataChangeEvent) {
            Log.d(TAG, "MinukuEvent is instance of no data change event. Checking if I should check"+
                    "time passed from last image report to check for missing reports");
            if(checkLastImageReport(snapshot)) {
                Log.d(TAG, "Should show questionnaire if returned true. Sending ActionEvent.");
                return (T) new MissedGlucoseReadingEvent("MISSED_DATA_GLUCOSE_READING", dataRecords);
            }

        }
        return null;
    }

    @Override
    public List<Class<? extends DataRecord>> dependsOnDataRecordType() throws DataRecordTypeNotFound {
        List<Class<? extends  DataRecord>> dependsOn = new ArrayList<>();
        dependsOn.add(GlucoseReadingImage.class);
        return dependsOn;
    }

    private List<Integer> getTimesForCheckingLastReports() {
        Set<String> glucoseReadingTimes = UserPreferences.getInstance().getPreferenceSet("gmTimes");

        List<Integer> timesForNotification = new LinkedList<>();

        if(glucoseReadingTimes == null) {
            return timesForNotification;
        }
        Set<Integer> glucoseReadingTimesInSeconds = new HashSet<Integer>();

        for (String time:glucoseReadingTimes) {
            Log.d(TAG, "gm time: " + time);
            glucoseReadingTimesInSeconds.add(convertHHMMtoSeconds(time));
        }
        //int endTimeInSeconds = convertHHMMtoSeconds(endTime);

        int partitionWindow = 3600;
        for (Integer time: glucoseReadingTimesInSeconds) {
            int temp = time+partitionWindow;
            Log.d(TAG, "time for glucose reading check: " + time);
            Log.d(TAG, "time for notification: " + temp);
            timesForNotification.add( temp);
        }

        return timesForNotification;
    }

    /**
     * Given a String in the format HH:MM, returns the number of seconds from midnight.
     * @param aTime
     * @return
     */
    private int convertHHMMtoSeconds(String aTime) {
        //atime example: "23:55" , length =5 0-1, 3-4
        int timeInseconds = 0;
        String hour =null;
        String minutes = null;
        if(aTime!=null) {
            String[] time = aTime.split(":");
            if(time.length>0) {
                hour = time[0];
                timeInseconds = timeInseconds + Integer.valueOf(hour)*3600;
            }
            if(time.length>1) {
                minutes = time[1];
                timeInseconds = timeInseconds + Integer.valueOf(minutes)*60;
            }
            Log.d(TAG, "hour: " + hour + "minutes: " + minutes);
        }
        return timeInseconds;
    }

    /**
     * times at which we need to check last reported image to make sure
     * that the
     * @return
     */
    private boolean checkLastImageReport(StreamSnapshot snapshot) {
        Calendar c = Calendar.getInstance();
        long now = c.getTimeInMillis();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        long passed = now - c.getTimeInMillis();
        long secondsPassed = passed / 1000;
        Log.d(TAG, "Time in seconds now is: " + secondsPassed);

        //compare now with start and end time
        String endTime = UserPreferences.getInstance().getPreference("endTime");
        Log.d(TAG, "end time " + endTime);
        String startTime = UserPreferences.getInstance().getPreference("startTime");
        Log.d(TAG, "start time " + startTime);
        if(endTime!=null && startTime!=null) {
            int endTimeInSeconds = convertHHMMtoSeconds(endTime);
            Log.d(TAG, "end time in seconds " + endTimeInSeconds);
            int startTimeInSeconds = convertHHMMtoSeconds(startTime);
            Log.d(TAG, "start time in seconds " + startTimeInSeconds);

            if (secondsPassed > endTimeInSeconds || secondsPassed < startTimeInSeconds) {
                Log.d(TAG, "Situation returning false because time now is beyond start or end time" +
                        "for the user");
                return false;
            }
        }
        Log.d(TAG, "Time now is in the range of startTime and endTime for user");

        long time = 0;
        //long time = secondsPassed;
        for(int oneOfExpectedReportingTimes:getTimesForCheckingLastReports()) {
            Log.d(TAG, "Seconds passed: " + secondsPassed + "; Time: " + oneOfExpectedReportingTimes);
            Log.d(TAG, "upper bound for time: " + secondsPassed);
            Log.d(TAG, "Lower bound for time: " +
                    (secondsPassed - Constants.IMAGE_STREAM_GENERATOR_UPDATE_FREQUENCY_MINUTES*60));

            // If current time is between one of the expected reporting times and
            // (one of the expected reporting times + update frequency), then
            // assign "time" the value of oneOfExpectedReportingTimes and break.
            if (oneOfExpectedReportingTimes <= secondsPassed
                    && secondsPassed< oneOfExpectedReportingTimes + Constants.IMAGE_STREAM_GENERATOR_UPDATE_FREQUENCY_MINUTES*60) {
                Log.d(TAG, "Setting the value of time");
                //time = secondsPassed;
                //time should be equal to i //TODO: check logic
                time = oneOfExpectedReportingTimes; //this is the relevant notification time for the current time.
                break;
            }
        }
        if(time==0) {
            Log.d(TAG, "Situation Returning false because there is no relevant time to be checked");
            return false;
        }

        Log.d(TAG, "Set value of time is: " + time);

        if(snapshot.getCurrentValue(GlucoseReadingImage.class) != null) {
            long lastReportedTime = snapshot.getCurrentValue(GlucoseReadingImage.class).getCreationTime();
            Log.d(TAG, "Last reported time: " + lastReportedTime);
            long lastReportedTimeInSeconds = (lastReportedTime - c.getTimeInMillis())/1000;
            Log.d(TAG, "Last reported time in seconds since midnight : " + lastReportedTimeInSeconds);
            long secondsPassedSinceLastReport = (time - lastReportedTimeInSeconds);
            Log.d(TAG, "time - lastReportedTimeInSec: " + secondsPassedSinceLastReport);

            if(secondsPassedSinceLastReport > (3600*2)) {// it means 2 hours
                // TODO: there should also be = condition, last report can be >= to notificaiton check time minus delta
                Log.d(TAG, "Situation returning true");
                return true;
            }
            else {
                Log.d(TAG, "Situation returning false because not enough time has passed since last report.");
                return false;
            }
        } else {
            Log.d(TAG, "current value from snapshot is null. Situation returning true");
            //wait till notification time ("time from above") and then return true
            return true;
        }
    }
}
