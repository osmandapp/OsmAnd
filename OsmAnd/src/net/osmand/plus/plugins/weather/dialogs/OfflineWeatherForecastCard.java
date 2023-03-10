package net.osmand.plus.plugins.weather.dialogs;

import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import net.osmand.data.LatLon;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.plugins.weather.OfflineForecastHelper;
import net.osmand.plus.plugins.weather.listener.RemoveLocalForecastListener;
import net.osmand.plus.plugins.weather.indexitem.WeatherIndexItem;
import net.osmand.plus.plugins.weather.viewholder.WeatherIndexItemViewHolder;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.widgets.ctxmenu.ViewCreator;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;

import java.util.ArrayList;
import java.util.List;

public class OfflineWeatherForecastCard extends MapBaseCard implements DownloadEvents, RemoveLocalForecastListener {

	private final OsmandSettings settings;
	private final List<WeatherIndexItemViewHolder> viewHolders = new ArrayList<>();

	private LinearLayout llContainer;

	public OfflineWeatherForecastCard(@NonNull MapActivity activity) {
		super(activity);
		settings = app.getSettings();
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.card_weather_offline_forecast;
	}

	@Override
	protected void updateContent() {
		llContainer = view.findViewById(R.id.items_container);
		recreateDownloadSection();
	}

	@Override
	public void onUpdatedIndexesList() {
		recreateDownloadSection();
	}

	@Override
	public void downloadInProgress() {
		updateIndexesList();
	}

	@Override
	public void downloadHasFinished() {
		updateIndexesList();
	}

	@Override
	public void onRemoveLocalForecastEvent() {
		updateIndexesList();
	}

	private void recreateDownloadSection() {
		if (mapActivity == null) return;
		DownloadIndexesThread downloadThread = app.getDownloadThread();
		DownloadResources indexes = downloadThread.getIndexes();
		if (!indexes.isDownloadedFromInternet && settings.isInternetConnectionAvailable()) {
			downloadThread.runReloadIndexFiles();
		}
		if (downloadThread.shouldDownloadIndexes()) {
			createReloadIndexesView();
		} else {
			createIndexesListView();
		}
	}

	private void createReloadIndexesView() {
		ViewCreator viewCreator = new ViewCreator(activity, nightMode);
		ContextMenuItem item = new ContextMenuItem(null)
				.setLayout(R.layout.list_item_icon_and_download)
				.setTitleId(R.string.downloading_list_indexes, mapActivity)
				.setHideDivider(true)
				.setLoading(true);
		llContainer.removeAllViews();
		llContainer.addView(viewCreator.getView(item, null));
	}

	private void createIndexesListView() {
		llContainer.removeAllViews();
		viewHolders.clear();
		ApplicationMode appMode = settings.getApplicationMode();
		List<IndexItem> weatherIndexes = findWeatherIndexes();
		for (int i = 0; i < weatherIndexes.size(); i++) {
			WeatherIndexItem ii = (WeatherIndexItem) weatherIndexes.get(i);
			boolean showDivider = i < (weatherIndexes.size() - 1);
			WeatherIndexItemViewHolder viewHolder = new WeatherIndexItemViewHolder(
					mapActivity, appMode, ii, nightMode, showDivider
			);
			viewHolders.add(viewHolder);
			llContainer.addView(viewHolder.getView());
		}
	}

	public void updateIndexesList() {
		for (WeatherIndexItemViewHolder viewHolder : viewHolders) {
			viewHolder.update();
		}
	}

	@NonNull
	private List<IndexItem> findWeatherIndexes() {
		LatLon location = mapActivity.getMapLocation();
		OfflineForecastHelper offlineForecastHelper = app.getOfflineForecastHelper();
		return offlineForecastHelper.findWeatherIndexesAt(location, true);
	}
}
