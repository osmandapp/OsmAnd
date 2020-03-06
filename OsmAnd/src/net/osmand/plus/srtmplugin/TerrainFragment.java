package net.osmand.plus.srtmplugin;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.slider.Slider;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.TerrainMode;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.inapp.InAppPurchaseHelper;

import org.apache.commons.logging.Log;

import java.util.List;


public class TerrainFragment extends BaseOsmAndFragment implements View.OnClickListener,
		Slider.OnSliderTouchListener, Slider.OnChangeListener {

	public static final String TAG = TerrainFragment.class.getSimpleName();
	private static final Log LOG = PlatformUtil.getLog(TerrainFragment.class.getSimpleName());
	private static final String SLOPES_WIKI_URL = "https://en.wikipedia.org/wiki/Grade_(slope)";
	private static final String PLUGIN_URL = "https://osmand.net/features/contour-lines-plugin";

	private OsmandApplication app;
	private UiUtilities uiUtilities;
	private OsmandSettings settings;
	private SRTMPlugin srtmPlugin;
	private boolean nightMode;
	private boolean srtmEnabled;
	private boolean terrainEnabled;

	private int colorProfile;
	private ColorStateList colorProfileStateList;
	private ColorStateList colorProfileInactiveStateList;

	private TextView emptyStateDescriptionTv;
	private TextView downloadDescriptionTv;
	private TextView transparencyValueTv;
	private TextView slopeReadMoreTv;
	private TextView descriptionTv;
	private TextView hillshadeBtn;
	private TextView minZoomTv;
	private TextView maxZoomTv;
	private TextView slopeBtn;
	private TextView titleTv;
	private TextView stateTv;
	private FrameLayout hillshadeBtnContainer;
	private FrameLayout slopeBtnContainer;
	private SwitchCompat switchCompat;
	private ImageView iconIv;
	private LinearLayout emptyState;
	private LinearLayout legendContainer;
	private LinearLayout contentContainer;
	private LinearLayout downloadContainer;
	private View legendBottomDivider;
	private View titleBottomDivider;
	private View legendTopDivider;
	private Slider transparencySlider;
	private Slider zoomSlider;

	public TerrainFragment() {

	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		app = requireMyApplication();
		settings = app.getSettings();
		uiUtilities = app.getUIUtilities();
		nightMode = !settings.isLightContent();
		srtmPlugin = OsmandPlugin.getPlugin(SRTMPlugin.class);
		srtmEnabled = OsmandPlugin.getEnabledPlugin(SRTMPlugin.class) != null
				|| InAppPurchaseHelper.isSubscribedToLiveUpdates(app);
		colorProfile = settings.getApplicationMode().getIconColorInfo().getColor(nightMode);
		colorProfileStateList = ColorStateList.valueOf(ContextCompat.getColor(app, colorProfile));
		colorProfileInactiveStateList = ColorStateList
				.valueOf(UiUtilities.getColorWithAlpha(colorProfile, 0.6f));
		terrainEnabled = srtmPlugin.isTerrainLayerEnabled();
		super.onCreate(savedInstanceState);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		inflater = UiUtilities.getInflater(app, nightMode);
		View root = inflater.inflate(R.layout.fragment_terrain, container, false);
		emptyStateDescriptionTv = root.findViewById(R.id.empty_state_description);
		downloadDescriptionTv = root.findViewById(R.id.download_description_tv);
		transparencyValueTv = root.findViewById(R.id.transparency_value_tv);
		legendBottomDivider = root.findViewById(R.id.legend_bottom_divider);
		transparencySlider = root.findViewById(R.id.transparency_slider);
		titleBottomDivider = root.findViewById(R.id.titleBottomDivider);
		legendTopDivider = root.findViewById(R.id.legend_top_divider);
		slopeReadMoreTv = root.findViewById(R.id.slope_read_more_tv);
		contentContainer = root.findViewById(R.id.content_container);
		legendContainer = root.findViewById(R.id.legend_container);
		switchCompat = root.findViewById(R.id.switch_compat);
		descriptionTv = root.findViewById(R.id.description);
		emptyState = root.findViewById(R.id.empty_state);
		titleTv = root.findViewById(R.id.title_tv);
		stateTv = root.findViewById(R.id.state_tv);
		iconIv = root.findViewById(R.id.icon_iv);
		slopeBtn = root.findViewById(R.id.slope_btn);
		zoomSlider = root.findViewById(R.id.zoom_slider);
		minZoomTv = root.findViewById(R.id.zoom_value_min);
		maxZoomTv = root.findViewById(R.id.zoom_value_max);
		hillshadeBtn = root.findViewById(R.id.hillshade_btn);
		slopeBtnContainer = root.findViewById(R.id.slope_btn_container);
		downloadContainer = root.findViewById(R.id.download_container);
		hillshadeBtnContainer = root.findViewById(R.id.hillshade_btn_container);

		titleTv.setText(R.string.shared_string_terrain);
		String wikiString = getString(R.string.shared_string_wikipedia);
		String readMoreText = String.format(
				getString(R.string.slope_read_more),
				wikiString
		);
		String emptyStateText = String.format(
				getString(R.string.ltr_or_rtl_combine_via_space),
				getString(R.string.terrain_empty_state_text),
				PLUGIN_URL
		);
		setupClickableText(slopeReadMoreTv, readMoreText, wikiString, SLOPES_WIKI_URL);
		setupClickableText(emptyStateDescriptionTv, emptyStateText, PLUGIN_URL, PLUGIN_URL);

		switchCompat.setChecked(terrainEnabled);
		hillshadeBtn.setOnClickListener(this);
		switchCompat.setOnClickListener(this);
		slopeBtn.setOnClickListener(this);

		transparencySlider.setTrackColorInactive(colorProfileInactiveStateList);
		transparencySlider.setTrackColorActive(colorProfileStateList);
		transparencySlider.setHaloColor(colorProfileInactiveStateList);
		transparencySlider.setThumbColor(colorProfileStateList);
		transparencySlider.setLabelBehavior(Slider.LABEL_GONE);
		zoomSlider.setTrackColorInactive(colorProfileInactiveStateList);
		zoomSlider.setTrackColorActive(colorProfileStateList);
		zoomSlider.setHaloColor(colorProfileInactiveStateList);
		zoomSlider.setThumbColor(colorProfileStateList);
		zoomSlider.setLabelBehavior(Slider.LABEL_GONE);
		zoomSlider.setTickColor(nightMode
				? ColorStateList.valueOf(R.color.color_white)
				: ColorStateList.valueOf(R.color.color_black));

		transparencySlider.addOnSliderTouchListener(this);
		zoomSlider.addOnSliderTouchListener(this);
		transparencySlider.addOnChangeListener(this);
		zoomSlider.addOnChangeListener(this);

		UiUtilities.setupCompoundButton(switchCompat, nightMode, UiUtilities.CompoundButtonType.PROFILE_DEPENDENT);

		updateUiMode();
		return root;
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
			case R.id.switch_compat:
				onSwitchClick();
				break;
			case R.id.hillshade_btn:
				setupTerrainMode(TerrainMode.HILLSHADE);
				break;
			case R.id.slope_btn:
				setupTerrainMode(TerrainMode.SLOPE);
				break;
			default:
				break;
		}
	}

	@Override
	public void onStartTrackingTouch(@NonNull Slider slider) {

	}

	@Override
	public void onStopTrackingTouch(@NonNull Slider slider) {
		switch (slider.getId()) {
			case R.id.transparency_slider:
				srtmPlugin.setTransparency((int) slider.getValue());
				break;
			case R.id.zoom_slider:
				List<Float> values = slider.getValues();
				srtmPlugin.setZoomValues(values.get(0).intValue(), values.get(1).intValue());
				break;
		}
		MapActivity mapActivity = (MapActivity) getActivity();
		srtmPlugin.updateLayers(mapActivity.getMapView(), mapActivity);
	}

	@Override
	public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
		if (fromUser) {
			switch (slider.getId()) {
				case R.id.transparency_slider:
					String transparency = (int) value + "%";
					transparencyValueTv.setText(transparency);
					break;
				case R.id.zoom_slider:
					List<Float> values = slider.getValues();
					minZoomTv.setText(String.valueOf(values.get(0).intValue()));
					maxZoomTv.setText(String.valueOf(values.get(1).intValue()));
					break;
			}
		}
	}

	private void updateUiMode() {
		TerrainMode mode = srtmPlugin.getTerrainMode();
		if (terrainEnabled) {
			int transparencyValue = srtmPlugin.getTransparency();
			String transparency = transparencyValue + "%";
			int minZoom = srtmPlugin.getMinZoom();
			int maxZoom = srtmPlugin.getMaxZoom();
			iconIv.setImageDrawable(uiUtilities.getIcon(R.drawable.ic_action_hillshade_dark, colorProfile));
			stateTv.setText(R.string.shared_string_enabled);
			transparencySlider.setValue(transparencyValue);
			transparencyValueTv.setText(transparency);
			zoomSlider.setValues((float) minZoom, (float) maxZoom);
			minZoomTv.setText(String.valueOf(minZoom));
			maxZoomTv.setText(String.valueOf(maxZoom));
			switch (mode) {
				case HILLSHADE:
					descriptionTv.setText(R.string.hillshade_description);
					downloadDescriptionTv.setText(R.string.hillshade_download_description);
//					zoomSlider.setValueFrom();
//					zoomSlider.setValueTo();
					break;
				case SLOPE:
					descriptionTv.setText(R.string.slope_description);
					downloadDescriptionTv.setText(R.string.slope_download_description);
					break;
			}
		} else {
			iconIv.setImageDrawable(uiUtilities.getIcon(
					R.drawable.ic_action_hillshade_dark,
					nightMode
							? R.color.icon_color_secondary_dark
							: R.color.icon_color_secondary_light));
			stateTv.setText(R.string.shared_string_disabled);
		}
		adjustGlobalVisibility();
		adjustLegendVisibility(mode);
		adjustModeButtons(mode);
	}

	private void adjustGlobalVisibility() {
		emptyState.setVisibility(terrainEnabled ? View.GONE : View.VISIBLE);
		titleBottomDivider.setVisibility(terrainEnabled ? View.GONE : View.VISIBLE);
		contentContainer.setVisibility(terrainEnabled ? View.VISIBLE : View.GONE);
	}

	private void adjustLegendVisibility(TerrainMode mode) {
		int visibility = TerrainMode.SLOPE.equals(mode) ? View.VISIBLE : View.GONE;
		legendContainer.setVisibility(visibility);
		legendBottomDivider.setVisibility(visibility);
		legendTopDivider.setVisibility(visibility);
	}

	private void adjustModeButtons(TerrainMode mode) {
		if (TerrainMode.SLOPE.equals(mode)) {
			slopeBtnContainer.setBackgroundResource(R.drawable.btn_border_right_active);
			slopeBtn.setTextColor(nightMode
					? getResources().getColor(R.color.text_color_primary_dark)
					: getResources().getColor(R.color.text_color_primary_light));
			hillshadeBtnContainer.setBackgroundResource(R.drawable.btn_border_left_inactive);
			hillshadeBtn.setTextColor(nightMode
					? getResources().getColor(R.color.active_color_primary_dark)
					: getResources().getColor(R.color.active_color_primary_light));
		} else {
			slopeBtnContainer.setBackgroundResource(R.drawable.btn_border_right_inactive);
			slopeBtn.setTextColor(nightMode
					? getResources().getColor(R.color.active_color_primary_dark)
					: getResources().getColor(R.color.active_color_primary_light));
			hillshadeBtnContainer.setBackgroundResource(R.drawable.btn_border_left_active);
			hillshadeBtn.setTextColor(nightMode
					? getResources().getColor(R.color.text_color_primary_dark)
					: getResources().getColor(R.color.text_color_primary_light));
		}
	}

	private void setupClickableText(TextView textView,
									String text,
									String clickableText,
									final String url) {
		SpannableString spannableString = new SpannableString(text);
		ClickableSpan clickableSpan = new ClickableSpan() {
			@Override
			public void onClick(@NonNull View view) {
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setData(Uri.parse(url));
				startActivity(i);
			}
		};
		try {
			int startIndex = text.indexOf(clickableText);
			spannableString.setSpan(clickableSpan, startIndex, startIndex + clickableText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			textView.setText(spannableString);
			textView.setMovementMethod(LinkMovementMethod.getInstance());
			textView.setHighlightColor(nightMode
					? getResources().getColor(R.color.active_color_primary_dark)
					: getResources().getColor(R.color.active_color_primary_light));
		} catch (RuntimeException e) {
			LOG.error("Error trying to find index of " + clickableText + " " + e);
		}
	}

	private void onSwitchClick() {
		terrainEnabled = !terrainEnabled;
		switchCompat.setChecked(terrainEnabled);
		srtmPlugin.setTerrainLayerEnabled(terrainEnabled);
		updateUiMode();
	}

	private void setupTerrainMode(TerrainMode mode) {
		TerrainMode currentMode = srtmPlugin.getTerrainMode();
		if (!currentMode.equals(mode)) {
			srtmPlugin.setTerrainMode(mode);
			updateUiMode();
		}
	}
}
