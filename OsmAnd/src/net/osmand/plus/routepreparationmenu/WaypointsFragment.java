package net.osmand.plus.routepreparationmenu;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
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
import net.osmand.StateChangedListener;
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
import net.osmand.plus.TargetPointsHelper.TargetPoint;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.WaypointDialogHelper;
import net.osmand.plus.helpers.WaypointDialogHelper.TargetOptionsBottomSheetDialogFragment;
import net.osmand.plus.helpers.WaypointHelper;
import net.osmand.plus.helpers.WaypointHelper.LocationPointWrapper;
import net.osmand.plus.views.controls.DynamicListView;
import net.osmand.plus.views.controls.DynamicListViewCallbacks;
import net.osmand.plus.views.controls.StableArrayAdapter;
import net.osmand.plus.views.controls.SwipeDismissListViewTouchListener;
import net.osmand.plus.views.controls.SwipeDismissListViewTouchListener.DismissCallbacks;
import net.osmand.plus.views.controls.SwipeDismissListViewTouchListener.Undoable;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.plus.widgets.TextViewExProgress;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;

import static net.osmand.plus.helpers.WaypointDialogHelper.showOnMap;

public class WaypointsFragment extends BaseOsmAndFragment implements ObservableScrollViewCallbacks,
		DynamicListViewCallbacks, WaypointDialogHelper.WaypointDialogHelperCallback, AddPointBottomSheetDialog.DialogListener {

	public static final String TAG = "WaypointsFragment";
	public static final String USE_ROUTE_INFO_MENU_KEY = "use_route_info_menu_key";

	private View view;
	private View mainView;
	private DynamicListView listView;

	private StableArrayAdapter listAdapter;
	private AdapterView.OnItemClickListener listAdapterOnClickListener;
	private SwipeDismissListViewTouchListener swipeDismissListener;

	private StateChangedListener<Void> onStateChangedListener;

	private CountDownTimer cTimer = null;

	private final int[] running = new int[]{-1};

	private boolean portrait;
	private boolean nightMode;
	private boolean wasDrawerDisabled;

	private boolean useRouteInfoMenu;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, final Bundle savedInstanceState) {
		MapActivity mapActivity = (MapActivity) requireActivity();
		portrait = AndroidUiHelper.isOrientationPortrait(mapActivity);
		nightMode = mapActivity.getMyApplication().getDaynightHelper().isNightModeForMapControls();

		view = inflater.inflate(R.layout.route_waypoints_fragment, parent, false);
		if (view == null) {
			return null;
		}
		AndroidUtils.addStatusBarPadding21v(mapActivity, view);
		Bundle args = getArguments();
		if (args == null) {
			args = savedInstanceState;
		}
		if (args != null) {
			useRouteInfoMenu = args.getBoolean(USE_ROUTE_INFO_MENU_KEY, false);
		}
		mainView = view.findViewById(R.id.main_view);

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

		view.findViewById(R.id.sort_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				boolean hasActivePoints = false;
				List<Object> items = listAdapter.getActiveObjects();
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

				if (hasActivePoints) {
					MapActivity mapActivity = getMapActivity();
					if (mapActivity != null) {
						TargetOptionsBottomSheetDialogFragment fragment = new TargetOptionsBottomSheetDialogFragment();
						fragment.setUsedOnMap(true);
						fragment.show(mapActivity.getSupportFragmentManager(), TargetOptionsBottomSheetDialogFragment.TAG);
					}
				}
			}
		});

		if (!portrait) {
			final TypedValue typedValueAttr = new TypedValue();
			mapActivity.getTheme().resolveAttribute(R.attr.left_menu_view_bg, typedValueAttr, true);
			mainView.setBackgroundResource(typedValueAttr.resourceId);
			mainView.setLayoutParams(new FrameLayout.LayoutParams(getResources().getDimensionPixelSize(R.dimen.dashboard_land_width), ViewGroup.LayoutParams.MATCH_PARENT));
			int width = getResources().getDimensionPixelSize(R.dimen.dashboard_land_width) - getResources().getDimensionPixelSize(R.dimen.dashboard_land_shadow_width);
			FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT);

			params.gravity = Gravity.BOTTOM;
			view.findViewById(R.id.control_buttons).setLayoutParams(params);
		}

		view.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		mainView.setOnClickListener(null);

		swipeDismissListener = new SwipeDismissListViewTouchListener(
				mapActivity,
				listView,
				new DismissCallbacks() {

					@Override
					public boolean canDismiss(int position) {
						StableArrayAdapter stableAdapter = listAdapter;
						if (stableAdapter != null) {
							List<Object> activeObjects = stableAdapter.getActiveObjects();
							Object obj = stableAdapter.getItem(position);
							if (obj instanceof LocationPointWrapper) {
								LocationPointWrapper w = (LocationPointWrapper) obj;
								if (w.getPoint() instanceof TargetPoint) {
									return !((TargetPoint) w.getPoint()).start;
								}
							}
							return activeObjects.contains(obj);
						}
						return false;
					}

					@Override
					public Undoable onDismiss(final int position) {
						StableArrayAdapter stableAdapter = listAdapter;
						if (stableAdapter != null) {
							Object item = stableAdapter.getItem(position);
							stableAdapter.setNotifyOnChange(false);
							stableAdapter.remove(item);
							stableAdapter.getObjects().remove(item);
							stableAdapter.getActiveObjects().remove(item);
							stableAdapter.refreshData();
							stableAdapter.notifyDataSetChanged();
						}
						cancelTimer();
						startTimer();
						return null;
					}

					@Override
					public void onHidePopup() {
					}
				});

		final FrameLayout addButton = view.findViewById(R.id.add_button);
		addButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				MapActivity mapActivity = getMapActivity();
				if (mapActivity != null) {
					Bundle args = new Bundle();
					args.putString(AddPointBottomSheetDialog.POINT_TYPE_KEY, MapRouteInfoMenu.PointType.INTERMEDIATE.name());
					AddPointBottomSheetDialog fragment = new AddPointBottomSheetDialog();
					fragment.setArguments(args);
					fragment.setUsedOnMap(true);
					fragment.setListener(WaypointsFragment.this);
					fragment.show(mapActivity.getSupportFragmentManager(), AddPointBottomSheetDialog.TAG);
				}
			}
		});

		FrameLayout clearButton = view.findViewById(R.id.clear_all_button);
		TextView clearButtonDescr = (TextView) view.findViewById(R.id.clear_all_button_descr);
		clearButtonDescr.setText(R.string.shared_string_clear_all);
		clearButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				OsmandApplication app = getMyApplication();
				if (app != null) {
					app.getTargetPointsHelper().clearAllPoints(true);
					updateTitle();
					reloadAdapter();
				}
			}
		});

		View applyButton = view.findViewById(R.id.start_button);
		applyButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				cancelTimer();
				updateRouteCalculationProgress(0);
				applyPointsChanges();
				updateTitle();
			}
		});

		View cancelButton = view.findViewById(R.id.cancel_button);
		TextViewEx cancelTitle = (TextViewEx) view.findViewById(R.id.cancel_button_descr);
		cancelTitle.setText(R.string.shared_string_undo);
		cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				cancelTimer();
				reloadAdapter();
				updateRouteCalculationProgress(0);
				updateTitle();
			}
		});

		onStateChangedListener = new StateChangedListener<Void>() {
			@Override
			public void stateChanged(Void change) {
				reloadAdapter();
				updateTitle();
			}
		};

		updateListAdapter();
		applyDayNightMode();

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			MapRouteInfoMenu.waypointsVisible = true;
			wasDrawerDisabled = mapActivity.isDrawerDisabled();
			mapActivity.getDashboard().getWaypointDialogHelper().addHelperCallback(this);
			mapActivity.getMyApplication().getTargetPointsHelper().addListener(onStateChangedListener);
			if (!wasDrawerDisabled) {
				mapActivity.disableDrawer();
			}
			updateRouteCalculationProgress(0);
			updateControlsVisibility(false, false);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		cancelTimer();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			MapRouteInfoMenu.waypointsVisible = false;
			mapActivity.getDashboard().getWaypointDialogHelper().removeHelperCallback(this);
			mapActivity.getMyApplication().getTargetPointsHelper().removeListener(onStateChangedListener);
			if (!wasDrawerDisabled) {
				mapActivity.enableDrawer();
			}
			updateControlsVisibility(true, useRouteInfoMenu);
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		outState.putBoolean(USE_ROUTE_INFO_MENU_KEY, useRouteInfoMenu);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		onDismiss();
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
		StableArrayAdapter stableAdapter = listAdapter;
		if (stableAdapter != null) {
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

	@SuppressWarnings("unchecked")
	@Override
	public void onItemsSwapped(final List<Object> items) {
		cancelTimer();
		startTimer();
	}

	@Nullable
	private MapActivity getMapActivity() {
		return (MapActivity) getActivity();
	}

	public void applyDayNightMode() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}
		boolean landscapeLayout = !portrait;
		boolean nightMode = mapActivity.getMyApplication().getDaynightHelper().isNightModeForMapControls();
		int colorActive = ContextCompat.getColor(mapActivity, nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light);
		if (!landscapeLayout) {
			AndroidUtils.setBackground(mapActivity, mainView, nightMode, R.drawable.route_info_menu_bg_light, R.drawable.route_info_menu_bg_dark);
		} else {
			AndroidUtils.setBackground(mapActivity, mainView, nightMode, R.drawable.route_info_menu_bg_left_light, R.drawable.route_info_menu_bg_left_dark);
		}

		((TextView) view.findViewById(R.id.sort_button)).setTextColor(colorActive);
		((TextView) view.findViewById(R.id.add_button_descr)).setTextColor(colorActive);
		((TextView) view.findViewById(R.id.clear_all_button_descr)).setTextColor(colorActive);
		((TextView) view.findViewById(R.id.title)).setTextColor(
				ContextCompat.getColor(mapActivity, nightMode ? R.color.text_color_primary_dark : R.color.text_color_primary_light));

		FrameLayout addButton = view.findViewById(R.id.add_button);
		TextView addButtonDescr = (TextView) view.findViewById(R.id.add_button_descr);

		addButtonDescr.setText(R.string.shared_string_add);
		addButtonDescr.setCompoundDrawablesWithIntrinsicBounds(getPaintedContentIcon(R.drawable.ic_action_plus, colorActive), null, null, null);
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
			AndroidUtils.setBackground(mapActivity, addButton, nightMode, R.drawable.btn_border_light, R.drawable.btn_border_dark);
			AndroidUtils.setBackground(mapActivity, addButtonDescr, nightMode, R.drawable.ripple_light, R.drawable.ripple_dark);
		} else {
			AndroidUtils.setBackground(mapActivity, addButton, nightMode, R.drawable.btn_border_trans_light, R.drawable.btn_border_trans_dark);
		}

		FrameLayout clearButton = view.findViewById(R.id.clear_all_button);
		TextView clearButtonDescr = (TextView) view.findViewById(R.id.clear_all_button_descr);
		clearButtonDescr.setText(R.string.shared_string_clear_all);
		clearButtonDescr.setCompoundDrawablesWithIntrinsicBounds(getPaintedContentIcon(R.drawable.ic_action_clear_all, colorActive), null, null, null);

		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
			AndroidUtils.setBackground(mapActivity, clearButton, nightMode, R.drawable.btn_border_light, R.drawable.btn_border_dark);
			AndroidUtils.setBackground(mapActivity, clearButtonDescr, nightMode, R.drawable.ripple_light, R.drawable.ripple_dark);
		} else {
			AndroidUtils.setBackground(mapActivity, clearButtonDescr, nightMode, R.drawable.btn_border_trans_light, R.drawable.btn_border_trans_dark);
		}
		AndroidUtils.setBackground(mapActivity, view.findViewById(R.id.cancel_button), nightMode, R.color.card_and_list_background_light, R.color.card_and_list_background_dark);
		AndroidUtils.setBackground(mapActivity, view.findViewById(R.id.controls_divider), nightMode, R.color.divider_color_light, R.color.divider_color_dark);

		((TextView) view.findViewById(R.id.cancel_button_descr)).setTextColor(colorActive);

		TextViewExProgress startButtonText = (TextViewExProgress) view.findViewById(R.id.start_button_descr);
		ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.progress_bar_button);
		startButtonText.setText(getText(R.string.shared_string_apply));

		int progressTextColor = nightMode ? R.color.active_buttons_and_links_text_disabled_dark : R.color.active_buttons_and_links_text_light;
		setupRouteCalculationButtonProgressBar(progressBar, startButtonText, progressTextColor);
	}

	public void reloadListAdapter(ArrayAdapter<Object> listAdapter) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}
		WaypointDialogHelper waypointDialogHelper = mapActivity.getDashboard().getWaypointDialogHelper();
		mapActivity.getMyApplication().getWaypointHelper().removeVisibleLocationPoint(new ArrayList<LocationPointWrapper>());

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
				OsmandApplication app = getMyApplication();
				if (app != null && listAdapter.getItem(item) instanceof LocationPointWrapper) {
					LocationPointWrapper ps = (LocationPointWrapper) listAdapter.getItem(item);
					if (ps != null) {
						showOnMap(app, ctx, ps.getPoint(), false);
					}
					dismiss();
				}
			}
		};
	}

	public StableArrayAdapter getWaypointsDrawerAdapter(
			final boolean edit, final List<LocationPointWrapper> deletedPoints,
			final MapActivity ctx, final int[] running, final boolean flat, final boolean nightMode) {

		final WaypointDialogHelper waypointDialogHelper = ctx.getDashboard().getWaypointDialogHelper();

		List<Object> points = waypointDialogHelper.getTargetPoints();
		List<Object> activePoints = waypointDialogHelper.getActivePoints(points);

		final StableArrayAdapter listAdapter = new StableArrayAdapter(ctx,
				R.layout.route_waypoint_item, R.id.waypoint_text, points, activePoints) {

			@NonNull
			@Override
			public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
				View v = convertView;
				Object obj = getItem(position);
				if (obj instanceof LocationPointWrapper) {
					LocationPointWrapper point = (LocationPointWrapper) obj;
					v = updateWaypointItemView(edit, deletedPoints, ctx, v, point, this, nightMode, flat, position);
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
							&& t.getOriginalPointDescription().isSearchingAddress(ctx)) {
						GeocodingLookupService.AddressLookupRequest lookupRequest
								= new GeocodingLookupService.AddressLookupRequest(t.point, new GeocodingLookupService.OnAddressLookupResult() {
							@Override
							public void geocodingDone(String address) {
								reloadListAdapter(listAdapter);
								//updateRouteInfoMenu(ctx);
							}
						}, null);
						ctx.getMyApplication().getGeocodingLookupService().lookupAddress(lookupRequest);
					}
				}
			}
		}

		return listAdapter;
	}

	public void updateControlsVisibility(boolean visible, boolean openingRouteInfo) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			int visibility = visible ? View.VISIBLE : View.GONE;
			mapActivity.findViewById(R.id.map_center_info).setVisibility(visibility);
			mapActivity.findViewById(R.id.map_left_widgets_panel).setVisibility(visibility);
			if (!openingRouteInfo) {
				mapActivity.findViewById(R.id.map_right_widgets_panel).setVisibility(visibility);
				if (!portrait) {
					mapActivity.getMapView().setMapPositionX(visible ? 0 : 1);
				}
			}
			mapActivity.refreshMap();
		}
	}

	public void newRouteIsCalculated(boolean newRoute, ValueHolder<Boolean> showToast) {
		reloadAdapter();
		showToast.value = false;
	}

	public void updateRouteCalculationProgress(int progress) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}
		ProgressBar progressBarButton = (ProgressBar) view.findViewById(R.id.progress_bar_button);
		if (progressBarButton != null) {
			if (progressBarButton.getVisibility() != View.VISIBLE) {
				progressBarButton.setVisibility(View.VISIBLE);
			}
			progressBarButton.setProgress(progress);
		}
		TextViewExProgress textViewExProgress = (TextViewExProgress) view.findViewById(R.id.start_button_descr);
		textViewExProgress.percent = progress / 100f;
		textViewExProgress.invalidate();
	}

	private void setupRouteCalculationButtonProgressBar(@NonNull ProgressBar pb, @NonNull TextViewExProgress textProgress, @ColorRes int progressTextColor) {
		OsmandApplication app = getMyApplication();
		if (app != null) {
			int bgColor = ContextCompat.getColor(app, nightMode ? R.color.activity_background_dark : R.color.activity_background_light);
			int progressColor = ContextCompat.getColor(app, nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light);
			pb.setProgressDrawable(AndroidUtils.createProgressDrawable(bgColor, ContextCompat.getColor(app, progressTextColor)));
			textProgress.paint.setColor(progressColor);
			textProgress.setTextColor(ContextCompat.getColor(app, R.color.active_buttons_and_links_text_disabled_dark));
		}
	}

	private void setDynamicListItems(DynamicListView listView, StableArrayAdapter listAdapter) {
		listView.setItemsList(listAdapter.getObjects());
		listView.setActiveItemsList(listAdapter.getActiveObjects());
	}

	private void updateListAdapter() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}
		List<LocationPointWrapper> deletedPoints = new ArrayList<>();
		listView.setEmptyView(null);
		StableArrayAdapter listAdapter = getWaypointsDrawerAdapter(true, deletedPoints, mapActivity, running, false, nightMode);
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
		OsmandApplication app = getMyApplication();
		if (app != null && isAdded()) {
			final TextViewEx title = (TextViewEx) view.findViewById(R.id.title);
			int pointsSize = app.getTargetPointsHelper().getAllPoints().size();
			String text = getString(R.string.shared_string_target_points) + " (" + (pointsSize != 0 ? pointsSize : 1) + ")";
			title.setText(text);
		}
	}

	private void applyPointsChanges() {
		OsmandApplication app = getMyApplication();
		if (app == null) {
			return;
		}
		app.runInUIThread(new Runnable() {
			@Override
			public void run() {
				OsmandApplication app = getMyApplication();
				if (app == null || !isVisible()) {
					return;
				}
				List<TargetPoint> allTargets = new ArrayList<>();
				TargetPoint start = null;
				List<Object> items = listAdapter.getActiveObjects();
				if (items != null) {
					for (Object obj : items) {
						if (obj instanceof LocationPointWrapper) {
							LocationPointWrapper p = (LocationPointWrapper) obj;
							if (p.getPoint() instanceof TargetPoint) {
								TargetPoint t = (TargetPoint) p.getPoint();
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
					TargetPoint first = allTargets.remove(0);
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
				applyPointsChanges();
				updateTitle();
			}
		};
		cTimer.start();
	}

	private void cancelTimer() {
		if (cTimer != null)
			cTimer.cancel();
	}

	private View updateWaypointItemView(final boolean edit, final List<LocationPointWrapper> deletedPoints,
	                                           final MapActivity mapActivity, View v,
	                                           final LocationPointWrapper point,
	                                           final ArrayAdapter adapter, final boolean nightMode,
	                                           final boolean flat, final int position) {
		final OsmandApplication app = mapActivity.getMyApplication();
		final WaypointDialogHelper helper = mapActivity.getDashboard().getWaypointDialogHelper();
		if (v == null || v.findViewById(R.id.info_close) == null) {
			v = UiUtilities.getInflater(mapActivity, nightMode).inflate(R.layout.route_waypoint_item, null);
		}
		v.setBackgroundColor(ContextCompat.getColor(mapActivity, nightMode ? R.color.card_and_list_background_dark : R.color.card_and_list_background_light));
		updatePointInfoView(mapActivity, v, point, true, nightMode, edit, false);

		final ImageView move = (ImageView) v.findViewById(R.id.info_move);
		final ImageButton remove = (ImageButton) v.findViewById(R.id.info_close);
		final View topDivider = (View) v.findViewById(R.id.top_divider);

		if (!edit) {
			remove.setVisibility(View.GONE);
			move.setVisibility(View.GONE);
		} else {
			boolean targets = point.type == WaypointHelper.TARGETS;
			boolean notFlatTargets = targets && !flat;
			boolean startPoint = notFlatTargets && ((TargetPoint) point.point).start;
			final TargetPointsHelper targetPointsHelper = app.getTargetPointsHelper();
			boolean canRemove = !targets || !targetPointsHelper.getIntermediatePoints().isEmpty();

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
									WaypointDialogHelper.replaceStartWithFirstIntermediate(targetPointsHelper, mapActivity, helper);
								}
							} else {
								targetPointsHelper.setStartPoint(null, true, null);
								WaypointDialogHelper.updateControls(mapActivity, helper);
							}
						}
					});
				} else {
					remove.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							WaypointDialogHelper.deletePoint(app, mapActivity, adapter, helper, point, deletedPoints, true);
						}
					});
				}
			}

			AndroidUtils.setBackground(mapActivity, topDivider, nightMode, R.color.divider_color_light, R.color.divider_color_dark);
			topDivider.setVisibility(position != 0 ? View.VISIBLE : View.GONE);

			move.setVisibility(notFlatTargets ? View.VISIBLE : View.GONE);
			if (notFlatTargets) {
				move.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_item_move, R.color.description_font_and_bottom_sheet_icons));
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

	private static void updatePointInfoView(final MapActivity mapActivity,
	                                        View localView, final LocationPointWrapper ps,
	                                        final boolean mapCenter, final boolean nightMode,
	                                        final boolean edit, final boolean topBar) {
		final OsmandApplication app = mapActivity.getMyApplication();
		WaypointHelper wh = mapActivity.getMyApplication().getWaypointHelper();
		final LocationPoint point = ps.getPoint();
		TextView text = (TextView) localView.findViewById(R.id.waypoint_text);
		if (!topBar) {
			text.setTextColor(ContextCompat.getColor(mapActivity, nightMode ? R.color.text_color_primary_dark : R.color.text_color_primary_light));
		}
		TextView textShadow = (TextView) localView.findViewById(R.id.waypoint_text_shadow);
		if (!edit) {
			localView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					showOnMap(app, mapActivity, point, mapCenter);
				}
			});
		}
		TextView textDist = (TextView) localView.findViewById(R.id.waypoint_dist);
		textDist.setTextColor(ContextCompat.getColor(app, nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light));
		((ImageView) localView.findViewById(R.id.waypoint_icon)).setImageDrawable(ps.getDrawable(mapActivity, app, nightMode));
		int dist = -1;
		boolean startPoint = ps.type == WaypointHelper.TARGETS && ((TargetPoint) ps.point).start;
		if (!startPoint) {
			if (!wh.isRouteCalculated()) {
				dist = (int) MapUtils.getDistance(mapActivity.getMapView().getLatitude(), mapActivity.getMapView().getLongitude(),
						point.getLatitude(), point.getLongitude());
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
					int colorId = R.color.description_font_and_bottom_sheet_icons;
					textDeviation.setTextColor(ContextCompat.getColor(app, R.color.description_font_and_bottom_sheet_icons));
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
			descText.setTextColor(ContextCompat.getColor(app, R.color.description_font_and_bottom_sheet_icons));
			switch (ps.type) {
				case WaypointHelper.TARGETS:
					TargetPoint targetPoint = (TargetPoint) ps.point;
					if (targetPoint.start) {
						pointDescription = mapActivity.getResources().getString(R.string.starting_point);
					} else {
						pointDescription = getPointDescription(mapActivity, targetPoint).getTypeName();
					}
					break;

				case WaypointHelper.FAVORITES:
					FavouritePoint favPoint = (FavouritePoint) ps.point;
					pointDescription = Algorithms.isEmpty(favPoint.getCategory()) ? mapActivity.getResources().getString(R.string.shared_string_favorites) : favPoint.getCategory();
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

	private static PointDescription getPointDescription(Context ctx, TargetPoint point) {
		if (!point.intermediate) {
			return new PointDescription(PointDescription.POINT_TYPE_TARGET, ctx.getString(R.string.route_descr_destination) + ":",
					point.getOnlyName());
		} else {
			return new PointDescription(PointDescription.POINT_TYPE_TARGET, ctx.getString(R.string.intermediate_waypoint) + ": " + (point.index + 1), point.getOnlyName());
		}
	}

	public static boolean showInstance(FragmentManager fragmentManager) {
		return WaypointsFragment.showInstance(fragmentManager, false);
	}

	public static boolean showInstance(FragmentManager fragmentManager, boolean useRouteInfoMenu) {
		try {
			WaypointsFragment fragment = new WaypointsFragment();

			Bundle args = new Bundle();
			args.putBoolean(USE_ROUTE_INFO_MENU_KEY, useRouteInfoMenu);
			fragment.setArguments(args);

			fragmentManager.beginTransaction()
					.add(R.id.routeMenuContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();

			return true;

		} catch (RuntimeException e) {
			return false;
		}
	}

	private void onDismiss() {
		try {
			if (useRouteInfoMenu) {
				MapActivity mapActivity = (MapActivity) getActivity();
				if (mapActivity != null) {
					mapActivity.getMapLayers().getMapControlsLayer().showRouteInfoControlDialog();
				}
			}
		} catch (Exception e) {
			//
		}
	}

	private void dismiss() {
		try {
			MapActivity mapActivity = (MapActivity) getActivity();
			if (mapActivity != null) {
				mapActivity.getSupportFragmentManager().beginTransaction().remove(this).commitAllowingStateLoss();
			}
		} catch (Exception e) {
			//
		}
	}

	@Override
	public void onSelectOnMap(AddPointBottomSheetDialog dialog) {
		MapActivity mapActivity = (MapActivity) getActivity();
		if (mapActivity != null) {
			mapActivity.getMapRouteInfoMenu().selectOnScreen(dialog.getPointType(), true);
			useRouteInfoMenu = false;
			dismiss();
		}
	}
}