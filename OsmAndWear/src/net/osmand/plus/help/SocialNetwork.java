package net.osmand.plus.help;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import net.osmand.plus.R;

enum SocialNetwork {

	TWITTER(R.string.twitter, R.string.community_twitter, R.drawable.ic_action_social_twitter),
	REDDIT(R.string.reddit, R.string.community_reddit, R.drawable.ic_action_social_reddit),
	FACEBOOK(R.string.facebook, R.string.community_facebook, R.drawable.ic_action_social_facebook);

	@StringRes
	public final int titleId;
	@StringRes
	public final int urlId;
	@DrawableRes
	public final int iconId;

	SocialNetwork(@StringRes int titleId, @StringRes int urlId, @DrawableRes int iconId) {
		this.titleId = titleId;
		this.urlId = urlId;
		this.iconId = iconId;
	}
}
