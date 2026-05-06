package net.osmand.plus.settings.backend.backup.items;

import static net.osmand.IndexConstants.GPX_INDEX_DIR;
import static net.osmand.shared.gpx.GpxParameter.APPEARANCE_LAST_MODIFIED_TIME;
import static net.osmand.shared.gpx.GpxParameter.LEGACY_POINTS_GROUPS_CHECKED;
import static net.osmand.shared.gpx.GpxParameter.POINTS_GROUPS;
import static net.osmand.shared.gpx.GpxParameter.SPLIT_INTERVAL;
import static net.osmand.shared.gpx.GpxParameter.SPLIT_TYPE;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.backup.FileSettingsItemReader;
import net.osmand.plus.settings.backend.backup.GpxAppearanceInfo;
import net.osmand.plus.settings.backend.backup.SettingsHelper;
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
import net.osmand.shared.gpx.GpxUtilities;
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
		appearanceInfo = new GpxAppearanceInfo(json);
	}

	@Override
	void writeToJson(@NonNull JSONObject json) throws JSONException {
		super.writeToJson(json);
		if (appearanceInfo != null) {
			appearanceInfo.toJson(json);
		}
	}

	@Override
	public void prepareForUpload() {
		migrateLegacyPointsGroupsForUploadIfNeeded();
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
				GpxDataItem dataItem = gpxDbHelper.getItem(kSavedFile);
				if (dataItem == null) {
					dataItem = new GpxDataItem(kSavedFile);
					if (!gpxDbHelper.add(dataItem)) {
						dataItem = gpxDbHelper.getItem(kSavedFile);
					}
				}
				if (dataItem != null) {
					updateGpxParams(dataItem, savedFile);
				}
				if (appearanceInfo.hasPointsGroups()) {
					updatePointsGroups(savedFile, dataItem);
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

	private void updateGpxParams(@NonNull GpxDataItem dataItem, @NonNull File targetFile) {
		Integer splitType = appearanceInfo.splitType != null
				? GpxSplitType.getSplitTypeByTypeId(appearanceInfo.splitType).getType() : null;
		boolean splitChanged = Algorithms.objectEquals(dataItem.getParameter(SPLIT_TYPE), splitType)
				|| Algorithms.objectEquals(dataItem.getParameter(SPLIT_INTERVAL), appearanceInfo.splitInterval);

		appearanceInfo.setParameters(dataItem);

		app.getGpxDbHelper().updateDataItem(dataItem);
		app.getGpxDbHelper().updateDataItemParameter(dataItem, APPEARANCE_LAST_MODIFIED_TIME, targetFile.lastModified());

		if (splitChanged) {
			GpxSelectionHelper gpxHelper = app.getSelectedGpxHelper();
			SelectedGpxFile selectedGpxFile = gpxHelper.getSelectedFileByPath(targetFile.getAbsolutePath());
			if (selectedGpxFile != null) {
				selectedGpxFile.resetSplitProcessed();
			}
		}
	}

	private void updatePointsGroups(@NonNull File savedFile, @Nullable GpxDataItem dataItem) {
		String pointsGroups = GpxUtilities.INSTANCE.serializePointsGroups(appearanceInfo.getPointsGroups());
		if (dataItem != null) {
			app.getGpxDbHelper().updateDataItemParameter(dataItem, POINTS_GROUPS, pointsGroups);
			app.getGpxDbHelper().updateDataItemParameter(dataItem, APPEARANCE_LAST_MODIFIED_TIME, savedFile.lastModified());
		}

		GpxSelectionHelper gpxHelper = app.getSelectedGpxHelper();
		SelectedGpxFile selectedGpxFile = gpxHelper.getSelectedFileByPath(savedFile.getAbsolutePath());
		if (selectedGpxFile != null) {
			GpxUtilities.INSTANCE.applyPointsGroups(selectedGpxFile.getGpxFile(), pointsGroups);
			gpxHelper.updateSelectedGpxFile(selectedGpxFile);
		}
	}

	private void migrateLegacyPointsGroupsForUploadIfNeeded() {
		if (!file.exists() || file.isDirectory()) {
			return;
		}
		try {
			GpxDataItem dataItem = getOrCreateDataItem(file);
			if (dataItem == null) {
				return;
			}
			Boolean legacyPointsGroupsChecked = dataItem.getParameter(LEGACY_POINTS_GROUPS_CHECKED);
			if (Boolean.TRUE.equals(legacyPointsGroupsChecked)) {
				return;
			}
			KFile kFile = SharedUtil.kFile(file);
			if (!GpxUtilities.INSTANCE.hasPointsGroupsExtension(kFile)) {
				markLegacyPointsGroupsChecked(dataItem);
				return;
			}
			String pointsGroups = dataItem.getParameter(POINTS_GROUPS);
			if (Algorithms.isEmpty(pointsGroups)) {
				GpxFile rawGpxFile = GpxUtilities.INSTANCE.loadGpxFile(kFile, null, null, true, true);
				if (rawGpxFile.getError() != null) {
					SettingsHelper.LOG.error("Failed to load GPX with legacy points_groups: " + file.getAbsolutePath(),
							SharedUtil.jException(rawGpxFile.getError()));
					return;
				}
				pointsGroups = GpxUtilities.INSTANCE.serializePointsGroups(rawGpxFile.getPointsGroups());
				app.getGpxDbHelper().updateDataItemParameter(dataItem, POINTS_GROUPS, pointsGroups);
			}
			GpxFile gpxFile = SharedUtil.loadGpxFile(file);
			if (gpxFile.getError() != null) {
				SettingsHelper.LOG.error("Failed to load migrated GPX for cleanup: " + file.getAbsolutePath(),
						SharedUtil.jException(gpxFile.getError()));
				return;
			}
			long lastModified = file.lastModified();
			Exception writeError = SharedUtil.writeGpxFile(file, gpxFile);
			if (writeError != null) {
				SettingsHelper.LOG.error("Failed to rewrite GPX without legacy points_groups: " + file.getAbsolutePath(), writeError);
				return;
			}
			setSize(0);
			if (lastModified > 0) {
				file.setLastModified(lastModified);
			}
			markLegacyPointsGroupsChecked(dataItem);
			appearanceInfo = new GpxAppearanceInfo(app, dataItem);
			refreshSelectedGpxFile(file);
		} catch (Exception e) {
			SettingsHelper.LOG.error("Failed to migrate legacy points_groups before upload: " + file.getAbsolutePath(), e);
		}
	}

	private void markLegacyPointsGroupsChecked(@NonNull GpxDataItem dataItem) {
		app.getGpxDbHelper().updateDataItemParameter(dataItem, LEGACY_POINTS_GROUPS_CHECKED, true);
	}

	@Nullable
	private GpxDataItem getOrCreateDataItem(@NonNull File targetFile) {
		KFile kFile = SharedUtil.kFile(targetFile);
		GpxDbHelper gpxDbHelper = app.getGpxDbHelper();
		GpxDataItem dataItem = gpxDbHelper.getItem(kFile);
		if (dataItem == null) {
			dataItem = new GpxDataItem(kFile);
			if (!gpxDbHelper.add(dataItem)) {
				dataItem = gpxDbHelper.getItem(kFile);
			}
		}
		return dataItem;
	}

	private void refreshSelectedGpxFile(@NonNull File targetFile) {
		GpxSelectionHelper gpxHelper = app.getSelectedGpxHelper();
		SelectedGpxFile selectedGpxFile = gpxHelper.getSelectedFileByPath(targetFile.getAbsolutePath());
		if (selectedGpxFile != null) {
			GpxFile gpxFile = SharedUtil.loadGpxFile(targetFile);
			if (gpxFile.getError() != null) {
				SettingsHelper.LOG.error("Failed to refresh selected GPX after points_groups migration: " + targetFile.getAbsolutePath(),
						SharedUtil.jException(gpxFile.getError()));
				return;
			}
			selectedGpxFile.setGpxFile(gpxFile, app);
			selectedGpxFile.resetSplitProcessed();
			gpxHelper.updateSelectedGpxFile(selectedGpxFile);
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
