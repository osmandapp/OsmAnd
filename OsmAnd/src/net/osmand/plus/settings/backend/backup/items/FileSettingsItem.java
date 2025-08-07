package net.osmand.plus.settings.backend.backup.items;

import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IndexConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.backup.BackupUtils;
import net.osmand.plus.download.SrtmDownloadItem;
import net.osmand.plus.helpers.FileNameTranslationHelper;
import net.osmand.plus.plugins.audionotes.Recording;
import net.osmand.plus.settings.backend.backup.FileSettingsItemReader;
import net.osmand.plus.settings.backend.backup.SettingsHelper;
import net.osmand.plus.settings.backend.backup.SettingsItemReader;
import net.osmand.plus.settings.backend.backup.SettingsItemType;
import net.osmand.plus.settings.backend.backup.SettingsItemWriter;
import net.osmand.plus.settings.backend.backup.StreamSettingsItemWriter;
import net.osmand.plus.utils.FileUtils;
import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class FileSettingsItem extends StreamSettingsItem {

	public enum FileSubtype {
		UNKNOWN("", null, R.drawable.ic_type_file),
		OTHER("other", "", R.drawable.ic_type_file),
		ROUTING_CONFIG("routing_config", IndexConstants.ROUTING_PROFILES_DIR, R.drawable.ic_action_route_distance),
		RENDERING_STYLE("rendering_style", IndexConstants.RENDERERS_DIR, R.drawable.ic_action_map_style),
		WIKI_MAP("wiki_map", IndexConstants.WIKI_INDEX_DIR, R.drawable.ic_plugin_wikipedia),
		SRTM_MAP("srtm_map", IndexConstants.SRTM_INDEX_DIR, R.drawable.ic_plugin_srtm),
		TERRAIN_DATA("terrain", IndexConstants.GEOTIFF_DIR, R.drawable.ic_action_terrain),
		OBF_MAP("obf_map", IndexConstants.MAPS_PATH, R.drawable.ic_map),
		TILES_MAP("tiles_map", IndexConstants.TILES_INDEX_DIR, R.drawable.ic_map),
		ROAD_MAP("road_map", IndexConstants.ROADS_INDEX_DIR, R.drawable.ic_map),
		GPX("gpx", IndexConstants.GPX_INDEX_DIR, R.drawable.ic_action_route_distance),
		TTS_VOICE("tts_voice", IndexConstants.VOICE_INDEX_DIR, R.drawable.ic_action_volume_up),
		VOICE("voice", IndexConstants.VOICE_INDEX_DIR, R.drawable.ic_action_volume_up),
		TRAVEL("travel", IndexConstants.WIKIVOYAGE_INDEX_DIR, R.drawable.ic_plugin_wikipedia),
		MULTIMEDIA_NOTES("multimedia_notes", IndexConstants.AV_INDEX_DIR, R.drawable.ic_action_photo_dark),
		NAUTICAL_DEPTH("nautical_depth", IndexConstants.NAUTICAL_INDEX_DIR, R.drawable.ic_action_nautical_depth),
		FAVORITES_BACKUP("favorites_backup", IndexConstants.BACKUP_INDEX_DIR, R.drawable.ic_action_folder_favorites),
		COLOR_PALETTE("colors_palette", IndexConstants.COLOR_PALETTE_DIR, R.drawable.ic_action_file_color_palette);

		private final String subtypeName;
		private final String subtypeFolder;
		private final int iconId;

		FileSubtype(@NonNull String subtypeName, String subtypeFolder, @DrawableRes int iconId) {
			this.subtypeName = subtypeName;
			this.subtypeFolder = subtypeFolder;
			this.iconId = iconId;
		}

		public boolean isMap() {
			return this == OBF_MAP || this == WIKI_MAP || this == TRAVEL || this == SRTM_MAP
					|| this == TERRAIN_DATA || this == TILES_MAP || this == ROAD_MAP || this == NAUTICAL_DEPTH;
		}

		@NonNull
		public String getSubtypeName() {
			return subtypeName;
		}

		public String getSubtypeFolder() {
			return subtypeFolder;
		}

		@DrawableRes
		public int getIconId() {
			return iconId;
		}

		@Nullable
		public static FileSubtype getSubtypeByName(@NonNull String name) {
			for (FileSubtype subtype : values()) {
				if (name.equals(subtype.subtypeName)) {
					return subtype;
				}
			}
			return null;
		}

		@NonNull
		public static FileSubtype getSubtypeByPath(@NonNull OsmandApplication app, @NonNull String fileName) {
			fileName = fileName.replace(app.getAppPath(null).getPath(), "");
			return getSubtypeByFileName(fileName);
		}

		@NonNull
		public static FileSubtype getSubtypeByFileName(@NonNull String fileName) {
			String name = BackupUtils.removeLeadingSlash(fileName);
			for (FileSubtype subtype : values()) {
				switch (subtype) {
					case UNKNOWN:
					case OTHER:
						break;
					case SRTM_MAP:
						if (SrtmDownloadItem.isSrtmFile(name)) {
							return subtype;
						}
						break;
					case TERRAIN_DATA:
						if (name.endsWith(IndexConstants.TIF_EXT)) {
							return subtype;
						}
						break;
					case WIKI_MAP:
						if (name.endsWith(IndexConstants.BINARY_WIKI_MAP_INDEX_EXT)) {
							return subtype;
						}
						break;
					case OBF_MAP:
						if (name.endsWith(IndexConstants.BINARY_MAP_INDEX_EXT) && !name.contains(File.separator)) {
							return subtype;
						}
						break;
					case TTS_VOICE:
						if (name.startsWith(subtype.subtypeFolder)) {
							if (name.endsWith(IndexConstants.VOICE_PROVIDER_SUFFIX)) {
								return subtype;
							} else if (name.endsWith(IndexConstants.TTSVOICE_INDEX_EXT_JS)) {
								int lastPathDelimiter = name.lastIndexOf('/');
								if (lastPathDelimiter != -1 && name.substring(0, lastPathDelimiter).endsWith(IndexConstants.VOICE_PROVIDER_SUFFIX)) {
									return subtype;
								}
							}
						}
						break;
					case NAUTICAL_DEPTH:
						if (name.endsWith(IndexConstants.BINARY_DEPTH_MAP_INDEX_EXT)) {
							return subtype;
						}
						break;
					case COLOR_PALETTE:
						if (name.endsWith(IndexConstants.TXT_EXT)) {
							return subtype;
						}
						break;
					default:
						if (name.startsWith(subtype.subtypeFolder)) {
							return subtype;
						}
						break;
				}
			}
			return UNKNOWN;
		}

		@NonNull
		@Override
		public String toString() {
			return subtypeName;
		}
	}

	protected File file;
	protected File fileToWrite;
	private final File appPath;
	protected FileSubtype subtype;
	private long size;

	public FileSettingsItem(@NonNull OsmandApplication app, @NonNull File file) throws IllegalArgumentException {
		super(app, file.getPath().replace(app.getAppPath(null).getPath(), ""));
		this.file = file;
		this.appPath = app.getAppPath(null);
		String fileName = getFileName();
		if (fileName != null) {
			this.subtype = FileSubtype.getSubtypeByFileName(fileName);
		}
		if (subtype == FileSubtype.UNKNOWN || subtype == null) {
			throw new IllegalArgumentException("Unknown file subtype: " + fileName);
		}
	}

	public FileSettingsItem(@NonNull OsmandApplication app, @NonNull JSONObject json) throws JSONException {
		super(app, json);
		this.appPath = app.getAppPath(null);
		if (subtype == FileSubtype.OTHER) {
			this.file = new File(appPath, name);
		} else if (subtype == FileSubtype.UNKNOWN || subtype == null) {
			throw new IllegalArgumentException("Unknown file subtype: " + getFileName());
		} else {
			String subtypeFolder = subtype.getSubtypeFolder();
			int nameIndex = fileName.indexOf(name);
			int folderIndex = fileName.indexOf(subtype.getSubtypeFolder());
			if (nameIndex != -1 && folderIndex != -1) {
				String subfolderPath = fileName.substring(folderIndex + subtype.getSubtypeFolder().length(), nameIndex);
				subtypeFolder = subtypeFolder + subfolderPath;
			}
			this.file = new File(app.getAppPath(subtypeFolder), name);
		}
	}

	@NonNull
	@Override
	public SettingsItemType getType() {
		return SettingsItemType.FILE;
	}

	public void setFileToWrite(@NonNull File file) throws IOException {
		fileToWrite = file;
		setInputStream(new FileInputStream(file));
	}

	@NonNull
	@Override
	public String getPublicName(@NonNull Context ctx) {
		if (subtype.isMap() || subtype == FileSubtype.TTS_VOICE || subtype == FileSubtype.VOICE) {
			return FileNameTranslationHelper.getFileNameWithRegion(app, file.getName());
		} else if (subtype == FileSubtype.MULTIMEDIA_NOTES) {
			if (file.exists()) {
				return new Recording(file).getName(app, true);
			} else {
				return Recording.getNameForMultimediaFile(app, file.getName(), getLastModifiedTime());
			}
		}
		return super.getPublicName(ctx);
	}

	@Override
	public long getLocalModifiedTime() {
		if (subtype == FileSubtype.VOICE) {
			return new File(file, file.getName() + "_" + IndexConstants.TTSVOICE_INDEX_EXT_JS).lastModified();
		} else if (subtype == FileSubtype.TTS_VOICE) {
			String langName = file.getName().replace(IndexConstants.VOICE_PROVIDER_SUFFIX, "");
			return new File(file, langName + "_" + IndexConstants.TTSVOICE_INDEX_EXT_JS).lastModified();
		} else {
			return file.lastModified();
		}
	}

	@Override
	public void setLocalModifiedTime(long lastModifiedTime) {
		if (subtype == FileSubtype.VOICE) {
			File jsFile = new File(file, file.getName() + "_" + IndexConstants.TTSVOICE_INDEX_EXT_JS);
			if (jsFile.exists()) {
				jsFile.setLastModified(lastModifiedTime);
			}
		} else if (subtype == FileSubtype.TTS_VOICE) {
			String langName = file.getName().replace(IndexConstants.VOICE_PROVIDER_SUFFIX, "");
			File jsFile = new File(file, langName + "_" + IndexConstants.TTSVOICE_INDEX_EXT_JS);
			if (jsFile.exists()) {
				jsFile.setLastModified(lastModifiedTime);
			}
		} else {
			file.setLastModified(lastModifiedTime);
		}
	}

	@Override
	public boolean needMd5Digest() {
		return CollectionUtils.equalsToAny(subtype, FileSubtype.TTS_VOICE, FileSubtype.VOICE, FileSubtype.GPX);
	}

	public File getPluginPath() {
		String pluginId = getPluginId();
		if (!Algorithms.isEmpty(pluginId)) {
			return new File(appPath, IndexConstants.PLUGINS_DIR + pluginId);
		}
		return appPath;
	}

	@Override
	void readFromJson(@NonNull JSONObject json) throws JSONException {
		super.readFromJson(json);
		String fileName = getFileName();
		if (subtype == null) {
			String subtypeStr = json.has("subtype") ? json.getString("subtype") : null;
			if (!Algorithms.isEmpty(subtypeStr)) {
				subtype = FileSubtype.getSubtypeByName(subtypeStr);
			} else if (!Algorithms.isEmpty(fileName)) {
				subtype = FileSubtype.getSubtypeByFileName(fileName);
			} else {
				subtype = FileSubtype.UNKNOWN;
			}
		}
		if (!Algorithms.isEmpty(fileName)) {
			if (subtype == FileSubtype.OTHER) {
				name = fileName;
			} else if (subtype != null && subtype != FileSubtype.UNKNOWN) {
				name = Algorithms.getFileWithoutDirs(fileName);
			}
		}
	}

	@Override
	void writeToJson(@NonNull JSONObject json) throws JSONException {
		super.writeToJson(json);
		if (subtype != null) {
			json.put("subtype", subtype.getSubtypeName());
		}
	}

	@Override
	public long getSize() {
		if (fileToWrite != null) {
			return fileToWrite.length();
		}
		if (size != 0) {
			return size;
		} else if (file != null) {
			if (file.isDirectory()) {
				List<File> filesToUpload = new ArrayList<>();
				FileUtils.collectFiles(file, filesToUpload, false);

				for (File file : filesToUpload) {
					size += file.length();
				}
			} else {
				size = file.length();
			}
		}
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	@NonNull
	public File getFile() {
		return file;
	}

	@Nullable
	public File getFileToWrite() {
		return fileToWrite;
	}

	@NonNull
	public FileSubtype getSubtype() {
		return subtype;
	}

	@Override
	public boolean exists() {
		return file.exists();
	}

	public File renameFile(File oldFile) {
		String oldPath = oldFile.getAbsolutePath();
		String prefix;
		if (file.isDirectory()) {
			prefix = file.getAbsolutePath();
		} else if (oldPath.endsWith(IndexConstants.BINARY_WIKI_MAP_INDEX_EXT)) {
			prefix = oldPath.substring(0, oldPath.lastIndexOf(IndexConstants.BINARY_WIKI_MAP_INDEX_EXT));
		} else if (oldPath.endsWith(IndexConstants.BINARY_SRTM_MAP_INDEX_EXT)) {
			prefix = oldPath.substring(0, oldPath.lastIndexOf(IndexConstants.BINARY_SRTM_MAP_INDEX_EXT));
		} else if (oldPath.endsWith(IndexConstants.BINARY_SRTM_FEET_MAP_INDEX_EXT)) {
			prefix = oldPath.substring(0, oldPath.lastIndexOf(IndexConstants.BINARY_SRTM_FEET_MAP_INDEX_EXT));
		} else if (oldPath.endsWith(IndexConstants.BINARY_ROAD_MAP_INDEX_EXT)) {
			prefix = oldPath.substring(0, oldPath.lastIndexOf(IndexConstants.BINARY_ROAD_MAP_INDEX_EXT));
		} else if (oldPath.contains(".")) {
			prefix = oldPath.substring(0, oldPath.lastIndexOf("."));
		} else {
			prefix = oldPath;
		}
		String suffix = oldPath.replace(prefix, "");
		int number = 0;
		while (true) {
			number++;
			String newName = prefix + "_" + number + suffix;
			File newFile = new File(newName);
			if (!newFile.exists()) {
				return newFile;
			}
		}
	}

	@Override
	public void delete() {
		super.delete();
		Algorithms.removeAllFiles(file);
	}

	@Nullable
	@Override
	public SettingsItemReader<? extends SettingsItem> getReader() {
		return new FileSettingsItemReader(this);
	}

	@Nullable
	@Override
	public SettingsItemWriter<? extends SettingsItem> getWriter() {
		if (!file.isDirectory()) {
			try {
				setInputStream(new FileInputStream(file));
			} catch (FileNotFoundException e) {
				warnings.add(app.getString(R.string.settings_item_read_error, file.getName()));
				SettingsHelper.LOG.error("Failed to set input stream from file: " + file.getName(), e);
			}
		}
		return new StreamSettingsItemWriter(this);
	}
}
