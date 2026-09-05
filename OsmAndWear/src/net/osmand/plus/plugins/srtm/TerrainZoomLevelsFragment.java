package net.osmand.plus.plugins.srtm;

import static net.osmand.plus.dashboard.DashboardOnMap.DashboardType.TERRAIN;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.slider.RangeSlider;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.configmap.ConfigureMapOptionFragment;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;

import java.util.List;

public class TerrainZoomLevelsFragment extends ConfigureMapOptionFragment {
	public static final String MIN_VALUE = "min_value";
	public static final String MAX_VALUE = "max_value";

	private SRTMPlugin srtmPlugin;
	private TextView minZoomTv;
	private TextView maxZoomTv;
	private RangeSlider zoomSlider;

	private int originalMinZoomValue;
	private int originalMaxZoomValue;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		srtmPlugin = PluginsHelper.getPlugin(SRTMPlugin.class);

		if (savedInstanceState != null && savedInstanceState.containsKey(MAX_VALUE) && savedInstanceState.containsKey(MIN_VALUE)) {
			originalMaxZoomValue = savedInstanceState.getInt(MAX_VALUE);
			originalMinZoomValue = savedInstanceState.getInt(MIN_VALUE);
		} else if (srtmPlugin != null) {
			originalMaxZoomValue = srtmPlugin.getTerrainMaxZoom();
			originalMinZoomValue = srtmPlugin.getTerrainMinZoom();
		}

		MapActivity activity = requireMapActivity();
		activity.getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			@Override
			public void handleOnBackPressed() {
				activity.getSupportFragmentManager().popBackStack();
				activity.getDashboard().setDashboardVisibility(true, TERRAIN, false);
			}
		});
	}

	@Override
	public void onDestroy() {
		srtmPlugin.setTerrainZoomValues(originalMinZoomValue, originalMaxZoomValue, srtmPlugin.getTerrainMode());
		super.onDestroy();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(MAX_VALUE, originalMaxZoomValue);
		outState.putInt(MIN_VALUE, originalMinZoomValue);
	}

	@Nullable
	@Override
	protected String getToolbarTitle() {
		return getString(R.string.shared_string_zoom_levels);
	}

	@Override
	protected void resetToDefault() {
		srtmPlugin.resetZoomLevelsToDefault();
		updateApplyButton(isChangesMade());
		setupSlider();
		refreshMap();
	}

	@Override
	protected void setupMainContent(@NonNull ViewGroup container) {
		View view = themedInflater.inflate(R.layout.terrain_zoom_levels_fragment, container, false);
		zoomSlider = view.findViewById(R.id.zoom_slider);
		minZoomTv = view.findViewById(R.id.zoom_value_min);
		maxZoomTv = view.findViewById(R.id.zoom_value_max);

		setupSlider();
		container.addView(view);
	}

	@Override
	protected void applyChanges() {
		originalMinZoomValue = srtmPlugin.getTerrainMinZoom();
		originalMaxZoomValue = srtmPlugin.getTerrainMaxZoom();
	}

	private void setupSlider() {
		minZoomTv.setText(getString(R.string.ltr_or_rtl_combine_via_colon, getString(R.string.shared_string_min), String.valueOf(srtmPlugin.getTerrainMinZoom())));
		maxZoomTv.setText(getString(R.string.ltr_or_rtl_combine_via_colon, getString(R.string.shared_string_max), String.valueOf(srtmPlugin.getTerrainMaxZoom())));

		zoomSlider.setValueFrom(SRTMPlugin.TERRAIN_MIN_SUPPORTED_ZOOM);
		zoomSlider.setValueTo(SRTMPlugin.TERRAIN_MAX_SUPPORTED_ZOOM);
		zoomSlider.setValues((float) srtmPlugin.getTerrainMinZoom(), (float) srtmPlugin.getTerrainMaxZoom());

		int profileColor = settings.getApplicationMode().getProfileColor(nightMode);
		UiUtilities.setupSlider(zoomSlider, nightMode, profileColor, true);
		zoomSlider.addOnChangeListener(zoomSliderChangeListener);
	}

	private final RangeSlider.OnChangeListener zoomSliderChangeListener = new RangeSlider.OnChangeListener() {
		@Override
		public void onValueChange(@NonNull RangeSlider slider, float value, boolean fromUser) {
			List<Float> values = slider.getValues();
			if (values.size() > 1) {
				minZoomTv.setText(getString(R.string.ltr_or_rtl_combine_via_colon, getString(R.string.shared_string_min), String.valueOf(values.get(0).intValue())));
				maxZoomTv.setText(getString(R.string.ltr_or_rtl_combine_via_colon, getString(R.string.shared_string_max), String.valueOf(values.get(1).intValue())));
				srtmPlugin.setTerrainZoomValues(values.get(0).intValue(), values.get(1).intValue(), srtmPlugin.getTerrainMode());
				updateApplyButton(isChangesMade());
				refreshMap();
			}
		}
	};

	private boolean isChangesMade() {
		return srtmPlugin.getTerrainMinZoom() != originalMinZoomValue || srtmPlugin.getTerrainMaxZoom() != originalMaxZoomValue;
	}

	public static void showInstance(@NonNull FragmentManager manager) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			manager.beginTransaction()
					.replace(R.id.fragmentContainer, new TerrainZoomLevelsFragment(), TAG)
					.addToBackStack(null)
					.commitAllowingStateLoss();
		}
	}
}