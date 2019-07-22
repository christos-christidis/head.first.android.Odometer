package com.packtpub.odometer;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.widget.TextView;

import java.util.Locale;

public class MainActivity extends Activity {

    private static final int NOTIFICATION_ID = 423;
    private final int PERMISSION_REQUEST_CODE = 698;

    private OdometerService mOdometer;
    private boolean mBound = false;

    private final Handler mHandler = new Handler();

    // SOS: when the activity restarts, I want to show the distance so far, while waiting for the
    // service to connect. This is accomplished by saving/restoring this field
    private long mCachedDistanceInMeters;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            OdometerService.OdometerBinder odometerBinder = (OdometerService.OdometerBinder) binder;
            mOdometer = odometerBinder.getOdometer();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState != null) {
            mCachedDistanceInMeters = savedInstanceState.getLong("mCachedDistanceInMeters");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        displayDistance();

        if (ContextCompat.checkSelfPermission(this, OdometerService.PERMISSION_STRING) ==
                PackageManager.PERMISSION_GRANTED) {
            connectToService();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{OdometerService.PERMISSION_STRING}, PERMISSION_REQUEST_CODE);
        }
    }

    private void connectToService() {
        Intent intent = new Intent(this, OdometerService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putLong("mCachedDistanceInMeters", mCachedDistanceInMeters);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mBound) {
            unbindService(connection);
            mBound = false;
        }

        mHandler.removeCallbacksAndMessages(null);
    }

    // SOS: requestPermissions() is asynchronous (this is the callback)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                connectToService();
            } else {
                showNotification();
            }
        }
    }

    private void showNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "MY_CHANNEL_ID")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.permission_denied))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVibrate(new long[]{1000, 1000})
                .setAutoCancel(true);

        // SOS: Clicking on the notification will restart app & request permission again
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private void displayDistance() {
        final TextView distanceView = findViewById(R.id.distance);

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mBound && mOdometer != null) {
                    mCachedDistanceInMeters = mOdometer.getDistance();
                }

                String distanceStr;
                if (mCachedDistanceInMeters < 1000) {
                    distanceStr = String.format(Locale.getDefault(), "%d m", mCachedDistanceInMeters);
                } else {
                    distanceStr = String.format(Locale.getDefault(), "%.1f km", mCachedDistanceInMeters / 1000.0);
                }
                distanceView.setText(distanceStr);
                mHandler.postDelayed(this, 1000);
            }
        });
    }
}
