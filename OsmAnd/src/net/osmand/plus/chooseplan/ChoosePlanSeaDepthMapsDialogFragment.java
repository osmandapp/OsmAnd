package net.osmand.plus.chooseplan;

public class ChoosePlanSeaDepthMapsDialogFragment extends ChoosePlanFreeBannerDialogFragment {
	public static final String TAG = ChoosePlanSeaDepthMapsDialogFragment.class.getSimpleName();

	private final OsmAndFeature[] subscriptionFeatures = {
			OsmAndFeature.SEA_DEPTH_MAPS,
			OsmAndFeature.CONTOUR_LINES_HILLSHADE_MAPS,
			OsmAndFeature.WIKIPEDIA_OFFLINE,
			OsmAndFeature.WIKIVOYAGE_OFFLINE,
			OsmAndFeature.DAILY_MAP_UPDATES,
			OsmAndFeature.UNLIMITED_DOWNLOADS,
			OsmAndFeature.UNLOCK_ALL_FEATURES,
	};
	private final OsmAndFeature[] selectedSubscriptionFeatures = {
			OsmAndFeature.SEA_DEPTH_MAPS,
			OsmAndFeature.CONTOUR_LINES_HILLSHADE_MAPS,
	};

	private final OsmAndFeature[] planTypeFeatures = {
			OsmAndFeature.SEA_DEPTH_MAPS,
			OsmAndFeature.CONTOUR_LINES_HILLSHADE_MAPS,
			OsmAndFeature.WIKIPEDIA_OFFLINE,
			OsmAndFeature.UNLIMITED_DOWNLOADS,
			OsmAndFeature.MONTHLY_MAP_UPDATES,
	};
	private final OsmAndFeature[] selectedPlanTypeFeatures = {};

	@Override
	public OsmAndFeature[] getSubscriptionFeatures() {
		return subscriptionFeatures;
	}

	@Override
	public OsmAndFeature[] getPlanTypeFeatures() {
		return planTypeFeatures;
	}

	@Override
	public OsmAndFeature[] getSelectedSubscriptionFeatures() {
		return selectedSubscriptionFeatures;
	}

	@Override
	public OsmAndFeature[] getSelectedPlanTypeFeatures() {
		return selectedPlanTypeFeatures;
	}
}
