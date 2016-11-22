package net.osmand.core.samples.android.sample1.mapcontextmenu;

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
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.Location;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.Utilities;
import net.osmand.core.samples.android.sample1.IconsCache;
import net.osmand.core.samples.android.sample1.MainActivity;
import net.osmand.core.samples.android.sample1.R;
import net.osmand.core.samples.android.sample1.SampleApplication;
import net.osmand.core.samples.android.sample1.SampleUtils;
import net.osmand.core.samples.android.sample1.mapcontextmenu.MenuController.MenuState;
import net.osmand.core.samples.android.sample1.search.QuickSearchListAdapter;
import net.osmand.core.samples.android.sample1.view.SingleTapConfirm;
import net.osmand.data.LatLon;
import net.osmand.data.QuadPoint;
import net.osmand.data.RotatedTileBox;
import net.osmand.util.Algorithms;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;
import static net.osmand.core.samples.android.sample1.mapcontextmenu.MenuBuilder.SHADOW_HEIGHT_TOP_DP;


public class MapContextMenuFragment extends Fragment {
	public static final String TAG = "MapContextMenuFragment";

	public static final float FAB_PADDING_TOP_DP = 4f;
	public static final float MARKER_PADDING_DP = 20f;
	public static final float MARKER_PADDING_X_DP = 50f;
	public static final float SKIP_HALF_SCREEN_STATE_KOEF = .21f;
	public static final int ZOOM_IN_STANDARD = 16;

	private View view;
	private View mainView;

	private MapContextMenu menu;
	private MenuController.TitleButtonController leftTitleButtonController;
	private MenuController.TitleButtonController rightTitleButtonController;
	private MenuController.TitleButtonController topRightTitleButtonController;

	private int menuTopViewHeight;
	private int menuTopShadowHeight;
	private int menuTopShadowAllHeight;
	private int menuTitleHeight;
	private int menuBottomViewHeight;
	private int menuFullHeight;
	private int menuFullHeightMax;
	private int menuTopViewHeightExcludingTitle;
	private int menuTitleTopBottomPadding;

	private int screenHeight;
	private int viewHeight;

	private int fabPaddingTopPx;
	private int markerPaddingPx;
	private int markerPaddingXPx;

	private LatLon mapCenter;
	private int origMarkerX;
	private int origMarkerY;
	private boolean customMapCenter;
	private boolean moving;
	private boolean nightMode;
	private boolean centered;
	private boolean initLayout = true;
	private boolean zoomIn;

	private float skipHalfScreenStateLimit;

