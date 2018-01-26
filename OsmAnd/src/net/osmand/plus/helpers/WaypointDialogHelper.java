package net.osmand.plus.helpers;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.Shape;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.Location;
import net.osmand.TspAnt;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.LocationPoint;
import net.osmand.data.PointDescription;
import net.osmand.plus.GeocodingLookupService;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.TargetPointsHelper.TargetPoint;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.MapActivityLayers;
import net.osmand.plus.dialogs.DirectionsDialogs;
import net.osmand.plus.helpers.WaypointHelper.LocationPointWrapper;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.views.controls.DynamicListView.DragIcon;
import net.osmand.plus.views.controls.ListDividerShape;
import net.osmand.plus.views.controls.StableArrayAdapter;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class WaypointDialogHelper {
	private MapActivity mapActivity;
	private OsmandApplication app;
	private WaypointHelper waypointHelper;
	private WaypointDialogHelperCallbacks helperCallbacks;

	private boolean flat;
	private List<LocationPointWrapper> deletedPoints;

	public interface WaypointDialogHelperCallbacks {
		void reloadAdapter();

		void deleteWaypoint(int position);

		void exchangeWaypoints(int pos1, int pos2);
	}

	private static class RadiusItem {
		int type;

		public RadiusItem(int type) {
			this.type = type;
		}
	}

	public void setHelperCallbacks(WaypointDialogHelperCallbacks callbacks) {
		this.helperCallbacks = callbacks;
	}

	public WaypointDialogHelper(MapActivity mapActivity) {
		this.app = mapActivity.getMyApplication();
		waypointHelper = this.app.getWaypointHelper();
		this.mapActivity = mapActivity;

	}

	public static void updatePointInfoView(final OsmandApplication app, final Activity activity,
										   View localView, final LocationPointWrapper ps,
										   final boolean mapCenter, final boolean nightMode,
										   final boolean edit, final boolean topBar) {
		WaypointHelper wh = app.getWaypointHelper();
		final LocationPoint point = ps.getPoint();
		TextView text = (TextView) localView.findViewById(R.id.waypoint_text);
		if (!topBar) {
			AndroidUtils.setTextPrimaryColor(activity, text, nightMode);
		}
		TextView textShadow = (TextView) localView.findViewById(R.id.waypoint_text_shadow);
		if (!edit) {
			localView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					showOnMap(app, activity, point, mapCenter);
				}
			});
		}
		TextView textDist = (TextView) localView.findViewById(R.id.waypoint_dist);
		((ImageView) localView.findViewById(R.id.waypoint_icon)).setImageDrawable(ps.getDrawable(activity, app, nightMode));
		int dist = -1;
		boolean startPoint = ps.type == WaypointHelper.TARGETS && ((TargetPoint) ps.point).start;
		if (!startPoint) {
			if (!wh.isRouteCalculated()) {
				if (activity instanceof MapActivity) {
					dist = (int) MapUtils.getDistance(((MapActivity) activity).getMapView().getLatitude(), ((MapActivity) activity)
							.getMapView().getLongitude(), point.getLatitude(), point.getLongitude());
				}
			} else {
				dist = wh.getRouteDistance(ps);
			}
		}

		if (dist > 0) {
			textDist.setText(OsmAndFormatter.getFormattedDistance(dist, app));
		} else {
			textDist.setText("");
		}

		TextView textDeviation = (TextView) localView.findViewById(R.id.waypoint_deviation);
		if (textDeviation != null) {
			if (dist > 0 && ps.deviationDistance > 0) {
				String devStr = "+" + OsmAndFormatter.getFormattedDistance(ps.deviationDistance, app);
				textDeviation.setText(devStr);
				if (!topBar) {
					int colorId = nightMode ? R.color.secondary_text_dark : R.color.secondary_text_light;
					AndroidUtils.setTextSecondaryColor(activity, textDeviation, nightMode);
					if (ps.deviationDirectionRight) {
						textDeviation.setCompoundDrawablesWithIntrinsicBounds(
								app.getIconsCache().getIcon(R.drawable.ic_small_turn_right, colorId),
								null, null, null);
					} else {
						textDeviation.setCompoundDrawablesWithIntrinsicBounds(
								app.getIconsCache().getIcon(R.drawable.ic_small_turn_left, colorId),
								null, null, null);
					}
				}
				textDeviation.setVisibility(View.VISIBLE);
			} else {
				textDeviation.setText("");
				textDeviation.setVisibility(View.GONE);
			}
		}

		String descr;
		PointDescription pd = point.getPointDescription(app);
		if (Algorithms.isEmpty(pd.getName())) {
			descr = pd.getTypeName();
		} else {
			descr = pd.getName();
		}

		if (textShadow != null) {
			textShadow.setText(descr);
		}
		text.setText(descr);

		String pointDescription = "";
		TextView descText = (TextView) localView.findViewById(R.id.waypoint_desc_text);
		if (descText != null) {
			AndroidUtils.setTextSecondaryColor(activity, descText, nightMode);
			switch (ps.type) {
				case WaypointHelper.TARGETS:
					TargetPoint targetPoint = (TargetPoint) ps.point;
					if (targetPoint.start) {
						pointDescription = activity.getResources().getString(R.string.starting_point);
					} else {
						pointDescription = targetPoint.getPointDescription(activity).getTypeName();
					}
					break;

				case WaypointHelper.FAVORITES:
					FavouritePoint favPoint = (FavouritePoint) ps.point;
					pointDescription = Algorithms.isEmpty(favPoint.getCategory()) ? activity.getResources().getString(R.string.shared_string_favorites) : favPoint.getCategory();
					break;
			}
		}

		if (Algorithms.objectEquals(descr, pointDescription)) {
			pointDescription = "";
		}
		if (dist > 0 && !Algorithms.isEmpty(pointDescription)) {
			pointDescription = "  •  " + pointDescription;
		}
		if (descText != null) {
			descText.setText(pointDescription);
		}
	}

	private List<Object> getPoints() {
		final List<Object> points;
		if (flat) {
			points = new ArrayList<Object>(waypointHelper.getAllPoints());
		} else {
			points = getStandardPoints();
		}
		return points;
	}

	private List<Object> getActivePoints(List<Object> points) {
		List<Object> activePoints = new ArrayList<>();
		for (Object p : points) {
			if (p instanceof LocationPointWrapper) {
				LocationPointWrapper w = (LocationPointWrapper) p;
				if (w.type == WaypointHelper.TARGETS) {
					activePoints.add(p);
				}
			}
		}
		return activePoints;
	}

	private List<Drawable> getCustomDividers(Context ctx, List<Object> points, boolean nightMode) {
		int color = ContextCompat.getColor(ctx, nightMode
				? R.color.dashboard_divider_dark : R.color.dashboard_divider_light);

		Shape fullDividerShape = new ListDividerShape(color, 0);
		Shape halfDividerShape = new ListDividerShape(color, AndroidUtils.dpToPx(ctx, 56f));
		Shape headerDividerShape = new ListDividerShape(color, AndroidUtils.dpToPx(ctx, 16f));

		final ShapeDrawable fullDivider = new ShapeDrawable(fullDividerShape);
		final ShapeDrawable halfDivider = new ShapeDrawable(halfDividerShape);
		final ShapeDrawable headerDivider = new ShapeDrawable(headerDividerShape);

		int divHeight = AndroidUtils.dpToPx(ctx, 1f);
		fullDivider.setIntrinsicHeight(divHeight);
		halfDivider.setIntrinsicHeight(divHeight);
		headerDivider.setIntrinsicHeight(divHeight);

		List<Drawable> res = new ArrayList<>();
		for (int i = 0; i < points.size(); i++) {
			Object obj = points.get(i);
			Object objNext = i + 1 < points.size() ? points.get(i + 1) : null;

			if (objNext == null) {
				break;
			}

			boolean labelView = (obj instanceof Integer);
			boolean bottomDividerViewNext = (objNext instanceof Boolean) && !((Boolean) objNext);

			boolean locationPoint = (obj instanceof LocationPointWrapper);
			boolean locationPointNext = (objNext instanceof LocationPointWrapper);

			Drawable d = null;

			if (locationPointNext) {
				d = locationPoint ? halfDivider : fullDivider;
			} else if (objNext instanceof RadiusItem && labelView) {
				d = headerDivider;
			} else if (locationPoint && !bottomDividerViewNext) {
				d = fullDivider;
			}

			res.add(d);
		}
		return res;
	}

	public StableArrayAdapter getWaypointsDrawerAdapter(
			final boolean edit, final List<LocationPointWrapper> deletedPoints,
			final MapActivity ctx, final int[] running, final boolean flat, final boolean nightMode) {

		this.flat = flat;
		this.deletedPoints = deletedPoints;

		final List<Object> points = getPoints();
		List<Object> activePoints = getActivePoints(points);

		final WaypointDialogHelper helper = this;

		final StableArrayAdapter listAdapter = new StableArrayAdapter(ctx,
				R.layout.waypoint_reached, R.id.title, points, activePoints) {

			@Override
			public void buildDividers() {
				dividers = getCustomDividers(ctx, getObjects(), nightMode);
			}

			@Override
			public boolean isEnabled(int position) {
				Object obj = getItem(position);
				boolean labelView = (obj instanceof Integer);
				boolean topDividerView = (obj instanceof Boolean) && ((Boolean) obj);
				boolean bottomDividerView = (obj instanceof Boolean) && !((Boolean) obj);

				boolean enabled = !labelView && !topDividerView && !bottomDividerView;

				if (enabled && obj instanceof RadiusItem) {
					int type = ((RadiusItem) obj).type;
					enabled = type != WaypointHelper.POI;
				}

				return enabled;
			}

			@Override
			public View getView(final int position, View convertView, ViewGroup parent) {
				// User super class to create the View
				View v = convertView;
				final ArrayAdapter<Object> thisAdapter = this;
				Object obj = getItem(position);
				boolean labelView = (obj instanceof Integer);
				boolean topDividerView = (obj instanceof Boolean) && ((Boolean) obj);
				boolean bottomDividerView = (obj instanceof Boolean) && !((Boolean) obj);
				if (obj instanceof RadiusItem) {
					final int type = ((RadiusItem) obj).type;
					v = createItemForRadiusProximity(ctx, type, running, position, thisAdapter, nightMode);
					//Drawable d = new ColorDrawable(mapActivity.getResources().getColor(R.color.dashboard_divider_light));
					AndroidUtils.setListItemBackground(mapActivity, v, nightMode);
				} else if (labelView) {
					v = createItemForCategory(ctx, (Integer) obj, running, position, thisAdapter, nightMode, helper);
					AndroidUtils.setListItemBackground(mapActivity, v, nightMode);
				} else if (topDividerView) {
					v = ctx.getLayoutInflater().inflate(R.layout.card_top_divider, null);
					AndroidUtils.setListBackground(mapActivity, v, nightMode);
				} else if (bottomDividerView) {
					v = ctx.getLayoutInflater().inflate(R.layout.card_bottom_divider, null);
					AndroidUtils.setListBackground(mapActivity, v, nightMode);
				} else if (obj instanceof LocationPointWrapper) {
					LocationPointWrapper point = (LocationPointWrapper) obj;
					v = updateWaypointItemView(edit, deletedPoints, app, ctx, helper, v, point, this,
							nightMode, flat);
					AndroidUtils.setListItemBackground(mapActivity, v, nightMode);
				}
				return v;
			}
		};

		for (Object p : points) {
			if (p instanceof LocationPointWrapper) {
				LocationPointWrapper w = (LocationPointWrapper) p;
				if (w.type == WaypointHelper.TARGETS) {
					final TargetPoint t = (TargetPoint) w.point;
					if (t.getOriginalPointDescription() != null
							&& t.getOriginalPointDescription().isSearchingAddress(mapActivity)) {
						GeocodingLookupService.AddressLookupRequest lookupRequest
								= new GeocodingLookupService.AddressLookupRequest(t.point, new GeocodingLookupService.OnAddressLookupResult() {
							@Override
							public void geocodingDone(String address) {
								if (helperCallbacks != null) {
									helperCallbacks.reloadAdapter();
								} else {
									reloadListAdapter(listAdapter);
								}
								//updateRouteInfoMenu(ctx);
							}
						}, null);
						app.getGeocodingLookupService().lookupAddress(lookupRequest);
					}

				}
			}
		}

		return listAdapter;
	}


	public static View updateWaypointItemView(final boolean edit, final List<LocationPointWrapper> deletedPoints,
											  final OsmandApplication app, final Activity ctx,
											  final WaypointDialogHelper helper, View v,
											  final LocationPointWrapper point,
											  final ArrayAdapter adapter, final boolean nightMode,
											  final boolean flat) {
		if (v == null || v.findViewById(R.id.info_close) == null) {
			v = ctx.getLayoutInflater().inflate(R.layout.waypoint_reached, null);
		}
		updatePointInfoView(app, ctx, v, point, true, nightMode, edit, false);

		v.findViewById(R.id.all_points).setVisibility(View.GONE);
		final ImageView move = (ImageView) v.findViewById(R.id.info_move);
		final ImageButton remove = (ImageButton) v.findViewById(R.id.info_close);

		if (!edit) {
			remove.setVisibility(View.GONE);
			move.setVisibility(View.GONE);
		} else {
			boolean notFlatTargets = point.type == WaypointHelper.TARGETS && !flat;
			boolean startPoint = notFlatTargets && ((TargetPoint) point.point).start;
			final TargetPointsHelper targetPointsHelper = app.getTargetPointsHelper();
			boolean canRemove = !targetPointsHelper.getIntermediatePoints().isEmpty();
			int iconResId = nightMode ? R.color.marker_circle_button_color_dark : R.color.ctx_menu_title_color_dark;

			remove.setVisibility(View.VISIBLE);
			remove.setImageDrawable(app.getIconsCache().getIcon(R.drawable.ic_action_remove_dark, iconResId));
			remove.setEnabled(canRemove);
			remove.setAlpha(canRemove ? 1 : .5f);
			if (canRemove) {
				if (notFlatTargets && startPoint) {
					remove.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							if (targetPointsHelper.getPointToStart() == null) {
								if (!targetPointsHelper.getIntermediatePoints().isEmpty()) {
									replaceStartWithFirstIntermediate(targetPointsHelper, ctx, helper);
								}
							} else {
								targetPointsHelper.setStartPoint(null, true, null);
								updateControls(ctx, helper);
							}
						}
					});
				} else {
					remove.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							deletePoint(app, ctx, adapter, helper, point, deletedPoints, true);
						}
					});
				}
			}

			move.setVisibility(notFlatTargets ? View.VISIBLE : View.GONE);
			if (notFlatTargets) {
				move.setImageDrawable(app.getIconsCache().getIcon(R.drawable.ic_action_reorder, iconResId));
				move.setTag(new DragIcon() {
					@Override
					public void onClick() {
						// do nothing
					}
				});
			}
		}

		return v;
	}

	private static void updateRouteInfoMenu(Activity ctx) {
		if (ctx instanceof MapActivity) {
			((MapActivity) ctx).getMapLayers().getMapControlsLayer().getMapRouteInfoMenu().updateMenu();
		}
	}

	// switch start & finish
	private static void switchStartAndFinish(TargetPointsHelper targetPointsHelper, TargetPoint finish,
											 Activity ctx, TargetPoint start, OsmandApplication app,
											 WaypointDialogHelper helper) {
		targetPointsHelper.setStartPoint(new LatLon(finish.getLatitude(),
				finish.getLongitude()), false, finish.getPointDescription(ctx));
		if (start == null) {
			Location loc = app.getLocationProvider().getLastKnownLocation();
			if (loc != null) {
				targetPointsHelper.navigateToPoint(new LatLon(loc.getLatitude(),
						loc.getLongitude()), true, -1);
			}
		} else {
			targetPointsHelper.navigateToPoint(new LatLon(start.getLatitude(),
					start.getLongitude()), true, -1, start.getPointDescription(ctx));
		}
		updateControls(ctx, helper);
	}

	private static void updateControls(Activity ctx, WaypointDialogHelper helper) {
		if (helper != null && helper.helperCallbacks != null) {
			helper.helperCallbacks.reloadAdapter();
		}
		updateRouteInfoMenu(ctx);
	}

	private static void replaceStartWithFirstIntermediate(TargetPointsHelper targetPointsHelper, Activity ctx,
														  WaypointDialogHelper helper) {
		List<TargetPoint> intermediatePoints = targetPointsHelper.getIntermediatePointsWithTarget();
		TargetPoint firstIntermediate = intermediatePoints.remove(0);
		targetPointsHelper.setStartPoint(new LatLon(firstIntermediate.getLatitude(),
				firstIntermediate.getLongitude()), false, firstIntermediate.getPointDescription(ctx));
		targetPointsHelper.reorderAllTargetPoints(intermediatePoints, true);

		updateControls(ctx, helper);
	}

	// switch start & first intermediate point
	private static void switchStartAndFirstIntermediate(TargetPointsHelper targetPointsHelper, Activity ctx,
														TargetPoint start, WaypointDialogHelper helper) {
		List<TargetPoint> intermediatePoints =  targetPointsHelper.getIntermediatePointsWithTarget();
		TargetPoint firstIntermediate = intermediatePoints.remove(0);
		targetPointsHelper.setStartPoint(new LatLon(firstIntermediate.getLatitude(),
				firstIntermediate.getLongitude()), false, firstIntermediate.getPointDescription(ctx));
		TargetPoint destination = new TargetPoint(new LatLon(start.getLatitude(),
				start.getLongitude()), start.getPointDescription(ctx));
		intermediatePoints.add(0, destination);
		targetPointsHelper.reorderAllTargetPoints(intermediatePoints, true);

		updateControls(ctx, helper);
	}

	public static void deletePoint(final OsmandApplication app, Activity ctx,
								   final ArrayAdapter adapter,
								   final WaypointDialogHelper helper,
								   final Object item,
								   final List<LocationPointWrapper> deletedPoints,
								   final boolean needCallback) {

		if (item instanceof LocationPointWrapper && adapter != null) {
			LocationPointWrapper point = (LocationPointWrapper) item;
			if (point.type == WaypointHelper.TARGETS && adapter instanceof StableArrayAdapter) {
				StableArrayAdapter stableAdapter = (StableArrayAdapter) adapter;
				if (helper != null && helper.helperCallbacks != null && needCallback) {
					helper.helperCallbacks.deleteWaypoint(stableAdapter.getPosition(item));
				}
				updateRouteInfoMenu(ctx);
			} else {
				ArrayList<LocationPointWrapper> arr = new ArrayList<>();
				arr.add(point);
				app.getWaypointHelper().removeVisibleLocationPoint(arr);

				deletedPoints.add(point);

				adapter.setNotifyOnChange(false);
				adapter.remove(point);
				if (adapter instanceof StableArrayAdapter) {
					StableArrayAdapter stableAdapter = (StableArrayAdapter) adapter;
					stableAdapter.getObjects().remove(item);
					stableAdapter.refreshData();
				}
				adapter.notifyDataSetChanged();
			}
		}
	}


	protected View createItemForRadiusProximity(final FragmentActivity ctx, final int type, final int[] running,
												final int position, final ArrayAdapter<Object> thisAdapter, boolean nightMode) {
		View v;
		if (type == WaypointHelper.POI) {
			v = ctx.getLayoutInflater().inflate(R.layout.drawer_list_radius_ex, null);
			AndroidUtils.setTextPrimaryColor(mapActivity, (TextView) v.findViewById(R.id.titleEx), nightMode);
			String descEx = app.getPoiFilters().isShowingAnyPoi() ? ctx.getString(R.string.poi) : app.getPoiFilters().getSelectedPoiFiltersName();
			((TextView) v.findViewById(R.id.title)).setText(ctx.getString(R.string.search_radius_proximity) + ":");
			((TextView) v.findViewById(R.id.titleEx)).setText(ctx.getString(R.string.shared_string_type) + ":");
			final TextView radiusEx = (TextView) v.findViewById(R.id.descriptionEx);
			radiusEx.setText(descEx);
			v.findViewById(R.id.secondCellContainer).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					running[0] = position;
					thisAdapter.notifyDataSetInvalidated();
					MapActivity map = (MapActivity) ctx;
					map.getMapLayers().showSingleChoicePoiFilterDialog(map.getMapView(),
							new MapActivityLayers.DismissListener() {
								@Override
								public void dismiss() {
									enableType(running, thisAdapter, type, true);
								}
							});
				}

			});
			AndroidUtils.setTextPrimaryColor(mapActivity, (TextView) v.findViewById(R.id.title), nightMode);
			final TextView radius = (TextView) v.findViewById(R.id.description);
			radius.setText(OsmAndFormatter.getFormattedDistance(waypointHelper.getSearchDeviationRadius(type), app));
			v.findViewById(R.id.firstCellContainer).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					selectDifferentRadius(type, running, position, thisAdapter, mapActivity);
				}

			});
		} else {
			v = ctx.getLayoutInflater().inflate(R.layout.drawer_list_radius, null);
			((TextView) v.findViewById(R.id.title)).setText(ctx.getString(R.string.search_radius_proximity));
			AndroidUtils.setTextPrimaryColor(mapActivity, (TextView) v.findViewById(R.id.title), nightMode);
			final TextView radius = (TextView) v.findViewById(R.id.description);
			radius.setText(OsmAndFormatter.getFormattedDistance(waypointHelper.getSearchDeviationRadius(type), app));
			radius.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					selectDifferentRadius(type, running, position, thisAdapter, mapActivity);
				}

			});
		}
		return v;
	}

	protected View createItemForCategory(final FragmentActivity ctx, final int type, final int[] running,
										 final int position, final ArrayAdapter<Object> thisAdapter,
										 boolean nightMode, final WaypointDialogHelper helper) {
		View v;
		v = ctx.getLayoutInflater().inflate(R.layout.waypoint_header, null);
		final CompoundButton btn = (CompoundButton) v.findViewById(R.id.toggle_item);
		btn.setVisibility(waypointHelper.isTypeConfigurable(type) ? View.VISIBLE : View.GONE);
		btn.setOnCheckedChangeListener(null);
		final boolean checked = waypointHelper.isTypeEnabled(type);
		btn.setChecked(checked);
		btn.setEnabled(running[0] == -1);
		v.findViewById(R.id.ProgressBar).setVisibility(position == running[0] ? View.VISIBLE : View.GONE);
		btn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				running[0] = position;
				thisAdapter.notifyDataSetInvalidated();
				if (type == WaypointHelper.POI && isChecked) {
					selectPoi(running, thisAdapter, type, isChecked, ctx);
				} else {
					enableType(running, thisAdapter, type, isChecked);
				}
			}

		});

		final ImageButton moreBtn = (ImageButton) v.findViewById(R.id.image_button);
		if (type == WaypointHelper.TARGETS) {
			moreBtn.setVisibility(View.VISIBLE);
			moreBtn.setImageDrawable(app.getIconsCache().getIcon(R.drawable.ic_overflow_menu_white, !nightMode));
			moreBtn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					boolean hasActivePoints = false;
					if (thisAdapter instanceof StableArrayAdapter) {
						List<Object> items = ((StableArrayAdapter) thisAdapter).getActiveObjects();
						if (items.size() > 0) {
							if (items.size() > 1) {
								hasActivePoints = true;
							} else {
								Object item = items.get(0);
								if (item instanceof LocationPointWrapper) {
									LocationPointWrapper w = (LocationPointWrapper) item;
									if (w.getPoint() instanceof TargetPoint) {
										hasActivePoints = !((TargetPoint) w.point).start;
									}
								} else {
									hasActivePoints = true;
								}
							}
						}
					}

					final PopupMenu optionsMenu = new PopupMenu(ctx, moreBtn);
					DirectionsDialogs.setupPopUpMenuIcon(optionsMenu);
					MenuItem item;
					if (hasActivePoints) {
						item = optionsMenu.getMenu()
								.add(R.string.intermediate_items_sort_by_distance)
								.setIcon(app.getIconsCache().getThemedIcon(R.drawable.ic_sort_waypoint_dark));
						item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
							@Override
							public boolean onMenuItemClick(MenuItem item) {
								// sort door-to-door
								sortAllTargets(app, ctx, helper);
								return true;
							}
						});

						item = optionsMenu.getMenu()
								.add(R.string.switch_start_finish)
								.setIcon(app.getIconsCache().getThemedIcon(R.drawable.ic_action_undo_dark));
						item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
							@Override
							public boolean onMenuItemClick(MenuItem item) {
								TargetPointsHelper targetPointsHelper = app.getTargetPointsHelper();
								switchStartAndFinish(targetPointsHelper, targetPointsHelper.getPointToNavigate(),
										ctx, targetPointsHelper.getPointToStart(), app, helper);
								return true;
							}
						});
					}
					if (optionsMenu.getMenu().size() > 0) {
						optionsMenu.show();
					}
				}
			});
		} else {
			moreBtn.setVisibility(View.GONE);
		}

		TextView tv = (TextView) v.findViewById(R.id.header_text);
		AndroidUtils.setTextPrimaryColor(mapActivity, tv, nightMode);
		tv.setText(getHeader(type, checked, ctx));
		return v;
	}

	private void selectPoi(final int[] running, final ArrayAdapter<Object> listAdapter, final int type,
						   final boolean enable, Activity ctx) {
		if (ctx instanceof MapActivity &&
				!app.getPoiFilters().isPoiFilterSelected(PoiUIFilter.CUSTOM_FILTER_ID)) {
			MapActivity map = (MapActivity) ctx;
			map.getMapLayers().showSingleChoicePoiFilterDialog(map.getMapView(),
					new MapActivityLayers.DismissListener() {
						@Override
						public void dismiss() {
							if (app.getPoiFilters().isShowingAnyPoi()) {
								enableType(running, listAdapter, type, enable);
							} else {
								running[0] = -1;
								if (helperCallbacks != null) {
									helperCallbacks.reloadAdapter();
								} else {
									reloadListAdapter(listAdapter);
								}
							}
						}
					});
		} else {
			enableType(running, listAdapter, type, enable);
		}
	}

	private void enableType(final int[] running, final ArrayAdapter<Object> listAdapter, final int type,
							final boolean enable) {
		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				app.getWaypointHelper().enableWaypointType(type, enable);
				return null;
			}

			protected void onPostExecute(Void result) {
				running[0] = -1;
				if (helperCallbacks != null) {
					helperCallbacks.reloadAdapter();
				} else {
					reloadListAdapter(listAdapter);
				}
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
	}

	public AdapterView.OnItemClickListener getDrawerItemClickListener(final FragmentActivity ctx, final int[] running,
																	  final ArrayAdapter<Object> listAdapter) {
		return new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int item, long l) {
				if (listAdapter.getItem(item) instanceof LocationPointWrapper) {
					LocationPointWrapper ps = (LocationPointWrapper) listAdapter.getItem(item);
					showOnMap(app, ctx, ps.getPoint(), false);
//				} else if (new Integer(WaypointHelper.TARGETS).equals(listAdapter.getItem(item))) {
//					IntermediatePointsDialog.openIntermediatePointsDialog(ctx, app, true);
				} else if (listAdapter.getItem(item) instanceof RadiusItem) {
					selectDifferentRadius(((RadiusItem) listAdapter.getItem(item)).type, running, item, listAdapter,
							ctx);
				}
			}
		};
	}

	protected void selectDifferentRadius(final int type, final int[] running, final int position,
										 final ArrayAdapter<Object> thisAdapter, Activity ctx) {
		int length = WaypointHelper.SEARCH_RADIUS_VALUES.length;
		String[] names = new String[length];
		int selected = 0;
		for (int i = 0; i < length; i++) {
			names[i] = OsmAndFormatter.getFormattedDistance(WaypointHelper.SEARCH_RADIUS_VALUES[i], app);
			if (WaypointHelper.SEARCH_RADIUS_VALUES[i] == waypointHelper.getSearchDeviationRadius(type)) {
				selected = i;
			}
		}
		new AlertDialog.Builder(ctx)
				.setSingleChoiceItems(names, selected, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						int value = WaypointHelper.SEARCH_RADIUS_VALUES[i];
						if (waypointHelper.getSearchDeviationRadius(type) != value) {
							running[0] = position;
							waypointHelper.setSearchDeviationRadius(type, value);
							recalculatePoints(running, thisAdapter, type);
							dialogInterface.dismiss();
							thisAdapter.notifyDataSetInvalidated();
						}
					}
				}).setTitle(app.getString(R.string.search_radius_proximity))
				.setNegativeButton(R.string.shared_string_cancel, null)
				.show();
	}

	private void recalculatePoints(final int[] running, final ArrayAdapter<Object> listAdapter, final int type) {
		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				app.getWaypointHelper().recalculatePoints(type);
				return null;
			}

			protected void onPostExecute(Void result) {
				running[0] = -1;
				if (helperCallbacks != null) {
					helperCallbacks.reloadAdapter();
				} else {
					reloadListAdapter(listAdapter);
				}
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
	}

	public void reloadListAdapter(ArrayAdapter<Object> listAdapter) {
		mapActivity.getMyApplication().getWaypointHelper().removeVisibleLocationPoint(deletedPoints);

		listAdapter.setNotifyOnChange(false);
		listAdapter.clear();
		List<Object> points = getPoints();
		for (Object point : points) {
			listAdapter.add(point);
		}
		if (listAdapter instanceof StableArrayAdapter) {
			((StableArrayAdapter) listAdapter).updateObjects(points, getActivePoints(points));
		}
		listAdapter.notifyDataSetChanged();
	}

	protected String getHeader(int type, boolean checked, Activity ctx) {
		String str = ctx.getString(R.string.waypoints);
		switch (type) {
			case WaypointHelper.TARGETS:
				str = ctx.getString(R.string.targets);
				break;
			case WaypointHelper.ALARMS:
				str = ctx.getString(R.string.way_alarms);
				break;
			case WaypointHelper.FAVORITES:
				str = ctx.getString(R.string.shared_string_my_favorites);
				break;
			case WaypointHelper.WAYPOINTS:
				str = ctx.getString(R.string.waypoints);
				break;
			case WaypointHelper.POI:
				str = ctx.getString(R.string.poi);
				break;
		}
		return str;
	}

	protected List<Object> getStandardPoints() {
		final List<Object> points = new ArrayList<>();
		boolean rc = waypointHelper.isRouteCalculated();
		for (int i = 0; i < WaypointHelper.MAX; i++) {
			List<LocationPointWrapper> tp = waypointHelper.getWaypoints(i);
			if ((rc || i == WaypointHelper.WAYPOINTS || i == WaypointHelper.TARGETS)
					&& waypointHelper.isTypeVisible(i)) {
				if (points.size() > 0) {
					points.add(true);
				}
				points.add(i);
				if (i == WaypointHelper.TARGETS) {
					TargetPoint start = app.getTargetPointsHelper().getPointToStart();
					if (start == null) {
						LatLon latLon;
						Location loc = app.getLocationProvider().getLastKnownLocation();
						if (loc != null) {
							latLon = new LatLon(loc.getLatitude(), loc.getLongitude());
						} else {
							latLon = new LatLon(mapActivity.getMapView().getLatitude(),
									mapActivity.getMapView().getLongitude());
						}
						start = TargetPoint.createStartPoint(latLon,
								new PointDescription(PointDescription.POINT_TYPE_MY_LOCATION,
										mapActivity.getString(R.string.shared_string_my_location)));

					} else {
						String oname = start.getOnlyName().length() > 0 ? start.getOnlyName()
								: (mapActivity.getString(R.string.route_descr_map_location)
								+ " " + mapActivity.getString(R.string.route_descr_lat_lon, start.getLatitude(), start.getLongitude()));

						start = TargetPoint.createStartPoint(new LatLon(start.getLatitude(), start.getLongitude()),
								new PointDescription(PointDescription.POINT_TYPE_LOCATION,
										oname));
					}
					points.add(new LocationPointWrapper(null, WaypointHelper.TARGETS, start, 0f, 0));

				} else if ((i == WaypointHelper.POI || i == WaypointHelper.FAVORITES || i == WaypointHelper.WAYPOINTS)
						&& rc) {
					if (waypointHelper.isTypeEnabled(i)) {
						points.add(new RadiusItem(i));
					}
				}
				if (tp != null && tp.size() > 0) {
					points.addAll(tp);
				}
				points.add(false);
			}
		}
		return points;
	}

	public static void showOnMap(OsmandApplication app, Activity a, LocationPoint locationPoint, boolean center) {
		if (!(a instanceof MapActivity)) {
			return;
		}
		app.getSettings().setMapLocationToShow(locationPoint.getLatitude(), locationPoint.getLongitude(),
				15, locationPoint.getPointDescription(a), false, locationPoint);
		MapActivity.launchMapActivityMoveToTop(a);
	}

	public static void sortAllTargets(final OsmandApplication app, final Activity activity,
									  final WaypointDialogHelper helper) {

		new AsyncTask<Void, Void, int[]>() {

			ProgressDialog dlg = null;
			long startDialogTime = 0;
			List<TargetPoint> intermediates;

			protected void onPreExecute() {
				startDialogTime = System.currentTimeMillis();
				dlg = new ProgressDialog(activity);
				dlg.setTitle("");
				dlg.setMessage(activity.getResources().getString(R.string.intermediate_items_sort_by_distance));
				dlg.show();
			}

			protected int[] doInBackground(Void[] params) {

				TargetPointsHelper targets = app.getTargetPointsHelper();
				intermediates = targets.getIntermediatePointsWithTarget();

				Location cll = app.getLocationProvider().getLastKnownLocation();
				ArrayList<TargetPoint> lt = new ArrayList<>(intermediates);
				TargetPoint start;

				if (cll != null) {
					LatLon ll = new LatLon(cll.getLatitude(), cll.getLongitude());
					start = TargetPoint.create(ll, null);
				} else if (app.getTargetPointsHelper().getPointToStart() != null) {
					TargetPoint ps = app.getTargetPointsHelper().getPointToStart();
					LatLon ll = new LatLon(ps.getLatitude(), ps.getLongitude());
					start = TargetPoint.create(ll, null);
				} else {
					start = lt.get(0);
				}
				TargetPoint end = lt.remove(lt.size() - 1);
				ArrayList<LatLon> al = new ArrayList<>();
				for (TargetPoint p : lt) {
					al.add(p.point);
				}
				return new TspAnt().readGraph(al, start.point, end.point).solve();
			}

			protected void onPostExecute(int[] result) {
				if (dlg != null) {
					long t = System.currentTimeMillis();
					if (t - startDialogTime < 500) {
						app.runInUIThread(new Runnable() {
							@Override
							public void run() {
								dlg.dismiss();
							}
						}, 500 - (t - startDialogTime));
					} else {
						dlg.dismiss();
					}
				}

				List<TargetPoint> alocs = new ArrayList<>();
				for (int i : result) {
					if (i > 0) {
						TargetPoint loc = intermediates.get(i - 1);
						alocs.add(loc);
					}
				}
				intermediates.clear();
				intermediates.addAll(alocs);

				TargetPointsHelper targets = app.getTargetPointsHelper();
				List<TargetPoint> cur = targets.getIntermediatePointsWithTarget();
				boolean eq = true;
				for (int j = 0; j < cur.size() && j < intermediates.size(); j++) {
					if (cur.get(j) != intermediates.get(j)) {
						eq = false;
						break;
					}
				}
				if (!eq) {
					targets.reorderAllTargetPoints(intermediates, true);
				}
				if (helper.helperCallbacks != null) {
					helper.helperCallbacks.reloadAdapter();
				}
				updateRouteInfoMenu(activity);
			}

		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}
}