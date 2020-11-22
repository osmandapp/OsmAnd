package net.osmand.plus.chooseplan;

import android.app.Activity;
import android.view.View;

import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.activities.OsmandInAppPurchaseActivity;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.inapp.InAppPurchases.InAppPurchase;

public class ChoosePlanHillshadeSrtmDialogFragment extends ChoosePlanDialogFragment {
	public static final String TAG = ChoosePlanHillshadeSrtmDialogFragment.class.getSimpleName();

	private final OsmAndFeature[] osmLiveFeatures = {
			OsmAndFeature.CONTOUR_LINES_HILLSHADE_MAPS,
			OsmAndFeature.SEA_DEPTH_MAPS,
			OsmAndFeature.DAILY_MAP_UPDATES,
			OsmAndFeature.UNLIMITED_DOWNLOADS,
			OsmAndFeature.WIKIPEDIA_OFFLINE,
			OsmAndFeature.WIKIVOYAGE_OFFLINE,
			OsmAndFeature.UNLOCK_ALL_FEATURES,
	};
	private final OsmAndFeature[] selectedOsmLiveFeatures = {
			OsmAndFeature.CONTOUR_LINES_HILLSHADE_MAPS,
			OsmAndFeature.SEA_DEPTH_MAPS,
	};

	private final OsmAndFeature[] planTypeFeatures = {
			OsmAndFeature.CONTOUR_LINES_HILLSHADE_MAPS,
	};
	private final OsmAndFeature[] selectedPlanTypeFeatures = {};

	@Override
	public OsmAndFeature[] getOsmLiveFeatures() {
		return osmLiveFeatures;
	}

	@Override
	public OsmAndFeature[] getPlanTypeFeatures() {
		return planTypeFeatures;
	}

	@Override
	public OsmAndFeature[] getSelectedOsmLiveFeatures() {
		return selectedOsmLiveFeatures;
	}

	@Override
	public OsmAndFeature[] getSelectedPlanTypeFeatures() {
		return selectedPlanTypeFeatures;
	}

	@Override
	public int getPlanTypeHeaderImageId() {
		return R.drawable.img_logo_38dp_contour_lines;
	}

	@Override
	public String getPlanTypeHeaderTitle() {
		return getString(R.string.srtm_plugin_name);
	}

	@Override
	public String getPlanTypeHeaderDescription() {
		return getString(R.string.paid_plugin);
	}

	@Override
	public String getPlanTypeButtonDescription() {
		return getString(R.string.in_app_purchase_desc);
	}

	@Override
	public void setPlanTypeButtonClickListener(View button) {
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Activity activity = getActivity();
				if (activity != null) {
					OsmandInAppPurchaseActivity.purchaseContourLines(activity);
					dismiss();
				}
			}
		});
	}

	@Nullable
	@Override
	public InAppPurchase getPlanTypePurchase() {
		InAppPurchaseHelper purchaseHelper = getOsmandApplication().getInAppPurchaseHelper();
		if (purchaseHelper != null) {
			return purchaseHelper.getContourLines();
		}
		return null;
	}
}
