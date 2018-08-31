package de.kinemic.example;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import de.kinemic.sdk.Engine;

public class EngineService extends Service {
  private static final String TAG = EngineService.class.getSimpleName();

  private Engine mEngine;
  private final LocalBinder mLocalBinder = new LocalBinder();

  public class LocalBinder extends Binder {
    Engine getEngine() {
      return mEngine;
    }
  }

  @Override
  public void onCreate() {
    super.onCreate();

    mEngine = new Engine(getApplicationContext());
    Log.d(TAG, "created");
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    mEngine.disconnect();
    mEngine.release();
    Log.d(TAG, "destroy");
  }

  @Override
  public IBinder onBind(Intent intent) {
    return mLocalBinder;
  }
}
