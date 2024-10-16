package net.osmand.plus.plugins.osmedit;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_CREATE_POI;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_OPEN_OSM_NOTE;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.OPEN_STREET_MAP_CATEGORY_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.OPEN_STREET_MAP_ITEMS_ID_SCHEME;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.OSM_EDITS;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.OSM_NOTES;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.PLUGIN_OSMAND_EDITING;
import static net.osmand.osm.edit.Entity.POI_TYPE_TAG;
import static net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem.INVALID_ID;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.NativeLibrary.RenderedObject;
import net.osmand.PlatformUtil;
import net.osmand.binary.ObfConstants;
import net.osmand.plus.plugins.osmedit.quickactions.ShowHideOSMEditsAction;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.data.Amenity;
import net.osmand.data.MapObject;
import net.osmand.data.TransportStop;
import net.osmand.osm.PoiType;
import net.osmand.osm.edit.Entity;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.TabActivity;
import net.osmand.plus.base.SelectionBottomSheet.DialogStateListener;
import net.osmand.plus.base.SelectionBottomSheet.SelectableItem;
import net.osmand.plus.configmap.ConfigureMapMenu;
import net.osmand.shared.gpx.TrackItem;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.dashboard.DashboardOnMap.DashboardType;
import net.osmand.plus.dashboard.tools.DashFragmentData;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.mapcontextmenu.controllers.AmenityMenuController;
import net.osmand.plus.mapcontextmenu.controllers.RenderedObjectMenuController;
import net.osmand.plus.measurementtool.LoginBottomSheetFragment;
import net.osmand.plus.myplaces.MyPlacesActivity;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.osmedit.data.OpenstreetmapPoint;
import net.osmand.plus.plugins.osmedit.data.OsmNotesPoint;
import net.osmand.plus.plugins.osmedit.data.OsmPoint;
import net.osmand.plus.plugins.osmedit.data.OsmPoint.Action;
import net.osmand.plus.plugins.osmedit.dialogs.EditPoiDialogFragment;
import net.osmand.plus.plugins.osmedit.dialogs.SendGpxBottomSheetFragment;
import net.osmand.plus.plugins.osmedit.dialogs.UploadMultipleGPXBottomSheet;
import net.osmand.plus.plugins.osmedit.fragments.DashOsmEditsFragment;
import net.osmand.plus.plugins.osmedit.fragments.OsmEditsFragment;
import net.osmand.plus.plugins.osmedit.helpers.OpenstreetmapLocalUtil;
import net.osmand.plus.plugins.osmedit.helpers.OpenstreetmapRemoteUtil;
import net.osmand.plus.plugins.osmedit.helpers.OpenstreetmapsDbHelper;
import net.osmand.plus.plugins.osmedit.helpers.OsmBugsDbHelper;
import net.osmand.plus.plugins.osmedit.helpers.OsmBugsLocalUtil;
import net.osmand.plus.plugins.osmedit.helpers.OsmBugsRemoteUtil;
import net.osmand.plus.plugins.osmedit.quickactions.AddOSMBugAction;
import net.osmand.plus.plugins.osmedit.quickactions.AddPOIAction;
import net.osmand.plus.plugins.osmedit.quickactions.ShowHideOSMBugAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.settings.fragments.SettingsScreenType;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.callback.ItemClickListener;
import net.osmand.plus.widgets.ctxmenu.callback.OnDataChangeUiAdapter;
import net.osmand.plus.widgets.ctxmenu.callback.OnRowItemClick;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;
import net.osmand.plus.widgets.popup.PopUpMenuItem;
import net.osmand.plus.widgets.popup.PopUpMenuItem.Builder;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.shared.io.KFile;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;


public class OsmEditingPlugin extends OsmandPlugin {

	private static final Log LOG = PlatformUtil.getLog(OsmEditingPlugin.class);
	public static final int OSM_EDIT_TAB = R.string.osm_edits;
	public static final String OSM_MAPPER_PREFIX = "OSMMapper";
	public static final String RENDERING_CATEGORY_OSM_ASSISTANT = "osm_assistant";

	public final OsmandPreference<String> OSM_USER_NAME_OR_EMAIL;
	public final OsmandPreference<String> OSM_USER_DISPLAY_NAME;
	public final CommonPreference<UploadVisibility> OSM_UPLOAD_VISIBILITY;

