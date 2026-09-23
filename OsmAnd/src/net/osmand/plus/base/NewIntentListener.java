package net.osmand.plus.base;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

/**
 * Implemented by fragments that react to a new intent delivered to their activity.
 * Called before the activity parses the intent.
 */
public interface NewIntentListener {

	void onNewIntent(@NonNull Intent intent);

	static void notifyFragments(@NonNull FragmentManager fragmentManager, @NonNull Intent intent) {
		for (Fragment fragment : fragmentManager.getFragments()) {
			if (fragment instanceof NewIntentListener listener) {
				listener.onNewIntent(intent);
			}
		}
	}
}
