package net.osmand.plus.plugins.osmedit.asynctasks;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.CallbackWithObject;
import net.osmand.data.MapObject;
import net.osmand.osm.edit.Entity;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.plugins.osmedit.data.OpenstreetmapPoint;
import net.osmand.plus.plugins.osmedit.data.OsmPoint.Action;
import net.osmand.plus.plugins.osmedit.helpers.OpenstreetmapUtil;

import java.util.List;

public class LoadEntityTask extends AsyncTask<Void, Void, Entity> {

	private final OsmEditingPlugin plugin = PluginsHelper.requirePlugin(OsmEditingPlugin.class);
	private final OsmandApplication app;
	private final MapObject mapObject;
	private final CallbackWithObject<Entity> callback;

	public LoadEntityTask(@NonNull OsmandApplication app, @NonNull MapObject mapObject,
			@Nullable CallbackWithObject<Entity> callback) {
		this.app = app;
		this.mapObject = mapObject;
		this.callback = callback;
	}

	@Override
	protected Entity doInBackground(Void... params) {
		boolean available = app.getSettings().isInternetConnectionAvailable(true);
		OpenstreetmapUtil osmUtil = available ? plugin.getPoiModificationRemoteUtil()
				: plugin.getPoiModificationLocalUtil();

		return osmUtil.loadEntity(mapObject);
	}

	@Override
	protected void onPostExecute(Entity entity) {
		if (entity != null) {
			Entity existingEntity = getExistingOsmEditEntity(entity.getId());
			entity = existingEntity != null ? existingEntity : entity;
		}
		if (callback != null) {
			callback.processResult(entity);
		}
	}

	@Nullable
	private Entity getExistingOsmEditEntity(long entityId) {
		List<OpenstreetmapPoint> osmEdits = plugin.getDBPOI().getOpenstreetmapPoints();
		for (OpenstreetmapPoint osmEdit : osmEdits) {
			if (osmEdit.getId() == entityId && osmEdit.getAction() == Action.MODIFY) {
				return osmEdit.getEntity();
			}
		}
		return null;
	}
}