package net.osmand.plus.settings.backend.backup;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.mapmarkers.MapMarkersDbHelper;
import net.osmand.plus.mapmarkers.MapMarkersGroup;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.util.Algorithms;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static net.osmand.IndexConstants.GPX_FILE_EXT;
import static net.osmand.plus.mapmarkers.MapMarkersHelper.CREATION_DATE;
import static net.osmand.plus.mapmarkers.MapMarkersHelper.GROUP_NAME;
import static net.osmand.plus.mapmarkers.MapMarkersHelper.GROUP_TYPE;
import static net.osmand.plus.mapmarkers.MapMarkersHelper.MARKER_HISTORY;
import static net.osmand.plus.mapmarkers.MapMarkersHelper.VISITED_DATE;

public class MarkersSettingsItem extends CollectionSettingsItem<MapMarkersGroup> {

	private MapMarkersHelper markersHelper;
	private Map<String, MapMarkersGroup> flatGroups = new LinkedHashMap<>();

	public MarkersSettingsItem(@NonNull OsmandApplication app, @NonNull List<MapMarkersGroup> items) {
		super(app, null, items);
	}

	public MarkersSettingsItem(@NonNull OsmandApplication app, @Nullable MarkersSettingsItem baseItem, @NonNull List<MapMarkersGroup> items) {
		super(app, baseItem, items);
	}

	MarkersSettingsItem(@NonNull OsmandApplication app, @NonNull JSONObject json) throws JSONException {
		super(app, json);
	}

	@Override
	protected void init() {
		super.init();
		markersHelper = app.getMapMarkersHelper();
		existingItems = new ArrayList<>(markersHelper.getMapMarkersGroups());
	}

	@NonNull
	@Override
	public SettingsItemType getType() {
		return SettingsItemType.MARKERS;
	}

	@NonNull
	@Override
	public String getName() {
		return "markers";
	}

	@NonNull
	@Override
	public String getPublicName(@NonNull Context ctx) {
		return ctx.getString(R.string.map_markers);
	}

	@NonNull
	public String getDefaultFileName() {
		return getName() + getDefaultFileExtension();
	}

	@NonNull
	public String getDefaultFileExtension() {
		return GPX_FILE_EXT;
	}

	@Override
	public void apply() {
		List<MapMarkersGroup> newItems = getNewItems();
		if (!newItems.isEmpty() || !duplicateItems.isEmpty()) {
			appliedItems = new ArrayList<>(newItems);

			for (MapMarkersGroup duplicate : duplicateItems) {
				if (shouldReplace) {
					MapMarkersGroup existingGroup = markersHelper.getMapMarkerGroupById(duplicate.getId(), duplicate.getType());
					if (existingGroup != null) {
						List<MapMarker> existingMarkers = new ArrayList<>(existingGroup.getMarkers());
						for (MapMarker marker : existingMarkers) {
							markersHelper.removeMarker(marker);
						}
					}
				}
				appliedItems.add(duplicate);
			}
			for (MapMarkersGroup markersGroup : appliedItems) {
				for (MapMarker marker : markersGroup.getMarkers()) {
					markersHelper.addMarker(marker);
				}
			}
		}
	}

	@Override
	public boolean isDuplicate(@NonNull MapMarkersGroup markersGroup) {
		String name = markersGroup.getName();
		for (MapMarkersGroup group : existingItems) {
			if (Algorithms.stringsEqual(group.getName(), name)
					&& !Algorithms.isEmpty(group.getMarkers())
					&& !Algorithms.isEmpty(markersGroup.getMarkers())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean shouldReadOnCollecting() {
		return true;
	}

	@NonNull
	@Override
	public MapMarkersGroup renameItem(@NonNull MapMarkersGroup item) {
		return item;
	}

	@Nullable
	@Override
	SettingsItemReader<MarkersSettingsItem> getReader() {
		return new SettingsItemReader<MarkersSettingsItem>(this) {

			@Override
			public void readFromStream(@NonNull InputStream inputStream, String entryName) throws IllegalArgumentException {
				GPXFile gpxFile = GPXUtilities.loadGPXFile(inputStream);
				if (gpxFile.error != null) {
					warnings.add(app.getString(R.string.settings_item_read_error, String.valueOf(getType())));
					SettingsHelper.LOG.error("Failed read gpx file", gpxFile.error);
				} else {
					List<Integer> markerColors = getMarkersColors();
					for (WptPt point : gpxFile.getPoints()) {
						LatLon latLon = new LatLon(point.lat, point.lon);

						int colorIndex = markerColors.indexOf(point.getColor());
						if (colorIndex == -1) {
							colorIndex = 0;
						}

						PointDescription pointDescription = new PointDescription(PointDescription.POINT_TYPE_LOCATION, point.name);
						MapMarker marker = new MapMarker(latLon, pointDescription, colorIndex, false, 0);

						String historyStr = point.getExtensionsToRead().get(MARKER_HISTORY);
						String creationDateStr = point.getExtensionsToRead().get(CREATION_DATE);
						String visitedDateStr = point.getExtensionsToRead().get(VISITED_DATE);
						marker.creationDate = Algorithms.parseLongSilently(creationDateStr, 0);
						marker.visitedDate = Algorithms.parseLongSilently(visitedDateStr, 0);
						marker.history = Boolean.parseBoolean(historyStr);
						marker.nextKey = MapMarkersDbHelper.TAIL_NEXT_VALUE;

						MapMarkersGroup group = getOrCreateGroup(point);
						group.getMarkers().add(marker);
					}
				}
			}
		};
	}

	private MapMarkersGroup getOrCreateGroup(WptPt point) {
		MapMarkersGroup markersGroup = flatGroups.get(point.category);
		if (markersGroup != null) {
			return markersGroup;
		}
		Map<String, String> extensions = point.getExtensionsToRead();
		String groupName = extensions.get(GROUP_NAME);
		String groupType = extensions.get(GROUP_TYPE);
		int type = Algorithms.parseIntSilently(groupType, MapMarkersGroup.ANY_TYPE);

		if (point.category != null && groupName != null) {
			markersGroup = new MapMarkersGroup(point.category, groupName, type);
		} else {
			markersGroup = new MapMarkersGroup();
		}
		flatGroups.put(markersGroup.getId(), markersGroup);
		items.add(markersGroup);

		return markersGroup;
	}

	private List<Integer> getMarkersColors() {
		List<Integer> colors = new ArrayList<>();
		for (int color : MapMarker.getColors(app)) {
			colors.add(color);
		}
		return colors;
	}

	@Nullable
	@Override
	SettingsItemWriter<MarkersSettingsItem> getWriter() {
		return new SettingsItemWriter<MarkersSettingsItem>(this) {

			@Override
			public boolean writeToStream(@NonNull OutputStream outputStream) throws IOException {
				List<MapMarker> mapMarkers = getMarkersFromGroups(items);
				GPXFile gpxFile = markersHelper.generateGpx(mapMarkers, true);
				Exception error = GPXUtilities.writeGpx(new OutputStreamWriter(outputStream, "UTF-8"), gpxFile);
				if (error != null) {
					warnings.add(app.getString(R.string.settings_item_write_error, String.valueOf(getType())));
					SettingsHelper.LOG.error("Failed write to gpx file", error);
					return false;
				}
				return true;
			}
		};
	}

	private List<MapMarker> getMarkersFromGroups(List<MapMarkersGroup> markersGroups) {
		List<MapMarker> mapMarkers = new ArrayList<>();
		for (MapMarkersGroup group : markersGroups) {
			mapMarkers.addAll(group.getMarkers());
		}
		return mapMarkers;
	}
}