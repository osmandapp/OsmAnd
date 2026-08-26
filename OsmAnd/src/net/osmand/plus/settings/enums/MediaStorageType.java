package net.osmand.plus.settings.enums;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum MediaStorageType {

	MAIN_STORAGE(R.string.main_storage, R.string.media_storage_main_storage_descr),
	SHARED_STORAGE(R.string.shared_storage, R.string.media_storage_shared_storage_descr),
	CAMERA_FOLDER(R.string.camera_folder, R.string.media_storage_camera_folder_descr),
	MANUALLY_SPECIFIED(R.string.storage_directory_manual, R.string.media_storage_manually_specified_descr);

	@StringRes
	private final int titleId;
	@StringRes
	private final int descriptionId;

	MediaStorageType(@StringRes int titleId, @StringRes int descriptionId) {
		this.titleId = titleId;
		this.descriptionId = descriptionId;
	}

	@StringRes
	public int getTitleId() {
		return titleId;
	}

	@StringRes
	public int getDescriptionId() {
		return descriptionId;
	}

	@NonNull
	public String toHumanString(@NonNull Context ctx) {
		return ctx.getString(titleId);
	}
}