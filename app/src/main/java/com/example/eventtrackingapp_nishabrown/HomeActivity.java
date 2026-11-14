package com.example.eventtrackingapp_nishabrown;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Displays the Date Mate home screen and loads saved events
 * from the SQLite database into the event grid.
 *
 * By default, the Home screen displays the four nearest upcoming events.
 * Users can also switch to a complete view of all saved events.
 */
public class HomeActivity extends AppCompatActivity {

    private Button buttonAddEvent;
    private Button buttonViewAllEvents;
    private ImageButton buttonMenu;

    private TextView textUpcomingEvents;
    private EditText editSearchEvents;
    private GridLayout gridUpcomingEvents;

    private DatabaseHelper databaseHelper;

    // Identifies the user currently signed in to DateMate.
    private long currentUserId = -1;

    // Tracks whether the user is viewing every saved event.
    private boolean showingAllEvents = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        // Retrieve the user ID provided during authentication.
        currentUserId = getIntent().getLongExtra("USER_ID", -1);

        // Prevent access when no authenticated user ID is available.
        if (currentUserId <= 0) {

            Toast.makeText(
                    this,
                    "Please log in to access your events.",
                    Toast.LENGTH_SHORT
            ).show();

            // Return to the login screen.
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
            );

            startActivity(intent);
            finish();
            return;
        }

        // Reference the Home screen controls.
        buttonAddEvent =
                findViewById(R.id.buttonAddEvent);

        buttonViewAllEvents =
                findViewById(R.id.buttonViewAllEvents);

        buttonMenu =
                findViewById(R.id.buttonMenu);

        textUpcomingEvents =
                findViewById(R.id.textUpcomingEvents);

        gridUpcomingEvents =
                findViewById(R.id.gridUpcomingEvents);

        editSearchEvents =
                findViewById(R.id.editSearchEvents);

        // Create access to the application's SQLite database.
        databaseHelper =
                new DatabaseHelper(this);

        // Display the shared navigation menu.
        buttonMenu.setOnClickListener(
                view -> NavigationHelper.showNavigationMenu(
                        HomeActivity.this,
                        view
                )
        );

        // Open the Add Event screen for the currently signed-in user.
        buttonAddEvent.setOnClickListener(view -> {

            Intent intent =
                    new Intent(
                            HomeActivity.this,
                            AddEventActivity.class
                    );

            // Pass the current user's ID to the Add Event screen.
            intent.putExtra("USER_ID", currentUserId);

            startActivity(intent);
        });

        /*
         * Toggle between the four nearest upcoming events
         * and every saved event in the database.
         */
        buttonViewAllEvents.setOnClickListener(view -> {

            showingAllEvents = !showingAllEvents;

            // Clear any active search when switching views.
            editSearchEvents.setText("");

            if (showingAllEvents) {

                // Display every event stored in the database.
                loadAllEvents();

                buttonViewAllEvents.setText(
                        "Show Upcoming Events"
                );

                textUpcomingEvents.setText(
                        "All Events"
                );

            } else {

                // Return to the four nearest upcoming events.
                loadEvents();

                buttonViewAllEvents.setText(
                        "View All Events"
                );

                textUpcomingEvents.setText(
                        "Upcoming Events"
                );
            }
        });

        // Search saved events as the user types.
        editSearchEvents.addTextChangedListener(
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence text,
                            int start,
                            int count,
                            int after
                    ) {
                        // No action is required before the text changes.
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence text,
                            int start,
                            int before,
                            int count
                    ) {

                        String searchText =
                                text.toString().trim();

                        if (searchText.isEmpty()) {

                            /*
                             * Return to whichever Home view the user
                             * was using before the search.
                             */
                            if (showingAllEvents) {

                                loadAllEvents();

                            } else {

                                loadEvents();
                            }

                        } else {

                            // Display events matching the search text.
                            searchEvents(searchText);
                        }
                    }

                    @Override
                    public void afterTextChanged(
                            Editable editable
                    ) {
                        // No action is required after the text changes.
                    }
                }
        );

        // Prevent content from overlapping system bars.
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.homeContent),
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
     * Confirms that the user wants to permanently delete an event.
     *
     * @param eventId Database ID of the event being deleted.
     * @param title   Title of the event being deleted.
     */
    private void showDeleteConfirmationDialog(
            int eventId,
            String title
    ) {

        new AlertDialog.Builder(this)
                .setTitle("Delete Event?")
                .setMessage(
                        "Are you sure you want to delete \""
                                + title
                                + "\"?"
                )
                .setPositiveButton(
                        "Delete",
                        (dialog, which) -> {

                            // Delete the event only if it belongs to the current user.
                            boolean eventDeleted =
                                    databaseHelper.deleteEvent(
                                            currentUserId,
                                            eventId
                                    );

                            if (eventDeleted) {

                                // Cancel reminders only after successful deletion.
                                ReminderScheduler.cancelEventReminders(
                                        HomeActivity.this,
                                        eventId
                                );

                                Toast.makeText(
                                        HomeActivity.this,
                                        "Event deleted successfully.",
                                        Toast.LENGTH_SHORT
                                ).show();

                                /*
                                 * Refresh the current Home state after deletion.
                                 */
                                String searchText =
                                        editSearchEvents
                                                .getText()
                                                .toString()
                                                .trim();

                                if (!searchText.isEmpty()) {

                                    searchEvents(searchText);

                                } else if (showingAllEvents) {

                                    loadAllEvents();

                                } else {

                                    loadEvents();
                                }

                            } else {

                                Toast.makeText(
                                        HomeActivity.this,
                                        "Unable to delete event.",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }
                )
                .setNegativeButton(
                        "Cancel",
                        (dialog, which) ->
                                dialog.dismiss()
                )
                .show();
    }

    /**
     * Reloads event information whenever the Home screen becomes active.
     */
    @Override
    protected void onResume() {
        super.onResume();

        // Do not load events without a valid user ID.
        if (currentUserId <= 0) {
            return;
        }

        String searchText =
                editSearchEvents == null
                        ? ""
                        : editSearchEvents
                          .getText()
                          .toString()
                          .trim();

        if (!searchText.isEmpty()) {

            // Restore the current search results.
            searchEvents(searchText);

        } else if (showingAllEvents) {

            // Restore the complete event view.
            loadAllEvents();

        } else {

            // Restore the four-event upcoming view.
            loadEvents();
        }

        /*
         * Keep the View All button and event heading synchronized
         * whenever the user returns to the Home screen.
         */
        updateHomeViewLabels();
    }

    /**
     * Updates the Home screen heading and toggle button
     * to match the currently selected event view.
     */
    private void updateHomeViewLabels() {

        if (buttonViewAllEvents == null
                || textUpcomingEvents == null) {

            return;
        }

        if (showingAllEvents) {

            buttonViewAllEvents.setText(
                    "Show Upcoming Events"
            );

            textUpcomingEvents.setText(
                    "All Events"
            );

        } else {

            buttonViewAllEvents.setText(
                    "View All Events"
            );

            textUpcomingEvents.setText(
                    "Upcoming Events"
            );
        }
    }

    /**
     * Searches the database for events matching the entered title.
     *
     * @param searchText Text entered into the event search field.
     */
    private void searchEvents(
            String searchText
    ) {

        // Remove the currently displayed cards.
        gridUpcomingEvents.removeAllViews();

        Cursor cursor =
                databaseHelper.searchEventsByTitle(
                        currentUserId,
                        searchText
                );

        try {

            while (cursor.moveToNext()) {

                int eventId =
                        cursor.getInt(
                                cursor.getColumnIndexOrThrow(
                                        "event_id"
                                )
                        );

                String title =
                        cursor.getString(
                                cursor.getColumnIndexOrThrow(
                                        "title"
                                )
                        );

                String date =
                        cursor.getString(
                                cursor.getColumnIndexOrThrow(
                                        "date"
                                )
                        );

                String time =
                        cursor.getString(
                                cursor.getColumnIndexOrThrow(
                                        "time"
                                )
                        );

                addEventCard(
                        eventId,
                        title,
                        date,
                        time
                );
            }

        } finally {

            // Release the cursor after processing search results.
            cursor.close();
        }
    }

    /**
     * Retrieves the four nearest upcoming events from SQLite
     * and displays them in the Home screen grid.
     */
    private void loadEvents() {

        // Remove existing event cards before refreshing the grid.
        gridUpcomingEvents.removeAllViews();

        Cursor cursor =
                databaseHelper.getAllEvents(currentUserId);

        try {

            while (cursor.moveToNext()) {

                int eventId =
                        cursor.getInt(
                                cursor.getColumnIndexOrThrow(
                                        "event_id"
                                )
                        );

                String title =
                        cursor.getString(
                                cursor.getColumnIndexOrThrow(
                                        "title"
                                )
                        );

                String date =
                        cursor.getString(
                                cursor.getColumnIndexOrThrow(
                                        "date"
                                )
                        );

                String time =
                        cursor.getString(
                                cursor.getColumnIndexOrThrow(
                                        "time"
                                )
                        );

                addEventCard(
                        eventId,
                        title,
                        date,
                        time
                );
            }

        } finally {

            // Release the cursor after loading upcoming events.
            cursor.close();
        }
    }

    /**
     * Retrieves every saved event from SQLite and displays
     * the complete event database in the Home screen grid.
     */
    private void loadAllEvents() {

        // Remove existing cards before displaying all events.
        gridUpcomingEvents.removeAllViews();

        Cursor cursor =
                databaseHelper.getEveryEvent(currentUserId);

        try {

            while (cursor.moveToNext()) {

                int eventId =
                        cursor.getInt(
                                cursor.getColumnIndexOrThrow(
                                        "event_id"
                                )
                        );

                String title =
                        cursor.getString(
                                cursor.getColumnIndexOrThrow(
                                        "title"
                                )
                        );

                String date =
                        cursor.getString(
                                cursor.getColumnIndexOrThrow(
                                        "date"
                                )
                        );

                String time =
                        cursor.getString(
                                cursor.getColumnIndexOrThrow(
                                        "time"
                                )
                        );

                addEventCard(
                        eventId,
                        title,
                        date,
                        time
                );
            }

        } finally {

            // Release the cursor after loading all events.
            cursor.close();
        }
    }

    /**
     * Creates a visual card for an event and adds it to the grid.
     *
     * The database values remain in yyyy-MM-dd and HH:mm format,
     * while the user sees a friendly date and 12-hour time.
     *
     * @param eventId Database ID of the event.
     * @param title   Event title.
     * @param date    Scheduled event date.
     * @param time    Scheduled event time.
     */
    private void addEventCard(
            int eventId,
            String title,
            String date,
            String time
    ) {

        // Create the event card container.
        LinearLayout eventCard =
                new LinearLayout(this);

        eventCard.setOrientation(
                LinearLayout.VERTICAL
        );

        eventCard.setGravity(
                Gravity.CENTER_HORIZONTAL
        );

        eventCard.setPadding(
                16,
                16,
                16,
                16
        );

        eventCard.setClickable(true);
        eventCard.setFocusable(true);

        // Configure the card for a two-column GridLayout.
        GridLayout.LayoutParams cardParams =
                new GridLayout.LayoutParams();

        cardParams.width = 0;

        cardParams.height =
                GridLayout.LayoutParams.WRAP_CONTENT;

        cardParams.columnSpec =
                GridLayout.spec(
                        GridLayout.UNDEFINED,
                        1f
                );

        cardParams.setMargins(
                8,
                8,
                8,
                8
        );

        eventCard.setLayoutParams(
                cardParams
        );

        // Convert the stored database values for display.
        String displayDate =
                formatDateForDisplay(
                        date
                );

        String displayTime =
                formatTimeForDisplay(
                        date,
                        time
                );

        // Display the event title.
        TextView titleView =
                new TextView(this);

        titleView.setText(title);
        titleView.setTextSize(16);
        titleView.setGravity(Gravity.CENTER);

        // Display the friendly event date.
        TextView dateView =
                new TextView(this);

        dateView.setText(displayDate);
        dateView.setTextSize(14);
        dateView.setGravity(Gravity.CENTER);

        // Display the friendly 12-hour time and timezone.
        TextView timeView =
                new TextView(this);

        timeView.setText(displayTime);
        timeView.setTextSize(14);
        timeView.setGravity(Gravity.CENTER);

        // Create the Edit button.
        Button editButton =
                new Button(this);

        editButton.setText("Edit");
        editButton.setAllCaps(false);

        // Create the Delete button.
        Button deleteButton =
                new Button(this);

        deleteButton.setText("Delete");
        deleteButton.setAllCaps(false);

        // Store the database ID with the event card.
        eventCard.setTag(eventId);

        // Open Event Details for the selected event.
        editButton.setOnClickListener(view -> {

            Intent intent =
                    new Intent(
                            HomeActivity.this,
                            EventDetailsActivity.class
                    );

            // Identify the selected event.
            intent.putExtra(
                    "EVENT_ID",
                    eventId
            );

            // Identify the currently signed-in user.
            intent.putExtra(
                    "USER_ID",
                    currentUserId
            );

            startActivity(intent);
        });

        // Confirm before permanently deleting the selected event.
        deleteButton.setOnClickListener(
                view -> showDeleteConfirmationDialog(
                        eventId,
                        title
                )
        );

        // Add event information and actions to the card.
        eventCard.addView(titleView);
        eventCard.addView(dateView);
        eventCard.addView(timeView);
        eventCard.addView(editButton);
        eventCard.addView(deleteButton);

        // Display the completed event card.
        gridUpcomingEvents.addView(
                eventCard
        );
    }

    /**
     * Converts the SQLite date format into a user-friendly date.
     *
     * Example:
     * 2026-08-14 becomes August 14, 2026.
     *
     * @param storedDate Date stored as yyyy-MM-dd.
     * @return Formatted date for display.
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

                return displayFormat.format(
                        date
                );
            }

        } catch (ParseException exception) {

            // If formatting fails, display the original database value.
            return storedDate;
        }

        return storedDate;
    }

    /**
     * Converts the stored 24-hour time into a 12-hour time
     * with AM/PM and the timezone abbreviation.
     *
     * Example:
     * 16:52 becomes 4:52 PM EDT.
     *
     * @param storedDate Date stored as yyyy-MM-dd.
     * @param storedTime Time stored as HH:mm.
     * @return Formatted time for display.
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

            /*
             * Parsing the date and time together ensures the timezone
             * abbreviation reflects daylight saving time for that date.
             */
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

            // If formatting fails, display the original stored time.
            return storedTime;
        }

        return storedTime;
    }
}