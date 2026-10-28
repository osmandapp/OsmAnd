package net.osmand.plus.views.layers;

import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.google.android.material.slider.Slider;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.rastermaps.LayerTransparencySeekbarMode;
import net.osmand.plus.plugins.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.utils.UiUtilities;

public class MapTransparencyHelper {

	private static CommonPreference<Integer> transparencySetting;
	private static CommonPreference<Float> parameterMinSetting;
	private static CommonPreference<Float> parameterMaxSetting;
	private static CommonPreference<Float> parameterStepSetting;
	private static CommonPreference<Float> parameterValueSetting;

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final MapControlsLayer layer;

	private Slider parameterSlider;
	private Slider transparencySlider;
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
		parameterBarLayout = null;
		transparencyBarLayout = null;
	}

	protected void initTransparencyBar() {
		MapActivity mapActivity = layer.requireMapActivity();
		parameterSlider = mapActivity.findViewById(R.id.layer_param_slider);
		transparencySlider = mapActivity.findViewById(R.id.map_transparency_slider);
		parameterBarLayout = mapActivity.findViewById(R.id.layer_param_layout);
		transparencyBarLayout = mapActivity.findViewById(R.id.map_transparency_layout);

		transparencySlider.setValueTo(255);
		if (transparencySetting != null) {
			transparencySlider.setValue(transparencySetting.get());
			transparencyBarLayout.setVisibility(View.VISIBLE);
		} else {
			transparencyBarLayout.setVisibility(View.GONE);
		}
		transparencySlider.addOnChangeListener((slider, value, fromUser) -> {
			if (transparencySetting != null) {
				transparencySetting.set((int) value);
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
					hideTransparencyBar();
					parameterBarLayout.setVisibility(View.VISIBLE);
					updateParameterSliderUi();
				} else {
					showTransparencyBar(settings.MAP_OVERLAY_TRANSPARENCY);
				}
			} else if (seekbarMode == LayerTransparencySeekbarMode.UNDERLAY && settings.MAP_UNDERLAY.get() != null) {
				showTransparencyBar(settings.MAP_TRANSPARENCY);
			}
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
		transparencySetting = preference;
		transparencyBarLayout.setVisibility(View.VISIBLE);
		transparencySlider.setValue(preference.get());
		updateTransparencySliderUi();
	}

	protected void updateTransparencySliderUi() {
		ApplicationMode appMode = app.getSettings().getApplicationMode();
		boolean nightMode = app.getDaynightHelper().isNightModeForMapControls();
		int selectedModeColor = appMode.getProfileColor(nightMode);
		UiUtilities.setupSlider(transparencySlider, nightMode, selectedModeColor);
	}

	public void hideTransparencyBar() {
		transparencyBarLayout.setVisibility(View.GONE);
		transparencySetting = null;
	}

	public void showParameterBar(@NonNull MapTileLayer layer) {
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
				hideTransparencyBar();
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
		boolean nightMode = app.getDaynightHelper().isNightModeForMapControls();
		int selectedModeColor = appMode.getProfileColor(nightMode);
		UiUtilities.setupSlider(parameterSlider, nightMode, selectedModeColor);
	}

	public void hideParameterBar() {
		parameterBarLayout.setVisibility(View.GONE);
		parameterMinSetting = null;
		parameterMaxSetting = null;
		parameterStepSetting = null;
		parameterValueSetting = null;
	}
}
