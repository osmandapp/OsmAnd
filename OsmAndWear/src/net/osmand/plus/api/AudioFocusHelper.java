package net.osmand.plus.api;

import android.content.Context;

public interface AudioFocusHelper {

	boolean requestAudFocus(Context context);

	void onAudioFocusChange(int focusChange);

	boolean abandonAudFocus(Context context);

}
