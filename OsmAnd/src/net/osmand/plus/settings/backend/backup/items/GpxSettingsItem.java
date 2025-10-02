package net.osmand.plus.settings.backend.backup.items;

import static net.osmand.IndexConstants.GPX_INDEX_DIR;
import static net.osmand.shared.gpx.GpxParameter.*;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.backup.FileSettingsItemReader;
import net.osmand.plus.settings.backend.backup.GpxAppearanceInfo;
import net.osmand.plus.settings.backend.backup.SettingsItemReader;
import net.osmand.plus.settings.backend.backup.SettingsItemType;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.plus.track.GpxSelectionParams;
import net.osmand.plus.track.GpxSplitType;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.utils.FileUtils;
import net.osmand.shared.gpx.GpxDataItem;
import net.osmand.shared.gpx.GpxDbHelper;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.GpxHelper;
import net.osmand.shared.io.KFile;
import net.osmand.util.Algorithms;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class GpxSettingsItem extends FileSettingsItem {

	private GpxAppearanceInfo appearanceInfo;

	public GpxSettingsItem(@NonNull OsmandApplication app, @NonNull File file) throws IllegalArgumentException {
		super(app, file);
		createGpxAppearanceInfo();
	}

	public GpxSettingsItem(@NonNull OsmandApplication app, @NonNull JSONObject json) throws JSONException {
		super(app, json);
		updateFile();
	}

	@Nullable
	public GpxAppearanceInfo getAppearanceInfo() {
		return appearanceInfo;
	}

	@NonNull
	@Override
	public SettingsItemType getType() {
		return SettingsItemType.GPX;
	}

	@NonNull
	@Override
	public String getPublicName(@NonNull Context ctx) {
		return GpxHelper.INSTANCE.getGpxTitle(file.getName());
	}

	@Override
	void readFromJson(@NonNull JSONObject json) throws JSONException {
		subtype = FileSubtype.GPX;
		super.readFromJson(json);
		appearanceInfo = GpxAppearanceInfo.fromJson(json);
	}

	@Override
	void writeToJson(@NonNull JSONObject json) throws JSONException {
		super.writeToJson(json);
		if (appearanceInfo != null) {
			appearanceInfo.toJson(json);
		}
	}

	@Override
	public void applyAdditionalParams(@Nullable SettingsItemReader<? extends SettingsItem> reader) {
		if (appearanceInfo != null) {
			File savedFile = null;
			if (reader instanceof FileSettingsItemReader) {
				savedFile = ((FileSettingsItemReader) reader).getSavedFile();
			}
			if (savedFile != null) {
				KFile kSavedFile = SharedUtil.kFile(savedFile);
				GpxDbHelper gpxDbHelper = app.getGpxDbHelper();
				boolean readItem = gpxDbHelper.hasGpxDataItem(kSavedFile);
				GpxDataItem dataItem = null;
				if (!readItem) {
					dataItem = new GpxDataItem(SharedUtil.kFile(savedFile));
					readItem = !gpxDbHelper.add(dataItem);
				}
				if (readItem) {
					dataItem = gpxDbHelper.getItem(kSavedFile, this::updateGpxParams);
				}
				if (dataItem != null) {
					updateGpxParams(dataItem);
				}
			}
		}
	}

	@Override
	public void delete() {
		if (FileUtils.removeGpxFile(app, file)) {
			File dir = file.getParentFile();
			File gpxDir = app.getAppPath(GPX_INDEX_DIR);
			if (dir != null && !dir.equals(gpxDir)) {
				dir.delete();
			}
		}
		super.delete();
	}

	private void updateGpxParams(@NonNull GpxDataItem dataItem) {
		int splitType = GpxSplitType.getSplitTypeByTypeId(appearanceInfo.splitType).getType();
		boolean splitChanged = Algorithms.objectEquals(dataItem.getParameter(SPLIT_TYPE), splitType)
				|| Algorithms.objectEquals(dataItem.getParameter(SPLIT_INTERVAL), appearanceInfo.splitInterval);

		dataItem.setParameter(COLOR, appearanceInfo.color);
		dataItem.setParameter(WIDTH, appearanceInfo.width);
		dataItem.setParameter(SHOW_ARROWS, appearanceInfo.showArrows);
		dataItem.setParameter(SHOW_START_FINISH, appearanceInfo.showStartFinish);
		dataItem.setParameter(SPLIT_TYPE, GpxSplitType.getSplitTypeByTypeId(appearanceInfo.splitType).getType());
		dataItem.setParameter(SPLIT_INTERVAL, appearanceInfo.splitInterval);
		dataItem.setParameter(COLORING_TYPE, appearanceInfo.coloringType);
		dataItem.setParameter(COLOR_PALETTE, appearanceInfo.gradientPaletteName);

		app.getGpxDbHelper().updateDataItem(dataItem);
		app.getGpxDbHelper().updateDataItemParameter(dataItem, APPEARANCE_LAST_MODIFIED_TIME, file.lastModified());

		if (splitChanged) {
			GpxSelectionHelper gpxHelper = app.getSelectedGpxHelper();
			SelectedGpxFile selectedGpxFile = gpxHelper.getSelectedFileByPath(file.getAbsolutePath());
			if (selectedGpxFile != null) {
				selectedGpxFile.resetSplitProcessed();
			}
		}
	}

	private void createGpxAppearanceInfo() {
		GpxDataItem dataItem = app.getGpxDbHelper().getItem(SharedUtil.kFile(file), item -> appearanceInfo = new GpxAppearanceInfo(app, item));
		if (dataItem != null) {
			appearanceInfo = new GpxAppearanceInfo(app, dataItem);
		}
	}

	private void updateFile() {
		String subtypeFolder = subtype.getSubtypeFolder();
		if (fileName.contains(name) && !(fileName.startsWith(subtypeFolder)
				|| fileName.contains(File.separator + subtypeFolder))) {
			this.file = new File(app.getAppPath(subtypeFolder), fileName);
		}
	}

	@Override
	public long getInfoModifiedTime() {
		GpxDataItem dataItem = app.getGpxDbHelper().getItem(SharedUtil.kFile(file));
		return dataItem != null ? dataItem.getParameter(APPEARANCE_LAST_MODIFIED_TIME) : 0;
	}

	@Nullable
	@Override
	public SettingsItemReader<? extends SettingsItem> getReader() {
		return new FileSettingsItemReader(this) {
			@Override
			public File readFromStream(@NonNull InputStream inputStream, @Nullable File inputFile, @Nullable String entryName) throws IOException, IllegalArgumentException {
				super.readFromStream(inputStream, inputFile, entryName);

				GpxSelectionHelper gpxHelper = app.getSelectedGpxHelper();
				SelectedGpxFile selectedGpxFile = gpxHelper.getSelectedFileByPath(file.getAbsolutePath());
				if (selectedGpxFile != null) {
					GpxFile gpxFile = SharedUtil.loadGpxFile(file);
					GpxSelectionParams params = GpxSelectionParams.newInstance()
							.showOnMap().syncGroup().setSelectedByUser(selectedGpxFile.selectedByUser);
					gpxHelper.selectGpxFile(gpxFile, params);
				}
				GpxDbHelper gpxDbHelper = app.getGpxDbHelper();
				KFile kFile = SharedUtil.kFile(file);
				if (!gpxDbHelper.hasGpxDataItem(kFile)) {
					gpxDbHelper.add(new GpxDataItem(kFile));
				}
				return file;
			}
		};
	}
}
