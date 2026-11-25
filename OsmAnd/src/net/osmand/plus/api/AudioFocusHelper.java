package net.osmand.plus.api;

import android.content.Context;

public interface AudioFocusHelper {

	boolean requestAudFocus(Context context, boolean pauseMusic);

	void onAudioFocusChange(int focusChange);

	boolean abandonAudFocus(Context context);

}
