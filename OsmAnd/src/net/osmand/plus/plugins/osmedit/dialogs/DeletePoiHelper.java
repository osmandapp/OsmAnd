package net.osmand.plus.plugins.osmedit.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.osm.edit.Entity;
import net.osmand.osm.edit.Node;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.plugins.osmedit.data.OpenstreetmapPoint;
import net.osmand.plus.plugins.osmedit.data.OsmPoint;
import net.osmand.plus.plugins.osmedit.data.OsmPoint.Action;
import net.osmand.plus.plugins.osmedit.helpers.OpenstreetmapLocalUtil;
import net.osmand.plus.plugins.osmedit.helpers.OpenstreetmapUtil;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;

import java.util.List;

public class DeletePoiHelper {

	private final OpenstreetmapUtil openstreetmapUtil;
	private final AppCompatActivity activity;
	private DeletePoiCallback callback;

	public void setCallback(DeletePoiCallback callback) {
		this.callback = callback;
	}

	DeletePoiHelper(AppCompatActivity activity) {
		this.activity = activity;
		OsmandSettings settings = ((OsmandApplication) activity.getApplication()).getSettings();
		OsmEditingPlugin plugin = PluginsHelper.getPlugin(OsmEditingPlugin.class);
		if (plugin.OFFLINE_EDITION.get() || !settings.isInternetConnectionAvailable(true)) {
			openstreetmapUtil = plugin.getPoiModificationLocalUtil();
		} else {
			openstreetmapUtil = plugin.getPoiModificationRemoteUtil();
		}
	}

	public void deletePoiWithDialog(Amenity amenity) {
		OsmAndTaskManager.executeTask(new AsyncTask<Amenity, Void, Entity>() {

			@Override
			protected Entity doInBackground(Amenity... params) {
				return openstreetmapUtil.loadEntity(params[0]);
			}

			@Override
			protected void onPostExecute(Entity entity) {
				deletePoiWithDialog(entity);
			}
		}, amenity);
	}

	void deletePoiWithDialog(Entity entity) {
		OsmandApplication app = AndroidUtils.getApp(activity);
		boolean nightMode = app.getDaynightHelper().isNightMode(ThemeUsageContext.OVER_MAP);
		Context themedContext = UiUtilities.getThemedContext(activity, nightMode);
		if (entity == null) {
			app.showToastMessage(R.string.poi_cannot_be_found);
			return;
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(themedContext);
		builder.setTitle(R.string.poi_remove_title);
		EditText comment;
		CheckBox closeChangesetCheckBox;
		boolean isLocalEdit = openstreetmapUtil instanceof OpenstreetmapLocalUtil;
		if (isLocalEdit) {
			closeChangesetCheckBox = null;
			comment = null;
		} else {
			LinearLayout ll = new LinearLayout(themedContext);
			ll.setPadding(16, 2, 16, 0);
			ll.setOrientation(LinearLayout.VERTICAL);
			closeChangesetCheckBox = new CheckBox(themedContext);
			closeChangesetCheckBox.setText(R.string.close_changeset);
			ll.addView(closeChangesetCheckBox);
			comment = new EditText(themedContext);
			comment.setText(R.string.poi_remove_title);
			ll.addView(comment);
			builder.setView(ll);
		}
		builder.setNegativeButton(R.string.shared_string_cancel, null);
		builder.setPositiveButton(isLocalEdit ? R.string.shared_string_save : R.string.shared_string_delete, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (entity instanceof Node) {
					String c = comment == null ? null : comment.getText().toString();
					boolean closeChangeSet = closeChangesetCheckBox != null
							&& closeChangesetCheckBox.isChecked();
					deleteNode(entity, c, closeChangeSet);
				}
			}
		});
		builder.create().show();
	}

	private void deleteNode(Entity entity, String c, boolean closeChangeSet) {
		boolean isLocalEdit = openstreetmapUtil instanceof OpenstreetmapLocalUtil;
		EditPoiDialogFragment.commitEntity(Action.DELETE, entity, openstreetmapUtil.getEntityInfo(entity.getId()), c, closeChangeSet,
				result -> {
					if (result != null) {
						OsmEditingPlugin plugin = PluginsHelper.getPlugin(OsmEditingPlugin.class);
						if (plugin != null && isLocalEdit) {
							List<OpenstreetmapPoint> points = plugin.getDBPOI().getOpenstreetmapPoints();
							if (activity instanceof MapActivity && points.size() > 0) {
								OsmPoint point = points.get(points.size() - 1);
								MapActivity mapActivity = (MapActivity) activity;
								mapActivity.getContextMenu().showOrUpdate(
										new LatLon(point.getLatitude(), point.getLongitude()),
										plugin.getOsmEditsLayer(mapActivity).getObjectName(point), point);
								mapActivity.getMapLayers().getContextMenuLayer().updateContextMenu();
							}
						} else {
							AndroidUtils.getApp(activity).showToastMessage(R.string.poi_remove_success);
						}
						if (activity instanceof MapActivity) {
							((MapActivity) activity).getMapView().refreshMap(true);
						}
						if (callback != null) {
							callback.poiDeleted();
						}
					}
					return false;
				}, activity, openstreetmapUtil, null);
	}

	public interface DeletePoiCallback {
		void poiDeleted();
	}
}
