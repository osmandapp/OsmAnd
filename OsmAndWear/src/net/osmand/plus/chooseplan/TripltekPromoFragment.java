package net.osmand.plus.chooseplan;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.FRAGMENT_TRIPLTEK_PROMO_ID;
import static net.osmand.plus.chooseplan.OsmAndFeature.ANDROID_AUTO;
import static net.osmand.plus.chooseplan.OsmAndFeature.EXTERNAL_SENSORS_SUPPORT;
import static net.osmand.plus.chooseplan.OsmAndFeature.HOURLY_MAP_UPDATES;
import static net.osmand.plus.chooseplan.OsmAndFeature.MONTHLY_MAP_UPDATES;
import static net.osmand.plus.chooseplan.OsmAndFeature.NAUTICAL;
import static net.osmand.plus.chooseplan.OsmAndFeature.RELIEF_3D;
import static net.osmand.plus.chooseplan.OsmAndFeature.TERRAIN;
import static net.osmand.plus.chooseplan.OsmAndFeature.UNLIMITED_MAP_DOWNLOADS;
import static net.osmand.plus.chooseplan.OsmAndFeature.WEATHER;
import static net.osmand.plus.chooseplan.OsmAndFeature.WIKIPEDIA;
import static net.osmand.plus.chooseplan.OsmAndFeature.WIKIVOYAGE;
import static net.osmand.plus.inapp.InAppPurchaseUtils.TRIPLTEK_PROMO_MONTHS;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.inapp.InAppPurchaseUtils;
import net.osmand.plus.utils.AndroidUtils;

import java.util.Arrays;
import java.util.List;

public class TripltekPromoFragment extends PromoCompanyFragment {

	public static final String TAG = TripltekPromoFragment.class.getSimpleName();

	@Override
	public void onResume() {
		super.onResume();
		settings.TRIPLTEK_PROMO_SHOWED.set(true);
	}

	@NonNull
	@Override
	protected List<OsmAndFeature> getFeatures() {
		return Arrays.asList(UNLIMITED_MAP_DOWNLOADS, RELIEF_3D, TERRAIN, ANDROID_AUTO, WIKIPEDIA,
				WIKIVOYAGE, WEATHER, MONTHLY_MAP_UPDATES, HOURLY_MAP_UPDATES, EXTERNAL_SENSORS_SUPPORT, NAUTICAL);
	}

	protected void setupContent(@NonNull View view) {
		super.setupContent(view);
		TextView title = view.findViewById(R.id.title);
		TextView description = view.findViewById(R.id.description);

		title.setText(getString(R.string.tripltek_promo, String.valueOf(TRIPLTEK_PROMO_MONTHS)));
		description.setText(getString(R.string.tripltek_promo_description, String.valueOf(TRIPLTEK_PROMO_MONTHS)));
	}

	public static boolean shouldShow(@NonNull OsmandApplication app) {
		if (Version.isTripltekBuild() && app.getAppCustomization().isFeatureEnabled(FRAGMENT_TRIPLTEK_PROMO_ID)) {
			return InAppPurchaseUtils.isTripltekPromoAvailable(app) && !app.getSettings().TRIPLTEK_PROMO_SHOWED.get();
		}
		return false;
	}

	public static void showInstance(@NonNull FragmentManager manager) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG, true)) {
			TripltekPromoFragment fragment = new TripltekPromoFragment();
			fragment.show(manager, TAG);
		}
	}
}