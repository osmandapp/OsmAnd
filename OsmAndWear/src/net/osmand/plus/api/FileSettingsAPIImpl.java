package net.osmand.plus.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class FileSettingsAPIImpl implements SettingsAPI {

	protected File file;
	protected ConcurrentHashMap<String, Object> map = new ConcurrentHashMap<String, Object>();

	public FileSettingsAPIImpl(File file) throws IOException {
		this.file = file;
		if (file.exists()) {
			Properties props = new Properties();
			FileInputStream fis = new FileInputStream(file);
			props.load(fis);
			for (Entry<Object, Object> entry : props.entrySet()) {
				String k = entry.getKey().toString();
				map.put(k, entry.getValue());
			}
		}
	}

	@Override
	public Object getPreferenceObject(String key) {
		return key;
	}

	private String wrap(Object pref, String key) {
		return pref + "." + key;
	}

	@Override
	public SettingsEditor edit(Object pref) {
		return new SettingsEditor() {
			final Map<String, Object> modified = new LinkedHashMap<String, Object>();

			@Override
			public SettingsEditor remove(String key) {
				modified.put(wrap(pref, key), null);
				return this;
			}

			@Override
			public SettingsEditor clear() {
				modified.clear();
				return this;
			}

			@Override
			public SettingsEditor putString(String key, String value) {
				modified.put(wrap(pref, key), value);
				return this;
			}

			@Override
			public SettingsEditor putLong(String key, long value) {
				modified.put(key, value + "");
				return this;
			}

			@Override
			public SettingsEditor putInt(String key, int value) {
				modified.put(wrap(pref, key), value);
				return this;
			}

			@Override
			public SettingsEditor putFloat(String key, float value) {
				modified.put(wrap(pref, key), value);
				return this;
			}

			@Override
			public SettingsEditor putBoolean(String key, boolean value) {
				modified.put(wrap(pref, key), value);
				return this;
			}

			@Override
			public boolean commit() {
				return commitToFile(modified);
			}

		};
	}

	private boolean commitToFile(Map<String, Object> modified) {
		for (Entry<String, Object> e : modified.entrySet()) {
			if (e.getValue() == null) {
				map.remove(e.getKey());
			} else {
				map.put(e.getKey(), e.getValue());
			}
		}
		return saveFile();
	}

	public boolean saveFile() {
		try {
			Properties ps = new Properties();
			Iterator<Entry<String, Object>> it = map.entrySet().iterator();
			while (it.hasNext()) {
				Entry<String, Object> e = it.next();
				ps.put(e.getKey(), String.valueOf(e.getValue()));
			}
			FileOutputStream fout = new FileOutputStream(file);
			ps.store(fout, null);
			fout.close();
			return true;
		} catch (IOException e1) {
			e1.printStackTrace();
			return false;
		}
	}

	@Override
	public String getString(Object pref, String key, String defValue) {
		Object obj = map.get(wrap(pref, key));
		if (obj == null) {
			return defValue;
		}
		return obj.toString();
	}

	@Override
	public float getFloat(Object pref, String key, float defValue) {
		Object obj = map.get(wrap(pref, key));
		if (obj == null) {
			return defValue;
		}
		if (obj instanceof Number) {
			return ((Number) obj).floatValue();
		} else {
			try {
				float flot = Float.parseFloat(obj.toString());
				map.put(wrap(pref, key), flot);
				return flot;
			} catch (NumberFormatException e) {
				return defValue;
			}
		}
	}

	@Override
	public boolean getBoolean(Object pref, String key, boolean defValue) {
		Object obj = map.get(wrap(pref, key));
		if (obj == null) {
			return defValue;
		}
		return Boolean.parseBoolean(obj.toString());
	}

	@Override
	public int getInt(Object pref, String key, int defValue) {
		Object obj = map.get(wrap(pref, key));
		if (obj == null) {
			return defValue;
		}
		if (obj instanceof Number) {
			return ((Number) obj).intValue();
		} else {
			try {
				int num = Integer.parseInt(obj.toString());
				map.put(wrap(pref, key), num);
				return num;
			} catch (NumberFormatException e) {
				return defValue;
			}
		}
	}

	@Override
	public long getLong(Object pref, String key, long defValue) {
		Object obj = map.get(wrap(pref, key));
		if (obj == null) {
			return defValue;
		}
		if (obj instanceof Number) {
			return ((Number) obj).longValue();
		} else {
			try {
				long num = Long.parseLong(obj.toString());
				map.put(wrap(pref, key), num);
				return num;
			} catch (NumberFormatException e) {
				return defValue;
			}
		}

	}

	@Override
	public boolean contains(Object pref, String key) {
		return map.containsKey(wrap(pref, key));
	}
}
