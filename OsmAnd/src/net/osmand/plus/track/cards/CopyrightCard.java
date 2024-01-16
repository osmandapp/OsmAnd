package net.osmand.plus.track.cards;

import androidx.annotation.NonNull;

import net.osmand.gpx.GPXUtilities;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.util.Algorithms;

public class CopyrightCard extends BaseMetadataCard {
	private final GPXUtilities.Metadata metadata;

	public CopyrightCard(@NonNull MapActivity mapActivity, @NonNull GPXUtilities.Metadata metadata) {
		super(mapActivity);
		this.metadata = metadata;
	}

	@Override
	void updateCard() {
		GPXUtilities.Copyright copyright = metadata.copyright;
		if (copyright == null) {
			return;
		}
		String author = copyright.author;
		String year = copyright.year;
		String license = copyright.license;

		boolean showCard = !Algorithms.isEmpty(author) || !Algorithms.isEmpty(year) || !Algorithms.isEmpty(license);
		updateVisibility(showCard);
		if (!showCard) {
			return;
		}

		if (!Algorithms.isEmpty(author)) {
			addNewItem(R.string.shared_string_name, author, false, true);
		}
		if (!Algorithms.isEmpty(year)) {
			addNewItem(R.string.year, year, false, true);
		}
		if (!Algorithms.isEmpty(license)) {
			addNewItem(R.string.shared_string_license, license, false, true);
		}
	}

	@Override
	protected int getCardTitle() {
		return R.string.shared_string_copyright;
	}
}