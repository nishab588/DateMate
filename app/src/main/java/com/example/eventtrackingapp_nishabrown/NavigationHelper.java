package com.example.eventtrackingapp_nishabrown;

import android.content.Intent;
import android.view.View;
import android.widget.PopupMenu;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Provides consistent hamburger menu navigation throughout Date Mate.
 */
public final class
NavigationHelper {

    /**
     * Prevents creation of NavigationHelper objects.
     */
    private NavigationHelper() {
    }

    /**
     * Displays the Date Mate navigation menu.
     *
     * @param activity   Activity currently displaying the menu.
     * @param anchorView View used to anchor the popup menu.
     */
    public static void showNavigationMenu(
            AppCompatActivity activity,
            View anchorView
    ) {

        PopupMenu popupMenu = new PopupMenu(activity, anchorView);

        // Load the shared navigation menu resource.
        popupMenu.getMenuInflater().inflate(
                R.menu.navigation_menu,
                popupMenu.getMenu()
        );

        // Respond to the selected navigation option.
        popupMenu.setOnMenuItemClickListener(item -> {

            int itemId = item.getItemId();

            if (itemId == R.id.menuHome) {

                // Return to Home unless the user is already there.
                if (!(activity instanceof HomeActivity)) {

                    Intent intent = new Intent(
                            activity,
                            HomeActivity.class
                    );

                    intent.addFlags(
                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    | Intent.FLAG_ACTIVITY_SINGLE_TOP
                    );

                    activity.startActivity(intent);
                    activity.finish();
                }

                return true;

            } else if (itemId == R.id.menuAddEvent) {

                // Open Add Event unless it is already being displayed.
                if (!(activity instanceof AddEventActivity)) {

                    Intent intent = new Intent(
                            activity,
                            AddEventActivity.class
                    );

                    activity.startActivity(intent);
                }

                return true;

            } else if (itemId == R.id.menuSettings) {

                // Open Settings unless it is already being displayed.
                if (!(activity instanceof SettingsActivity)) {

                    Intent intent = new Intent(
                            activity,
                            SettingsActivity.class
                    );

                    activity.startActivity(intent);
                }

                return true;

            } else if (itemId == R.id.menuLogout) {

                // Return to Login and clear all authenticated activities.
                Intent intent = new Intent(
                        activity,
                        MainActivity.class
                );

                intent.setFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_CLEAR_TASK
                );

                activity.startActivity(intent);

                return true;
            }

            return false;
        });

        popupMenu.show();
    }
}