package net.osmand.plus.settings.backend.backup;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.MapMarkersHelper.MapMarkersGroup;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static net.osmand.IndexConstants.GPX_FILE_EXT;

public class MarkersSettingsItem extends CollectionSettingsItem<MapMarkersGroup> {

	private MapMarkersHelper markersHelper;

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

				}
				appliedItems.add(shouldReplace ? duplicate : renameItem(duplicate));
			}
		}
	}

	@Override
	public boolean isDuplicate(@NonNull MapMarkersGroup markersGroup) {
		String name = markersGroup.getName();
		for (MapMarkersGroup group : existingItems) {
			if (group.getName().equals(name)) {
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
			public void readFromStream(@NonNull InputStream inputStream, File destination) throws IOException, IllegalArgumentException {
				GPXFile gpxFile = GPXUtilities.loadGPXFile(inputStream);
				if (gpxFile.error != null) {
					warnings.add(app.getString(R.string.settings_item_read_error, String.valueOf(getType())));
					SettingsHelper.LOG.error("Failed read gpx file", gpxFile.error);
				} else {
					Map<String, MapMarkersGroup> flatGroups = new LinkedHashMap<>();
				}
			}
		};
	}

	@Nullable
	@Override
	SettingsItemWriter<MarkersSettingsItem> getWriter() {
		return new SettingsItemWriter<MarkersSettingsItem>(this) {

			@Override
			public boolean writeToStream(@NonNull OutputStream outputStream) throws IOException {
//				Exception error = GPXUtilities.writeGpx(new OutputStreamWriter(outputStream, "UTF-8"), gpxFile);
//				if (error != null) {
//					warnings.add(app.getString(R.string.settings_item_write_error, String.valueOf(getType())));
//					SettingsHelper.LOG.error("Failed write to gpx file", error);
//					return false;
//				}
				return true;
			}
		};
	}
}