	public final OsmandPreference<String> USER_OSM_BUG_NAME;
	public final OsmandPreference<String> OSM_USER_PASSWORD;
	public final OsmandPreference<String> OSM_USER_ACCESS_TOKEN;
	public final OsmandPreference<String> OSM_USER_ACCESS_TOKEN_SECRET;

	public final OsmandPreference<Boolean> OFFLINE_EDITION;
	public final OsmandPreference<Boolean> OSM_USE_DEV_URL;

	public final OsmandPreference<Boolean> SHOW_OSM_BUGS;
	public final OsmandPreference<Boolean> SHOW_OSM_EDITS;
	public final CommonPreference<Boolean> SHOW_CLOSED_OSM_BUGS;
	public final CommonPreference<Integer> SHOW_OSM_BUGS_MIN_ZOOM;

	// Constants for determining the order of items in the additional actions context menu
	private static final int CREATE_POI_ITEM_ORDER = 7300;
	private static final int MODIFY_POI_ITEM_ORDER = 7300;
	private static final int MODIFY_OSM_CHANGE_ITEM_ORDER = 7300;
	private static final int OPEN_OSM_NOTE_ITEM_ORDER = 7600;
	private static final int MODIFY_OSM_NOTE_ITEM_ORDER = 7600;

	private OpenstreetmapsDbHelper dbpoi;
	private OsmBugsDbHelper dbbug;
	private OpenstreetmapLocalUtil localUtil;
	private OpenstreetmapRemoteUtil remoteUtil;
	private OsmBugsRemoteUtil remoteNotesUtil;
	private OsmBugsLocalUtil localNotesUtil;

	private OsmBugsLayer osmBugsLayer;
	private OsmEditsLayer osmEditsLayer;

	public OsmEditingPlugin(OsmandApplication app) {
		super(app);

		OSM_USER_NAME_OR_EMAIL = registerStringPreference("user_name", "").makeGlobal().makeShared();
		OSM_USER_DISPLAY_NAME = registerStringPreference("user_display_name", "").makeGlobal().makeShared();
		OSM_UPLOAD_VISIBILITY = registerEnumStringPreference("upload_visibility", UploadVisibility.PUBLIC, UploadVisibility.values(), UploadVisibility.class).makeGlobal().makeShared();

		USER_OSM_BUG_NAME = registerStringPreference("user_osm_bug_name", "NoName/OsmAnd").makeGlobal().makeShared();
		OSM_USER_PASSWORD = registerStringPreference("user_password", "").makeGlobal().makeShared();
		OSM_USER_ACCESS_TOKEN = registerStringPreference("user_access_token", "").makeGlobal();
		OSM_USER_ACCESS_TOKEN_SECRET = registerStringPreference("user_access_token_secret", "").makeGlobal();

		OFFLINE_EDITION = registerBooleanPreference("offline_osm_editing", true).makeGlobal().makeShared();
		OSM_USE_DEV_URL = registerBooleanPreference("use_dev_url", false).makeGlobal().makeShared();

		SHOW_OSM_BUGS = registerBooleanPreference("show_osm_bugs", false).makeProfile().cache();
		SHOW_OSM_EDITS = registerBooleanPreference("show_osm_edits", true).makeProfile().cache();
		SHOW_CLOSED_OSM_BUGS = registerBooleanPreference("show_closed_osm_bugs", false).makeProfile().cache();
		SHOW_OSM_BUGS_MIN_ZOOM = registerIntPreference("show_osm_bugs_min_zoom", 8).makeProfile().cache();
	}

	@Override
	public String getId() {
		return PLUGIN_OSMAND_EDITING;
	}

	@NonNull
	public OpenstreetmapsDbHelper getDBPOI() {
		if (dbpoi == null) {
			dbpoi = new OpenstreetmapsDbHelper(app);
		}
		return dbpoi;
	}

	@NonNull
	public OpenstreetmapLocalUtil getPoiModificationLocalUtil() {
		if (localUtil == null) {
			localUtil = new OpenstreetmapLocalUtil(this);
		}
		return localUtil;
	}

	@NonNull
	public OpenstreetmapRemoteUtil getPoiModificationRemoteUtil() {
		if (remoteUtil == null) {
			remoteUtil = new OpenstreetmapRemoteUtil(app);
		}
		return remoteUtil;
	}

