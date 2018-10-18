package de.kinemic.example;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import de.kinemic.gesture.Engine;

public class EngineActivity extends AppCompatActivity {
  private static final String TAG = EngineActivity.class.getSimpleName();

  protected Engine mEngine = null;
  protected boolean mBound = false;
  private boolean mHandlersRegistered = false;

  @Override
  protected void onCreate(@Nullable final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    bindService(new Intent(this, EngineService.class), mServiceConnection, BIND_AUTO_CREATE);
  }

  @Override
  protected void onDestroy() {
    unbindService(mServiceConnection);
    super.onDestroy();
  }

  @Override
  protected void onResume() {
    super.onResume();

    if (mBound && !mHandlersRegistered) {
      registerHandlers();
      mHandlersRegistered = true;
    }
  }

  @Override
  protected void onPause() {
    if (mBound && mHandlersRegistered) {
      unregisterHandlers();
      mHandlersRegistered = false;
    }

    super.onPause();
  }

  protected void registerHandlers() {

  }

  protected void unregisterHandlers() {

  }

  private final ServiceConnection mServiceConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(final ComponentName name, final IBinder service) {
        mEngine = ((EngineService.LocalBinder) service).getEngine();
        mBound = true;

        /* service connected after onResume() */
        if (!mHandlersRegistered) {
          registerHandlers();
          mHandlersRegistered = true;
        }
    }

    @Override
    public void onServiceDisconnected(final ComponentName name) {
      mBound = false;
      mEngine = null;
      mHandlersRegistered = false;
    }
  };
}
