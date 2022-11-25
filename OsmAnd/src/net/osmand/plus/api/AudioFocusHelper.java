package net.osmand.plus.api;

import android.content.Context;

import net.osmand.plus.settings.backend.ApplicationMode;

public interface AudioFocusHelper {

	boolean requestAudFocus(Context context);

	void onAudioFocusChange(int focusChange);

	boolean abandonAudFocus(Context context);

}
