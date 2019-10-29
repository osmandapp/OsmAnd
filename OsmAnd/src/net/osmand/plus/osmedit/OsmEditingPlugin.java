package net.osmand.plus.osmedit;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.data.Amenity;
import net.osmand.data.MapObject;
import net.osmand.data.TransportStop;
import net.osmand.osm.PoiType;
import net.osmand.osm.edit.Entity;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.ItemClickListener;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.EnumAdapter;
import net.osmand.plus.activities.EnumAdapter.IEnumWithResource;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.TabActivity;
import net.osmand.plus.dashboard.DashboardOnMap.DashboardType;
import net.osmand.plus.dashboard.tools.DashFragmentData;
import net.osmand.plus.myplaces.AvailableGPXFragment;
import net.osmand.plus.myplaces.AvailableGPXFragment.GpxInfo;
import net.osmand.plus.myplaces.FavoritesActivity;
import net.osmand.plus.osmedit.OsmPoint.Action;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.List;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_CREATE_POI;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_MODIFY_OSM_CHANGE;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_MODIFY_OSM_NOTE;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_MODIFY_POI;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_OPEN_OSM_NOTE;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.OSM_EDITS;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.OSM_NOTES;


public class OsmEditingPlugin extends OsmandPlugin {
	private static final Log LOG = PlatformUtil.getLog(OsmEditingPlugin.class);
	public static final int OSM_EDIT_TAB = R.string.osm_edits;
	private static final String ID = "osm.editing";

	// Constants for determining the order of items in the additional actions context menu
	private static final int CREATE_POI_ITEM_ORDER = 7300;
	private static final int MODIFY_POI_ITEM_ORDER = 7300;
	private static final int MODIFY_OSM_CHANGE_ITEM_ORDER = 7300;
	private static final int OPEN_OSM_NOTE_ITEM_ORDER = 7600;
	private static final int MODIFY_OSM_NOTE_ITEM_ORDER = 7600;

	private OsmandSettings settings;
	private OsmandApplication app;
	private OpenstreetmapsDbHelper dbpoi;
	private OsmBugsDbHelper dbbug;
	private OpenstreetmapLocalUtil localUtil;
	private OpenstreetmapRemoteUtil remoteUtil;
	private OsmBugsRemoteUtil remoteNotesUtil;
	private OsmBugsLocalUtil localNotesUtil;

	public OsmEditingPlugin(OsmandApplication app) {
		this.app = app;
		settings = app.getSettings();
	}

	@Override
	public String getId() {
		return ID;
	}

	public OpenstreetmapsDbHelper getDBPOI() {
		if (dbpoi == null) {
			dbpoi = new OpenstreetmapsDbHelper(app);
		}
		return dbpoi;
	}

	public OpenstreetmapLocalUtil getPoiModificationLocalUtil() {
		if (localUtil == null) {
			localUtil = new OpenstreetmapLocalUtil(this);
		}
		return localUtil;
	}

	public OpenstreetmapRemoteUtil getPoiModificationRemoteUtil() {
		if (remoteUtil == null) {
			remoteUtil = new OpenstreetmapRemoteUtil(app);
		}
		return remoteUtil;
	}

	public OsmBugsRemoteUtil getOsmNotesRemoteUtil() {
		if (remoteNotesUtil == null) {
			remoteNotesUtil = new OsmBugsRemoteUtil(app);
		}
		return remoteNotesUtil;
	}

	public OsmBugsLocalUtil getOsmNotesLocalUtil() {
		if (localNotesUtil == null) {
			localNotesUtil = new OsmBugsLocalUtil(app, getDBBug());
		}
		return localNotesUtil;
	}


	public OsmBugsDbHelper getDBBug() {
		if (dbbug == null) {
			dbbug = new OsmBugsDbHelper(app);
		}
		return dbbug;
	}


	private OsmBugsLayer osmBugsLayer;
	private OsmEditsLayer osmEditsLayer;
//	private EditingPOIDialogProvider poiActions;

