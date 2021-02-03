package net.osmand.plus.openplacereviews;

import android.graphics.drawable.Drawable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.settings.fragments.BaseSettingsFragment.SettingsScreenType;

public class OpenPlaceReviewsPlugin extends OsmandPlugin {

	private static final String ID = "osmand.openplacereviews";

	public OpenPlaceReviewsPlugin(OsmandApplication app) {
		super(app);
	}

	@Override
	public String getId() {
		return ID;
	}


	@Override
	public String getName() {
		return app.getString(R.string.open_place_reviews);
	}

	@Override
	public CharSequence getDescription() {
		return app.getString(R.string.open_place_reviews_plugin_description);
	}

	@Override
	public SettingsScreenType getSettingsScreenType() {
		return SettingsScreenType.OPEN_PLACE_REVIEWS;
	}

	@Override
	public int getLogoResourceId() {
		return R.drawable.ic_img_logo_openplacereview;
	}

	@Override
	public Drawable getAssetResourceImage() {
		return app.getUIUtilities().getIcon(R.drawable.img_plugin_openplacereviews);
	}

	@Override
	public void disable(OsmandApplication app) {
		if (app.getSettings().OPR_USE_DEV_URL.get()) {
			app.getSettings().OPR_USE_DEV_URL.set(false);
			app.getOprAuthHelper().resetAuthorization();
		}
		super.disable(app);
	}
}