	private int screenOrientation;

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {

		screenHeight = AndroidUtils.getScreenHeight(getActivity());
		skipHalfScreenStateLimit = screenHeight * SKIP_HALF_SCREEN_STATE_KOEF;

		viewHeight = screenHeight - AndroidUtils.getStatusBarHeight(getMainActivity());

		fabPaddingTopPx = dpToPx(FAB_PADDING_TOP_DP);
		markerPaddingPx = dpToPx(MARKER_PADDING_DP);
		markerPaddingXPx = dpToPx(MARKER_PADDING_X_DP);

		menu = getMainActivity().getContextMenu();
		view = inflater.inflate(R.layout.map_context_menu_fragment, container, false);
		if (!menu.isActive()) {
			return view;
		}
		nightMode = menu.isNightMode();
		mainView = view.findViewById(R.id.context_menu_main);

		leftTitleButtonController = menu.getLeftTitleButtonController();
		rightTitleButtonController = menu.getRightTitleButtonController();
		topRightTitleButtonController = menu.getTopRightTitleButtonController();

		RotatedTileBox box = getBox();
		customMapCenter = menu.getMapCenter() != null;
		if (!customMapCenter) {
			mapCenter = box.getCenterLatLon();
			menu.setMapCenter(mapCenter);
			double markerLat = menu.getLatLon().getLatitude();
			double markerLon = menu.getLatLon().getLongitude();
			origMarkerX = (int) box.getPixXFromLatLon(markerLat, markerLon);
			origMarkerY = (int) box.getPixYFromLatLon(markerLat, markerLon);
		} else {
			mapCenter = menu.getMapCenter();
			origMarkerX = box.getCenterPixelX();
			origMarkerY = box.getCenterPixelY();
		}

		// Left title button
		final Button leftTitleButton = (Button) view.findViewById(R.id.title_button);
		if (leftTitleButtonController != null) {
			leftTitleButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					leftTitleButtonController.buttonPressed();
				}
			});
		}

		// Right title button
		final Button rightTitleButton = (Button) view.findViewById(R.id.title_button_right);
		if (rightTitleButtonController != null) {
			rightTitleButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					rightTitleButtonController.buttonPressed();
				}
			});
		}

		// Top Right title button
		final Button topRightTitleButton = (Button) view.findViewById(R.id.title_button_top_right);
		if (topRightTitleButtonController != null) {
			topRightTitleButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					topRightTitleButtonController.buttonPressed();
				}
			});
		}

		updateButtonsAndProgress();

		if (menu.isLandscapeLayout()) {
			final TypedValue typedValueAttr = new TypedValue();
			getMainActivity().getTheme().resolveAttribute(R.attr.left_menu_view_bg, typedValueAttr, true);
			mainView.setBackgroundResource(typedValueAttr.resourceId);
			mainView.setLayoutParams(new FrameLayout.LayoutParams(menu.getLandscapeWidthPx(),
					ViewGroup.LayoutParams.MATCH_PARENT));
			View fabContainer = view.findViewById(R.id.context_menu_fab_container);
			fabContainer.setLayoutParams(new FrameLayout.LayoutParams(menu.getLandscapeWidthPx(),
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
					moving = false;
					int posY = getViewY();
					if (!centered) {
						if (!zoomIn && menu.supportZoomIn()) {
							LatLon centerLatLon = getMainActivity().getScreenCenter();
							if (centerLatLon.equals(menu.getLatLon())) {
								zoomIn = true;
							}
						}
						centerMarkerLocation();
					} else if (!zoomIn && menu.supportZoomIn()) {
						int fZoom = getZoom();
						zoomIn = true;
						if (fZoom < ZOOM_IN_STANDARD) {
							/* todo animation
							AnimateDraggingMapThread thread = map.getAnimatedDraggingThread();
							thread.startZooming(ZOOM_IN_STANDARD,
									map.getZoomFractionalPart(), true);
							*/
							getMainActivity().setZoom(ZOOM_IN_STANDARD);
						}
					}
					if (hasMoved) {
						applyPosY(posY, false, false, 0, 0);
					}
					openMenuHalfScreen();
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
						moving = true;
						break;

					case MotionEvent.ACTION_MOVE:
						if (moving) {
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
						}

						break;

					case MotionEvent.ACTION_UP:
					case MotionEvent.ACTION_CANCEL:
						if (moving) {
							moving = false;
							int currentY = getViewY();

							slidingUp = Math.abs(maxVelocityY) > 500 && (currentY - dyMain) < -50;
							slidingDown = Math.abs(maxVelocityY) > 500 && (currentY - dyMain) > 50;

							velocity.recycle();

							boolean skipHalfScreenState = Math.abs(currentY - dyMain) > skipHalfScreenStateLimit;
							changeMenuState(currentY, skipHalfScreenState, slidingUp, slidingDown);
						}
						break;

				}
				return true;
			}
		};

		View topView = view.findViewById(R.id.context_menu_top_view);
		topView.setOnTouchListener(slideTouchListener);
		View topShadowView = view.findViewById(R.id.context_menu_top_shadow);
		topShadowView.setOnTouchListener(slideTouchListener);
		View topShadowAllView = view.findViewById(R.id.context_menu_top_shadow_all);
		//AndroidUtils.setBackground(getMainActivity(), topShadowAllView, nightMode, R.drawable.bg_map_context_menu_light,
		//		R.drawable.bg_map_context_menu_dark);
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

		AndroidUtils.setTextPrimaryColor(getMainActivity(),
				(TextView) view.findViewById(R.id.context_menu_line1), nightMode);
		View menuLine2 = view.findViewById(R.id.context_menu_line2);
		if (menuLine2 != null) {
			AndroidUtils.setTextSecondaryColor(getMainActivity(), (TextView) menuLine2, nightMode);
		}
		((Button) view.findViewById(R.id.title_button_top_right))
				.setTextColor(!nightMode ? getResources().getColor(R.color.map_widget_blue) : getResources().getColor(R.color.osmand_orange));
		AndroidUtils.setTextSecondaryColor(getMainActivity(),
				(TextView) view.findViewById(R.id.distance), nightMode);

		((Button) view.findViewById(R.id.title_button))
				.setTextColor(!nightMode ? getResources().getColor(R.color.map_widget_blue) : getResources().getColor(R.color.osmand_orange));
		AndroidUtils.setTextSecondaryColor(getMainActivity(),
				(TextView) view.findViewById(R.id.title_button_right_text), nightMode);
		((Button) view.findViewById(R.id.title_button_right))
				.setTextColor(!nightMode ? getResources().getColor(R.color.map_widget_blue) : getResources().getColor(R.color.osmand_orange));

		// FAB
		/*
		fabView = (ImageView) view.findViewById(R.id.context_menu_fab_view);
		if (menu.fabVisible()) {
			fabView.setImageDrawable(iconsCache.getIcon(menu.getFabIconId(), 0));
			if (menu.isLandscapeLayout()) {
				FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) fabView.getLayoutParams();
				params.setMargins(0, 0, dpToPx(28f), 0);
				fabView.setLayoutParams(params);
			}
			fabView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					menu.fabPressed();
				}
			});
		} else {
			fabView.setVisibility(View.GONE);
		}
		*/

		buildBottomView();

		view.findViewById(R.id.context_menu_bottom_scroll).setBackgroundColor(nightMode ?
				getResources().getColor(R.color.ctx_menu_info_view_bg_dark) : getResources().getColor(R.color.ctx_menu_info_view_bg_light));
		view.findViewById(R.id.context_menu_bottom_view).setBackgroundColor(nightMode ?
				getResources().getColor(R.color.ctx_menu_info_view_bg_dark) : getResources().getColor(R.color.ctx_menu_info_view_bg_light));

		return view;
	}

	public RotatedTileBox getBox() {
		return getMainActivity().getBox();
	}

	public void openMenuFullScreen() {
		changeMenuState(getViewY(), true, true, false);
	}

	public void openMenuHalfScreen() {
		int oldMenuState = menu.getCurrentMenuState();
		if (oldMenuState == MenuState.HEADER_ONLY) {
			changeMenuState(getViewY(), false, true, false);
		} else if (oldMenuState == MenuState.FULL_SCREEN && !menu.isLandscapeLayout()) {
			changeMenuState(getViewY(), false, false, true);
		}
	}

	private void changeMenuState(int currentY, boolean skipHalfScreenState,
								 boolean slidingUp, boolean slidingDown) {
		boolean needCloseMenu = false;

		int oldMenuState = menu.getCurrentMenuState();
		if (menuBottomViewHeight > 0 && slidingUp) {
			menu.slideUp();
			if (skipHalfScreenState) {
				menu.slideUp();
			}
		} else if (slidingDown) {
			needCloseMenu = !menu.slideDown();
			if (!needCloseMenu && skipHalfScreenState) {
				menu.slideDown();
			}
		}
		int newMenuState = menu.getCurrentMenuState();
		boolean needMapAdjust = false;//oldMenuState != newMenuState && newMenuState != MenuState.FULL_SCREEN;

		if (newMenuState != oldMenuState) {
			doBeforeMenuStateChange(oldMenuState, newMenuState);
		}

		applyPosY(currentY, needCloseMenu, needMapAdjust, oldMenuState, newMenuState);
	}

	private void applyPosY(final int currentY, final boolean needCloseMenu, boolean needMapAdjust,
						   final int previousMenuState, final int newMenuState) {
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

							boolean canceled = false;

							@Override
							public void onAnimationCancel(Animator animation) {
								canceled = true;
							}

							@Override
							public void onAnimationEnd(Animator animation) {
								if (!canceled) {
									if (needCloseMenu) {
										menu.close();
									} else {
										updateMainViewLayout(posY);
										if (previousMenuState != 0 && newMenuState != 0 && previousMenuState != newMenuState) {
											doAfterMenuStateChange(previousMenuState, newMenuState);
										}
									}
								}
							}
						})
						.start();

				/*
				fabView.animate().y(getFabY(posY))
						.setDuration(200)
						.setInterpolator(new DecelerateInterpolator())
						.start();
				*/

				if (needMapAdjust) {
					adjustMapPosition(posY, true, centered);
				}
			} else {
				setViewY(posY, false, needMapAdjust);
				if (needCloseMenu) {
					menu.close();
				} else {
					updateMainViewLayout(posY);
					if (previousMenuState != 0 && newMenuState != 0 && previousMenuState != newMenuState) {
						doAfterMenuStateChange(previousMenuState, newMenuState);
					}
				}
			}
		}
	}

	public void updateMapCenter(LatLon mapCenter) {
		customMapCenter = true;
		menu.setMapCenter(mapCenter);
		this.mapCenter = mapCenter;
		origMarkerX = mainView.getWidth() / 2;
		origMarkerY = mainView.getHeight() / 2;
	}

	private void updateButtonsAndProgress() {
		// Title buttons
		boolean showTitleButtonsContainer = (leftTitleButtonController != null || rightTitleButtonController != null);
		final View titleButtonsContainer = view.findViewById(R.id.title_button_container);
		titleButtonsContainer.setVisibility(showTitleButtonsContainer ? View.VISIBLE : View.GONE);

		// Left title button
		final Button leftTitleButton = (Button) view.findViewById(R.id.title_button);
		final TextView titleButtonRightText = (TextView) view.findViewById(R.id.title_button_right_text);
		if (leftTitleButtonController != null) {
			leftTitleButton.setText(leftTitleButtonController.caption);
			leftTitleButton.setVisibility(leftTitleButtonController.visible ? View.VISIBLE : View.GONE);

			Drawable leftIcon = leftTitleButtonController.getLeftIcon();
			if (leftIcon != null) {
				leftTitleButton.setCompoundDrawablesWithIntrinsicBounds(leftIcon, null, null, null);
				leftTitleButton.setCompoundDrawablePadding(dpToPx(4f));
			}

			if (leftTitleButtonController.needRightText) {
				titleButtonRightText.setText(leftTitleButtonController.rightTextCaption);
				titleButtonRightText.setVisibility(View.VISIBLE);
			} else {
				titleButtonRightText.setVisibility(View.GONE);
			}
		} else {
			leftTitleButton.setVisibility(View.GONE);
			titleButtonRightText.setVisibility(View.GONE);
		}

		// Right title button
		final Button rightTitleButton = (Button) view.findViewById(R.id.title_button_right);
		if (rightTitleButtonController != null) {
			rightTitleButton.setText(rightTitleButtonController.caption);
			rightTitleButton.setVisibility(rightTitleButtonController.visible ? View.VISIBLE : View.GONE);

			Drawable leftIcon = rightTitleButtonController.getLeftIcon();
			rightTitleButton.setCompoundDrawablesWithIntrinsicBounds(leftIcon, null, null, null);
			rightTitleButton.setCompoundDrawablePadding(dpToPx(4f));
		} else {
			rightTitleButton.setVisibility(View.GONE);
		}

		// Top Right title button
		final Button topRightTitleButton = (Button) view.findViewById(R.id.title_button_top_right);
		if (topRightTitleButtonController != null) {
			topRightTitleButton.setText(topRightTitleButtonController.caption);
			topRightTitleButton.setVisibility(topRightTitleButtonController.visible ? View.VISIBLE : View.INVISIBLE);

			Drawable leftIcon = topRightTitleButtonController.getLeftIcon();
			topRightTitleButton.setCompoundDrawablesWithIntrinsicBounds(leftIcon, null, null, null);
			topRightTitleButton.setCompoundDrawablePadding(dpToPx(4f));
		} else {
			topRightTitleButton.setVisibility(View.GONE);
		}
	}

	private void buildHeader() {
		IconsCache iconsCache = getMyApplication().getIconsCache();

		final View iconLayout = view.findViewById(R.id.context_menu_icon_layout);
		final ImageView iconView = (ImageView) view.findViewById(R.id.context_menu_icon_view);
		Drawable icon = menu.getLeftIcon();
		int iconId = menu.getLeftIconId();
		if (icon != null) {
			iconView.setImageDrawable(icon);
			iconLayout.setVisibility(View.VISIBLE);
		} else if (iconId != 0) {
			iconView.setImageDrawable(iconsCache.getOsmandIcon(iconId,
					!nightMode ? R.color.osmand_orange : R.color.osmand_orange_dark));
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
	public void onResume() {
		super.onResume();
		if (!menu.isActive()) {
			dismissMenu();
			return;
		}
		screenOrientation = SampleUtils.getScreenOrientation(getActivity());
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (mapCenter != null) {
			PointI centerI = Utilities.convertLatLonTo31(
					new net.osmand.core.jni.LatLon(mapCenter.getLatitude(), mapCenter.getLongitude()));
			getMainActivity().setTarget(centerI);
		}
		menu.setMapCenter(null);
		menu.setMapZoom(0);
	}

	public void rebuildMenu() {
		buildHeader();

		LinearLayout bottomLayout = (LinearLayout) view.findViewById(R.id.context_menu_bottom_view);
		bottomLayout.removeAllViews();
		buildBottomView();

		runLayoutListener();
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private void runLayoutListener() {
		ViewTreeObserver vto = view.getViewTreeObserver();
		vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

			@Override
			public void onGlobalLayout() {

				ViewTreeObserver obs = view.getViewTreeObserver();
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
					obs.removeOnGlobalLayoutListener(this);
				} else {
					obs.removeGlobalOnLayoutListener(this);
				}

				if (getActivity() == null) {
					return;
				}

				int newMenuTopViewHeight = view.findViewById(R.id.context_menu_top_view).getHeight();
				menuTopShadowHeight = view.findViewById(R.id.context_menu_top_shadow).getHeight();
				int newMenuTopShadowAllHeight = view.findViewById(R.id.context_menu_top_shadow_all).getHeight();
				menuFullHeight = view.findViewById(R.id.context_menu_main).getHeight();

				int dy = 0;
				if (!menu.isLandscapeLayout()) {
					TextView line1 = (TextView) view.findViewById(R.id.context_menu_line1);
					TextView line2 = (TextView) view.findViewById(R.id.context_menu_line2);
					int line2LineCount = 0;
					int line2LineHeight = 0;
					int line2MeasuredHeight = 0;
					if (line2 != null) {
						line2LineCount = line2.getLineCount();
						line2LineHeight = line2.getLineHeight();
						line2MeasuredHeight = line2.getMeasuredHeight();
					}
					if (menuTopViewHeight != 0) {
						int titleHeight = line1.getLineCount() * line1.getLineHeight() + line2LineCount * line2LineHeight + menuTitleTopBottomPadding;
						if (titleHeight < line1.getMeasuredHeight() + line2MeasuredHeight) {
							titleHeight = line1.getMeasuredHeight() + line2MeasuredHeight;
						}
						newMenuTopViewHeight = menuTopViewHeightExcludingTitle + titleHeight;
						dy = Math.max(0, newMenuTopViewHeight - menuTopViewHeight - (newMenuTopShadowAllHeight - menuTopShadowAllHeight));
					} else {
						menuTopViewHeightExcludingTitle = newMenuTopViewHeight - line1.getMeasuredHeight() - line2MeasuredHeight;
						menuTitleTopBottomPadding = (line1.getMeasuredHeight() - line1.getLineCount() * line1.getLineHeight())
								+ (line2MeasuredHeight - line2LineCount * line2LineHeight);
					}
				}
				menuTopViewHeight = newMenuTopViewHeight;
				menuTopShadowAllHeight = newMenuTopShadowAllHeight;
				menuTitleHeight = menuTopShadowHeight + menuTopShadowAllHeight + dy;
				menuBottomViewHeight = view.findViewById(R.id.context_menu_bottom_view).getHeight();

				menuFullHeightMax = menuTitleHeight + menuBottomViewHeight;

				if (origMarkerX == 0 && origMarkerY == 0) {
					origMarkerX = view.getWidth() / 2;
					origMarkerY = view.getHeight() / 2;
				}

				if (initLayout && centered) {
					centerMarkerLocation();
				}
				if (!moving) {
					doLayoutMenu();
				}
				initLayout = false;
			}

		});
	}

	public void centerMarkerLocation() {
		centered = true;
		showOnMap(menu.getLatLon(), true, true, false);
	}

	private int getZoom() {
		int zoom;
		if (zoomIn) {
			zoom = ZOOM_IN_STANDARD;
		} else {
			zoom = menu.getMapZoom();
		}
		if (zoom == 0) {
			zoom = (int)getMainActivity().getZoom();
		}
		return zoom;
	}

	private void showOnMap(LatLon latLon, boolean updateCoords, boolean needMove, boolean alreadyAdjusted) {
		//AnimateDraggingMapThread thread = map.getAnimatedDraggingThread(); todo amimation
		int fZoom = getZoom();
		double flat = latLon.getLatitude();
		double flon = latLon.getLongitude();

		RotatedTileBox cp = getBox();
		//cp.setCenterLocation(0.5f, map.getMapPosition() == OsmandSettings.BOTTOM_CONSTANT ? 0.15f : 0.5f);
		cp.setLatLonCenter(flat, flon);
		cp.setZoom(fZoom);
		flat = cp.getLatFromPixel(cp.getPixWidth() / 2, cp.getPixHeight() / 2);
		flon = cp.getLonFromPixel(cp.getPixWidth() / 2, cp.getPixHeight() / 2);

		if (updateCoords) {
			mapCenter = new LatLon(flat, flon);
			menu.setMapCenter(mapCenter);
			origMarkerX = cp.getCenterPixelX();
			origMarkerY = cp.getCenterPixelY();
		}

		if (!alreadyAdjusted) {
			LatLon adjustedLatLon = getAdjustedMarkerLocation(getPosY(), new LatLon(flat, flon), true, fZoom);
			flat = adjustedLatLon.getLatitude();
			flon = adjustedLatLon.getLongitude();
		}

		if (needMove) {
			//thread.startMoving(flat, flon, fZoom, true); todo animation
			PointI targetI = Utilities.convertLatLonTo31(new net.osmand.core.jni.LatLon(flat, flon));
			getMainActivity().setTarget(targetI);
			getMainActivity().setZoom(fZoom);
		}
	}

	private void setAddressLocation() {
		// Text line 1
		TextView line1 = (TextView) view.findViewById(R.id.context_menu_line1);
		line1.setText(menu.getTitleStr());

		// Text line 2
		LinearLayout line2layout = (LinearLayout) view.findViewById(R.id.context_menu_line2_layout);
		TextView line2 = (TextView) view.findViewById(R.id.context_menu_line2);
		if (menu.hasCustomAddressLine()) {
			line2layout.removeAllViews();
			menu.buildCustomAddressLine(line2layout);
		} else {
			String typeStr = menu.getTypeStr();
			String streetStr = menu.getStreetStr();
			StringBuilder line2Str = new StringBuilder();
			if (!Algorithms.isEmpty(typeStr)) {
				line2Str.append(typeStr);
				Drawable icon = menu.getTypeIcon();
				line2.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
				line2.setCompoundDrawablePadding(dpToPx(5f));
			}
			if (!Algorithms.isEmpty(streetStr) && !menu.displayStreetNameInTitle()) {
				if (line2Str.length() > 0) {
					line2Str.append(": ");
				}
				line2Str.append(streetStr);
			}
			line2.setText(line2Str.toString());
		}

		updateCompassVisibility();
	}

	private void updateCompassVisibility() {
		View compassView = view.findViewById(R.id.compass_layout);
		Location ll = getMyApplication().getLocationProvider().getLastKnownLocation();
		boolean gpsFixed = ll != null && System.currentTimeMillis() - ll.getTime() < 1000 * 60 * 60 * 20;
		if (gpsFixed && menu.displayDistanceDirection() && menu.getCurrentMenuState() != MenuState.FULL_SCREEN) {
			updateDistanceDirection();
			compassView.setVisibility(View.VISIBLE);
		} else {
			if (!menu.displayDistanceDirection()) {
				compassView.setVisibility(View.GONE);
			} else {
				compassView.setVisibility(View.INVISIBLE);
			}
		}
	}

	private void updateDistanceDirection() {
		TextView distanceText = (TextView) view.findViewById(R.id.distance);
		ImageView direction = (ImageView) view.findViewById(R.id.direction);

		float myHeading = menu.getHeading() == null ? 0f : menu.getHeading();
		QuickSearchListAdapter.updateLocationView(false, menu.getMyLocation(), myHeading, direction, distanceText,
				menu.getLatLon().getLatitude(), menu.getLatLon().getLongitude(), screenOrientation, getMyApplication(), getActivity());
	}

	private int getPosY() {
		return getPosY(false);
	}

	private int getPosY(boolean needCloseMenu) {
		if (needCloseMenu) {
			return screenHeight;
		}

		int destinationState;
		int minHalfY;
		if (menu.isExtended()) {
			destinationState = menu.getCurrentMenuState();
			minHalfY = viewHeight - (int) (viewHeight * menu.getHalfScreenMaxHeightKoef());
		} else {
			destinationState = MenuState.HEADER_ONLY;
			minHalfY = viewHeight;
		}

		int posY = 0;
		switch (destinationState) {
			case MenuState.HEADER_ONLY:
				posY = viewHeight - menuTitleHeight;
				break;
			case MenuState.HALF_SCREEN:
				posY = viewHeight - menuFullHeightMax;
				posY = Math.max(posY, minHalfY);
				break;
			case MenuState.FULL_SCREEN:
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
			return (int) mainView.getY();
		} else {
			return mainView.getPaddingTop();
		}
	}

	private void setViewY(int y, boolean animated, boolean adjustMapPos) {
		if (!oldAndroid()) {
			mainView.setY(y);
			//fabView.setY(getFabY(y));
		} else {
			mainView.setPadding(0, y, 0, 0);
			//fabView.setPadding(0, getFabY(y), 0, 0);
		}
		if (!customMapCenter) {
			if (adjustMapPos) {
				adjustMapPosition(y, animated, centered);
			}
		} else {
			customMapCenter = false;
		}
	}

	private void adjustMapPosition(int y, boolean animated, boolean center) {
		//map.getAnimatedDraggingThread().stopAnimatingSync(); todo animation
		LatLon latlon = getAdjustedMarkerLocation(y, menu.getLatLon(), center, getZoom());

		LatLon mapLatLon = getMainActivity().getScreenCenter();
		if (mapLatLon.getLatitude() == latlon.getLatitude() && mapLatLon.getLongitude() == latlon.getLongitude()) {
			return;
		}

		if (animated) {
			showOnMap(latlon, false, true, true);
		} else {
			PointI targetI = Utilities.convertLatLonTo31(
					new net.osmand.core.jni.LatLon(latlon.getLatitude(), latlon.getLongitude()));
			getMainActivity().setTarget(targetI);
		}
	}

	private LatLon getAdjustedMarkerLocation(int y, LatLon reqMarkerLocation, boolean center, int zoom) {
		double markerLat = reqMarkerLocation.getLatitude();
		double markerLon = reqMarkerLocation.getLongitude();
		RotatedTileBox box = getBox();
		//box.setCenterLocation(0.5f, map.getMapPosition() == OsmandSettings.BOTTOM_CONSTANT ? 0.15f : 0.5f);
		box.setZoom(zoom);
		int markerMapCenterX = (int) box.getPixXFromLatLon(mapCenter.getLatitude(), mapCenter.getLongitude());
		int markerMapCenterY = (int) box.getPixYFromLatLon(mapCenter.getLatitude(), mapCenter.getLongitude());
		float cpyOrig = box.getCenterPixelPoint().y;

		box.setCenterLocation(0.5f, 0.5f);
		int markerX = (int) box.getPixXFromLatLon(markerLat, markerLon);
		int markerY = (int) box.getPixYFromLatLon(markerLat, markerLon);
		QuadPoint cp = box.getCenterPixelPoint();
		float cpx = cp.x;
		float cpy = cp.y;

		float cpyDelta = menu.isLandscapeLayout() ? 0 : cpyOrig - cpy;

		markerY += cpyDelta;
		y += cpyDelta;
		float origMarkerY = this.origMarkerY + cpyDelta;

		LatLon latlon;
		if (center) {
			latlon = reqMarkerLocation;
		} else {
			latlon = box.getLatLonFromPixel(markerMapCenterX, markerMapCenterY);
		}
		if (menu.isLandscapeLayout()) {
			int x = menu.getLandscapeWidthPx();
			if (markerX - markerPaddingXPx < x || markerX > origMarkerX) {
				int dx = (x + markerPaddingXPx) - markerX;
				int dy = 0;
				if (center) {
					dy = (int) cpy - markerY;
				} else {
					cpy = cpyOrig;
				}
				if (dx >= 0 || center) {
					latlon = box.getLatLonFromPixel(cpx - dx, cpy - dy);
				}
			}
		} else {
			if (markerY + markerPaddingPx > y || markerY < origMarkerY) {
				int dx = 0;
				int dy = markerY - (y - markerPaddingPx);
				if (markerY - dy <= origMarkerY) {
					if (center) {
						dx = markerX - (int) cpx;
					}
					latlon = box.getLatLonFromPixel(cpx + dx, cpy + dy);
				}
			}
		}
		return latlon;
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
		setViewY(posY, true, !initLayout || !centered);
		updateMainViewLayout(posY);
	}

	public void dismissMenu() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			try {
				activity.getSupportFragmentManager().popBackStack(TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
			} catch (Exception e) {
				//ignore
			}
		}
	}

	public void refreshTitle() {
		setAddressLocation();
		runLayoutListener();
	}

	public void setFragmentVisibility(boolean visible) {
		if (visible) {
			view.setVisibility(View.VISIBLE);
			if (mapCenter != null) {
				PointI targetI = Utilities.convertLatLonTo31(
						new net.osmand.core.jni.LatLon(mapCenter.getLatitude(), mapCenter.getLongitude()));
				getMainActivity().setTarget(targetI);
			}
			adjustMapPosition(getPosY(), true, false);
		} else {
			view.setVisibility(View.GONE);
		}
	}

	public SampleApplication getMyApplication() {
		if (getActivity() == null) {
			return null;
		}
		return (SampleApplication) getActivity().getApplication();
	}

	public static boolean showInstance(final MapContextMenu menu, final MainActivity mainActivity,
									   final boolean centered) {
		try {

			if (menu.getLatLon() == null) {
				return false;
			}

			int slideInAnim = R.anim.slide_in_bottom;
			int slideOutAnim = R.anim.slide_out_bottom;

			if (menu.isExtended()) {
				slideInAnim = menu.getSlideInAnimation();
				slideOutAnim = menu.getSlideOutAnimation();
			}

			MapContextMenuFragment fragment = new MapContextMenuFragment();
			fragment.centered = centered;
			mainActivity.getSupportFragmentManager().beginTransaction()
					.setCustomAnimations(slideInAnim, slideOutAnim, slideInAnim, slideOutAnim)
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG).commitAllowingStateLoss();

			return true;

		} catch (RuntimeException e) {
			return false;
		}
	}

	public void updateMenu() {
		updateButtonsAndProgress();
		runLayoutListener();
	}

	private MainActivity getMainActivity() {
		return (MainActivity) getActivity();
	}

	private int dpToPx(float dp) {
		Resources r = getActivity().getResources();
		return (int) TypedValue.applyDimension(
				COMPLEX_UNIT_DIP,
				dp,
				r.getDisplayMetrics()
		);
	}

	public void updateLocation(boolean centerChanged, boolean locationChanged, boolean compassChanged) {
		updateDistanceDirection();
	}

	private void doBeforeMenuStateChange(int previousState, int newState) {
	}

	private void doAfterMenuStateChange(int previousState, int newState) {
		updateCompassVisibility();
	}
}

