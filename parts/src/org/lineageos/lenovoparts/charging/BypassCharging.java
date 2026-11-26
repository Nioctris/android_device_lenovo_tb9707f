/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lenovoparts.charging;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import org.lineageos.lenovoparts.R;

/**
 * Manual bypass charging: the charger powers the tablet and the battery
 * is not charged, at any charge level. It is a separate kernel vote from
 * the charge limit of Lineage charging control, so the two do not fight.
 * While it is on, an ongoing notification says so.
 */
public final class BypassCharging {

    public static final String SETTING = "lenovo_bypass_charging";
    public static final String ACTION_DISABLE =
            "org.lineageos.lenovoparts.action.DISABLE_BYPASS_CHARGING";

    private static final String NODE = "/sys/class/power_supply/battery/bypass_charging";
    private static final String CHANNEL = "bypass_charging";
    private static final int NOTIFICATION_ID = 1;

    private BypassCharging() {}

    public static boolean isEnabled(ContentResolver cr) {
        return Settings.System.getInt(cr, SETTING, 0) == 1;
    }

    public static void setEnabled(Context context, boolean enabled) {
        Settings.System.putInt(context.getContentResolver(), SETTING, enabled ? 1 : 0);
        Sysfs.write(NODE, enabled ? "1" : "0");
        updateNotification(context, enabled);
    }

    public static void restore(Context context) {
        if (isEnabled(context.getContentResolver())) {
            Sysfs.write(NODE, "1");
            updateNotification(context, true);
        }
    }

    private static void updateNotification(Context context, boolean enabled) {
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (!enabled) {
            nm.cancel(NOTIFICATION_ID);
            return;
        }

        nm.createNotificationChannel(new NotificationChannel(CHANNEL,
                context.getString(R.string.bypass_charging_title),
                NotificationManager.IMPORTANCE_LOW));

        PendingIntent open = PendingIntent.getActivity(context, 0,
                new Intent(context, ChargingActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE);
        PendingIntent disable = PendingIntent.getBroadcast(context, 0,
                new Intent(ACTION_DISABLE).setClass(context, BypassChargingReceiver.class),
                PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new Notification.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_bypass_charging)
                .setContentTitle(context.getString(R.string.bypass_charging_notification_title))
                .setContentText(context.getString(R.string.bypass_charging_notification_text))
                .setContentIntent(open)
                .addAction(new Notification.Action.Builder(null,
                        context.getString(R.string.bypass_charging_notification_disable),
                        disable).build())
                .setOngoing(true)
                .setShowWhen(false)
                .setLocalOnly(true)
                .build();
        nm.notify(NOTIFICATION_ID, notification);
    }
}
