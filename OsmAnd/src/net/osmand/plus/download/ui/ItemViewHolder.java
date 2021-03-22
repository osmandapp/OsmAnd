package net.osmand.plus.download.ui;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.view.ViewCompat;

import net.osmand.map.OsmandRegions;
import net.osmand.map.WorldRegion;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.LocalIndexHelper.LocalIndexType;
import net.osmand.plus.activities.LocalIndexInfo;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.PluginsFragment;
import net.osmand.plus.chooseplan.ChoosePlanDialogFragment;
import net.osmand.plus.chooseplan.ChoosePlanDialogFragment.ChoosePlanDialogType;
import net.osmand.plus.download.CityItem;
import net.osmand.plus.download.CustomIndexItem;
import net.osmand.plus.download.DownloadItem;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadResourceGroup;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.download.MultipleIndexesUiHelper;
import net.osmand.plus.download.MultipleIndexesUiHelper.SelectItemsToDownloadListener;
import net.osmand.plus.download.MultipleIndexItem;
import net.osmand.plus.download.ui.LocalIndexesFragment.LocalIndexOperationTask;
import net.osmand.plus.helpers.FileNameTranslationHelper;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.util.Algorithms;

import java.io.File;
import java.text.DateFormat;
import java.util.List;

public class ItemViewHolder {

	protected final TextView nameTextView;
	protected final TextView descrTextView;
	protected final ImageView leftImageView;
	protected final ImageView rightImageButton;
	protected final Button rightButton;
	protected final ProgressBar progressBar;

	private boolean srtmDisabled;
	private boolean srtmNeedsInstallation;
	private boolean nauticalPluginDisabled;
	private boolean depthContoursPurchased;

	protected final DownloadActivity context;

	private int textColorPrimary;
	private int textColorSecondary;

	boolean showTypeInDesc;
	boolean showTypeInName;
	boolean showParentRegionName;
	boolean showRemoteDate;
	boolean silentCancelDownload;
	boolean showProgressInDesc;

	private DateFormat dateFormat;


	private enum RightButtonAction {
		DOWNLOAD,
		ASK_FOR_SEAMARKS_PLUGIN,
		ASK_FOR_SRTM_PLUGIN_PURCHASE,
		ASK_FOR_SRTM_PLUGIN_ENABLE,
		ASK_FOR_FULL_VERSION_PURCHASE,
		ASK_FOR_DEPTH_CONTOURS_PURCHASE
	}


