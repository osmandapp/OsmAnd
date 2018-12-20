package net.osmand.plus.routepreparationmenu;

import android.Manifest;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import net.osmand.AndroidUtils;
import net.osmand.Location;
import net.osmand.data.RotatedTileBox;
import net.osmand.osm.edit.Node;
import net.osmand.plus.LockableViewPager;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.routeCards.PublicTransportCard;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.routing.TransportRoutingHelper;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.router.TransportRoutePlanner;

import java.util.ArrayList;
import java.util.List;

public class ChooseRouteFragment extends BaseOsmAndFragment {

	public static final String TAG = "ChooseRouteFragment";

	private OsmandMapTileView map;
	private MapActivity mapActivity;

	private View view;
	private LockableViewPager viewPager;
	private ImageButton myLocButtonView;

	private boolean portrait;
	private boolean nightMode;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		mapActivity = (MapActivity) getActivity();
		nightMode = mapActivity.getMyApplication().getDaynightHelper().isNightModeForMapControls();
		portrait = AndroidUiHelper.isOrientationPortrait(mapActivity);
		map = getMapActivity().getMapView();

		view = inflater.inflate(R.layout.fragment_show_all_routes, null);

		AndroidUtils.addStatusBarPadding21v(mapActivity, view);

		viewPager = view.findViewById(R.id.pager);

		final List<PublicTransportCard> routeCards = new ArrayList<>();
		List<TransportRoutePlanner.TransportRouteResult> routes = getMyApplication().getTransportRoutingHelper().getRoutes();
		for (int i = 0; i < routes.size(); i++) {
			PublicTransportCard card = new PublicTransportCard(mapActivity, routes.get(i), i);
			card.setSecondButtonVisible(true);
			card.setShowTopShadow(false);
			card.setShowBottomShadow(false);
			routeCards.add(card);
		}

