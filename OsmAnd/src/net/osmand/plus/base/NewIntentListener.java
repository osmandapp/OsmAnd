package net.osmand.plus.base;

import android.content.Intent;

import androidx.annotation.NonNull;

/**
 * Implemented by MapActivity fragments that react to a new intent.
 * Called before the activity parses the intent.
 */
public interface NewIntentListener {

	void onNewIntent(@NonNull Intent intent);
}