	public ItemViewHolder(View view, DownloadActivity context) {
		this.context = context;
		dateFormat = android.text.format.DateFormat.getMediumDateFormat(context);
		progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
		rightButton = (Button) view.findViewById(R.id.rightButton);
		leftImageView = (ImageView) view.findViewById(R.id.icon);
		descrTextView = (TextView) view.findViewById(R.id.description);
		rightImageButton = (ImageView) view.findViewById(R.id.secondaryIcon);
		nameTextView = (TextView) view.findViewById(R.id.title);

		ViewCompat.setAccessibilityDelegate(view, context.getAccessibilityAssistant());
		ViewCompat.setAccessibilityDelegate(rightButton, context.getAccessibilityAssistant());
		ViewCompat.setAccessibilityDelegate(rightImageButton, context.getAccessibilityAssistant());

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

	private void initAppStatusVariables() {
		srtmDisabled = context.isSrtmDisabled();
		nauticalPluginDisabled = context.isNauticalPluginDisabled();
		srtmNeedsInstallation = context.isSrtmNeedsInstallation();
		depthContoursPurchased = InAppPurchaseHelper.isDepthContoursPurchased(context.getMyApplication());
	}

	public void bindIndexItem(final DownloadItem downloadItem) {
		bindIndexItem(downloadItem, null);
	}

	public void bindIndexItem(final DownloadItem downloadItem, final String cityName) {
		initAppStatusVariables();
		boolean isDownloading = downloadItem.isDownloading(context.getDownloadThread());
		int progress = -1;
		if (context.getDownloadThread().getCurrentDownloadingItem() == downloadItem) {
			progress = context.getDownloadThread().getCurrentDownloadingItemProgress();
		}
		boolean disabled = checkDisabledAndClickAction(downloadItem);
		/// name and left item
		String name;
		if(showTypeInName) {
			name = downloadItem.getType().getString(context);
		} else {
			name = downloadItem.getVisibleName(context, context.getMyApplication().getRegions(), showParentRegionName);
		}
		String text = (!Algorithms.isEmpty(cityName) && !cityName.equals(name) ? cityName + "\n" : "") + name;
		nameTextView.setText(text);
		if(!disabled) {
			nameTextView.setTextColor(textColorPrimary);
		} else {
			nameTextView.setTextColor(textColorSecondary);
		}
		int color = textColorSecondary;
		if(downloadItem.isDownloaded() && !isDownloading) {
			int colorId = downloadItem.isOutdated() ? R.color.color_distance : R.color.color_ok;
			color = context.getResources().getColor(colorId);
		}
		if (downloadItem.isDownloaded()) {
			leftImageView.setImageDrawable(getContentIcon(context,
					downloadItem.getType().getIconResource(), color));
		} else if (disabled) {
			leftImageView.setImageDrawable(getContentIcon(context,
					downloadItem.getType().getIconResource(), textColorSecondary));
		} else {
			leftImageView.setImageDrawable(getContentIcon(context,
					downloadItem.getType().getIconResource()));
		}
		descrTextView.setTextColor(textColorSecondary);
		if (!isDownloading) {
			progressBar.setVisibility(View.GONE);
			descrTextView.setVisibility(View.VISIBLE);
			if (downloadItem instanceof CustomIndexItem && (((CustomIndexItem) downloadItem).getSubName(context) != null)) {
				descrTextView.setText(((CustomIndexItem) downloadItem).getSubName(context));
			} else if (downloadItem.getType() == DownloadActivityType.DEPTH_CONTOUR_FILE && !depthContoursPurchased) {
				descrTextView.setText(context.getString(R.string.depth_contour_descr));
			} else if ((downloadItem.getType() == DownloadActivityType.SRTM_COUNTRY_FILE
					|| downloadItem.getType() == DownloadActivityType.HILLSHADE_FILE
					|| downloadItem.getType() == DownloadActivityType.SLOPE_FILE) && srtmDisabled) {
				if (showTypeInName) {
					descrTextView.setText("");
				} else {
					descrTextView.setText(downloadItem.getType().getString(context));
				}
			} else if (downloadItem instanceof MultipleIndexItem) {
				MultipleIndexItem item = (MultipleIndexItem) downloadItem;
				String allRegionsHeader = context.getString(R.string.shared_strings_all_regions);
				String regionsHeader = context.getString(R.string.regions);
				String allRegionsCount = String.valueOf(item.getAllIndexes().size());
				String leftToDownloadCount = String.valueOf(item.getIndexesToDownload().size());
				String header;
				String count;
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
				} else {
					header = allRegionsHeader;
					count = allRegionsCount;
				}
				String fullDescription =
						context.getString(R.string.ltr_or_rtl_combine_via_colon, header, count);
				if (item.hasActualDataToDownload()) {
					fullDescription = context.getString(
							R.string.ltr_or_rtl_combine_via_bold_point, fullDescription,
							item.getSizeDescription(context));
				}
				descrTextView.setText(fullDescription);
			} else {
				IndexItem item = (IndexItem) downloadItem;
				String pattern = context.getString(R.string.ltr_or_rtl_combine_via_bold_point);
				String type = item.getType().getString(context);
				String size = item.getSizeDescription(context);
				String date = item.getDate(dateFormat, showRemoteDate);
				String fullDescription = String.format(pattern, size, date);
				if (showTypeInDesc) {
					fullDescription = String.format(pattern, type, fullDescription);
				}
				descrTextView.setText(fullDescription);
			}

		} else {
			progressBar.setVisibility(View.VISIBLE);
			progressBar.setIndeterminate(progress == -1);
			progressBar.setProgress(progress);

			if (showProgressInDesc) {
				double mb = downloadItem.getArchiveSizeMB();
				String v ;
				if (progress != -1) {
					v = context.getString(R.string.value_downloaded_of_max, mb * progress / 100, mb);
				} else {
					v = context.getString(R.string.file_size_in_mb, mb);
				}
				String fullDescription = v;
				if(showTypeInDesc && downloadItem.getType() == DownloadActivityType.ROADS_FILE) {
					fullDescription = context.getString(R.string.ltr_or_rtl_combine_via_bold_point,
							downloadItem.getType().getString(context), fullDescription);
				}
				descrTextView.setText(fullDescription);
				descrTextView.setVisibility(View.VISIBLE);
			} else {
				descrTextView.setVisibility(View.GONE);
			}

		}
	}

