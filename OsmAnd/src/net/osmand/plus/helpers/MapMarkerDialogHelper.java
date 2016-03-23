package net.osmand.plus.helpers;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.Shape;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.text.format.DateFormat;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.IndexConstants;
import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.IconsCache;
import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dashboard.DashLocationFragment;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.dialogs.DirectionsDialogs;
import net.osmand.plus.views.DirectionDrawable;
import net.osmand.plus.views.controls.DynamicListView;
import net.osmand.plus.views.controls.ListDividerShape;
import net.osmand.plus.views.controls.StableArrayAdapter;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MapMarkerDialogHelper {
	public static final int ACTIVE_MARKERS = 0;
	public static final int MY_LOCATION = 10;
	public static final int MARKERS_HISTORY = 100;

	private MapActivity mapActivity;
	private OsmandApplication app;
	private MapMarkersHelper markersHelper;
	private MapMarkersDialogHelperCallbacks helperCallbacks;
	private boolean sorted;
	private boolean nightMode;
	private boolean selectionMode;

	private boolean useCenter;
	private LatLon myLoc;
	private LatLon loc;
	private Float heading;
	private int screenOrientation;
	private boolean reloading;
	private long lastUpdateTime;
	private boolean allSelected;

	public interface MapMarkersDialogHelperCallbacks {
		void reloadAdapter();

		void deleteMapMarker(int position);

		void showMarkersRouteOnMap();
	}

	public MapMarkerDialogHelper(MapActivity mapActivity) {
		this.mapActivity = mapActivity;
		app = mapActivity.getMyApplication();
		markersHelper = app.getMapMarkersHelper();
	}

	public void setHelperCallbacks(MapMarkersDialogHelperCallbacks helperCallbacks) {
		this.helperCallbacks = helperCallbacks;
	}

	public boolean isInSelectionMode() {
		return selectionMode;
	}

	public boolean hasActiveMarkers() {
		return markersHelper.getActiveMapMarkers().size() > 0;
	}

	public void setSelectionMode(boolean selectionMode) {
		this.selectionMode = selectionMode;
	}

	public boolean isNightMode() {
		return nightMode;
	}

	public void setNightMode(boolean nightMode) {
		this.nightMode = nightMode;
	}

	public boolean isSorted() {
		return sorted;
	}

	public void setSorted(boolean sorted) {
		this.sorted = sorted;
	}

	public AdapterView.OnItemClickListener getItemClickListener(final ArrayAdapter<Object> listAdapter) {
		return new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int item, long l) {
				Object obj = listAdapter.getItem(item);
				if (obj instanceof MapMarker) {
					MapMarker marker = (MapMarker) obj;
					if (selectionMode) {
						CheckBox checkBox = (CheckBox) view.findViewById(R.id.checkbox);
						checkBox.setChecked(!checkBox.isChecked());
						marker.selected = checkBox.isChecked();
						markersHelper.updateMapMarker(marker, false);
						if (helperCallbacks != null) {
							helperCallbacks.showMarkersRouteOnMap();
						}
					} else {
						if (!marker.history) {
							showMarkerOnMap(mapActivity, marker);
						} else {
							showHistoryOnMap(marker);
						}
					}
				} else if (obj instanceof Integer && (Integer) obj == MY_LOCATION && selectionMode) {
					CheckBox checkBox = (CheckBox) view.findViewById(R.id.checkbox);
					checkBox.setChecked(!checkBox.isChecked());
					markersHelper.setStartFromMyLocation(checkBox.isChecked());
					if (helperCallbacks != null) {
						helperCallbacks.showMarkersRouteOnMap();
					}
				}
			}
		};
	}

	public StableArrayAdapter getMapMarkersListAdapter() {

		screenOrientation = DashLocationFragment.getScreenOrientation(mapActivity);
		calculateLocationParams();

		final List<Object> objects = getListObjects();
		List<Object> activeObjects = getActiveObjects(objects);

		allSelected = true;
		List<MapMarker> activeMarkers = new ArrayList<>(markersHelper.getActiveMapMarkers());
		for (MapMarker m : activeMarkers) {
			if (!m.selected) {
				allSelected = false;
				break;
			}
		}

		return new StableArrayAdapter(mapActivity,
				R.layout.map_marker_item, R.id.title, objects, activeObjects) {

			@Override
			public void buildDividers() {
				dividers = getCustomDividers(getObjects());
			}

			@Override
			public boolean isEnabled(int position) {
				Object obj = getItem(position);
				return obj instanceof MapMarker
						|| (obj instanceof Integer && (Integer) obj == MY_LOCATION);
			}

			@Override
			public View getView(final int position, View convertView, ViewGroup parent) {
				// User super class to create the View
				View v = convertView;
				Object obj = getItem(position);
				boolean labelView = (obj instanceof Integer);
				boolean topDividerView = (obj instanceof Boolean) && ((Boolean) obj);
				boolean bottomDividerView = (obj instanceof Boolean) && !((Boolean) obj);
				if (labelView) {
					if ((Integer) obj == MY_LOCATION) {
						v = updateMyLocationView(v);
					} else {
						v = createItemForCategory(this, (Integer) obj);
					}
					AndroidUtils.setListItemBackground(mapActivity, v, nightMode);
				} else if (topDividerView) {
					v = mapActivity.getLayoutInflater().inflate(R.layout.card_top_divider, null);
					AndroidUtils.setListBackground(mapActivity, v, nightMode);
				} else if (bottomDividerView) {
					v = mapActivity.getLayoutInflater().inflate(R.layout.card_bottom_divider, null);
					AndroidUtils.setListBackground(mapActivity, v, nightMode);
				} else if (obj instanceof MapMarker) {
					MapMarker marker = (MapMarker) obj;
					v = updateMapMarkerItemView(this, v, marker);
					AndroidUtils.setListItemBackground(mapActivity, v, nightMode);
				}
				return v;
			}
		};
	}

	private List<Drawable> getCustomDividers(List<Object> points) {
		int color;
		if (nightMode) {
			color = mapActivity.getResources().getColor(R.color.dashboard_divider_dark);
		} else {
			color = mapActivity.getResources().getColor(R.color.dashboard_divider_light);
		}

		Shape fullDividerShape = new ListDividerShape(color, 0);
		Shape halfDividerShape = new ListDividerShape(color, AndroidUtils.dpToPx(mapActivity, 56f));

		final ShapeDrawable fullDivider = new ShapeDrawable(fullDividerShape);
		final ShapeDrawable halfDivider = new ShapeDrawable(halfDividerShape);

		int divHeight = AndroidUtils.dpToPx(mapActivity, 1f);
		fullDivider.setIntrinsicHeight(divHeight);
		halfDivider.setIntrinsicHeight(divHeight);

		List<Drawable> res = new ArrayList<>();
		for (int i = 0; i < points.size(); i++) {
			Object obj = points.get(i);
			Object objNext = i + 1 < points.size() ? points.get(i + 1) : null;

			if (objNext == null) {
				break;
			}

			boolean bottomDividerViewNext = (objNext instanceof Boolean) && !((Boolean) objNext);

			boolean mapMarker = (obj instanceof MapMarker);
			boolean mapMarkerNext = (objNext instanceof MapMarker);

			Drawable d = null;

			if (mapMarkerNext) {
				if (mapMarker) {
					d = halfDivider;
				} else {
					d = fullDivider;
				}
			} else if (mapMarker && !bottomDividerViewNext) {
				d = fullDivider;
			}

			res.add(d);
		}
		return res;
	}

	protected View createItemForCategory(final ArrayAdapter<Object> listAdapter, final int type) {
		View v = mapActivity.getLayoutInflater().inflate(R.layout.waypoint_header, null);
		v.findViewById(R.id.toggle_item).setVisibility(View.GONE);
		v.findViewById(R.id.ProgressBar).setVisibility(View.GONE);

		if (type == MARKERS_HISTORY) {
			final Button btn = (Button) v.findViewById(R.id.header_button);
			btn.setTextColor(!nightMode ? mapActivity.getResources().getColor(R.color.map_widget_blue)
					: mapActivity.getResources().getColor(R.color.osmand_orange));
			btn.setText(mapActivity.getString(R.string.shared_string_clear));
			btn.setVisibility(View.VISIBLE);
			btn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					AlertDialog.Builder builder = new AlertDialog.Builder(mapActivity);
					builder.setMessage(mapActivity.getString(R.string.clear_markers_history_q))
							.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									listAdapter.notifyDataSetInvalidated();
									markersHelper.removeMarkersHistory();
									if (markersHelper.getActiveMapMarkers().size() == 0) {
										mapActivity.getDashboard().hideDashboard();
									} else if (helperCallbacks != null) {
										helperCallbacks.reloadAdapter();
									} else {
										reloadListAdapter(listAdapter);
									}
								}
							})
							.setNegativeButton(R.string.shared_string_no, null)
							.show();
				}
			});

		} else if (type == ACTIVE_MARKERS) {
			if (selectionMode) {
				final Button btn = (Button) v.findViewById(R.id.header_button);
				btn.setTextColor(!nightMode ? mapActivity.getResources().getColor(R.color.map_widget_blue)
						: mapActivity.getResources().getColor(R.color.osmand_orange));
				if (allSelected) {
					btn.setText(mapActivity.getString(R.string.shared_string_deselect_all));
				} else {
					btn.setText(mapActivity.getString(R.string.shared_string_select_all));
				}
				btn.setVisibility(View.VISIBLE);
				btn.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						List<MapMarker> markers = markersHelper.getActiveMapMarkers();
						for (MapMarker marker : markers) {
							marker.selected = !allSelected;
						}
						markersHelper.setStartFromMyLocation(!allSelected);
						allSelected = !allSelected;
						if (helperCallbacks != null) {
							helperCallbacks.reloadAdapter();
						} else {
							reloadListAdapter(listAdapter);
						}
					}
				});

			} else {
				final ImageButton btn = (ImageButton) v.findViewById(R.id.image_button);
				btn.setImageDrawable(app.getIconsCache().getContentIcon(R.drawable.ic_overflow_menu_white, !nightMode));
				btn.setVisibility(View.VISIBLE);
				btn.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {

						IconsCache iconsCache = app.getIconsCache();
						final PopupMenu optionsMenu = new PopupMenu(mapActivity, v);
						DirectionsDialogs.setupPopUpMenuIcon(optionsMenu);
						MenuItem item;
						item = optionsMenu.getMenu().add(R.string.shared_string_clear)
								.setIcon(iconsCache.getContentIcon(R.drawable.ic_action_delete_dark));
						item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
							@Override
							public boolean onMenuItemClick(MenuItem item) {
								AlertDialog.Builder builder = new AlertDialog.Builder(mapActivity);
								builder.setMessage(mapActivity.getString(R.string.clear_active_markers_q))
										.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {
											@Override
											public void onClick(DialogInterface dialog, int which) {
												listAdapter.notifyDataSetInvalidated();
												markersHelper.removeActiveMarkers();
												if (markersHelper.getMapMarkersHistory().size() == 0) {
													mapActivity.getDashboard().hideDashboard();
												} else if (helperCallbacks != null) {
													helperCallbacks.reloadAdapter();
												} else {
													reloadListAdapter(listAdapter);
												}
											}
										})
										.setNegativeButton(R.string.shared_string_no, null)
										.show();
								return true;
							}
						});

						if (!sorted) {
							item = optionsMenu.getMenu().add(R.string.shared_string_reverse_order).setIcon(
									iconsCache.getContentIcon(R.drawable.ic_action_undo_dark));
							item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
								@Override
								public boolean onMenuItemClick(MenuItem item) {
									markersHelper.reverseActiveMarkersOrder();
									if (helperCallbacks != null) {
										helperCallbacks.reloadAdapter();
									} else {
										reloadListAdapter(listAdapter);
									}
									return true;
								}
							});
						}

						item = optionsMenu.getMenu().add(R.string.shared_string_save_as_gpx).setIcon(
								iconsCache.getContentIcon(R.drawable.ic_action_save));
						item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
							@Override
							public boolean onMenuItemClick(MenuItem item) {
								generateGPX(markersHelper.getActiveMapMarkers());
								return true;
							}
						});

						optionsMenu.show();
					}
				});
			}
		}

		TextView tv = (TextView) v.findViewById(R.id.header_text);
		AndroidUtils.setTextPrimaryColor(mapActivity, tv, nightMode);
		tv.setText(getHeader(type));
		return v;
	}

	protected View updateMapMarkerItemView(final StableArrayAdapter adapter, View v, final MapMarker marker) {
		if (v == null || v.findViewById(R.id.info_close) == null) {
			v = mapActivity.getLayoutInflater().inflate(R.layout.map_marker_item, null);
		}
		updateMapMarkerInfo(mapActivity, v, loc, heading, useCenter, nightMode, screenOrientation,
				selectionMode, helperCallbacks, marker);
		final View more = v.findViewById(R.id.all_points);
		final View move = v.findViewById(R.id.info_move);
		final View remove = v.findViewById(R.id.info_close);
		remove.setVisibility(View.GONE);
		more.setVisibility(View.GONE);
		if (!marker.history && !sorted) {
			move.setVisibility(View.VISIBLE);
			((ImageView) move).setImageDrawable(app.getIconsCache().getContentIcon(
					R.drawable.ic_action_reorder, !nightMode));
			move.setTag(new DynamicListView.DragIcon() {
				@Override
				public void onClick() {
					final PopupMenu optionsMenu = new PopupMenu(mapActivity, move);
					DirectionsDialogs.setupPopUpMenuIcon(optionsMenu);
					MenuItem item;
					item = optionsMenu.getMenu().add(
							R.string.shared_string_remove).setIcon(app.getIconsCache().
							getContentIcon(R.drawable.ic_action_remove_dark));
					item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
						@Override
						public boolean onMenuItemClick(MenuItem item) {
							if (helperCallbacks != null) {
								int pos = adapter.getPosition(marker);
								if (pos != -1) {
									helperCallbacks.deleteMapMarker(pos);
								}
							}
							return true;
						}
					});
					optionsMenu.show();
				}
			});
		} else {
			move.setVisibility(View.GONE);
			move.setTag(null);
		}
		return v;
	}

	public static void updateMapMarkerInfo(final Context ctx, View localView, LatLon loc,
										   Float heading, boolean useCenter, boolean nightMode,
										   int screenOrientation, boolean selectionMode,
										   final MapMarkersDialogHelperCallbacks helperCallbacks,
										   final MapMarker marker) {
		TextView text = (TextView) localView.findViewById(R.id.waypoint_text);
		TextView textShadow = (TextView) localView.findViewById(R.id.waypoint_text_shadow);
		TextView textDist = (TextView) localView.findViewById(R.id.waypoint_dist);
		ImageView arrow = (ImageView) localView.findViewById(R.id.direction);
		ImageView waypointIcon = (ImageView) localView.findViewById(R.id.waypoint_icon);
		TextView waypointDeviation = (TextView) localView.findViewById(R.id.waypoint_deviation);
		TextView descText = (TextView) localView.findViewById(R.id.waypoint_desc_text);
		final CheckBox checkBox = (CheckBox) localView.findViewById(R.id.checkbox);

		if (text == null || textDist == null || arrow == null || waypointIcon == null
				|| waypointDeviation == null || descText == null) {
			return;
		}

		float[] mes = new float[2];
		if (loc != null && marker.point != null) {
			Location.distanceBetween(marker.getLatitude(), marker.getLongitude(), loc.getLatitude(), loc.getLongitude(), mes);
		}
		boolean newImage = false;
		int arrowResId = R.drawable.ic_destination_arrow_white;
		DirectionDrawable dd;
		if (!(arrow.getDrawable() instanceof DirectionDrawable)) {
			newImage = true;
			dd = new DirectionDrawable(ctx, arrow.getWidth(), arrow.getHeight());
		} else {
			dd = (DirectionDrawable) arrow.getDrawable();
		}
		if (!marker.history) {
			dd.setImage(arrowResId, useCenter ? R.color.color_distance : R.color.color_myloc_distance);
		} else {
			dd.setImage(arrowResId, nightMode ? R.color.secondary_text_dark : R.color.secondary_text_light);
		}
		if (loc == null || heading == null || marker.point == null) {
			dd.setAngle(0);
		} else {
			dd.setAngle(mes[1] - heading + 180 + screenOrientation);
		}
		if (newImage) {
			arrow.setImageDrawable(dd);
		}
		arrow.setVisibility(View.VISIBLE);
		arrow.invalidate();

		final OsmandApplication app = (OsmandApplication) ctx.getApplicationContext();

		if (!marker.history) {
			waypointIcon.setImageDrawable(getMapMarkerIcon(app, marker.colorIndex));
			AndroidUtils.setTextPrimaryColor(ctx, text, nightMode);
			textDist.setTextColor(ctx.getResources()
					.getColor(useCenter ? R.color.color_distance : R.color.color_myloc_distance));
		} else {
			waypointIcon.setImageDrawable(app.getIconsCache()
					.getContentIcon(R.drawable.ic_action_flag_dark, !nightMode));
			AndroidUtils.setTextSecondaryColor(ctx, text, nightMode);
			AndroidUtils.setTextSecondaryColor(ctx, textDist, nightMode);
		}

		int dist = (int) mes[0];
		textDist.setText(OsmAndFormatter.getFormattedDistance(dist, app));

		waypointDeviation.setVisibility(View.GONE);

		String descr;
		PointDescription pd = marker.getPointDescription(app);
		if (Algorithms.isEmpty(pd.getName())) {
			descr = pd.getTypeName();
		} else {
			descr = pd.getName();
		}

		if (textShadow != null) {
			textShadow.setText(descr);
		}
		text.setText(descr);

		descText.setVisibility(View.GONE);

		if (selectionMode) {
			checkBox.setChecked(marker.selected);
			checkBox.setVisibility(View.VISIBLE);
			checkBox.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					marker.selected = checkBox.isChecked();
					app.getMapMarkersHelper().updateMapMarker(marker, false);
					if (helperCallbacks != null) {
						helperCallbacks.showMarkersRouteOnMap();
					} else if (ctx instanceof MapActivity) {
						((MapActivity) ctx).refreshMap();
					}
				}
			});
		} else {
			checkBox.setVisibility(View.GONE);
			checkBox.setOnClickListener(null);
		}

		/*
		String pointDescription = "";
		if (descText != null) {
			AndroidUtils.setTextSecondaryColor(this, descText, nightMode);
			pointDescription = marker.getPointDescription(this).getTypeName();
		}

		if (descr.equals(pointDescription)) {
			pointDescription = "";
		}
		if (dist > 0 && !Algorithms.isEmpty(pointDescription)) {
			pointDescription = "  •  " + pointDescription;
		}
		if (descText != null) {
			descText.setText(pointDescription);
		}
		*/
	}

	protected void updateMapMarkerArrowDistanceView(View localView, final MapMarker marker) {
		TextView textDist = (TextView) localView.findViewById(R.id.waypoint_dist);
		ImageView arrow = (ImageView) localView.findViewById(R.id.direction);
		if (textDist == null || arrow == null) {
			return;
		}
		float[] mes = new float[2];
		if (loc != null && marker.point != null) {
			Location.distanceBetween(marker.getLatitude(), marker.getLongitude(), loc.getLatitude(), loc.getLongitude(), mes);
		}
		boolean newImage = false;
		int arrowResId = R.drawable.ic_destination_arrow_white;
		DirectionDrawable dd;
		if (!(arrow.getDrawable() instanceof DirectionDrawable)) {
			newImage = true;
			dd = new DirectionDrawable(mapActivity, arrow.getWidth(), arrow.getHeight());
		} else {
			dd = (DirectionDrawable) arrow.getDrawable();
		}
		if (!marker.history) {
			dd.setImage(arrowResId, useCenter ? R.color.color_distance : R.color.color_myloc_distance);
		} else {
			dd.setImage(arrowResId, nightMode ? R.color.secondary_text_dark : R.color.secondary_text_light);
		}
		if (loc == null || heading == null || marker.point == null) {
			dd.setAngle(0);
		} else {
			dd.setAngle(mes[1] - heading + 180 + screenOrientation);
		}
		if (newImage) {
			arrow.setImageDrawable(dd);
		}
		arrow.invalidate();

		int dist = (int) mes[0];
		textDist.setText(OsmAndFormatter.getFormattedDistance(dist, app));
	}

	protected View updateMyLocationView(View v) {
		if (v == null || v.findViewById(R.id.info_close) == null) {
			v = mapActivity.getLayoutInflater().inflate(R.layout.map_marker_item, null);
		}
		updateMyLocationInfo(mapActivity, v, nightMode, selectionMode, helperCallbacks);
		final View more = v.findViewById(R.id.all_points);
		final View move = v.findViewById(R.id.info_move);
		final View remove = v.findViewById(R.id.info_close);
		remove.setVisibility(View.GONE);
		more.setVisibility(View.GONE);
		move.setVisibility(View.GONE);
		move.setTag(null);
		return v;
	}

	public static void updateMyLocationInfo(final Context ctx, View localView, boolean nightMode,
											boolean selectionMode,
											final MapMarkersDialogHelperCallbacks helperCallbacks) {
		TextView text = (TextView) localView.findViewById(R.id.waypoint_text);
		TextView textDist = (TextView) localView.findViewById(R.id.waypoint_dist);
		ImageView arrow = (ImageView) localView.findViewById(R.id.direction);
		ImageView waypointIcon = (ImageView) localView.findViewById(R.id.waypoint_icon);
		TextView waypointDeviation = (TextView) localView.findViewById(R.id.waypoint_deviation);
		TextView descText = (TextView) localView.findViewById(R.id.waypoint_desc_text);
		final CheckBox checkBox = (CheckBox) localView.findViewById(R.id.checkbox);

		if (text == null || textDist == null || arrow == null || waypointIcon == null
				|| waypointDeviation == null || descText == null) {
			return;
		}

		arrow.setVisibility(View.GONE);
		textDist.setVisibility(View.GONE);
		waypointDeviation.setVisibility(View.GONE);

		final OsmandApplication app = (OsmandApplication) ctx.getApplicationContext();

		ApplicationMode appMode = app.getSettings().getApplicationMode();
		waypointIcon.setImageDrawable(ctx.getResources().getDrawable(appMode.getResourceLocationDay()));

		text.setText(ctx.getString(R.string.shared_string_my_location));
		descText.setText(ctx.getResources().getString(R.string.starting_point));
		descText.setVisibility(View.VISIBLE);

		AndroidUtils.setTextPrimaryColor(ctx, text, nightMode);
		AndroidUtils.setTextSecondaryColor(ctx, descText, nightMode);

		if (selectionMode) {
			checkBox.setChecked(app.getMapMarkersHelper().isStartFromMyLocation());
			checkBox.setVisibility(View.VISIBLE);
			checkBox.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					app.getMapMarkersHelper().setStartFromMyLocation(checkBox.isChecked());
					if (helperCallbacks != null) {
						helperCallbacks.showMarkersRouteOnMap();
					} else if (ctx instanceof MapActivity) {
						((MapActivity) ctx).refreshMap();
					}
				}
			});
		} else {
			checkBox.setVisibility(View.GONE);
			checkBox.setOnClickListener(null);
		}
	}

	public static void showMarkerOnMap(MapActivity mapActivity, MapMarker marker) {
		mapActivity.getMyApplication().getSettings().setMapLocationToShow(marker.getLatitude(), marker.getLongitude(),
				15, marker.getPointDescription(mapActivity), true, marker);
		MapActivity.launchMapActivityMoveToTop(mapActivity);
	}

	public void showHistoryOnMap(MapMarker marker) {
		app.getSettings().setMapLocationToShow(marker.getLatitude(), marker.getLongitude(),
				15, new PointDescription(PointDescription.POINT_TYPE_LOCATION,
						marker.getPointDescription(mapActivity).getName()),
				false, null);
		MapActivity.launchMapActivityMoveToTop(mapActivity);
	}

	protected String getHeader(int type) {
		String str = mapActivity.getString(R.string.map_markers);
		switch (type) {
			case ACTIVE_MARKERS:
				str = mapActivity.getString(R.string.active_markers);
				break;
			case MARKERS_HISTORY:
				str = mapActivity.getString(R.string.shared_string_history);
				break;
		}
		return str;
	}

	public void reloadListAdapter(ArrayAdapter<Object> listAdapter) {
		reloading = true;
		listAdapter.setNotifyOnChange(false);
		listAdapter.clear();
		List<Object> objects = getListObjects();
		for (Object point : objects) {
			listAdapter.add(point);
		}
		if (listAdapter instanceof StableArrayAdapter) {
			((StableArrayAdapter) listAdapter).updateObjects(objects, getActiveObjects(objects));
		}
		listAdapter.notifyDataSetChanged();
		reloading = false;
	}

	public void calcDistance(LatLon anchor, List<MapMarker> markers) {
		for (MapMarker m : markers) {
			m.dist = (int) (MapUtils.getDistance(m.getLatitude(), m.getLongitude(),
					anchor.getLatitude(), anchor.getLongitude()));
		}
	}

	protected List<Object> getListObjects() {
		final List<Object> objects = new ArrayList<>();

		LatLon mapLocation =
				new LatLon(mapActivity.getMapView().getLatitude(), mapActivity.getMapView().getLongitude());

		List<MapMarker> activeMarkers = new ArrayList<>(markersHelper.getActiveMapMarkers());
		calcDistance(mapLocation, activeMarkers);
		if (sorted) {
			Collections.sort(activeMarkers, new Comparator<MapMarker>() {
				@Override
				public int compare(MapMarker lhs, MapMarker rhs) {
					return lhs.dist < rhs.dist ? -1 : (lhs.dist == rhs.dist ? 0 : 1);
				}
			});
		}
		if (activeMarkers.size() > 0) {
			objects.add(ACTIVE_MARKERS);
			if (selectionMode) {
				objects.add(MY_LOCATION);
			}
			objects.addAll(activeMarkers);
			objects.add(false);
		}

		if (!selectionMode) {
			List<MapMarker> markersHistory = new ArrayList<>(markersHelper.getMapMarkersHistory());
			calcDistance(mapLocation, markersHistory);
			if (markersHistory.size() > 0) {
				if (activeMarkers.size() > 0) {
					objects.add(true);
				}
				objects.add(MARKERS_HISTORY);
				objects.addAll(markersHistory);
				objects.add(false);
			}
		}

		return objects;
	}

	private List<Object> getActiveObjects(List<Object> objects) {
		List<Object> activeObjects = new ArrayList<>();
		for (Object obj : objects) {
			if (obj instanceof MapMarker) {
				activeObjects.add(obj);
			}
		}
		return activeObjects;
	}

	public static Drawable getMapMarkerIcon(OsmandApplication app, int colorIndex) {
		return app.getIconsCache().getIcon(R.drawable.ic_action_flag_dark, getMapMarkerColorId(colorIndex));
	}

	public static int getMapMarkerColorId(int colorIndex) {
		int colorId;
		switch (colorIndex) {
			case 0:
				colorId = R.color.marker_blue;
				break;
			case 1:
				colorId = R.color.marker_green;
				break;
			case 2:
				colorId = R.color.marker_orange;
				break;
			case 3:
				colorId = R.color.marker_red;
				break;
			case 4:
				colorId = R.color.marker_yellow;
				break;
			case 5:
				colorId = R.color.marker_teal;
				break;
			case 6:
				colorId = R.color.marker_purple;
				break;
			default:
				colorId = R.color.marker_blue;
		}
		return colorId;
	}

	public void updateLocation(ListView listView, boolean compassChanged) {
		if ((compassChanged && !mapActivity.getDashboard().isMapLinkedToLocation())
				|| reloading || System.currentTimeMillis() - lastUpdateTime < 100) {
			return;
		}

		lastUpdateTime = System.currentTimeMillis();

		try {
			LatLon prevMyLoc = myLoc;
			calculateLocationParams();

			for (int i = listView.getFirstVisiblePosition(); i <= listView.getLastVisiblePosition(); i++) {
				Object obj = listView.getItemAtPosition(i);
				View v = listView.getChildAt(i - listView.getFirstVisiblePosition());
				if (obj instanceof MapMarker && v != null) {
					updateMapMarkerArrowDistanceView(v, (MapMarker) obj);
				}
			}

			if (selectionMode && markersHelper.isStartFromMyLocation() && prevMyLoc == null && myLoc != null) {
				if (helperCallbacks != null) {
					helperCallbacks.showMarkersRouteOnMap();
				} else {
					mapActivity.refreshMap();
				}
			}
		} catch (Exception e) {
		}
	}

	public void updateMarkerView(ListView listView, MapMarker marker) {
		try {
			for (int i = listView.getFirstVisiblePosition(); i <= listView.getLastVisiblePosition(); i++) {
				Object obj = listView.getItemAtPosition(i);
				View v = listView.getChildAt(i - listView.getFirstVisiblePosition());
				if (obj == marker) {
					updateMapMarkerInfo(mapActivity, v, loc, heading, useCenter, nightMode,
							screenOrientation, selectionMode, helperCallbacks, marker);
				}
			}
		} catch (Exception e) {
		}
	}

	private void calculateLocationParams() {
		DashboardOnMap d = mapActivity.getDashboard();
		if (d == null) {
			return;
		}

		float head = d.getHeading();
		float mapRotation = d.getMapRotation();
		LatLon mw = d.getMapViewLocation();
		Location l = d.getMyLocation();
		boolean mapLinked = d.isMapLinkedToLocation() && l != null;
		myLoc = l == null ? null : new LatLon(l.getLatitude(), l.getLongitude());
		useCenter = !mapLinked;
		loc = (useCenter ? mw : myLoc);
		heading = useCenter ? -mapRotation : head;
	}

	private void generateGPX(List<MapMarker> markers) {
		final File dir = app.getAppPath(IndexConstants.GPX_INDEX_DIR + "/map markers");
		if (!dir.exists()) {
			dir.mkdirs();
		}
		Date date = new Date();
		String fileName = DateFormat.format("yyyy-MM-dd", date).toString() + "_" + new SimpleDateFormat("HH-mm_EEE", Locale.US).format(date);
		File fout = new File(dir, fileName + ".gpx");
		int ind = 1;
		while (fout.exists()) {
			fout = new File(dir, fileName + "_" + (++ind) + ".gpx");
		}
		GPXFile file = new GPXFile();
		for (MapMarker marker : markersHelper.getActiveMapMarkers()) {
			WptPt wpt = new WptPt();
			wpt.lat = marker.getLatitude();
			wpt.lon = marker.getLongitude();
			wpt.setColor(mapActivity.getResources().getColor(getMapMarkerColorId(marker.colorIndex)));
			wpt.name = marker.getOnlyName();
			//wpt.link = r.getFileName();
			//wpt.time = r.getFile().lastModified();
			//wpt.category = r.getSearchHistoryType();
			file.points.add(wpt);
		}
		GPXUtilities.writeGpxFile(fout, file, app);
	}
}
