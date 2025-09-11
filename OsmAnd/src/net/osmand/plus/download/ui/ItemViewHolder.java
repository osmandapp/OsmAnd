package net.osmand.plus.download.ui;

import static net.osmand.plus.download.DownloadActivityType.*;
import static net.osmand.plus.download.DownloadResources.WORLD_SEAMARKS_KEY;
import static net.osmand.plus.download.local.OperationType.DELETE_OPERATION;
import static net.osmand.plus.download.ui.ItemViewHolder.RightButtonAction.ASK_FOR_SRTM_PLUGIN_ENABLE;
import static net.osmand.plus.download.ui.ItemViewHolder.RightButtonAction.ASK_FOR_SRTM_PLUGIN_PURCHASE;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import net.osmand.map.OsmandRegions;
import net.osmand.map.WorldRegion;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.chooseplan.ChoosePlanFragment;
import net.osmand.plus.chooseplan.OsmAndFeature;
import net.osmand.plus.download.*;
import net.osmand.plus.download.local.LocalItem;
import net.osmand.plus.download.local.LocalItemType;
import net.osmand.plus.download.local.LocalItemUtils;
import net.osmand.plus.download.local.LocalOperationTask;
import net.osmand.plus.download.local.LocalOperationTask.OperationListener;
import net.osmand.plus.download.local.OperationType;
import net.osmand.plus.helpers.FileNameTranslationHelper;
import net.osmand.plus.inapp.InAppPurchaseUtils;
import net.osmand.plus.plugins.PluginsFragment;
import net.osmand.plus.plugins.accessibility.AccessibilityAssistant;
import net.osmand.plus.plugins.custom.CustomIndexItem;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.util.Algorithms;

import java.io.File;
import java.text.DateFormat;
import java.util.List;

public class ItemViewHolder {

	protected final OsmandApplication app;
	protected final View view;
	protected final TextView tvName;
	protected final TextView tvDesc;
	protected final ImageView ivLeft;
	protected final ImageView ivBtnRight;
	protected final Button btnRight;
	protected final ProgressBar pbProgress;

	private boolean srtmDisabled;
	private boolean srtmNeedsInstallation;
	private boolean nauticalPluginDisabled;
	private boolean depthContoursPurchased;
	private boolean weatherAvailable;

	protected final DownloadActivity context;

	private final int textColorPrimary;
	private final int textColorSecondary;

	boolean showTypeInDesc;
	boolean showTypeInName;
	boolean useShortName;
	boolean showParentRegionName;
	boolean showRemoteDate;
	boolean silentCancelDownload;
	boolean showProgressInDesc;

	private final DateFormat dateFormat;


	protected enum RightButtonAction {
		DOWNLOAD,
		ASK_FOR_SEAMARKS_PLUGIN,
		ASK_FOR_SRTM_PLUGIN_PURCHASE,
		ASK_FOR_SRTM_PLUGIN_ENABLE,
		ASK_FOR_FULL_VERSION_PURCHASE,
		ASK_FOR_DEPTH_CONTOURS_PURCHASE,
		ASK_FOR_WEATHER_PURCHASE
	}


	public ItemViewHolder(@NonNull View view, @NonNull DownloadActivity context) {
		this.context = context;
		this.app = context.getApp();
		this.view = view;
		dateFormat = android.text.format.DateFormat.getMediumDateFormat(context);
		pbProgress = view.findViewById(R.id.progressBar);
		btnRight = view.findViewById(R.id.rightButton);
		ivLeft = view.findViewById(R.id.icon);
		tvDesc = view.findViewById(R.id.description);
		ivBtnRight = view.findViewById(R.id.secondaryIcon);
		tvName = view.findViewById(R.id.title);

		ViewCompat.setAccessibilityDelegate(view, context.getAccessibilityAssistant());
		ViewCompat.setAccessibilityDelegate(btnRight, context.getAccessibilityAssistant());

		TypedValue typedValue = new TypedValue();
		Resources.Theme theme = context.getTheme();
		theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
		textColorPrimary = typedValue.data;
		theme.resolveAttribute(android.R.attr.textColorSecondary, typedValue, true);
		textColorSecondary = typedValue.data;
	}

