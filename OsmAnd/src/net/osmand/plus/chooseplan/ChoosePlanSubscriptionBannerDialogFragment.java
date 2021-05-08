package net.osmand.plus.chooseplan;

public class ChoosePlanSubscriptionBannerDialogFragment extends ChoosePlanFreeBannerDialogFragment {
	public static final String TAG = ChoosePlanSubscriptionBannerDialogFragment.class.getSimpleName();

	@Override
	public OsmAndFeature[] getPlanTypeFeatures() {
		return new OsmAndFeature[] {};
	}
	@Override
	public OsmAndFeature[] getSelectedPlanTypeFeatures() {
		return new OsmAndFeature[] {};
	}
}