	@Override
	public void updateLayers(OsmandMapTileView mapView, MapActivity activity) {
		if (isActive()) {
			if (osmBugsLayer == null) {
				registerLayers(activity);
			}
			if (mapView.getLayers().contains(osmEditsLayer) != settings.SHOW_OSM_EDITS.get()) {
				if (settings.SHOW_OSM_EDITS.get()) {
					mapView.addLayer(osmEditsLayer, 3.5f);
				} else {
					mapView.removeLayer(osmEditsLayer);
				}
			}
			if (mapView.getLayers().contains(osmBugsLayer) != settings.SHOW_OSM_BUGS.get()) {
				if (settings.SHOW_OSM_BUGS.get()) {
					mapView.addLayer(osmBugsLayer, 2);
				} else {
					mapView.removeLayer(osmBugsLayer);
				}
			}
		} else {
			if (osmBugsLayer != null) {
				mapView.removeLayer(osmBugsLayer);
			}
			if (osmEditsLayer != null) {
				mapView.removeLayer(osmEditsLayer);
			}
		}
	}

	@Override
	public void registerLayers(@NonNull MapActivity activity) {
		osmBugsLayer = new OsmBugsLayer(activity, this);
		osmEditsLayer = new OsmEditsLayer(activity, this);
	}

	public OsmEditsLayer getOsmEditsLayer(@NonNull MapActivity activity) {
		if (osmEditsLayer == null) {
			registerLayers(activity);
		}
		return osmEditsLayer;
	}

	public OsmBugsLayer getBugsLayer(@NonNull MapActivity activity) {
		if (osmBugsLayer == null) {
			registerLayers(activity);
		}
		return osmBugsLayer;
	}

	@Override
	public Class<? extends Activity> getSettingsActivity() {
		return SettingsOsmEditingActivity.class;
	}

