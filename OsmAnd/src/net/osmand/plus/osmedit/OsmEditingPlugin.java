package net.osmand.plus.osmedit;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

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
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.TabActivity;
import net.osmand.plus.dashboard.DashboardOnMap.DashboardType;
import net.osmand.plus.dashboard.tools.DashFragmentData;
import net.osmand.plus.measurementtool.LoginBottomSheetFragment;
import net.osmand.plus.myplaces.AvailableGPXFragment;
import net.osmand.plus.myplaces.AvailableGPXFragment.GpxInfo;
import net.osmand.plus.myplaces.FavoritesActivity;
import net.osmand.plus.osmedit.OsmPoint.Action;
import net.osmand.plus.osmedit.dialogs.SendGpxBottomSheetFragment;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.settings.backend.OsmandPreference;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.fragments.BaseSettingsFragment.SettingsScreenType;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_CREATE_POI;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_OPEN_OSM_NOTE;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.OSM_EDITS;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.OSM_NOTES;
import static net.osmand.osm.edit.Entity.POI_TYPE_TAG;
import static net.osmand.plus.ContextMenuAdapter.makeDeleteAction;


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
	private OpenstreetmapsDbHelper dbpoi;
	private OsmBugsDbHelper dbbug;
	private OpenstreetmapLocalUtil localUtil;
	private OpenstreetmapRemoteUtil remoteUtil;
	private OsmBugsRemoteUtil remoteNotesUtil;
	private OsmBugsLocalUtil localNotesUtil;

	public OsmEditingPlugin(OsmandApplication app) {
		super(app);
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
	protected List<QuickActionType> getQuickActionTypes() {
		List<QuickActionType> quickActionTypes = new ArrayList<>();
		quickActionTypes.add(AddPOIAction.TYPE);
		quickActionTypes.add(AddOSMBugAction.TYPE);
		quickActionTypes.add(ShowHideOSMBugAction.TYPE);
		return quickActionTypes;
	}

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
	public SettingsScreenType getSettingsScreenType() {
		return SettingsScreenType.OPEN_STREET_MAP_EDITING;
	}

	@Override
	public String getPrefsDescription() {
		return app.getString(R.string.osm_editing_prefs_descr);
	}

	@Override
	public void registerMapContextMenuActions(final MapActivity mapActivity,
											  final double latitude,
											  final double longitude,
											  ContextMenuAdapter adapter,
											  final Object selectedObj, boolean configureMenu) {
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
					openOsmNote(mapActivity, latitude, longitude, "", false);
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
					.setId(MAP_CONTEXT_MENU_CREATE_POI)
					.setIcon(R.drawable.ic_action_edit_dark)
					.setOrder(MODIFY_POI_ITEM_ORDER)
					.setListener(listener)
					.createItem());
		} else if (selectedObj instanceof OpenstreetmapPoint && ((OpenstreetmapPoint) selectedObj).getAction() != Action.DELETE) {
			adapter.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.poi_context_menu_modify_osm_change, mapActivity)
					.setId(MAP_CONTEXT_MENU_CREATE_POI)
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
					.setId(MAP_CONTEXT_MENU_OPEN_OSM_NOTE)
					.setIcon(R.drawable.ic_action_edit_dark)
					.setOrder(MODIFY_OSM_NOTE_ITEM_ORDER)
					.setListener(listener)
					.createItem());
		} else {
			adapter.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.context_menu_item_open_note, mapActivity)
					.setId(MAP_CONTEXT_MENU_OPEN_OSM_NOTE)
					.setIcon(R.drawable.ic_action_osm_note_add)
					.setOrder(OPEN_OSM_NOTE_ITEM_ORDER)
					.setListener(listener)
					.createItem());
		}
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
		mTabs.add(favoritesActivity.getTabIndicator(OSM_EDIT_TAB, OsmEditsFragment.class));
		if (intent != null && "OSM".equals(intent.getStringExtra("TAB"))) {
			app.getSettings().FAVORITES_TAB.set(OSM_EDIT_TAB);
		}
	}

	@Override
	public void registerLayerContextMenuActions(OsmandMapTileView mapView, ContextMenuAdapter adapter, final MapActivity mapActivity) {
		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setId(OSM_NOTES)
				.setTitleId(R.string.layer_osm_bugs, mapActivity)
				.setSelected(settings.SHOW_OSM_BUGS.get())
				.setIcon(R.drawable.ic_action_osm_note)
				.setColor(app, settings.SHOW_OSM_BUGS.get() ? R.color.osmand_orange : ContextMenuItem.INVALID_ID)
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
							OsmandPreference<Boolean> showOsmBugs = settings.SHOW_OSM_BUGS;
							showOsmBugs.set(isChecked);
							adapter.getItem(pos).setColor(app, showOsmBugs.get() ?
									R.color.osmand_orange : ContextMenuItem.INVALID_ID);
							adapter.notifyDataSetChanged();
							updateLayers(mapActivity.getMapView(), mapActivity);
						}
						return true;
					}
				})
				.setItemDeleteAction(makeDeleteAction(settings.SHOW_OSM_BUGS))
				.createItem());

		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setId(OSM_EDITS)
				.setTitleId(R.string.layer_osm_edits, mapActivity)
				.setSelected(settings.SHOW_OSM_EDITS.get())
				.setIcon(R.drawable.ic_action_openstreetmap_logo)
				.setColor(app, settings.SHOW_OSM_EDITS.get() ? R.color.osmand_orange : ContextMenuItem.INVALID_ID)
				.setListener(new ContextMenuAdapter.OnRowItemClick() {
					@Override
					public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked, int[] viewCoordinates) {
						if (itemId == R.string.layer_osm_edits) {
							OsmandPreference<Boolean> showOsmEdits = settings.SHOW_OSM_EDITS;
							showOsmEdits.set(isChecked);
							adapter.getItem(pos).setColor(app, showOsmEdits.get() ? R.color.osmand_orange : ContextMenuItem.INVALID_ID);
							adapter.notifyDataSetChanged();
							updateLayers(mapActivity.getMapView(), mapActivity);
						}
						return true;
					}
				})
				.setItemDeleteAction(makeDeleteAction(settings.SHOW_OSM_EDITS))
				.createItem());
	}

	@Override
	public CharSequence getDescription() {
		return app.getString(R.string.osm_editing_plugin_description);
	}

	@Override
	public void contextMenuFragment(final FragmentActivity activity, final Fragment fragment, final Object info, ContextMenuAdapter adapter) {
		if (fragment instanceof AvailableGPXFragment) {
			adapter.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.local_index_mi_upload_gpx, activity)
					.setIcon(R.drawable.ic_action_export)
					.setListener(new ContextMenuAdapter.ItemClickListener() {

						@Override
						public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked, int[] viewCoordinates) {
							sendGPXFiles(activity, (AvailableGPXFragment) fragment, (GpxInfo) info);
							return true;
						}
					}).createItem());
		}
	}

	@Override
	public void optionsMenuFragment(final FragmentActivity activity, final Fragment fragment, ContextMenuAdapter optionsMenuAdapter) {
		if (fragment instanceof AvailableGPXFragment) {
			final AvailableGPXFragment f = ((AvailableGPXFragment) fragment);
			optionsMenuAdapter.addItem(new ContextMenuItem.ItemBuilder().setTitleId(R.string.local_index_mi_upload_gpx, activity)
					.setIcon(R.drawable.ic_action_export)
					.setColor(app, R.color.color_white)
					.setListener(new ItemClickListener() {

						@Override
						public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int itemId, int pos, boolean isChecked, int[] viewCoordinates) {
							f.openSelectionMode(R.string.local_index_mi_upload_gpx, R.drawable.ic_action_export,
									R.drawable.ic_action_export, new OnClickListener() {
										@Override
										public void onClick(DialogInterface dialog, int which) {
											List<GpxInfo> selectedItems = f.getSelectedItems();
											sendGPXFiles(activity, f,
													selectedItems.toArray(new GpxInfo[0]));
										}
									});
							return true;
						}
					})
					.createItem());
		}
	}

	public enum UploadVisibility {
		PUBLIC(R.string.gpxup_public, R.string.gpx_upload_public_visibility_descr),
		IDENTIFIABLE(R.string.gpxup_identifiable, R.string.gpx_upload_identifiable_visibility_descr),
		TRACKABLE(R.string.gpxup_trackable, R.string.gpx_upload_trackable_visibility_descr),
		PRIVATE(R.string.gpxup_private, R.string.gpx_upload_private_visibility_descr);

		@StringRes
		private final int titleId;
		@StringRes
		private final int descriptionId;

		UploadVisibility(int titleId, int descriptionId) {
			this.titleId = titleId;
			this.descriptionId = descriptionId;
		}

		public String asURLparam() {
			return name().toLowerCase();
		}

		@StringRes
		public int getTitleId() {
			return titleId;
		}

		@StringRes
		public int getDescriptionId() {
			return descriptionId;
		}
	}

	public boolean sendGPXFiles(final FragmentActivity activity, Fragment fragment, final GpxInfo... info) {
		String name = settings.OSM_USER_NAME.get();
		String pwd = settings.OSM_USER_PASSWORD.get();
		String authToken = settings.OSM_USER_ACCESS_TOKEN.get();
		if ((Algorithms.isEmpty(name) || Algorithms.isEmpty(pwd)) && Algorithms.isEmpty(authToken)) {
			LoginBottomSheetFragment.showInstance(activity.getSupportFragmentManager(), fragment);
			return false;
		} else {
			SendGpxBottomSheetFragment.showInstance(activity.getSupportFragmentManager(), fragment, info);
			return true;
		}
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
	public Drawable getAssetResourceImage() {
		return app.getUIUtilities().getIcon(R.drawable.osm_editing);
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

	public static SpannableString getTitle(OsmPoint osmPoint, Context ctx) {
		SpannableString title = new SpannableString(getName(osmPoint));
		if (TextUtils.isEmpty(title)) {
			title = SpannableString.valueOf(getCategory(osmPoint, ctx));
			title.setSpan(new StyleSpan(Typeface.ITALIC), 0, title.length(), 0);
		}
		return title;
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
			category = ((OpenstreetmapPoint) osmPoint).getEntity().getTag(POI_TYPE_TAG);
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
