package net.osmand.plus.chooseplan;

public class ChoosePlanWikipediaDialogFragment extends ChoosePlanFreeBannerDialogFragment {
	public static final String TAG = ChoosePlanWikipediaDialogFragment.class.getSimpleName();

	private final OsmAndFeature[] osmLiveFeatures = {
			OsmAndFeature.WIKIPEDIA_OFFLINE,
			OsmAndFeature.WIKIVOYAGE_OFFLINE,
			OsmAndFeature.DAILY_MAP_UPDATES,
			OsmAndFeature.UNLIMITED_DOWNLOADS,
			OsmAndFeature.CONTOUR_LINES_HILLSHADE_MAPS,
			OsmAndFeature.SEA_DEPTH_MAPS,
			OsmAndFeature.UNLOCK_ALL_FEATURES,
	};
	private final OsmAndFeature[] selectedOsmLiveFeatures = {
			OsmAndFeature.WIKIPEDIA_OFFLINE,
			OsmAndFeature.WIKIVOYAGE_OFFLINE,
	};

	private final OsmAndFeature[] planTypeFeatures = {
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
