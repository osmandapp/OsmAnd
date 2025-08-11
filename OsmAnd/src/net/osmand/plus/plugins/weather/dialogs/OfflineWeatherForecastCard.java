package net.osmand.plus.plugins.weather.dialogs;

import static android.text.format.DateUtils.WEEK_IN_MILLIS;
import static net.osmand.plus.download.DownloadActivityType.WEATHER_FORECAST;
import static net.osmand.plus.download.local.LocalItemType.WEATHER_DATA;
import static net.osmand.plus.download.local.OperationType.DELETE_OPERATION;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.download.DownloadValidationManager;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.download.local.LocalItem;
import net.osmand.plus.download.local.LocalOperationTask;
import net.osmand.plus.download.local.LocalOperationTask.OperationListener;
import net.osmand.plus.download.local.OperationType;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class OfflineWeatherForecastCard extends MapBaseCard implements DownloadEvents, OperationListener {

	private static final Log LOG = PlatformUtil.getLog(OfflineWeatherForecastCard.class);

	private final OsmandSettings settings;
	private final DownloadIndexesThread downloadThread;
	private final DownloadValidationManager downloadManager;

	private final List<View> itemViews = new ArrayList<>();
	private final int profileColor;
	private final int defaultColor;
	private ViewGroup itemsContainer;

	public OfflineWeatherForecastCard(@NonNull MapActivity activity) {
		super(activity);
		settings = app.getSettings();
		downloadThread = app.getDownloadThread();
		downloadManager = new DownloadValidationManager(app);

		ApplicationMode appMode = settings.getApplicationMode();
		profileColor = appMode.getProfileColor(nightMode);
		defaultColor = ColorUtilities.getDefaultIconColor(app, nightMode);
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.card_weather_offline_forecast;
	}

	@Override
	protected void updateContent() {
		itemsContainer = view.findViewById(R.id.items_container);

		if (!downloadThread.getIndexes().isDownloadedFromInternet && settings.isInternetConnectionAvailable()) {
			downloadThread.runReloadIndexFiles();
		}

		itemViews.clear();
		itemsContainer.removeAllViews();

		if (downloadThread.shouldDownloadIndexes()) {
			setupReloadIndexesItem();
		} else {
			setupIndexItems();
		}
	}

	private void setupReloadIndexesItem() {
		View itemView = themedInflater.inflate(R.layout.list_item_icon_and_download, itemsContainer, false);

		TextView title = itemView.findViewById(R.id.title);
		title.setText(R.string.downloading_list_indexes);

		ProgressBar progressBar = itemView.findViewById(R.id.ProgressBar);
		progressBar.setIndeterminate(true);
		AndroidUiHelper.updateVisibility(progressBar, true);
		AndroidUiHelper.updateVisibility(itemView.findViewById(R.id.icon), false);
		AndroidUiHelper.updateVisibility(itemView.findViewById(R.id.divider), false);

		itemViews.add(itemView);
		itemsContainer.addView(itemView);
	}

	private void setupIndexItems() {
		List<IndexItem> weatherItems = findWeatherIndexes();
		Iterator<IndexItem> iterator = weatherItems.iterator();
		while (iterator.hasNext()) {
			View itemView = createIndexItemView(iterator.next(), iterator.hasNext());
			itemViews.add(itemView);
			itemsContainer.addView(itemView);
		}
		updateVisibility(!weatherItems.isEmpty());
	}

	@NonNull
	private View createIndexItemView(@NonNull IndexItem indexItem, boolean showDivider) {
		View itemView = themedInflater.inflate(R.layout.list_item_icon_and_download, itemsContainer, false);
		itemView.setTag(indexItem);

		TextView title = itemView.findViewById(R.id.title);
		title.setText(indexItem.getVisibleName(app, app.getRegions(), false));

		updateItemView(indexItem, itemView);
		AndroidUiHelper.updateVisibility(itemView.findViewById(R.id.divider), showDivider);
		AndroidUtils.setBackground(itemView, UiUtilities.getColoredSelectableDrawable(app, profileColor, 0.3f));

		return itemView;
	}

	private void updateItemView(@NonNull IndexItem indexItem, @NonNull View itemView) {
		boolean downloaded = indexItem.isDownloaded();
		TextView description = itemView.findViewById(R.id.description);
		if (downloaded) {
			String pattern = app.getString(R.string.ltr_or_rtl_combine_via_dash);
			String caption = app.getString(R.string.available_until, "").trim();
			String expireDate = OsmAndFormatter.getFormattedDate(app, getDataExpireTime(indexItem));
			description.setText(String.format(pattern, caption, expireDate));
		} else {
			String pattern = app.getString(R.string.ltr_or_rtl_combine_via_bold_point);
			String type = WEATHER_FORECAST.getString(app);
			description.setText(String.format(pattern, type, indexItem.getSizeDescription(app)));
		}
		AndroidUiHelper.updateVisibility(description, !downloadThread.isDownloading(indexItem));

		ImageView icon = itemView.findViewById(R.id.icon);
		icon.setImageResource(WEATHER_FORECAST.getIconResource());
		icon.setColorFilter(downloaded ? profileColor : defaultColor);

		ImageView secondaryIcon = itemView.findViewById(R.id.secondary_icon);
		updateSecondaryIcon(indexItem, itemView);
		itemView.setOnClickListener(v -> {
			if (downloadThread.isDownloading(indexItem)) {
				downloadThread.cancelDownload(indexItem);
			} else if (indexItem.isDownloaded()) {
				showPopUpMenu(indexItem, secondaryIcon);
			} else {
				downloadManager.startDownload(mapActivity, indexItem);
			}
		});
		updateProgress(indexItem, itemView);
	}

	private void updateProgress(@NonNull IndexItem indexItem, @NonNull View itemView) {
		ProgressBar progressBar = itemView.findViewById(R.id.ProgressBar);

		boolean downloading = downloadThread.isDownloading(indexItem);
		if (downloading) {
			boolean currentDownloading = downloadThread.isCurrentDownloading(indexItem);
			progressBar.setIndeterminate(!currentDownloading);
			if (currentDownloading) {
				progressBar.setProgress((int) downloadThread.getCurrentDownloadProgress());
			}
		}
		AndroidUiHelper.updateVisibility(progressBar, downloading);
	}

	private void updateSecondaryIcon(@NonNull IndexItem indexItem, @NonNull View itemView) {
		boolean downloaded = indexItem.isDownloaded();
		boolean downloading = downloadThread.isDownloading(indexItem);
		ImageView secondaryIcon = itemView.findViewById(R.id.secondary_icon);

		if (downloading) {
			secondaryIcon.setColorFilter(defaultColor);
			secondaryIcon.setImageResource(R.drawable.ic_action_remove_dark);
		} else {
			secondaryIcon.setColorFilter(downloaded ? defaultColor : profileColor);
			secondaryIcon.setImageResource(downloaded ? R.drawable.ic_overflow_menu_white : R.drawable.ic_action_import);
		}
		AndroidUiHelper.updateVisibility(secondaryIcon, true);
	}

	public long getDataExpireTime(@NonNull IndexItem indexItem) {
		long downloadTime = indexItem.getLocalTimestamp();
		if (!indexItem.isDownloaded() || downloadTime == 0) {
			downloadTime = indexItem.getTimestamp();
		}
		return downloadTime + WEEK_IN_MILLIS;
	}

	private void showPopUpMenu(@NonNull IndexItem indexItem, @NonNull View anchorView) {
		List<PopUpMenuItem> menuItems = new ArrayList<>();

		menuItems.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_update)
				.setIcon(getContentIcon(R.drawable.ic_action_refresh_dark))
				.setOnClickListener(v -> downloadManager.startDownload(mapActivity, indexItem))
				.create()
		);

		menuItems.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_remove)
				.setIcon(getContentIcon(R.drawable.ic_action_delete_outlined))
				.showTopDivider(true)
				.setOnClickListener(v -> confirmRemove(indexItem))
				.create()
		);

		PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
		displayData.anchorView = anchorView;
		displayData.menuItems = menuItems;
		displayData.nightMode = nightMode;
		displayData.layoutId = R.layout.popup_menu_item_full_divider;
		PopUpMenu.show(displayData);
	}

	private void confirmRemove(@NonNull IndexItem indexItem) {
		String name = indexItem.getVisibleName(app, app.getRegions(), false);
		String fileName = app.getString(R.string.ltr_or_rtl_combine_via_space, name, WEATHER_FORECAST.getString(app));

		AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
		builder.setMessage(app.getString(R.string.delete_confirmation_msg, fileName));
		builder.setPositiveButton(R.string.shared_string_yes, (dialog, which) -> remove(indexItem.getDownloadedFiles(app)));
		builder.setNegativeButton(R.string.shared_string_no, null);
		builder.show();
	}

	private void remove(@NonNull List<File> filesToDelete) {
		LocalItem[] params = new LocalItem[filesToDelete.size()];
		for (int i = 0; i < filesToDelete.size(); i++) {
			File file = filesToDelete.get(i);
			params[i] = new LocalItem(file, WEATHER_DATA);
		}
		LocalOperationTask removeTask = new LocalOperationTask(app, DELETE_OPERATION, this);
		OsmAndTaskManager.executeTask(removeTask, params);
	}

	@Override
	public void onUpdatedIndexesList() {
		updateContent();
	}

	@Override
	public void downloadInProgress() {
		if (AndroidUtils.isActivityNotDestroyed(mapActivity) && itemsContainer != null) {
			for (int i = 0; i < itemsContainer.getChildCount(); i++) {
				View itemView = itemsContainer.getChildAt(i);
				IndexItem indexItem = itemView != null ? (IndexItem) itemView.getTag() : null;
				if (indexItem != null) {
					updateItemView(indexItem, itemView);
				}
			}
		}
	}

	@Override
	public void downloadHasFinished() {
		updateContent();
	}

	@Override
	public void onOperationFinished(@NonNull OperationType type, @NonNull String result) {
		updateContent();
	}

	@NonNull
	private List<IndexItem> findWeatherIndexes() {
		List<IndexItem> items = new ArrayList<>();
		try {
			LatLon location = mapActivity.getMapLocation();
			items.addAll(DownloadResources.findIndexItemsAt(app, location, WEATHER_FORECAST, true));
		} catch (IOException e) {
			LOG.error(e);
		}
		IndexItem worldItem = downloadThread.getIndexes().getWeatherWorldItem();
		if (worldItem != null && !items.contains(worldItem)) {
			items.add(0, worldItem);
		}
		return items;
	}
}
