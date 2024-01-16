package net.osmand.plus.track.cards;

import androidx.annotation.NonNull;

import net.osmand.gpx.GPXUtilities;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.util.Algorithms;

import java.util.Map;

public class MetadataExtensionsCard extends BaseMetadataCard {
	private final GPXUtilities.Metadata metadata;

	public MetadataExtensionsCard(@NonNull MapActivity mapActivity, @NonNull GPXUtilities.Metadata metadata) {
		super(mapActivity);
		this.metadata = metadata;
	}

	@Override
	void updateCard() {
		Map<String, String> extensions = metadata.extensions;
		if (extensions == null) {
			return;
		}
		updateVisibility(!Algorithms.isEmpty(extensions));

		for (String key : extensions.keySet()) {
			addNewItem(extensions.get(key), key, false, true);
		}
	}

	@Override
	protected int getCardTitle() {
		return R.string.shared_string_extensions;
	}
}