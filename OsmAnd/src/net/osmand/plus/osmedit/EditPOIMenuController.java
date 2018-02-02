package net.osmand.plus.osmedit;

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

import net.osmand.data.PointDescription;
import net.osmand.osm.PoiType;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.osmedit.OsmPoint.Action;
import net.osmand.plus.osmedit.dialogs.SendPoiDialogFragment;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.util.Algorithms;

import java.util.Map;

public class EditPOIMenuController extends MenuController {

	private OsmPoint osmPoint;
	private OsmEditingPlugin plugin;
	private String category;
	private String actionStr;

	public EditPOIMenuController(final MapActivity mapActivity, PointDescription pointDescription, OsmPoint osmPoint) {
		super(new EditPOIMenuBuilder(mapActivity, osmPoint), pointDescription, mapActivity);
		this.osmPoint = osmPoint;
		plugin = OsmandPlugin.getPlugin(OsmEditingPlugin.class);
		if (osmPoint instanceof OsmNotesPoint) {
			builder.setShowTitleIfTruncated(false);
		}

		leftTitleButtonController = new TitleButtonController() {
			@Override
			public void buttonPressed() {
				if (plugin != null) {
					SendPoiDialogFragment sendPoiDialogFragment =
							SendPoiDialogFragment.createInstance(new OsmPoint[]{getOsmPoint()}, SendPoiDialogFragment.PoiUploaderType.SIMPLE);
					sendPoiDialogFragment.show(getMapActivity().getSupportFragmentManager(), SendPoiDialogFragment.TAG);
				}
			}
		};
		leftTitleButtonController.caption = getMapActivity().getString(R.string.shared_string_upload);
		leftTitleButtonController.updateStateListDrawableIcon(R.drawable.ic_action_export, true);

		rightTitleButtonController = new TitleButtonController() {
			@Override
			public void buttonPressed() {
				AlertDialog.Builder bld = new AlertDialog.Builder(getMapActivity());
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
		rightTitleButtonController.updateStateListDrawableIcon(R.drawable.ic_action_delete_dark, true);

		category = getCategory();

		if (osmPoint.getGroup() == OsmPoint.Group.POI) {
			if(osmPoint.getAction() == Action.DELETE) {
				actionStr = getMapActivity().getString(R.string.osm_edit_deleted_poi);
			} else if(osmPoint.getAction() == Action.MODIFY) {
				actionStr = getMapActivity().getString(R.string.osm_edit_modified_poi);
			} else/* if(osmPoint.getAction() == Action.CREATE) */{
				actionStr = getMapActivity().getString(R.string.osm_edit_created_poi);
			}
		} else if (osmPoint.getGroup() == OsmPoint.Group.BUG) {
			if(osmPoint.getAction() == Action.DELETE) {
				actionStr = getMapActivity().getString(R.string.osm_edit_removed_note);
			} else if(osmPoint.getAction() == Action.MODIFY) {
				actionStr = getMapActivity().getString(R.string.osm_edit_commented_note);
			} else if(osmPoint.getAction() == Action.REOPEN) {
				actionStr = getMapActivity().getString(R.string.osm_edit_reopened_note);
			} else/* if(osmPoint.getAction() == Action.CREATE) */{
				actionStr = getMapActivity().getString(R.string.osm_edit_created_note);
			}
		} else {
			actionStr = "";
		}
	}

	@Override
	public String getTypeStr() {
		return category;
	}

	@Override
	protected void setObject(Object object) {
		if (object instanceof OsmPoint) {
			this.osmPoint = (OsmPoint) object;
		}
	}

	@Override
	protected Object getObject() {
		return osmPoint;
	}

	public OsmPoint getOsmPoint() {
		return osmPoint;
	}

	@Override
	public boolean needTypeStr() {
		return !Algorithms.isEmpty(category);
	}

	@Override
	public boolean displayDistanceDirection() {
		return true;
	}

	@Override
	public CharSequence getAdditionalInfoStr() {
		return actionStr;
	}

	@Override
	public int getAdditionalInfoColorId() {
		if (osmPoint.getAction() == Action.DELETE) {
			return R.color.color_osm_edit_delete;
		} else if (osmPoint.getAction() == Action.MODIFY || osmPoint.getAction() == Action.REOPEN) {
			return R.color.color_osm_edit_modify;
		} else {
			return R.color.color_osm_edit_create;
		}
	}

	@Override
	public int getRightIconId() {
		if (osmPoint.getGroup() == OsmPoint.Group.POI) {
			OpenstreetmapPoint osmP = (OpenstreetmapPoint) osmPoint;
			int iconResId = 0;
			String poiTranslation = osmP.getEntity().getTag(EditPoiData.POI_TYPE_TAG);
			if (poiTranslation != null) {
				Map<String, PoiType> poiTypeMap = getMapActivity().getMyApplication().getPoiTypes().getAllTranslatedNames(false);
				PoiType poiType = poiTypeMap.get(poiTranslation.toLowerCase());
				if (poiType != null) {
					String id = null;
					if (RenderingIcons.containsBigIcon(poiType.getIconKeyName())) {
						id = poiType.getIconKeyName();
					} else if (RenderingIcons.containsBigIcon(poiType.getOsmTag() + "_" + poiType.getOsmValue())) {
						id = poiType.getOsmTag() + "_" + poiType.getOsmValue();
					}
					if (id != null) {
						iconResId = RenderingIcons.getBigIconResourceId(id);
					}
				}
			}
			if (iconResId == 0) {
				iconResId = R.drawable.ic_type_info;
			}
			return iconResId;
		} else if (osmPoint.getGroup() == OsmPoint.Group.BUG) {
			return R.drawable.ic_type_bug;
		} else {
			return 0;
		}
	}

	@Override
	public int getAdditionalInfoIconRes() {
		if (osmPoint.getAction() == Action.DELETE) {
			return R.drawable.ic_action_type_delete_16;
		} else if (osmPoint.getAction() == Action.MODIFY || osmPoint.getAction() == Action.REOPEN) {
			return R.drawable.ic_action_type_edit_16;
		} else {
			return R.drawable.ic_action_type_add_16;
		}
	}

	@Override
	public boolean needStreetName() {
		return false;
	}

	private String getCategory() {
		return OsmEditingPlugin.getCategory(osmPoint, getMapActivity());
	}
}
