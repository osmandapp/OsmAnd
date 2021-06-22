package net.osmand.plus.settings.backend.backup.items;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.mapmarkers.ItineraryType;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.mapmarkers.MapMarkersDbHelper;
import net.osmand.plus.mapmarkers.MapMarkersGroup;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.plus.settings.backend.ExportSettingsType;
import net.osmand.plus.settings.backend.backup.SettingsHelper;
import net.osmand.plus.settings.backend.backup.SettingsItemReader;
import net.osmand.plus.settings.backend.backup.SettingsItemType;
import net.osmand.plus.settings.backend.backup.SettingsItemWriter;
import net.osmand.util.Algorithms;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static net.osmand.IndexConstants.GPX_FILE_EXT;

public class HistoryMarkersSettingsItem extends CollectionSettingsItem<MapMarker> {

	private MapMarkersHelper markersHelper;

	public HistoryMarkersSettingsItem(@NonNull OsmandApplication app, @NonNull List<MapMarker> items) {
		super(app, null, items);
	}

	public HistoryMarkersSettingsItem(@NonNull OsmandApplication app, @Nullable HistoryMarkersSettingsItem baseItem, @NonNull List<MapMarker> items) {
		super(app, baseItem, items);
	}

	public HistoryMarkersSettingsItem(@NonNull OsmandApplication app, @NonNull JSONObject json) throws JSONException {
		super(app, json);
	}

	@Override
	protected void init() {
		super.init();
		markersHelper = app.getMapMarkersHelper();
		existingItems = new ArrayList<>(markersHelper.getMapMarkersHistory());
	}

	@NonNull
	@Override
	public SettingsItemType getType() {
		return SettingsItemType.HISTORY_MARKERS;
	}

	@NonNull
	@Override
	public String getName() {
		return "history_markers";
	}

	@NonNull
	@Override
	public String getPublicName(@NonNull Context ctx) {
		return ctx.getString(R.string.markers_history);
	}

	@NonNull
	public String getDefaultFileExtension() {
		return GPX_FILE_EXT;
	}

	@Override
	public long getLocalModifiedTime() {
		return markersHelper.getMarkersHistoryLastModifiedTime();
	}

	@Override
	public void setLocalModifiedTime(long lastModifiedTime) {
		markersHelper.setMarkersHistoryLastModifiedTime(lastModifiedTime);
	}

	@Override
	public void apply() {
		List<MapMarker> newItems = getNewItems();
		if (!newItems.isEmpty() || !duplicateItems.isEmpty()) {
			appliedItems = new ArrayList<>(newItems);

			for (MapMarker duplicate : duplicateItems) {
				if (shouldReplace) {
					MapMarker existingMarker = markersHelper.getMapMarker(duplicate.point);
					markersHelper.removeMarker(existingMarker);
				}
				appliedItems.add(shouldReplace ? duplicate : renameItem(duplicate));
			}

			for (MapMarker marker : appliedItems) {
				markersHelper.addMarker(marker);
			}
		}
	}

	@Override
	public boolean isDuplicate(@NonNull MapMarker mapMarker) {
		for (MapMarker marker : existingItems) {
			if (marker.equals(mapMarker)
					&& Algorithms.objectEquals(marker.getOnlyName(), mapMarker.getOnlyName())) {
				return true;
			}
		}
		return false;
	}

	@NonNull
	@Override
	public MapMarker renameItem(@NonNull MapMarker item) {
		int number = 0;
		while (true) {
			number++;
			String name = item.getOnlyName() + " " + number;
			PointDescription description = new PointDescription(PointDescription.POINT_TYPE_LOCATION, name);
			MapMarker renamedMarker = new MapMarker(item.point, description, item.colorIndex, item.selected, item.index);
			if (!isDuplicate(renamedMarker)) {
				renamedMarker.history = true;
				renamedMarker.visitedDate = item.visitedDate;
				renamedMarker.creationDate = item.creationDate;
				renamedMarker.nextKey = MapMarkersDbHelper.HISTORY_NEXT_VALUE;
				return renamedMarker;
			}
		}
	}

	public MapMarkersGroup getMarkersGroup() {
		String name = app.getString(R.string.markers_history);
		String groupId = ExportSettingsType.HISTORY_MARKERS.name();
		MapMarkersGroup markersGroup = new MapMarkersGroup(groupId, name, ItineraryType.MARKERS);
		markersGroup.setMarkers(items);
		return markersGroup;
	}

	@Nullable
	@Override
	public SettingsItemReader<HistoryMarkersSettingsItem> getReader() {
		return new SettingsItemReader<HistoryMarkersSettingsItem>(this) {

			@Override
			public void readFromStream(@NonNull InputStream inputStream, String entryName) throws IllegalArgumentException {
				GPXFile gpxFile = GPXUtilities.loadGPXFile(inputStream);
				if (gpxFile.error != null) {
					warnings.add(app.getString(R.string.settings_item_read_error, String.valueOf(getType())));
					SettingsHelper.LOG.error("Failed read gpx file", gpxFile.error);
				} else {
					List<MapMarker> mapMarkers = markersHelper.getDataHelper().readMarkersFromGpx(gpxFile, true);
					items.addAll(mapMarkers);
				}
			}
		};
	}

	@Nullable
	@Override
	public SettingsItemWriter<? extends SettingsItem> getWriter() {
		GPXFile gpxFile = markersHelper.getDataHelper().generateGpx(items, true);
		return getGpxWriter(gpxFile);
	}
}