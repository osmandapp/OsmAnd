package net.osmand.plus.track.fragments;

import static net.osmand.plus.plugins.srtm.SRTMPlugin.MAX_VERTICAL_EXAGGERATION;
import static net.osmand.plus.plugins.srtm.SRTMPlugin.MIN_VERTICAL_EXAGGERATION;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.configmap.MapOptionSliderFragment;
import net.osmand.plus.track.TrackDrawInfo;
import net.osmand.plus.utils.AndroidUtils;

public class TrackWallHeightFragment extends MapOptionSliderFragment {

	public static final float MIN_WALL_HEIGHT = 0;
	public static final float MAX_WALL_HEIGHT = 2000;

	private TrackDrawInfo trackDrawInfo;

	@Nullable
	@Override
	protected String getToolbarTitle() {
		return getString(trackDrawInfo.isFixedHeight() ? R.string.vertical_exaggeration : R.string.wall_height);
	}

	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState != null) {
			trackDrawInfo = new TrackDrawInfo(savedInstanceState);
		} else {
			value = originalValue = trackDrawInfo.isFixedHeight()
					? trackDrawInfo.getElevationMeters() : trackDrawInfo.getAdditionalExaggeration();
		}
	}

	@Override
	protected void setupMainContent(@NonNull ViewGroup container) {
		super.setupMainContent(container);

		View view = container.findViewById(R.id.slider_container);
		TextView description = view.findViewById(R.id.description);
		description.setText(R.string.track_vertical_exaggeration_description);
	}

	@Override
	protected void setupSlider(@NonNull View view) {
		super.setupSlider(view);

		boolean fixedHeight = trackDrawInfo.isFixedHeight();
		slider.setValueTo(fixedHeight ? MAX_WALL_HEIGHT : MAX_VERTICAL_EXAGGERATION);
		slider.setValueFrom(fixedHeight ? MIN_WALL_HEIGHT : MIN_VERTICAL_EXAGGERATION);
		slider.setStepSize(fixedHeight ? 100 : 0.1f);
	}

	@Override
	public void onResume() {
		super.onResume();
		app.getOsmandMap().getMapLayers().getGpxLayer().setTrackDrawInfo(trackDrawInfo);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle bundle) {
		super.onSaveInstanceState(bundle);

		if (trackDrawInfo != null) {
			trackDrawInfo.saveToBundle(bundle);
		}
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull Fragment target, @NonNull TrackDrawInfo drawInfo) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			TrackWallHeightFragment fragment = new TrackWallHeightFragment();
			fragment.trackDrawInfo = drawInfo;
			fragment.setTargetFragment(target, 0);
			manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(null)
					.commitAllowingStateLoss();
		}
	}
}
