package net.osmand.plus.api;

import android.content.Context;

public interface AudioFocusHelper {

	boolean requestFocus(Context context, int streamType);

	void onAudioFocusChange(int focusChange);

	boolean abandonFocus(Context context, int streamType);

}
