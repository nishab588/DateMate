package com.example.eventtrackingapp_nishabrown;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Manages the SQLite database used by the Date Mate application.
 * The database stores user login credentials and event information.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    // Database information.
    private static final String DATABASE_NAME = "DateMate.db";
    private static final int DATABASE_VERSION = 2;

    // Users table and column names.
    private static final String TABLE_USERS = "users";
    private static final String USER_ID = "user_id";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";

    // Events table and column names.
    private static final String TABLE_EVENTS = "events";
    private static final String EVENT_ID = "event_id";
    private static final String EVENT_USER_ID = "user_id";
    private static final String EVENT_TITLE = "title";
    private static final String EVENT_DATE = "date";
    private static final String EVENT_TIME = "time";
    private static final String EVENT_LOCATION = "location";
    private static final String EVENT_DESCRIPTION = "description";

    /**
     * Creates an instance of the database helper.
     *
     * @param context Current application context.
     */
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Creates the database tables when the database is first initialized.
     *
     * @param db SQLite database instance.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {

        // Create the table used to store application users.
        String createUsersTable =
                "CREATE TABLE " + TABLE_USERS + " (" +
                        USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        USERNAME + " TEXT UNIQUE NOT NULL, " +
                        PASSWORD + " TEXT NOT NULL)";

        // Create the table used to store event information.
        String createEventsTable =
                "CREATE TABLE " + TABLE_EVENTS + " (" +
                        EVENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        EVENT_USER_ID + " INTEGER, " +
                        EVENT_TITLE + " TEXT NOT NULL, " +
                        EVENT_DATE + " TEXT, " +
                        EVENT_TIME + " TEXT, " +
                        EVENT_LOCATION + " TEXT, " +
                        EVENT_DESCRIPTION + " TEXT, " +
                        "FOREIGN KEY (" + EVENT_USER_ID + ") REFERENCES " +
                        TABLE_USERS + "(" + USER_ID + "))";

        db.execSQL(createUsersTable);
        db.execSQL(createEventsTable);
    }

    /**
     * Updates the database structure without deleting existing records.
     *
     * @param db         SQLite database instance.
     * @param oldVersion Previous database version.
     * @param newVersion Updated database version.
     */
    @Override
    public void onUpgrade(
            SQLiteDatabase db,
            int oldVersion,
            int newVersion
    ) {

        // Version 2 adds user ownership to saved events.
        if (oldVersion < 2) {
            db.execSQL(
                    "ALTER TABLE " + TABLE_EVENTS +
                            " ADD COLUMN " + EVENT_USER_ID + " INTEGER"
            );
        }
    }

    /**
     * Creates a new user account.
     *
     * @param username Username selected by the user.
     * @param password Password selected by the user.
     * @return true when the account is successfully created.
     */
    public boolean createUser(
            String username,
            String password
    ) {

        SQLiteDatabase db = getWritableDatabase();

        // Store the new user's credentials.
        ContentValues values = new ContentValues();
        values.put(USERNAME, username);
        values.put(PASSWORD, password);

        // Insert the user into the database.
        long result = db.insert(
                TABLE_USERS,
                null,
                values
        );

        return result != -1;
    }

    /**
     * Checks whether the provided username and password match
     * an existing user account.
     *
     * @param username Username entered on the login screen.
     * @param password Password entered on the login screen.
     * @return true when matching credentials are found.
     */
    public boolean checkUser(
            String username,
            String password
    ) {

        SQLiteDatabase db = getReadableDatabase();

        // Search for a user with the provided username and password.
        Cursor cursor = db.query(
                TABLE_USERS,
                new String[]{USER_ID},
                USERNAME + " = ? AND " + PASSWORD + " = ?",
                new String[]{username, password},
                null,
                null,
                null
        );

        boolean userExists = cursor.getCount() > 0;

        // Release the cursor after the query is complete.
        cursor.close();

        return userExists;
    }

    /**
     * Checks whether a username is already registered.
     *
     * @param username Username to search for.
     * @return true when the username already exists.
     */
    public boolean usernameExists(String username) {

        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.query(
                TABLE_USERS,
                new String[]{USER_ID},
                USERNAME + " = ?",
                new String[]{username},
                null,
                null,
                null
        );

        boolean exists = cursor.getCount() > 0;

        // Release the cursor after the query is complete.
        cursor.close();

        return exists;
    }

    /**
     * Adds a new event to the events table.
     *
     * @param title       Name of the event.
     * @param date        Scheduled event date.
     * @param time        Scheduled event time.
     * @param location    Event location.
     * @param description Additional event details.
     * @return database ID of the new event, or -1 if the insert fails.
     */
    public long createEvent(
            String title,
            String date,
            String time,
            String location,
            String description
    ) {

        SQLiteDatabase db = getWritableDatabase();

        // Store the event information before inserting it into the database.
        ContentValues values = new ContentValues();
        values.put(EVENT_TITLE, title);
        values.put(EVENT_DATE, date);
        values.put(EVENT_TIME, time);
        values.put(EVENT_LOCATION, location);
        values.put(EVENT_DESCRIPTION, description);

        /*
         * SQLite returns the ID assigned to the new row.
         * Date Mate uses this ID to create unique reminder alarms.
         */
        return db.insert(
                TABLE_EVENTS,
                null,
                values
        );
    }

    /**
     * Retrieves the four nearest upcoming events from the database.
     *
     * @return Cursor containing up to four upcoming event records.
     */
    public Cursor getAllEvents() {

        SQLiteDatabase db = getReadableDatabase();

        // Get today's date in YYYY-MM-DD format.
        String today = new java.text.SimpleDateFormat(
                "yyyy-MM-dd",
                java.util.Locale.US
        ).format(new java.util.Date());

        // Return only current or future events, ordered by date and time.
        return db.query(
                TABLE_EVENTS,
                null,
                EVENT_DATE + " >= ?",
                new String[]{today},
                null,
                null,
                EVENT_DATE + " ASC, " + EVENT_TIME + " ASC",
                "4"
        );
    }

    /**
     * Retrieves every saved event from the database.
     *
     * @return Cursor containing all event records ordered by date and time.
     */
    public Cursor getEveryEvent() {

        SQLiteDatabase db = getReadableDatabase();

        /*
         * Return all saved events, including past and future events.
         * This supports the View All Events option on the Home screen.
         */
        return db.query(
                TABLE_EVENTS,
                null,
                null,
                null,
                null,
                null,
                EVENT_DATE + " ASC, " + EVENT_TIME + " ASC"
        );
    }

    /**
     * Searches all saved events by event title.
     *
     * @param searchText Text entered by the user.
     * @return Cursor containing matching event records.
     */
    public Cursor searchEventsByTitle(String searchText) {

        SQLiteDatabase db = getReadableDatabase();

        // Search all events regardless of date so older records
        // can still be located and managed.
        return db.query(
                TABLE_EVENTS,
                null,
                EVENT_TITLE + " LIKE ?",
                new String[]{"%" + searchText + "%"},
                null,
                null,
                EVENT_DATE + " ASC, " + EVENT_TIME + " ASC"
        );
    }

    /**
     * Retrieves one event using its database ID.
     *
     * @param eventId Database ID of the requested event.
     * @return Cursor containing the matching event record.
     */
    public Cursor getEventById(int eventId) {

        SQLiteDatabase db = getReadableDatabase();

        // Search for the event that matches the provided database ID.
        return db.query(
                TABLE_EVENTS,
                null,
                EVENT_ID + " = ?",
                new String[]{String.valueOf(eventId)},
                null,
                null,
                null
        );
    }

    /**
     * Updates an existing event in the database.
     *
     * @param eventId     Database ID of the event being updated.
     * @param title       Updated event title.
     * @param date        Updated event date.
     * @param time        Updated event time.
     * @param location    Updated event location.
     * @param description Updated event description.
     * @return true when the event is successfully updated.
     */
    public boolean updateEvent(
            int eventId,
            String title,
            String date,
            String time,
            String location,
            String description
    ) {

        SQLiteDatabase db = getWritableDatabase();

        // Store the updated event values.
        ContentValues values = new ContentValues();
        values.put(EVENT_TITLE, title);
        values.put(EVENT_DATE, date);
        values.put(EVENT_TIME, time);
        values.put(EVENT_LOCATION, location);
        values.put(EVENT_DESCRIPTION, description);

        int rowsUpdated = db.update(
                TABLE_EVENTS,
                values,
                EVENT_ID + " = ?",
                new String[]{String.valueOf(eventId)}
        );

        return rowsUpdated > 0;
    }

    /**
     * Deletes an event from the database.
     *
     * @param eventId Database ID of the event being deleted.
     * @return true when the event is successfully deleted.
     */
    public boolean deleteEvent(int eventId) {

        SQLiteDatabase db = getWritableDatabase();

        int rowsDeleted = db.delete(
                TABLE_EVENTS,
                EVENT_ID + " = ?",
                new String[]{String.valueOf(eventId)}
        );

        return rowsDeleted > 0;
    }
}