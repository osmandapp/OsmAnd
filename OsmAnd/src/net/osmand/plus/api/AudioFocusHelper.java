package net.osmand.plus.api;

import android.content.Context;

import net.osmand.plus.settings.backend.ApplicationMode;

public interface AudioFocusHelper {

	boolean requestAudFocus(Context context, ApplicationMode applicationMode, int streamType);

	void onAudioFocusChange(int focusChange);

	boolean abandonAudFocus(Context context, ApplicationMode applicationMode, int streamType);

}
