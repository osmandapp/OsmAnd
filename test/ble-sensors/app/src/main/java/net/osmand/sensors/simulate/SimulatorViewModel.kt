package net.osmand.sensors.simulate

import android.app.Application
import androidx.lifecycle.AndroidViewModel

class SimulatorViewModel(application: Application) : AndroidViewModel(application) {

    val simulator = BleSensorSimulator(application)

    override fun onCleared() {
        simulator.stop()
    }
}