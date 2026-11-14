package com.example.eventtrackingapp_nishabrown;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/**
 * Receives scheduled event reminder broadcasts and sends
 * an SMS reminder when Date Mate notifications are enabled.
 */
public class EventReminderReceiver extends BroadcastReceiver {

    private static final String PREFERENCES_NAME = "DateMatePreferences";
    private static final String SMS_PHONE_KEY = "smsPhoneNumber";

    @Override
    public void onReceive(Context context, Intent intent) {

        // Retrieve the event information included with the scheduled reminder.
        String eventTitle =
                intent.getStringExtra("EVENT_TITLE");

        String eventDate =
                intent.getStringExtra("EVENT_DATE");

        String eventTime =
                intent.getStringExtra("EVENT_TIME");

        // Retrieve the saved SMS reminder phone number.
        SharedPreferences sharedPreferences =
                context.getSharedPreferences(
                        PREFERENCES_NAME,
                        Context.MODE_PRIVATE
                );

        String phoneNumber =
                sharedPreferences.getString(
                        SMS_PHONE_KEY,
                        ""
                );

        // Do not attempt to send a reminder without required information.
        if (eventTitle == null
                || eventDate == null
                || eventTime == null
                || phoneNumber == null
                || phoneNumber.trim().isEmpty()) {

            return;
        }

        // SmsHelper checks both Android permission and the user's
        // Date Mate notification preference before sending.
        SmsHelper.sendEventReminder(
                context,
                phoneNumber,
                eventTitle,
                eventDate,
                eventTime
        );
    }
}