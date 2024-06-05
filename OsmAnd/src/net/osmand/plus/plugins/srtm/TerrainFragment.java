package net.osmand.plus.plugins.srtm;

import static net.osmand.IndexConstants.GEOTIFF_SQLITE_CACHE_DIR;

import android.app.Activity;
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

import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;
import net.osmand.plus.widgets.popup.PopUpMenuWidthMode;
import net.osmand.plus.widgets.style.CustomClickableSpan;
import net.osmand.plus.widgets.style.CustomTypefaceSpan;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class TerrainFragment extends BaseOsmAndFragment implements View.OnClickListener, DownloadEvents {

	public static final String TAG = TerrainFragment.class.getSimpleName();
	private static final Log LOG = PlatformUtil.getLog(TerrainFragment.class.getSimpleName());

	private SRTMPlugin srtmPlugin;
	private boolean terrainEnabled;

	private int profileColor;

	private TextView visibilityTv;
	private TextView zoomLevelsTv;
	private TextView coloSchemeTv;
	private TextView cacheSizeValueTv;

	private View legend;

	private TextView downloadDescriptionTv;
	private TextView descriptionTv;
	private TextView stateTv;
	private SwitchCompat switchCompat;
	private ImageView iconIv;
	private LinearLayout emptyState;
	private View emptyStateDivider;
	private LinearLayout contentContainer;
	private View titleBottomDivider;

	private DownloadMapsCard downloadMapsCard;

	@Nullable
	private MapActivity getMapActivity() {
		Activity activity = getActivity();
		if (activity instanceof MapActivity && !activity.isFinishing()) {
			return (MapActivity) activity;
		}
		return null;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		srtmPlugin = PluginsHelper.getPlugin(SRTMPlugin.class);
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
		View root = themedInflater.inflate(R.layout.fragment_terrain, container, false);
		profileColor = settings.getApplicationMode().getProfileColor(nightMode);

		showHideTopShadow(root);

		visibilityTv = root.findViewById(R.id.visibility_value);
		zoomLevelsTv = root.findViewById(R.id.zoom_value);
		coloSchemeTv = root.findViewById(R.id.color_scheme_name);
		cacheSizeValueTv = root.findViewById(R.id.cache_size_value);

		legend = root.findViewById(R.id.legend);

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
		downloadMapsCard = new DownloadMapsCard(app, srtmPlugin, root.findViewById(R.id.download_maps_card), nightMode);

		titleTv.setText(R.string.shared_string_terrain);
		String pluginUrl = getString(R.string.osmand_features_contour_lines_plugin);
		String emptyStateText = getString(R.string.terrain_empty_state_text) + "\n" + pluginUrl;
		setupClickableText(emptyStateDescriptionTv, emptyStateText, pluginUrl, pluginUrl, true);

		switchCompat.setChecked(terrainEnabled);
		switchCompat.setOnClickListener(this);
		UiUtilities.setupCompoundButton(switchCompat, nightMode, UiUtilities.CompoundButtonType.PROFILE_DEPENDENT);

		setupColorSchemeCard(root);
		setupCacheSizeCard();
		updateUiMode();
		return root;
	}

	private void updateColorSchemeCard(TerrainMode mode) {
		int transparencyValue = (int) (srtmPlugin.getTerrainTransparency() / 2.55);
		String transparency = transparencyValue + "%";
		visibilityTv.setText(transparency);

		int minZoom = srtmPlugin.getTerrainMinZoom();
		int maxZoom = srtmPlugin.getTerrainMaxZoom();
		String zoomLevels = minZoom + " - " + maxZoom;
		zoomLevelsTv.setText(zoomLevels);
		coloSchemeTv.setText(mode.getDescription());
		AndroidUiHelper.updateVisibility(legend, mode.getType() == TerrainMode.TerrainType.SLOPE);
	}

	private void setupColorSchemeCard(@NonNull View root) {
		View colorSchemeBtn = root.findViewById(R.id.color_scheme_button);
		colorSchemeBtn.setOnClickListener(view -> {
			List<PopUpMenuItem> menuItems = new ArrayList<>();
			for (TerrainMode mode : TerrainMode.values(app)) {
				menuItems.add(new PopUpMenuItem.Builder(app)
						.setTitle(mode.getDescription())
						.setOnClickListener(v -> setupTerrainMode(mode))
						.create());
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

		visibilityBtn.setOnClickListener(view -> {
			MapActivity mapActivity = getMapActivity();
			if (mapActivity != null) {
				mapActivity.getDashboard().hideDashboard();
				TerrainVisibilityFragment.showInstance(mapActivity.getSupportFragmentManager());
			}
		});
		zoomLevelsBtn.setOnClickListener(view -> {
			MapActivity mapActivity = getMapActivity();
			if (mapActivity != null) {
				mapActivity.getDashboard().hideDashboard();
				TerrainZoomLevelsFragment.showInstance(mapActivity.getSupportFragmentManager());
			}
		});
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

			if (mode.getType() == TerrainMode.TerrainType.HILLSHADE) {
				descriptionTv.setText(R.string.hillshade_description);
				downloadDescriptionTv.setText(R.string.hillshade_download_description);
			} else if (mode.getType() == TerrainMode.TerrainType.SLOPE) {
				descriptionTv.setText(R.string.slope_legend_description);
				String wikiString = getString(R.string.shared_string_wikipedia);
				String readMoreText = String.format(
						getString(R.string.slope_legend_description),
						wikiString
				);
				String wikiSlopeUrl = getString(R.string.url_wikipedia_slope);
				setupClickableText(descriptionTv, readMoreText, wikiString, wikiSlopeUrl, false);
				downloadDescriptionTv.setText(R.string.slope_download_description);
			} else if (mode.getType() == TerrainMode.TerrainType.HEIGHT) {
				descriptionTv.setText(R.string.height_legend_description);
			}
			downloadMapsCard.updateDownloadSection(getMapActivity());
		} else {
			iconIv.setImageDrawable(uiUtilities.getIcon(
					R.drawable.ic_action_hillshade_dark,
					nightMode
							? R.color.icon_color_secondary_dark
							: R.color.icon_color_secondary_light));
			stateTv.setText(R.string.shared_string_disabled);
		}
		adjustGlobalVisibility();
		updateColorSchemeCard(mode);
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
				spannableString.setSpan(new CustomTypefaceSpan(FontCache.getRobotoMedium(app)), startIndex, startIndex + clickableText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
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
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			srtmPlugin.updateLayers(mapActivity, mapActivity);
		}
	}

	@Override
	public void onUpdatedIndexesList() {
		downloadMapsCard.updateDownloadSection(getMapActivity());
	}

	@Override
	public void downloadInProgress() {
		downloadMapsCard.downloadInProgress();
	}

	@Override
	public void downloadHasFinished() {
		downloadMapsCard.updateDownloadSection(getMapActivity());
		MapActivity mapActivity = getMapActivity();
		SRTMPlugin plugin = PluginsHelper.getActivePlugin(SRTMPlugin.class);
		if (mapActivity != null && plugin != null && plugin.isTerrainLayerEnabled()) {
			plugin.registerLayers(mapActivity, mapActivity);
		}
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			fragmentManager.beginTransaction()
					.replace(R.id.content, new TerrainFragment(), TAG)
					.commitAllowingStateLoss();
		}
	}
}