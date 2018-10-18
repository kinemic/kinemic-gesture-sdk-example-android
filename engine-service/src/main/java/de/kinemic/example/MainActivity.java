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
import de.kinemic.gesture.ActivationState;
import de.kinemic.gesture.ConnectionState;
import de.kinemic.gesture.EnumNames;
import de.kinemic.gesture.OnActivationStateChangeListener;
import de.kinemic.gesture.OnBatteryChangeListener;
import de.kinemic.gesture.OnConnectionStateChangeListener;
import de.kinemic.gesture.OnGestureListener;
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
    mEngine.unregisterOnConnectionStateChangeListener(mConnectionStateListener);
    mEngine.unregisterOnGestureListener(mGestureListener);
    mEngine.unregisterOnActivationStateChangeListener(mActivationStateChangeListener);
    mEngine.unregisterOnBatteryChangeListener(mBatteryChangeListener);
  }

  @Override
  protected void registerHandlers() {
    // register handlers on resume
    mEngine.registerOnConnectionStateChangeListener(mConnectionStateListener);
    mEngine.registerOnGestureListener(mGestureListener);
    mEngine.registerOnActivationStateChangeListener(mActivationStateChangeListener);
    mEngine.registerOnBatteryChangeListener(mBatteryChangeListener);

    updateFabButton(mEngine.getConnectionState());
  }

  private void updateFabButton(@ConnectionState int connectionState) {
    // Change the action of the FAB depending of the connection state
    mFabDisconnects = connectionState != ConnectionState.DISCONNECTED;
    mFabButton.setImageResource(mFabDisconnects ? R.drawable.ic_close_white_24dp : R.drawable.ic_search_white_24dp);
  }

  private OnBatteryChangeListener mBatteryChangeListener = new OnBatteryChangeListener() {
    @Override
    public void onBatteryChanged(int batteryPercent) {
      Snackbar.make(mFabButton, "Battery: " + batteryPercent, Snackbar.LENGTH_SHORT).show();
      Log.i(TAG, "battery changed: " + batteryPercent);
    }
  };

  private OnActivationStateChangeListener mActivationStateChangeListener = new OnActivationStateChangeListener() {
    @Override
    public void onActivationStateChanged(int state) {
      Snackbar.make(mFabButton, "ActivationState: " + nameOfAS(state), Snackbar.LENGTH_SHORT).show();
      Log.i(TAG, "activation state changed: " + nameOfAS(state));
    }
  };

  private OnConnectionStateChangeListener mConnectionStateListener = new OnConnectionStateChangeListener() {
    @Override
    public void onConnectionStateChanged(int state) {
      updateFabButton(state);

      Snackbar.make(mFabButton, "ConnectionState: " + EnumNames.fromConnectionState(state), Snackbar.LENGTH_SHORT).show();
      Log.i(TAG, "connection state changed: " + EnumNames.fromConnectionState(state));
    }
  };

  private OnGestureListener mGestureListener = new OnGestureListener() {
    @Override
    public void onGesture(int gesture) {
      Snackbar.make(mFabButton, "Gesture: " + EnumNames.fromGesture(gesture), Snackbar.LENGTH_SHORT).show();
      Log.i(TAG, "got gesture: " + EnumNames.fromGesture(gesture));
    }
  };

  /** small helper for readable activation states */
  private String nameOfAS(@ActivationState int state) {
    switch (state) {
      case ActivationState.ACTIVE:
        return "Active";
      case ActivationState.INACTIVE:
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
        .setIcon(de.kinemic.gesture.R.drawable.ic_search_black_24dp)
        .setTitle("Select Sensor")
        .setPositiveButton("Clear", null)
        .setNegativeButton("Cancel", null)
        .setOnDismissListener(dialogInterface -> mEngine.stopSearch())
        .setAdapter(adapter, (dialog, which) -> {
          // Sensor was selected in list -> connect to it
          mEngine.stopSearch();
          mEngine.connect(adapter.getItem(which).address);
        });

    AlertDialog dialog = builder.show();

    // reuse the positive button to non closing action (needs to be done here to not close)
    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> adapter.clear());

    mEngine.startSearch(adapter);
  }

}
