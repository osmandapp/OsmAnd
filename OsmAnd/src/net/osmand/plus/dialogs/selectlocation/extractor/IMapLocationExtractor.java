package net.osmand.plus.dialogs.selectlocation.extractor;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;

public interface IMapLocationExtractor<T> {
	T extractLocation(@NonNull OsmandApplication app);
}
