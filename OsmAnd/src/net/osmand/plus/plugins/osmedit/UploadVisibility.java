package net.osmand.plus.plugins.osmedit;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum UploadVisibility {

	@Deprecated // https://community.openstreetmap.org/t/changes-coming-to-gps-trace-uploads/145728
	PUBLIC(R.string.gpxup_public, R.string.gpx_upload_public_visibility_descr),
	IDENTIFIABLE(R.string.gpxup_identifiable, R.string.gpx_upload_identifiable_visibility_descr),
	TRACKABLE(R.string.gpxup_trackable, R.string.gpx_upload_trackable_visibility_descr),
	@Deprecated
	PRIVATE(R.string.gpxup_private, R.string.gpx_upload_private_visibility_descr);

	@StringRes
	private final int titleId;
	@StringRes
	private final int descriptionId;

	UploadVisibility(@StringRes int titleId, @StringRes int descriptionId) {
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
	public static UploadVisibility getDefaultValue() {
		return IDENTIFIABLE;
	}

	@NonNull
	public static UploadVisibility[] getAvailableValues() {
		return new UploadVisibility[] {IDENTIFIABLE, TRACKABLE};
	}

	@NonNull
	public String asUrlParam() {
		return name().toLowerCase();
	}
}