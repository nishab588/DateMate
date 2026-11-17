package com.example.eventtrackingapp_nishabrown;

import android.os.Bundle;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.content.Intent;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;
import java.util.Locale;

/**
 * Displays DateMate's interactive monthly calendar
 * and prepares the event list for the selected date.
 */
public class CalendarActivity extends AppCompatActivity {

    // Calendar screen controls.
    private GridLayout calendarGrid;
    private TextView textSelectedDate;
    private LinearLayout layoutCalendarEvents;
    private Button buttonBackHome;

    // Custom controls for navigating between calendar months.
    private Button buttonPreviousMonth;
    private Button buttonNextMonth;
    private TextView textDisplayedMonth;

    // Tracks the month currently displayed on the calendar.
    private Calendar displayedMonth;

    // Stores the currently selected date in SQLite format.
    private String selectedDate = "";

    // Identifies the currently signed-in user.
    private long currentUserId;

    // Provides access to the user's saved events.
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Connect this activity to its XML layout.
        setContentView(R.layout.activity_calendar);

        // Retrieve the authenticated user's ID from navigation.
        currentUserId = getIntent().getLongExtra(
                "USER_ID",
                -1
        );

        // Prevent access when no authenticated user ID is available.
        if (currentUserId <= 0) {

            Toast.makeText(
                    this,
                    "Please log in to access your calendar.",
                    Toast.LENGTH_SHORT
            ).show();

            // Return to the login screen.
            Intent intent = new Intent(
                    this,
                    MainActivity.class
            );

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
            );