	@Override
	public void registerMapContextMenuActions(final MapActivity mapActivity,
											  final double latitude,
											  final double longitude,
											  ContextMenuAdapter adapter,
											  final Object selectedObj) {
		ContextMenuAdapter.ItemClickListener listener = new ContextMenuAdapter.ItemClickListener() {
			@Override
			public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int resId, int pos, boolean isChecked, int[] viewCoordinates) {
				if (resId == R.string.context_menu_item_create_poi) {
					//getPoiActions(mapActivity).showCreateDialog(latitude, longitude);
					EditPoiDialogFragment editPoiDialogFragment =
							EditPoiDialogFragment.createAddPoiInstance(latitude, longitude,
									mapActivity.getMyApplication());
					editPoiDialogFragment.show(mapActivity.getSupportFragmentManager(),
							EditPoiDialogFragment.TAG);
				} else if (resId == R.string.context_menu_item_open_note) {
					openOsmNote(mapActivity, latitude, longitude);
				} else if (resId == R.string.context_menu_item_modify_note) {
					modifyOsmNote(mapActivity, (OsmNotesPoint) selectedObj);
				} else if (resId == R.string.poi_context_menu_modify) {
					if (selectedObj instanceof TransportStop && ((TransportStop) selectedObj).getAmenity() != null) {
						EditPoiDialogFragment.showEditInstance(((TransportStop) selectedObj).getAmenity(), mapActivity);
					} else if (selectedObj instanceof MapObject) {
						EditPoiDialogFragment.showEditInstance((MapObject) selectedObj, mapActivity);
					}
				} else if (resId == R.string.poi_context_menu_modify_osm_change) {
					final Entity entity = ((OpenstreetmapPoint) selectedObj).getEntity();
					EditPoiDialogFragment.createInstance(entity, false)
							.show(mapActivity.getSupportFragmentManager(), EditPoiDialogFragment.TAG);
				}
				return true;
			}
		};
		boolean isEditable = false;
		if (selectedObj instanceof Amenity || (selectedObj instanceof TransportStop && ((TransportStop) selectedObj).getAmenity() != null)) {
			Amenity amenity;
			if (selectedObj instanceof Amenity) {
				amenity = (Amenity) selectedObj;
			} else {
				amenity = ((TransportStop) selectedObj).getAmenity();
			}
			final PoiType poiType = amenity.getType().getPoiTypeByKeyName(amenity.getSubType());
			isEditable = !amenity.getType().isWiki() && poiType !=null && !poiType.isNotEditableOsm();
		} else if (selectedObj instanceof MapObject) {
			Long objectId = ((MapObject) selectedObj).getId();
			isEditable = objectId != null && objectId > 0 && (objectId % 2 == MapObject.AMENITY_ID_RIGHT_SHIFT
					|| (objectId >> MapObject.NON_AMENITY_ID_RIGHT_SHIFT) < Integer.MAX_VALUE);
		}
		if (isEditable) {
			adapter.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.poi_context_menu_modify, mapActivity)
					.setId(MAP_CONTEXT_MENU_MODIFY_POI)
					.setIcon(R.drawable.ic_action_edit_dark)
					.setOrder(MODIFY_POI_ITEM_ORDER)
					.setListener(listener)
					.createItem());
		} else if (selectedObj instanceof OpenstreetmapPoint && ((OpenstreetmapPoint) selectedObj).getAction() != Action.DELETE) {
			adapter.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.poi_context_menu_modify_osm_change, mapActivity)
					.setId(MAP_CONTEXT_MENU_MODIFY_OSM_CHANGE)
					.setIcon(R.drawable.ic_action_edit_dark)
					.setOrder(MODIFY_OSM_CHANGE_ITEM_ORDER)
					.setListener(listener)
					.createItem());
		} else {
			adapter.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.context_menu_item_create_poi, mapActivity)
					.setId(MAP_CONTEXT_MENU_CREATE_POI)
					.setIcon(R.drawable.ic_action_plus_dark)
					.setOrder(CREATE_POI_ITEM_ORDER)
					.setListener(listener)
					.createItem());
		}
		if (selectedObj instanceof OsmNotesPoint) {
			adapter.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.context_menu_item_modify_note, mapActivity)
					.setId(MAP_CONTEXT_MENU_MODIFY_OSM_NOTE)
					.setIcon(R.drawable.ic_action_edit_dark)
					.setOrder(MODIFY_OSM_NOTE_ITEM_ORDER)
					.setListener(listener)
					.createItem());
		} else {
			adapter.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.context_menu_item_open_note, mapActivity)
					.setId(MAP_CONTEXT_MENU_OPEN_OSM_NOTE)
					.setIcon(R.drawable.ic_action_bug_dark)
					.setOrder(OPEN_OSM_NOTE_ITEM_ORDER)
					.setListener(listener)
					.createItem());
		}
	}

	public void openOsmNote(MapActivity mapActivity, double latitude, double longitude) {
		if (osmBugsLayer == null) {
            registerLayers(mapActivity);
        }
		osmBugsLayer.openBug(latitude, longitude, "");
	}

	public void openOsmNote(MapActivity mapActivity, double latitude, double longitude, String message, boolean autofill) {
		if (osmBugsLayer == null) {
			registerLayers(mapActivity);
		}
		osmBugsLayer.openBug(latitude, longitude, message, autofill);
	}

	public void modifyOsmNote(MapActivity mapActivity, OsmNotesPoint point) {
		if (osmBugsLayer == null) {
			registerLayers(mapActivity);
		}
		osmBugsLayer.modifyBug(point);
	}

	@Override
	public void addMyPlacesTab(FavoritesActivity favoritesActivity, List<TabActivity.TabItem> mTabs, Intent intent) {
		mTabs.add(favoritesActivity.getTabIndicator(R.string.osm_edits, OsmEditsFragment.class));
		if (intent != null && "OSM".equals(intent.getStringExtra("TAB"))) {
			app.getSettings().FAVORITES_TAB.set(R.string.osm_edits);
		}
	}

	@Override
	public void registerLayerContextMenuActions(OsmandMapTileView mapView, ContextMenuAdapter adapter, final MapActivity mapActivity) {
		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setId(OSM_NOTES)
				.setTitleId(R.string.layer_osm_bugs, mapActivity)
				.setSelected(settings.SHOW_OSM_BUGS.get())
				.setIcon(R.drawable.ic_action_bug_dark)
				.setColor(settings.SHOW_OSM_BUGS.get() ? R.color.osmand_orange : ContextMenuItem.INVALID_ID)
				.setSecondaryIcon(R.drawable.ic_action_additional_option)
				.setListener(new ContextMenuAdapter.OnRowItemClick() {

					@Override
					public boolean onRowItemClick(ArrayAdapter<ContextMenuItem> adapter, View view, int itemId, int position) {
						if (itemId == R.string.layer_osm_bugs) {
							mapActivity.getDashboard().setDashboardVisibility(true,
									DashboardType.OSM_NOTES, AndroidUtils.getCenterViewCoordinates(view));
							return false;
						}
						return true;
					}

					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked, int[] viewCoordinates) {
						if (itemId == R.string.layer_osm_bugs) {
							OsmandSettings.OsmandPreference<Boolean> showOsmBugs = settings.SHOW_OSM_BUGS;
							showOsmBugs.set(isChecked);
							adapter.getItem(pos).setColorRes(showOsmBugs.get() ?
									R.color.osmand_orange : ContextMenuItem.INVALID_ID);
							adapter.notifyDataSetChanged();
							updateLayers(mapActivity.getMapView(), mapActivity);
						}
						return true;
					}
				})
				.setPosition(16)
				.createItem());

		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setId(OSM_EDITS)
				.setTitleId(R.string.layer_osm_edits, mapActivity)
				.setSelected(settings.SHOW_OSM_EDITS.get())
				.setIcon(R.drawable.ic_action_openstreetmap_logo)
				.setColor(settings.SHOW_OSM_EDITS.get() ? R.color.osmand_orange : ContextMenuItem.INVALID_ID)
				.setListener(new ContextMenuAdapter.OnRowItemClick() {
					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked, int[] viewCoordinates) {
						if (itemId == R.string.layer_osm_edits) {
							OsmandSettings.OsmandPreference<Boolean> showOsmEdits = settings.SHOW_OSM_EDITS;
							showOsmEdits.set(isChecked);
							adapter.getItem(pos).setColorRes(showOsmEdits.get() ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
							adapter.notifyDataSetChanged();
							updateLayers(mapActivity.getMapView(), mapActivity);
						}
						return true;
					}
				})
				.setPosition(17)
				.createItem());
	}

	@Override
	public String getDescription() {
		return app.getString(R.string.osm_editing_plugin_description);
	}

	@Override
	public void contextMenuFragment(final Activity la, final Fragment fragment, final Object info, ContextMenuAdapter adapter) {
		if (fragment instanceof AvailableGPXFragment) {
			adapter.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.local_index_mi_upload_gpx, la)
					.setIcon(R.drawable.ic_action_export)
					.setListener(new ContextMenuAdapter.ItemClickListener() {

						@Override
						public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked, int[] viewCoordinates) {
							sendGPXFiles(la, (AvailableGPXFragment) fragment, (GpxInfo) info);
							return true;
						}
					}).createItem());
		}
	}

	@Override
	public void optionsMenuFragment(final Activity activity, final Fragment fragment, ContextMenuAdapter optionsMenuAdapter) {
		if (fragment instanceof AvailableGPXFragment) {
			final AvailableGPXFragment f = ((AvailableGPXFragment) fragment);
			optionsMenuAdapter.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.local_index_mi_upload_gpx, activity)
					.setIcon(R.drawable.ic_action_export)
					.setColor(R.color.color_white)
					.setListener(new ItemClickListener() {

						@Override
						public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked, int[] viewCoordinates) {
							f.openSelectionMode(R.string.local_index_mi_upload_gpx, R.drawable.ic_action_export,
									R.drawable.ic_action_export, new OnClickListener() {
										@Override
										public void onClick(DialogInterface dialog, int which) {
											List<GpxInfo> selectedItems = f.getSelectedItems();
											sendGPXFiles(activity, f,
													selectedItems.toArray(new GpxInfo[selectedItems.size()]));
										}
									});
							return true;
						}
					})
					.setPosition(5)
					.createItem());
		}
	}


	public enum UploadVisibility implements IEnumWithResource {
		Public(R.string.gpxup_public),
		Identifiable(R.string.gpxup_identifiable),
		Trackable(R.string.gpxup_trackable),
		Private(R.string.gpxup_private);
		private final int resourceId;

		UploadVisibility(int resourceId) {
			this.resourceId = resourceId;
		}

		public String asURLparam() {
			return name().toLowerCase();
		}

		@Override
		public int stringResource() {
			return resourceId;
		}
	}

	public boolean sendGPXFiles(final Activity la, AvailableGPXFragment f, final GpxInfo... info) {
		String name = settings.USER_NAME.get();
		String pwd = settings.USER_PASSWORD.get();
		if (Algorithms.isEmpty(name) || Algorithms.isEmpty(pwd)) {
			Toast.makeText(la, R.string.validate_gpx_upload_name_pwd, Toast.LENGTH_LONG).show();
			return false;
		}
		AlertDialog.Builder bldr = new AlertDialog.Builder(la);
		LayoutInflater inflater = (LayoutInflater) la.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		final View view = inflater.inflate(R.layout.send_gpx_osm, null);
		final EditText descr = (EditText) view.findViewById(R.id.memory_size);
		if (info.length > 0 && info[0].getFileName() != null) {
			int dt = info[0].getFileName().indexOf('.');
			descr.setText(info[0].getFileName().substring(0, dt));
		}
		final EditText tags = (EditText) view.findViewById(R.id.TagsText);
		final Spinner visibility = ((Spinner) view.findViewById(R.id.Visibility));
		EnumAdapter<UploadVisibility> adapter = new EnumAdapter<>(la, android.R.layout.simple_spinner_item, UploadVisibility.values());
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		visibility.setAdapter(adapter);
		visibility.setSelection(0);

		bldr.setView(view);
		bldr.setNegativeButton(R.string.shared_string_no, null);
		bldr.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				new UploadGPXFilesTask(la, descr.getText().toString(), tags.getText().toString(),
						(UploadVisibility) visibility.getItemAtPosition(visibility.getSelectedItemPosition())
				).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, info);
			}
		});
		bldr.show();
		return true;
	}

	@Override
	public String getName() {
		return app.getString(R.string.osm_settings);
	}

	@Override
	public int getLogoResourceId() {
		return R.drawable.ic_action_openstreetmap_logo;
	}

	@Override
	public int getAssetResourceName() {
		return R.drawable.osm_editing;
	}


	@Override
	public String getHelpFileName() {
		return "feature_articles/osm-editing-plugin.html";
	}

	public static String getEditName(OsmPoint point) {
		String prefix = getPrefix(point);
		String name = getName(point);
		if (point.getGroup() == OsmPoint.Group.POI) {
			String subtype = "";
			if (!Algorithms.isEmpty(((OpenstreetmapPoint) point).getSubtype())) {
				subtype = " (" + ((OpenstreetmapPoint) point).getSubtype() + ") ";
			}
			return prefix + subtype + name;
		} else if (point.getGroup() == OsmPoint.Group.BUG) {
			return prefix + name;
		} else {
			return prefix;
		}
	}

	public static String getName(OsmPoint point) {
		if (point.getGroup() == OsmPoint.Group.POI) {
			return ((OpenstreetmapPoint) point).getName();
		} else if (point.getGroup() == OsmPoint.Group.BUG) {
			return ((OsmNotesPoint) point).getText();
		} else {
			return "";
		}
	}

	public static String getCategory(OsmPoint osmPoint, Context context) {
		String category = "";
		if (osmPoint.getGroup() == OsmPoint.Group.POI) {
			category = ((OpenstreetmapPoint) osmPoint).getEntity().getTag(EditPoiData.POI_TYPE_TAG);
			if (Algorithms.isEmpty(category)) {
				category = context.getString(R.string.shared_string_without_name);
			}
		} else if (osmPoint.getGroup() == OsmPoint.Group.BUG) {
			category = context.getString(R.string.osn_bug_name);
		}
		return category;
	}

	public static String getPrefix(OsmPoint osmPoint) {
		return (osmPoint.getGroup() == OsmPoint.Group.POI ? "POI" : "Bug") + " id: " + osmPoint.getId() + " ";
	}

	public static String getDescription(OsmPoint osmPoint, Context context, boolean needPrefix) {
		String action = "";
		if (osmPoint.getAction() == OsmPoint.Action.CREATE) {
			action = context.getString(R.string.shared_string_added);
		} else if (osmPoint.getAction() == OsmPoint.Action.MODIFY) {
			action = context.getString(R.string.shared_string_edited);
		} else if (osmPoint.getAction() == OsmPoint.Action.DELETE) {
			action = context.getString(R.string.shared_string_deleted);
		} else if (osmPoint.getAction() == OsmPoint.Action.REOPEN) {
			action = context.getString(R.string.shared_string_edited);
		}

		String category = getCategory(osmPoint, context);

		String description = "";
		if (!Algorithms.isEmpty(action)) {
			description += action + " • ";
		}
		if (!Algorithms.isEmpty(category)) {
			description += category;
		}
		if (needPrefix) {
			String prefix = getPrefix(osmPoint);
			description += " • " + prefix;
		}

		return description;
	}

	@Override
	public DashFragmentData getCardFragment() {
		return DashOsmEditsFragment.FRAGMENT_DATA;
	}
}
