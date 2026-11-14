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
    private static final int DATABASE_VERSION = 3;

    // Users table and column names.
    private static final String TABLE_USERS = "users";
    private static final String USER_ID = "user_id";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";

    // Additional user registration fields.
    private static final String FIRST_NAME = "first_name";
    private static final String LAST_NAME = "last_name";
    private static final String EMAIL = "email";
    private static final String PHONE = "phone";

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
                        FIRST_NAME + " TEXT, " +
                        LAST_NAME + " TEXT, " +
                        EMAIL + " TEXT UNIQUE, " +
                        PHONE + " TEXT, " +
                        USERNAME + " TEXT UNIQUE, " +
                        PASSWORD + " TEXT)";

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
     * Updates the database structure while preserving existing data.
     *
     * @param db         SQLite database instance.
     * @param oldVersion Previous database version.
     * @param newVersion Updated database version.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        // Version 2: Add user ownership to events.
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_EVENTS +
                    " ADD COLUMN " + EVENT_USER_ID + " INTEGER");
        }

        // Version 3: Add additional user registration information.
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + TABLE_USERS +
                    " ADD COLUMN " + FIRST_NAME + " TEXT");

            db.execSQL("ALTER TABLE " + TABLE_USERS +
                    " ADD COLUMN " + LAST_NAME + " TEXT");

            db.execSQL("ALTER TABLE " + TABLE_USERS +
                    " ADD COLUMN " + EMAIL + " TEXT");

            db.execSQL("ALTER TABLE " + TABLE_USERS +
                    " ADD COLUMN " + PHONE + " TEXT");

            // Prevent duplicate email addresses regardless of capitalization.
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email " +
                    "ON " + TABLE_USERS + "(" + EMAIL + " COLLATE NOCASE)");
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

        // Generate a secure password hash before storing the account.
        String hashedPassword = PasswordUtils.hashPassword(password);

        // Store the username and hashed password.
        ContentValues values = new ContentValues();
        values.put(USERNAME, username);
        values.put(PASSWORD, hashedPassword);

        // Insert the user into the database.
        long result = db.insert(
                TABLE_USERS,
                null,
                values
        );

        return result != -1;
    }


    /**
     * Creates a new user account with complete registration information.
     * The password is securely hashed before being saved.
     *
     * @param firstName User's first name.
     * @param lastName User's optional last name.
     * @param email User's email address.
     * @param phone User's phone number.
     * @param username User's selected username.
     * @param password User's selected password.
     * @return New user's database ID, or -1 if registration fails.
     */
    public long createUser(
            String firstName,
            String lastName,
            String email,
            String phone,
            String username,
            String password
    ) {

        SQLiteDatabase db = getWritableDatabase();

        // Securely hash the password before storing it.
        String hashedPassword = PasswordUtils.hashPassword(password);

        // Prepare the registration information for the database.
        ContentValues values = new ContentValues();
        values.put(FIRST_NAME, firstName);
        values.put(LAST_NAME, lastName);
        values.put(EMAIL, email);
        values.put(PHONE, phone);
        values.put(USERNAME, username);
        values.put(PASSWORD, hashedPassword);

        // Insert the new account and return its database ID.
        return db.insert(
                TABLE_USERS,
                null,
                values
        );
    }

    /**
     * Verifies a user's login credentials.
     * Existing plain-text passwords are upgraded to secure hashes
     * after a successful login.
     *
     * @param username Username entered on the login screen.
     * @param password Password entered on the login screen.
     * @return true when the credentials are valid.
     */
    public boolean checkUser(
            String username,
            String password
    ) {

        SQLiteDatabase db = getWritableDatabase();

        // Find the account using its username.
        try (Cursor cursor = db.query(
                TABLE_USERS,
                new String[]{USER_ID, PASSWORD},
                USERNAME + " = ?",
                new String[]{username},
                null,
                null,
                null
        )) {

            // Reject usernames that are not registered.
            if (!cursor.moveToFirst()) {
                return false;
            }

            long userId = cursor.getLong(
                    cursor.getColumnIndexOrThrow(USER_ID)
            );

            String storedPassword = cursor.getString(
                    cursor.getColumnIndexOrThrow(PASSWORD)
            );

            // Verify passwords already stored using PBKDF2.
            if (storedPassword != null &&
                    storedPassword.matches("^[0-9]+:[A-Za-z0-9+/=]+:[A-Za-z0-9+/=]+$")) {

                return PasswordUtils.verifyPassword(
                        password,
                        storedPassword
                );
            }

            // Support an existing account with a plain-text password.
            if (storedPassword != null &&
                    storedPassword.equals(password)) {

                // Replace the old password with a secure hash.
                ContentValues values = new ContentValues();
                values.put(
                        PASSWORD,
                        PasswordUtils.hashPassword(password)
                );

                int updatedRows = db.update(
                        TABLE_USERS,
                        values,
                        USER_ID + " = ? AND " + PASSWORD + " = ?",
                        new String[]{
                                String.valueOf(userId),
                                storedPassword
                        }
                );

                return updatedRows == 1;
            }

            return false;
        }
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
     * Checks whether an email address is already registered.
     *
     * @param email Email address to search for.
     * @return true if the email address already exists.
     */
    public boolean emailExists(String email) {

        SQLiteDatabase db = getReadableDatabase();

        // Search for an existing account with the same email address.
        try (Cursor cursor = db.query(
                TABLE_USERS,
                new String[]{USER_ID},
                EMAIL + " = ? COLLATE NOCASE",
                new String[]{email},
                null,
                null,
                null
        )) {

            // Return true if a matching email address is found.
            return cursor.moveToFirst();
        }
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