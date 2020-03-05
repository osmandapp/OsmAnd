package net.osmand.plus.srtmplugin;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatSeekBar;
import androidx.appcompat.widget.SwitchCompat;

import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.TerrainMode;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.chooseplan.ChoosePlanDialogFragment;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.download.DownloadValidationManager;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.inapp.InAppPurchaseHelper;

import java.io.IOException;
import java.util.List;

import static net.osmand.plus.srtmplugin.ContourLinesMenu.closeDashboard;

public class TerrainFragment extends BaseOsmAndFragment implements View.OnClickListener {

	public static final String TAG = TerrainFragment.class.getSimpleName();

	//	TODO replace with correct string
	private static final String SLOPES_WIKI_URL = "https://osmand.net/features/contour-lines-plugin";
	private static final String PLUGIN_URL = "https://osmand.net/features/contour-lines-plugin";

	private OsmandApplication app;
	private UiUtilities uiUtilities;
	private OsmandSettings settings;
	private SRTMPlugin srtmPlugin;
	private boolean nightMode;
	private boolean srtmEnabled;
	private boolean terrainEnabled;

	private int colorProfile;

	private TextView downloadDescriptionTv;
	private TextView emptyStateDescriptionTv;
	private TextView transparencyValueTv;
	private TextView slopeReadMoreTv;
	private TextView descriptionTv;
	private TextView titleTv;
	private TextView stateTv;
	private TextView slopeBtn;
	private TextView hillshadeBtn;
	private FrameLayout slopeBtnContainer;
	private FrameLayout hillshadeBtnContainer;
	private SwitchCompat switchCompat;
	private ImageView iconIv;
	private LinearLayout legendContainer;
	private LinearLayout emptyState;
	private LinearLayout contentContainer;
	private LinearLayout downloadContainer;
	private View legendTopDivider;
	private View legendBottomDivider;
	private View titleBottomDivider;
	private AppCompatSeekBar transparencySlider;

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
		hillshadeBtn = root.findViewById(R.id.hillshade_btn);
		slopeBtnContainer = root.findViewById(R.id.slope_btn_container);
		hillshadeBtnContainer = root.findViewById(R.id.hillshade_btn_container);
		downloadContainer = root.findViewById(R.id.download_container);

		titleTv.setText(R.string.shared_string_terrain);
		String emptyStateText = String.format(
				getString(R.string.ltr_or_rtl_combine_via_colon),
				getString(R.string.terrain_empty_state_text),
				PLUGIN_URL
		);
		setupClickableText(emptyStateDescriptionTv, emptyStateText, PLUGIN_URL, PLUGIN_URL);
		String wikiString = getString(R.string.shared_string_wikipedia);
		String readMoreText = String.format(
				getString(R.string.slope_read_more),
				wikiString
		);
		setupClickableText(slopeReadMoreTv, readMoreText, wikiString, SLOPES_WIKI_URL);

		switchCompat.setChecked(terrainEnabled);
		switchCompat.setOnClickListener(this);
		slopeBtn.setOnClickListener(this);
		hillshadeBtn.setOnClickListener(this);
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

	private void updateUiMode() {
		TerrainMode mode = srtmPlugin.getTerrainMode();
		if (terrainEnabled) {
			iconIv.setImageDrawable(uiUtilities.getIcon(R.drawable.ic_action_hillshade_dark, colorProfile));
			stateTv.setText(R.string.shared_string_enabled);
			switch (mode) {
				case HILLSHADE:
					descriptionTv.setText(R.string.hillshade_description);
					downloadDescriptionTv.setText(R.string.hillshade_download_description);
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
//			LOG.error("Error trying to find index of " + clickableText + " " + e);
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

