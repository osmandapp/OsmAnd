package net.osmand.plus.track.cards;

import androidx.annotation.NonNull;

import net.osmand.gpx.GPXUtilities;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.util.Algorithms;

public class AuthorCard extends BaseMetadataCard {
	private final GPXUtilities.Metadata metadata;

	public AuthorCard(@NonNull MapActivity mapActivity, @NonNull GPXUtilities.Metadata metadata) {
		super(mapActivity);
		this.metadata = metadata;
	}

	@Override
	void updateCard() {
		GPXUtilities.Author author = metadata.author;
		if (author == null) {
			return;
		}
		String name = author.name;
		String email = author.email;
		String link = author.link;

		boolean showCard = !Algorithms.isEmpty(name) || !Algorithms.isEmpty(email) || !Algorithms.isEmpty(link);
		updateVisibility(showCard);
		if (!showCard) {
			return;
		}

		if (!Algorithms.isEmpty(name)) {
			addNewItem(R.string.shared_string_name, name, false, true);
		}
		if (!Algorithms.isEmpty(email)) {
			addNewItem(R.string.shared_string_email_address, email, false, true);
		}
		if (!Algorithms.isEmpty(link)) {
			addNewItem(R.string.shared_string_link, link, true, true);
		}
	}

	@Override
	protected int getCardTitle() {
		return R.string.shared_string_author;
	}
}