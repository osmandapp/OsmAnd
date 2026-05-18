package net.osmand.plus.track.fragments.controller;

import androidx.annotation.NonNull;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.track.fragments.EditDescriptionFragment.OnDescriptionSavedCallback;

public abstract class EditDescriptionController {

	protected MapActivity activity;

	public EditDescriptionController(@NonNull MapActivity activity) {
		this.activity = activity;
	}

	public abstract void saveEditedDescription(@NonNull String editedText, @NonNull OnDescriptionSavedCallback callback);

}
