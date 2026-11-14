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
     * Verifies a user's login credentials using secure password hashing.
     *
     * @param username Username entered on the login screen.
     * @param password Password entered on the login screen.
     * @return true when the credentials are valid.
     */
    public boolean checkUser(
            String username,
            String password
    ) {

        SQLiteDatabase db = getReadableDatabase();

        // Find the registered account using its username.
        try (Cursor cursor = db.query(
                TABLE_USERS,
                new String[]{PASSWORD},
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

            // Retrieve the stored password hash.
            String storedPassword = cursor.getString(
                    cursor.getColumnIndexOrThrow(PASSWORD)
            );

            // Reject missing or invalid password records.
            if (storedPassword == null ||
                    !storedPassword.matches(
                            "^[0-9]+:[A-Za-z0-9+/=]+:[A-Za-z0-9+/=]+$"
                    )) {
                return false;
            }

            // Verify the entered password against the stored PBKDF2 hash.
            return PasswordUtils.verifyPassword(
                    password,
                    storedPassword
            );
        }
    }

    /**
     * Retrieves the database ID associated with a registered username.
     *
     * @param username Username of the registered user.
     * @return The user's ID, or -1 if the account is not found.
     */
    public long getUserId(String username) {

        SQLiteDatabase db = getReadableDatabase();

        try (Cursor cursor = db.query(
                TABLE_USERS,
                new String[]{"user_id"},
                USERNAME + " = ?",
                new String[]{username},
                null,
                null,
                null
        )) {

            if (cursor.moveToFirst()) {
                return cursor.getLong(
                        cursor.getColumnIndexOrThrow("user_id")
                );
            }

            return -1;
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
     * Adds a new event to the account of the currently signed-in user.
     *
     * @param userId      Database ID of the event's owner.
     * @param title       Name of the event.
     * @param date        Scheduled event date.
     * @param time        Scheduled event time.
     * @param location    Event location.
     * @param description Additional event details.
     * @return Database ID of the new event, or -1 if creation fails.
     */
    public long createEvent(
            long userId,
            String title,
            String date,
            String time,
            String location,
            String description
    ) {

        // Do not create events without a valid user ID.
        if (userId <= 0) {
            return -1;
        }

        SQLiteDatabase db = getWritableDatabase();

        // Associate the new event with its owner.
        ContentValues values = new ContentValues();
        values.put(EVENT_USER_ID, userId);
        values.put(EVENT_TITLE, title);
        values.put(EVENT_DATE, date);
        values.put(EVENT_TIME, time);
        values.put(EVENT_LOCATION, location);
        values.put(EVENT_DESCRIPTION, description);

        /*
         * SQLite returns the ID assigned to the new event.
         * DateMate uses this ID to schedule unique reminder alarms.
         */
        return db.insert(
                TABLE_EVENTS,
                null,
                values
        );
    }

    /**
     * Retrieves the four nearest upcoming events belonging to a user.
     *
     * @param userId Database ID of the currently signed-in user.
     * @return Cursor containing up to four upcoming events for that user.
     */
    public Cursor getAllEvents(long userId) {

        SQLiteDatabase db = getReadableDatabase();

        // Get today's date in YYYY-MM-DD format.
        String today = new java.text.SimpleDateFormat(
                "yyyy-MM-dd",
                java.util.Locale.US
        ).format(new java.util.Date());

        // Retrieve only upcoming events belonging to this account.
        return db.query(
                TABLE_EVENTS,
                null,
                EVENT_USER_ID + " = ? AND " + EVENT_DATE + " >= ?",
                new String[]{
                        String.valueOf(userId),
                        today
                },
                null,
                null,
                EVENT_DATE + " ASC, " + EVENT_TIME + " ASC",
                "4"
        );
    }

    /**
     * Retrieves all saved events belonging to the currently signed-in user.
     *
     * @param userId Database ID of the currently signed-in user.
     * @return Cursor containing the user's events ordered by date and time.
     */
    public Cursor getEveryEvent(long userId) {

        SQLiteDatabase db = getReadableDatabase();

        /*
         * Return all past and future events belonging to this user.
         * This supports the View All Events option on the Home screen.
         */
        return db.query(
                TABLE_EVENTS,
                null,
                EVENT_USER_ID + " = ?",
                new String[]{
                        String.valueOf(userId)
                },
                null,
                null,
                EVENT_DATE + " ASC, " + EVENT_TIME + " ASC"
        );
    }

    /**
     * Searches saved events by title for the currently signed-in user.
     *
     * @param userId Database ID of the currently signed-in user.
     * @param searchText Text entered by the user.
     * @return Cursor containing matching events belonging to the user.
     */
    public Cursor searchEventsByTitle(long userId, String searchText) {

        SQLiteDatabase db = getReadableDatabase();

        /*
         * Search past and future events belonging to this user.
         * Partial title matches are supported.
         */
        return db.query(
                TABLE_EVENTS,
                null,
                EVENT_USER_ID + " = ? AND " + EVENT_TITLE + " LIKE ?",
                new String[]{
                        String.valueOf(userId),
                        "%" + searchText + "%"
                },
                null,
                null,
                EVENT_DATE + " ASC, " + EVENT_TIME + " ASC"
        );
    }

    /**
     * Retrieves an event only if it belongs to the signed-in user.
     *
     * @param userId Database ID of the currently signed-in user.
     * @param eventId Database ID of the requested event.
     * @return Cursor containing the matching event, if authorized.
     */
    public Cursor getEventById(long userId, int eventId) {

        SQLiteDatabase db = getReadableDatabase();

        // Retrieve the event only when both IDs match.
        return db.query(
                TABLE_EVENTS,
                null,
                EVENT_ID + " = ? AND " + EVENT_USER_ID + " = ?",
                new String[]{
                        String.valueOf(eventId),
                        String.valueOf(userId)
                },
                null,
                null,
                null
        );
    }

    /**
     * Updates an existing event only if it belongs to the current user.
     *
     * @param userId      Database ID of the currently signed-in user.
     * @param eventId     Database ID of the event being updated.
     * @param title       Updated event title.
     * @param date        Updated event date.
     * @param time        Updated event time.
     * @param location    Updated event location.
     * @param description Updated event description.
     * @return true when the event is successfully updated.
     */
    public boolean updateEvent(
            long userId,
            int eventId,
            String title,
            String date,
            String time,
            String location,
            String description
    ) {

        // Reject invalid user or event IDs.
        if (userId <= 0 || eventId <= 0) {
            return false;
        }

        SQLiteDatabase db = getWritableDatabase();

        // Store the updated event values.
        ContentValues values = new ContentValues();
        values.put(EVENT_TITLE, title);
        values.put(EVENT_DATE, date);
        values.put(EVENT_TIME, time);
        values.put(EVENT_LOCATION, location);
        values.put(EVENT_DESCRIPTION, description);

        // Update only when the event belongs to the specified user.
        int rowsUpdated = db.update(
                TABLE_EVENTS,
                values,
                EVENT_ID + " = ? AND " + EVENT_USER_ID + " = ?",
                new String[]{
                        String.valueOf(eventId),
                        String.valueOf(userId)
                }
        );

        return rowsUpdated > 0;
    }

    /**
     * Deletes an event only if it belongs to the current user.
     *
     * @param userId  Database ID of the currently signed-in user.
     * @param eventId Database ID of the event being deleted.
     * @return true when the event is successfully deleted.
     */
    public boolean deleteEvent(long userId, int eventId) {

        // Reject invalid user or event IDs.
        if (userId <= 0 || eventId <= 0) {
            return false;
        }

        SQLiteDatabase db = getWritableDatabase();

        // Delete only when the event belongs to the specified user.
        int rowsDeleted = db.delete(
                TABLE_EVENTS,
                EVENT_ID + " = ? AND " + EVENT_USER_ID + " = ?",
                new String[]{
                        String.valueOf(eventId),
                        String.valueOf(userId)
                }
        );

        return rowsDeleted > 0;
    }
}