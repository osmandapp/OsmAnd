package net.osmand.plus.chooseplan;

public class ChoosePlanWikivoyageDialogFragment extends ChoosePlanSubscriptionBannerDialogFragment {
	public static final String TAG = ChoosePlanWikivoyageDialogFragment.class.getSimpleName();

	private final OsmAndFeature[] subscriptionFeatures = {
			OsmAndFeature.WIKIVOYAGE_OFFLINE,
			OsmAndFeature.WIKIPEDIA_OFFLINE,
			OsmAndFeature.DAILY_MAP_UPDATES,
			OsmAndFeature.UNLIMITED_DOWNLOADS,
			OsmAndFeature.CONTOUR_LINES_HILLSHADE_MAPS,
			OsmAndFeature.SEA_DEPTH_MAPS,
			OsmAndFeature.UNLOCK_ALL_FEATURES,
	};
	private final OsmAndFeature[] selectedSubscriptionFeatures = {
			OsmAndFeature.WIKIVOYAGE_OFFLINE,
			OsmAndFeature.WIKIPEDIA_OFFLINE,
	};

	@Override
	public OsmAndFeature[] getSubscriptionFeatures() {
		return subscriptionFeatures;
	}

	@Override
	public OsmAndFeature[] getSelectedSubscriptionFeatures() {
		return selectedSubscriptionFeatures;
	}

}
