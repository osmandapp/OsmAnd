package net.osmand.plus.settings.backend.backup.items;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.backup.SettingsItemType;
import net.osmand.plus.settings.backend.backup.SettingsItemWriter;
import net.osmand.util.Algorithms;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

public class ResourcesSettingsItem extends FileSettingsItem {

	public ResourcesSettingsItem(@NonNull OsmandApplication app, @NonNull JSONObject json) throws JSONException {
		super(app, json);
		shouldReplace = true;
		String fileName = getFileName();
		if (!Algorithms.isEmpty(fileName) && !fileName.endsWith(File.separator)) {
			this.fileName = fileName + File.separator;
		}
	}

	@NonNull
	@Override
	public SettingsItemType getType() {
		return SettingsItemType.RESOURCES;
	}

	@NonNull
	@Override
	public String getPublicName(@NonNull Context ctx) {
		return ctx.getString(R.string.shared_string_resources);
	}

	@Override
	void readFromJson(@NonNull JSONObject json) throws JSONException {
		subtype = FileSubtype.OTHER;
		super.readFromJson(json);
	}

	@Override
	void writeToJson(@NonNull JSONObject json) throws JSONException {
		super.writeToJson(json);
		String fileName = getFileName();
		if (!Algorithms.isEmpty(fileName)) {
			if (fileName.endsWith(File.separator)) {
				fileName = fileName.substring(0, fileName.length() - 1);
			}
			json.put("file", fileName);
		}
	}

	@Override
	public boolean applyFileName(@NonNull String fileName) {
		if (fileName.endsWith(File.separator)) {
			return false;
		}
		String itemFileName = getFileName();
		if (itemFileName != null && itemFileName.endsWith(File.separator)) {
			if (fileName.startsWith(itemFileName)) {
				this.file = new File(getPluginPath(), fileName);
				return true;
			} else {
				return false;
			}
		} else {
			return super.applyFileName(fileName);
		}
	}

	@Nullable
	@Override
	public SettingsItemWriter<? extends SettingsItem> getWriter() {
		return null;
	}
}
