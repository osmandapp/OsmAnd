package net.osmand.plus.plugins.aprs;

import androidx.annotation.NonNull;

/**
 * Accepts decoded AX.25 frames from aprs-driver, KISS TCP, or test harness.
 */
public interface AprsMessageListener {

	void onAx25Frame(@NonNull byte[] frame);

	void stopListener();
}
