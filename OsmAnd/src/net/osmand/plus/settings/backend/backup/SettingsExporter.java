package net.osmand.plus.settings.backend.backup;

import net.osmand.util.Algorithms;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

class SettingsExporter {

	private Map<String, SettingsItem> items;
	private Map<String, String> additionalParams;
	private boolean exportItemsFiles;

	SettingsExporter(boolean exportItemsFiles) {
		this.exportItemsFiles = exportItemsFiles;
		items = new LinkedHashMap<>();
		additionalParams = new LinkedHashMap<>();
	}

	void addSettingsItem(SettingsItem item) throws IllegalArgumentException {
		if (items.containsKey(item.getName())) {
			throw new IllegalArgumentException("Already has such item: " + item.getName());
		}
		items.put(item.getName(), item);
	}

	void addAdditionalParam(String key, String value) {
		additionalParams.put(key, value);
	}

	void exportSettings(File file) throws JSONException, IOException {
		JSONObject json = createItemsJson();
		OutputStream os = new BufferedOutputStream(new FileOutputStream(file), SettingsHelper.BUFFER);
		ZipOutputStream zos = new ZipOutputStream(os);
		try {
			ZipEntry entry = new ZipEntry("items.json");
			zos.putNextEntry(entry);
			zos.write(json.toString(2).getBytes("UTF-8"));
			zos.closeEntry();
			if (exportItemsFiles) {
				writeItemFiles(zos);
			}
			zos.flush();
			zos.finish();
		} finally {
			Algorithms.closeStream(zos);
			Algorithms.closeStream(os);
		}
	}

	private void writeItemFiles(ZipOutputStream zos) throws IOException {
		for (SettingsItem item : items.values()) {
			SettingsItemWriter<? extends SettingsItem> writer = item.getWriter();
			if (writer != null) {
				String fileName = item.getFileName();
				if (Algorithms.isEmpty(fileName)) {
					fileName = item.getDefaultFileName();
				}
				if (item instanceof FileSettingsItem && ((FileSettingsItem) item).isSubFolders()) {
					File file = ((FileSettingsItem) item).getFile();
					zipDirsWithFiles(file, writer, zos);
				} else {
					ZipEntry entry = new ZipEntry(fileName);
					zos.putNextEntry(entry);
					writer.writeToStream(zos);
					zos.closeEntry();
				}
			}
		}
	}

	public void zipDirsWithFiles(File f, SettingsItemWriter<? extends SettingsItem> writer, ZipOutputStream zos)
			throws IOException {
		if (f == null) {
			return;
		}
		if (f.isDirectory()) {
			File[] fs = f.listFiles();
			if (fs != null) {
				for (File c : fs) {
					zipDirsWithFiles(c, writer, zos);
				}
			}
		} else {
			FileSettingsItem item = (FileSettingsItem) writer.getItem();
			String zipEntryName = Algorithms.isEmpty(item.getSubtype().getSubtypeFolder())
					? f.getName()
					: f.getPath().substring(f.getPath().indexOf(item.getSubtype().getSubtypeFolder()));
			ZipEntry entry = new ZipEntry(zipEntryName);
			zos.putNextEntry(entry);
			item.setInputStream(new FileInputStream(f));
			writer.writeToStream(zos);
			zos.closeEntry();
		}
	}

	private JSONObject createItemsJson() throws JSONException {
		JSONObject json = new JSONObject();
		json.put("version", SettingsHelper.VERSION);
		for (Map.Entry<String, String> param : additionalParams.entrySet()) {
			json.put(param.getKey(), param.getValue());
		}
		JSONArray itemsJson = new JSONArray();
		for (SettingsItem item : items.values()) {
			itemsJson.put(new JSONObject(item.toJson()));
		}
		json.put("items", itemsJson);
		return json;
	}
}
