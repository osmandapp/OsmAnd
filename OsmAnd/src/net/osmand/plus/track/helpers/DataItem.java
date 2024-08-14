package net.osmand.plus.track.helpers;

import static net.osmand.gpx.GpxParameter.FILE_DIR;
import static net.osmand.gpx.GpxParameter.FILE_LAST_MODIFIED_TIME;
import static net.osmand.gpx.GpxParameter.FILE_NAME;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.gpx.GpxParameter;
import net.osmand.plus.OsmandApplication;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public abstract class DataItem {

	@NonNull
	protected final File file;
	protected final Map<GpxParameter, Object> map = new HashMap<>();

	public DataItem(@NonNull OsmandApplication app, @NonNull File file) {
		this.file = file;
		initFileParameters(app);
	}

	private void initFileParameters(@NonNull OsmandApplication app) {
		map.put(FILE_NAME, file.getName());
		map.put(FILE_DIR, GpxDbUtils.getGpxFileDir(app, file));
		map.put(FILE_LAST_MODIFIED_TIME, file.lastModified());
	}

	@NonNull
	public File getFile() {
		return file;
	}

	public boolean hasData() {
		return !map.isEmpty();
	}

	public boolean hasAppearanceData() {
		return hasData() && GpxParameter.getAppearanceParameters().stream().anyMatch(key -> map.get(key) != null);
	}

	@NonNull
	@SuppressWarnings("unchecked")
	public <T> T requireParameter(@NonNull GpxParameter parameter) {
		Object res = getParameter(parameter);
		if (res == null) {
			throw new IllegalStateException("Requested parameter '" + parameter + "' is null.");
		} else {
			return ((Class<T>) parameter.getTypeClass()).cast(res);
		}
	}

	@Nullable
	@SuppressWarnings("unchecked")
	public <T> T getParameter(@NonNull GpxParameter parameter) {
		Object value = null;
		if (map.containsKey(parameter)) {
			value = map.get(parameter);
		}
		if (value == null && !parameter.isAppearanceParameter()) {
			value = parameter.getDefaultValue();
		}
		return ((Class<T>) parameter.getTypeClass()).cast(value);
	}

	public boolean setParameter(@NonNull GpxParameter parameter, @Nullable Object value) {
		if (isValidValue(parameter, value)) {
			map.put(parameter, value);
			return true;
		}
		return false;
	}

	public boolean isValidValue(@NonNull GpxParameter parameter, @Nullable Object value) {
		return true;
	}

	@Override
	public int hashCode() {
		return file.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		DataItem other = (DataItem) obj;
		return file.equals(other.file);
	}
}