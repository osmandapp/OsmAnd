package net.osmand.plus.settings.backend.backup.items;

import static net.osmand.IndexConstants.GPX_FILE_EXT;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.shared.SharedUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.mapmarkers.ItineraryDataHelper;
import net.osmand.plus.mapmarkers.ItineraryDataHelper.ItineraryGroupInfo;
import net.osmand.plus.mapmarkers.ItineraryDataHelperKt;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.mapmarkers.MapMarkersGroup;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.plus.settings.backend.backup.SettingsHelper;
import net.osmand.plus.settings.backend.backup.SettingsItemReader;
import net.osmand.plus.settings.backend.backup.SettingsItemType;
import net.osmand.plus.settings.backend.backup.SettingsItemWriter;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.GpxUtilities.GpxExtensionsReader;
import net.osmand.util.Algorithms;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ItinerarySettingsItem extends CollectionSettingsItem<MapMarkersGroup> {

	private static final int APPROXIMATE_ITINERARY_SIZE_BYTES = 285;

	private MapMarkersHelper markersHelper;
	private ItineraryDataHelper dataHelper;

	public ItinerarySettingsItem(@NonNull OsmandApplication app, @NonNull List<MapMarkersGroup> items) {
		super(app, null, items);
	}

	public ItinerarySettingsItem(@NonNull OsmandApplication app, @Nullable ItinerarySettingsItem baseItem, @NonNull List<MapMarkersGroup> items) {
		super(app, baseItem, items);
	}

	public ItinerarySettingsItem(@NonNull OsmandApplication app, @NonNull JSONObject json) throws JSONException {
		super(app, json);
	}

	@Override
	protected void init() {
		super.init();
		markersHelper = app.getMapMarkersHelper();
		dataHelper = markersHelper.getDataHelper();
		existingItems = new ArrayList<>(markersHelper.getVisibleMapMarkersGroups());
	}

	@NonNull
	@Override
	public SettingsItemType getType() {
		return SettingsItemType.ITINERARY_GROUPS;
	}

	@NonNull
	@Override
	public String getName() {
		return "itinerary";
	}

	@NonNull
	@Override
	public String getPublicName(@NonNull Context ctx) {
		return ctx.getString(R.string.shared_string_itinerary);
	}

	@NonNull
	public String getDefaultFileExtension() {
		return GPX_FILE_EXT;
	}

	@Override
	public long getLocalModifiedTime() {
		return markersHelper.getDataHelper().getLastModifiedTime();
	}

	@Override
	public void setLocalModifiedTime(long lastModifiedTime) {
		markersHelper.getDataHelper().setLastModifiedTime(lastModifiedTime);
	}

	@Override
	public void apply() {
		List<MapMarkersGroup> newItems = getNewItems();
		if (!newItems.isEmpty() || !duplicateItems.isEmpty()) {
			appliedItems = new ArrayList<>(newItems);

			for (MapMarkersGroup duplicate : duplicateItems) {
				if (shouldReplace) {
					MapMarkersGroup existingGroup = markersHelper.getMapMarkerGroupById(duplicate.getId(), duplicate.getType());
					markersHelper.removeMarkersGroup(existingGroup);
				}
				appliedItems.add(shouldReplace ? duplicate : renameItem(duplicate));
			}

			for (MapMarkersGroup markersGroup : appliedItems) {
				markersHelper.enableGroup(markersGroup);
			}
			markersHelper.syncAllGroups();
		}
	}

	@Override
	public void delete() {
		super.delete();
		markersHelper.syncAllGroups();
	}

	@Override
	protected void deleteItem(MapMarkersGroup item) {
		markersHelper.removeMarkersGroup(item);
	}

	@Override
	public boolean isDuplicate(@NonNull MapMarkersGroup markersGroup) {
		for (MapMarkersGroup group : existingItems) {
			if (group.getType() == markersGroup.getType()
					&& Algorithms.objectEquals(group.getId(), markersGroup.getId())) {
				return true;
			}
		}
		return false;
	}

	@NonNull
	@Override
	public MapMarkersGroup renameItem(@NonNull MapMarkersGroup item) {
		return item;
	}

	@Override
	public long getEstimatedItemSize(@NonNull MapMarkersGroup item) {
		return APPROXIMATE_ITINERARY_SIZE_BYTES;
	}

	@Nullable
	@Override
	public SettingsItemReader<ItinerarySettingsItem> getReader() {
		return new SettingsItemReader<ItinerarySettingsItem>(this) {

			@Override
			public void readFromStream(@NonNull InputStream inputStream, @Nullable File inputFile,
			                           @Nullable String entryName) throws IllegalArgumentException {
				List<ItineraryGroupInfo> groupInfos = new ArrayList<>();
				GpxExtensionsReader gpxExtensionsReader = ItineraryDataHelperKt.getGpxExtensionsReader(groupInfos);
				GpxFile gpxFile = SharedUtil.loadGpxFile(inputStream, gpxExtensionsReader, false);
				if (gpxFile.getError() != null) {
					warnings.add(app.getString(R.string.settings_item_read_error, String.valueOf(getType())));
					SettingsHelper.LOG.error("Failed read gpx file", SharedUtil.jException(gpxFile.getError()));
				} else {
					Map<String, MapMarker> markers = new LinkedHashMap<>();
					Map<String, MapMarkersGroup> groups = new LinkedHashMap<>();
					dataHelper.collectMarkersGroups(gpxFile, groups, groupInfos, markers);
					items.addAll(groups.values());
				}
			}
		};
	}

	@Nullable
	@Override
	public SettingsItemWriter<? extends SettingsItem> getWriter() {
		GpxFile gpxFile = dataHelper.generateGpx(items, null);
		return getGpxWriter(gpxFile);
	}
}