	@NonNull
	public OsmBugsRemoteUtil getOsmNotesRemoteUtil() {
		if (remoteNotesUtil == null) {
			remoteNotesUtil = new OsmBugsRemoteUtil(app);
		}
		return remoteNotesUtil;
	}

	@NonNull
	public OsmBugsLocalUtil getOsmNotesLocalUtil() {
		if (localNotesUtil == null) {
			localNotesUtil = new OsmBugsLocalUtil(app, getDBBug());
		}
		return localNotesUtil;
	}

	public String getOsmUrl() {
		String osmUrl;
		if (OSM_USE_DEV_URL.get()) {
			osmUrl = "https://master.apis.dev.openstreetmap.org/";
		} else {
			osmUrl = "https://api.openstreetmap.org/";
		}
		return osmUrl;
	}

	@NonNull
	public OsmBugsDbHelper getDBBug() {
		if (dbbug == null) {
			dbbug = new OsmBugsDbHelper(app);
		}
		return dbbug;
	}

	@Override
	protected List<QuickActionType> getQuickActionTypes() {
		List<QuickActionType> quickActionTypes = new ArrayList<>();
		quickActionTypes.add(AddPOIAction.TYPE);
		quickActionTypes.add(AddOSMBugAction.TYPE);
		quickActionTypes.add(ShowHideOSMBugAction.TYPE);
		quickActionTypes.add(ShowHideOSMEditsAction.TYPE);
		return quickActionTypes;
	}

