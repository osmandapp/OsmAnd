package net.osmand.plus.plugins.srtm;

import static net.osmand.plus.download.DownloadActivityType.HILLSHADE_FILE;
import static net.osmand.plus.download.DownloadActivityType.SLOPE_FILE;
import static net.osmand.plus.plugins.srtm.TerrainMode.HILLSHADE;
import static net.osmand.plus.plugins.srtm.TerrainMode.SLOPE;
import static net.osmand.plus.utils.UiUtilities.CustomRadioButtonType.END;
import static net.osmand.plus.utils.UiUtilities.CustomRadioButtonType.START;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.github.ksoichiro.android.observablescrollview.ObservableListView;
import com.google.android.material.slider.RangeSlider;
import com.google.android.material.slider.Slider;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.download.DownloadValidationManager;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.ContextMenuListAdapter;
import net.osmand.plus.widgets.ctxmenu.ViewCreator;
import net.osmand.plus.widgets.ctxmenu.callback.ItemClickListener;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;
import net.osmand.plus.widgets.style.CustomTypefaceSpan;

import org.apache.commons.logging.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;


public class TerrainFragment extends BaseOsmAndFragment implements View.OnClickListener, DownloadEvents {

	public static final String TAG = TerrainFragment.class.getSimpleName();
	private static final Log LOG = PlatformUtil.getLog(TerrainFragment.class.getSimpleName());

	private OsmandApplication app;
	private UiUtilities uiUtilities;
	private OsmandSettings settings;
	private SRTMPlugin srtmPlugin;
	private boolean nightMode;
	private boolean terrainEnabled;

	private int colorProfile;

	private TextView downloadDescriptionTv;
	private TextView transparencyValueTv;
	private TextView descriptionTv;
	private LinearLayout customRadioButton;
	private TextView minZoomTv;
	private TextView maxZoomTv;
	private TextView stateTv;
	private SwitchCompat switchCompat;
	private ImageView iconIv;
	private LinearLayout emptyState;
	private LinearLayout legendContainer;
	private LinearLayout contentContainer;
	private LinearLayout downloadContainer;
	private View legendBottomDivider;
	private View titleBottomDivider;
	private View legendTopDivider;
	private View downloadTopDivider;
	private View downloadBottomDivider;
	private Slider transparencySlider;
	private RangeSlider zoomSlider;
	private ObservableListView observableListView;

	private ContextMenuListAdapter listAdapter;