		viewPager.setClipToPadding(false);
		final ViewsPagerAdapter pagerAdapter = new ViewsPagerAdapter(mapActivity, routeCards);
		viewPager.setAdapter(pagerAdapter);
		viewPager.setSwipeLocked(routeCards.size() < 2);
		viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			public void onPageScrollStateChanged(int state) {
			}

			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
			}

			public void onPageSelected(int position) {
				mapActivity.getMyApplication().getTransportRoutingHelper().setCurrentRoute(routeCards.get(position).getRouteId());
				mapActivity.refreshMap();
			}
		});

		if (!portrait) {
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, AndroidUtils.dpToPx(getMyApplication(), 200f));
			params.gravity = Gravity.BOTTOM;
			viewPager.setLayoutParams(params);
		}

		ImageButton backButtonView = (ImageButton) view.findViewById(R.id.back_button);
		backButtonView.setImageDrawable(getContentIcon(R.drawable.ic_arrow_back));
		AndroidUtils.setBackground(mapActivity, backButtonView, nightMode, R.drawable.btn_circle_trans, R.drawable.btn_circle_night);
		backButtonView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss(mapActivity);
			}
		});

		View fabButtonsView = view.findViewById(R.id.fab_container);
		ImageButton zoomInButtonView = (ImageButton) view.findViewById(R.id.map_zoom_in_button);
		ImageButton zoomOutButtonView = (ImageButton) view.findViewById(R.id.map_zoom_out_button);
		myLocButtonView = (ImageButton) view.findViewById(R.id.map_my_location_button);
		if (portrait) {
			updateImageButton(zoomInButtonView, R.drawable.map_zoom_in, R.drawable.map_zoom_in_night,
					R.drawable.btn_circle_trans, R.drawable.btn_circle_night, nightMode);
			updateImageButton(zoomOutButtonView, R.drawable.map_zoom_out, R.drawable.map_zoom_out_night,
					R.drawable.btn_circle_trans, R.drawable.btn_circle_night, nightMode);
			zoomInButtonView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					doZoomIn();
				}
			});
			zoomOutButtonView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					doZoomOut();
				}
			});
			myLocButtonView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (OsmAndLocationProvider.isLocationPermissionAvailable(mapActivity)) {
						mapActivity.getMapViewTrackingUtilities().backToLocationImpl();
					} else {
						ActivityCompat.requestPermissions(mapActivity,
								new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
								OsmAndLocationProvider.REQUEST_LOCATION_PERMISSION);
					}
				}
			});
			fabButtonsView.setVisibility(View.VISIBLE);
		} else {
			fabButtonsView.setVisibility(View.GONE);
		}

		updateMyLocation(mapActivity.getRoutingHelper());

		return view;
	}

	private void adjustMapPosition() {
		RoutingHelper rh = mapActivity.getRoutingHelper();
		TransportRoutingHelper transportRoutingHelper = rh.getTransportRoutingHelper();
		if (getMapActivity().getMapView() != null) {
			Location lt = rh.getLastProjection();
			if (lt == null) {
				lt = getMyApplication().getTargetPointsHelper().getPointToStartLocation();
			}
			if (lt != null) {
				double left = lt.getLongitude(), right = lt.getLongitude();
				double top = lt.getLatitude(), bottom = lt.getLatitude();
				List<Location> list = rh.getCurrentCalculatedRoute();
				for (Location l : list) {
					left = Math.min(left, l.getLongitude());
					right = Math.max(right, l.getLongitude());
					top = Math.max(top, l.getLatitude());
					bottom = Math.min(bottom, l.getLatitude());
				}
				TransportRoutePlanner.TransportRouteResult result = transportRoutingHelper.getCurrentRouteResult();
				for (TransportRoutePlanner.TransportRouteResultSegment segment : result.getSegments()) {
					for (Node n : segment.getNodes()) {
						left = Math.min(left, n.getLongitude());
						right = Math.max(right, n.getLongitude());
						top = Math.max(top, n.getLatitude());
						bottom = Math.min(bottom, n.getLatitude());
					}
				}
				List<TargetPointsHelper.TargetPoint> targetPoints = getMyApplication().getTargetPointsHelper().getIntermediatePointsWithTarget();
				for (TargetPointsHelper.TargetPoint l : targetPoints) {
					left = Math.min(left, l.getLongitude());
					right = Math.max(right, l.getLongitude());
					top = Math.max(top, l.getLatitude());
					bottom = Math.min(bottom, l.getLatitude());
				}
				RotatedTileBox tb = getMapActivity().getMapView().getCurrentRotatedTileBox().copy();
				int tileBoxWidthPx = 0;
				int tileBoxHeightPx = 0;

				if (!portrait) {
					tileBoxWidthPx = tb.getPixWidth() - view.getWidth();
				} else {
					int fHeight = viewPager.getHeight() + AndroidUtils.getStatusBarHeight(getMyApplication());
					tileBoxHeightPx = tb.getPixHeight() - fHeight;
				}
				getMapActivity().getMapView().fitRectToMap(left, right, top, bottom, tileBoxWidthPx, tileBoxHeightPx, 0);
			}
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		getMapActivity().getMapLayers().getMapControlsLayer().showMapControlsIfHidden();
		MapRouteInfoMenu.chooseRoutesVisible = true;
	}

	public void onPause() {
		super.onPause();
		MapRouteInfoMenu.chooseRoutesVisible = false;
	}

	@Override
	public int getStatusBarColorId() {
		return R.color.status_bar_transparent_gradient;
	}

	private void updateMyLocation(RoutingHelper rh) {
		Location lastKnownLocation = mapActivity.getMyApplication().getLocationProvider().getLastKnownLocation();
		boolean enabled = lastKnownLocation != null;
		boolean tracked = mapActivity.getMapViewTrackingUtilities().isMapLinkedToLocation();

		if (!enabled) {
			myLocButtonView.setImageDrawable(getContentIcon(R.drawable.map_my_location));
			AndroidUtils.setBackground(mapActivity, myLocButtonView, nightMode, R.drawable.btn_circle_trans, R.drawable.btn_circle_night);
			myLocButtonView.setContentDescription(mapActivity.getString(R.string.unknown_location));
		} else if (tracked) {
			myLocButtonView.setImageDrawable(getIcon(R.drawable.map_my_location, R.color.color_myloc_distance));
			AndroidUtils.setBackground(mapActivity, myLocButtonView, nightMode, R.drawable.btn_circle_trans, R.drawable.btn_circle_night);
		} else {
			myLocButtonView.setImageResource(R.drawable.map_my_location);
			AndroidUtils.setBackground(mapActivity, myLocButtonView, nightMode, R.drawable.btn_circle_blue, R.drawable.btn_circle_blue);
			myLocButtonView.setContentDescription(mapActivity.getString(R.string.map_widget_back_to_loc));
		}
		if (mapActivity.getMyApplication().accessibilityEnabled()) {
			myLocButtonView.setClickable(enabled && !tracked && rh.isFollowingMode());
		}
	}

	private MapActivity getMapActivity() {
		return (MapActivity) getActivity();
	}

	public void doZoomIn() {
		if (map.isZooming() && map.hasCustomMapRatio()) {
			getMapActivity().changeZoom(2, System.currentTimeMillis());
		} else {
			if (!map.hasCustomMapRatio()) {
				//setCustomMapRatio();
			}
			getMapActivity().changeZoom(1, System.currentTimeMillis());
		}
	}

	public void doZoomOut() {
		if (!map.hasCustomMapRatio()) {
			//setCustomMapRatio();
		}
		getMapActivity().changeZoom(-1, System.currentTimeMillis());
	}

	private void updateImageButton(ImageButton button, int iconLightId, int iconDarkId, int bgLightId, int bgDarkId, boolean night) {
		button.setImageDrawable(getMapActivity().getMyApplication().getUIUtilities().getIcon(night ? iconDarkId : iconLightId));
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
			button.setBackground(getMapActivity().getResources().getDrawable(night ? bgDarkId : bgLightId,
					getMapActivity().getTheme()));
		} else {
			button.setBackgroundDrawable(getMapActivity().getResources().getDrawable(night ? bgDarkId : bgLightId));
		}
	}

	@Override
	protected Drawable getContentIcon(@DrawableRes int id) {
		return getIcon(id, nightMode ? R.color.ctx_menu_info_text_dark : R.color.icon_color);
	}

	private void dismiss(MapActivity mapActivity) {
		try {
			mapActivity.getSupportFragmentManager().beginTransaction().remove(this).commitAllowingStateLoss();
		} catch (Exception e) {
			// ignore
		}
	}

	public static boolean showInstance(FragmentManager fragmentManager) {
		return showFragment(new ChooseRouteFragment(), fragmentManager);
	}

	private static boolean showFragment(ChooseRouteFragment fragment, FragmentManager fragmentManager) {
		try {
			fragment.setRetainInstance(true);
			fragmentManager.beginTransaction()
					.add(R.id.fragmentContainer, fragment, ChooseRouteFragment.TAG)
					.commitAllowingStateLoss();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private class ViewsPagerAdapter extends PagerAdapter {

		private List<PublicTransportCard> cards;
		private MapActivity mapActivity;

		ViewsPagerAdapter(MapActivity mapActivity, List<PublicTransportCard> cards) {
			this.mapActivity = mapActivity;
			this.cards = cards;
		}

		@Override
		public float getPageWidth(int position) {
			return portrait ? super.getPageWidth(position) : 0.7f;
		}

		public void setCards(List<PublicTransportCard> cards) {
			this.cards = cards;
			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			return itemsCount();
		}

		private int itemsCount() {
			return cards.size();
		}

		private View createPageView(int position) {
			return cards.get(position).build(mapActivity);
		}

		@NonNull
		@Override
		public Object instantiateItem(@NonNull ViewGroup container, final int position) {

			View view = createPageView(position);
			view.setBackgroundDrawable(null);
			view.setBackgroundResource(R.drawable.route_cards_topsides_light);
			view.findViewById(R.id.details_button).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					mapActivity.getMyApplication().getTransportRoutingHelper().setCurrentRoute(cards.get(position).getRouteId());
					mapActivity.getMapView().refreshMap(true);
				}
			});
			view.findViewById(R.id.show_button).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					adjustMapPosition();
				}
			});
			container.addView(view, 0);

			return view;
		}

		@Override
		public void destroyItem(@NonNull ViewGroup collection, int position, @NonNull Object view) {
			collection.removeView((View) view);
		}

		@Override
		public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
			return view == object;
		}
	}
}