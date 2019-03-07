package de.kinemic.example;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.Toast;

import com.github.barteksc.pdfviewer.PDFView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import de.kinemic.gesture.ActivationState;
import de.kinemic.gesture.ConnectionReason;
import de.kinemic.gesture.ConnectionState;
import de.kinemic.gesture.Engine;
import de.kinemic.gesture.Gesture;
import de.kinemic.gesture.OnActivationStateChangeListener;
import de.kinemic.gesture.OnBatteryChangeListener;
import de.kinemic.gesture.OnConnectionStateChangeListener;
import de.kinemic.gesture.OnGestureListener;
import de.kinemic.gesture.OnStreamQualityChangeListener;
import de.kinemic.gesture.common.EngineProvider;
import de.kinemic.gesture.common.fragments.BandFloatingActionButtonFragment;
import de.kinemic.gesture.common.fragments.BandMenuFragment;
import de.kinemic.gesture.common.fragments.GestureFloatingActionButtonFragment;

public class MainActivity extends AppCompatActivity implements OnActivationStateChangeListener,
        OnBatteryChangeListener, OnConnectionStateChangeListener, OnGestureListener, OnStreamQualityChangeListener {

  private static final String TAG = MainActivity.class.getSimpleName();

  private Engine mEngine;

  private PDFViewAirmouseAdapter mAirmouseAdapter;

  @BindView(R.id.fabSensor) FloatingActionButton mFabButton;
  @BindView(R.id.fabGestureDark) FloatingActionButton mFabGestureDark;
  @BindView(R.id.pdfView) PDFView mPdfView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    ButterKnife.bind(this);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
      }
    }

    mEngine = ((EngineProvider) getApplication()).getEngine();

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
            .onDraw(mAirmouseAdapter) /* debug */
            .load();
  }

  @Override
  protected void onPause() {
    mEngine.unregisterOnBatteryChangeListener(this);
    mEngine.unregisterOnStreamQualityChangeListener(this);
    mEngine.unregisterOnActivationStateChangeListener(this);
    mEngine.unregisterOnConnectionStateChangeListener(this);
    mEngine.unregisterOnGestureListener(this);
    mEngine = null;

    super.onPause();
  }

  @Override
  protected void onResume() {
    super.onResume();

    mEngine = ((EngineProvider) getApplication()).getEngine();
    mEngine.registerOnBatteryChangeListener(this);
    mEngine.registerOnStreamQualityChangeListener(this);
    mEngine.registerOnActivationStateChangeListener(this);
    mEngine.registerOnConnectionStateChangeListener(this);
    mEngine.registerOnGestureListener(this);
  }

  @Override
  public void onBatteryChanged(int batteryPercent) {
    Snackbar.make(mFabButton, "Battery: " + batteryPercent, Snackbar.LENGTH_SHORT).show();
    Log.i(TAG, "battery changed: " + batteryPercent);
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
    mEngine.buzz(150);
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

  @OnClick(R.id.buzz_button)
  public void buzz() {
    mEngine.buzz(300);
  }

  @OnCheckedChanged({R.id.red_box, R.id.green_box, R.id.blue_box})
  public void changeColor() {
    boolean red = ((CheckBox) findViewById(R.id.red_box)).isChecked();
    boolean green = ((CheckBox) findViewById(R.id.green_box)).isChecked();
    boolean blue = ((CheckBox) findViewById(R.id.blue_box)).isChecked();

    mEngine.setLed(red, green, blue);
  }

}