	private final Slider.OnChangeListener transparencySliderChangeListener = new Slider.OnChangeListener() {
		@Override
		public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
			if (fromUser) {
				String transparencyStr = (int) value + "%";
				transparencyValueTv.setText(transparencyStr);
				srtmPlugin.setTerrainTransparency((int) Math.ceil(value * 2.55), srtmPlugin.getTerrainMode());
				refreshMap();
			}
		}
	};

	private final RangeSlider.OnChangeListener zoomSliderChangeListener = new RangeSlider.OnChangeListener() {
		@Override
		public void onValueChange(@NonNull RangeSlider slider, float value, boolean fromUser) {
			List<Float> values = slider.getValues();
			if (values.size() > 0) {
				minZoomTv.setText(String.valueOf(values.get(0).intValue()));
				maxZoomTv.setText(String.valueOf(values.get(1).intValue()));
				srtmPlugin.setTerrainZoomValues(values.get(0).intValue(), values.get(1).intValue(), srtmPlugin.getTerrainMode());
				refreshMap();
			}
		}
	};

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
		app = requireMyApplication();
		settings = app.getSettings();
		uiUtilities = app.getUIUtilities();
		nightMode = app.getDaynightHelper().isNightModeForMapControls();
		srtmPlugin = PluginsHelper.getPlugin(SRTMPlugin.class);
		colorProfile = settings.getApplicationMode().getProfileColor(nightMode);
		terrainEnabled = srtmPlugin.isTerrainLayerEnabled();
		super.onCreate(savedInstanceState);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View root = UiUtilities.getInflater(inflater.getContext(), nightMode).inflate(R.layout.fragment_terrain, container, false);

		showHideTopShadow(root);

		TextView emptyStateDescriptionTv = root.findViewById(R.id.empty_state_description);
		TextView slopeReadMoreTv = root.findViewById(R.id.slope_read_more_tv);
		TextView titleTv = root.findViewById(R.id.title_tv);
		downloadDescriptionTv = root.findViewById(R.id.download_description_tv);
		transparencyValueTv = root.findViewById(R.id.transparency_value_tv);
		legendBottomDivider = root.findViewById(R.id.legend_bottom_divider);
		transparencySlider = root.findViewById(R.id.transparency_slider);
		titleBottomDivider = root.findViewById(R.id.titleBottomDivider);
		legendTopDivider = root.findViewById(R.id.legend_top_divider);
		contentContainer = root.findViewById(R.id.content_container);
		legendContainer = root.findViewById(R.id.legend_container);
		switchCompat = root.findViewById(R.id.switch_compat);
		descriptionTv = root.findViewById(R.id.description);
		emptyState = root.findViewById(R.id.empty_state);
		stateTv = root.findViewById(R.id.state_tv);
		iconIv = root.findViewById(R.id.icon_iv);
		zoomSlider = root.findViewById(R.id.zoom_slider);
		minZoomTv = root.findViewById(R.id.zoom_value_min);
		maxZoomTv = root.findViewById(R.id.zoom_value_max);
		customRadioButton = root.findViewById(R.id.custom_radio_buttons);
		TextView hillshadeBtn = root.findViewById(R.id.left_button);
		TextView slopeBtn = root.findViewById(R.id.right_button);
		downloadContainer = root.findViewById(R.id.download_container);
		downloadTopDivider = root.findViewById(R.id.download_container_top_divider);
		downloadBottomDivider = root.findViewById(R.id.download_container_bottom_divider);
		observableListView = root.findViewById(R.id.list_view);

		titleTv.setText(R.string.shared_string_terrain);
		String wikiString = getString(R.string.shared_string_wikipedia);
		String readMoreText = String.format(
				getString(R.string.slope_read_more),
				wikiString
		);
		String wikiSlopeUrl = getString(R.string.url_wikipedia_slope);
		String pluginUrl = getString(R.string.osmand_features_contour_lines_plugin);
		String emptyStateText = getString(R.string.terrain_empty_state_text) + "\n" + pluginUrl;
		setupClickableText(slopeReadMoreTv, readMoreText, wikiString, wikiSlopeUrl, false);
		setupClickableText(emptyStateDescriptionTv, emptyStateText, pluginUrl, pluginUrl, true);

		switchCompat.setChecked(terrainEnabled);
		hillshadeBtn.setOnClickListener(this);
		hillshadeBtn.setText(R.string.shared_string_hillshade);
		switchCompat.setOnClickListener(this);
		slopeBtn.setOnClickListener(this);
		slopeBtn.setText(R.string.shared_string_slope);

		UiUtilities.setupSlider(transparencySlider, nightMode, colorProfile);
		UiUtilities.setupSlider(zoomSlider, nightMode, colorProfile, true);

		transparencySlider.addOnChangeListener(transparencySliderChangeListener);
		zoomSlider.addOnChangeListener(zoomSliderChangeListener);
		transparencySlider.setValueTo(100);
		transparencySlider.setValueFrom(0);

		UiUtilities.setupCompoundButton(switchCompat, nightMode, UiUtilities.CompoundButtonType.PROFILE_DEPENDENT);

		updateUiMode();
		return root;
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
		} else if (id == R.id.left_button) {
			setupTerrainMode(HILLSHADE);
		} else if (id == R.id.right_button) {
			setupTerrainMode(SLOPE);
		}
	}

	private void updateUiMode() {
		TerrainMode mode = srtmPlugin.getTerrainMode();
		if (terrainEnabled) {
			int transparencyValue = (int) (srtmPlugin.getTerrainTransparency() / 2.55);
			String transparency = transparencyValue + "%";
			int minZoom = srtmPlugin.getTerrainMinZoom();
			int maxZoom = srtmPlugin.getTerrainMaxZoom();
			iconIv.setImageDrawable(uiUtilities.getPaintedIcon(R.drawable.ic_action_hillshade_dark, colorProfile));
			stateTv.setText(R.string.shared_string_enabled);
			transparencySlider.setValue(transparencyValue);
			transparencyValueTv.setText(transparency);

			zoomSlider.setValueFrom(SRTMPlugin.TERRAIN_MIN_SUPPORTED_ZOOM);
			zoomSlider.setValueTo(SRTMPlugin.TERRAIN_MAX_SUPPORTED_ZOOM);
			zoomSlider.setValues((float) minZoom, (float) maxZoom);

			minZoomTv.setText(String.valueOf(minZoom));
			maxZoomTv.setText(String.valueOf(maxZoom));
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
			updateDownloadSection();
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
		int visibility = mode == SLOPE ? View.VISIBLE : View.GONE;
		legendContainer.setVisibility(visibility);
		legendBottomDivider.setVisibility(visibility);
		legendTopDivider.setVisibility(visibility);
	}

	private void adjustModeButtons(TerrainMode mode) {
		if (mode == SLOPE) {
			UiUtilities.updateCustomRadioButtons(app, customRadioButton, nightMode, END);
		} else {
			UiUtilities.updateCustomRadioButtons(app, customRadioButton, nightMode, START);
		}
	}

	private void setupClickableText(TextView textView,
									String text,
									String clickableText,
									String url,
									boolean medium) {
		SpannableString spannableString = new SpannableString(text);
		ClickableSpan clickableSpan = new ClickableSpan() {
			@Override
			public void onClick(@NonNull View view) {
				FragmentActivity activity = getActivity();
				if (activity != null) {
					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setData(Uri.parse(url));
					AndroidUtils.startActivityIfSafe(activity, intent);
				}
			}

			@Override
			public void updateDrawState(@NonNull TextPaint ds) {
				super.updateDrawState(ds);
				ds.setUnderlineText(false);
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

	private void refreshMap() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.refreshMap();
		}
	}

	private void updateLayers() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			srtmPlugin.updateLayers(mapActivity, mapActivity);
		}
	}

	private void updateDownloadSection() {
		ContextMenuAdapter adapter = new ContextMenuAdapter(app);

		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}
		WeakReference<MapActivity> mapActivityRef = new WeakReference<>(mapActivity);

		DownloadIndexesThread downloadThread = app.getDownloadThread();
		if (!downloadThread.getIndexes().isDownloadedFromInternet) {
			if (settings.isInternetConnectionAvailable()) {
				downloadThread.runReloadIndexFiles();
			}
		}

		if (downloadThread.shouldDownloadIndexes()) {
			adapter.addItem(new ContextMenuItem(null)
					.setLayout(R.layout.list_item_icon_and_download)
					.setTitleId(R.string.downloading_list_indexes, mapActivity)
					.setLoading(true));
		} else {
			try {
				TerrainMode mode = srtmPlugin.getTerrainMode();
				IndexItem currentDownloadingItem = downloadThread.getCurrentDownloadingItem();
				int currentDownloadingProgress = (int) downloadThread.getCurrentDownloadProgress();
				List<IndexItem> terrainItems = DownloadResources.findIndexItemsAt(
						app, mapActivity.getMapLocation(),
						mode == HILLSHADE ? HILLSHADE_FILE : SLOPE_FILE, false, -1, true);
				if (terrainItems.size() > 0) {
					downloadContainer.setVisibility(View.VISIBLE);
					downloadTopDivider.setVisibility(View.VISIBLE);
					downloadBottomDivider.setVisibility(View.VISIBLE);
					for (IndexItem indexItem : terrainItems) {
						ContextMenuItem _item = new ContextMenuItem(null)
								.setLayout(R.layout.list_item_icon_and_download)
								.setTitle(indexItem.getVisibleName(app, app.getRegions(), false))
								.setDescription(mode == HILLSHADE
										? HILLSHADE_FILE.getString(app) + " • " + indexItem.getSizeDescription(app)
										: SLOPE_FILE.getString(app) + " • " + indexItem.getSizeDescription(app))
								.setIcon(mode == HILLSHADE ? HILLSHADE_FILE.getIconResource() : SLOPE_FILE.getIconResource())
								.setListener((uiAdapter, view, item, isChecked) -> {
									MapActivity mapActivity1 = mapActivityRef.get();
									if (mapActivity1 != null && !mapActivity1.isFinishing()) {
										if (downloadThread.isDownloading(indexItem)) {
											downloadThread.cancelDownload(indexItem);
												item.setProgress(ContextMenuItem.INVALID_ID);
												item.setLoading(false);
												item.setSecondaryIcon(R.drawable.ic_action_import);
												uiAdapter.onDataSetChanged();
										} else {
											new DownloadValidationManager(app).startDownload(mapActivity1, indexItem);
												item.setProgress(ContextMenuItem.INVALID_ID);
												item.setLoading(true);
												item.setSecondaryIcon(R.drawable.ic_action_remove_dark);
												uiAdapter.onDataSetChanged();
										}
									}
									return false;
								})
								.setProgressListener((progressObject, progress, adptr, itemId, position) -> {
									if (progressObject instanceof IndexItem) {
										IndexItem progressItem = (IndexItem) progressObject;
										if (indexItem.compareTo(progressItem) == 0) {
											ContextMenuItem item = adptr.getItem(position);
											if (item != null) {
												item.setProgress(progress);
												item.setLoading(true);
												item.setSecondaryIcon(R.drawable.ic_action_remove_dark);
												adptr.notifyDataSetChanged();
											}
											return true;
										}
									}
									return false;
								});

						if (indexItem == currentDownloadingItem) {
							_item.setLoading(true)
									.setProgress(currentDownloadingProgress)
									.setSecondaryIcon(R.drawable.ic_action_remove_dark);
						} else {
							_item.setSecondaryIcon(R.drawable.ic_action_import);
						}
						adapter.addItem(_item);
					}
				} else {
					downloadContainer.setVisibility(View.GONE);
					downloadTopDivider.setVisibility(View.GONE);
					downloadBottomDivider.setVisibility(View.GONE);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		ApplicationMode appMode = settings.getApplicationMode();
		ViewCreator viewCreator = new ViewCreator(mapActivity, nightMode);
		viewCreator.setDefaultLayoutId(R.layout.list_item_icon_and_menu);
		viewCreator.setCustomControlsColor(appMode.getProfileColor(nightMode));

		listAdapter = adapter.toListAdapter(mapActivity, viewCreator);
		observableListView.setAdapter(listAdapter);
		observableListView.setOnItemClickListener((parent, view, position, id) -> {
			ContextMenuItem item = adapter.getItem(position);
			ItemClickListener click = item.getItemClickListener();
			if (click != null) {
				click.onContextMenuClick(listAdapter, view, item, false);
			}
		});
	}

	@Override
	public void onUpdatedIndexesList() {
		updateDownloadSection();
	}

	@Override
	public void downloadInProgress() {
		DownloadIndexesThread downloadThread = app.getDownloadThread();
		IndexItem downloadIndexItem = downloadThread.getCurrentDownloadingItem();
		if (downloadIndexItem != null && listAdapter != null) {
			int downloadProgress = (int) downloadThread.getCurrentDownloadProgress();
			ArrayAdapter<ContextMenuItem> adapter = listAdapter;
			for (int i = 0; i < adapter.getCount(); i++) {
				ContextMenuItem item = adapter.getItem(i);
				if (item != null && item.getProgressListener() != null) {
					item.getProgressListener().onProgressChanged(
							downloadIndexItem, downloadProgress, adapter, (int) adapter.getItemId(i), i);
				}
			}
		}
	}

	@Override
	public void downloadHasFinished() {
		updateDownloadSection();
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