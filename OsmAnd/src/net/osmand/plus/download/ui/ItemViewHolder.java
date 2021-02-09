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

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.view.ViewCompat;

import net.osmand.map.WorldRegion;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.LocalIndexHelper.LocalIndexType;
import net.osmand.plus.activities.LocalIndexInfo;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.PluginsFragment;
import net.osmand.plus.chooseplan.ChoosePlanDialogFragment;
import net.osmand.plus.download.CityItem;
import net.osmand.plus.download.CustomIndexItem;
import net.osmand.plus.download.DisplayItem;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadResourceGroup;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.download.MultiIndexItem;
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

	public void bindIndexItem(final DisplayItem displayItem) {
		bindIndexItem(displayItem, null);
	}

	public void bindIndexItem(final DisplayItem displayItem, final String cityName) {
		initAppStatusVariables();
		boolean isDownloading = displayItem.isDownloading(context.getDownloadThread());
		int progress = -1;
		if (context.getDownloadThread().getCurrentDownloadingItem() == displayItem) {
			progress = context.getDownloadThread().getCurrentDownloadingItemProgress();
		}
		boolean disabled = checkDisabledAndClickAction(displayItem);
		/// name and left item
		String name;
		boolean isMultiIndexItem = displayItem instanceof MultiIndexItem;
		if(showTypeInName || isMultiIndexItem) {
			name = displayItem.getType().getString(context);
		} else {
			name = ((IndexItem) displayItem).getVisibleName(context, context.getMyApplication().getRegions(), showParentRegionName);
		}
		String text = (!Algorithms.isEmpty(cityName) && !cityName.equals(name) ? cityName + "\n" : "") + name;
		nameTextView.setText(text);
		if(!disabled) {
			nameTextView.setTextColor(textColorPrimary);
		} else {
			nameTextView.setTextColor(textColorSecondary);
		}
		int color = textColorSecondary;
		if(displayItem.isDownloaded() && !isDownloading) {
			int colorId = displayItem.isOutdated() ? R.color.color_distance : R.color.color_ok;
			color = context.getResources().getColor(colorId);
		}
		if (displayItem.isDownloaded()) {
			leftImageView.setImageDrawable(getContentIcon(context,
					displayItem.getType().getIconResource(), color));
		} else if (disabled) {
			leftImageView.setImageDrawable(getContentIcon(context,
					displayItem.getType().getIconResource(), textColorSecondary));
		} else {
			leftImageView.setImageDrawable(getContentIcon(context,
					displayItem.getType().getIconResource()));
		}
		descrTextView.setTextColor(textColorSecondary);
		if (!isDownloading) {
			progressBar.setVisibility(View.GONE);
			descrTextView.setVisibility(View.VISIBLE);
			if (displayItem instanceof CustomIndexItem && (((CustomIndexItem) displayItem).getSubName(context) != null)) {
				descrTextView.setText(((CustomIndexItem) displayItem).getSubName(context));
			} else if (displayItem.getType() == DownloadActivityType.DEPTH_CONTOUR_FILE && !depthContoursPurchased) {
				descrTextView.setText(context.getString(R.string.depth_contour_descr));
			} else if ((displayItem.getType() == DownloadActivityType.SRTM_COUNTRY_FILE
					|| displayItem.getType() == DownloadActivityType.HILLSHADE_FILE
					|| displayItem.getType() == DownloadActivityType.SLOPE_FILE) && srtmDisabled) {
				if (showTypeInName) {
					descrTextView.setText("");
				} else {
					descrTextView.setText(displayItem.getType().getString(context));
				}
			} else if (isMultiIndexItem) {
				MultiIndexItem item = (MultiIndexItem) displayItem;
				String columnFormat = context.getString(R.string.ltr_or_rtl_combine_via_colon);
				String allRegionsTitle = context.getString(R.string.shared_strings_all_regions);
				int regionsCount = item.getAllIndexes().size();
				String regionsCountDescription =
						String.format(columnFormat, allRegionsTitle, String.valueOf(regionsCount));
				String downloadSizeDescription = item.getSizeToDownloadDescription(context);
				String boldPointFormat =
						context.getString(R.string.ltr_or_rtl_combine_via_bold_point);
				String fullDescription =
						String.format(boldPointFormat, regionsCountDescription, downloadSizeDescription);
				descrTextView.setText(fullDescription);
			} else {
				IndexItem item = (IndexItem) displayItem;
				if (showTypeInDesc) {
					descrTextView.setText(item.getType().getString(context) +
							" • " + item.getSizeDescription(context) +
							" • " + (showRemoteDate ? item.getRemoteDate(dateFormat) : item.getLocalDate(dateFormat)));
				} else {
					descrTextView.setText(item.getSizeDescription(context) + " • " +
							(showRemoteDate ? item.getRemoteDate(dateFormat) : item.getLocalDate(dateFormat)));
				}
			}

		} else {
			progressBar.setVisibility(View.VISIBLE);
			progressBar.setIndeterminate(progress == -1);
			progressBar.setProgress(progress);
			
			if (showProgressInDesc) {
				double mb = displayItem.getArchiveSizeMB();
				String v ;
				if (progress != -1) {
					v = context.getString(R.string.value_downloaded_of_max, mb * progress / 100, mb);
				} else {
					v = context.getString(R.string.file_size_in_mb, mb);
				}
				if(showTypeInDesc && displayItem.getType() == DownloadActivityType.ROADS_FILE) {
					descrTextView.setText(displayItem.getType().getString(context) + " • " + v);
				} else {
					descrTextView.setText(v);
				}
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

	protected void download(DisplayItem item, DownloadResourceGroup parentOptional) {
		boolean handled = false;
		if(parentOptional != null && item instanceof IndexItem) {
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
	private void confirmDownload(final DisplayItem item) {
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

	private void startDownload(DisplayItem item) {
		if (item instanceof MultiIndexItem) {
			MultiIndexItem multiIndexItem = (MultiIndexItem) item;
			List<IndexItem> indexes = multiIndexItem.getIndexesToDownload();
			if (!Algorithms.isEmpty(indexes)) {
				IndexItem[] indexesArray = new IndexItem[indexes.size()];
				context.startDownload(indexes.toArray(indexesArray));
			}
		} else if (item instanceof IndexItem) {
			IndexItem indexItem = (IndexItem) item;
			context.startDownload(indexItem);
		}
	}

	private boolean checkDisabledAndClickAction(final DisplayItem item) {
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
			} else if (item instanceof IndexItem) {
				if(item.isDownloaded() && !item.isOutdated()) {
					rightImageButton.setImageDrawable(getContentIcon(context, R.drawable.ic_overflow_menu_white));
					rightImageButton.setContentDescription(context.getString(R.string.shared_string_more));
				} else {
					rightImageButton.setImageDrawable(getContentIcon(context, R.drawable.ic_action_import));
					rightImageButton.setContentDescription(context.getString(R.string.shared_string_download));
				}
			} else if (item instanceof MultiIndexItem) {
				MultiIndexItem multiIndexItem = (MultiIndexItem) item;
				if (multiIndexItem.hasMapsToDownload()) {
					rightImageButton.setImageDrawable(getContentIcon(context, R.drawable.ic_action_multi_download));
					rightImageButton.setContentDescription(context.getString(R.string.shared_string_download));
				} else {
					rightImageButton.setVisibility(View.GONE);
				}
			}
			rightImageButton.setOnClickListener(action);
		}
		
		return disabled;
	}

	@SuppressLint("DefaultLocale")
	public RightButtonAction getClickAction(final DisplayItem item) {
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

		} else if (item.getType() == DownloadActivityType.WIKIPEDIA_FILE
				&& !Version.isPaidVersion(context.getMyApplication())) {
			clickAction = RightButtonAction.ASK_FOR_FULL_VERSION_PURCHASE;
		} else if (item.getType() == DownloadActivityType.DEPTH_CONTOUR_FILE && !depthContoursPurchased) {
			clickAction = RightButtonAction.ASK_FOR_DEPTH_CONTOURS_PURCHASE;
		}
		return clickAction;
	}

	public OnClickListener getRightButtonAction(final DisplayItem item, final RightButtonAction clickAction) {
		if (clickAction != RightButtonAction.DOWNLOAD) {
			return new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					switch (clickAction) {
						case ASK_FOR_FULL_VERSION_PURCHASE:
							context.getMyApplication().logEvent("in_app_purchase_show_from_wiki_context_menu");
							ChoosePlanDialogFragment.showWikipediaInstance(context.getSupportFragmentManager());
							break;
						case ASK_FOR_DEPTH_CONTOURS_PURCHASE:
							ChoosePlanDialogFragment.showSeaDepthMapsInstance(context.getSupportFragmentManager());
							break;
						case ASK_FOR_SEAMARKS_PLUGIN:
							showPluginsScreen();
							Toast.makeText(context.getApplicationContext(),
									context.getString(R.string.activate_seamarks_plugin), Toast.LENGTH_SHORT).show();
							break;
						case ASK_FOR_SRTM_PLUGIN_PURCHASE:
							ChoosePlanDialogFragment.showHillshadeSrtmPluginInstance(context.getSupportFragmentManager());
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
					} else if (item instanceof IndexItem) {
						if(item.isDownloaded() && !item.isOutdated()){
							contextMenu(v, item, item.getRelatedGroup());
						} else {
							download(item, item.getRelatedGroup());
						}
					} else if (item instanceof MultiIndexItem) {
						if (((MultiIndexItem) item).hasMapsToDownload()) {
							download(item, item.getRelatedGroup());
						}
					}
				}
			};
		}
	}

	protected void contextMenu(View v, final DisplayItem displayItem, final DownloadResourceGroup parentOptional) {
		if (!(displayItem instanceof IndexItem)) return;
		final IndexItem indexItem = (IndexItem) displayItem;

		final PopupMenu optionsMenu = new PopupMenu(context, v);
		MenuItem item;
		
		final File fl = indexItem.getTargetFile(context.getMyApplication());
		if (fl.exists()) {
			item = optionsMenu.getMenu().add(R.string.shared_string_remove).setIcon(
							context.getMyApplication().getUIUtilities().getThemedIcon(R.drawable.ic_action_remove_dark));
			item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					LocalIndexType tp = getLocalIndexType(indexItem);
					final LocalIndexInfo info = new LocalIndexInfo(tp, fl, false, context.getMyApplication());
					AlertDialog.Builder confirm = new AlertDialog.Builder(context);
					confirm.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							new LocalIndexOperationTask(context, null, LocalIndexOperationTask.DELETE_OPERATION)
									.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, info);
						}
					});
					confirm.setNegativeButton(R.string.shared_string_no, null);
					String fn = FileNameTranslationHelper.getFileName(context, context.getMyApplication().getRegions(),
							indexItem.getVisibleName(context, context.getMyApplication().getRegions()));
					confirm.setMessage(context.getString(R.string.delete_confirmation_msg, fn));
					confirm.show();
					return true;
				}
			});
		}
		item = optionsMenu.getMenu().add(R.string.shared_string_download)
				.setIcon(context.getMyApplication().getUIUtilities().getThemedIcon(R.drawable.ic_action_import));
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				download(indexItem, parentOptional);
				return true;
			}
		});
		
		optionsMenu.show();
	}

	private LocalIndexType getLocalIndexType(DisplayItem displayItem) {
		LocalIndexType tp = LocalIndexType.MAP_DATA;
		if (displayItem.getType() == DownloadActivityType.HILLSHADE_FILE) {
			tp = LocalIndexType.TILES_DATA;
		} else if (displayItem.getType() == DownloadActivityType.SLOPE_FILE) {
			tp = LocalIndexType.TILES_DATA;
		} else if (displayItem.getType() == DownloadActivityType.ROADS_FILE) {
			tp = LocalIndexType.MAP_DATA;
		} else if (displayItem.getType() == DownloadActivityType.SRTM_COUNTRY_FILE) {
			tp = LocalIndexType.SRTM_DATA;
		} else if (displayItem.getType() == DownloadActivityType.WIKIPEDIA_FILE) {
			tp = LocalIndexType.MAP_DATA;
		} else if (displayItem.getType() == DownloadActivityType.WIKIVOYAGE_FILE) {
			tp = LocalIndexType.MAP_DATA;
		} else if (displayItem.getType() == DownloadActivityType.TRAVEL_FILE) {
			tp = LocalIndexType.MAP_DATA;
		} else if (displayItem.getType() == DownloadActivityType.FONT_FILE) {
			tp = LocalIndexType.FONT_DATA;
		} else if (displayItem.getType() == DownloadActivityType.VOICE_FILE) {
			tp = displayItem.getBasename().contains("tts") ? LocalIndexType.TTS_VOICE_DATA
					: LocalIndexType.VOICE_DATA;
		}
		return tp;
	}

	private Drawable getContentIcon(DownloadActivity context, int resourceId) {
		return context.getMyApplication().getUIUtilities().getThemedIcon(resourceId);
	}

	private Drawable getContentIcon(DownloadActivity context, int resourceId, int color) {
		return context.getMyApplication().getUIUtilities().getPaintedIcon(resourceId, color);
	}
}
