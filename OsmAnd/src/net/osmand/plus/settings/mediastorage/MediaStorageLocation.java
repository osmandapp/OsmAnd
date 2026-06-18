package net.osmand.plus.settings.mediastorage;

import static net.osmand.plus.settings.enums.MediaStorageType.MANUALLY_SPECIFIED;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.MediaStorageType;
import net.osmand.util.Algorithms;

import java.util.Objects;

public class MediaStorageLocation {

	private final MediaStorageType storageType;
	@Nullable
	private final Uri manualUri;

	private MediaStorageLocation(@NonNull MediaStorageType storageType, @Nullable Uri manualUri) {
		this.storageType = storageType;
		this.manualUri = storageType == MANUALLY_SPECIFIED ? manualUri : null;
	}

	@NonNull
	public MediaStorageType getStorageType() {
		return storageType;
	}

	@Nullable
	public Uri getManualUri() {
		return manualUri;
	}

	@NonNull
	public static MediaStorageLocation fromSettings(@NonNull OsmandApplication app) {
		OsmandSettings settings = app.getSettings();
		return fromSelection(settings.MEDIA_STORAGE_TYPE.get(), settings.MEDIA_STORAGE_MANUAL_URI.get());
	}

	@NonNull
	public static MediaStorageLocation fromSelection(@NonNull MediaStorageType storageType, @Nullable String manualUri) {
		Uri treeUri = Algorithms.isEmpty(manualUri) ? null : Uri.parse(manualUri);
		return new MediaStorageLocation(storageType, treeUri);
	}

	public boolean hasSameStorage(@NonNull MediaStorageLocation other) {
		return storageType == other.storageType && (storageType != MANUALLY_SPECIFIED || Objects.equals(manualUri, other.manualUri));
	}
}