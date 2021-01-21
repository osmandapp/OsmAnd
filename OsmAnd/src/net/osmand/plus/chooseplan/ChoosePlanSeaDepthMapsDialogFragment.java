package net.osmand.plus.chooseplan;

public class ChoosePlanSeaDepthMapsDialogFragment extends ChoosePlanFreeBannerDialogFragment {
	public static final String TAG = ChoosePlanSeaDepthMapsDialogFragment.class.getSimpleName();

	private final OsmAndFeature[] osmLiveFeatures = {
			OsmAndFeature.SEA_DEPTH_MAPS,
			OsmAndFeature.CONTOUR_LINES_HILLSHADE_MAPS,
			OsmAndFeature.WIKIPEDIA_OFFLINE,
			OsmAndFeature.WIKIVOYAGE_OFFLINE,
			OsmAndFeature.DAILY_MAP_UPDATES,
			OsmAndFeature.UNLIMITED_DOWNLOADS,
			OsmAndFeature.UNLOCK_ALL_FEATURES,
	};
	private final OsmAndFeature[] selectedOsmLiveFeatures = {
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
}
