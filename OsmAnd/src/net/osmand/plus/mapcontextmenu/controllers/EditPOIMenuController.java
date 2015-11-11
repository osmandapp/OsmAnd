package net.osmand.plus.mapcontextmenu.controllers;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;

import net.osmand.access.AccessibleAlertBuilder;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.ProgressImplementation;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.mapcontextmenu.builders.EditPOIMenuBuilder;
import net.osmand.plus.osmedit.OpenstreetmapPoint;
import net.osmand.plus.osmedit.OpenstreetmapRemoteUtil;
import net.osmand.plus.osmedit.OsmBugsRemoteUtil;
import net.osmand.plus.osmedit.OsmEditingPlugin;
import net.osmand.plus.osmedit.OsmEditsUploadListener;
import net.osmand.plus.osmedit.OsmEditsUploadListenerHelper;
import net.osmand.plus.osmedit.OsmNotesPoint;
import net.osmand.plus.osmedit.OsmPoint;
import net.osmand.plus.osmedit.UploadOpenstreetmapPointAsyncTask;
import net.osmand.plus.osmedit.dialogs.SendPoiDialogFragment;
import net.osmand.plus.osmedit.dialogs.SendPoiDialogFragment.ProgressDialogPoiUploader;
import net.osmand.util.Algorithms;

import java.util.Map;

public class EditPOIMenuController extends MenuController {

	private OsmEditingPlugin plugin;
	private String pointTypeStr;
	private ProgressDialogPoiUploader poiUploader;

	public EditPOIMenuController(OsmandApplication app, MapActivity mapActivity, PointDescription pointDescription, final OsmPoint osmPoint) {
		super(new EditPOIMenuBuilder(app, osmPoint), pointDescription, mapActivity);
		plugin = OsmandPlugin.getPlugin(OsmEditingPlugin.class);

		poiUploader = new ProgressDialogPoiUploader() {
			@Override
			public void showProgressDialog(OsmPoint[] points, boolean closeChangeSet) {
				ProgressDialog dialog = ProgressImplementation.createProgressDialog(
						getMapActivity(),
						getMapActivity().getString(R.string.uploading),
						getMapActivity().getString(R.string.local_openstreetmap_uploading),
						ProgressDialog.STYLE_HORIZONTAL).getDialog();
				OsmEditsUploadListener listener = new OsmEditsUploadListenerHelper(getMapActivity(),
						getMapActivity().getString(R.string.local_openstreetmap_were_uploaded)) {
					@Override
					public void uploadEnded(Map<OsmPoint, String> loadErrorsMap) {
						super.uploadEnded(loadErrorsMap);
						for (OsmPoint osmPoint : loadErrorsMap.keySet()) {
							if (loadErrorsMap.get(osmPoint) == null) {
								getMapActivity().getContextMenu().close();
							}
						}
					}
				};
				OpenstreetmapRemoteUtil remotepoi = new OpenstreetmapRemoteUtil(getMapActivity());
				OsmBugsRemoteUtil remotebug = new OsmBugsRemoteUtil(getMapActivity().getMyApplication());
				UploadOpenstreetmapPointAsyncTask uploadTask = new UploadOpenstreetmapPointAsyncTask(
						dialog, listener, plugin, remotepoi, remotebug, points.length, closeChangeSet);
				uploadTask.execute(points);

				dialog.show();
			}
		};

		leftTitleButtonController = new TitleButtonController() {
			@Override
			public void buttonPressed() {
				if (plugin != null) {
					SendPoiDialogFragment sendPoiDialogFragment = SendPoiDialogFragment.createInstance(new OsmPoint[]{osmPoint});
					sendPoiDialogFragment.setPoiUploader(poiUploader);
					sendPoiDialogFragment.show(getMapActivity().getSupportFragmentManager(), SendPoiDialogFragment.TAG);
				}
			}
		};
		leftTitleButtonController.caption = getMapActivity().getString(R.string.shared_string_upload);
		leftTitleButtonController.leftIconId = R.drawable.ic_action_export;

		rightTitleButtonController = new TitleButtonController() {
			@Override
			public void buttonPressed() {
				AccessibleAlertBuilder bld = new AccessibleAlertBuilder(getMapActivity());
				bld.setMessage(R.string.recording_delete_confirm);
				bld.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (plugin != null) {
							boolean deleted = false;
							if (osmPoint instanceof OsmNotesPoint) {
								deleted = plugin.getDBBug().deleteAllBugModifications((OsmNotesPoint) osmPoint);
							} else if (osmPoint instanceof OpenstreetmapPoint) {
								deleted = plugin.getDBPOI().deletePOI((OpenstreetmapPoint) osmPoint);
							}
							if (deleted) {
								getMapActivity().getContextMenu().close();
							}
						}
					}
				});
				bld.setNegativeButton(R.string.shared_string_no, null);
				bld.show();
			}
		};
		rightTitleButtonController.caption = getMapActivity().getString(R.string.shared_string_delete);
		rightTitleButtonController.leftIconId = R.drawable.ic_action_delete_dark;

		if (osmPoint.getGroup() == OsmPoint.Group.POI) {
			pointTypeStr = getMapActivity().getString(R.string.osm_edit_created_poi);
		} else if (osmPoint.getGroup() == OsmPoint.Group.BUG) {
			pointTypeStr = getMapActivity().getString(R.string.osm_edit_created_bug);
		} else {
			pointTypeStr = "";
		}
	}

	@Override
	protected int getSupportedMenuStatesPortrait() {
		return MenuState.HEADER_ONLY | MenuState.HALF_SCREEN | MenuState.FULL_SCREEN;
	}

	@Override
	public boolean needTypeStr() {
		return !Algorithms.isEmpty(pointTypeStr);
	}

	@Override
	public Drawable getLeftIcon() {
		return getIcon(R.drawable.ic_action_gabout_dark, R.color.osmand_orange_dark, R.color.osmand_orange);
	}

	@Override
	public String getTypeStr() {
		return pointTypeStr;
	}

	@Override
	public boolean needStreetName() {
		return false;
	}
}
