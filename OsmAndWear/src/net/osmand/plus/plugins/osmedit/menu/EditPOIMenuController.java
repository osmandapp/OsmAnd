package net.osmand.plus.plugins.osmedit.menu;

import static net.osmand.osm.edit.Entity.POI_TYPE_TAG;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;

import net.osmand.data.PointDescription;
import net.osmand.osm.PoiType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.measurementtool.LoginBottomSheetFragment;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.plugins.osmedit.data.OpenstreetmapPoint;
import net.osmand.plus.plugins.osmedit.data.OsmNotesPoint;
import net.osmand.plus.plugins.osmedit.data.OsmPoint;
import net.osmand.plus.plugins.osmedit.data.OsmPoint.Action;
import net.osmand.plus.plugins.osmedit.dialogs.SendOsmNoteBottomSheetFragment;
import net.osmand.plus.plugins.osmedit.dialogs.SendPoiBottomSheetFragment;
import net.osmand.plus.plugins.osmedit.oauth.OsmOAuthAuthorizationAdapter;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.util.Algorithms;

import java.util.Map;

public class EditPOIMenuController extends MenuController {

	private final OsmEditingPlugin plugin;

	private OsmPoint osmPoint;
	private final String categoryDescr;
	private final String actionStr;

	public EditPOIMenuController(@NonNull MapActivity mapActivity, @NonNull PointDescription pointDescription, @NonNull OsmPoint osmPoint) {
		super(new EditPOIMenuBuilder(mapActivity, osmPoint), pointDescription, mapActivity);
		this.osmPoint = osmPoint;
		plugin = PluginsHelper.getPlugin(OsmEditingPlugin.class);
		if (osmPoint instanceof OsmNotesPoint) {
			builder.setShowTitleIfTruncated(false);
		}

		leftTitleButtonController = new TitleButtonController() {
			@Override
			public void buttonPressed() {
				MapActivity activity = getMapActivity();
				if (plugin != null && activity != null) {
					OsmPoint point = getOsmPoint();
					OsmandApplication app = activity.getMyApplication();
					OsmOAuthAuthorizationAdapter client = new OsmOAuthAuthorizationAdapter(app);
					boolean isLogged = client.isValidToken()
							|| !Algorithms.isEmpty(plugin.OSM_USER_NAME_OR_EMAIL.get())
							&& !Algorithms.isEmpty(plugin.OSM_USER_PASSWORD.get());

					FragmentManager fragmentManager = activity.getSupportFragmentManager();
					if (point instanceof OpenstreetmapPoint) {
						if (isLogged) {
							SendPoiBottomSheetFragment.showInstance(fragmentManager, new OsmPoint[] {point});
						} else {
							LoginBottomSheetFragment.showInstance(fragmentManager, null);
						}
					} else if (point instanceof OsmNotesPoint) {
						SendOsmNoteBottomSheetFragment.showInstance(fragmentManager, new OsmPoint[] {point});
					}
				}
			}
		};
		leftTitleButtonController.caption = mapActivity.getString(R.string.shared_string_upload);
		leftTitleButtonController.startIconId = R.drawable.ic_action_export;

		rightTitleButtonController = new TitleButtonController() {
			@Override
			public void buttonPressed() {
				MapActivity activity = getMapActivity();
				if (activity != null) {
					AlertDialog.Builder bld = new AlertDialog.Builder(activity);
					String itemName = pointDescription.getName();
					bld.setMessage(activity.getString(R.string.delete_confirmation_msg, itemName));
					bld.setPositiveButton(R.string.shared_string_yes, (dialog, which) -> {
						MapActivity a = getMapActivity();
						if (plugin != null && a != null) {
							boolean deleted = false;
							OsmPoint point = getOsmPoint();
							if (point instanceof OsmNotesPoint) {
								deleted = plugin.getDBBug().deleteAllBugModifications((OsmNotesPoint) point);
							} else if (point instanceof OpenstreetmapPoint) {
								deleted = plugin.getDBPOI().deletePOI((OpenstreetmapPoint) point);
							}
							if (deleted) {
								a.getContextMenu().close();
							}
						}
					});
					bld.setNegativeButton(R.string.shared_string_no, null);
					bld.show();
				}
			}
		};
		rightTitleButtonController.caption = mapActivity.getString(R.string.shared_string_delete);
		rightTitleButtonController.startIconId = R.drawable.ic_action_delete_dark;

		categoryDescr = getCategoryDescr();

		if (osmPoint.getGroup() == OsmPoint.Group.POI) {
			if (osmPoint.getAction() == Action.DELETE) {
				actionStr = mapActivity.getString(R.string.osm_edit_deleted_poi);
			} else if (osmPoint.getAction() == Action.MODIFY) {
				actionStr = mapActivity.getString(R.string.osm_edit_modified_poi);
			} else/* if(osmPoint.getAction() == Action.CREATE) */ {
				actionStr = mapActivity.getString(R.string.osm_edit_created_poi);
			}
		} else if (osmPoint.getGroup() == OsmPoint.Group.BUG) {
			if (osmPoint.getAction() == Action.DELETE) {
				actionStr = mapActivity.getString(R.string.osm_edit_closed_note);
			} else if (osmPoint.getAction() == Action.MODIFY) {
				actionStr = mapActivity.getString(R.string.osm_edit_commented_note);
			} else if (osmPoint.getAction() == Action.REOPEN) {
				actionStr = mapActivity.getString(R.string.osm_edit_reopened_note);
			} else/* if(osmPoint.getAction() == Action.CREATE) */ {
				actionStr = mapActivity.getString(R.string.osm_edit_created_note);
			}
		} else {
			actionStr = "";
		}
	}

	@NonNull
	@Override
	public String getTypeStr() {
		return categoryDescr;
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
		return !Algorithms.isEmpty(categoryDescr);
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
			String poiTranslation = osmP.getEntity().getTag(POI_TYPE_TAG);
			MapActivity mapActivity = getMapActivity();
			if (poiTranslation != null && mapActivity != null) {
				Map<String, PoiType> poiTypeMap = mapActivity.getMyApplication().getPoiTypes().getAllTranslatedNames(false);
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
				iconResId = R.drawable.ic_action_info_dark;
			}
			return iconResId;
		} else if (osmPoint.getGroup() == OsmPoint.Group.BUG) {
			return R.drawable.ic_action_osm_note_add;
		} else {
			return 0;
		}
	}

	@Override
	public Drawable getRightIcon() {
		int iconResId = getRightIconId();
		if (iconResId != 0) {
			return getIcon(iconResId, getAdditionalInfoColorId());
		} else {
			return null;
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

	private String getCategoryDescr() {
		return OsmEditingPlugin.getDescription(osmPoint, getMapActivity(), false);
	}
}