            startActivity(intent);
            finish();
            return;
        }

        // Reference the calendar screen controls.
        calendarGrid = findViewById(R.id.calendarGrid);
        textSelectedDate = findViewById(R.id.textSelectedDate);
        layoutCalendarEvents = findViewById(R.id.layoutCalendarEvents);
        buttonBackHome = findViewById(R.id.buttonBackHome);

        // Connect the custom month navigation controls.
        buttonPreviousMonth = findViewById(R.id.buttonPreviousMonth);
        buttonNextMonth = findViewById(R.id.buttonNextMonth);
        textDisplayedMonth = findViewById(R.id.textDisplayedMonth);

        // Connect the Calendar screen to the existing SQLite database.
        databaseHelper = new DatabaseHelper(this);

        // Initialize the calendar using today's date.
        Calendar today = Calendar.getInstance();

        // Track the month currently displayed on the calendar.
        displayedMonth = (Calendar) today.clone();

        // Set the tracked date to the first day of the month.
        displayedMonth.set(Calendar.DAY_OF_MONTH, 1);

        // Build the calendar grid for the current month.
        populateCalendarGrid();

        // Display the current month and year in the custom header.
        textDisplayedMonth.setText(formatMonthYear(displayedMonth));

        updateSelectedDate(
                today.get(Calendar.YEAR),
                today.get(Calendar.MONTH),
                today.get(Calendar.DAY_OF_MONTH)
        );

        // Navigate to the previous month.
        buttonPreviousMonth.setOnClickListener(view -> {

            displayedMonth.add(Calendar.MONTH, -1);

            updateDisplayedMonth();
        });

        // Navigate to the next month.
        buttonNextMonth.setOnClickListener(view -> {

            displayedMonth.add(Calendar.MONTH, 1);

            updateDisplayedMonth();
        });

        // Return to the previous screen.
        buttonBackHome.setOnClickListener(view -> finish());
    }

    /**
     * Refreshes the selected date's events when the user
     * returns to the Calendar screen.
     */
    @Override
    protected void onResume() {
        super.onResume();

        // Refresh only after the database and calendar are initialized.
        if (databaseHelper != null && !selectedDate.isEmpty()) {
            loadEventsForSelectedDate();
        }
    }

    /**
     * Stores the selected date and updates the screen heading.
     *
     * Calendar months are zero-based, so add one for SQLite.
     */
    private void updateSelectedDate(
            int year,
            int month,
            int dayOfMonth
    ) {

        selectedDate = String.format(
                Locale.US,
                "%04d-%02d-%02d",
                year,
                month + 1,
                dayOfMonth
        );

        // Display the selected month and year above the event list.
        Calendar selectedCalendar = Calendar.getInstance();

        selectedCalendar.set(
                year,
                month,
                dayOfMonth
        );

        String monthHeading = formatMonthYear(selectedCalendar);

        textSelectedDate.setText(
                "Events for " + monthHeading
        );

        // Retrieve events whenever the selected date changes.
        loadEventsForSelectedDate();
    }

    /**
     * Retrieves events scheduled for the selected calendar date.
     * Only events belonging to the signed-in user are included.
     */
    private void loadEventsForSelectedDate() {

        // Remove any events previously displayed.
        layoutCalendarEvents.removeAllViews();

        // Extract the year and month from the selected date.
        int year = Integer.parseInt(
                selectedDate.substring(0, 4)
        );

        int month = Integer.parseInt(
                selectedDate.substring(5, 7)
        );

        // Retrieve all events for the selected month and user.
        try (android.database.Cursor cursor =
                     databaseHelper.getEventsByMonth(
                             currentUserId,
                             year,
                             month
                     )) {

            // Check whether the selected date has any events.
            if (cursor.getCount() == 0) {

                TextView emptyMessage = new TextView(this);
                emptyMessage.setText("No events scheduled for this date.");
                emptyMessage.setTextSize(16);
                emptyMessage.setPadding(8, 16, 8, 16);

                layoutCalendarEvents.addView(emptyMessage);
                return;
            }

            // Display each event scheduled for the selected date.
            while (cursor.moveToNext()) {

                // Retrieve the event information from SQLite.
                int eventId = cursor.getInt(
                        cursor.getColumnIndexOrThrow("event_id")
                );

                String title = cursor.getString(
                        cursor.getColumnIndexOrThrow("title")
                );

                String time = cursor.getString(
                        cursor.getColumnIndexOrThrow("time")
                );

                // Retrieve the event's scheduled date.
                String date = cursor.getString(
                        cursor.getColumnIndexOrThrow("date")
                );

                // Create a vertical container for the event.
                LinearLayout eventLayout = new LinearLayout(this);
                eventLayout.setOrientation(LinearLayout.VERTICAL);
                eventLayout.setPadding(16, 16, 16, 16);

                // Display the event title and scheduled time.
                TextView eventText = new TextView(this);
                eventText.setText(
                        title +
                                "\nDate: " + date +
                                "\nTime: " + time
                );
                eventText.setTextSize(16);

                eventLayout.addView(eventText);

                // Add a button to open the event details.
                Button detailsButton = new Button(this);
                detailsButton.setText("View Details");

                detailsButton.setOnClickListener(view -> {

                    Intent intent = new Intent(
                            CalendarActivity.this,
                            EventDetailsActivity.class
                    );

                    intent.putExtra("EVENT_ID", eventId);
                    intent.putExtra("USER_ID", currentUserId);

                    startActivity(intent);
                });

                eventLayout.addView(detailsButton);

                // Add the completed event to the calendar's event list.
                layoutCalendarEvents.addView(eventLayout);
            }
        }
    }

    /**
     * Updates the calendar and custom month heading
     * when the user navigates between months.
     */
    private void updateDisplayedMonth() {

        // Display the month and year currently being viewed.
        String monthHeading = formatMonthYear(displayedMonth);

        textDisplayedMonth.setText(monthHeading);

        // Rebuild the calendar grid whenever the displayed month changes.
        populateCalendarGrid();

        // Update the date used to retrieve monthly events.
        selectedDate = String.format(
                Locale.US,
                "%04d-%02d-01",
                displayedMonth.get(Calendar.YEAR),
                displayedMonth.get(Calendar.MONTH) + 1
        );

        // Update the heading above the event list.
        textSelectedDate.setText("Events for " + monthHeading);

        // Refresh the event list for the displayed month.
        loadEventsForSelectedDate();
    }

    /**
     * Builds the calendar grid for the currently displayed month.
     */
    private void populateCalendarGrid() {

        // Remove the previous month's calendar dates.
        calendarGrid.removeAllViews();

        // Create a copy so the displayed month is not modified.
        Calendar monthCalendar = (Calendar) displayedMonth.clone();
        monthCalendar.set(Calendar.DAY_OF_MONTH, 1);

        // Display the weekday headings.
        String[] weekdays = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};

        for (String weekday : weekdays) {

            TextView heading = new TextView(this);
            heading.setText(weekday);
            heading.setGravity(android.view.Gravity.CENTER);
            heading.setTextSize(14);
            heading.setTypeface(null, android.graphics.Typeface.BOLD);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);

            calendarGrid.addView(heading, params);
        }

        // Add blank spaces before the first day of the month.
        int firstDayOfWeek = monthCalendar.get(Calendar.DAY_OF_WEEK) - 1;

        for (int i = 0; i < firstDayOfWeek; i++) {

            TextView blankDay = new TextView(this);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);

            calendarGrid.addView(blankDay, params);
        }

        // Add each day of the displayed month.
        int daysInMonth = monthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int day = 1; day <= daysInMonth; day++) {

            TextView dayView = new TextView(this);
            dayView.setText(String.valueOf(day));
            dayView.setGravity(android.view.Gravity.CENTER);
            dayView.setTextSize(16);
            dayView.setPadding(0, 16, 0, 16);

            // Highlight today's date in the calendar.
            Calendar today = Calendar.getInstance();

            if (displayedMonth.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                    && displayedMonth.get(Calendar.MONTH) == today.get(Calendar.MONTH)
                    && day == today.get(Calendar.DAY_OF_MONTH)) {

                dayView.setBackgroundColor(
                        android.graphics.Color.rgb(33, 150, 243)
                );

                dayView.setTextColor(android.graphics.Color.WHITE);
                dayView.setTypeface(null, android.graphics.Typeface.BOLD);
            }

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);

            calendarGrid.addView(dayView, params);
        }
    }

    /**
     * Formats a calendar date as a month and year.
     *
     * @param calendar Calendar containing the date to format.
     * @return Formatted month and year (for example, November 2026).
     */
    private String formatMonthYear(Calendar calendar) {
        return new java.text.SimpleDateFormat(
                "MMMM yyyy",
                Locale.US
        ).format(calendar.getTime());
    }
}