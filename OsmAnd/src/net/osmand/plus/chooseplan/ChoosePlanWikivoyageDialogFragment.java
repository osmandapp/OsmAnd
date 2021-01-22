package net.osmand.plus.chooseplan;

public class ChoosePlanWikivoyageDialogFragment extends ChoosePlanOsmLiveBannerDialogFragment {
	public static final String TAG = ChoosePlanWikivoyageDialogFragment.class.getSimpleName();

	private final OsmAndFeature[] osmLiveFeatures = {
			OsmAndFeature.WIKIVOYAGE_OFFLINE,
			OsmAndFeature.WIKIPEDIA_OFFLINE,
			OsmAndFeature.DAILY_MAP_UPDATES,
			OsmAndFeature.UNLIMITED_DOWNLOADS,
			OsmAndFeature.CONTOUR_LINES_HILLSHADE_MAPS,
			OsmAndFeature.SEA_DEPTH_MAPS,
			OsmAndFeature.UNLOCK_ALL_FEATURES,
	};
	private final OsmAndFeature[] selectedOsmLiveFeatures = {
			OsmAndFeature.WIKIVOYAGE_OFFLINE,
			OsmAndFeature.WIKIPEDIA_OFFLINE,
	};

	@Override
	public OsmAndFeature[] getOsmLiveFeatures() {
		return osmLiveFeatures;
	}

	@Override
	public OsmAndFeature[] getSelectedOsmLiveFeatures() {
		return selectedOsmLiveFeatures;
	}

}
