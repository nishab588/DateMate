package com.example.eventtrackingapp_nishabrown;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.telephony.SmsManager;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Provides SMS reminder functionality for Date Mate.
 * Messages are only attempted when Android SMS permission is granted
 * and the user has enabled SMS reminders inside the application.
 */
public final class SmsHelper {

    private static final String PREFERENCES_NAME =
            "DateMatePreferences";

    private static final String SMS_ENABLED_KEY =
            "smsNotificationsEnabled";

    /**
     * Prevents creation of SmsHelper objects.
     */
    private SmsHelper() {
    }

    /**
     * Determines whether Date Mate is currently allowed
     * to attempt sending SMS reminders.
     *
     * @param context Current application context.
     * @return true when permission exists and reminders are enabled.
     */
    public static boolean canSendSms(Context context) {

        boolean permissionGranted =
                ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.SEND_SMS
                ) == PackageManager.PERMISSION_GRANTED;

        SharedPreferences sharedPreferences =
                context.getSharedPreferences(
                        PREFERENCES_NAME,
                        Context.MODE_PRIVATE
                );

        boolean notificationsEnabled =
                sharedPreferences.getBoolean(
                        SMS_ENABLED_KEY,
                        false
                );

        return permissionGranted && notificationsEnabled;
    }

    /**
     * Sends an SMS reminder for an upcoming Date Mate event.
     *
     * @param context     Current application context.
     * @param phoneNumber Destination phone number.
     * @param eventTitle  Name of the event.
     * @param eventDate   Event date stored as yyyy-MM-dd.
     * @param eventTime   Event time stored as HH:mm.
     * @return true when the send request is submitted.
     */
    public static boolean sendEventReminder(
            Context context,
            String phoneNumber,
            String eventTitle,
            String eventDate,
            String eventTime
    ) {

        if (!canSendSms(context)) {

            Toast.makeText(
                    context,
                    "SMS test stopped: notifications are disabled or permission is unavailable.",
                    Toast.LENGTH_LONG
            ).show();

            return false;
        }

        if (phoneNumber == null
                || phoneNumber.trim().isEmpty()) {

            Toast.makeText(
                    context,
                    "SMS test stopped: no phone number is saved.",
                    Toast.LENGTH_LONG
            ).show();

            return false;
        }

        String formattedDate =
                formatDateForMessage(
                        eventDate
                );

        String formattedTime =
                formatTimeForMessage(
                        eventDate,
                        eventTime
                );

        String message =
                "Date Mate Reminder: "
                        + eventTitle
                        + " is scheduled for "
                        + formattedDate
                        + " at "
                        + formattedTime
                        + ".";

        try {

            SmsManager smsManager =
                    SmsManager.getDefault();

            /*
             * Create a callback that Android will broadcast after
             * attempting to send the SMS.
             */
            Intent sentResultIntent =
                    new Intent(
                            context,
                            SmsSentReceiver.class
                    );

            PendingIntent sentPendingIntent =
                    PendingIntent.getBroadcast(
                            context,
                            5000,
                            sentResultIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT
                                    | PendingIntent.FLAG_IMMUTABLE
                    );

            smsManager.sendTextMessage(
                    phoneNumber,
                    null,
                    message,
                    sentPendingIntent,
                    null
            );

            Toast.makeText(
                    context,
                    "SMS send request submitted.",
                    Toast.LENGTH_SHORT
            ).show();

            return true;

        } catch (UnsupportedOperationException exception) {

            Toast.makeText(
                    context,
                    "SMS send result: This emulator does not support cellular messaging.",
                    Toast.LENGTH_LONG
            ).show();

            return false;

        } catch (SecurityException exception) {

            Toast.makeText(
                    context,
                    "SMS send result: SMS permission is unavailable.",
                    Toast.LENGTH_LONG
            ).show();

            return false;

        } catch (Exception exception) {

            Toast.makeText(
                    context,
                    "SMS send result: Unable to send SMS.",
                    Toast.LENGTH_LONG
            ).show();

            return false;
        }
    }

    /**
     * Formats the stored event date for the reminder message.
     */
    private static String formatDateForMessage(
            String storedDate
    ) {

        SimpleDateFormat databaseFormat =
                new SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.US
                );

        SimpleDateFormat displayFormat =
                new SimpleDateFormat(
                        "MMMM d, yyyy",
                        Locale.US
                );

        databaseFormat.setLenient(false);

        try {

            Date date =
                    databaseFormat.parse(
                            storedDate
                    );

            if (date != null) {
                return displayFormat.format(date);
            }

        } catch (ParseException exception) {

            return storedDate;
        }

        return storedDate;
    }

    /**
     * Formats the stored time using 12-hour AM/PM format
     * and the device's timezone abbreviation.
     */
    private static String formatTimeForMessage(
            String storedDate,
            String storedTime
    ) {

        SimpleDateFormat databaseFormat =
                new SimpleDateFormat(
                        "yyyy-MM-dd HH:mm",
                        Locale.US
                );

        SimpleDateFormat displayFormat =
                new SimpleDateFormat(
                        "h:mm a z",
                        Locale.US
                );

        databaseFormat.setLenient(false);

        try {

            Date dateTime =
                    databaseFormat.parse(
                            storedDate
                                    + " "
                                    + storedTime
                    );

            if (dateTime != null) {
                return displayFormat.format(dateTime);
            }

        } catch (ParseException exception) {

            return storedTime;
        }

        return storedTime;
    }
}