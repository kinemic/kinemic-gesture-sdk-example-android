package de.kinemic.example

import android.app.Application
import android.os.Handler
import android.preference.PreferenceManager
import de.kinemic.gesture.*
import de.kinemic.gesture.common.EngineProvider
import de.kinemic.gesture.multi.OnActivationStateChangeListener
import de.kinemic.gesture.multi.OnConnectionStateChangeListener

/**
 * This class provides an Application implementing the [EngineProvider] interface
 */
class EngineApplicationDevelop : Application(), EngineProvider {

    companion object {
        private const val LAST_BAND_PREF_KEY = "de.kinemic.gesture.common.LAST_BAND"
    }

    override var lastConnectedBand: String? = null

    private val activationStateChangeListener: OnActivationStateChangeListener = OnActivationStateChangeListener { band, state ->
        when(state) {
            ActivationState.ACTIVE -> Handler().postDelayed({ engine.setLed(band, Led.BLUE) }, 10)
            ActivationState.INACTIVE -> Handler().postDelayed({ engine.setLed(band, Led.YELLOW) }, 10)
        }
    }

    private val connectionStateChangeListener: OnConnectionStateChangeListener = OnConnectionStateChangeListener { band, state, _ ->
        when (state) {
            ConnectionState.DISCONNECTING -> {
                lastConnectedBand = band
                lastConnectedBand?.let{
                    PreferenceManager.getDefaultSharedPreferences(applicationContext).edit().putString(Companion.LAST_BAND_PREF_KEY, it).apply()
                }
            }
            ConnectionState.CONNECTED -> {
                if (lastConnectedBand == band) {
                    lastConnectedBand = null
                }
            }
        }
    }

    override val engine: Engine by lazy {
        lastConnectedBand = PreferenceManager.getDefaultSharedPreferences(applicationContext).getString(LAST_BAND_PREF_KEY, lastConnectedBand)

        val engine = EngineDevelop(applicationContext)

        // register handler to set led color for active/inactive state
        engine.registerOnActivationStateChangeListener(activationStateChangeListener)

        // register handler to remember last connected sensor
        engine.registerOnConnectionStateChangeListener(connectionStateChangeListener)

        engine
    }
}