	public void setShowRemoteDate(boolean showRemoteDate) {
		this.showRemoteDate = showRemoteDate;
	}


	public void setShowParentRegionName(boolean showParentRegionName) {
		this.showParentRegionName = showParentRegionName;
	}

	public void setShowProgressInDescr(boolean b) {
		showProgressInDesc = b;
	}

	public void setSilentCancelDownload(boolean silentCancelDownload) {
		this.silentCancelDownload = silentCancelDownload;
	}

	public void setShowTypeInDesc(boolean showTypeInDesc) {
		this.showTypeInDesc = showTypeInDesc;
	}

	public void setShowTypeInName(boolean showTypeInName) {
		this.showTypeInName = showTypeInName;
	}

	public void setUseShortName(boolean useShortName) {
		this.useShortName = useShortName;
	}

	private void initAppStatusVariables() {
		srtmDisabled = context.isSrtmDisabled();
		nauticalPluginDisabled = context.isNauticalPluginDisabled();
		srtmNeedsInstallation = context.isSrtmNeedsInstallation();
		depthContoursPurchased = InAppPurchaseUtils.isDepthContoursAvailable(app);
		weatherAvailable = InAppPurchaseUtils.isWeatherAvailable(app);
	}

	public void bindDownloadItem(@NonNull DownloadItem downloadItem) {
		bindDownloadItem(downloadItem, null);
	}

