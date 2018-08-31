package de.kinemic.example;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import de.kinemic.sdk.Engine;
import de.kinemic.sdk.Engine.ConnectionState;
import de.kinemic.sdk.EngineHandler;
import de.kinemic.sdk.EnumHelper;
import de.kinemic.sdk.GestureHandler;
import de.kinemic.sdk.SensorHandler;
import javax.annotation.Nullable;

public class MainActivity extends EngineActivity {

  private static final String TAG = MainActivity.class.getSimpleName();

  /** State which indicates which action to perfom on FAB press */
  private boolean mFabDisconnects = false;

  /** FAB to search sensors or disconnect current sensor */
  @BindView(R.id.fab)
  FloatingActionButton mFabButton;

  @BindView(R.id.child_button)
  Button mChildButton;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    ButterKnife.bind(this);

    // This permission is required to find bluetooth devices
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
      }
    }

    Intent intent = getIntent();
    if (intent != null && intent.hasExtra("TITLE")) {
      setTitle(intent.getStringExtra("TITLE"));
      mChildButton.setVisibility(View.GONE);
    }
  }

  @Override
  protected void unregisterHandlers() {
    // unregister handlers on resume
    mEngine.unregister(mEngineHandler);
    mEngine.unregister(mGestureHandler);
    mEngine.unregister(mSensorHandler);
  }

  @Override
  protected void registerHandlers() {
    // register handlers on resume
    mEngine.register(mEngineHandler);
    mEngine.register(mGestureHandler);
    mEngine.register(mSensorHandler);

    updateFabButton(mEngine.getConnectionState());
  }

  private void updateFabButton(@Engine.ConnectionState int connectionState) {
    // Change the action of the FAB depending of the connection state
    mFabDisconnects = connectionState != ConnectionState.DISCONNECTED;
    mFabButton.setImageResource(mFabDisconnects ? R.drawable.ic_close_white_24dp : R.drawable.ic_search_white_24dp);
  }

  private SensorHandler mSensorHandler = new SensorHandler() {
    @Override
    public void onBatteryChanged(int batteryPercent) {
      Snackbar.make(mFabButton, "Battery: " + batteryPercent, Snackbar.LENGTH_SHORT).show();
      Log.i(TAG, "battery changed: " + batteryPercent);
    }

    @Override
    public void onActivationStateChanged(@Engine.ActivationState int state) {
      Snackbar.make(mFabButton, "ActivationState: " + nameOfAS(state), Snackbar.LENGTH_SHORT).show();
      Log.i(TAG, "activation state changed: " + nameOfAS(state));
    }
  };

  private EngineHandler mEngineHandler = new EngineHandler() {
    @Override
    public void onConnectionStateChanged(@Engine.ConnectionState int state) {
      updateFabButton(state);

      Snackbar.make(mFabButton, "ConnectionState: " + EnumHelper.nameFromConnectionState(state), Snackbar.LENGTH_SHORT).show();
      Log.i(TAG, "connection state changed: " + EnumHelper.nameFromConnectionState(state));
    }
  };

  private GestureHandler mGestureHandler = new GestureHandler() {
    @Override
    public void onGesture(@Engine.GestureId int gesture) {
      Snackbar.make(mFabButton, "Gesture: " + EnumHelper.nameFrom(gesture), Snackbar.LENGTH_SHORT).show();
      Log.i(TAG, "got gesture: " + EnumHelper.nameFrom(gesture));
    }
  };

  /** small helper for readable activation states */
  private String nameOfAS(@Engine.ActivationState int state) {
    switch (state) {
      case Engine.ActivationState.ACTIVE:
        return "Active";
      case Engine.ActivationState.INACTIVE:
        return "Inactive";
      default:
        return "Unknown"; // will never happen
    }
  }

  @OnClick(R.id.buzz_button)
  public void buzz() {

    // use vibration as haptic feedback
    mEngine.buzz(400);
  }

  @OnClick(R.id.child_button)
  public void navigateToChild() {

    // change to a new activity to showcase the shared engine
    Intent intent = new Intent(this, MainActivity.class);
    intent.putExtra("TITLE", "Child");
    startActivity(intent);
  }

  @OnCheckedChanged({R.id.red_box, R.id.green_box, R.id.blue_box})
  public void changeColor() {
    boolean red = ((CheckBox) findViewById(R.id.red_box)).isChecked();
    boolean green = ((CheckBox) findViewById(R.id.green_box)).isChecked();
    boolean blue = ((CheckBox) findViewById(R.id.blue_box)).isChecked();

    // use LED as visual feedback
    mEngine.setLed(red, green, blue);
  }

  @OnClick(R.id.fab)
  public void fabButtonClicked(View view) {
    if (mFabDisconnects) {
      Snackbar.make(view, "Disconnect...", Snackbar.LENGTH_SHORT).show();
      mEngine.disconnect();
    } else {
      showSearchDialog();
    }
  }


  /** show a list of sensors and connect to selected one */
  private void showSearchDialog() {
    // List adapter to manage search results. Implements SearchCallback.
    SearchResultAdapter adapter = new SearchResultAdapter(this);

    AlertDialog.Builder builder = new AlertDialog.Builder(this)
        .setIcon(de.kinemic.sdk.R.drawable.ic_search_black_24dp)
        .setTitle("Select Sensor")
        .setPositiveButton("Clear", null)
        .setNegativeButton("Cancel", null)
        .setOnDismissListener(dialogInterface -> mEngine.stopSensorSearch())
        .setAdapter(adapter, (dialog, which) -> {
          // Sensor was selected in list -> connect to it
          mEngine.stopSensorSearch();
          mEngine.connect(adapter.getItem(which).address);
        });

    AlertDialog dialog = builder.show();

    // reuse the positive button to non closing action (needs to be done here to not close)
    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> adapter.clear());

    mEngine.startSensorSearch(adapter);
  }

}
