package net.osmand.plus.settings.backend.backup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IndexConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.util.Algorithms;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileSettingsItem extends StreamSettingsItem {



	public enum FileSubtype {
		UNKNOWN("", null),
		OTHER("other", ""),
		ROUTING_CONFIG("routing_config", IndexConstants.ROUTING_PROFILES_DIR),
		RENDERING_STYLE("rendering_style", IndexConstants.RENDERERS_DIR),
		WIKI_MAP("wiki_map", IndexConstants.WIKI_INDEX_DIR),
		SRTM_MAP("srtm_map", IndexConstants.SRTM_INDEX_DIR),
		OBF_MAP("obf_map", IndexConstants.MAPS_PATH),
		TILES_MAP("tiles_map", IndexConstants.TILES_INDEX_DIR),
		GPX("gpx", IndexConstants.GPX_INDEX_DIR),
		VOICE("voice", IndexConstants.VOICE_INDEX_DIR),
		TRAVEL("travel", IndexConstants.WIKIVOYAGE_INDEX_DIR),
		MULTIMEDIA_NOTES("multimedia_notes", IndexConstants.AV_INDEX_DIR);

		private String subtypeName;
		private String subtypeFolder;

		FileSubtype(String subtypeName, String subtypeFolder) {
			this.subtypeName = subtypeName;
			this.subtypeFolder = subtypeFolder;
		}

		public boolean isMap() {
			return this == OBF_MAP || this == WIKI_MAP || this == SRTM_MAP;
		}

		public String getSubtypeName() {
			return subtypeName;
		}

		public String getSubtypeFolder() {
			return subtypeFolder;
		}

		public static FileSubtype getSubtypeByName(@NonNull String name) {
			for (FileSubtype subtype : FileSubtype.values()) {
				if (name.equals(subtype.subtypeName)) {
					return subtype;
				}
			}
			return null;
		}

		public static FileSubtype getSubtypeByFileName(@NonNull String fileName) {
			String name = fileName;
			if (fileName.startsWith(File.separator)) {
				name = fileName.substring(1);
			}
			for (FileSubtype subtype : FileSubtype.values()) {
				switch (subtype) {
					case UNKNOWN:
					case OTHER:
						break;
					case SRTM_MAP:
						if (name.endsWith(IndexConstants.BINARY_SRTM_MAP_INDEX_EXT)) {
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
	private File appPath;
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

	FileSettingsItem(@NonNull OsmandApplication app, @NonNull JSONObject json) throws JSONException {
		super(app, json);
		this.appPath = app.getAppPath(null);
		if (subtype == FileSubtype.OTHER) {
			this.file = new File(appPath, name);
		} else if (subtype == FileSubtype.UNKNOWN || subtype == null) {
			throw new IllegalArgumentException("Unknown file subtype: " + getFileName());
		} else {
			String subtypeFolder = subtype.subtypeFolder;
			int nameIndex = fileName.indexOf(name);
			int folderIndex = fileName.indexOf(subtype.subtypeFolder);
			if (nameIndex != -1 && folderIndex != -1) {
				String subfolderPath = fileName.substring(folderIndex + subtype.subtypeFolder.length(), nameIndex);
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

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	@NonNull
	public File getFile() {
		return file;
	}

	@NonNull
	public FileSubtype getSubtype() {
		return subtype;
	}

	@Override
	public boolean exists() {
		return file.exists();
	}

	private File renameFile(File file) {
		int number = 0;
		String path = file.getAbsolutePath();
		while (true) {
			number++;
			String copyName = path.replaceAll(file.getName(), file.getName().replaceFirst("[.]", "_" + number + "."));
			File copyFile = new File(copyName);
			if (!copyFile.exists()) {
				return copyFile;
			}
		}
	}

	@Nullable
	@Override
	SettingsItemReader<? extends SettingsItem> getReader() {
		return new StreamSettingsItemReader(this) {
			@Override
			public void readFromStream(@NonNull InputStream inputStream) throws IOException, IllegalArgumentException {
				OutputStream output;
				File dest = FileSettingsItem.this.file;
				if (dest.exists() && !shouldReplace) {
					dest = renameFile(dest);
				}
				if (dest.getParentFile() != null && !dest.getParentFile().exists()) {
					dest.getParentFile().mkdirs();
				}
				output = new FileOutputStream(dest);
				byte[] buffer = new byte[SettingsHelper.BUFFER];
				int count;
				try {
					while ((count = inputStream.read(buffer)) != -1) {
						output.write(buffer, 0, count);
					}
					output.flush();
				} finally {
					Algorithms.closeStream(output);
				}
			}
		};
	}

	@Nullable
	@Override
	public SettingsItemWriter<? extends SettingsItem> getWriter() {
		try {
			setInputStream(new FileInputStream(file));
		} catch (FileNotFoundException e) {
			warnings.add(app.getString(R.string.settings_item_read_error, file.getName()));
			SettingsHelper.LOG.error("Failed to set input stream from file: " + file.getName(), e);
		}
		return super.getWriter();
	}
}
