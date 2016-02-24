package net.osmand.plus.osmedit;

import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;

import net.osmand.access.AccessibleAlertBuilder;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.osmedit.OsmPoint.Action;
import net.osmand.plus.osmedit.dialogs.SendPoiDialogFragment;
import net.osmand.plus.osmedit.dialogs.SendPoiDialogFragment.ProgressDialogPoiUploader;
import net.osmand.util.Algorithms;

public class EditPOIMenuController extends MenuController {

	private OsmPoint osmPoint;
	private OsmEditingPlugin plugin;
	private String pointTypeStr;
	private ProgressDialogPoiUploader poiUploader;

	public EditPOIMenuController(OsmandApplication app, final MapActivity mapActivity, PointDescription pointDescription, OsmPoint osmPoint) {
		super(new EditPOIMenuBuilder(app, osmPoint), pointDescription, mapActivity);
		this.osmPoint = osmPoint;
		plugin = OsmandPlugin.getPlugin(OsmEditingPlugin.class);

		poiUploader = new SendPoiDialogFragment.SimpleProgressDialogPoiUploader() {
			@NonNull
			@Override
			protected MapActivity getMapActivity() {
				return mapActivity;
			}
		};

		leftTitleButtonController = new TitleButtonController() {
			@Override
			public void buttonPressed() {
				if (plugin != null) {
					SendPoiDialogFragment sendPoiDialogFragment = SendPoiDialogFragment.createInstance(new OsmPoint[]{getOsmPoint()});
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
							OsmPoint point = getOsmPoint();
							if (point instanceof OsmNotesPoint) {
								deleted = plugin.getDBBug().deleteAllBugModifications((OsmNotesPoint) point);
							} else if (point instanceof OpenstreetmapPoint) {
								deleted = plugin.getDBPOI().deletePOI((OpenstreetmapPoint) point);
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
			if(osmPoint.getAction() == Action.DELETE) {
				pointTypeStr = getMapActivity().getString(R.string.osm_edit_deleted_poi);
			} else if(osmPoint.getAction() == Action.MODIFY) {
				pointTypeStr = getMapActivity().getString(R.string.osm_edit_modified_poi);
			} else/* if(osmPoint.getAction() == Action.CREATE) */{
				pointTypeStr = getMapActivity().getString(R.string.osm_edit_created_poi);
			}
			
		} else if (osmPoint.getGroup() == OsmPoint.Group.BUG) {
			if(osmPoint.getAction() == Action.DELETE) {
				pointTypeStr = getMapActivity().getString(R.string.osm_edit_removed_note);
			} else if(osmPoint.getAction() == Action.MODIFY) {
				pointTypeStr = getMapActivity().getString(R.string.osm_edit_commented_note);
			} else if(osmPoint.getAction() == Action.REOPEN) {
				pointTypeStr = getMapActivity().getString(R.string.osm_edit_reopened_note);
			} else/* if(osmPoint.getAction() == Action.CREATE) */{
				pointTypeStr = getMapActivity().getString(R.string.osm_edit_created_note);
			}
		} else {
			pointTypeStr = "";
		}
	}

	@Override
	protected void setObject(Object object) {
		if (object instanceof OsmPoint) {
			this.osmPoint = (OsmPoint) object;
		}
	}

	public OsmPoint getOsmPoint() {
		return osmPoint;
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
		return getIcon(R.drawable.ic_action_gabout_dark, R.color.created_poi_icon_color);
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
