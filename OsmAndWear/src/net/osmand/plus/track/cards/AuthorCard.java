package net.osmand.plus.track.cards;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.shared.gpx.primitives.Author;
import net.osmand.shared.gpx.primitives.Metadata;
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

		Author author = metadata.getAuthor();
		boolean visible = author != null && (!Algorithms.isEmpty(author.getName())
				|| !Algorithms.isEmpty(author.getEmail()) || !Algorithms.isEmpty(author.getLink()));

		updateVisibility(visible);

		if (visible) {
			if (!Algorithms.isEmpty(author.getName())) {
				createItemRow(getString(R.string.shared_string_name), author.getName(), getContentIcon(R.drawable.ic_action_user));
			}
			if (!Algorithms.isEmpty(author.getEmail())) {
				createEmailItemRow(getString(R.string.shared_string_email_address), author.getEmail(), R.drawable.ic_action_at_mail);
			}
			createLinkItemRow(getString(R.string.shared_string_link), author.getLink(), R.drawable.ic_action_link);
		}
	}
}