	public void bindDownloadItem(@NonNull DownloadItem downloadItem, @Nullable String cityName) {
		initAppStatusVariables();
		boolean isDownloading = downloadItem.isDownloading(context.getDownloadThread());
		float progress = -1;
		DownloadIndexesThread downloadThread = context.getDownloadThread();
		if (downloadThread.isCurrentDownloading(downloadItem)) {
			progress = downloadThread.getCurrentDownloadProgress();
		}
		boolean disabled = checkDisabledAndClickAction(downloadItem);
		/// name and left item
		String name;
		if (showTypeInName) {
			name = downloadItem.getType().getString(context);
		} else {
			name = downloadItem.getVisibleName(context, app.getRegions(), showParentRegionName, useShortName);
		}
		String text = (!Algorithms.isEmpty(cityName) && !cityName.equals(name) ? cityName + "\n" : "") + name;
		tvName.setText(text);
		ViewCompat.setAccessibilityDelegate(ivBtnRight, new AccessibilityAssistant(context) {

			@Override
			public void onInitializeAccessibilityNodeInfo(@NonNull View host,
			                                              @NonNull AccessibilityNodeInfoCompat info) {
				super.onInitializeAccessibilityNodeInfo(host, info);
				info.setContentDescription(context.getString(R.string.shared_string_download) + tvName.getText());
				info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
						AccessibilityNodeInfo.ACTION_CLICK, context.getString(R.string.shared_string_download)
				));
				info.setEnabled(host.isEnabled());
			}
		});

		if (!disabled) {
			tvName.setTextColor(textColorPrimary);
		} else {
			tvName.setTextColor(textColorSecondary);
		}
		int iconColor = textColorSecondary;
		if (downloadItem.isDownloaded() && !isDownloading) {
			iconColor = ColorUtilities.getColor(context, getIconColorId(downloadItem));
		}
		if (downloadItem.isDownloaded()) {
			ivLeft.setImageDrawable(getContentIcon(context,
					downloadItem.getType().getIconResource(), iconColor));
		} else if (disabled) {
			ivLeft.setImageDrawable(getContentIcon(context,
					downloadItem.getType().getIconResource(), textColorSecondary));
		} else {
			ivLeft.setImageDrawable(getThemedIcon(context,
					downloadItem.getType().getIconResource()));
		}
		tvDesc.setTextColor(textColorSecondary);

		if (!isDownloading) {
			pbProgress.setVisibility(View.GONE);
			tvDesc.setVisibility(View.VISIBLE);
			if (downloadItem instanceof CustomIndexItem && (((CustomIndexItem) downloadItem).getSubName(context) != null)) {
				tvDesc.setText(((CustomIndexItem) downloadItem).getSubName(context));
			} else if ((downloadItem.getType() == DEPTH_CONTOUR_FILE
					|| downloadItem.getType() == DEPTH_MAP_FILE) && !depthContoursPurchased) {
				tvDesc.setText(context.getString(R.string.depth_contour_descr));
			} else if ((downloadItem.getType() == SRTM_COUNTRY_FILE
					|| downloadItem.getType() == HILLSHADE_FILE
					|| downloadItem.getType() == SLOPE_FILE) && srtmDisabled) {
				if (showTypeInName) {
					tvDesc.setText("");
				} else {
					tvDesc.setText(downloadItem.getType().getString(context));
				}
			} else if (downloadItem instanceof MultipleDownloadItem) {
				setupCommonMultipleDescription((MultipleDownloadItem) downloadItem);
			} else {
				setupCommonDescription(downloadItem);
			}

		} else {
			pbProgress.setVisibility(View.VISIBLE);
			pbProgress.setIndeterminate(progress < 0);
			pbProgress.setProgress((int) progress);

			if (showProgressInDesc) {
				double mb = downloadItem.getArchiveSizeMB();
				String v;
				if (progress != -1) {
					v = context.getString(R.string.value_downloaded_of_max, mb * progress / 100, mb);
				} else {
					v = context.getString(R.string.file_size_in_mb, mb);
				}
				String fullDescription = v;
				if (showTypeInDesc && downloadItem.getType() == DownloadActivityType.ROADS_FILE) {
					fullDescription = context.getString(R.string.ltr_or_rtl_combine_via_bold_point,
							downloadItem.getType().getString(context), fullDescription);
				}
				tvDesc.setText(fullDescription);
				tvDesc.setVisibility(View.VISIBLE);
			} else {
				tvDesc.setVisibility(View.GONE);
			}
		}
	}

	public void bindDownloadItem(CityItem cityItem) {
		if (cityItem.getIndexItem() != null) {
			bindDownloadItem(cityItem.getIndexItem(), cityItem.getName());
		} else {
			tvName.setText(cityItem.getName());
			tvName.setTextColor(textColorPrimary);
			ivLeft.setImageDrawable(getThemedIcon(context, R.drawable.ic_map));
			tvDesc.setVisibility(View.GONE);
			pbProgress.setVisibility(View.GONE);
		}
	}

	private void setupCommonMultipleDescription(@NonNull MultipleDownloadItem item) {
		String regionsHeader = context.getString(R.string.regions);
		String allRegionsHeader = context.getString(R.string.shared_strings_all_regions);
		String allRegionsCount = String.valueOf(item.getAllItems().size());
		String leftToDownloadCount = String.valueOf(item.getItemsToDownload().size());

		String header = allRegionsHeader;
		String count = allRegionsCount;
		if (item.hasActualDataToDownload()) {
			if (!item.isDownloaded()) {
				header = allRegionsHeader;
				count = leftToDownloadCount;
			} else {
				header = regionsHeader;
				count = String.format(
						context.getString(R.string.ltr_or_rtl_combine_via_slash),
						leftToDownloadCount,
						allRegionsCount);
			}
		}

		String fullDescription = context.getString(R.string.ltr_or_rtl_combine_via_colon, header, count);
		String additionalDescription = item.getAdditionalDescription(context);
		if (additionalDescription != null) {
			fullDescription += " " + additionalDescription;
		}
		if (item.hasActualDataToDownload()) {
			fullDescription = context.getString(
					R.string.ltr_or_rtl_combine_via_bold_point, fullDescription,
					item.getSizeDescription(context));
		}
		tvDesc.setText(fullDescription);
	}

	private void setupCommonDescription(@NonNull DownloadItem item) {
		String size = item.getSizeDescription(context);
		String date = item.getDate(dateFormat, showRemoteDate);
		String additional = item.getAdditionalDescription(context);
		String pattern = context.getString(R.string.ltr_or_rtl_combine_via_bold_point);

		String fullDescription;
		if (showTypeInDesc) {
			String type = item.getType().getString(context);
			if (additional != null) {
				type += " " + additional;
			}
			fullDescription = String.format(pattern, type, String.format(pattern, size, date));
		} else {
			if (additional != null) {
				size += " " + additional;
			}
			fullDescription = String.format(pattern, size, date);
		}
		tvDesc.setText(fullDescription);
	}

	private void showIndeterminateProgress() {
		tvDesc.setVisibility(View.GONE);
		pbProgress.setVisibility(View.VISIBLE);
		pbProgress.setIndeterminate(true);
	}

	private void hideIndeterminateProgress() {
		tvDesc.setVisibility(View.VISIBLE);
		pbProgress.setVisibility(View.GONE);
		pbProgress.setIndeterminate(false);
	}

	private boolean checkDisabledAndClickAction(DownloadItem item) {
		RightButtonAction clickAction = getClickAction(item);
		boolean disabled = clickAction != RightButtonAction.DOWNLOAD;
		OnClickListener action = getRightButtonAction(item, clickAction);
		if (clickAction != RightButtonAction.DOWNLOAD) {
			btnRight.setText(R.string.shared_string_get);
			btnRight.setVisibility(View.VISIBLE);
			ivBtnRight.setVisibility(View.GONE);
			btnRight.setOnClickListener(action);
		} else {
			btnRight.setVisibility(View.GONE);
			ivBtnRight.setVisibility(View.VISIBLE);
			boolean isDownloading = item.isDownloading(context.getDownloadThread());
			if (isDownloading) {
				ivBtnRight.setImageDrawable(getThemedIcon(context, R.drawable.ic_action_remove_dark));
				ivBtnRight.setContentDescription(context.getString(R.string.shared_string_cancel));
			} else if (!item.hasActualDataToDownload()) {
				ivBtnRight.setImageDrawable(getThemedIcon(context, R.drawable.ic_overflow_menu_white));
				ivBtnRight.setContentDescription(context.getString(R.string.shared_string_more));
			} else {
				ivBtnRight.setImageDrawable(getThemedIcon(context, getDownloadActionIconId(item)));
				ivBtnRight.setContentDescription(context.getString(R.string.shared_string_download));
			}
			ivBtnRight.setOnClickListener(action);
		}

		return disabled;
	}

	@ColorRes
	protected int getIconColorId(@NonNull DownloadItem downloadItem) {
		return downloadItem.isOutdated() ? R.color.color_distance : R.color.color_ok;
	}

	private int getDownloadActionIconId(@NonNull DownloadItem item) {
		return item instanceof MultipleDownloadItem ? R.drawable.ic_action_multi_download : R.drawable.ic_action_gsave_dark;
	}

	@NonNull
	public RightButtonAction getClickAction(@NonNull DownloadItem item) {
		RightButtonAction action = RightButtonAction.DOWNLOAD;
		if (!item.isFree()) {
			DownloadActivityType type = item.getType();
			if (item.getBasename().equalsIgnoreCase(WORLD_SEAMARKS_KEY) && nauticalPluginDisabled) {
				action = RightButtonAction.ASK_FOR_SEAMARKS_PLUGIN;
			} else if ((type == SRTM_COUNTRY_FILE || type == HILLSHADE_FILE || type == SLOPE_FILE || type == GEOTIFF_FILE) && srtmDisabled) {
				action = srtmNeedsInstallation ? ASK_FOR_SRTM_PLUGIN_PURCHASE : ASK_FOR_SRTM_PLUGIN_ENABLE;
			} else if ((type == WIKIPEDIA_FILE || type == TRAVEL_FILE) && !Version.isPaidVersion(app)) {
				action = RightButtonAction.ASK_FOR_FULL_VERSION_PURCHASE;
			} else if ((type == DEPTH_CONTOUR_FILE || type == DEPTH_MAP_FILE) && !depthContoursPurchased) {
				action = RightButtonAction.ASK_FOR_DEPTH_CONTOURS_PURCHASE;
			} else if (item.getType() == WEATHER_FORECAST && !weatherAvailable) {
				action = RightButtonAction.ASK_FOR_WEATHER_PURCHASE;
			} else if ((item.getType() == WIKIPEDIA_FILE || item.getType() == TRAVEL_FILE)
					&& !Version.isPaidVersion(app)) {
				action = RightButtonAction.ASK_FOR_FULL_VERSION_PURCHASE;
			} else if ((item.getType() == DEPTH_CONTOUR_FILE || item.getType() == DEPTH_MAP_FILE) && !depthContoursPurchased) {
				action = RightButtonAction.ASK_FOR_DEPTH_CONTOURS_PURCHASE;
			}
		}
		return action;
	}

	public OnClickListener getRightButtonAction(DownloadItem item, RightButtonAction clickAction) {
		if (clickAction != RightButtonAction.DOWNLOAD) {
			return new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					switch (clickAction) {
						case ASK_FOR_FULL_VERSION_PURCHASE:
							app.logEvent("in_app_purchase_show_from_wiki_context_menu");
							ChoosePlanFragment.showInstance(context, OsmAndFeature.WIKIPEDIA);
							break;
						case ASK_FOR_WEATHER_PURCHASE:
							app.logEvent("in_app_purchase_show_from_weather_context_menu");
							ChoosePlanFragment.showInstance(context, OsmAndFeature.WEATHER);
							break;
						case ASK_FOR_DEPTH_CONTOURS_PURCHASE:
							ChoosePlanFragment.showInstance(context, OsmAndFeature.NAUTICAL);
							break;
						case ASK_FOR_SEAMARKS_PLUGIN:
							showPluginsScreen();
							app.showShortToastMessage(R.string.activate_seamarks_plugin);
							break;
						case ASK_FOR_SRTM_PLUGIN_PURCHASE:
							ChoosePlanFragment.showInstance(context, OsmAndFeature.TERRAIN);
							break;
						case ASK_FOR_SRTM_PLUGIN_ENABLE:
							showPluginsScreen();
							app.showShortToastMessage(R.string.activate_srtm_plugin);
							break;
						case DOWNLOAD:
							break;
					}
				}

				private void showPluginsScreen() {
					Bundle params = new Bundle();
					params.putBoolean(PluginsFragment.OPEN_PLUGINS, true);
					Intent intent = context.getIntent();
					MapActivity.launchMapActivityMoveToTop(context, intent != null ? intent.getExtras() : null, null, params);
				}
			};
		} else {
			boolean isDownloading = item.isDownloading(context.getDownloadThread());
			return v -> {
				if (isDownloading) {
					if (silentCancelDownload) {
						context.getDownloadThread().cancelDownload(item);
					} else {
						context.makeSureUserCancelDownload(item);
					}
				} else if (!item.hasActualDataToDownload()) {
					showContextMenu(v, item, item.getRelatedGroup());
				} else {
					download(item, item.getRelatedGroup());
				}
			};
		}
	}

	protected void showContextMenu(View v,
			DownloadItem downloadItem,
			DownloadResourceGroup parentOptional) {
		PopupMenu optionsMenu = new PopupMenu(context, v);

		OnMenuItemClickListener removeItemClickListener = null;
		List<File> downloadedFiles = downloadItem.getDownloadedFiles(app);
		if (!Algorithms.isEmpty(downloadedFiles)) {
			removeItemClickListener = _item -> {
				confirmRemove(downloadItem, downloadedFiles);
				return true;
			};
		}
		if (removeItemClickListener != null) {
			optionsMenu.getMenu()
					.add(R.string.shared_string_remove)
					.setIcon(getThemedIcon(context, R.drawable.ic_action_remove_dark))
					.setOnMenuItemClickListener(removeItemClickListener);
		}

		MenuItem item = optionsMenu.getMenu()
				.add(R.string.shared_string_download)
				.setIcon(getThemedIcon(context, R.drawable.ic_action_import));
		item.setOnMenuItemClickListener(_item -> {
			download(downloadItem, parentOptional);
			return true;
		});

		optionsMenu.show();
	}

	protected void download(DownloadItem item, DownloadResourceGroup parentOptional) {
		boolean handled = false;
		if (parentOptional != null && item instanceof IndexItem indexItem) {
			WorldRegion region = DownloadResourceGroup.getRegion(parentOptional);
			context.setDownloadItem(region, indexItem.getTargetFile(app).getAbsolutePath());
		}
		if (item.getType() == DownloadActivityType.ROADS_FILE && parentOptional != null) {
			for (IndexItem ii : parentOptional.getIndividualResources()) {
				if (ii.getType() == DownloadActivityType.NORMAL_FILE) {
					if (ii.isDownloaded()) {
						handled = true;
						confirmDownload(item);
					}
					break;
				}
			}
		}
		if (!handled) {
			startDownload(item);
		}
	}

	private void confirmDownload(@NonNull DownloadItem item) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(R.string.are_you_sure);
		builder.setMessage(R.string.confirm_download_roadmaps);
		builder.setNegativeButton(R.string.shared_string_cancel, null);
		builder.setPositiveButton(R.string.shared_string_download, (dialog, which) -> startDownload(item));
		builder.show();
	}

	private void startDownload(DownloadItem item) {
		if (item instanceof IndexItem indexItem) {
			context.startDownload(indexItem);
		} else {
			selectIndexesToDownload(item);
		}
	}

	private void selectIndexesToDownload(@NonNull DownloadItem item) {
		SelectIndexesHelper.showDialog(item, context, dateFormat, showRemoteDate, indexes -> {
			IndexItem[] indexesArray = new IndexItem[indexes.size()];
			context.startDownload(indexes.toArray(indexesArray));
		});
	}

	private void confirmRemove(@NonNull DownloadItem downloadItem,
			@NonNull List<File> downloadedFiles) {
		AlertDialog.Builder confirm = new AlertDialog.Builder(context);

		String message;
		if (downloadedFiles.size() > 1) {
			message = context.getString(R.string.delete_number_files_question, downloadedFiles.size());
		} else {
			OsmandRegions regions = app.getRegions();
			String visibleName = downloadItem.getVisibleName(context, regions);
			String fileName = FileNameTranslationHelper.getFileName(context, regions, visibleName);
			message = context.getString(R.string.delete_confirmation_msg, fileName);
		}
		confirm.setMessage(message);

		confirm.setPositiveButton(R.string.shared_string_yes, (dialog, which) -> remove(downloadedFiles));
		confirm.setNegativeButton(R.string.shared_string_no, null);

		confirm.show();
	}

	private void remove(@NonNull List<File> filesToDelete) {
		LocalItem[] params = new LocalItem[filesToDelete.size()];
		for (int i = 0; i < filesToDelete.size(); i++) {
			File file = filesToDelete.get(i);
			LocalItemType type = LocalItemUtils.getItemType(app, file);
			if (type != null) {
				params[i] = new LocalItem(file, type);
			}
		}
		LocalOperationTask removeTask = new LocalOperationTask(app, DELETE_OPERATION, new OperationListener() {
			@Override
			public void onOperationFinished(@NonNull OperationType type, @NonNull String result) {
				if (AndroidUtils.isActivityNotDestroyed(context)) {
					context.onUpdatedIndexesList();
				}
			}
		});
		OsmAndTaskManager.executeTask(removeTask, params);
	}

	private Drawable getThemedIcon(DownloadActivity context, int resourceId) {
		return app.getUIUtilities().getThemedIcon(resourceId);
	}

	private Drawable getContentIcon(DownloadActivity context, int resourceId, int color) {
		return app.getUIUtilities().getPaintedIcon(resourceId, color);
	}
}
