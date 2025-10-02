package net.osmand.plus.keyevent.listener;

import androidx.annotation.NonNull;

import net.osmand.plus.settings.backend.ApplicationMode;

public interface InputDevicesEventListener {
	void processInputDevicesEvent(@NonNull ApplicationMode appMode, @NonNull EventType event);
}
