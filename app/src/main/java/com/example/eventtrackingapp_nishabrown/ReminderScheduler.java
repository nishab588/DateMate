package com.example.eventtrackingapp_nishabrown;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Schedules, cancels, and tests SMS reminder alarms
 * for Date Mate events.
 *
 * Real reminders are scheduled for 24 hours before an event
 * and again at the event's scheduled date and time.
 */
public final class ReminderScheduler {

    /**
     * Prevents creation of ReminderScheduler objects.
     */
    private ReminderScheduler() {
    }

    /**
     * Schedules the reminder alarms for a saved event.
     *
     * @param context    Current application context.
     * @param eventId    Database ID of the event.
     * @param eventTitle Name of the event.
     * @param eventDate  Event date stored as yyyy-MM-dd.
     * @param eventTime  Event time stored as HH:mm.
     */
    public static void scheduleEventReminders(
            Context context,
            int eventId,
            String eventTitle,
            String eventDate,
            String eventTime
    ) {

        long eventTimeMillis =
                getEventTimeInMillis(
                        eventDate,
                        eventTime
                );

        // Do not schedule reminders if the date or time cannot be parsed.
        if (eventTimeMillis == -1) {
            return;
        }

        /*
         * Cancel any existing alarms for this event before
         * scheduling updated reminders.
         */
        cancelEventReminders(
                context,
                eventId
        );

        // Calculate the reminder time 24 hours before the event.
        long twentyFourHourReminder =
                eventTimeMillis - (24 * 60 * 60 * 1000L);

        long currentTime =
                System.currentTimeMillis();

        // Schedule the 24-hour reminder only if it is still in the future.
        if (twentyFourHourReminder > currentTime) {

            scheduleReminder(
                    context,
                    eventId,
                    1,
                    eventTitle,
                    eventDate,
                    eventTime,
                    twentyFourHourReminder
            );
        }

        // Schedule the day-of reminder at the event's scheduled time.
        if (eventTimeMillis > currentTime) {

            scheduleReminder(
                    context,
                    eventId,
                    2,
                    eventTitle,
                    eventDate,
                    eventTime,
                    eventTimeMillis
            );
        }
    }

    /**
     * Sends a test reminder through the same BroadcastReceiver
     * used by scheduled event reminders.
     *
     * This does not create an alarm. It allows Date Mate's
     * SMS reminder chain to be tested immediately.
     *
     * @param context    Current application context.
     * @param eventTitle Name used in the test reminder.
     * @param eventDate  Event date stored as yyyy-MM-dd.
     * @param eventTime  Event time stored as HH:mm.
     */
    public static void sendTestReminder(
            Context context,
            String eventTitle,
            String eventDate,
            String eventTime
    ) {

        Intent testIntent =
                new Intent(
                        context,
                        EventReminderReceiver.class
                );

        testIntent.putExtra(
                "EVENT_TITLE",
                eventTitle
        );

        testIntent.putExtra(
                "EVENT_DATE",
                eventDate
        );

        testIntent.putExtra(
                "EVENT_TIME",
                eventTime
        );

        /*
         * Send the broadcast directly to Date Mate's receiver.
         * EventReminderReceiver then calls SmsHelper exactly
         * as it would for a scheduled alarm.
         */
        context.sendBroadcast(testIntent);
    }

    /**
     * Cancels all scheduled reminders associated with an event.
     *
     * @param context Current application context.
     * @param eventId Database ID of the event being removed.
     */
    public static void cancelEventReminders(
            Context context,
            int eventId
    ) {

        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(
                        Context.ALARM_SERVICE
                );

        if (alarmManager == null) {
            return;
        }

        // Cancel the 24-hour reminder.
        cancelReminder(
                context,
                alarmManager,
                eventId,
                1
        );

        // Cancel the day-of reminder.
        cancelReminder(
                context,
                alarmManager,
                eventId,
                2
        );
    }

    /**
     * Cancels one scheduled reminder using the same request code
     * that was used when the alarm was originally created.
     *
     * @param context      Current application context.
     * @param alarmManager Android alarm service.
     * @param eventId      Database ID of the event.
     * @param reminderType Identifies the reminder being cancelled.
     */
    private static void cancelReminder(
            Context context,
            AlarmManager alarmManager,
            int eventId,
            int reminderType
    ) {

        Intent reminderIntent =
                new Intent(
                        context,
                        EventReminderReceiver.class
                );

        // Recreate the same request code used when scheduling the alarm.
        int requestCode =
                (eventId * 10) + reminderType;

        PendingIntent pendingIntent =
                PendingIntent.getBroadcast(
                        context,
                        requestCode,
                        reminderIntent,
                        PendingIntent.FLAG_NO_CREATE
                                | PendingIntent.FLAG_IMMUTABLE
                );

        if (pendingIntent != null) {

            // Remove the alarm from AlarmManager.
            alarmManager.cancel(pendingIntent);

            // Release the associated PendingIntent.
            pendingIntent.cancel();
        }
    }

    /**
     * Creates an AlarmManager alarm for one event reminder.
     *
     * @param context         Current application context.
     * @param eventId         Database ID of the event.
     * @param reminderType    Identifies the 24-hour or day-of reminder.
     * @param eventTitle      Name of the event.
     * @param eventDate       Scheduled event date.
     * @param eventTime       Scheduled event time.
     * @param triggerAtMillis Time when Android should trigger the reminder.
     */
    private static void scheduleReminder(
            Context context,
            int eventId,
            int reminderType,
            String eventTitle,
            String eventDate,
            String eventTime,
            long triggerAtMillis
    ) {

        // Create an explicit broadcast for Date Mate's reminder receiver.
        Intent reminderIntent =
                new Intent(
                        context,
                        EventReminderReceiver.class
                );

        reminderIntent.putExtra(
                "EVENT_TITLE",
                eventTitle
        );

        reminderIntent.putExtra(
                "EVENT_DATE",
                eventDate
        );

        reminderIntent.putExtra(
                "EVENT_TIME",
                eventTime
        );

        /*
         * Use a unique request code for each event and reminder type.
         * This keeps the 24-hour and day-of reminders separate.
         */
        int requestCode =
                (eventId * 10) + reminderType;

        PendingIntent pendingIntent =
                PendingIntent.getBroadcast(
                        context,
                        requestCode,
                        reminderIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE
                );

        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(
                        Context.ALARM_SERVICE
                );

        if (alarmManager != null) {

            /*
             * Use an inexact alarm for normal Date Mate reminders.
             * This avoids requiring special exact-alarm access.
             */
            alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
            );
        }
    }

    /**
     * Converts a stored event date and time into milliseconds
     * using the device's current timezone.
     *
     * @param eventDate Event date stored as yyyy-MM-dd.
     * @param eventTime Event time stored as HH:mm.
     * @return Event time in milliseconds, or -1 if parsing fails.
     */
    private static long getEventTimeInMillis(
            String eventDate,
            String eventTime
    ) {

        SimpleDateFormat dateFormat =
                new SimpleDateFormat(
                        "yyyy-MM-dd HH:mm",
                        Locale.US
                );

        // Explicitly use the device's timezone.
        dateFormat.setTimeZone(
                TimeZone.getDefault()
        );

        // Reject invalid dates instead of silently correcting them.
        dateFormat.setLenient(false);

        try {

            Date eventDateTime =
                    dateFormat.parse(
                            eventDate + " " + eventTime
                    );

            if (eventDateTime != null) {
                return eventDateTime.getTime();
            }

        } catch (ParseException exception) {

            // Return -1 when the stored date or time cannot be parsed.
            return -1;
        }

        return -1;
    }
}