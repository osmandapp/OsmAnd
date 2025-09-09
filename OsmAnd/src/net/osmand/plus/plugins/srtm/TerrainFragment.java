package net.osmand.plus.plugins.srtm;

import static net.osmand.IndexConstants.GEOTIFF_SQLITE_CACHE_DIR;
import static net.osmand.plus.plugins.srtm.TerrainMode.TerrainType.HEIGHT;
import static net.osmand.plus.plugins.srtm.TerrainMode.TerrainType.HILLSHADE;
import static net.osmand.plus.plugins.srtm.TerrainMode.TerrainType.SLOPE;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.github.mikephil.charting.charts.GradientChart;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseFullScreenFragment;
import net.osmand.plus.card.color.palette.gradient.GradientUiHelper;
import net.osmand.plus.charts.ChartUtils;
import net.osmand.plus.chooseplan.ChoosePlanFragment;
import net.osmand.plus.chooseplan.OsmAndFeature;
import net.osmand.plus.chooseplan.button.PurchasingUtils;
import net.osmand.plus.configmap.TerrainZoomLevelsController;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.download.local.LocalItemType;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.inapp.InAppPurchaseUtils;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FontCache;
import net.osmand.plus.utils.InsetsUtils.InsetSide;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;
import net.osmand.plus.widgets.popup.PopUpMenuWidthMode;
import net.osmand.plus.widgets.style.CustomClickableSpan;
import net.osmand.plus.widgets.style.CustomTypefaceSpan;
import net.osmand.shared.ColorPalette;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public class TerrainFragment extends BaseFullScreenFragment implements View.OnClickListener, DownloadEvents {

	public static final String TAG = TerrainFragment.class.getSimpleName();
	private static final Log LOG = PlatformUtil.getLog(TerrainFragment.class.getSimpleName());

	private SRTMPlugin srtmPlugin;
	private boolean terrainEnabled;

	private int profileColor;

	private TextView visibilityTv;
	private TextView zoomLevelsTv;
	private TextView coloSchemeTv;
	private TextView cacheSizeValueTv;

	private TextView downloadDescriptionTv;
	private TextView descriptionTv;
	private TextView stateTv;
	private SwitchCompat switchCompat;
	private ImageView iconIv;
	private ImageView proIv;
	private LinearLayout emptyState;
	private View emptyStateDivider;
	private LinearLayout contentContainer;
	private View titleBottomDivider;
	private GradientChart gradientChart;

	private DownloadMapsCard downloadMapsCard;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		srtmPlugin = PluginsHelper.requirePlugin(SRTMPlugin.class);
		terrainEnabled = srtmPlugin.isTerrainLayerEnabled();
	}

	@Override
	protected boolean isUsedOnMap() {
		return true;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View root = inflate(R.layout.fragment_terrain, container, false);
		profileColor = getAppMode().getProfileColor(nightMode);

		showHideTopShadow(root);

		visibilityTv = root.findViewById(R.id.visibility_value);
		zoomLevelsTv = root.findViewById(R.id.zoom_value);
		coloSchemeTv = root.findViewById(R.id.color_scheme_name);
		cacheSizeValueTv = root.findViewById(R.id.cache_size_value);
		View cacheButton = root.findViewById(R.id.cache_button);
		cacheButton.setOnClickListener(view -> showCacheScreen());

		TextView emptyStateDescriptionTv = root.findViewById(R.id.empty_state_description);
		TextView titleTv = root.findViewById(R.id.title_tv);
		downloadDescriptionTv = root.findViewById(R.id.download_description_tv);
		titleBottomDivider = root.findViewById(R.id.titleBottomDivider);
		contentContainer = root.findViewById(R.id.content_container);
		switchCompat = root.findViewById(R.id.switch_compat);
		descriptionTv = root.findViewById(R.id.description);
		emptyState = root.findViewById(R.id.empty_state);
		emptyStateDivider = root.findViewById(R.id.empty_state_divider);
		stateTv = root.findViewById(R.id.state_tv);
		iconIv = root.findViewById(R.id.icon_iv);
		proIv = root.findViewById(R.id.pro_icon);
		gradientChart = root.findViewById(R.id.chart);
		View modifyButton = root.findViewById(R.id.button_modify);
		downloadMapsCard = new DownloadMapsCard(app, srtmPlugin, root.findViewById(R.id.download_maps_card), nightMode);

		titleTv.setText(R.string.shared_string_terrain);
		String pluginUrl = getString(R.string.osmand_features_contour_lines_plugin);
		String emptyStateText = getString(R.string.terrain_empty_state_text) + "\n" + pluginUrl;
		setupClickableText(emptyStateDescriptionTv, emptyStateText, pluginUrl, pluginUrl, true);

		switchCompat.setChecked(terrainEnabled);
		switchCompat.setOnClickListener(this);
		UiUtilities.setupCompoundButton(switchCompat, nightMode, UiUtilities.CompoundButtonType.PROFILE_DEPENDENT);

		modifyButton.setOnClickListener(view -> callMapActivity(mapActivity -> {
			if (isColoringTypeAvailable()) {
				mapActivity.getDashboard().hideDashboard();
				FragmentManager manager = mapActivity.getSupportFragmentManager();
				ModifyGradientFragment.showInstance(manager, srtmPlugin.getTerrainMode().getType());
			} else {
				ChoosePlanFragment.showInstance(mapActivity, OsmAndFeature.ADVANCED_WIDGETS);
			}
		}));

		setupColorSchemeCard(root);
		setupCacheSizeCard();
		updateUiMode();
		return root;
	}

	private void updateChart() {
		int labelsColor = ColorUtilities.getPrimaryTextColor(app, nightMode);
		int xAxisGridColor = AndroidUtils.getColorFromAttr(app, R.attr.chart_x_grid_line_axis_color);

		ChartUtils.setupGradientChart(app, gradientChart, 9, 24, false, xAxisGridColor, labelsColor);
		TerrainMode mode = srtmPlugin.getTerrainMode();
		ColorPalette colorPalette = app.getColorPaletteHelper().getGradientColorPaletteSync(mode.getMainFile());
		if (colorPalette != null) {
			AndroidUiHelper.updateVisibility(gradientChart, true);
			IAxisValueFormatter formatter = GradientUiHelper.getGradientTypeFormatter(app, mode.getType(), null);
			LineData barData = ChartUtils.buildGradientChart(app, gradientChart, colorPalette, formatter, nightMode);

			gradientChart.setData(barData);
			gradientChart.notifyDataSetChanged();
			gradientChart.invalidate();
		} else {
			AndroidUiHelper.updateVisibility(gradientChart, false);
		}
	}

	public void showCacheScreen() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			Intent intent = new Intent(app, app.getAppCustomization().getDownloadIndexActivity());
			intent.putExtra(DownloadActivity.TAB_TO_OPEN, DownloadActivity.LOCAL_TAB);
			intent.putExtra(DownloadActivity.LOCAL_ITEM_TYPE, LocalItemType.CACHE.name());
			activity.startActivity(intent);
		}
	}

	@NonNull
	private String formatChartValue(float value) {
		DecimalFormat decimalFormat = new DecimalFormat("#");
		return decimalFormat.format(value);
	}

	private void updateColorSchemeCard(TerrainMode mode) {
		int transparencyValue = (int) (srtmPlugin.getTerrainTransparency() / 2.55);
		String transparency = transparencyValue + "%";
		visibilityTv.setText(transparency);

		NumberFormat numberFormat = OsmAndFormatter.getNumberFormat(app);
		String minZoom = numberFormat.format(srtmPlugin.getTerrainMinZoom());
		String maxZoom = numberFormat.format(srtmPlugin.getTerrainMaxZoom());
		String zoomLevels = getString(R.string.ltr_or_rtl_combine_via_dash, minZoom, maxZoom);
		zoomLevelsTv.setText(zoomLevels);
		coloSchemeTv.setText(mode.getType().getName(app));
	}

	private void setupColorSchemeCard(@NonNull View root) {
		View colorSchemeBtn = root.findViewById(R.id.color_scheme_button);
		colorSchemeBtn.setOnClickListener(view -> {
			List<PopUpMenuItem> menuItems = new ArrayList<>();
			for (TerrainMode mode : TerrainMode.values(app)) {
				if (mode.isDefaultMode()) {
					menuItems.add(new PopUpMenuItem.Builder(app)
							.setTitle(mode.getType().getName(app))
							.setOnClickListener(v -> setupTerrainMode(mode))
							.create());
				}
			}
			PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
			displayData.anchorView = view;
			displayData.menuItems = menuItems;
			displayData.nightMode = nightMode;
			displayData.layoutId = R.layout.popup_menu_item_checkbox;
			displayData.widthMode = PopUpMenuWidthMode.STANDARD;
			PopUpMenu.show(displayData);
		});

		View visibilityBtn = root.findViewById(R.id.visibility_button);
		View zoomLevelsBtn = root.findViewById(R.id.zoom_levels_button);

		visibilityBtn.setOnClickListener(v -> callMapActivity(mapActivity -> {
			mapActivity.getDashboard().hideDashboard();
			TerrainVisibilityFragment.showInstance(mapActivity.getSupportFragmentManager());
		}));

		zoomLevelsBtn.setOnClickListener(v -> callMapActivity(mapActivity -> {
			mapActivity.getDashboard().hideDashboard();
			TerrainZoomLevelsController.showDialog(mapActivity, srtmPlugin);
		}));
	}

	private void setupCacheSizeCard() {
		cacheSizeValueTv.setText(getFormattedCacheSize());
	}

	@NonNull
	private String getFormattedCacheSize() {
		long totalSize = 0;
		File sqliteCacheDir = new File(app.getCacheDir(), GEOTIFF_SQLITE_CACHE_DIR);

		File[] files = sqliteCacheDir.listFiles();
		if (!Algorithms.isEmpty(files)) {
			for (File file : files) {
				if (file.isFile()) {
					totalSize += file.length();
				}
			}
		}
		return AndroidUtils.formatSize(app, totalSize);
	}

	private void showHideTopShadow(@NonNull View view) {
		boolean portrait = AndroidUiHelper.isOrientationPortrait(requireActivity());
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.shadow_on_map), portrait);
	}

	@Override
	public void onClick(View view) {
		int id = view.getId();
		if (id == R.id.switch_compat) {
			onSwitchClick();
		}
	}

	private void updateUiMode() {
		TerrainMode mode = srtmPlugin.getTerrainMode();
		if (terrainEnabled) {
			iconIv.setImageDrawable(uiUtilities.getPaintedIcon(R.drawable.ic_action_hillshade_dark, profileColor));
			stateTv.setText(R.string.shared_string_enabled);

			if (mode.getType() == HILLSHADE) {
				descriptionTv.setText(R.string.hillshade_description);
				downloadDescriptionTv.setText(R.string.hillshade_download_description);
			} else if (mode.getType() == SLOPE) {
				descriptionTv.setText(R.string.slope_legend_description);
				String wikiString = getString(R.string.shared_string_wikipedia);
				String readMoreText = String.format(
						getString(R.string.slope_legend_description),
						wikiString
				);
				String wikiSlopeUrl = getString(R.string.url_wikipedia_slope);
				setupClickableText(descriptionTv, readMoreText, wikiString, wikiSlopeUrl, false);
				downloadDescriptionTv.setText(R.string.slope_download_description);
			} else if (mode.getType() == HEIGHT) {
				descriptionTv.setText(R.string.height_legend_description);
			}
			callMapActivity(downloadMapsCard::updateDownloadSection);
		} else {
			iconIv.setImageDrawable(getIcon(R.drawable.ic_action_hillshade_dark, ColorUtilities.getSecondaryIconColorId(nightMode)));
			stateTv.setText(R.string.shared_string_disabled);
		}
		AndroidUiHelper.updateVisibility(proIv, !isColoringTypeAvailable());
		proIv.setImageResource(PurchasingUtils.getProFeatureIconId(nightMode));

		adjustGlobalVisibility();
		updateColorSchemeCard(mode);
		updateChart();
	}

	private void adjustGlobalVisibility() {
		emptyState.setVisibility(terrainEnabled ? View.GONE : View.VISIBLE);
		emptyStateDivider.setVisibility(terrainEnabled ? View.GONE : View.VISIBLE);
		titleBottomDivider.setVisibility(terrainEnabled ? View.GONE : View.VISIBLE);
		contentContainer.setVisibility(terrainEnabled ? View.VISIBLE : View.GONE);
	}

	private void setupClickableText(TextView textView,
	                                String text,
	                                String clickableText,
	                                String url,
	                                boolean medium) {
		SpannableString spannableString = new SpannableString(text);
		ClickableSpan clickableSpan = new CustomClickableSpan() {
			@Override
			public void onClick(@NonNull View view) {
				FragmentActivity activity = getActivity();
				if (activity != null) {
					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setData(Uri.parse(url));
					AndroidUtils.startActivityIfSafe(activity, intent);
				}
			}
		};
		try {
			int startIndex = text.indexOf(clickableText);
			if (medium) {
				spannableString.setSpan(new CustomTypefaceSpan(FontCache.getMediumFont()), startIndex, startIndex + clickableText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			spannableString.setSpan(clickableSpan, startIndex, startIndex + clickableText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			textView.setText(spannableString);
			textView.setMovementMethod(LinkMovementMethod.getInstance());
			textView.setHighlightColor(ColorUtilities.getActiveColor(app, nightMode));
		} catch (RuntimeException e) {
			LOG.error("Error trying to find index of " + clickableText + " " + e);
		}
	}

	private void onSwitchClick() {
		terrainEnabled = !terrainEnabled;
		switchCompat.setChecked(terrainEnabled);
		srtmPlugin.setTerrainLayerEnabled(terrainEnabled);
		updateUiMode();
		updateLayers();
	}


	private void setupTerrainMode(TerrainMode mode) {
		TerrainMode currentMode = srtmPlugin.getTerrainMode();
		if (currentMode != mode) {
			srtmPlugin.setTerrainMode(mode);
			updateUiMode();
			updateLayers();
		}
	}

	private void updateLayers() {
		callMapActivity(srtmPlugin::updateLayers);
	}

	@Override
	public void onUpdatedIndexesList() {
		callMapActivity(downloadMapsCard::updateDownloadSection);
	}

	@Override
	public void downloadInProgress() {
		downloadMapsCard.downloadInProgress();
	}

	@Override
	public void downloadHasFinished() {
		callMapActivity(mapActivity -> {
			downloadMapsCard.updateDownloadSection(mapActivity);
			SRTMPlugin plugin = PluginsHelper.getActivePlugin(SRTMPlugin.class);
			if (plugin != null && plugin.isTerrainLayerEnabled()) {
				plugin.registerLayers(mapActivity, mapActivity);
			}
		});
	}

	private boolean isColoringTypeAvailable() {
		return InAppPurchaseUtils.isColoringTypeAvailable(app);
	}

	@Nullable
	public Set<InsetSide> getRootInsetSides() {
		return null;
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			fragmentManager.beginTransaction()
					.replace(R.id.content, new TerrainFragment(), TAG)
					.commitAllowingStateLoss();
		}
	}
}