package com.example.eventtrackingapp_nishabrown;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
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

    private Button buttonSelectDate;
    private Button buttonSelectTime;
    private Button buttonSaveEvent;

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

        buttonSelectDate = findViewById(R.id.buttonSelectDate);
        buttonSelectTime = findViewById(R.id.buttonSelectTime);
        buttonSaveEvent = findViewById(R.id.buttonSaveEvent);

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

        // Open the date selector when the user chooses Select Date.
        buttonSelectDate.setOnClickListener(
                view -> showDatePicker()
        );

        // Open the time selector when the user chooses Select Time.
        buttonSelectTime.setOnClickListener(
                view -> showTimePicker()
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
        long eventId =
                databaseHelper.createEvent(
                        currentUserId,
                        title,
                        selectedDate,
                        selectedTime,
                        location,
                        description
                );

        if (eventId != -1) {

            // Schedule the 24-hour and day-of reminders for the new event.
            ReminderScheduler.scheduleEventReminders(
                    this,
                    (int) eventId,
                    title,
                    selectedDate,
                    selectedTime
            );

            Toast.makeText(
                    this,
                    "Event saved successfully.",
                    Toast.LENGTH_SHORT
            ).show();

            // Return to the previous screen after a successful save.
            finish();

        } else {

            Toast.makeText(
                    this,
                    "Unable to save event.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }
}