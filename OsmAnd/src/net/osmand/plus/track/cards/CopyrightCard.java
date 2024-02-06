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
				createItemRow(getString(R.string.shared_string_author), copyright.author, getContentIcon(R.drawable.ic_action_copyright));
			}
			if (!Algorithms.isEmpty(copyright.year)) {
				createItemRow(getString(R.string.year), copyright.year, getContentIcon(R.drawable.ic_action_calendar_month));
			}
			createLinkItemRow(getString(R.string.shared_string_license), copyright.license, R.drawable.ic_action_link);
		}
	}
}