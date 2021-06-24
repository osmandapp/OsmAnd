package net.osmand.plus.chooseplan;

public class ChoosePlanWikipediaDialogFragment extends ChoosePlanFreeBannerDialogFragment {
	public static final String TAG = ChoosePlanWikipediaDialogFragment.class.getSimpleName();

	private final OsmAndFeatureOld[] subscriptionFeatures = {
			OsmAndFeatureOld.WIKIPEDIA_OFFLINE,
			OsmAndFeatureOld.WIKIVOYAGE_OFFLINE,
			OsmAndFeatureOld.DAILY_MAP_UPDATES,
			OsmAndFeatureOld.UNLIMITED_DOWNLOADS,
			OsmAndFeatureOld.CONTOUR_LINES_HILLSHADE_MAPS,
			OsmAndFeatureOld.SEA_DEPTH_MAPS,
			OsmAndFeatureOld.UNLOCK_ALL_FEATURES,
	};
	private final OsmAndFeatureOld[] selectedSubscriptionFeatures = {
			OsmAndFeatureOld.WIKIPEDIA_OFFLINE,
			OsmAndFeatureOld.WIKIVOYAGE_OFFLINE,
	};

	private final OsmAndFeatureOld[] planTypeFeatures = {
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
