package net.osmand.plus.plugins.antplus

interface BleConnectionStateListener {
    fun onStateChanged(address: String?, newState: Int)
}