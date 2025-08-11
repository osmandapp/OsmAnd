package net.osmand.plus.settings.backend.backup.items;

import static net.osmand.IndexConstants.GPX_FILE_EXT;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.shared.SharedUtil;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.mapmarkers.ItineraryType;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.mapmarkers.MapMarkersGroup;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.plus.settings.backend.backup.SettingsHelper;
import net.osmand.plus.settings.backend.backup.SettingsItemReader;
import net.osmand.plus.settings.backend.backup.SettingsItemType;
import net.osmand.plus.settings.backend.backup.SettingsItemWriter;
import net.osmand.plus.settings.backend.backup.exporttype.ExportType;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.util.Algorithms;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class HistoryMarkersSettingsItem extends CollectionSettingsItem<MapMarker> {

	private static final int APPROXIMATE_HISTORY_MARKER_SIZE_BYTES = 380;

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
	protected void deleteItem(MapMarker item) {
		markersHelper.removeMarker(item);
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
			MapMarker renamedMarker = new MapMarker(item.point, description, item.colorIndex);
			if (!isDuplicate(renamedMarker)) {
				renamedMarker.history = true;
				renamedMarker.selected = item.selected;
				renamedMarker.visitedDate = item.visitedDate;
				renamedMarker.creationDate = item.creationDate;
				return renamedMarker;
			}
		}
	}

	@Override
	public long getEstimatedItemSize(@NonNull MapMarker item) {
		return APPROXIMATE_HISTORY_MARKER_SIZE_BYTES;
	}

	public MapMarkersGroup getMarkersGroup() {
		String name = app.getString(R.string.markers_history);
		String groupId = ExportType.HISTORY_MARKERS.name();
		MapMarkersGroup markersGroup = new MapMarkersGroup(groupId, name, ItineraryType.MARKERS);
		markersGroup.setMarkers(items);
		return markersGroup;
	}

	@Nullable
	@Override
	public SettingsItemReader<HistoryMarkersSettingsItem> getReader() {
		return new SettingsItemReader<HistoryMarkersSettingsItem>(this) {

			@Override
			public File readFromStream(@NonNull InputStream inputStream, @Nullable File inputFile,
			                           @Nullable String entryName) throws IllegalArgumentException {
				GpxFile gpxFile = SharedUtil.loadGpxFile(inputStream);
				if (gpxFile.getError() != null) {
					warnings.add(app.getString(R.string.settings_item_read_error, String.valueOf(getType())));
					SettingsHelper.LOG.error("Failed read gpx file", SharedUtil.jException(gpxFile.getError()));
				} else {
					List<MapMarker> mapMarkers = markersHelper.getDataHelper().readMarkersFromGpx(gpxFile, true);
					items.addAll(mapMarkers);
				}
				return null;
			}
		};
	}

	@Nullable
	@Override
	public SettingsItemWriter<? extends SettingsItem> getWriter() {
		GpxFile gpxFile = markersHelper.getDataHelper().generateGpx(items, true);
		return getGpxWriter(gpxFile);
	}
}