package de.kinemic.example;

import android.app.Activity;
import android.app.Application;
import android.app.Application.ActivityLifecycleCallbacks;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.widget.Toast;
import de.kinemic.sdk.Engine;
import de.kinemic.sdk.Engine.ConnectionState;
import java.util.TimerTask;

/**
 * Custom Application which hosts the engine.
 * This Application ensures that we release the engine and connected sensors when the user leaves the app.
 */
public class EngineApplication extends Application implements ActivityLifecycleCallbacks {
    private static final String TAG = EngineApplication.class.getSimpleName();

    /** If none of our activities is in focus for LOGOUT_DELAY ms, we disconnect the sensor and release the engine */
    private static final long LOGOUT_DELAY = 1000L;

    /** If the user returns to our app in RECONNECT_TIMEOUT ms, we reconnect the last sensor */
    private static final long RECONNECT_TIMEOUT = 60000L;

    /** The singleton engine instance */
    protected Engine mEngine;

    /** Handler to handle timeouts and delays */
    private Handler mHandler;

    /** True if we forced a disconnect due to LOGOUT_DELAY */
    private boolean mGotDisconnected = false;

    /** The address of the sensor we disconnected */
    private String mLastSensor = null;

    /** The time we disconnected */
    private long mDisconnectedAt = -1L;

    /** This runnable will be canceled most of the times, except when the user leaves our app */
    private final Runnable mDelayedLogout = new Runnable() {
        @Override
        public void run() {
            releaseEngine();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        registerActivityLifecycleCallbacks(this);
        mHandler = new Handler(getMainLooper());
    }

    /**
     * Get the singleton engine instance.
     * The engine will be lazily instantiated if needed.
     */
    public synchronized @NonNull Engine getEngine() {
        return getEngine(false);
    }

    /**
     * Get the singleton engine instance.
     * The engine will be lazily instantiated if needed.
     * @param reconnectLastSensor if true, we try to reconnect the last sensor we disconnected due to leaving our application
     */
    public synchronized @NonNull Engine getEngine(boolean reconnectLastSensor) {
        if (mEngine == null) {
            mEngine = new Engine(getApplicationContext());

            if (reconnectLastSensor) reconnectLastSensor();
        }
        return mEngine;
    }

    /** Convenient method to get Engine without casting the Application object */
    public static @NonNull Engine getEngine(Activity context) {
        return ((EngineApplication) context.getApplication()).getEngine();
    }

    /** Convenient method to get Engine without casting the Application object.
     * @param reconnectLastSensor if true, we try to reconnect the last sensor we disconnected due to leaving our application
     */
    public static @NonNull Engine getEngine(Activity context, boolean reconnectLastSensor) {
        return ((EngineApplication) context.getApplication()).getEngine(reconnectLastSensor);
    }

    private void reconnectLastSensor() {
        if (mGotDisconnected && mLastSensor != null && System.currentTimeMillis() - mDisconnectedAt < RECONNECT_TIMEOUT) {
            /* indicate to the user that we will reconnect the sensor, this can be done via a dialog asking the user if he wants to reconnect */
            Toast.makeText(getApplicationContext(), "Reconnecting...", Toast.LENGTH_LONG).show();
            mEngine.connect(mLastSensor);

            mGotDisconnected = false;
            mLastSensor = null;
            mDisconnectedAt = -1L;
        }
    }

    @Override
    public void onActivityCreated(final Activity activity, final Bundle savedInstanceState) {
        mHandler.removeCallbacks(mDelayedLogout);
    }

    @Override
    public void onActivityStarted(final Activity activity) {
        mHandler.removeCallbacks(mDelayedLogout);
    }

    @Override
    public void onActivityResumed(final Activity activity) {
        mHandler.removeCallbacks(mDelayedLogout);
    }

    @Override
    public void onActivityPaused(final Activity activity) {
        mHandler.postDelayed(mDelayedLogout, LOGOUT_DELAY);
    }

    @Override
    public void onActivityStopped(final Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(final Activity activity, final Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(final Activity activity) {

    }

    public synchronized void releaseEngine() {
        if (mEngine != null) {
            if (mEngine.getConnectionState() == ConnectionState.CONNECTED) {
                mLastSensor = mEngine.getSensor();
                mGotDisconnected = true;
                mDisconnectedAt = System.currentTimeMillis();

                /* indicate to the user that we will disconnect the sensor, this can be done via a dialog asking the user if he wants to disconnect or stay connected */
                Toast.makeText(getApplicationContext(), "Disconnect...", Toast.LENGTH_LONG).show();
                mEngine.disconnect();
            }
            mEngine.release();
            mEngine = null;
        }
    }

}
