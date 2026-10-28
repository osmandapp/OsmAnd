package net.osmand.plus.track.cards;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.shared.gpx.primitives.Copyright;
import net.osmand.shared.gpx.primitives.Metadata;
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

		Copyright copyright = metadata.getCopyright();
		boolean visible = copyright != null && (!Algorithms.isEmpty(copyright.getAuthor())
				|| !Algorithms.isEmpty(copyright.getYear()) || !Algorithms.isEmpty(copyright.getLicense()));

		updateVisibility(visible);

		if (visible) {
			if (!Algorithms.isEmpty(copyright.getAuthor())) {
				createItemRow(getString(R.string.shared_string_author), copyright.getAuthor(), getContentIcon(R.drawable.ic_action_copyright));
			}
			if (!Algorithms.isEmpty(copyright.getYear())) {
				createItemRow(getString(R.string.year), copyright.getYear(), getContentIcon(R.drawable.ic_action_calendar_month));
			}
			createLinkItemRow(getString(R.string.shared_string_license), copyright.getLicense(), R.drawable.ic_action_link);
		}
	}
}