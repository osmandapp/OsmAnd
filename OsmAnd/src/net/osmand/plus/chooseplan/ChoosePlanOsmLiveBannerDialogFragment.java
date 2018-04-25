package net.osmand.plus.chooseplan;

public class ChoosePlanOsmLiveBannerDialogFragment extends ChoosePlanFreeBannerDialogFragment {
	public static final String TAG = ChoosePlanOsmLiveBannerDialogFragment.class.getSimpleName();

	@Override
	public OsmAndFeature[] getPlanTypeFeatures() {
		return new OsmAndFeature[] {};
	}
	@Override
	public OsmAndFeature[] getSelectedPlanTypeFeatures() {
		return new OsmAndFeature[] {};
	}
}
