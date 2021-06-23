package net.osmand.plus.chooseplan;

public class ChoosePlanSubscriptionBannerDialogFragment extends ChoosePlanFreeBannerDialogFragment {
	public static final String TAG = ChoosePlanSubscriptionBannerDialogFragment.class.getSimpleName();

	@Override
	public OsmAndFeatureOld[] getPlanTypeFeatures() {
		return new OsmAndFeatureOld[] {};
	}
	@Override
	public OsmAndFeatureOld[] getSelectedPlanTypeFeatures() {
		return new OsmAndFeatureOld[] {};
	}
}