	public void bindIndexItem(final CityItem cityItem) {
		if (cityItem.getIndexItem() != null) {
			bindIndexItem(cityItem.getIndexItem(), cityItem.getName());
		} else {
			nameTextView.setText(cityItem.getName());
			nameTextView.setTextColor(textColorPrimary);
			leftImageView.setImageDrawable(getContentIcon(context, R.drawable.ic_map));
			descrTextView.setVisibility(View.GONE);
			progressBar.setVisibility(View.GONE);
		}
	}

	private boolean checkDisabledAndClickAction(final DownloadItem item) {
		RightButtonAction clickAction = getClickAction(item);
		boolean disabled = clickAction != RightButtonAction.DOWNLOAD;
		OnClickListener action = getRightButtonAction(item, clickAction);
		if (clickAction != RightButtonAction.DOWNLOAD) {
			rightButton.setText(R.string.get_plugin);
			rightButton.setVisibility(View.VISIBLE);
			rightImageButton.setVisibility(View.GONE);
			rightButton.setOnClickListener(action);
		} else {
			rightButton.setVisibility(View.GONE);
			rightImageButton.setVisibility(View.VISIBLE);
			final boolean isDownloading = item.isDownloading(context.getDownloadThread());
			if (isDownloading) {
				rightImageButton.setImageDrawable(getContentIcon(context, R.drawable.ic_action_remove_dark));
				rightImageButton.setContentDescription(context.getString(R.string.shared_string_cancel));
			} else if(!item.hasActualDataToDownload()) {
				rightImageButton.setImageDrawable(getContentIcon(context, R.drawable.ic_overflow_menu_white));
				rightImageButton.setContentDescription(context.getString(R.string.shared_string_more));
			} else {
				rightImageButton.setImageDrawable(getContentIcon(context, getDownloadActionIconId(item)));
				rightImageButton.setContentDescription(context.getString(R.string.shared_string_download));
			}
			rightImageButton.setOnClickListener(action);
		}

		return disabled;
	}

	private int getDownloadActionIconId(@NonNull DownloadItem item) {
		return item instanceof MultipleIndexItem ?
				R.drawable.ic_action_multi_download :
				R.drawable.ic_action_import;
	}

	@SuppressLint("DefaultLocale")
	public RightButtonAction getClickAction(final DownloadItem item) {
		RightButtonAction clickAction = RightButtonAction.DOWNLOAD;
		if (item.getBasename().toLowerCase().equals(DownloadResources.WORLD_SEAMARKS_KEY)
				&& nauticalPluginDisabled) {
			clickAction = RightButtonAction.ASK_FOR_SEAMARKS_PLUGIN;
		} else if ((item.getType() == DownloadActivityType.SRTM_COUNTRY_FILE
				|| item.getType() == DownloadActivityType.HILLSHADE_FILE
				|| item.getType() == DownloadActivityType.SLOPE_FILE) && srtmDisabled) {
			if (srtmNeedsInstallation) {
				clickAction = RightButtonAction.ASK_FOR_SRTM_PLUGIN_PURCHASE;
			} else {
				clickAction = RightButtonAction.ASK_FOR_SRTM_PLUGIN_ENABLE;
			}

		} else if ((item.getType() == DownloadActivityType.WIKIPEDIA_FILE
				|| item.getType() == DownloadActivityType.TRAVEL_FILE)
				&& !Version.isPaidVersion(context.getMyApplication())) {
			clickAction = RightButtonAction.ASK_FOR_FULL_VERSION_PURCHASE;
		} else if (item.getType() == DownloadActivityType.DEPTH_CONTOUR_FILE && !depthContoursPurchased) {
			clickAction = RightButtonAction.ASK_FOR_DEPTH_CONTOURS_PURCHASE;
		}
		return clickAction;
	}

