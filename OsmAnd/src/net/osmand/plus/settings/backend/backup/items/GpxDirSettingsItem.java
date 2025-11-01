package net.osmand.plus.settings.backend.backup.items;

import static net.osmand.shared.gpx.GpxParameter.APPEARANCE_LAST_MODIFIED_TIME;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.backup.GpxAppearanceInfo;
import net.osmand.plus.settings.backend.backup.SettingsHelper;
import net.osmand.plus.settings.backend.backup.SettingsItemReader;
import net.osmand.plus.settings.backend.backup.SettingsItemType;
import net.osmand.plus.settings.backend.backup.SettingsItemWriter;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.plus.utils.FileUtils;
import net.osmand.shared.gpx.GpxDbHelper;
import net.osmand.shared.gpx.GpxDirItem;
import net.osmand.shared.io.KFile;
import net.osmand.util.Algorithms;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

public class GpxDirSettingsItem extends SettingsItem {

	private static final int APPROXIMATE_SIZE_BYTES = 350;

	private GpxDbHelper gpxDbHelper;

	private GpxDirItem dirItem;

	public GpxDirSettingsItem(@NonNull OsmandApplication app, @NonNull GpxDirItem dirItem) {
		super(app);
		this.dirItem = dirItem;
	}

	public GpxDirSettingsItem(@NonNull OsmandApplication app, @Nullable GpxDirSettingsItem baseItem,
			@NonNull GpxDirItem dirItem) {
		super(app, baseItem);
		this.dirItem = dirItem;
	}

	public GpxDirSettingsItem(@NonNull OsmandApplication app,
			@NonNull JSONObject json) throws JSONException {
		super(app, json);
	}

	public GpxDirItem getDirItem() {
		return dirItem;
	}

	@Override
	protected void init() {
		super.init();
		gpxDbHelper = app.getGpxDbHelper();
	}

	@NonNull
	@Override
	public SettingsItemType getType() {
		return SettingsItemType.GPX_DIR;
	}

	@NonNull
	@Override
	public String getName() {
		return FileUtils.getRelativeAppPath(app, dirItem.getFile().path());
	}

	@NonNull
	@Override
	public String getPublicName(@NonNull Context ctx) {
		return dirItem.getFile().name();
	}

	@Override
	public long getEstimatedSize() {
		return APPROXIMATE_SIZE_BYTES;
	}

	@Override
	public long getLocalModifiedTime() {
		GpxDirItem item = gpxDbHelper.getGpxDirItem(dirItem.getFile());
		Long time = item.getParameter(APPEARANCE_LAST_MODIFIED_TIME);
		return time != null ? time : 0;
	}

	@Override
	public void setLocalModifiedTime(long lastModifiedTime) {
		gpxDbHelper.updateDataItemParameter(dirItem, APPEARANCE_LAST_MODIFIED_TIME, lastModifiedTime);
	}

	@Override
	public void apply() {
		KFile file = dirItem.getFile();
		File dir = app.getAppPath(file.path());
		if (!dir.exists()) {
			dir.mkdirs();
		}
		if (gpxDbHelper.hasGpxDirItem(file)) {
			gpxDbHelper.updateDataItem(dirItem);
		} else {
			gpxDbHelper.add(dirItem);
		}
	}

	@Override
	public void delete() {
		super.delete();
		gpxDbHelper.remove(dirItem);
	}

	@Override
	void readItemsFromJson(@NonNull JSONObject json) throws IllegalArgumentException {
		if (dirItem == null && !Algorithms.isEmpty(fileName)) {
			File file = app.getAppPath(Algorithms.getFileNameWithoutExtension(fileName));
			dirItem = new GpxDirItem(SharedUtil.kFile(file));
		}
		GpxAppearanceInfo appearanceInfo = new GpxAppearanceInfo(json);
		appearanceInfo.setParameters(dirItem);
	}

	@NonNull
	@Override
	JSONObject writeItemsToJson(@NonNull JSONObject json) {
		try {
			GpxAppearanceInfo appearanceInfo = new GpxAppearanceInfo(app, dirItem);
			appearanceInfo.toJson(json);
		} catch (JSONException e) {
			warnings.add(app.getString(R.string.settings_item_write_error, String.valueOf(getType())));
			SettingsHelper.LOG.error("Failed write to json", e);
		}
		return json;
	}

	@Override
	public boolean shouldReadOnCollecting() {
		return true;
	}

	@Nullable
	@Override
	public SettingsItemReader<? extends SettingsItem> getReader() {
		return getJsonReader(true);
	}

	@Nullable
	@Override
	public SettingsItemWriter<? extends SettingsItem> getWriter() {
		return getJsonWriter();
	}
}