	@Override
	public void updateLayers(@NonNull Context context, @Nullable MapActivity mapActivity) {
		OsmandMapTileView mapView = app.getOsmandMap().getMapView();
		if (isActive()) {
			if (osmBugsLayer == null) {
				registerLayers(context, mapActivity);
			}
			if (mapView.getLayers().contains(osmEditsLayer) != SHOW_OSM_EDITS.get()) {
				if (SHOW_OSM_EDITS.get()) {
					mapView.addLayer(osmEditsLayer, 3.5f);
				} else {
					mapView.removeLayer(osmEditsLayer);
				}
			}
			if (mapView.getLayers().contains(osmBugsLayer) != SHOW_OSM_BUGS.get()) {
				if (SHOW_OSM_BUGS.get()) {
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
	public void registerLayers(@NonNull Context context, @Nullable MapActivity mapActivity) {
		if (osmBugsLayer != null) {
			app.getOsmandMap().getMapView().removeLayer(osmBugsLayer);
		}
		if (osmEditsLayer != null) {
			app.getOsmandMap().getMapView().removeLayer(osmEditsLayer);
		}
		osmBugsLayer = new OsmBugsLayer(context, this);
		osmEditsLayer = new OsmEditsLayer(context, this);
	}

	public OsmEditsLayer getOsmEditsLayer(@NonNull MapActivity mapActivity) {
		if (osmEditsLayer == null) {
			registerLayers(mapActivity, mapActivity);
		}
		return osmEditsLayer;
	}

	public OsmBugsLayer getBugsLayer(@NonNull MapActivity mapActivity) {
		if (osmBugsLayer == null) {
			registerLayers(mapActivity, mapActivity);
		}
		return osmBugsLayer;
	}

	@Nullable
	@Override
	public SettingsScreenType getSettingsScreenType() {
		return SettingsScreenType.OPEN_STREET_MAP_EDITING;
	}

	@Override
	public String getPrefsDescription() {
		return app.getString(R.string.osm_editing_prefs_descr);
	}

	@Override
	public void registerMapContextMenuActions(@NonNull MapActivity mapActivity,
	                                          double latitude,
	                                          double longitude,
	                                          ContextMenuAdapter adapter,
	                                          Object selectedObj, boolean configureMenu) {
		ItemClickListener listener = (uiAdapter, view, item, isChecked) -> {
			int resId = item.getTitleId();
			if (resId == R.string.context_menu_item_create_poi) {
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
				Entity entity = ((OpenstreetmapPoint) selectedObj).getEntity();
				EditPoiDialogFragment.createInstance(entity, false)
						.show(mapActivity.getSupportFragmentManager(), EditPoiDialogFragment.TAG);
			}
			return true;
		};
		boolean isEditable = false;
		if (selectedObj instanceof Amenity || (selectedObj instanceof TransportStop && ((TransportStop) selectedObj).getAmenity() != null)) {
			Amenity amenity;
			if (selectedObj instanceof Amenity) {
				amenity = (Amenity) selectedObj;
			} else {
				amenity = ((TransportStop) selectedObj).getAmenity();
			}
			PoiType poiType = amenity.getType().getPoiTypeByKeyName(amenity.getSubType());
			isEditable = !amenity.getType().isWiki() && poiType != null && !poiType.isNotEditableOsm();
		} else if (selectedObj instanceof MapObject) {
			isEditable = ObfConstants.isOsmUrlAvailable((MapObject) selectedObj);
		}
		if (isEditable) {
			adapter.addItem(new ContextMenuItem(MAP_CONTEXT_MENU_CREATE_POI)
					.setTitleId(R.string.poi_context_menu_modify, mapActivity)
					.setIcon(R.drawable.ic_action_edit_dark)
					.setOrder(MODIFY_POI_ITEM_ORDER)
					.setListener(listener));
		} else if (selectedObj instanceof OpenstreetmapPoint && ((OpenstreetmapPoint) selectedObj).getAction() != Action.DELETE) {
			adapter.addItem(new ContextMenuItem(MAP_CONTEXT_MENU_CREATE_POI)
					.setTitleId(R.string.poi_context_menu_modify_osm_change, mapActivity)
					.setIcon(R.drawable.ic_action_edit_dark)
					.setOrder(MODIFY_OSM_CHANGE_ITEM_ORDER)
					.setListener(listener));
		} else {
			adapter.addItem(new ContextMenuItem(MAP_CONTEXT_MENU_CREATE_POI)
					.setTitleId(R.string.context_menu_item_create_poi, mapActivity)
					.setIcon(R.drawable.ic_action_plus_dark)
					.setOrder(CREATE_POI_ITEM_ORDER)
					.setListener(listener));
		}
		if (selectedObj instanceof OsmNotesPoint) {
			adapter.addItem(new ContextMenuItem(MAP_CONTEXT_MENU_OPEN_OSM_NOTE)
					.setTitleId(R.string.context_menu_item_modify_note, mapActivity)
					.setIcon(R.drawable.ic_action_edit_dark)
					.setOrder(MODIFY_OSM_NOTE_ITEM_ORDER)
					.setListener(listener));
		} else {
			adapter.addItem(new ContextMenuItem(MAP_CONTEXT_MENU_OPEN_OSM_NOTE)
					.setTitleId(R.string.context_menu_item_open_note, mapActivity)
					.setIcon(R.drawable.ic_action_osm_note_add)
					.setOrder(OPEN_OSM_NOTE_ITEM_ORDER)
					.setListener(listener));
		}
	}

	public void openOsmNote(@NonNull MapActivity mapActivity, double latitude, double longitude,
	                        String message, boolean autofill) {
		if (osmBugsLayer == null) {
			registerLayers(mapActivity, mapActivity);
		}
		osmBugsLayer.openBug(mapActivity, latitude, longitude, message, autofill);
	}

	public void modifyOsmNote(@NonNull MapActivity mapActivity, OsmNotesPoint point) {
		if (osmBugsLayer == null) {
			registerLayers(mapActivity, mapActivity);
		}
		osmBugsLayer.modifyBug(mapActivity, point);
	}

	@Override
	public void addMyPlacesTab(MyPlacesActivity myPlacesActivity, List<TabActivity.TabItem> mTabs, Intent intent) {
		mTabs.add(myPlacesActivity.getTabIndicator(OSM_EDIT_TAB, OsmEditsFragment.class));
		if (intent != null && "OSM".equals(intent.getStringExtra("TAB"))) {
			app.getSettings().FAVORITES_TAB.set(OSM_EDIT_TAB);
		}
	}

	@Override
	protected void registerConfigureMapCategoryActions(@NonNull ContextMenuAdapter adapter,
	                                                   @NonNull MapActivity mapActivity,
	                                                   @NonNull List<RenderingRuleProperty> customRules) {
		adapter.addItem(new ContextMenuItem(OPEN_STREET_MAP_CATEGORY_ID)
				.setCategory(true)
				.setTitleId(R.string.shared_string_open_street_map, mapActivity)
				.setLayout(R.layout.list_group_title_with_switch));

		adapter.addItem(new ContextMenuItem(OSM_NOTES)
				.setTitleId(R.string.layer_osm_bugs, mapActivity)
				.setSelected(SHOW_OSM_BUGS.get())
				.setIcon(R.drawable.ic_action_osm_note)
				.setColor(app, SHOW_OSM_BUGS.get() ? R.color.osmand_orange : INVALID_ID)
				.setSecondaryIcon(R.drawable.ic_action_additional_option)
				.setListener(new OnRowItemClick() {

					@Override
					public boolean onRowItemClick(@NonNull OnDataChangeUiAdapter uiAdapter,
					                              @NonNull View view, @NotNull ContextMenuItem item) {
						if (item.getTitleId() == R.string.layer_osm_bugs) {
							DashboardOnMap dashboard = mapActivity.getDashboard();
							dashboard.setDashboardVisibility(true, DashboardType.OSM_NOTES, AndroidUtils.getCenterViewCoordinates(view));
							return false;
						}
						return true;
					}

					@Override
					public boolean onContextMenuClick(OnDataChangeUiAdapter uiAdapter,
					                                  @Nullable View view, @NotNull ContextMenuItem item,
					                                  boolean isChecked) {
						if (item.getTitleId() == R.string.layer_osm_bugs) {
							SHOW_OSM_BUGS.set(isChecked);
							item.setColor(app, SHOW_OSM_BUGS.get() ? R.color.osmand_orange : INVALID_ID);
							uiAdapter.onDataSetChanged();
							updateLayers(mapActivity, mapActivity);
						}
						return true;
					}
				})
				.setItemDeleteAction(SHOW_OSM_BUGS));

		adapter.addItem(new ContextMenuItem(OSM_EDITS)
				.setTitleId(R.string.layer_osm_edits, mapActivity)
				.setSelected(SHOW_OSM_EDITS.get())
				.setIcon(R.drawable.ic_action_openstreetmap_logo)
				.setColor(app, SHOW_OSM_EDITS.get() ? R.color.osmand_orange : INVALID_ID)
				.setListener(new OnRowItemClick() {
					@Override
					public boolean onContextMenuClick(@Nullable OnDataChangeUiAdapter uiAdapter, @Nullable View view, @NotNull ContextMenuItem item, boolean isChecked) {
						OsmandPreference<Boolean> showOsmEdits = SHOW_OSM_EDITS;
						showOsmEdits.set(isChecked);
						item.setColor(app, showOsmEdits.get() ? R.color.osmand_orange : INVALID_ID);
						uiAdapter.onDataSetChanged();
						updateLayers(mapActivity, mapActivity);
						return true;
					}
				})
				.setItemDeleteAction(SHOW_OSM_EDITS));

		boolean nightMode = app.getDaynightHelper().isNightModeForMapControls();
		Iterator<RenderingRuleProperty> iterator = customRules.iterator();
		while (iterator.hasNext()) {
			RenderingRuleProperty property = iterator.next();
			if (RENDERING_CATEGORY_OSM_ASSISTANT.equals(property.getCategory())) {
				iterator.remove();
				String id = OPEN_STREET_MAP_ITEMS_ID_SCHEME + property.getAttrName();
				adapter.addItem(ConfigureMapMenu.createRenderingProperty(adapter, mapActivity, INVALID_ID, property, id, nightMode));
			}
		}
	}

	@Nullable
	@Override
	protected String getRenderPropertyPrefix() {
		return OSM_MAPPER_PREFIX;
	}

	@Override
	public CharSequence getDescription(boolean linksEnabled) {
		String docsUrl = app.getString(R.string.docs_plugin_osm);
		String description = app.getString(R.string.osm_editing_plugin_description, docsUrl);
		return linksEnabled ? UiUtilities.createUrlSpannable(description, docsUrl) : description;
	}

	@Override
	public void optionsMenuFragment(FragmentActivity activity, Fragment fragment, Set<TrackItem> selectedItems, List<PopUpMenuItem> items) {
		String title = app.getString(R.string.upload_to_openstreetmap);
		items.add(new Builder(app)
				.setTitle(title)
				.setIcon(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_upload_to_openstreetmap))
				.setOnClickListener(v -> {
					if (selectedItems.isEmpty()) {
						String message = app.getString(R.string.local_index_no_items_to_do, title.toLowerCase());
						app.showShortToastMessage(Algorithms.capitalizeFirstLetter(message));
					} else {
						showUploadConfirmationDialog(activity, fragment, selectedItems);
					}
				})
				.create()
		);
	}

	private void showUploadConfirmationDialog(@NonNull FragmentActivity activity, @NonNull Fragment fragment, @NonNull Set<TrackItem> trackItems) {
		long[] size = new long[1];
		List<SelectableItem<TrackItem>> items = new ArrayList<>();
		for (TrackItem gpxInfo : trackItems) {
			KFile file = gpxInfo.getFile();
			if (file != null) {
				SelectableItem<TrackItem> item = new SelectableItem<>();
				item.setObject(gpxInfo);
				item.setTitle(gpxInfo.getName());
				item.setIconId(R.drawable.ic_notification_track);

				items.add(item);
				size[0] += file.length();
			}
		}
		FragmentManager manager = activity.getSupportFragmentManager();
		UploadMultipleGPXBottomSheet dialog = UploadMultipleGPXBottomSheet.showInstance(manager, items, items);
		if (dialog != null) {
			dialog.setDialogStateListener(new DialogStateListener() {
				@Override
				public void onDialogCreated() {
					dialog.setTitle(app.getString(R.string.upload_to_openstreetmap));
					dialog.setApplyButtonTitle(app.getString(R.string.shared_string_continue));
					String total = app.getString(R.string.shared_string_total);
					dialog.setTitleDescription(app.getString(R.string.ltr_or_rtl_combine_via_colon, total,
							AndroidUtils.formatSize(app, size[0])));
				}
			});
			dialog.setOnApplySelectionListener(selectedItems -> {
				List<File> files = new ArrayList<>();
				for (SelectableItem<TrackItem> item : selectedItems) {
					KFile file = item.getObject().getFile();
					if (file != null) {
						files.add(SharedUtil.jFile(file));
					}
				}
				sendGPXFiles(activity, fragment, files.toArray(new File[0]));
			});
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

		public String asUrlParam() {
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

	public boolean sendGPXFiles(FragmentActivity activity, Fragment fragment, File... files) {
		String name = OSM_USER_NAME_OR_EMAIL.get();
		String pwd = OSM_USER_PASSWORD.get();
		String authToken = OSM_USER_ACCESS_TOKEN.get();
		if ((Algorithms.isEmpty(name) || Algorithms.isEmpty(pwd)) && Algorithms.isEmpty(authToken)) {
			LoginBottomSheetFragment.showInstance(activity.getSupportFragmentManager(), fragment);
			return false;
		} else {
			SendGpxBottomSheetFragment.showInstance(activity.getSupportFragmentManager(), files, fragment);
			return true;
		}
	}

	@Override
	public boolean isMenuControllerSupported(Class<? extends MenuController> menuControllerClass) {
		return menuControllerClass == AmenityMenuController.class || menuControllerClass == RenderedObjectMenuController.class;
	}

	@Override
	public void buildContextMenuRows(@NonNull MenuBuilder menuBuilder, @NonNull View view, Object object) {
		if (object instanceof Amenity amenity) {
			String link = ObfConstants.getOsmUrlForId(amenity);
			if (!Algorithms.isEmpty(link)) {
				menuBuilder.buildRow(view, R.drawable.ic_action_openstreetmap_logo, null, link,
						0, false, null, true, 0, true, null, false);
			}
		} else if (object instanceof RenderedObject renderedObject) {
			String link = ObfConstants.getOsmUrlForId(renderedObject);
			if (!Algorithms.isEmpty(link)) {
				menuBuilder.buildRow(view, R.drawable.ic_action_info_dark, null, link, 0, false,
						null, true, 0, true, null, false);
			}
		}
	}

	@Override
	public String getName() {
		return app.getString(R.string.osm_editing_plugin_name);
	}

	@Override
	public int getLogoResourceId() {
		return R.drawable.ic_action_openstreetmap_logo;
	}

	@Override
	public Drawable getAssetResourceImage() {
		return app.getUIUtilities().getIcon(R.drawable.osm_editing);
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
