package com.example.eventtrackingapp_nishabrown;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Handles creation of new events for the Date Mate application.
 * New events are stored in SQLite and scheduled for SMS reminders.
 */
public class AddEventActivity extends AppCompatActivity {

    private EditText editEventName;
    private EditText editEventLocation;
    private EditText editEventDescription;

    private TextView textSelectedDate;
    private TextView textSelectedTime;

    // Displays the selected end date for recurring events.
    private TextView textRecurrenceEndDate;

    private Button buttonSelectDate;
    private Button buttonSelectTime;
    private Button buttonSaveEvent;

    // Opens the date picker for the recurring event end date.
    private Button buttonRecurrenceEndDate;

    // Allows users to select how frequently an event repeats.
    private Spinner spinnerEventRepeat;

    private ImageButton buttonMenu;

    private DatabaseHelper databaseHelper;

    // Identifies the account creating the new event.
    private long currentUserId = -1;

    /*
     * These values are stored in database-friendly formats:
     *
     * Date: yyyy-MM-dd
     * Time: HH:mm
     */
    private String selectedDate = "";
    private String selectedTime = "";

    // Stores the final date of a recurring event series.
    private String selectedRecurrenceEndDate = "";

    // Store individual date and time values for user-friendly display.
    private int selectedYear = -1;
    private int selectedMonth = -1;
    private int selectedDay = -1;
    private int selectedHour = -1;
    private int selectedMinute = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_event);

        // Retrieve the account ID passed from the Home screen.
        currentUserId = getIntent().getLongExtra("USER_ID", -1);

        // Prevent event creation when account information is missing.
        if (currentUserId <= 0) {
            Toast.makeText(
                    this,
                    "Please log in to create an event.",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        // Reference the controls used to create a new event.
        editEventName = findViewById(R.id.editEventName);
        editEventLocation = findViewById(R.id.editEventLocation);
        editEventDescription = findViewById(R.id.editEventDescription);

        textSelectedDate = findViewById(R.id.textSelectedDate);
        textSelectedTime = findViewById(R.id.textSelectedTime);
        textRecurrenceEndDate = findViewById(R.id.textRecurrenceEndDate);

        buttonSelectDate = findViewById(R.id.buttonSelectDate);
        buttonSelectTime = findViewById(R.id.buttonSelectTime);
        buttonSaveEvent = findViewById(R.id.buttonSaveEvent);
        buttonRecurrenceEndDate = findViewById(R.id.buttonRecurrenceEndDate);
        spinnerEventRepeat = findViewById(R.id.spinnerEventRepeat);

        // Reference the hamburger navigation button.
        buttonMenu = findViewById(R.id.buttonMenu);

        // Display the shared navigation menu.
        buttonMenu.setOnClickListener(
                view -> NavigationHelper.showNavigationMenu(
                        AddEventActivity.this,
                        view
                )
        );

        // Create access to the application's SQLite database.
        databaseHelper = new DatabaseHelper(this);

        // Define the available recurrence frequencies.
        String[] repeatOptions = {
                "Does Not Repeat",
                "Daily",
                "Weekly",
                "Monthly"
        };

        // Create the adapter that displays the recurrence options.
        ArrayAdapter<String> repeatAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                repeatOptions
        );

        // Use the standard dropdown layout when the Spinner is opened.
        repeatAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        // Connect the recurrence options to the Spinner.
        spinnerEventRepeat.setAdapter(repeatAdapter);

        // Show the recurrence end date controls only for repeating events.
        spinnerEventRepeat.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {

                        // Position 0 represents "Does Not Repeat".
                        boolean isRecurring = position != 0;

                        buttonRecurrenceEndDate.setVisibility(
                                isRecurring ? View.VISIBLE : View.GONE
                        );

                        textRecurrenceEndDate.setVisibility(
                                isRecurring ? View.VISIBLE : View.GONE
                        );
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        // No action is required.
                    }
                }
        );

        // Open the date selector when the user chooses Select Date.
        buttonSelectDate.setOnClickListener(
                view -> showDatePicker()
        );

        // Open the time selector when the user chooses Select Time.
        buttonSelectTime.setOnClickListener(
                view -> showTimePicker()
        );

        // Open the calendar to select the recurring event end date.
        buttonRecurrenceEndDate.setOnClickListener(
                view -> showRecurrenceEndDatePicker()
        );

        // Save the event and schedule its reminders.
        buttonSaveEvent.setOnClickListener(
                view -> saveEvent()
        );

        // Adjust the layout to prevent content from overlapping system bars.
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.addEventContent),
                (view, insets) -> {

                    Insets systemBars = insets.getInsets(
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
     * Displays a calendar dialog and stores the selected date.
     */
    private void showDatePicker() {

        Calendar calendar = Calendar.getInstance();

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog =
                new DatePickerDialog(
                        this,
                        (view, yearSelected, monthSelected, daySelected) -> {

                            // Store the selected date components.
                            selectedYear = yearSelected;
                            selectedMonth = monthSelected;
                            selectedDay = daySelected;

                            /*
                             * Store the date in yyyy-MM-dd format.
                             * This allows SQLite to sort dates chronologically.
                             */
                            selectedDate = String.format(
                                    Locale.US,
                                    "%04d-%02d-%02d",
                                    selectedYear,
                                    selectedMonth + 1,
                                    selectedDay
                            );

                            // Display the date in a more user-friendly format.
                            updateDateDisplay();

                            /*
                             * Refresh the time display because the timezone
                             * abbreviation can depend on the event date.
                             */
                            if (selectedHour != -1) {
                                updateTimeDisplay();
                            }
                        },
                        year,
                        month,
                        day
                );

        datePickerDialog.show();
    }

    /**
     * Allows the user to select the final date of a recurring event series.
     */
    private void showRecurrenceEndDatePicker() {

        Calendar calendar = Calendar.getInstance();

        // Use the event's starting date when one has been selected.
        if (selectedYear != -1) {
            calendar.set(selectedYear, selectedMonth, selectedDay);
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, day) -> {

                    // Store the end date in SQLite-friendly format.
                    selectedRecurrenceEndDate = String.format(
                            Locale.US,
                            "%04d-%02d-%02d",
                            year,
                            month + 1,
                            day
                    );

                    // Display the selected date in a readable format.
                    Calendar endCalendar = Calendar.getInstance();
                    endCalendar.set(year, month, day);

                    SimpleDateFormat dateFormat = new SimpleDateFormat(
                            "MMMM d, yyyy",
                            Locale.US
                    );

                    textRecurrenceEndDate.setText(
                            "Repeat Ends: " +
                                    dateFormat.format(endCalendar.getTime())
                    );
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        // Prevent selecting an end date before the event's start date.
        if (selectedYear != -1) {
            Calendar minimumDate = Calendar.getInstance();
            minimumDate.clear();
            minimumDate.set(selectedYear, selectedMonth, selectedDay);

            datePickerDialog.getDatePicker().setMinDate(
                    minimumDate.getTimeInMillis()
            );
        }

        datePickerDialog.show();
    }

    /**
     * Displays a time dialog and stores the selected time.
     */
    private void showTimePicker() {

        Calendar calendar = Calendar.getInstance();

        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog =
                new TimePickerDialog(
                        this,
                        (view, hourSelected, minuteSelected) -> {

                            // Store the selected time components.
                            selectedHour = hourSelected;
                            selectedMinute = minuteSelected;

                            /*
                             * Store the time internally using 24-hour format.
                             * ReminderScheduler uses this value when parsing
                             * the event date and time.
                             */
                            selectedTime = String.format(
                                    Locale.US,
                                    "%02d:%02d",
                                    selectedHour,
                                    selectedMinute
                            );

                            // Display the time using 12-hour AM/PM format.
                            updateTimeDisplay();
                        },
                        hour,
                        minute,
                        false
                );

        timePickerDialog.show();
    }

    /**
     * Displays the selected date as Month Day, Year.
     *
     * Example:
     * August 14, 2026
     */
    private void updateDateDisplay() {

        if (selectedYear == -1
                || selectedMonth == -1
                || selectedDay == -1) {

            return;
        }

        Calendar eventCalendar = Calendar.getInstance();

        eventCalendar.set(
                selectedYear,
                selectedMonth,
                selectedDay
        );

        SimpleDateFormat displayDateFormat =
                new SimpleDateFormat(
                        "MMMM d, yyyy",
                        Locale.US
                );

        String displayDate =
                displayDateFormat.format(
                        eventCalendar.getTime()
                );

        textSelectedDate.setText(
                "Date: " + displayDate
        );
    }

    /**
     * Displays the selected time using a 12-hour clock,
     * AM/PM indicator, and the device's timezone.
     *
     * Example:
     * 4:52 PM EDT
     */
    private void updateTimeDisplay() {

        if (selectedHour == -1
                || selectedMinute == -1) {

            return;
        }

        Calendar eventCalendar = Calendar.getInstance();

        /*
         * Use the selected event date when available so the correct
         * daylight-saving timezone abbreviation is displayed.
         */
        if (selectedYear != -1
                && selectedMonth != -1
                && selectedDay != -1) {

            eventCalendar.set(
                    selectedYear,
                    selectedMonth,
                    selectedDay,
                    selectedHour,
                    selectedMinute,
                    0
            );

        } else {

            // If the time is selected first, use today's date temporarily.
            eventCalendar.set(
                    Calendar.HOUR_OF_DAY,
                    selectedHour
            );

            eventCalendar.set(
                    Calendar.MINUTE,
                    selectedMinute
            );

            eventCalendar.set(
                    Calendar.SECOND,
                    0
            );
        }

        SimpleDateFormat displayTimeFormat =
                new SimpleDateFormat(
                        "h:mm a z",
                        Locale.US
                );

        String displayTime =
                displayTimeFormat.format(
                        eventCalendar.getTime()
                );

        textSelectedTime.setText(
                "Time: " + displayTime
        );
    }

    /**
     * Advances a calendar to the next recurring event date.
     *
     * @param calendar Date of the current occurrence.
     * @param recurrenceType Selected recurrence frequency.
     */
    private void advanceRecurrenceDate(
            Calendar calendar,
            String recurrenceType
    ) {

        switch (recurrenceType) {

            case "Daily":
                calendar.add(Calendar.DAY_OF_MONTH, 1);
                break;

            case "Weekly":
                calendar.add(Calendar.WEEK_OF_YEAR, 1);
                break;

            case "Monthly":
                // Move to the first day before advancing to avoid date overflow.
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                calendar.add(Calendar.MONTH, 1);

                // Preserve the original day when possible.
                // Otherwise, use the last valid day of the month.
                int lastDayOfMonth = calendar.getActualMaximum(
                        Calendar.DAY_OF_MONTH
                );

                calendar.set(
                        Calendar.DAY_OF_MONTH,
                        Math.min(selectedDay, lastDayOfMonth)
                );
                break;
        }
    }

    /**
     * Validates the event information, stores the event in SQLite,
     * and schedules the event's SMS reminder alarms.
     */
    private void saveEvent() {

        String title =
                editEventName
                        .getText()
                        .toString()
                        .trim();

        String location =
                editEventLocation
                        .getText()
                        .toString()
                        .trim();

        String description =
                editEventDescription
                        .getText()
                        .toString()
                        .trim();

        // Require the event name, date, and time before saving.
        if (title.isEmpty()
                || selectedDate.isEmpty()
                || selectedTime.isEmpty()) {

            Toast.makeText(
                    this,
                    "Please enter an event name, date, and time.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // Determine whether the user selected a recurring event.
        boolean isRecurring = spinnerEventRepeat.getSelectedItemPosition() != 0;

        // Require a valid end date when recurrence is enabled.
        if (isRecurring) {

            if (selectedRecurrenceEndDate.isEmpty()) {

                Toast.makeText(
                        this,
                        "Please select a repeat end date.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            // Dates use yyyy-MM-dd, so chronological string comparison is valid.
            if (selectedRecurrenceEndDate.compareTo(selectedDate) < 0) {

                Toast.makeText(
                        this,
                        "The repeat end date cannot be before the event date.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }
        }

        // Identify the recurrence frequency selected by the user.
        String recurrenceType = "None";

        if (isRecurring) {
            recurrenceType = spinnerEventRepeat
                    .getSelectedItem()
                    .toString();
        }

        // Begin a transaction when saving a recurring event series.
        if (isRecurring) {
            databaseHelper.beginEventTransaction();
        }

        // Track whether the recurring event transaction has started.
        boolean transactionStarted = isRecurring;

        // Keep the original event ID available after the transaction ends.
        long eventId = -1;

        // Track whether all event occurrences were saved successfully.
        boolean eventSavedSuccessfully = false;

        // Store the IDs and dates of additional recurring occurrences.
        List<Long> recurringEventIds = new ArrayList<>();
        List<String> recurringEventDates = new ArrayList<>();

        try {

        /*
         * Store the new event in SQLite.
         *
         * The database receives:
         * Date = yyyy-MM-dd
         * Time = HH:mm
         *
         * The user still sees the friendly formatted versions
         * displayed on the screen.
         */
        eventId =
                databaseHelper.createEvent(
                        currentUserId,
                        title,
                        selectedDate,
                        selectedTime,
                        location,
                        description
                );

        if (eventId != -1) {

            // Associate the first event with its recurring series.
            if (isRecurring) {

                boolean recurrenceSaved = databaseHelper.setEventRecurrence(
                        currentUserId,
                        eventId,
                        recurrenceType,
                        selectedRecurrenceEndDate,
                        eventId
                );

                if (!recurrenceSaved) {

                    Toast.makeText(
                            this,
                            "Unable to save recurrence information.",
                            Toast.LENGTH_SHORT
                    ).show();

                    return;
                }
            }

            // Prepare the dates used to generate additional occurrences.
            if (isRecurring) {

                Calendar occurrenceDate = Calendar.getInstance();
                occurrenceDate.clear();
                occurrenceDate.set(
                        selectedYear,
                        selectedMonth,
                        selectedDay
                );

                // Establish the final date allowed for this recurring series.
                Calendar recurrenceEndDate = Calendar.getInstance();
                recurrenceEndDate.clear();

                String[] endDateParts = selectedRecurrenceEndDate.split("-");

                recurrenceEndDate.set(
                        Integer.parseInt(endDateParts[0]),
                        Integer.parseInt(endDateParts[1]) - 1,
                        Integer.parseInt(endDateParts[2])
                );

                // Move past the initial event, which has already been saved.
                advanceRecurrenceDate(occurrenceDate, recurrenceType);

                // Generate each additional occurrence through the selected end date.
                while (!occurrenceDate.after(recurrenceEndDate)) {

                    // Format the occurrence date for SQLite.
                    String occurrenceDateString = String.format(
                            Locale.US,
                            "%04d-%02d-%02d",
                            occurrenceDate.get(Calendar.YEAR),
                            occurrenceDate.get(Calendar.MONTH) + 1,
                            occurrenceDate.get(Calendar.DAY_OF_MONTH)
                    );

                    // Save this occurrence as a separate event belonging to the user.
                    long occurrenceId = databaseHelper.createEvent(
                            currentUserId,
                            title,
                            occurrenceDateString,
                            selectedTime,
                            location,
                            description
                    );

                    // Stop saving the series if an occurrence could not be created.
                    if (occurrenceId == -1) {

                        Toast.makeText(
                                this,
                                "Unable to create a recurring event occurrence.",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }

                    // Associate the new occurrence with the original recurring series.
                    boolean occurrenceRecurrenceSaved = databaseHelper.setEventRecurrence(
                            currentUserId,
                            occurrenceId,
                            recurrenceType,
                            selectedRecurrenceEndDate,
                            eventId
                    );

                    if (!occurrenceRecurrenceSaved) {
                        Toast.makeText(
                                this,
                                "Unable to save recurring event information.",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }

                    // Record this occurrence for reminder scheduling after the transaction.
                    recurringEventIds.add(occurrenceId);
                    recurringEventDates.add(occurrenceDateString);

                    // Advance to the next date in the recurring series.
                    advanceRecurrenceDate(occurrenceDate, recurrenceType);
                }
            }

            // Commit the recurring series only after every occurrence is saved.
            if (isRecurring) {
                databaseHelper.markEventTransactionSuccessful();
            }

            // All required database operations completed successfully.
            eventSavedSuccessfully = true;

        } else {

            Toast.makeText(
                    this,
                    "Unable to save event.",
                    Toast.LENGTH_SHORT
            ).show();
        }

        } finally {
            if (transactionStarted) {
                databaseHelper.endEventTransaction();
            }
        }

        // Schedule reminders only after the event has been saved successfully.
        if (eventSavedSuccessfully && eventId != -1) {
            ReminderScheduler.scheduleEventReminders(
                    this,
                    (int) eventId,
                    title,
                    selectedDate,
                    selectedTime
            );
        }

        // Schedule reminders for additional recurring occurrences.
        if (eventSavedSuccessfully && eventId != -1) {
            for (int i = 0; i < recurringEventIds.size(); i++) {
                ReminderScheduler.scheduleEventReminders(
                        this,
                        recurringEventIds.get(i).intValue(),
                        title,
                        recurringEventDates.get(i),
                        selectedTime
                );
            }
        }

        // Confirm the event was saved and return to the previous screen.
        if (eventSavedSuccessfully && eventId != -1) {
            Toast.makeText(
                    this,
                    "Event saved successfully.",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
        }
    }
}