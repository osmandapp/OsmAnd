package net.osmand.plus.plugins.srtm;

import static net.osmand.plus.dashboard.DashboardOnMap.*;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.slider.Slider;

import net.osmand.plus.R;
import net.osmand.plus.configmap.ConfigureMapOptionFragment;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;

public class TerrainVisibilityFragment extends ConfigureMapOptionFragment {
	public static final String VISIBILITY = "visibility";

	private SRTMPlugin srtmPlugin;
	private TextView visibilityTv;
	private Slider visibilitySlider;

	private int originalVisibilityValue;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		srtmPlugin = PluginsHelper.getPlugin(SRTMPlugin.class);

		if (savedInstanceState != null && savedInstanceState.containsKey(VISIBILITY)) {
			originalVisibilityValue = savedInstanceState.getInt(VISIBILITY);
		} else if (srtmPlugin != null) {
			originalVisibilityValue = srtmPlugin.getTerrainTransparency();
		}
	}

	@Override
	protected DashboardType getBaseDashboardType() {
		return DashboardType.TERRAIN;
	}

	@Override
	public void onDestroy() {
		srtmPlugin.setTerrainTransparency(originalVisibilityValue, srtmPlugin.getTerrainMode());
		super.onDestroy();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(VISIBILITY, originalVisibilityValue);
	}

	@Override
	protected String getToolbarTitle() {
		return getString(R.string.gpx_visibility_txt);
	}

	@Override
	protected void onResetToDefault() {
		srtmPlugin.resetTransparencyToDefault();
		updateApplyButton(isChangesMade());
		setupSlider();
		refreshMap();
	}

	@Override
	protected void setupMainContent() {
		View view = themedInflater.inflate(R.layout.terrain_visibility_fragment, null, false);
		visibilitySlider = view.findViewById(R.id.transparency_slider);
		visibilityTv = view.findViewById(R.id.transparency_value_tv);

		setupSlider();
		contentContainer.addView(view);
	}

	@Override
	protected void onApplyButtonClick() {
		originalVisibilityValue = srtmPlugin.getTerrainTransparency();
	}

	private void setupSlider() {
		int transparencyValue = (int) (srtmPlugin.getTerrainTransparency() / 2.55);
		String transparency = transparencyValue + "%";
		visibilityTv.setText(transparency);

		visibilitySlider.addOnChangeListener(transparencySliderChangeListener);
		visibilitySlider.setValueTo(100);
		visibilitySlider.setValueFrom(0);
		visibilitySlider.setValue(transparencyValue);
		int profileColor = settings.getApplicationMode().getProfileColor(nightMode);
		UiUtilities.setupSlider(visibilitySlider, nightMode, profileColor);
	}

	private final Slider.OnChangeListener transparencySliderChangeListener = new Slider.OnChangeListener() {
		@Override
		public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
			if (fromUser) {
				String transparencyStr = (int) value + "%";
				visibilityTv.setText(transparencyStr);
				srtmPlugin.setTerrainTransparency((int) Math.ceil(value * 2.55), srtmPlugin.getTerrainMode());
				updateApplyButton(isChangesMade());
				refreshMap();
			}
		}
	};

	private boolean isChangesMade() {
		return srtmPlugin.getTerrainTransparency() != originalVisibilityValue;
	}

	public static void showInstance(@NonNull FragmentManager manager) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			manager.beginTransaction()
					.replace(R.id.fragmentContainer, new TerrainVisibilityFragment(), TAG)
					.addToBackStack(null)
					.commitAllowingStateLoss();
		}
	}
}