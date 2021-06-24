package net.osmand.plus.chooseplan;

public class ChoosePlanHillshadeSrtmDialogFragment extends ChoosePlanFreeBannerDialogFragment {
	public static final String TAG = ChoosePlanHillshadeSrtmDialogFragment.class.getSimpleName();

	private final OsmAndFeatureOld[] subscriptionFeatures = {
			OsmAndFeatureOld.CONTOUR_LINES_HILLSHADE_MAPS,
			OsmAndFeatureOld.SEA_DEPTH_MAPS,
			OsmAndFeatureOld.DAILY_MAP_UPDATES,
			OsmAndFeatureOld.UNLIMITED_DOWNLOADS,
			OsmAndFeatureOld.WIKIPEDIA_OFFLINE,
			OsmAndFeatureOld.WIKIVOYAGE_OFFLINE,
			OsmAndFeatureOld.UNLOCK_ALL_FEATURES,
	};
	private final OsmAndFeatureOld[] selectedSubscriptionFeatures = {
			OsmAndFeatureOld.CONTOUR_LINES_HILLSHADE_MAPS,
			OsmAndFeatureOld.SEA_DEPTH_MAPS,
	};

	private final OsmAndFeatureOld[] planTypeFeatures = {
			OsmAndFeatureOld.CONTOUR_LINES_HILLSHADE_MAPS,
			OsmAndFeatureOld.SEA_DEPTH_MAPS,
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
