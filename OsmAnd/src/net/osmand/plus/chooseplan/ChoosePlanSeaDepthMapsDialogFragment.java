package net.osmand.plus.chooseplan;

public class ChoosePlanSeaDepthMapsDialogFragment extends ChoosePlanFreeBannerDialogFragment {
	public static final String TAG = ChoosePlanSeaDepthMapsDialogFragment.class.getSimpleName();

	private final OsmAndFeatureOld[] subscriptionFeatures = {
			OsmAndFeatureOld.SEA_DEPTH_MAPS,
			OsmAndFeatureOld.CONTOUR_LINES_HILLSHADE_MAPS,
			OsmAndFeatureOld.WIKIPEDIA_OFFLINE,
			OsmAndFeatureOld.WIKIVOYAGE_OFFLINE,
			OsmAndFeatureOld.DAILY_MAP_UPDATES,
			OsmAndFeatureOld.UNLIMITED_DOWNLOADS,
			OsmAndFeatureOld.UNLOCK_ALL_FEATURES,
	};
	private final OsmAndFeatureOld[] selectedSubscriptionFeatures = {
			OsmAndFeatureOld.SEA_DEPTH_MAPS,
			OsmAndFeatureOld.CONTOUR_LINES_HILLSHADE_MAPS,
	};

	private final OsmAndFeatureOld[] planTypeFeatures = {
			OsmAndFeatureOld.SEA_DEPTH_MAPS,
			OsmAndFeatureOld.CONTOUR_LINES_HILLSHADE_MAPS,
			OsmAndFeatureOld.WIKIPEDIA_OFFLINE,
			OsmAndFeatureOld.UNLIMITED_DOWNLOADS,
			OsmAndFeatureOld.MONTHLY_MAP_UPDATES,
	};
	private final OsmAndFeatureOld[] selectedPlanTypeFeatures = {};

	@Override
	public OsmAndFeatureOld[] getSubscriptionFeatures() {
		return subscriptionFeatures;
	}

	@Override
	public OsmAndFeatureOld[] getPlanTypeFeatures() {
		return planTypeFeatures;
	}

	@Override
	public OsmAndFeatureOld[] getSelectedSubscriptionFeatures() {
		return selectedSubscriptionFeatures;
	}

	@Override
	public OsmAndFeatureOld[] getSelectedPlanTypeFeatures() {
		return selectedPlanTypeFeatures;
	}
}
