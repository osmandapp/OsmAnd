package net.osmand.plus.track.helpers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.gpx.GpxParameter;
import net.osmand.plus.OsmandApplication;

import java.io.File;

public class GpxDirItem extends DataItem {

	public GpxDirItem(@NonNull OsmandApplication app, @NonNull File file) {
		super(app, file);
	}

	@Override
	public boolean isValidValue(@NonNull GpxParameter parameter, @Nullable Object value) {
		return parameter.isAppearanceParameter() && (value == null || parameter.getTypeClass() == value.getClass());
	}
}