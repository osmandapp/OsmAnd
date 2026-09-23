package net.osmand.plus.views.layers;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.slider.Slider;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.rastermaps.LayerTransparencySeekbarMode;
import net.osmand.plus.plugins.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.plugins.srtm.SRTMPlugin;
import net.osmand.plus.plugins.srtm.building.Buildings3DSunHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.utils.UiUtilities;

public class MapTransparencyHelper {

	private static final int TRANSPARENCY_MAX = 255;

	private static CommonPreference<Integer> transparencySetting;
	// The same slider picks the time of day for 3D buildings shadows
	private static boolean sunTimeMode;
	private static CommonPreference<Float> parameterMinSetting;
	private static CommonPreference<Float> parameterMaxSetting;
	private static CommonPreference<Float> parameterStepSetting;
	private static CommonPreference<Float> parameterValueSetting;

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final MapControlsLayer layer;

	private Slider parameterSlider;
	private Slider transparencySlider;
	private TextView transparencyValue;
	private LinearLayout parameterBarLayout;
	private LinearLayout transparencyBarLayout;

	public MapTransparencyHelper(@NonNull MapControlsLayer layer) {
		this.layer = layer;
		this.app = layer.getApplication();
		this.settings = app.getSettings();
	}

	protected void destroyTransparencyBar() {
		parameterSlider = null;
		transparencySlider = null;
		transparencyValue = null;
		parameterBarLayout = null;
		transparencyBarLayout = null;
	}

	protected void initTransparencyBar() {
		MapActivity mapActivity = layer.requireMapActivity();
		parameterSlider = mapActivity.findViewById(R.id.layer_param_slider);
		transparencySlider = mapActivity.findViewById(R.id.map_transparency_slider);
		transparencyValue = mapActivity.findViewById(R.id.map_transparency_value);
		parameterBarLayout = mapActivity.findViewById(R.id.layer_param_layout);
		transparencyBarLayout = mapActivity.findViewById(R.id.map_transparency_layout);

		setupSliderRange();
		if (transparencySetting != null) {
			transparencySlider.setValue(transparencySetting.get());
			transparencyBarLayout.setVisibility(View.VISIBLE);
			updateSunTimeValue();
		} else {
			transparencyBarLayout.setVisibility(View.GONE);
		}
		transparencySlider.addOnChangeListener((slider, value, fromUser) -> {
			if (transparencySetting != null) {
				transparencySetting.set((int) value);
				updateSunTimeValue();
				mapActivity.refreshMap();
			}
		});
		boolean showParameterSlider = false;
		if (parameterMinSetting != null && parameterMaxSetting != null
				&& parameterStepSetting != null && parameterValueSetting != null) {
			float paramMin = parameterMinSetting.get();
			float paramMax = parameterMaxSetting.get();
			float paramStep = parameterStepSetting.get();
			float paramValue = parameterValueSetting.get();
			if (paramMin < paramMax && paramStep < Math.abs(paramMax - paramMin) && paramStep > 0
					&& paramValue >= paramMin && paramValue <= paramMax) {
				parameterSlider.setValueFrom(paramMin);
				parameterSlider.setValueTo(paramMax);
				parameterSlider.setStepSize(paramStep);
				parameterSlider.setValue(paramValue);
				showParameterSlider = true;
			}
		}
		parameterSlider.addOnChangeListener((slider, value, fromUser) -> {
			if (parameterValueSetting != null) {
				parameterValueSetting.set(value);
				mapActivity.refreshMap();
			}
		});

		LayerTransparencySeekbarMode seekbarMode = settings.LAYER_TRANSPARENCY_SEEKBAR_MODE.get();
		if (PluginsHelper.isActive(OsmandRasterMapsPlugin.class)) {
			if (seekbarMode == LayerTransparencySeekbarMode.OVERLAY && settings.MAP_OVERLAY.get() != null) {
				if (showParameterSlider) {
					hideBar();
					parameterBarLayout.setVisibility(View.VISIBLE);
					updateParameterSliderUi();
				} else {
					showTransparencyBar(settings.MAP_OVERLAY_TRANSPARENCY);
				}
			} else if (seekbarMode == LayerTransparencySeekbarMode.UNDERLAY && settings.MAP_UNDERLAY.get() != null) {
				showTransparencyBar(settings.MAP_TRANSPARENCY);
			}
		}
		SRTMPlugin srtmPlugin = PluginsHelper.getActivePlugin(SRTMPlugin.class);
		if (transparencySetting == null && srtmPlugin != null && srtmPlugin.isBuildingsSunTimeSliderEnabled()) {
			showSunTimeBar(srtmPlugin.BUILDINGS_3D_SUN_TIME);
		}
	}

	private void updateSunTimeValue() {
		if (transparencyValue != null) {
			AndroidUiHelper.updateVisibility(transparencyValue, sunTimeMode);
			if (sunTimeMode && transparencySlider != null) {
				transparencyValue.setText(Buildings3DSunHelper.formatMinuteOfDay((int) transparencySlider.getValue()));
			}
		}
	}