	public OnClickListener getRightButtonAction(final DownloadItem item, final RightButtonAction clickAction) {
		if (clickAction != RightButtonAction.DOWNLOAD) {
			return new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					switch (clickAction) {
						case ASK_FOR_FULL_VERSION_PURCHASE:
							context.getMyApplication().logEvent("in_app_purchase_show_from_wiki_context_menu");
							ChoosePlanDialogFragment.showDialogInstance(context.getMyApplication(), context.getSupportFragmentManager(), ChoosePlanDialogType.WIKIPEDIA);
							break;
						case ASK_FOR_DEPTH_CONTOURS_PURCHASE:
							ChoosePlanDialogFragment.showDialogInstance(context.getMyApplication(), context.getSupportFragmentManager(), ChoosePlanDialogType.SEA_DEPTH_MAPS);
							break;
						case ASK_FOR_SEAMARKS_PLUGIN:
							showPluginsScreen();
							Toast.makeText(context.getApplicationContext(),
									context.getString(R.string.activate_seamarks_plugin), Toast.LENGTH_SHORT).show();
							break;
						case ASK_FOR_SRTM_PLUGIN_PURCHASE:
							ChoosePlanDialogFragment.showDialogInstance(context.getMyApplication(), context.getSupportFragmentManager(), ChoosePlanDialogType.HILLSHADE_SRTM_PLUGIN);
							break;
						case ASK_FOR_SRTM_PLUGIN_ENABLE:
							showPluginsScreen();
							Toast.makeText(context, context.getString(R.string.activate_srtm_plugin),
									Toast.LENGTH_SHORT).show();
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
			final boolean isDownloading = item.isDownloading(context.getDownloadThread());
			return new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if(isDownloading) {
						if(silentCancelDownload) {
							context.getDownloadThread().cancelDownload(item);
						} else {
							context.makeSureUserCancelDownload(item);
						}
					} else if(!item.hasActualDataToDownload()){
						showContextMenu(v, item, item.getRelatedGroup());
					} else {
						download(item, item.getRelatedGroup());
					}
				}
			};
		}
	}

	protected void showContextMenu(View v,
	                               final DownloadItem downloadItem,
	                               final DownloadResourceGroup parentOptional) {
		OsmandApplication app = context.getMyApplication();
		PopupMenu optionsMenu = new PopupMenu(context, v);
		MenuItem item;

		final List<File> downloadedFiles = downloadItem.getDownloadedFiles(app);
		if (!Algorithms.isEmpty(downloadedFiles)) {
			item = optionsMenu.getMenu().add(R.string.shared_string_remove)
					.setIcon(getContentIcon(context, R.drawable.ic_action_remove_dark));
			item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					confirmRemove(downloadItem, downloadedFiles);
					return true;
				}
			});
		}
		item = optionsMenu.getMenu().add(R.string.shared_string_download)
				.setIcon(context.getMyApplication().getUIUtilities().getThemedIcon(R.drawable.ic_action_import));
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				download(downloadItem, parentOptional);
				return true;
			}
		});

		optionsMenu.show();
	}

	protected void download(DownloadItem item, DownloadResourceGroup parentOptional) {
		boolean handled = false;
		if (parentOptional != null && item instanceof IndexItem) {
			IndexItem indexItem = (IndexItem) item;
			WorldRegion region = DownloadResourceGroup.getRegion(parentOptional);
			context.setDownloadItem(region, indexItem.getTargetFile(context.getMyApplication()).getAbsolutePath());
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
		if(!handled) {
			startDownload(item);
		}
	}
	private void confirmDownload(final DownloadItem item) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(R.string.are_you_sure);
		builder.setMessage(R.string.confirm_download_roadmaps);
		builder.setNegativeButton(R.string.shared_string_cancel, null).setPositiveButton(
				R.string.shared_string_download, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (item != null) {
							startDownload(item);
						}
					}
				});
		builder.show();
	}

	private void startDownload(DownloadItem item) {
		if (item instanceof MultipleIndexItem) {
			selectIndexesToDownload((MultipleIndexItem) item);
		} else if (item instanceof IndexItem) {
			IndexItem indexItem = (IndexItem) item;
			context.startDownload(indexItem);
		}
	}

	private void selectIndexesToDownload(MultipleIndexItem item) {
		OsmandApplication app = context.getMyApplication();
		MultipleIndexesUiHelper.showDialog(item, context, app, dateFormat, showRemoteDate,
				new SelectItemsToDownloadListener() {
					@Override
					public void onItemsToDownloadSelected(List<IndexItem> indexes) {
						IndexItem[] indexesArray = new IndexItem[indexes.size()];
						context.startDownload(indexes.toArray(indexesArray));
					}
				}
		);
	}

	private void confirmRemove(@NonNull final DownloadItem downloadItem,
	                           @NonNull final List<File> downloadedFiles) {
		OsmandApplication app = context.getMyApplication();
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

		confirm.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				LocalIndexType type = getLocalIndexType(downloadItem);
				remove(type, downloadedFiles);
			}
		});
		confirm.setNegativeButton(R.string.shared_string_no, null);

		confirm.show();
	}

	private void remove(@NonNull LocalIndexType type,
	                    @NonNull List<File> filesToDelete) {
		OsmandApplication app = context.getMyApplication();
		LocalIndexOperationTask removeTask = new LocalIndexOperationTask(
				context,
				null,
				LocalIndexOperationTask.DELETE_OPERATION);
		LocalIndexInfo[] params = new LocalIndexInfo[filesToDelete.size()];
		for (int i = 0; i < filesToDelete.size(); i++) {
			File file = filesToDelete.get(i);
			params[i] = new LocalIndexInfo(type, file, false, app);
		}
		removeTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
	}

	@NonNull
	private LocalIndexType getLocalIndexType(@NonNull DownloadItem downloadItem) {
		LocalIndexType type = LocalIndexType.MAP_DATA;
		if (downloadItem.getType() == DownloadActivityType.HILLSHADE_FILE) {
			type = LocalIndexType.TILES_DATA;
		} else if (downloadItem.getType() == DownloadActivityType.SLOPE_FILE) {
			type = LocalIndexType.TILES_DATA;
		} else if (downloadItem.getType() == DownloadActivityType.ROADS_FILE) {
			type = LocalIndexType.MAP_DATA;
		} else if (downloadItem.getType() == DownloadActivityType.SRTM_COUNTRY_FILE) {
			type = LocalIndexType.SRTM_DATA;
		} else if (downloadItem.getType() == DownloadActivityType.WIKIPEDIA_FILE) {
			type = LocalIndexType.MAP_DATA;
		} else if (downloadItem.getType() == DownloadActivityType.WIKIVOYAGE_FILE) {
			type = LocalIndexType.MAP_DATA;
		} else if (downloadItem.getType() == DownloadActivityType.TRAVEL_FILE) {
			type = LocalIndexType.MAP_DATA;
		} else if (downloadItem.getType() == DownloadActivityType.FONT_FILE) {
			type = LocalIndexType.FONT_DATA;
		} else if (downloadItem.getType() == DownloadActivityType.VOICE_FILE) {
			type = downloadItem.getBasename().contains("tts") ? LocalIndexType.TTS_VOICE_DATA
					: LocalIndexType.VOICE_DATA;
		}
		return type;
	}

	private Drawable getContentIcon(DownloadActivity context, int resourceId) {
		return context.getMyApplication().getUIUtilities().getThemedIcon(resourceId);
	}

	private Drawable getContentIcon(DownloadActivity context, int resourceId, int color) {
		return context.getMyApplication().getUIUtilities().getPaintedIcon(resourceId, color);
	}
}
