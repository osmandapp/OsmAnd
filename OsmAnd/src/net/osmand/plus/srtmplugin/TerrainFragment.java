package net.osmand.plus.srtmplugin;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatSeekBar;
import android.support.v7.widget.SwitchCompat;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.TerrainMode;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.widgets.TextViewEx;

public class TerrainFragment extends BaseOsmAndFragment {

	public static final String TAG = TerrainFragment.class.getSimpleName();

	private static final String SLOPES_WIKI_URL = "";
	private static final String PLUGIN_URL = "";

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
	private SwitchCompat switchCompat;
	private ImageView iconIv;
	private LinearLayout legendContainer;
	private LinearLayout emptyState;
	private LinearLayout contentContainer;
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
		titleTv = root.findViewById(R.id.title_tv);
		stateTv = root.findViewById(R.id.state_tv);
		iconIv = root.findViewById(R.id.icon_iv);

		adjustUi();

		return root;
	}

	private void adjustUi() {
		TerrainMode mode = srtmPlugin.getTerrainMode();
		switch (mode) {
			case HILLSHADE:
				descriptionTv.setText(R.string.hillshade_description);
				downloadDescriptionTv.setText(R.string.hillshade_download_description);
				break;
			case SLOPE:
				descriptionTv.setText(R.string.slope_description);
				downloadDescriptionTv.setText(R.string.slope_download_description);
				String wikiString = getString(R.string.shared_string_wikipedia);
				setupClickableText(slopeReadMoreTv,
						String.format(getString(R.string.slope_read_more), wikiString),
						wikiString,
						SLOPES_WIKI_URL);
				break;
		}

		if (!terrainEnabled) {
			setupClickableText(emptyStateDescriptionTv,
					String.format(getString(R.string.slope_read_more), PLUGIN_URL),
					PLUGIN_URL,
					PLUGIN_URL);
		}
		adjustGlobalVisibility();
		adjustLegendVisibility(mode);
	}

	private void adjustGlobalVisibility() {
		emptyStateDescriptionTv.setVisibility(terrainEnabled ? View.GONE : View.VISIBLE);
		titleBottomDivider.setVisibility(terrainEnabled ? View.GONE : View.VISIBLE);
		contentContainer.setVisibility(terrainEnabled ? View.VISIBLE : View.GONE);
	}

	private void adjustLegendVisibility(TerrainMode mode) {
		int visibility = TerrainMode.SLOPE.equals(mode) ? View.VISIBLE : View.GONE;
		legendContainer.setVisibility(visibility);
		legendBottomDivider.setVisibility(visibility);
		legendTopDivider.setVisibility(visibility);
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
}
