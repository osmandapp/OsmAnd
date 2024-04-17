package net.osmand.plus.plugins.srtm;

import static net.osmand.plus.download.DownloadActivityType.GEOTIFF_FILE;
import static net.osmand.plus.download.DownloadActivityType.HILLSHADE_FILE;
import static net.osmand.plus.download.DownloadActivityType.SLOPE_FILE;
import static net.osmand.plus.plugins.srtm.SRTMPlugin.getFormattedScaleValue;
import static net.osmand.plus.plugins.srtm.TerrainMode.HILLSHADE;

import android.app.Activity;
import android.os.Bundle;
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
import androidx.fragment.app.FragmentManager;

import com.github.ksoichiro.android.observablescrollview.ObservableListView;

import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.download.DownloadValidationManager;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.development.OsmandDevelopmentPlugin;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.ContextMenuListAdapter;
import net.osmand.plus.widgets.ctxmenu.ViewCreator;
import net.osmand.plus.widgets.ctxmenu.callback.ItemClickListener;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;

import org.apache.commons.logging.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

public class Relief3DFragment extends BaseOsmAndFragment implements View.OnClickListener, DownloadIndexesThread.DownloadEvents {

	public static final String TAG = Relief3DFragment.class.getSimpleName();
	private static final Log LOG = PlatformUtil.getLog(Relief3DFragment.class.getSimpleName());

	private SRTMPlugin srtmPlugin;
	private boolean relief3DEnabled;

	private int profileColor;

	private TextView exaggerationValueTv;

	private TextView downloadDescriptionTv;
	private TextView stateTv;
	private SwitchCompat switchCompat;
	private ImageView iconIv;
	private LinearLayout contentContainer;
	private LinearLayout downloadContainer;
	private View titleBottomDivider;
	private View downloadTopDivider;
	private View downloadBottomDivider;
	private ObservableListView observableListView;

	private ContextMenuListAdapter listAdapter;

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
		relief3DEnabled = settings.ENABLE_3D_MAPS.get();
	}

	@Override
	protected boolean isUsedOnMap() {
		return true;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View root = themedInflater.inflate(R.layout.fragment_relief_3d, container, false);
		profileColor = settings.getApplicationMode().getProfileColor(nightMode);
		showHideTopShadow(root);

		exaggerationValueTv = root.findViewById(R.id.exaggeration_value);
		downloadDescriptionTv = root.findViewById(R.id.download_description_tv);
		titleBottomDivider = root.findViewById(R.id.titleBottomDivider);
		contentContainer = root.findViewById(R.id.content_container);
		switchCompat = root.findViewById(R.id.switch_compat);
		stateTv = root.findViewById(R.id.state_tv);
		iconIv = root.findViewById(R.id.icon_iv);
		downloadContainer = root.findViewById(R.id.download_container);
		downloadTopDivider = root.findViewById(R.id.download_container_top_divider);
		downloadBottomDivider = root.findViewById(R.id.download_container_bottom_divider);
		observableListView = root.findViewById(R.id.list_view);

		TextView titleTv = root.findViewById(R.id.title_tv);
		titleTv.setText(R.string.relief_3d);

		switchCompat.setChecked(relief3DEnabled);
		switchCompat.setOnClickListener(this);
		UiUtilities.setupCompoundButton(switchCompat, nightMode, UiUtilities.CompoundButtonType.PROFILE_DEPENDENT);

		setupContentCard(root);
		updateUiMode();
		return root;
	}

	public float getElevationScaleFactor() {
		return srtmPlugin.getVerticalExaggerationScale();
	}

	private void setupContentCard(@NonNull View root) {
		downloadDescriptionTv.setText(R.string.relief_3d_download_description);
		View verticalExaggerationBtn = root.findViewById(R.id.vertical_exaggeration_button);
		verticalExaggerationBtn.setOnClickListener(view -> {
			MapActivity mapActivity = getMapActivity();
			if (mapActivity != null) {
				mapActivity.getDashboard().hideDashboard();
				VerticalExaggerationFragment.showInstance(mapActivity.getSupportFragmentManager());
			}
		});
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
		if (relief3DEnabled) {
			iconIv.setImageDrawable(uiUtilities.getPaintedIcon(R.drawable.ic_action_3d_relief, profileColor));
			stateTv.setText(R.string.shared_string_on);
			updateDownloadSection();
		} else {
			iconIv.setImageDrawable(uiUtilities.getIcon(
					R.drawable.ic_action_3d_relief,
					nightMode
							? R.color.icon_color_secondary_dark
							: R.color.icon_color_secondary_light));
			stateTv.setText(R.string.shared_string_off);
		}
		exaggerationValueTv.setText(getFormattedScaleValue(app, getElevationScaleFactor()));
		adjustGlobalVisibility();
	}

	private void adjustGlobalVisibility() {
		titleBottomDivider.setVisibility(relief3DEnabled ? View.GONE : View.VISIBLE);
		contentContainer.setVisibility(relief3DEnabled ? View.VISIBLE : View.GONE);
	}

	private void onSwitchClick() {
		relief3DEnabled = !relief3DEnabled;
		switchCompat.setChecked(relief3DEnabled);
		settings.ENABLE_3D_MAPS.set(relief3DEnabled);
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			app.runInUIThread(() -> app.getOsmandMap().getMapLayers().getMapInfoLayer().recreateAllControls(mapActivity));
		}

		updateUiMode();
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
				DownloadActivityType type = getDownloadActivityType();
				IndexItem currentDownloadingItem = downloadThread.getCurrentDownloadingItem();
				int currentDownloadingProgress = (int) downloadThread.getCurrentDownloadProgress();
				List<IndexItem> terrainItems = DownloadResources.findIndexItemsAt(app,
						mapActivity.getMapLocation(), type, false, -1, true);
				if (terrainItems.size() > 0) {
					downloadContainer.setVisibility(View.VISIBLE);
					downloadTopDivider.setVisibility(View.VISIBLE);
					downloadBottomDivider.setVisibility(View.VISIBLE);
					for (IndexItem indexItem : terrainItems) {
						ContextMenuItem _item = new ContextMenuItem(null)
								.setLayout(R.layout.list_item_icon_and_download)
								.setTitle(indexItem.getVisibleName(app, app.getRegions(), false))
								.setDescription(type.getString(app) + " • " + indexItem.getSizeDescription(app))
								.setIcon(type.getIconResource())
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
				LOG.error(e);
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

	@NonNull
	private DownloadActivityType getDownloadActivityType() {
		OsmandDevelopmentPlugin plugin = PluginsHelper.getPlugin(OsmandDevelopmentPlugin.class);
		if (plugin != null && plugin.generateTerrainFrom3DMaps()) {
			return GEOTIFF_FILE;
		} else {
			return srtmPlugin.getTerrainMode() == HILLSHADE ? HILLSHADE_FILE : SLOPE_FILE;
		}
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
					.replace(R.id.content, new Relief3DFragment(), TAG)
					.commitAllowingStateLoss();
		}
	}
}