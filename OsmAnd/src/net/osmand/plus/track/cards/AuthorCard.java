package net.osmand.plus.track.cards;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.gpx.GPXUtilities.Author;
import net.osmand.gpx.GPXUtilities.Metadata;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.util.Algorithms;

public class AuthorCard extends BaseMetadataCard {

	public AuthorCard(@NonNull MapActivity mapActivity, @NonNull Metadata metadata) {
		super(mapActivity, metadata);
	}

	@Override
	@StringRes
	protected int getTitleId() {
		return R.string.shared_string_author;
	}

	@Override
	public void updateContent() {
		super.updateContent();

		Author author = metadata.author;
		boolean visible = author != null && (!Algorithms.isEmpty(author.name)
				|| !Algorithms.isEmpty(author.email) || !Algorithms.isEmpty(author.link));

		updateVisibility(visible);

		if (visible) {
			if (!Algorithms.isEmpty(author.name)) {
				createItemRow(getString(R.string.shared_string_name), author.name, getContentIcon(R.drawable.ic_action_user));
			}
			if (!Algorithms.isEmpty(author.email)) {
				createEmailItemRow(getString(R.string.shared_string_email_address), author.email, R.drawable.ic_action_at_mail);
			}
			createLinkItemRow(getString(R.string.shared_string_link), author.link, R.drawable.ic_action_link);
		}
	}
}