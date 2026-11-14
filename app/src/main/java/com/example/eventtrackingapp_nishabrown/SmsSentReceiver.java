package com.example.eventtrackingapp_nishabrown;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;
import android.widget.Toast;

/**
 * Displays the result of an SMS send attempt.
 * This makes SMS testing observable in the Android Emulator.
 */
public class SmsSentReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        String message;

        switch (getResultCode()) {

            case Activity.RESULT_OK:
                message = "SMS send result: Sent successfully.";
                break;

            case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                message = "SMS send result: Generic failure.";
                break;

            case SmsManager.RESULT_ERROR_NO_SERVICE:
                message = "SMS send result: No cellular service.";
                break;

            case SmsManager.RESULT_ERROR_NULL_PDU:
                message = "SMS send result: Invalid SMS data.";
                break;

            case SmsManager.RESULT_ERROR_RADIO_OFF:
                message = "SMS send result: Cellular radio is off.";
                break;

            case SmsManager.RESULT_ERROR_LIMIT_EXCEEDED:
                message = "SMS send result: SMS limit exceeded.";
                break;

            default:
                message =
                        "SMS send result: Failed with code "
                                + getResultCode()
                                + ".";
                break;
        }

        Toast.makeText(
                context,
                message,
                Toast.LENGTH_LONG
        ).show();
    }
}