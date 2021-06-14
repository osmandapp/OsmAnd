package net.osmand.plus.chooseplan;

import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.R;

import java.util.Arrays;

public class OsmAndProPlanFragment extends SelectedPlanFragment {

	public static void showInstance(@NonNull FragmentActivity activity) {
		OsmAndProPlanFragment fragment = new OsmAndProPlanFragment();
		fragment.show(activity.getSupportFragmentManager(), TAG);
	}


	@Override
	protected void initData(@Nullable Bundle args) {
		features.remove(OsmAndFeature.MONTHLY_MAP_UPDATES);
		features.remove(OsmAndFeature.WIKIPEDIA);
		features.remove(OsmAndFeature.WIKIVOYAGE);

		includedFeatures.addAll(features);
	}

	@Override
	protected int getHeaderBgColorId() {
		return nightMode ?
				R.color.activity_background_color_dark :
				R.color.activity_background_color_light;
	}

	@Override
	protected String getHeader() {
		return getString(R.string.osmand_pro);
	}

	@Override
	protected String getTagline() {
		return getString(R.string.osmand_pro_tagline);
	}

	@Override
	protected int getHeaderIconId() {
		return R.drawable.ic_action_osmand_pro_logo;
	}

	@Override
	protected Drawable getPreviewListCheckmark() {
		return getCheckmark();
	}

}
