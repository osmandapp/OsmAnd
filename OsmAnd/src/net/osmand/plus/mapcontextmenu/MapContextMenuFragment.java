package net.osmand.plus.mapcontextmenu;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.data.LatLon;
import net.osmand.data.QuadPoint;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.IconsCache;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.AnimateDraggingMapThread;
import net.osmand.plus.views.OsmandMapTileView;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;
import static net.osmand.plus.mapcontextmenu.MenuBuilder.SHADOW_HEIGHT_BOTTOM_DP;
import static net.osmand.plus.mapcontextmenu.MenuBuilder.SHADOW_HEIGHT_TOP_DP;


public class MapContextMenuFragment extends Fragment {

	public static final String TAG = "MapContextMenuFragment";

	public static final float FAB_PADDING_TOP_DP = 4f;
	public static final float MARKER_PADDING_DP = 20f;
	public static final float MARKER_PADDING_X_DP = 50f;

	private View view;
	private View mainView;
	ImageView fabView;

	MapContextMenu menu;

	private int menuTopViewHeight;
	private int menuTopShadowHeight;
	private int menuTopShadowAllHeight;
	private int menuTitleHeight;
	private int menuBottomViewHeight;
	private int menuFullHeight;
	private int menuFullHeightMax;

	private int fabPaddingTopPx;
	private int markerPaddingPx;
	private int markerPaddingXPx;

	private OsmandMapTileView map;
	private LatLon mapCenter;
	private int origMarkerX;
	private int origMarkerY;
	private boolean customMapCenter;

	private class SingleTapConfirm implements OnGestureListener {

		@Override
		public boolean onDown(MotionEvent e) {
			return false;
		}

		@Override
		public void onShowPress(MotionEvent e) {

		}

		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			return true;
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			return false;
		}

