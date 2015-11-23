package net.osmand.plus.osmedit;

import java.util.List;

import net.osmand.PlatformUtil;
import net.osmand.access.AccessibleToast;
import net.osmand.data.Amenity;
import net.osmand.osm.PoiType;
import net.osmand.osm.edit.Node;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.OnContextMenuClick;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.EnumAdapter;
import net.osmand.plus.activities.EnumAdapter.IEnumWithResource;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.TabActivity;
import net.osmand.plus.dashboard.tools.DashFragmentData;
import net.osmand.plus.myplaces.AvailableGPXFragment;
import net.osmand.plus.myplaces.AvailableGPXFragment.GpxInfo;
import net.osmand.plus.myplaces.FavoritesActivity;
import net.osmand.plus.osmedit.OsmPoint.Action;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;


public class OsmEditingPlugin extends OsmandPlugin {
	private static final Log LOG = PlatformUtil.getLog(OsmEditingPlugin.class);
	private static final String ID = "osm.editing";
	private OsmandSettings settings;
	private OsmandApplication app;
	OpenstreetmapsDbHelper dbpoi;
	OsmBugsDbHelper dbbug;
	OpenstreetmapLocalUtil localUtil;
	OpenstreetmapRemoteUtil remoteUtil;
	OsmBugsRemoteUtil remoteNotesUtil;
	OsmBugsLocalUtil localNotesUtil;
	
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
		if(localUtil == null) {
			localUtil = new OpenstreetmapLocalUtil(this);
		}
		return localUtil;
	}
	
	public OpenstreetmapRemoteUtil getPoiModificationRemoteUtil() {
		if(remoteUtil == null) {
			remoteUtil = new OpenstreetmapRemoteUtil(app);
		}
		return remoteUtil;
	}
	
	public OsmBugsRemoteUtil getOsmNotesRemoteUtil() {
		if(remoteNotesUtil == null) {
			remoteNotesUtil = new OsmBugsRemoteUtil(app);
		}
		return remoteNotesUtil;
	}
	
	public OsmBugsLocalUtil getOsmNotesLocalUtil() {
		if(localNotesUtil == null) {
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
			if (!mapView.getLayers().contains(osmEditsLayer)) {
				activity.getMapView().addLayer(osmEditsLayer, 3.5f);
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
	public void registerLayers(MapActivity activity) {
		osmBugsLayer = new OsmBugsLayer(activity, this);
		osmEditsLayer = new OsmEditsLayer(activity, this);
	}

	public OsmEditsLayer getOsmEditsLayer(MapActivity activity) {
		if (osmEditsLayer == null) {
			registerLayers(activity);
		}
		return osmEditsLayer;
	}

	public OsmBugsLayer getBugsLayer(MapActivity activity) {
		if (osmBugsLayer == null) {
			registerLayers(activity);
		}
		return osmBugsLayer;
	}

	@Override
	public void mapActivityCreate(MapActivity activity) {
		// Always create new actions !
//		poiActions = new EditingPOIDialogProvider(activity, this);
//		activity.addDialogProvider(getPoiActions(activity));
		activity.addDialogProvider(getBugsLayer(activity));
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
		OnContextMenuClick listener = new OnContextMenuClick() {
			@Override
			public boolean onContextMenuClick(ArrayAdapter<?> adapter, int resId, int pos, boolean isChecked) {
				if (resId == R.string.context_menu_item_create_poi) {
					//getPoiActions(mapActivity).showCreateDialog(latitude, longitude);
					EditPoiDialogFragment editPoiDialogFragment =
							EditPoiDialogFragment.createAddPoiInstance(latitude, longitude,
									mapActivity.getMyApplication());
					editPoiDialogFragment.show(mapActivity.getSupportFragmentManager(),
							EditPoiDialogFragment.TAG);
				} else if (resId == R.string.context_menu_item_open_note) {
					if (osmBugsLayer == null) {
						registerLayers(mapActivity);
					}
					osmBugsLayer.openBug(latitude, longitude, "");
				} else if (resId == R.string.poi_context_menu_delete) {
					new EditPoiDialogFragment.ShowDeleteDialogAsyncTask(mapActivity)
							.execute((Amenity) selectedObj);
				} else if (resId == R.string.poi_context_menu_modify) {
					EditPoiDialogFragment.showEditInstance((Amenity) selectedObj, mapActivity);
				} else if (resId == R.string.poi_context_menu_modify_osm_change) {
					final Node entity = ((OpenstreetmapPoint) selectedObj).getEntity();
					EditPoiDialogFragment.createInstance(entity, false)
							.show(mapActivity.getSupportFragmentManager(), "edit_poi");
				}
				return true;
			}
		};
		boolean isEditable = false;
		if (selectedObj instanceof Amenity) {
			Amenity amenity = (Amenity) selectedObj;
			final PoiType poiType = amenity.getType().getPoiTypeByKeyName(amenity.getSubType());
			isEditable = !amenity.getType().isWiki() && !poiType.isNotEditableOsm();
		}
		if (isEditable) {
			adapter.item(R.string.poi_context_menu_modify).iconColor(R.drawable.ic_action_edit_dark).listen(listener).position(1).reg();
			adapter.item(R.string.poi_context_menu_delete).iconColor(R.drawable.ic_action_delete_dark).listen(listener).position(2).reg();
		} else if (selectedObj instanceof OpenstreetmapPoint && ((OpenstreetmapPoint) selectedObj).getAction() != Action.DELETE) {
			adapter.item(R.string.poi_context_menu_modify_osm_change)
					.iconColor(R.drawable.ic_action_edit_dark).listen(listener).position(1).reg();
		} else {
			adapter.item(R.string.context_menu_item_create_poi).iconColor(R.drawable.ic_action_plus_dark).listen(listener).position(-1).reg();
		}
		adapter.item(R.string.context_menu_item_open_note).iconColor(R.drawable.ic_action_bug_dark).listen(listener).reg();
	}

	@Override
	public void addMyPlacesTab(FavoritesActivity favoritesActivity, List<TabActivity.TabItem> mTabs, Intent intent) {
		if (getDBPOI().getOpenstreetmapPoints().size() > 0 || getDBBug().getOsmbugsPoints().size() > 0) {
			mTabs.add(favoritesActivity.getTabIndicator(R.string.osm_edits, OsmEditsFragment.class));
			if (intent != null && "OSM".equals(intent.getStringExtra("TAB"))) {
				app.getSettings().FAVORITES_TAB.set(R.string.osm_edits);
			}
		}
	}

	@Override
	public void registerLayerContextMenuActions(OsmandMapTileView mapView, ContextMenuAdapter adapter, final MapActivity mapActivity) {
		adapter.item(R.string.layer_osm_bugs).selected(settings.SHOW_OSM_BUGS.get() ? 1 : 0)
				.iconColor(R.drawable.ic_action_bug_dark).listen(new OnContextMenuClick() {

			@Override
			public boolean onContextMenuClick(ArrayAdapter<?> adapter, int itemId, int pos, boolean isChecked) {
				if (itemId == R.string.layer_osm_bugs) {
					settings.SHOW_OSM_BUGS.set(isChecked);
					updateLayers(mapActivity.getMapView(), mapActivity);
				}
				return true;
			}
		}).position(16).reg();

	}

	@Override
	public String getDescription() {
		return app.getString(R.string.osm_editing_plugin_description);
	}

	@Override
	public void contextMenuFragment(final Activity la, final Fragment fragment, final Object info, ContextMenuAdapter adapter) {
		if (fragment instanceof AvailableGPXFragment) {
			adapter.item(R.string.local_index_mi_upload_gpx)
					.iconColor(R.drawable.ic_action_export)
					.listen(new OnContextMenuClick() {

						@Override
						public boolean onContextMenuClick(ArrayAdapter<?> adapter, int itemId, int pos, boolean isChecked) {
							sendGPXFiles(la, (AvailableGPXFragment) fragment, (GpxInfo) info);
							return true;
						}
					}).reg();
		}
	}

	@Override
	public void optionsMenuFragment(final Activity activity, final Fragment fragment, ContextMenuAdapter optionsMenuAdapter) {
		if (fragment instanceof AvailableGPXFragment) {
			final AvailableGPXFragment f = ((AvailableGPXFragment) fragment);
			optionsMenuAdapter.item(R.string.local_index_mi_upload_gpx)
					.icon(R.drawable.ic_action_export)
					.listen(new OnContextMenuClick() {

						@Override
						public boolean onContextMenuClick(ArrayAdapter<?> adapter, int itemId, int pos, boolean isChecked) {
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
					}).position(5).reg();
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
			AccessibleToast.makeText(la, R.string.validate_gpx_upload_name_pwd, Toast.LENGTH_LONG).show();
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
				).execute(info);
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
		return R.drawable.ic_action_bug_dark;
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
		if (point.getGroup() == OsmPoint.Group.POI) {
			String subtype = "";
			if (!Algorithms.isEmpty(((OpenstreetmapPoint) point).getSubtype())) {
				subtype = " (" + ((OpenstreetmapPoint) point).getSubtype() + ") ";
			}
			return prefix + subtype + ((OpenstreetmapPoint) point).getName();
		} else if (point.getGroup() == OsmPoint.Group.BUG) {
			return prefix  + ((OsmNotesPoint) point).getText();
		} else {
			return prefix;
		}
	}

	private static String getPrefix(OsmPoint osmPoint) {
		return (osmPoint.getGroup() == OsmPoint.Group.POI ? "POI " : "Bug ") + " id: " + osmPoint.getId();
	}

	@Override
	public DashFragmentData getCardFragment() {
		return DashOsmEditsFragment.FRAGMENT_DATA;
	}
}
