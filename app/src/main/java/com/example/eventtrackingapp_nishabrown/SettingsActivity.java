package com.example.eventtrackingapp_nishabrown;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Handles SMS notification settings for Date Mate.
 * Android SMS permission and the user's in-app notification preference
 * are managed separately so Date Mate can continue to function
 * when SMS reminders are disabled.
 */
public class SettingsActivity extends AppCompatActivity {

    private static final int SMS_PERMISSION_REQUEST_CODE = 100;

    // Stores the user's Date Mate SMS notification preferences.
    private static final String PREFERENCES_NAME = "DateMatePreferences";
    private static final String SMS_ENABLED_KEY = "smsNotificationsEnabled";
    private static final String SMS_PHONE_KEY = "smsPhoneNumber";

    private SharedPreferences sharedPreferences;

    private TextView textSmsStatus;
    private TextView textSavedPhoneNumber;

    private Button buttonEnableSms;
    private Button buttonContinueWithoutSms;
    private Button buttonEditPhoneNumber;
    private Button buttonTestSms;

    private ImageButton buttonMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings_sms);

        // Access the application's saved notification preferences.
        sharedPreferences = getSharedPreferences(
                PREFERENCES_NAME,
                MODE_PRIVATE
        );

        // Reference the SMS settings screen controls.
        textSmsStatus = findViewById(R.id.textPermissionStatus);
        textSavedPhoneNumber = findViewById(R.id.textSavedPhoneNumber);

        buttonEnableSms = findViewById(R.id.buttonEnableSms);

        buttonContinueWithoutSms =
                findViewById(R.id.buttonContinueWithoutSms);

        buttonEditPhoneNumber =
                findViewById(R.id.buttonEditPhoneNumber);

        buttonTestSms =
                findViewById(R.id.buttonTestSms);

        buttonMenu = findViewById(R.id.buttonMenu);

        // Display the saved phone number and current SMS status.
        updatePhoneNumberDisplay();
        updatePermissionStatus();

        // Display the shared navigation menu.
        buttonMenu.setOnClickListener(
                view -> NavigationHelper.showNavigationMenu(
                        SettingsActivity.this,
                        view
                )
        );

        // Enable or disable Date Mate SMS reminders.
        buttonEnableSms.setOnClickListener(view -> {

            boolean notificationsEnabled =
                    sharedPreferences.getBoolean(
                            SMS_ENABLED_KEY,
                            false
                    );

            if (notificationsEnabled) {

                // Confirm before disabling Date Mate SMS reminders.
                showDisableConfirmationDialog();

            } else {

                String savedPhoneNumber =
                        sharedPreferences.getString(
                                SMS_PHONE_KEY,
                                ""
                        );

                if (savedPhoneNumber == null
                        || savedPhoneNumber.trim().isEmpty()) {

                    // Ask for a phone number before enabling SMS reminders.
                    showPhoneNumberDialog(true);

                } else {

                    // A phone number already exists.
                    enableSmsNotifications();
                }
            }
        });

        // Allow the user to change the saved reminder phone number.
        buttonEditPhoneNumber.setOnClickListener(
                view -> showPhoneNumberDialog(false)
        );

        // Return to the previous screen without enabling SMS reminders.
        buttonContinueWithoutSms.setOnClickListener(
                view -> finish()
        );

        // Send an immediate test reminder through Date Mate's SMS reminder chain.
        buttonTestSms.setOnClickListener(view -> {

            boolean notificationsEnabled =
                    sharedPreferences.getBoolean(
                            SMS_ENABLED_KEY,
                            false
                    );

            String savedPhoneNumber =
                    sharedPreferences.getString(
                            SMS_PHONE_KEY,
                            ""
                    );

            if (!notificationsEnabled) {

                Toast.makeText(
                        this,
                        "Enable SMS notifications before sending a test reminder.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            if (savedPhoneNumber == null
                    || savedPhoneNumber.trim().isEmpty()) {

                Toast.makeText(
                        this,
                        "Save a phone number before sending a test reminder.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            String testDate =
                    new SimpleDateFormat(
                            "yyyy-MM-dd",
                            Locale.US
                    ).format(new Date());

            String testTime =
                    new SimpleDateFormat(
                            "HH:mm",
                            Locale.US
                    ).format(new Date());

            ReminderScheduler.sendTestReminder(
                    this,
                    "Test Event",
                    testDate,
                    testTime
            );

            Toast.makeText(
                    this,
                    "Test reminder sent.",
                    Toast.LENGTH_SHORT
            ).show();
        });

        // Prevent screen content from overlapping the system bars.
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.settingsContent),
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
     * Refreshes the SMS settings whenever the user
     * returns to the Settings screen.
     */
    @Override
    protected void onResume() {
        super.onResume();

        updatePhoneNumberDisplay();
        updatePermissionStatus();
    }

    /**
     * Displays a dialog that allows the user to enter
     * or update the phone number used for SMS reminders.
     *
     * @param enableAfterSave true when SMS reminders should
     *                        be enabled after saving the number.
     */
    private void showPhoneNumberDialog(boolean enableAfterSave) {

        // Create the phone number input field for the dialog.
        final EditText phoneInput = new EditText(this);

        phoneInput.setHint("Phone Number");
        phoneInput.setInputType(InputType.TYPE_CLASS_PHONE);

        // Load the currently saved number when editing.
        String savedPhoneNumber =
                sharedPreferences.getString(
                        SMS_PHONE_KEY,
                        ""
                );

        if (savedPhoneNumber != null
                && !savedPhoneNumber.isEmpty()) {

            phoneInput.setText(savedPhoneNumber);

            // Place the cursor at the end of the existing number.
            phoneInput.setSelection(
                    phoneInput.getText().length()
            );
        }

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(
                                enableAfterSave
                                        ? "Enter Phone Number"
                                        : "Edit Phone Number"
                        )
                        .setMessage(
                                "Enter the phone number that should receive Date Mate event reminders."
                        )
                        .setView(phoneInput)
                        .setPositiveButton(
                                "Save",
                                null
                        )
                        .setNegativeButton(
                                "Cancel",
                                (dialogInterface, which) ->
                                        dialogInterface.dismiss()
                        )
                        .create();

        // Override normal Save behavior so an empty number
        // does not automatically close the dialog.
        dialog.setOnShowListener(dialogInterface -> {

            Button saveButton =
                    dialog.getButton(
                            AlertDialog.BUTTON_POSITIVE
                    );

            saveButton.setOnClickListener(view -> {

                String phoneNumber =
                        phoneInput
                                .getText()
                                .toString()
                                .trim();

                if (phoneNumber.isEmpty()) {

                    phoneInput.setError(
                            "Please enter a phone number."
                    );

                    return;
                }

                // Save the phone number for future SMS reminders.
                sharedPreferences
                        .edit()
                        .putString(
                                SMS_PHONE_KEY,
                                phoneNumber
                        )
                        .apply();

                // Refresh the number shown on the Settings screen.
                updatePhoneNumberDisplay();

                Toast.makeText(
                        SettingsActivity.this,
                        "Phone number saved.",
                        Toast.LENGTH_SHORT
                ).show();

                dialog.dismiss();

                if (enableAfterSave) {

                    // Continue enabling SMS notifications
                    // after the number has been saved.
                    enableSmsNotifications();
                }
            });
        });

        dialog.show();
    }

    /**
     * Begins the process of enabling SMS notifications.
     * Android permission is requested only when necessary.
     */
    private void enableSmsNotifications() {

        boolean permissionGranted =
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.SEND_SMS
                ) == PackageManager.PERMISSION_GRANTED;

        if (permissionGranted) {

            // Android permission already exists.
            setSmsNotificationsEnabled(true);

            Toast.makeText(
                    this,
                    "SMS notifications enabled.",
                    Toast.LENGTH_SHORT
            ).show();

        } else {

            // Ask Android for permission to send SMS messages.
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.SEND_SMS
                    },
                    SMS_PERMISSION_REQUEST_CODE
            );
        }
    }

    /**
     * Confirms that the user wants to disable
     * Date Mate SMS reminders.
     */
    private void showDisableConfirmationDialog() {

        new AlertDialog.Builder(this)
                .setTitle("Disable SMS Notifications?")
                .setMessage(
                        "Are you sure you want to stop receiving SMS event reminders?"
                )
                .setPositiveButton(
                        "Confirm",
                        (dialog, which) -> {

                            // Disable reminders without changing
                            // Android SMS permission.
                            setSmsNotificationsEnabled(false);

                            Toast.makeText(
                                    this,
                                    "SMS notifications disabled.",
                                    Toast.LENGTH_SHORT
                            ).show();
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
     * Saves the user's Date Mate SMS notification preference.
     *
     * @param enabled true to enable SMS event reminders.
     */
    private void setSmsNotificationsEnabled(boolean enabled) {

        sharedPreferences
                .edit()
                .putBoolean(
                        SMS_ENABLED_KEY,
                        enabled
                )
                .apply();

        updatePermissionStatus();
    }

    /**
     * Displays the currently saved SMS reminder phone number.
     */
    private void updatePhoneNumberDisplay() {

        String savedPhoneNumber =
                sharedPreferences.getString(
                        SMS_PHONE_KEY,
                        ""
                );

        if (savedPhoneNumber == null
                || savedPhoneNumber.trim().isEmpty()) {

            textSavedPhoneNumber.setText(
                    "No phone number saved"
            );

            // Do not show Edit until a number exists.
            buttonEditPhoneNumber.setVisibility(
                    View.GONE
            );

        } else {

            textSavedPhoneNumber.setText(
                    "SMS Phone Number: " + savedPhoneNumber
            );

            // Allow the saved number to be changed.
            buttonEditPhoneNumber.setVisibility(
                    View.VISIBLE
            );
        }
    }

    /**
     * Handles the user's response to the Android
     * SMS permission request.
     */
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == SMS_PERMISSION_REQUEST_CODE) {

            if (grantResults.length > 0
                    && grantResults[0]
                    == PackageManager.PERMISSION_GRANTED) {

                // Android permission was granted,
                // so enable Date Mate reminders.
                setSmsNotificationsEnabled(true);

                Toast.makeText(
                        this,
                        "SMS notifications enabled.",
                        Toast.LENGTH_SHORT
                ).show();

            } else {

                // Android permission was denied.
                setSmsNotificationsEnabled(false);

                Toast.makeText(
                        this,
                        "SMS permission denied. Event notifications will remain disabled.",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    /**
     * Updates the screen based on Android SMS permission
     * and the user's saved Date Mate notification preference.
     */
    private void updatePermissionStatus() {

        boolean permissionGranted =
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.SEND_SMS
                ) == PackageManager.PERMISSION_GRANTED;

        boolean notificationsEnabled =
                sharedPreferences.getBoolean(
                        SMS_ENABLED_KEY,
                        false
                );

        if (!permissionGranted) {

            // Android permission is unavailable.
            textSmsStatus.setText(
                    "SMS Notifications: Disabled"
            );

            buttonEnableSms.setText(
                    "Enable SMS Notifications"
            );

            buttonContinueWithoutSms.setVisibility(
                    View.VISIBLE
            );

            buttonTestSms.setVisibility(
                    View.GONE
            );

        } else if (notificationsEnabled) {

            // Permission exists and Date Mate reminders are enabled.
            textSmsStatus.setText(
                    "SMS Notifications: Enabled"
            );

            buttonEnableSms.setText(
                    "Disable SMS Notifications"
            );

            buttonContinueWithoutSms.setVisibility(
                    View.GONE
            );

            buttonTestSms.setVisibility(
                    View.VISIBLE
            );

        } else {

            // Permission exists but reminders are turned off in Date Mate.
            textSmsStatus.setText(
                    "SMS Notifications: Disabled"
            );

            buttonEnableSms.setText(
                    "Enable SMS Notifications"
            );

            buttonContinueWithoutSms.setVisibility(
                    View.VISIBLE
            );

            buttonTestSms.setVisibility(
                    View.GONE
            );
        }

        buttonEnableSms.setEnabled(true);
    }
}