package net.osmand.plus.mapcontextmenu.controllers;

import android.app.ProgressDialog;
import android.graphics.drawable.Drawable;

import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.ProgressImplementation;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.mapcontextmenu.builders.EditPOIMenuBuilder;
import net.osmand.plus.osmedit.OpenstreetmapRemoteUtil;
import net.osmand.plus.osmedit.OsmBugsRemoteUtil;
import net.osmand.plus.osmedit.OsmEditingPlugin;
import net.osmand.plus.osmedit.OsmEditsUploadListener;
import net.osmand.plus.osmedit.OsmEditsUploadListenerHelper;
import net.osmand.plus.osmedit.OsmPoint;
import net.osmand.plus.osmedit.UploadOpenstreetmapPointAsyncTask;
import net.osmand.plus.osmedit.dialogs.SendPoiDialogFragment;
import net.osmand.plus.osmedit.dialogs.SendPoiDialogFragment.ProgressDialogPoiUploader;
import net.osmand.util.Algorithms;

import java.util.Map;

public class EditPOIMenuController extends MenuController {

	private PointDescription pointDescription;
	private OsmEditingPlugin plugin;
	private String pointTypeStr;
	private ProgressDialogPoiUploader poiUploader;

	public EditPOIMenuController(OsmandApplication app, final MapActivity mapActivity, final PointDescription pointDescription, final OsmPoint osmPoint) {
		super(new EditPOIMenuBuilder(app, osmPoint), mapActivity);
		this.pointDescription = pointDescription;
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

		titleButtonController = new TitleButtonController() {
			@Override
			public void buttonPressed() {
				if (plugin != null) {
					SendPoiDialogFragment sendPoiDialogFragment = SendPoiDialogFragment.createInstance(new OsmPoint[]{osmPoint});
					sendPoiDialogFragment.setPoiUploader(poiUploader);
					sendPoiDialogFragment.show(mapActivity.getSupportFragmentManager(), SendPoiDialogFragment.TAG);
				}
			}
		};
		titleButtonController.caption = getMapActivity().getString(R.string.local_openstreetmap_upload);

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
	public String getNameStr() {
		return pointDescription.getSimpleName(getMapActivity(), false);
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
