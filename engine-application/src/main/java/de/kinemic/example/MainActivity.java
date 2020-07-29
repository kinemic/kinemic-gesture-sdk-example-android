package de.kinemic.example;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import com.github.barteksc.pdfviewer.PDFView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import de.kinemic.gesture.ActivationState;
import de.kinemic.gesture.ConnectionReason;
import de.kinemic.gesture.ConnectionState;
import de.kinemic.gesture.EngineDevelop;
import de.kinemic.gesture.Gesture;
import de.kinemic.gesture.OnActivationStateChangeListener;
import de.kinemic.gesture.OnBatteryChangeListener;
import de.kinemic.gesture.OnConnectionStateChangeListener;
import de.kinemic.gesture.OnDigitListener;
import de.kinemic.gesture.OnGestureListener;
import de.kinemic.gesture.OnStreamQualityChangeListener;
import de.kinemic.gesture.common.EngineActivity;
import de.kinemic.gesture.common.fragments.BandFloatingActionButtonFragment;
import de.kinemic.gesture.common.fragments.BandMenuFragment;
import de.kinemic.gesture.common.fragments.GestureFloatingActionButtonFragment;

public class MainActivity extends EngineActivity implements OnActivationStateChangeListener,
        OnBatteryChangeListener, OnConnectionStateChangeListener, OnGestureListener, OnStreamQualityChangeListener,
        OnDigitListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private EngineDevelop mEngine;

    private PDFViewAirmouseAdapter mAirmouseAdapter;

    private FloatingActionButton mFabButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mFabButton = findViewById(R.id.fabSensor);
        FloatingActionButton mFabGestureDark = findViewById(R.id.fabGestureDark);
        PDFView mPdfView = findViewById(R.id.pdfView);

        findViewById(R.id.buzz_button).setOnClickListener(vibration_button_listener);
        findViewById(R.id.red_box).setOnClickListener(color_box_listener);
        findViewById(R.id.green_box).setOnClickListener(color_box_listener);
        findViewById(R.id.blue_box).setOnClickListener(color_box_listener);

        mEngine = (EngineDevelop) getEngine();

        if (savedInstanceState == null) {
            // this fragment manages a sensor menu icon which depicts the sensor state with its icon and shows a info dialog on click
            getSupportFragmentManager().beginTransaction().add(BandMenuFragment.newInstance(BandMenuFragment.ConnectMode.CHOOSE), "sensor_menu").commit();
        }

        if (savedInstanceState == null) {
            // this fragment manages a sensor fab icon which depicts the sensor state with its icon and shows a info dialog on click
            BandFloatingActionButtonFragment bandFabFragment = BandFloatingActionButtonFragment.newInstance(BandFloatingActionButtonFragment.ConnectMode.CHOOSE);
            getSupportFragmentManager().beginTransaction().add(bandFabFragment, "band_fab").commit();
            bandFabFragment.setFloatingActionButton(mFabButton);

            // this fragment manages a gesture fab and depicts gestures with it's assigned floating action button
            GestureFloatingActionButtonFragment gestureFabFragmentDark = GestureFloatingActionButtonFragment.newInstance(true);
            getSupportFragmentManager().beginTransaction().add(gestureFabFragmentDark, "gesture_fab_dark").commit();
            gestureFabFragmentDark.setFloatingActionButton(mFabGestureDark);
        } else {
            BandFloatingActionButtonFragment bandFabFragment = (BandFloatingActionButtonFragment) getSupportFragmentManager().findFragmentByTag("band_fab");
            if (bandFabFragment != null) bandFabFragment.setFloatingActionButton(mFabButton);

            GestureFloatingActionButtonFragment gestureFabFragmentDark = (GestureFloatingActionButtonFragment) getSupportFragmentManager().findFragmentByTag("gesture_fab_dark");
            if (gestureFabFragmentDark != null) gestureFabFragmentDark.setFloatingActionButton(mFabGestureDark);
        }

        mAirmouseAdapter = new PDFViewAirmouseAdapter(this, mEngine, mPdfView);

        mPdfView.fromAsset("Sample.pdf")
                .pages(0)
                .onDraw(mAirmouseAdapter)
                .load();
    }

    @Override
    protected void onPause() {
        mEngine.unregisterOnBatteryChangeListener(this);
        mEngine.unregisterOnStreamQualityChangeListener(this);
        mEngine.unregisterOnActivationStateChangeListener(this);
        mEngine.unregisterOnConnectionStateChangeListener(this);
        mEngine.unregisterOnGestureListener(this);
        mEngine.unregisterOnDigitListener(this);
        mEngine = null;

        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        mEngine = (EngineDevelop) getEngine();
        checkPermissions();

        mEngine.registerOnBatteryChangeListener(this);
        mEngine.registerOnStreamQualityChangeListener(this);
        mEngine.registerOnActivationStateChangeListener(this);
        mEngine.registerOnConnectionStateChangeListener(this);
        mEngine.registerOnGestureListener(this);
        mEngine.registerOnDigitListener(this);
    }

    @Override
    public void onBatteryChanged(int batteryPercent, boolean charging, boolean powered) {
        Snackbar.make(mFabButton, "Battery: " + batteryPercent, Snackbar.LENGTH_SHORT).show();
        Log.i(TAG, "battery changed: " + batteryPercent + " (charging: " + charging + ")");
    }

    @Override
    public void onStreamQualityChanged(int quality) {
        Snackbar.make(mFabButton, "Stream Quality: " + quality, Snackbar.LENGTH_SHORT).show();
        Log.i(TAG, "Stream Quality changed: " + quality);
    }

    @Override
    public void onActivationStateChanged(@NonNull ActivationState state) {
        Log.i(TAG, "activation state changed: " + state);
    }
    @Override
    public void onConnectionStateChanged(@NonNull ConnectionState state, @NonNull ConnectionReason reason) {
        Log.i(TAG, "connection state changed: " + state.toString() + " (" + reason.toString() + ")");
    }

    @Override
    public void onGesture(@NonNull Gesture gesture) {
        mEngine.vibrate(150);
        Log.i(TAG, "got gesture: " + gesture);

        if (gesture == Gesture.ROTATE_RL) {
            if (mAirmouseAdapter.isAirmouseActive()) return;
            mAirmouseAdapter.setAirmouseActive(true);
            Toast.makeText(this, "AirMouse started", Toast.LENGTH_SHORT).show();
        } else if (gesture == Gesture.ROTATE_LR) {
            if (!mAirmouseAdapter.isAirmouseActive()) return;
            mAirmouseAdapter.setAirmouseActive(false);
            Toast.makeText(this, "AirMouse stopped", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDigit(int digit) {
        mEngine.vibrate(150);
        Log.i(TAG, "got digit: " + digit);
        Toast.makeText(this, "Digit: " + digit, Toast.LENGTH_SHORT).show();
    }

    private View.OnClickListener vibration_button_listener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mEngine.vibrate(300);
        }
    };

    private View.OnClickListener color_box_listener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            boolean red = ((CheckBox) findViewById(R.id.red_box)).isChecked();
            boolean green = ((CheckBox) findViewById(R.id.green_box)).isChecked();
            boolean blue = ((CheckBox) findViewById(R.id.blue_box)).isChecked();

            mEngine.setLed(red, green, blue);
        }
    };

}
