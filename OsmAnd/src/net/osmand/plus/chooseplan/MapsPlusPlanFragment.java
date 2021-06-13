package net.osmand.plus.chooseplan;

import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.R;

public class MapsPlusPlanFragment extends SelectedPlanFragment {

	public static void showInstance(@NonNull FragmentActivity activity) {
		MapsPlusPlanFragment fragment = new MapsPlusPlanFragment();
		fragment.show(activity.getSupportFragmentManager(), TAG);
	}

	@Override
	protected void initData(@Nullable Bundle args) {
		includedFeatures.clear();
		noIncludedFeatures.clear();
		for (OsmAndFeature feature : OsmAndFeature.values()) {
			if (feature.isAvailableInMapsPlus()) {
				includedFeatures.add(feature);
			} else {
				noIncludedFeatures.add(feature);
			}
		}
	}

	@Override
	protected int getHeaderBgColorId() {
		return nightMode ?
				R.color.list_background_color_dark :
				R.color.list_background_color_light;
	}

	@Override
	protected String getHeader() {
		return getString(R.string.maps_plus);
	}

	@Override
	protected String getTagline() {
		return getString(R.string.osmand_maps_plus_tagline);
	}

	@Override
	protected int getHeaderIconId() {
		return R.drawable.ic_action_osmand_maps_plus;
	}

	@Override
	protected Drawable getPreviewListCheckmark() {
		return getContentIcon(R.drawable.ic_action_done);
	}

}
