package net.osmand.plus.track.cards;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.gpx.GPXUtilities.Copyright;
import net.osmand.gpx.GPXUtilities.Metadata;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.util.Algorithms;

public class CopyrightCard extends BaseMetadataCard {

	public CopyrightCard(@NonNull MapActivity mapActivity, @NonNull Metadata metadata) {
		super(mapActivity, metadata);
	}

	@Override
	@StringRes
	protected int getTitleId() {
		return R.string.shared_string_copyright;
	}

	@Override
	public void updateContent() {
		super.updateContent();

		Copyright copyright = metadata.copyright;
		boolean visible = copyright != null && (!Algorithms.isEmpty(copyright.author)
				|| !Algorithms.isEmpty(copyright.year) || !Algorithms.isEmpty(copyright.license));

		updateVisibility(visible);

		if (visible) {
			if (!Algorithms.isEmpty(copyright.author)) {
				createItemRow(getString(R.string.shared_string_name), copyright.author, null);
			}
			if (!Algorithms.isEmpty(copyright.year)) {
				createItemRow(getString(R.string.year), copyright.year, null);
			}
			if (!Algorithms.isEmpty(copyright.license)) {
				createItemRow(getString(R.string.shared_string_license), copyright.license, null);
			}
		}
	}
}