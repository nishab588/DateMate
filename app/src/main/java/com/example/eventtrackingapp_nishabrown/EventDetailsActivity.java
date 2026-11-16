package com.example.eventtrackingapp_nishabrown;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Displays, edits, updates, and deletes a selected Date Mate event.
 * Dates and times are displayed in a user-friendly format while
 * database values remain in yyyy-MM-dd and HH:mm format.
 */
public class EventDetailsActivity extends AppCompatActivity {

    private EditText textEventName;
    private EditText textEventDate;
    private EditText textEventTime;
    private EditText textEventLocation;
    private EditText textEventDescription;

    private Button buttonEditEvent;
    private Button buttonDeleteEvent;
    private Button buttonSaveChanges;
    private Button buttonCancel;
    private ImageButton buttonMenu;

    private DatabaseHelper databaseHelper;

    // Identifies the currently signed-in user.
    private long currentUserId = -1;

    // Identifies the event being viewed or edited.
    private int eventId = -1;

    // Identifies the recurring series associated with the selected event.
    // A value of -1 means the event does not belong to a recurring series.
    private long seriesId = -1;

    /*
     * Keep the database versions of the date and time separate
     * from the user-friendly values displayed on the screen.
     */
    private String selectedDate = "";
    private String selectedTime = "";

    // Tracks whether the user is editing the selected occurrence
    // or the selected occurrence and all subsequent events in its series.
    private boolean editingRecurringSeries = false;

    // Preserves the original date so we know where series updates begin.
    private String originalEventDate = "";