	private void setupSliderRange() {
		transparencySlider.setValueFrom(0);
		if (sunTimeMode) {
			transparencySlider.setValueTo(Buildings3DSunHelper.MINUTES_IN_DAY - 1);
			transparencySlider.setStepSize(1);
			transparencySlider.setLabelFormatter(value -> Buildings3DSunHelper.formatMinuteOfDay((int) value));
		} else {
			transparencySlider.setValueTo(TRANSPARENCY_MAX);
			transparencySlider.setStepSize(0);
			transparencySlider.setLabelFormatter(null);
		}
	}

	public void showSunTimeBar(@NonNull CommonPreference<Integer> preference) {
		if (transparencySlider == null) {
			return;
		}
		hideParameterBar();
		sunTimeMode = true;
		transparencySetting = preference;
		setupSliderRange();
		transparencyBarLayout.setVisibility(View.VISIBLE);
		transparencySlider.setValue(Math.max(0, Math.min(preference.get(), Buildings3DSunHelper.MINUTES_IN_DAY - 1)));
		updateSunTimeValue();
		updateTransparencySliderUi();
	}

	public void hideSunTimeBar() {
		if (sunTimeMode) {
			hideBar();
		}
	}

	private void restoreSunTimeBarIfNeeded() {
		SRTMPlugin srtmPlugin = PluginsHelper.getActivePlugin(SRTMPlugin.class);
		if (srtmPlugin != null && srtmPlugin.isBuildingsSunTimeSliderEnabled()) {
			showSunTimeBar(srtmPlugin.BUILDINGS_3D_SUN_TIME);
		}
	}

	public void updateTransparencySliderValue() {
		LayerTransparencySeekbarMode seekbarMode = settings.LAYER_TRANSPARENCY_SEEKBAR_MODE.get();
		if (PluginsHelper.isActive(OsmandRasterMapsPlugin.class)) {
			if (seekbarMode == LayerTransparencySeekbarMode.OVERLAY && settings.MAP_OVERLAY.get() != null) {
				transparencySlider.setValue(settings.MAP_OVERLAY_TRANSPARENCY.get());
			} else if (seekbarMode == LayerTransparencySeekbarMode.UNDERLAY && settings.MAP_UNDERLAY.get() != null) {
				transparencySlider.setValue(settings.MAP_TRANSPARENCY.get());
			}
		}
	}

	public void showTransparencyBar(@NonNull CommonPreference<Integer> preference) {
		hideParameterBar();
		sunTimeMode = false;
		transparencySetting = preference;
		setupSliderRange();
		updateSunTimeValue();
		transparencyBarLayout.setVisibility(View.VISIBLE);
		transparencySlider.setValue(preference.get());
		updateTransparencySliderUi();
	}

	protected void updateTransparencySliderUi() {
		ApplicationMode appMode = app.getSettings().getApplicationMode();
		boolean nightMode = app.getDaynightHelper().isNightMode(ThemeUsageContext.OVER_MAP);
		int selectedModeColor = appMode.getProfileColor(nightMode);
		UiUtilities.setupSlider(transparencySlider, nightMode, selectedModeColor);
	}

	// Hides the overlay/underlay transparency, the time of day bar stays or comes back
	public void hideTransparencyBar() {
		if (!sunTimeMode) {
			hideBar();
			restoreSunTimeBarIfNeeded();
		}
	}

	private void hideBar() {
		AndroidUiHelper.updateVisibility(transparencyBarLayout, false);
		transparencySetting = null;
		sunTimeMode = false;
		updateSunTimeValue();
	}

	public void showParameterBar(@NonNull MapTileLayer layer) {
		hideBar();
		CommonPreference<Float> paramMinPref = layer.getParamMinPref();
		CommonPreference<Float> paramMaxPref = layer.getParamMaxPref();
		CommonPreference<Float> paramStepPref = layer.getParamStepPref();
		CommonPreference<Float> paramValuePref = layer.getParamValuePref();
		parameterMinSetting = paramMinPref;
		parameterMaxSetting = paramMaxPref;
		parameterStepSetting = paramStepPref;
		parameterValueSetting = paramValuePref;

		if (paramMinPref != null && paramMaxPref != null && paramStepPref != null && paramValuePref != null) {
			float paramMin = paramMinPref.get();
			float paramMax = paramMaxPref.get();
			float paramStep = paramStepPref.get();
			float paramValue = paramValuePref.get();
			if (paramMin < paramMax && paramStep < Math.abs(paramMax - paramMin) && paramStep > 0
					&& paramValue >= paramMin && paramValue <= paramMax) {
				parameterBarLayout.setVisibility(View.VISIBLE);
				parameterSlider.setValueFrom(paramMin);
				parameterSlider.setValueTo(paramMax);
				parameterSlider.setStepSize(paramStep);
				parameterSlider.setValue(paramValue);
				updateParameterSliderUi();
				layer.setupParameterListener();
			}
		}
	}

	private void updateParameterSliderUi() {
		ApplicationMode appMode = app.getSettings().getApplicationMode();
		boolean nightMode = app.getDaynightHelper().isNightMode(ThemeUsageContext.OVER_MAP);
		int selectedModeColor = appMode.getProfileColor(nightMode);
		UiUtilities.setupSlider(parameterSlider, nightMode, selectedModeColor);
	}

	public void hideParameterBar() {
		AndroidUiHelper.updateVisibility(parameterBarLayout, false);
		parameterMinSetting = null;
		parameterMaxSetting = null;
		parameterStepSetting = null;
		parameterValueSetting = null;
	}
}
