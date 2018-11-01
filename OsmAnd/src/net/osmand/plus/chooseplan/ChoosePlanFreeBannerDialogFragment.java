package net.osmand.plus.chooseplan;

import android.app.Activity;
import android.view.View;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.OsmandInAppPurchaseActivity;
import net.osmand.plus.inapp.InAppPurchaseHelper;

public class ChoosePlanFreeBannerDialogFragment extends ChoosePlanDialogFragment {
	public static final String TAG = ChoosePlanFreeBannerDialogFragment.class.getSimpleName();

	private final OsmAndFeature[] osmLiveFeatures = {
			OsmAndFeature.DAILY_MAP_UPDATES,
			OsmAndFeature.UNLIMITED_DOWNLOADS,
			OsmAndFeature.WIKIPEDIA_OFFLINE,
			OsmAndFeature.WIKIVOYAGE_OFFLINE,
			OsmAndFeature.CONTOUR_LINES_HILLSHADE_MAPS,
			OsmAndFeature.SEA_DEPTH_MAPS,
			OsmAndFeature.UNLOCK_ALL_FEATURES,
	};
	private final OsmAndFeature[] selectedOsmLiveFeatures = {
			OsmAndFeature.DAILY_MAP_UPDATES,
			OsmAndFeature.UNLIMITED_DOWNLOADS,
	};

	private final OsmAndFeature[] planTypeFeatures = {
			OsmAndFeature.WIKIPEDIA_OFFLINE,
			OsmAndFeature.WIKIVOYAGE_OFFLINE,
			OsmAndFeature.UNLIMITED_DOWNLOADS,
			OsmAndFeature.MONTHLY_MAP_UPDATES,
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
		return R.drawable.img_logo_38dp_osmand;
	}

	@Override
	public String getPlanTypeHeaderTitle() {
		return getString(R.string.osmand_unlimited);
	}

	@Override
	public String getPlanTypeHeaderDescription() {
		if (getOsmandApplication().isPlusVersionInApp()) {
			return getString(R.string.in_app_purchase);
		} else {
			return getString(R.string.paid_app);
		}
	}

	@Override
	public String getPlanTypeButtonTitle() {
		InAppPurchaseHelper purchaseHelper = getOsmandApplication().getInAppPurchaseHelper();
		return getString(R.string.purchase_unlim_title, purchaseHelper.getFullVersion().getPrice(getContext()));
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
					OsmandApplication app = getOsmandApplication();
					if (app.isPlusVersionInApp()) {
						app.logEvent(getActivity(), "in_app_purchase_redirect_from_banner");
					} else {
						app.logEvent(getActivity(), "paid_version_redirect_from_banner");
						dismiss();
					}
					OsmandInAppPurchaseActivity.purchaseFullVersion(activity);
				}
			}
		});
	}
}
