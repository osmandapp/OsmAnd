package net.osmand.plus.routepreparationmenu;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.ListPopupWindow;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks;
import com.github.ksoichiro.android.observablescrollview.ScrollState;

import net.osmand.AndroidUtils;
import net.osmand.ValueHolder;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.LocationPoint;
import net.osmand.data.PointDescription;
import net.osmand.plus.GeocodingLookupService;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.WaypointDialogHelper;
import net.osmand.plus.helpers.WaypointHelper;
import net.osmand.plus.views.controls.DynamicListView;
import net.osmand.plus.views.controls.DynamicListViewCallbacks;
import net.osmand.plus.views.controls.StableArrayAdapter;
import net.osmand.plus.views.controls.SwipeDismissListViewTouchListener;
import net.osmand.plus.widgets.ImageViewExProgress;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.plus.widgets.TextViewExProgress;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;

import static net.osmand.plus.helpers.WaypointDialogHelper.showOnMap;

public class WaypointsFragment extends BaseOsmAndFragment implements ObservableScrollViewCallbacks,
		DynamicListViewCallbacks, WaypointDialogHelper.WaypointDialogHelperCallbacks {

	public static final String TAG = "WaypointsFragment";

	private OsmandApplication app;

	private MapActivity mapActivity;
	private WaypointDialogHelper waypointDialogHelper;

	private DynamicListView listView;
	private StableArrayAdapter listAdapter;
	private AdapterView.OnItemClickListener listAdapterOnClickListener;
	private View view;
	private CountDownTimer cTimer = null;
	private SwipeDismissListViewTouchListener swipeDismissListener;

	private final int[] running = new int[]{-1};

	private boolean portrait;
	private boolean nightMode;
	private boolean wasDrawerDisabled;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = getMyApplication();
		mapActivity = (MapActivity) getActivity();
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
		portrait = AndroidUiHelper.isOrientationPortrait(mapActivity);
		nightMode = mapActivity.getMyApplication().getDaynightHelper().isNightModeForMapControls();
		waypointDialogHelper = mapActivity.getDashboard().getWaypointDialogHelper();
		waypointDialogHelper.addHelperCallbacks(this);

		view = inflater.inflate(R.layout.route_waypoints_fragment, parent, false);
		if (view == null) {
			return view;
		}
		AndroidUtils.addStatusBarPadding21v(app, view);

		listView = (DynamicListView) view.findViewById(R.id.dash_list_view);
		listView.setDrawSelectorOnTop(true);
		listView.setDynamicListViewCallbacks(this);

		final ImageView backButton = (ImageView) view.findViewById(R.id.back_button);
		backButton.setImageDrawable(getContentIcon(R.drawable.ic_arrow_back));
		backButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		updateTitle();

		final TextViewEx sortButton = (TextViewEx) view.findViewById(R.id.text_button);
		sortButton.setVisibility(View.VISIBLE);
		sortButton.setTextColor(ContextCompat.getColor(app, nightMode ? R.color.color_dialog_buttons_dark : R.color.color_dialog_buttons_light));
		sortButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				boolean hasActivePoints = false;
				List<Object> items = listAdapter.getActiveObjects();
				if (items.size() > 0) {
					if (items.size() > 1) {
						hasActivePoints = true;
					} else {
						Object item = items.get(0);
						if (item instanceof WaypointHelper.LocationPointWrapper) {
							WaypointHelper.LocationPointWrapper w = (WaypointHelper.LocationPointWrapper) item;
							if (w.getPoint() instanceof TargetPointsHelper.TargetPoint) {
								hasActivePoints = !((TargetPointsHelper.TargetPoint) w.point).start;
							}
						} else {
							hasActivePoints = true;
						}
					}
				}

				if (hasActivePoints) {
					WaypointDialogHelper.TargetOptionsBottomSheetDialogFragment fragment = new WaypointDialogHelper.TargetOptionsBottomSheetDialogFragment();
					fragment.setUsedOnMap(true);
					fragment.setTargetFragment(WaypointsFragment.this, 0);
					fragment.show(mapActivity.getSupportFragmentManager(), WaypointDialogHelper.TargetOptionsBottomSheetDialogFragment.TAG);
				}
			}
		});

		if (!portrait) {
			final TypedValue typedValueAttr = new TypedValue();
			mapActivity.getTheme().resolveAttribute(R.attr.left_menu_view_bg, typedValueAttr, true);
			view.findViewById(R.id.main_view).setBackgroundResource(typedValueAttr.resourceId);
			view.setLayoutParams(new FrameLayout.LayoutParams(getResources().getDimensionPixelSize(R.dimen.dashboard_land_width), ViewGroup.LayoutParams.MATCH_PARENT));
			view.findViewById(R.id.main_view).setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

			FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(AndroidUtils.dpToPx(app, 345f), ViewGroup.LayoutParams.WRAP_CONTENT);

			params.gravity = Gravity.BOTTOM;
			view.findViewById(R.id.control_buttons).setLayoutParams(params);
		}

		swipeDismissListener = new SwipeDismissListViewTouchListener(
				mapActivity,
				listView,
				new SwipeDismissListViewTouchListener.DismissCallbacks() {

					@Override
					public boolean canDismiss(int position) {
						if (listAdapter instanceof StableArrayAdapter) {
							List<Object> activeObjects = ((StableArrayAdapter) listAdapter).getActiveObjects();
							Object obj = listAdapter.getItem(position);
							if (obj instanceof WaypointHelper.LocationPointWrapper) {
								WaypointHelper.LocationPointWrapper w = (WaypointHelper.LocationPointWrapper) obj;
								if (w.getPoint() instanceof TargetPointsHelper.TargetPoint) {
									return !((TargetPointsHelper.TargetPoint) w.getPoint()).start;
								}
							}
							return activeObjects.contains(obj);
						}
						return false;
					}

					@Override
					public SwipeDismissListViewTouchListener.Undoable onDismiss(final int position) {
						final Object item;
						final StableArrayAdapter stableAdapter;
						final int activeObjPos;
						if (listAdapter instanceof StableArrayAdapter) {
							stableAdapter = (StableArrayAdapter) listAdapter;
							item = stableAdapter.getItem(position);

							stableAdapter.setNotifyOnChange(false);
							stableAdapter.remove(item);
							stableAdapter.getObjects().remove(item);
							activeObjPos = stableAdapter.getActiveObjects().indexOf(item);
							stableAdapter.getActiveObjects().remove(item);
							stableAdapter.refreshData();
							stableAdapter.notifyDataSetChanged();
						} else {
							item = null;
							stableAdapter = null;
							activeObjPos = 0;
						}
						return new SwipeDismissListViewTouchListener.Undoable() {
							@Override
							public void undo() {
								if (item != null) {
									stableAdapter.setNotifyOnChange(false);
									stableAdapter.insert(item, position);
									stableAdapter.getObjects().add(position, item);
									stableAdapter.getActiveObjects().add(activeObjPos, item);
									stableAdapter.refreshData();
									apply();
								}
							}

							@Override
							public String getTitle() {
								List<Object> activeObjects;
								if ((getMyApplication().getRoutingHelper().isRoutePlanningMode() || getMyApplication().getRoutingHelper().isFollowingMode())
										&& item != null
										&& ((activeObjects = stableAdapter.getActiveObjects()).isEmpty() || isContainsOnlyStart(activeObjects))) {
									return mapActivity.getResources().getString(R.string.cancel_navigation);
								} else {
									return null;
								}
							}
						};
					}

					@Override
					public void onHidePopup() {
						if (listAdapter instanceof StableArrayAdapter) {
							StableArrayAdapter stableAdapter = (StableArrayAdapter) listAdapter;
							stableAdapter.refreshData();
							onItemsSwapped(stableAdapter.getActiveObjects());
							List<Object> activeObjects = stableAdapter.getActiveObjects();
							if (activeObjects.isEmpty() || isContainsOnlyStart(activeObjects)) {
								mapActivity.getMapActions().stopNavigationWithoutConfirm();
								getMyApplication().getTargetPointsHelper().removeAllWayPoints(false, true);
								mapActivity.getMapLayers().getMapControlsLayer().getMapRouteInfoMenu().hide();
							}
						}
					}

					private boolean isContainsOnlyStart(List<Object> items) {
						if (items.size() == 1) {
							Object item = items.get(0);
							if (item instanceof WaypointHelper.LocationPointWrapper) {
								WaypointHelper.LocationPointWrapper w = (WaypointHelper.LocationPointWrapper) item;
								if (w.getPoint() instanceof TargetPointsHelper.TargetPoint) {
									return ((TargetPointsHelper.TargetPoint) w.getPoint()).start;
								}
							}
						}
						return false;
					}
				});

		final FrameLayout addButton = view.findViewById(R.id.add_button);
		addButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onWaypointItemClick(view.findViewById(R.id.waypoints_control_buttons));
			}
		});

		FrameLayout clearButton = view.findViewById(R.id.clear_all_button);
		TextView clearButtonDescr = (TextView) view.findViewById(R.id.clear_all_button_descr);
		clearButtonDescr.setText(R.string.shared_string_clear_all);
		clearButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				app.getTargetPointsHelper().clearAllPoints(true);
				updateTitle();
				reloadAdapter();
			}
		});

		View applyButton = view.findViewById(R.id.start_button);
		applyButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				cancelTimer();
				updateRouteCalculationProgress(100);
				apply();
			}
		});

		View cancelButton = view.findViewById(R.id.cancel_button);
		cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		updateListAdapter();
		applyDayNightMode();

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		wasDrawerDisabled = mapActivity.isDrawerDisabled();
		if (!wasDrawerDisabled) {
			mapActivity.disableDrawer();
		}
		updateRouteCalculationProgress(0);
	}

	@Override
	public void onPause() {
		super.onPause();
		cancelTimer();
		if (!wasDrawerDisabled) {
			mapActivity.enableDrawer();
		}
	}

	@Override
	public void onScrollChanged(int scrollY, boolean firstScroll, boolean dragging) {

	}

	@Override
	public void onDownMotionEvent() {

	}

	@Override
	public void onUpOrCancelMotionEvent(ScrollState scrollState) {

	}

	@Override
	public void reloadAdapter() {
		if (listAdapter != null && listAdapter instanceof StableArrayAdapter) {
			StableArrayAdapter stableAdapter = (StableArrayAdapter) listAdapter;
			reloadListAdapter(stableAdapter);
			setDynamicListItems(listView, stableAdapter);
		}
	}

	@Override
	public void onItemSwapping(int position) {
	}

	@Override
	public void onWindowVisibilityChanged(int visibility) {

	}

	@Override
	public void deleteWaypoint(int position) {
		deleteSwipeItem(position);
	}

	@Override
	public void exchangeWaypoints(int pos1, int pos2) {

	}

	@SuppressWarnings("unchecked")
	@Override
	public void onItemsSwapped(final List<Object> items) {
		cancelTimer();
		startTimer();
	}

	public void applyDayNightMode() {
		final FrameLayout addButton = view.findViewById(R.id.add_button);
		final TextView addButtonDescr = (TextView) view.findViewById(R.id.add_button_descr);
		boolean landscapeLayout = !portrait;
		boolean nightMode = app.getDaynightHelper().isNightModeForMapControls();
		if (!landscapeLayout) {
			AndroidUtils.setBackground(app, view.findViewById(R.id.main_view), nightMode, R.drawable.route_info_menu_bg_light, R.drawable.route_info_menu_bg_dark);
		} else {
			AndroidUtils.setBackground(app, view.findViewById(R.id.main_view), nightMode, R.drawable.route_info_menu_bg_left_light, R.drawable.route_info_menu_bg_left_dark);
		}

		addButtonDescr.setText(R.string.shared_string_add);
		addButtonDescr.setCompoundDrawablesWithIntrinsicBounds(getIcon(R.drawable.ic_action_plus, R.color.active_buttons_and_links_light), null, null, null);
		AndroidUtils.setBackground(app, addButton, nightMode, R.drawable.btn_border_trans_light, R.drawable.btn_border_trans_dark);
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
			AndroidUtils.setBackground(app, addButtonDescr, nightMode, R.drawable.ripple_light, R.drawable.ripple_dark);
		} else {
			AndroidUtils.setBackground(app, addButtonDescr, nightMode, R.drawable.ripple_light, R.drawable.ripple_dark);
		}

		int colorActive = ContextCompat.getColor(mapActivity, nightMode ? R.color.active_buttons_and_links_dark : R.color.active_buttons_and_links_light);
		((TextView) view.findViewById(R.id.add_button_descr)).setTextColor(colorActive);
		((TextView) view.findViewById(R.id.clear_all_button_descr)).setTextColor(colorActive);

		FrameLayout clearButton = view.findViewById(R.id.clear_all_button);
		TextView clearButtonDescr = (TextView) view.findViewById(R.id.clear_all_button_descr);
		clearButtonDescr.setText(R.string.shared_string_clear_all);
		clearButtonDescr.setCompoundDrawablesWithIntrinsicBounds(getIcon(R.drawable.ic_action_clear_all, R.color.active_buttons_and_links_light), null, null, null);
		AndroidUtils.setBackground(app, clearButton, nightMode, R.drawable.btn_border_trans_light, R.drawable.btn_border_trans_dark);

		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
			AndroidUtils.setBackground(app, clearButtonDescr, nightMode, R.drawable.ripple_light, R.drawable.ripple_dark);
		} else {
			AndroidUtils.setBackground(app, clearButtonDescr, nightMode, R.drawable.ripple_light, R.drawable.ripple_dark);
		}

		AndroidUtils.setBackground(app, view.findViewById(R.id.dividerControlButtons), nightMode,
				R.color.divider_light, R.color.divider_dark);

		((TextView) view.findViewById(R.id.cancel_button_descr)).setTextColor(
				ContextCompat.getColor(mapActivity, nightMode ? R.color.active_buttons_and_links_dark : R.color.route_info_cancel_button_color_light));

		AndroidUtils.setBackground(app, view.findViewById(R.id.cancel_button), nightMode, R.color.card_and_list_background_light, R.color.card_and_list_background_dark);
		((TextView) view.findViewById(R.id.start_button_descr)).setText(getText(R.string.shared_string_apply));
		((ImageView) view.findViewById(R.id.start_icon)).setImageResource(R.drawable.ic_action_start_navigation);

		setupRouteCalculationButtonProgressBar((ProgressBar) view.findViewById(R.id.progress_bar_button));
	}

	public void reloadListAdapter(ArrayAdapter<Object> listAdapter) {
		mapActivity.getMyApplication().getWaypointHelper().removeVisibleLocationPoint(new ArrayList<WaypointHelper.LocationPointWrapper>());

		listAdapter.setNotifyOnChange(false);
		listAdapter.clear();
		List<Object> points = waypointDialogHelper.getTargetPoints();
		for (Object point : points) {
			listAdapter.add(point);
		}
		if (listAdapter instanceof StableArrayAdapter) {
			((StableArrayAdapter) listAdapter).updateObjects(points, waypointDialogHelper.getActivePoints(points));
		}
		listAdapter.notifyDataSetChanged();
	}

	public AdapterView.OnItemClickListener getDrawerItemClickListener(final FragmentActivity ctx, final int[] running,
	                                                                  final ArrayAdapter<Object> listAdapter) {
		return new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int item, long l) {
				if (listAdapter.getItem(item) instanceof TargetPointsHelper.TargetPoint) {
					TargetPointsHelper.TargetPoint ps = (TargetPointsHelper.TargetPoint) listAdapter.getItem(item);
					showOnMap(app, ctx, ps, false);
				}
			}
		};
	}

	public StableArrayAdapter getWaypointsDrawerAdapter(
			final boolean edit, final List<WaypointHelper.LocationPointWrapper> deletedPoints,
			final MapActivity ctx, final int[] running, final boolean flat, final boolean nightMode) {

		List<Object> points = waypointDialogHelper.getTargetPoints();
		List<Object> activePoints = waypointDialogHelper.getActivePoints(points);

		final StableArrayAdapter listAdapter = new StableArrayAdapter(ctx,
				R.layout.route_waypoint_item, R.id.waypoint_text, points, activePoints) {

			@Override
			public View getView(final int position, View convertView, ViewGroup parent) {
				View v = convertView;
				Object obj = getItem(position);
				if (obj instanceof WaypointHelper.LocationPointWrapper) {
					WaypointHelper.LocationPointWrapper point = (WaypointHelper.LocationPointWrapper) obj;
					v = updateWaypointItemView(edit, deletedPoints, app, ctx, waypointDialogHelper, v, point, this, nightMode, flat, position);
					AndroidUtils.setListItemBackground(mapActivity, v, nightMode);
				}
				return v;
			}
		};

		for (Object p : points) {
			if (p instanceof WaypointHelper.LocationPointWrapper) {
				WaypointHelper.LocationPointWrapper w = (WaypointHelper.LocationPointWrapper) p;
				if (w.type == WaypointHelper.TARGETS) {
					final TargetPointsHelper.TargetPoint t = (TargetPointsHelper.TargetPoint) w.point;
					if (t.getOriginalPointDescription() != null
							&& t.getOriginalPointDescription().isSearchingAddress(mapActivity)) {
						GeocodingLookupService.AddressLookupRequest lookupRequest
								= new GeocodingLookupService.AddressLookupRequest(t.point, new GeocodingLookupService.OnAddressLookupResult() {
							@Override
							public void geocodingDone(String address) {
								reloadListAdapter(listAdapter);
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

	public void newRouteIsCalculated(boolean newRoute, ValueHolder<Boolean> showToast) {
		reloadAdapter();
		showToast.value = false;
	}

	public void updateRouteCalculationProgress(int progress) {
		ProgressBar progressBarButton = (ProgressBar) view.findViewById(R.id.progress_bar_button);
		if (progressBarButton != null) {
			if (progressBarButton.getVisibility() != View.VISIBLE) {
				progressBarButton.setVisibility(View.VISIBLE);
			}
			progressBarButton.setProgress(progress);
		}
		TextViewExProgress textViewExProgress = (TextViewExProgress) view.findViewById(R.id.start_button_descr);
		textViewExProgress.percent = progress / 100f;
		int color = nightMode ? R.color.main_font_dark : R.color.card_and_list_background_light;
		textViewExProgress.color1 = ContextCompat.getColor(mapActivity, color);
		textViewExProgress.color2 = ContextCompat.getColor(mapActivity, R.color.description_font_and_bottom_sheet_icons);
		textViewExProgress.invalidate();
		ImageViewExProgress imageViewExProgress = (ImageViewExProgress) view.findViewById(R.id.start_icon);
		imageViewExProgress.percent = progress / 100f;
		imageViewExProgress.color1 = ContextCompat.getColor(mapActivity, color);
		imageViewExProgress.color2 = ContextCompat.getColor(mapActivity, R.color.description_font_and_bottom_sheet_icons);
		imageViewExProgress.invalidate();
	}

	public void setupRouteCalculationButtonProgressBar(@NonNull ProgressBar pb) {
		int bgColor = ContextCompat.getColor(app, nightMode ? R.color.route_info_cancel_button_color_dark : R.color.activity_background_light);
		int progressColor = ContextCompat.getColor(getMyApplication(), nightMode ? R.color.active_buttons_and_links_dark : R.color.active_buttons_and_links_light);

		pb.setProgressDrawable(AndroidUtils.createProgressDrawable(bgColor, progressColor));
	}

	private void setDynamicListItems(DynamicListView listView, StableArrayAdapter listAdapter) {
		listView.setItemsList(listAdapter.getObjects());
		listView.setActiveItemsList(listAdapter.getActiveObjects());
	}

	private void updateListAdapter() {
		List<WaypointHelper.LocationPointWrapper> deletedPoints = new ArrayList<>();
		listView.setEmptyView(null);
		StableArrayAdapter listAdapter = getWaypointsDrawerAdapter(true, deletedPoints, mapActivity, running, false, false);
		AdapterView.OnItemClickListener listener = getDrawerItemClickListener(mapActivity, running, listAdapter);
		setDynamicListItems(listView, listAdapter);
		updateListAdapter(listAdapter, listener);

	}

	private void updateListAdapter(StableArrayAdapter listAdapter, AdapterView.OnItemClickListener listener) {
		this.listAdapter = listAdapter;
		listAdapterOnClickListener = listener;
		if (listView != null) {
			listView.setAdapter(listAdapter);
			if (listAdapterOnClickListener != null) {
				listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
						listAdapterOnClickListener.onItemClick(parent, view, position - listView.getHeaderViewsCount(), id);
					}
				});
			} else {
				listView.setOnItemClickListener(null);
			}
		}
	}

	private void deleteSwipeItem(int position) {
		if (swipeDismissListener != null) {
			swipeDismissListener.delete(position);
		}
	}

	private void updateTitle() {
		final TextViewEx title = (TextViewEx) view.findViewById(R.id.title);
		int pointsSize = app.getTargetPointsHelper().getAllPoints().size();
		title.setText(app.getString(R.string.waypoints, (pointsSize != 0 ? pointsSize : 1)));
	}

	private void onWaypointItemClick(View addWaypointItem) {
		if (mapActivity != null) {
			final MapRouteInfoMenu routeMenu = mapActivity.getMapLayers().getMapControlsLayer().getMapRouteInfoMenu();
			final ListPopupWindow popup = new ListPopupWindow(mapActivity);
			popup.setAnchorView(addWaypointItem);
			popup.setDropDownGravity(Gravity.END | Gravity.TOP);
			popup.setModal(true);
			popup.setAdapter(routeMenu.getIntermediatesPopupAdapter(mapActivity));
			popup.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					boolean hideDashboard = false;
					if (id == MapRouteInfoMenu.SPINNER_FAV_ID) {
						routeMenu.selectFavorite(null, false, true);
					} else if (id == MapRouteInfoMenu.SPINNER_MAP_ID) {
						hideDashboard = true;
						routeMenu.selectOnScreen(false, true);
						dismiss();
					} else if (id == MapRouteInfoMenu.SPINNER_ADDRESS_ID) {
						mapActivity.showQuickSearch(MapActivity.ShowQuickSearchMode.INTERMEDIATE_SELECTION, false);
					} else if (id == MapRouteInfoMenu.SPINNER_MAP_MARKER_MORE_ID) {
						routeMenu.selectMapMarker(-1, false, true);
					} else if (id == MapRouteInfoMenu.SPINNER_MAP_MARKER_1_ID) {
						routeMenu.selectMapMarker(0, false, true);
					} else if (id == MapRouteInfoMenu.SPINNER_MAP_MARKER_2_ID) {
						routeMenu.selectMapMarker(1, false, true);
					}
					popup.dismiss();
					if (hideDashboard) {
						mapActivity.getDashboard().hideDashboard();
					}
				}
			});
			popup.show();
		}
	}

	private void apply() {
		app.runInUIThread(new Runnable() {
			@Override
			public void run() {
				List<TargetPointsHelper.TargetPoint> allTargets = new ArrayList<>();
				TargetPointsHelper.TargetPoint start = null;
				List<Object> items = listAdapter.getActiveObjects();
				if (items != null) {
					for (Object obj : items) {
						if (obj instanceof WaypointHelper.LocationPointWrapper) {
							WaypointHelper.LocationPointWrapper p = (WaypointHelper.LocationPointWrapper) obj;
							if (p.getPoint() instanceof TargetPointsHelper.TargetPoint) {
								TargetPointsHelper.TargetPoint t = (TargetPointsHelper.TargetPoint) p.getPoint();
								if (t.start) {
									start = t;
								} else {
									t.intermediate = true;
								}
								allTargets.add(t);
							}
						}
					}
					if (allTargets.size() > 0) {
						allTargets.get(allTargets.size() - 1).intermediate = false;
					}
				}
				TargetPointsHelper targetPointsHelper = getMyApplication().getTargetPointsHelper();
				if (start != null) {
					int startInd = allTargets.indexOf(start);
					TargetPointsHelper.TargetPoint first = allTargets.remove(0);
					if (startInd != 0) {
						start.start = false;
						start.intermediate = startInd != allTargets.size() - 1;
						if (targetPointsHelper.getPointToStart() == null) {
							start.getOriginalPointDescription().setName(PointDescription
									.getLocationNamePlain(getMyApplication(), start.getLatitude(), start.getLongitude()));
						}
						first.start = true;
						first.intermediate = false;
						targetPointsHelper.setStartPoint(new LatLon(first.getLatitude(), first.getLongitude()),
								false, first.getPointDescription(getMyApplication()));
					}
				}
				targetPointsHelper.reorderAllTargetPoints(allTargets, false);
				newRouteIsCalculated(false, new ValueHolder<Boolean>());
				targetPointsHelper.updateRouteAndRefresh(true);
			}
		}, 50);
	}

	private void startTimer() {
		cTimer = new CountDownTimer(10000, 200) {

			public void onTick(long millisUntilFinished) {
				updateRouteCalculationProgress((int) ((((10000 - millisUntilFinished) / 10000f)) * 100));
			}

			public void onFinish() {
				updateRouteCalculationProgress(100);
				apply();
			}
		};
		cTimer.start();
	}

	private void cancelTimer() {
		if (cTimer != null)
			cTimer.cancel();
	}

	private static View updateWaypointItemView(final boolean edit, final List<WaypointHelper.LocationPointWrapper> deletedPoints,
	                                           final OsmandApplication app, final Activity ctx,
	                                           final WaypointDialogHelper helper, View v,
	                                           final WaypointHelper.LocationPointWrapper point,
	                                           final ArrayAdapter adapter, final boolean nightMode,
	                                           final boolean flat, final int position) {
		if (v == null || v.findViewById(R.id.info_close) == null) {
			v = ctx.getLayoutInflater().inflate(R.layout.route_waypoint_item, null);
		}
		updatePointInfoView(app, ctx, v, point, true, nightMode, edit, false);

		final ImageView move = (ImageView) v.findViewById(R.id.info_move);
		final ImageButton remove = (ImageButton) v.findViewById(R.id.info_close);
		final View topDivider = (View) v.findViewById(R.id.top_divider);

		if (!edit) {
			remove.setVisibility(View.GONE);
			move.setVisibility(View.GONE);
		} else {
			boolean targets = point.type == WaypointHelper.TARGETS;
			boolean notFlatTargets = targets && !flat;
			boolean startPoint = notFlatTargets && ((TargetPointsHelper.TargetPoint) point.point).start;
			final TargetPointsHelper targetPointsHelper = app.getTargetPointsHelper();
			boolean canRemove = !targets || !targetPointsHelper.getIntermediatePoints().isEmpty();
			int iconResId = nightMode ? R.color.marker_circle_button_color_dark : R.color.ctx_menu_title_color_dark;

			remove.setVisibility(View.VISIBLE);
			remove.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_remove_dark));
			remove.setEnabled(canRemove);
			remove.setAlpha(canRemove ? 1 : .5f);
			if (canRemove) {
				if (notFlatTargets && startPoint) {
					remove.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							if (targetPointsHelper.getPointToStart() == null) {
								if (!targetPointsHelper.getIntermediatePoints().isEmpty()) {
									helper.replaceStartWithFirstIntermediate(targetPointsHelper, ctx, helper);
								}
							} else {
								targetPointsHelper.setStartPoint(null, true, null);
								helper.updateControls(ctx, helper);
							}
						}
					});
				} else {
					remove.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							helper.deletePoint(app, ctx, adapter, helper, point, deletedPoints, true);
						}
					});
				}
			}

			AndroidUtils.setBackground(ctx, topDivider, nightMode, R.color.divider_light, R.color.divider_dark);
			topDivider.setVisibility(position != 0 ? View.VISIBLE : View.GONE);

			move.setVisibility(notFlatTargets ? View.VISIBLE : View.GONE);
			if (notFlatTargets) {
				move.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_item_move, iconResId));
				move.setTag(new DynamicListView.DragIcon() {
					@Override
					public void onClick() {
						// do nothing
					}
				});
			}
		}

		return v;
	}


	private static void updatePointInfoView(final OsmandApplication app, final Activity activity,
	                                        View localView, final WaypointHelper.LocationPointWrapper ps,
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
		boolean startPoint = ps.type == WaypointHelper.TARGETS && ((TargetPointsHelper.TargetPoint) ps.point).start;
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
								app.getUIUtilities().getIcon(R.drawable.ic_small_turn_right, colorId),
								null, null, null);
					} else {
						textDeviation.setCompoundDrawablesWithIntrinsicBounds(
								app.getUIUtilities().getIcon(R.drawable.ic_small_turn_left, colorId),
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
					TargetPointsHelper.TargetPoint targetPoint = (TargetPointsHelper.TargetPoint) ps.point;
					if (targetPoint.start) {
						pointDescription = activity.getResources().getString(R.string.starting_point);
					} else {
						pointDescription = getPointDescription(activity, targetPoint).getTypeName();
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

	private static PointDescription getPointDescription(Context ctx, TargetPointsHelper.TargetPoint point) {
		if (!point.intermediate) {
			return new PointDescription(PointDescription.POINT_TYPE_TARGET, ctx.getString(R.string.route_descr_destination) + ":",
					point.getOnlyName());
		} else {
			return new PointDescription(PointDescription.POINT_TYPE_TARGET, ctx.getString(R.string.intermediate_waypoint, "" + (point.index + 1)),
					point.getOnlyName());
		}
	}

	private void dismiss() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			try {
				activity.getSupportFragmentManager().popBackStack(TAG,
						FragmentManager.POP_BACK_STACK_INCLUSIVE);
			} catch (Exception e) {
				//
			}
		}
	}
}