    // Stores the recurrence frequency of the selected event.
    private String recurrenceType = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_event_details);

        // Reference the fields that display and edit event information.
        textEventName = findViewById(R.id.textEventNameLabel);
        textEventDate = findViewById(R.id.textEventDate);
        textEventTime = findViewById(R.id.textEventTime);
        textEventLocation = findViewById(R.id.textEventLocation);
        textEventDescription = findViewById(R.id.textEventDescription);

        // Reference the event action buttons.
        buttonEditEvent = findViewById(R.id.buttonEditEvent);
        buttonDeleteEvent = findViewById(R.id.buttonDeleteEvent);
        buttonSaveChanges = findViewById(R.id.buttonSaveChanges);
        buttonCancel = findViewById(R.id.buttonCancel);

        // Reference the hamburger navigation button.
        buttonMenu = findViewById(R.id.buttonMenu);

        // Display the shared navigation menu.
        buttonMenu.setOnClickListener(
                view -> NavigationHelper.showNavigationMenu(
                        EventDetailsActivity.this,
                        view
                )
        );

        // Create access to the application's SQLite database.
        databaseHelper = new DatabaseHelper(this);

        // Retrieve the currently signed-in user's ID.
        currentUserId = getIntent().getLongExtra("USER_ID", -1);

        // Prevent access when no valid user ID was provided.
        if (currentUserId <= 0) {

            Toast.makeText(
                    this,
                    "Please log in to access your events.",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        // Retrieve the database ID sent from the Home screen.
        eventId = getIntent().getIntExtra(
                "EVENT_ID",
                -1
        );

        if (eventId == -1) {

            Toast.makeText(
                    this,
                    "Unable to load event.",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        // Load the selected event in read-only mode.
        loadEventDetails();
        setEditingEnabled(false);

        // Display editing options for recurring events.
        buttonEditEvent.setOnClickListener(view -> {

            if (isRecurringEvent()) {
                showRecurringEditOptions();
            } else {
                // Continue editing ordinary events normally.
                enableEditing();
            }
        });

        // Save changes made to the selected event.
        buttonSaveChanges.setOnClickListener(
                view -> saveChanges()
        );

        // Display deletion options for recurring events.
        buttonDeleteEvent.setOnClickListener(view -> {

            if (isRecurringEvent()) {
                showRecurringDeleteOptions();
            } else {
                // Continue using the existing confirmation for ordinary events.
                showDeleteConfirmationDialog();
            }
        });

        // Return to the previous screen without making additional changes.
        buttonCancel.setOnClickListener(
                view -> finish()
        );

        /*
         * When editing, tapping the date field opens the DatePicker
         * instead of requiring the user to type the date manually.
         */
        textEventDate.setOnClickListener(view -> {

            if (textEventDate.isEnabled()) {
                showDatePicker();
            }
        });

        /*
         * When editing, tapping the time field opens the TimePicker.
         */
        textEventTime.setOnClickListener(view -> {

            if (textEventTime.isEnabled()) {
                showTimePicker();
            }
        });

        // Prevent screen content from overlapping the system bars.
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.eventDetailsContent),
                (view, insets) -> {

                    Insets systemBars =
                            insets.getInsets(
                                    WindowInsetsCompat.Type.systemBars()
                            );

                    view.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );

                    return insets;
                }
        );
    }

    /**
     * Retrieves the selected event from SQLite and displays
     * user-friendly versions of the date and time.
     */
    private void loadEventDetails() {

        // Retrieve the event only if it belongs to the current user.
        Cursor cursor =
                databaseHelper.getEventById(
                        currentUserId,
                        eventId
                );

        try {

            if (cursor.moveToFirst()) {

                String title =
                        cursor.getString(
                                cursor.getColumnIndexOrThrow(
                                        "title"
                                )
                        );

                selectedDate =
                        cursor.getString(
                                cursor.getColumnIndexOrThrow(
                                        "date"
                                )
                        );

                // Preserve the original date for recurring-series updates.
                originalEventDate = selectedDate;

                selectedTime =
                        cursor.getString(
                                cursor.getColumnIndexOrThrow(
                                        "time"
                                )
                        );

                String location =
                        cursor.getString(
                                cursor.getColumnIndexOrThrow(
                                        "location"
                                )
                        );

                String description =
                        cursor.getString(
                                cursor.getColumnIndexOrThrow(
                                        "description"
                                )
                        );

                // Retrieve the recurring series ID, if one exists.
                int seriesIdColumn = cursor.getColumnIndexOrThrow("series_id");

                if (!cursor.isNull(seriesIdColumn)) {
                    seriesId = cursor.getLong(seriesIdColumn);
                } else {
                    seriesId = -1;
                }

                textEventName.setText(title);

                textEventDate.setText(
                        formatDateForDisplay(
                                selectedDate
                        )
                );

                textEventTime.setText(
                        formatTimeForDisplay(
                                selectedDate,
                                selectedTime
                        )
                );

                // Retrieve the recurrence frequency for series editing.
                int recurrenceTypeColumn =
                        cursor.getColumnIndexOrThrow("recurrence_type");

                if (!cursor.isNull(recurrenceTypeColumn)) {
                    recurrenceType = cursor.getString(recurrenceTypeColumn);
                } else {
                    recurrenceType = "";
                }

                textEventLocation.setText(location);
                textEventDescription.setText(description);

            } else {

                // The event does not exist or does not belong to this user.
                Toast.makeText(
                        this,
                        "Event not found or access denied.",
                        Toast.LENGTH_SHORT
                ).show();

                finish();
                return;
            }

        } finally {

            // Release the cursor after retrieving the event.
            cursor.close();
        }
    }

    /**
     * Determines whether the selected event belongs to a recurring series.
     *
     * @return True if the event is part of a recurring series.
     */
    private boolean isRecurringEvent() {
        return seriesId > 0;
    }

    /**
     * Allows the user to choose which recurring events to edit.
     */
    private void showRecurringEditOptions() {

        String[] options = {
                "This Event Only",
                "This and Future Events"
        };

        new AlertDialog.Builder(this)
                .setTitle("Edit Recurring Event")
                .setItems(options, (dialog, which) -> {

                    if (which == 0) {

                        // Edit only the selected occurrence.
                        editingRecurringSeries = false;
                        enableEditing();

                    } else {

                        // Edit the selected occurrence and all future occurrences.
                        editingRecurringSeries = true;
                        enableEditing();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Allows the user to choose which recurring events to delete.
     */
    private void showRecurringDeleteOptions() {

        String[] options = {
                "This Event Only",
                "Entire Series"
        };

        new AlertDialog.Builder(this)
                .setTitle("Delete Recurring Event")
                .setItems(options, (dialog, which) -> {

                    if (which == 0) {
                        // Use the existing confirmation for one occurrence.
                        showDeleteConfirmationDialog();

                    } else {
                        // Confirm before deleting the entire recurring series.
                        showDeleteSeriesConfirmationDialog();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Confirms that the user wants to permanently delete
     * every occurrence in a recurring event series.
     */
    private void showDeleteSeriesConfirmationDialog() {

        new AlertDialog.Builder(this)
                .setTitle("Delete Entire Series?")
                .setMessage(
                        "Are you sure you want to delete all occurrences "
                                + "of this recurring event? "
                                + "This action cannot be undone."
                )
                .setPositiveButton(
                        "Delete Entire Series",
                        (dialog, which) -> deleteRecurringSeries()
                )
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Unlocks the event fields so the user can modify
     * the currently saved information.
     */
    private void enableEditing() {

        setEditingEnabled(true);

        // Hide actions that should not be used while editing.
        buttonEditEvent.setVisibility(View.GONE);
        buttonDeleteEvent.setVisibility(View.GONE);

        // Display the Save Changes control.
        buttonSaveChanges.setVisibility(View.VISIBLE);

        // Place the cursor in the event name field.
        textEventName.requestFocus();
    }

    /**
     * Enables or disables editing for all event fields.
     *
     * @param enabled true when the event should be editable.
     */
    private void setEditingEnabled(boolean enabled) {

        textEventName.setEnabled(enabled);
        textEventDate.setEnabled(enabled);
        textEventTime.setEnabled(enabled);
        textEventLocation.setEnabled(enabled);
        textEventDescription.setEnabled(enabled);

        /*
         * Prevent users from manually typing date and time values.
         * DatePicker and TimePicker handle those fields instead.
         */
        textEventDate.setFocusable(false);
        textEventTime.setFocusable(false);
    }

    /**
     * Opens a DatePicker using the event's currently saved date.
     */
    private void showDatePicker() {

        Calendar calendar =
                getCalendarFromStoredDateTime();

        int year =
                calendar.get(Calendar.YEAR);

        int month =
                calendar.get(Calendar.MONTH);

        int day =
                calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog =
                new DatePickerDialog(
                        this,
                        (view, selectedYear, selectedMonth, selectedDay) -> {

                            // Store the database-friendly date.
                            selectedDate =
                                    String.format(
                                            Locale.US,
                                            "%04d-%02d-%02d",
                                            selectedYear,
                                            selectedMonth + 1,
                                            selectedDay
                                    );

                            // Display the friendly date.
                            textEventDate.setText(
                                    formatDateForDisplay(
                                            selectedDate
                                    )
                            );

                            /*
                             * Refresh the time display because the timezone
                             * abbreviation may change with daylight saving time.
                             */
                            textEventTime.setText(
                                    formatTimeForDisplay(
                                            selectedDate,
                                            selectedTime
                                    )
                            );
                        },
                        year,
                        month,
                        day
                );

        datePickerDialog.show();
    }

    /**
     * Opens a TimePicker using the event's currently saved time.
     */
    private void showTimePicker() {

        Calendar calendar =
                getCalendarFromStoredDateTime();

        int hour =
                calendar.get(Calendar.HOUR_OF_DAY);

        int minute =
                calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog =
                new TimePickerDialog(
                        this,
                        (view, selectedHour, selectedMinute) -> {

                            /*
                             * Keep the database value in 24-hour format.
                             */
                            selectedTime =
                                    String.format(
                                            Locale.US,
                                            "%02d:%02d",
                                            selectedHour,
                                            selectedMinute
                                    );

                            // Display the friendly 12-hour version.
                            textEventTime.setText(
                                    formatTimeForDisplay(
                                            selectedDate,
                                            selectedTime
                                    )
                            );
                        },
                        hour,
                        minute,
                        false
                );

        timePickerDialog.show();
    }

    /**
     * Creates a Calendar representing the event's currently
     * stored date and time.
     */
    private Calendar getCalendarFromStoredDateTime() {

        Calendar calendar =
                Calendar.getInstance();

        SimpleDateFormat databaseFormat =
                new SimpleDateFormat(
                        "yyyy-MM-dd HH:mm",
                        Locale.US
                );

        databaseFormat.setLenient(false);

        try {

            Date eventDateTime =
                    databaseFormat.parse(
                            selectedDate
                                    + " "
                                    + selectedTime
                    );

            if (eventDateTime != null) {
                calendar.setTime(eventDateTime);
            }

        } catch (ParseException exception) {

            // Use the current date and time if parsing fails.
        }

        return calendar;
    }

    /**
     * Converts a Calendar date into the format used by SQLite.
     *
     * @param calendar Calendar containing the event date.
     * @return Date formatted as yyyy-MM-dd.
     */
    private String formatCalendarDateForDatabase(Calendar calendar) {

        SimpleDateFormat databaseFormat =
                new SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.US
                );

        return databaseFormat.format(calendar.getTime());
    }

    /**
     * Converts a database date into a Calendar for recurrence calculations.
     *
     * @param storedDate Date formatted as yyyy-MM-dd.
     * @return Calendar containing the stored date, or null if invalid.
     */
    private Calendar getCalendarFromDatabaseDate(String storedDate) {

        SimpleDateFormat databaseFormat =
                new SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.US
                );

        databaseFormat.setLenient(false);

        try {

            Date parsedDate = databaseFormat.parse(storedDate);

            if (parsedDate != null) {

                Calendar calendar = Calendar.getInstance();
                calendar.setTime(parsedDate);

                return calendar;
            }

        } catch (ParseException exception) {

            // Return null when the stored date cannot be parsed.
        }

        return null;
    }

    /**
     * Calculates the date of a recurring occurrence.
     * Monthly events remain anchored to the selected day of the month.
     *
     * @param startDate Date of the first updated occurrence.
     * @param occurrenceIndex Number of recurrence intervals after the first.
     * @return Calculated date, or null if the recurrence type is invalid.
     */
    private Calendar calculateRecurringDate(
            Calendar startDate,
            int occurrenceIndex
    ) {

        Calendar calculatedDate =
                (Calendar) startDate.clone();

        switch (recurrenceType.toLowerCase(Locale.US)) {

            case "daily":
                calculatedDate.add(Calendar.DAY_OF_MONTH, occurrenceIndex);
                break;

            case "weekly":
                calculatedDate.add(Calendar.WEEK_OF_YEAR, occurrenceIndex);
                break;

            case "monthly":

                int originalDay =
                        startDate.get(Calendar.DAY_OF_MONTH);

                calculatedDate.set(Calendar.DAY_OF_MONTH, 1);
                calculatedDate.add(Calendar.MONTH, occurrenceIndex);

                int lastDay =
                        calculatedDate.getActualMaximum(
                                Calendar.DAY_OF_MONTH
                        );

                calculatedDate.set(
                        Calendar.DAY_OF_MONTH,
                        Math.min(originalDay, lastDay)
                );
                break;

            default:
                return null;
        }

        return calculatedDate;
    }

    /**
     * Converts a database date into a user-friendly display value.
     *
     * Example:
     * 2026-08-14 becomes August 14, 2026.
     */
    private String formatDateForDisplay(
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
     * Converts a stored 24-hour time into a user-friendly
     * 12-hour time with AM/PM and timezone.
     *
     * Example:
     * 16:52 becomes 4:52 PM EDT.
     */
    private String formatTimeForDisplay(
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

            Date eventDateTime =
                    databaseFormat.parse(
                            storedDate
                                    + " "
                                    + storedTime
                    );

            if (eventDateTime != null) {
                return displayFormat.format(
                        eventDateTime
                );
            }

        } catch (ParseException exception) {

            return storedTime;
        }

        return storedTime;
    }

    /**
     * Updates the selected recurring occurrence and all future
     * occurrences while preserving earlier events in the series.
     *
     * @param title Updated event title.
     * @param time Updated event time.
     * @param location Updated event location.
     * @param description Updated event description.
     */
    private void updateFutureRecurringEvents(
            String title,
            String time,
            String location,
            String description
    ) {

        // Retrieve the selected occurrence and all future occurrences
        // belonging to the currently signed-in user.
        Cursor cursor = databaseHelper.getFutureEventsBySeriesId(
                currentUserId,
                seriesId,
                originalEventDate
        );

        // Store the IDs of the selected and future occurrences.
        List<Integer> futureEventIds = new ArrayList<>();

        try {

            while (cursor.moveToNext()) {

                int recurringEventId = cursor.getInt(
                        cursor.getColumnIndexOrThrow("event_id")
                );

                futureEventIds.add(recurringEventId);
            }

        } finally {

            // Always close the cursor after reading the events.
            cursor.close();
        }

        // Confirm that there are occurrences available to update.
        if (futureEventIds.isEmpty()) {

            Toast.makeText(
                    this,
                    "No recurring events were found to update.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // Convert the newly selected date into a Calendar.
        Calendar newStartDate =
                getCalendarFromDatabaseDate(selectedDate);

        if (newStartDate == null) {

            Toast.makeText(
                    this,
                    "Unable to process the selected event date.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // Prepare the new dates before updating the database.
        List<String> updatedEventDates = new ArrayList<>();

        for (int index = 0; index < futureEventIds.size(); index++) {

            Calendar occurrenceDate =
                    calculateRecurringDate(newStartDate, index);

            if (occurrenceDate == null) {

                Toast.makeText(
                        this,
                        "Unable to calculate recurring event dates.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            // Store the calculated date in SQLite's yyyy-MM-dd format.
            updatedEventDates.add(
                    formatCalendarDateForDatabase(occurrenceDate)
            );
        }

        // Begin a transaction so all recurring event updates
        // can be saved together or rolled back if an error occurs.
        databaseHelper.beginEventTransaction();

        try {

            // Update each occurrence using its corresponding event ID and date.
            for (int index = 0; index < futureEventIds.size(); index++) {

                int recurringEventId = futureEventIds.get(index);
                String updatedDate = updatedEventDates.get(index);

                // Update the shared event information.
                boolean detailsUpdated =
                        databaseHelper.updateRecurringEventDetails(
                                currentUserId,
                                recurringEventId,
                                title,
                                time,
                                location,
                                description
                        );

                // Update the occurrence's scheduled date.
                boolean dateUpdated =
                        databaseHelper.updateRecurringEventDate(
                                currentUserId,
                                recurringEventId,
                                updatedDate
                        );

                // Stop and roll back if either update fails.
                if (!detailsUpdated || !dateUpdated) {
                    throw new IllegalStateException(
                            "Unable to update recurring event ID: "
                                    + recurringEventId
                    );
                }
            }

            // Commit the transaction only after every occurrence is updated.
            databaseHelper.markEventTransactionSuccessful();

        } catch (Exception exception) {

            // Inform the user if a recurring event update fails.
            Toast.makeText(
                    this,
                    "Unable to update recurring event series.",
                    Toast.LENGTH_SHORT
            ).show();

            return;

        } finally {

            // Always end the transaction, even if an update fails.
            databaseHelper.endEventTransaction();
        }

        // Reschedule reminders after the database transaction succeeds.
        for (int index = 0; index < futureEventIds.size(); index++) {

            int recurringEventId = futureEventIds.get(index);
            String updatedDate = updatedEventDates.get(index);

            // Remove reminders associated with the previous schedule.
            ReminderScheduler.cancelEventReminders(
                    this,
                    recurringEventId
            );

            // Schedule reminders using the updated event information.
            ReminderScheduler.scheduleEventReminders(
                    this,
                    recurringEventId,
                    title,
                    updatedDate,
                    time
            );
        }

        // Confirm that the recurring events were updated successfully.
        Toast.makeText(
                this,
                "Recurring events updated successfully.",
                Toast.LENGTH_SHORT
        ).show();

        // Return to the Home screen after saving.
        finish();
    }

    /**
     * Validates and saves modifications to the selected event.
     * Reminder alarms are rescheduled after a successful update.
     */
    private void saveChanges() {

        String title =
                textEventName
                        .getText()
                        .toString()
                        .trim();

        String location =
                textEventLocation
                        .getText()
                        .toString()
                        .trim();

        String description =
                textEventDescription
                        .getText()
                        .toString()
                        .trim();

        /*
         * selectedDate and selectedTime contain the database-friendly
         * values rather than the formatted text shown to the user.
         */
        if (title.isEmpty()
                || selectedDate.isEmpty()
                || selectedTime.isEmpty()) {

            Toast.makeText(
                    this,
                    "Event name, date, and time are required.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // Update the selected occurrence and all future occurrences
        // when the user chooses to edit the recurring series.
        if (editingRecurringSeries) {

            updateFutureRecurringEvents(
                    title,
                    selectedTime,
                    location,
                    description
            );

            return;
        }

        // Update the event only if it belongs to the current user.
        boolean eventUpdated =
                databaseHelper.updateEvent(
                        currentUserId,
                        eventId,
                        title,
                        selectedDate,
                        selectedTime,
                        location,
                        description
                );

        if (eventUpdated) {

            /*
             * Reschedule the reminders using the database-friendly
             * date and time values.
             */
            ReminderScheduler.scheduleEventReminders(
                    this,
                    eventId,
                    title,
                    selectedDate,
                    selectedTime
            );

            Toast.makeText(
                    this,
                    "Event updated successfully.",
                    Toast.LENGTH_SHORT
            ).show();

            // Return to Home after saving the updated event.
            finish();

        } else {

            Toast.makeText(
                    this,
                    "Unable to update event.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    /**
     * Confirms that the user wants to permanently delete
     * the currently selected event.
     */
    private void showDeleteConfirmationDialog() {

        String title =
                textEventName
                        .getText()
                        .toString()
                        .trim();

        new AlertDialog.Builder(this)
                .setTitle("Delete Event?")
                .setMessage(
                        "Are you sure you want to delete \""
                                + title
                                + "\"?"
                )
                .setPositiveButton(
                        "Delete",
                        (dialog, which) -> deleteEvent()
                )
                .setNegativeButton(
                        "Cancel",
                        (dialog, which) -> dialog.dismiss()
                )
                .show();
    }

    /**
     * Deletes an entire recurring event series and cancels
     * the reminders associated with each occurrence.
     */
    private void deleteRecurringSeries() {

        if (currentUserId <= 0 || seriesId <= 0) {
            Toast.makeText(
                    this,
                    "Unable to identify recurring event series.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        // Store event IDs before deleting their database records.
        List<Integer> eventIds = new ArrayList<>();

        Cursor cursor = databaseHelper.getEventsBySeriesId(
                currentUserId,
                seriesId
        );

        try {
            while (cursor.moveToNext()) {
                int recurringEventId = cursor.getInt(
                        cursor.getColumnIndexOrThrow("event_id")
                );

                eventIds.add(recurringEventId);
            }
        } finally {
            cursor.close();
        }

        // Delete only the recurring series belonging to this user.
        boolean seriesDeleted = databaseHelper.deleteEventSeries(
                currentUserId,
                seriesId
        );

        if (seriesDeleted) {

            // Cancel reminders for every deleted occurrence.
            for (int recurringEventId : eventIds) {
                ReminderScheduler.cancelEventReminders(
                        this,
                        recurringEventId
                );
            }

            Toast.makeText(
                    this,
                    "Recurring event series deleted successfully.",
                    Toast.LENGTH_SHORT
            ).show();

            finish();

        } else {
            Toast.makeText(
                    this,
                    "Unable to delete recurring event series.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    /**
     * Removes the selected event from SQLite and cancels
     * any reminder alarms associated with it.
     */
    private void deleteEvent() {

        // Delete the event only if it belongs to the current user.
        boolean eventDeleted =
                databaseHelper.deleteEvent(
                        currentUserId,
                        eventId
                );

        if (eventDeleted) {

            // Cancel reminders only after successful deletion.
            ReminderScheduler.cancelEventReminders(
                    this,
                    eventId
            );

            // Return to Home after deleting the event.
            finish();

        } else {

            Toast.makeText(
                    this,
                    "Unable to delete event.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }
}