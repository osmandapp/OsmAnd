package net.osmand.plus.api;

import android.content.Context;

import net.osmand.plus.settings.backend.ApplicationMode;

public interface AudioFocusHelper {

	boolean requestFocus(Context context, ApplicationMode applicationMode, int streamType);

	void onAudioFocusChange(int focusChange);

	boolean abandonFocus(Context context, ApplicationMode applicationMode, int streamType);

}
