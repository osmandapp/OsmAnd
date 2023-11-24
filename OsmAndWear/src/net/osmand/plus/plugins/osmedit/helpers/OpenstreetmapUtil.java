package net.osmand.plus.plugins.osmedit.helpers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.MapObject;
import net.osmand.osm.edit.Entity;
import net.osmand.osm.edit.EntityInfo;
import net.osmand.plus.plugins.osmedit.data.OsmPoint.Action;

import java.util.Set;

public interface OpenstreetmapUtil {

	default EntityInfo getEntityInfo(long id) {
		return null;
	}

	default void closeChangeSet() {

	}

	Entity commitEntityImpl(@NonNull Action action, Entity entity, EntityInfo info, String comment,
	                        boolean closeChangeSet, @Nullable Set<String> changedTags);

	Entity loadEntity(@NonNull MapObject mapObject);
}