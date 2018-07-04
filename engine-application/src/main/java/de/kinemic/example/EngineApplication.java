package de.kinemic.example;

import android.app.Application;
import de.kinemic.sdk.Engine;

/**
 * Custom Application which hosts the engine.
 * Note: You need to call releaseEngine() to release resources!
 */
public class EngineApplication extends Application {
    private static final String TAG = EngineApplication.class.getSimpleName();

    protected Engine mEngine;

    public synchronized Engine getEngine() {
        if (mEngine == null) {
            mEngine = new Engine(getApplicationContext());
        }
        return mEngine;
    }

    public synchronized void releaseEngine() {
        if (mEngine != null) {
            mEngine.disconnect();
            mEngine.release();
            mEngine = null;
        }
    }

}
