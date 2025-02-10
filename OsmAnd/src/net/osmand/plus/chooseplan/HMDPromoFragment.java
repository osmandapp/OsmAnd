package net.osmand.plus.chooseplan;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.FRAGMENT_HMD_PROMO_ID;
import static net.osmand.plus.chooseplan.OsmAndFeature.*;
import static net.osmand.plus.inapp.InAppPurchaseUtils.HMD_PROMO_MONTHS;

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

public class HMDPromoFragment extends PromoCompanyFragment {

	public static final String TAG = HMDPromoFragment.class.getSimpleName();

	@Override
	public void onResume() {
		super.onResume();
		settings.HMD_PROMO_SHOWED.set(true);
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

		title.setText(getString(R.string.hmd_promo, String.valueOf(HMD_PROMO_MONTHS)));
		description.setText(getString(R.string.hmd_promo_description, String.valueOf(HMD_PROMO_MONTHS)));
	}

	public static boolean shouldShow(@NonNull OsmandApplication app) {
		if (Version.isHMDBuild() && app.getAppCustomization().isFeatureEnabled(FRAGMENT_HMD_PROMO_ID)) {
			return InAppPurchaseUtils.isHMDPromoAvailable(app) && !app.getSettings().HMD_PROMO_SHOWED.get();
		}
		return false;
	}

	public static void showInstance(@NonNull FragmentManager manager) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG, true)) {
			HMDPromoFragment fragment = new HMDPromoFragment();
			fragment.show(manager, TAG);
		}
	}
}