package net.osmand.plus.chooseplan;

public class ChoosePlanWikivoyageDialogFragment extends ChoosePlanSubscriptionBannerDialogFragment {
	public static final String TAG = ChoosePlanWikivoyageDialogFragment.class.getSimpleName();

	private final OsmAndFeatureOld[] subscriptionFeatures = {
			OsmAndFeatureOld.WIKIVOYAGE_OFFLINE,
			OsmAndFeatureOld.WIKIPEDIA_OFFLINE,
			OsmAndFeatureOld.DAILY_MAP_UPDATES,
			OsmAndFeatureOld.UNLIMITED_DOWNLOADS,
			OsmAndFeatureOld.CONTOUR_LINES_HILLSHADE_MAPS,
			OsmAndFeatureOld.SEA_DEPTH_MAPS,
			OsmAndFeatureOld.UNLOCK_ALL_FEATURES,
	};
	private final OsmAndFeatureOld[] selectedSubscriptionFeatures = {
			OsmAndFeatureOld.WIKIVOYAGE_OFFLINE,
			OsmAndFeatureOld.WIKIPEDIA_OFFLINE,
	};

	@Override
	public OsmAndFeatureOld[] getSubscriptionFeatures() {
		return subscriptionFeatures;
	}

	@Override
	public OsmAndFeatureOld[] getSelectedSubscriptionFeatures() {
		return selectedSubscriptionFeatures;
	}

}
