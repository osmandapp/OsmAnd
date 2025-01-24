package net.osmand.plus.track.cards;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.shared.gpx.primitives.Metadata;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.util.Algorithms;

import java.util.Map;

public class TrackExtensionsCard extends BaseMetadataCard {
	private final Map<String, String> extensions;

	public TrackExtensionsCard(@NonNull MapActivity mapActivity, @NonNull Metadata metadata,
	                           @NonNull Map<String, String> extensions) {
		super(mapActivity, metadata);
		this.extensions = extensions;
	}

	@Override
	@StringRes
	protected int getTitleId() {
		return R.string.shared_string_additional;
	}

	@Override
	public void updateContent() {
		super.updateContent();
		updateVisibility(!Algorithms.isEmpty(extensions));
		if (!Algorithms.isEmpty(extensions)) {
			for (String key : extensions.keySet()) {
				String value = extensions.get(key);
				if (value != null) {
					createItemRow(Algorithms.capitalizeFirstLetter(key), value, null);
				}
			}
		}
	}
}