		@Override
		public void onLongPress(MotionEvent e) {

		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			return false;
		}


	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		menu.saveMenuState(outState);
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {

		fabPaddingTopPx = dpToPx(FAB_PADDING_TOP_DP);
		markerPaddingPx = dpToPx(MARKER_PADDING_DP);
		markerPaddingXPx = dpToPx(MARKER_PADDING_X_DP);

		menu = getMapActivity().getContextMenu();
		if (savedInstanceState != null) {
			menu.restoreMenuState(savedInstanceState);
		}

		map = getMapActivity().getMapView();
		RotatedTileBox box = map.getCurrentRotatedTileBox().copy();
		customMapCenter = menu.getMapCenter() != null;
		if (!customMapCenter) {
			mapCenter = box.getCenterLatLon();
			menu.setMapCenter(mapCenter);
			double markerLat = menu.getLatLon().getLatitude();
			double markerLon = menu.getLatLon().getLongitude();
			origMarkerX = (int)box.getPixXFromLatLon(markerLat, markerLon);
			origMarkerY = (int)box.getPixYFromLatLon(markerLat, markerLon);
		} else {
			mapCenter = menu.getMapCenter();
			origMarkerX = box.getCenterPixelX();
			origMarkerY = box.getCenterPixelY();
		}

		view = inflater.inflate(R.layout.map_context_menu_fragment, container, false);
		mainView = view.findViewById(R.id.context_menu_main);

		Button titleButton = (Button) view.findViewById(R.id.title_button);
		titleButton.setVisibility(menu.hasTitleButton() ? View.VISIBLE : View.GONE);
		if (menu.hasTitleButton()) {
			titleButton.setText(menu.getTitleButtonCaption());
			titleButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					menu.titleButtonPressed();
				}
			});
		}

		if (menu.isLandscapeLayout()) {
			mainView.setLayoutParams(new FrameLayout.LayoutParams(dpToPx(menu.getLandscapeWidthDp()),
					ViewGroup.LayoutParams.MATCH_PARENT));
			View fabContainer = view.findViewById(R.id.context_menu_fab_container);
			fabContainer.setLayoutParams(new FrameLayout.LayoutParams(dpToPx(menu.getLandscapeWidthDp()),
					ViewGroup.LayoutParams.MATCH_PARENT));
		}

		runLayoutListener();

		final GestureDetector singleTapDetector = new GestureDetector(view.getContext(), new SingleTapConfirm());

		final View.OnTouchListener slideTouchListener = new View.OnTouchListener() {
			private float dy;
			private float dyMain;
			private VelocityTracker velocity;
			private boolean slidingUp;
			private boolean slidingDown;

			private float velocityY;
			private float maxVelocityY;

			private boolean hasMoved;

			@Override
			public boolean onTouch(View v, MotionEvent event) {

				if (singleTapDetector.onTouchEvent(event)) {
					showOnMap(menu.getLatLon(), true, false);

					if (hasMoved) {
						applyPosY(getViewY(), false, false);
					}
					return true;
				}

				if (menu.isLandscapeLayout()) {
					return true;
				}

				switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						hasMoved = false;
						dy = event.getY();
						dyMain = getViewY();
						velocity = VelocityTracker.obtain();
						velocityY = 0;
						maxVelocityY = 0;
						velocity.addMovement(event);
						break;

					case MotionEvent.ACTION_MOVE:
						hasMoved = true;
						float y = event.getY();
						float newY = getViewY() + (y - dy);
						setViewY((int) newY, false, false);

						menuFullHeight = view.getHeight() - (int) newY + 10;
						if (!oldAndroid()) {
							ViewGroup.LayoutParams lp = mainView.getLayoutParams();
							lp.height = Math.max(menuFullHeight, menuTitleHeight);
							mainView.setLayoutParams(lp);
							mainView.requestLayout();
						}

						velocity.addMovement(event);
						velocity.computeCurrentVelocity(1000);
						velocityY = Math.abs(velocity.getYVelocity());
						if (velocityY > maxVelocityY)
							maxVelocityY = velocityY;

						break;

					case MotionEvent.ACTION_UP:
					case MotionEvent.ACTION_CANCEL:
						int currentY = getViewY();

						slidingUp = Math.abs(maxVelocityY) > 500 && (currentY - dyMain) < -50;
						slidingDown = Math.abs(maxVelocityY) > 500 && (currentY - dyMain) > 50;

						velocity.recycle();

						boolean needCloseMenu = false;

						int oldMenuState = menu.getCurrentMenuState();
						if (menuBottomViewHeight > 0 && slidingUp) {
							menu.slideUp();
						} else if (slidingDown) {
							needCloseMenu = !menu.slideDown();
						}
						int newMenuState = menu.getCurrentMenuState();
						boolean needMapAdjust = oldMenuState != newMenuState && newMenuState != MenuController.MenuState.FULL_SCREEN;

						applyPosY(currentY, needCloseMenu, needMapAdjust);

						break;

				}
				return true;
			}

			private void applyPosY(final int currentY, final boolean needCloseMenu, boolean needMapAdjust) {
				final int posY = getPosY(needCloseMenu);
				if (currentY != posY) {
					if (posY < currentY) {
						updateMainViewLayout(posY);
					}

					if (!oldAndroid()) {
						mainView.animate().y(posY)
								.setDuration(200)
								.setInterpolator(new DecelerateInterpolator())
								.setListener(new AnimatorListenerAdapter() {
									@Override
									public void onAnimationCancel(Animator animation) {
										if (needCloseMenu) {
											menu.close();
										} else {
											updateMainViewLayout(posY);
										}
									}

									@Override
									public void onAnimationEnd(Animator animation) {
										if (needCloseMenu) {
											menu.close();
										} else {
											updateMainViewLayout(posY);
										}
									}
								})
								.start();

						fabView.animate().y(getFabY(posY))
								.setDuration(200)
								.setInterpolator(new DecelerateInterpolator())
								.start();

						if (needMapAdjust) {
							adjustMapPosition(posY, true);
						}
					} else {
						setViewY(posY, false, needMapAdjust);
						updateMainViewLayout(posY);
					}
				}
			}
		};

		View topView = view.findViewById(R.id.context_menu_top_view);
		topView.setOnTouchListener(slideTouchListener);
		View topShadowView = view.findViewById(R.id.context_menu_top_shadow);
		topShadowView.setOnTouchListener(slideTouchListener);
		View topShadowAllView = view.findViewById(R.id.context_menu_top_shadow_all);
		topShadowAllView.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getY() <= dpToPx(SHADOW_HEIGHT_TOP_DP) || event.getAction() != MotionEvent.ACTION_DOWN)
					return slideTouchListener.onTouch(v, event);
				else
					return false;
			}
		});

		buildHeader();

		IconsCache iconsCache = getMyApplication().getIconsCache();
		boolean light = getMyApplication().getSettings().isLightContent();

		// FAB
		fabView = (ImageView)view.findViewById(R.id.context_menu_fab_view);
		fabView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				menu.fabPressed();
			}
		});

		// Action buttons
		final ImageButton buttonFavorite = (ImageButton) view.findViewById(R.id.context_menu_fav_button);
		buttonFavorite.setImageDrawable(iconsCache.getIcon(menu.getFavActionIconId(),
				light ? R.color.icon_color : R.color.dashboard_subheader_text_dark));
		buttonFavorite.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				menu.buttonFavoritePressed();
			}
		});

		final ImageButton buttonWaypoint = (ImageButton) view.findViewById(R.id.context_menu_route_button);
		buttonWaypoint.setImageDrawable(iconsCache.getIcon(R.drawable.map_action_waypoints,
				light ? R.color.icon_color : R.color.dashboard_subheader_text_dark));
		buttonWaypoint.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				menu.buttonWaypointPressed();
			}
		});

		final ImageButton buttonShare = (ImageButton) view.findViewById(R.id.context_menu_share_button);
		buttonShare.setImageDrawable(iconsCache.getIcon(R.drawable.abc_ic_menu_share_mtrl_alpha,
				light ? R.color.icon_color : R.color.dashboard_subheader_text_dark));
		buttonShare.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				menu.buttonSharePressed();
			}
		});

		final ImageButton buttonMore = (ImageButton) view.findViewById(R.id.context_menu_more_button);
		buttonMore.setImageDrawable(iconsCache.getIcon(R.drawable.ic_overflow_menu_white,
				light ? R.color.icon_color : R.color.dashboard_subheader_text_dark));
		buttonMore.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				menu.buttonMorePressed();
			}
		});

		buildBottomView();

		getMapActivity().getMapLayers().getMapControlsLayer().setControlsClickable(false);

		return view;
	}

	private void recalculateFullHeightMax() {
		menuFullHeightMax = menuTitleHeight + (menuBottomViewHeight > 0 ? menuBottomViewHeight : -dpToPx(SHADOW_HEIGHT_BOTTOM_DP));
	}

	private void buildHeader() {
		IconsCache iconsCache = getMyApplication().getIconsCache();
		boolean light = getMyApplication().getSettings().isLightContent();

		final View iconLayout = view.findViewById(R.id.context_menu_icon_layout);
		final ImageView iconView = (ImageView) view.findViewById(R.id.context_menu_icon_view);
		Drawable icon = menu.getLeftIcon();
		int iconId = menu.getLeftIconId();
		if (icon != null) {
			iconView.setImageDrawable(icon);
			iconLayout.setVisibility(View.VISIBLE);
		} else if (iconId != 0) {
			iconView.setImageDrawable(iconsCache.getIcon(iconId,
					light ? R.color.osmand_orange : R.color.osmand_orange_dark, 0.75f));
			iconLayout.setVisibility(View.VISIBLE);
		} else {
			iconLayout.setVisibility(View.GONE);
		}
		setAddressLocation();
	}

	private void buildBottomView() {
		View bottomView = view.findViewById(R.id.context_menu_bottom_view);
		if (menu.isExtended()) {
			bottomView.setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					return true;
				}
			});
			menu.build(bottomView);
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		map.setLatLon(mapCenter.getLatitude(), mapCenter.getLongitude());
		menu.setMapCenter(null);
		getMapActivity().getMapLayers().getMapControlsLayer().setControlsClickable(true);
	}

	public void rebuildMenu() {
		buildHeader();

		LinearLayout bottomLayout = (LinearLayout)view.findViewById(R.id.context_menu_bottom_view);
		bottomLayout.removeAllViews();
		buildBottomView();

		runLayoutListener();
	}

	private void runLayoutListener() {
		ViewTreeObserver vto = view.getViewTreeObserver();
		vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

			@Override
			public void onGlobalLayout() {

				int newMenuTopViewHeight = view.findViewById(R.id.context_menu_top_view).getHeight();
				menuTopShadowHeight = view.findViewById(R.id.context_menu_top_shadow).getHeight();
				menuTopShadowAllHeight = view.findViewById(R.id.context_menu_top_shadow_all).getHeight();
				menuFullHeight = view.findViewById(R.id.context_menu_main).getHeight();

				int dy = 0;
				if (!menu.isLandscapeLayout() && menuTopViewHeight != 0) {
					dy = Math.max(0, newMenuTopViewHeight - menuTopViewHeight);
				}
				menuTopViewHeight = newMenuTopViewHeight;
				menuTitleHeight = menuTopShadowHeight + menuTopShadowAllHeight + dy;
				menuBottomViewHeight = view.findViewById(R.id.context_menu_bottom_view).getHeight();

				recalculateFullHeightMax();

				ViewTreeObserver obs = view.getViewTreeObserver();

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
					obs.removeOnGlobalLayoutListener(this);
				} else {
					obs.removeGlobalOnLayoutListener(this);
				}

				if (origMarkerX == 0 && origMarkerY == 0) {
					origMarkerX = view.getWidth() / 2;
					origMarkerY = view.getHeight() / 2;
				}

				doLayoutMenu();
			}

		});
	}

	private void showOnMap(LatLon latLon, boolean updateCoords, boolean ignoreCoef) {
		AnimateDraggingMapThread thread = map.getAnimatedDraggingThread();
		int fZoom = map.getZoom();
		double flat = latLon.getLatitude();
		double flon = latLon.getLongitude();

		RotatedTileBox cp = map.getCurrentRotatedTileBox().copy();
		if (ignoreCoef) {
			cp.setCenterLocation(0.5f, 0.5f);
		} else {
			cp.setCenterLocation(0.5f, map.getMapPosition() == OsmandSettings.BOTTOM_CONSTANT ? 0.15f : 0.5f);
		}
		cp.setLatLonCenter(flat, flon);
		flat = cp.getLatFromPixel(cp.getPixWidth() / 2, cp.getPixHeight() / 2);
		flon = cp.getLonFromPixel(cp.getPixWidth() / 2, cp.getPixHeight() / 2);

		if (updateCoords) {
			mapCenter = new LatLon(flat, flon);
			menu.setMapCenter(mapCenter);
			origMarkerX = cp.getCenterPixelX();
			origMarkerY = cp.getCenterPixelY();
		}

		thread.startMoving(flat, flon, fZoom, true);
	}

	private void setAddressLocation() {
		// Text line 1
		TextView line1 = (TextView) view.findViewById(R.id.context_menu_line1);
		line1.setText(menu.getTitleStr());

		// Text line 2
		TextView line2 = (TextView) view.findViewById(R.id.context_menu_line2);
		line2.setText(menu.getLocationStr());
		Drawable icon = menu.getSecondLineIcon();
		if (icon != null) {
			line2.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
			line2.setCompoundDrawablePadding(dpToPx(5f));
		}
	}

	private int getPosY() {
		return getPosY(false);
	}

	private int getPosY(boolean needCloseMenu) {
		if (needCloseMenu) {
			return getScreenHeight();
		}

		int destinationState;
		int minHalfY;
		if (menu.isExtended()) {
			destinationState = menu.getCurrentMenuState();
			minHalfY = view.getHeight() - (int)(view.getHeight() * menu.getHalfScreenMaxHeightKoef());
		} else {
			destinationState = MenuController.MenuState.HEADER_ONLY;
			minHalfY = view.getHeight();
		}

		int posY = 0;
		switch (destinationState) {
			case MenuController.MenuState.HEADER_ONLY:
				posY = view.getHeight() - (menuTitleHeight - dpToPx(SHADOW_HEIGHT_BOTTOM_DP));
				break;
			case MenuController.MenuState.HALF_SCREEN:
				posY = view.getHeight() - menuFullHeightMax;
				posY = Math.max(posY, minHalfY);
				break;
			case MenuController.MenuState.FULL_SCREEN:
				posY = -menuTopShadowHeight - dpToPx(SHADOW_HEIGHT_TOP_DP);
				break;
			default:
				break;
		}
		return posY;
	}

	private void updateMainViewLayout(int posY) {
		menuFullHeight = view.getHeight() - posY;
		if (!oldAndroid()) {
			ViewGroup.LayoutParams lp = mainView.getLayoutParams();
			lp.height = Math.max(menuFullHeight, menuTitleHeight);
			mainView.setLayoutParams(lp);
			mainView.requestLayout();
		}
	}

	private int getViewY() {
		if (!oldAndroid()) {
			return (int)mainView.getY();
		} else {
			return mainView.getPaddingTop();
		}
	}

	private void setViewY(int y, boolean animated, boolean adjustMapPos) {
		if (!oldAndroid()) {
			mainView.setY(y);
			fabView.setY(getFabY(y));
		} else {
			mainView.setPadding(0, y, 0, 0);
			fabView.setPadding(0, getFabY(y), 0, 0);
		}
		if (!customMapCenter) {
			if (adjustMapPos) {
				adjustMapPosition(y, animated);
			}
		} else {
			customMapCenter = false;
		}
	}

	private void adjustMapPosition(int y, boolean animated) {
		double markerLat = menu.getLatLon().getLatitude();
		double markerLon = menu.getLatLon().getLongitude();
		RotatedTileBox box = map.getCurrentRotatedTileBox().copy();

		LatLon latlon = mapCenter;
		if (menu.isLandscapeLayout()) {
			int markerX = (int)box.getPixXFromLatLon(markerLat, markerLon);
			int x = dpToPx(menu.getLandscapeWidthDp());
			if (markerX - markerPaddingXPx < x || markerX > origMarkerX) {
				int dx = (x + markerPaddingXPx) - markerX;
				if (markerX - dx <= origMarkerX) {
					QuadPoint cp = box.getCenterPixelPoint();
					latlon = box.getLatLonFromPixel(cp.x - dx, cp.y);
				} else {
					latlon = box.getCenterLatLon();
				}
			}
		} else {
			int markerY = (int)box.getPixYFromLatLon(markerLat, markerLon);
			if (markerY + markerPaddingPx > y || markerY < origMarkerY) {
				int dy = markerY - (y - markerPaddingPx);
				if (markerY - dy <= origMarkerY) {
					QuadPoint cp = box.getCenterPixelPoint();
					latlon = box.getLatLonFromPixel(cp.x + 0, cp.y + dy);
				}
			}
		}

		if (map.getLatitude() == latlon.getLatitude() && map.getLongitude() == latlon.getLongitude()) {
			return;
		}

		if (animated) {
			showOnMap(latlon, false, true);
		} else {
			map.setLatLon(latlon.getLatitude(), latlon.getLongitude());
		}
	}

	private int getFabY(int y) {
		int fabY = y + fabPaddingTopPx;
		if (fabY < fabPaddingTopPx) {
			fabY = fabPaddingTopPx;
		}
		return fabY;
	}

	private boolean oldAndroid() {
		return (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH);
	}

	private void doLayoutMenu() {
		final int posY = getPosY();
		setViewY(posY, true, true);
		updateMainViewLayout(posY);
	}

	public void dismissMenu() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			activity.getSupportFragmentManager().popBackStack(TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
		}
	}

	public void refreshTitle() {
		setAddressLocation();
		runLayoutListener();
	}

	public void setFragmentVisibility(boolean visible) {
		if (visible) {
			view.setVisibility(View.VISIBLE);
			adjustMapPosition(getPosY(), true);
		} else {
			view.setVisibility(View.GONE);
		}
	}

	public OsmandApplication getMyApplication() {
		if (getActivity() == null) {
			return null;
		}
		return (OsmandApplication) getActivity().getApplication();
	}

	public static void showInstance(final MapActivity mapActivity) {

		int slideInAnim = R.anim.slide_in_bottom;
		int slideOutAnim = R.anim.slide_out_bottom;

		MapContextMenu menu = mapActivity.getContextMenu();
		if (menu.isExtended()) {
			slideInAnim = menu.getSlideInAnimation();
			slideOutAnim = menu.getSlideOutAnimation();
		}

		MapContextMenuFragment fragment = new MapContextMenuFragment();
		mapActivity.getSupportFragmentManager().beginTransaction()
				.setCustomAnimations(slideInAnim, slideOutAnim, slideInAnim, slideOutAnim)
				.add(R.id.fragmentContainer, fragment, TAG)
				.addToBackStack(TAG).commit();
	}

	private MapActivity getMapActivity() {
		return (MapActivity)getActivity();
	}

	private int dpToPx(float dp) {
		Resources r = getActivity().getResources();
		return (int) TypedValue.applyDimension(
				COMPLEX_UNIT_DIP,
				dp,
				r.getDisplayMetrics()
		);
	}

	private int getScreenHeight() {
		DisplayMetrics dm = new DisplayMetrics();
		getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
		return dm.heightPixels;
	}
}

