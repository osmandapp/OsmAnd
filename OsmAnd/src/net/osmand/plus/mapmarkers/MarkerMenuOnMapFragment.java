package net.osmand.plus.mapmarkers;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.IconsCache;
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.OsmAndLocationProvider.OsmAndCompassListener;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dashboard.DashLocationFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.util.MapUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static net.osmand.plus.OsmandSettings.LANDSCAPE_MIDDLE_RIGHT_CONSTANT;
import static net.osmand.plus.OsmandSettings.MIDDLE_TOP_CONSTANT;

public class MarkerMenuOnMapFragment extends Fragment implements OsmAndCompassListener, OsmAndLocationListener {

	public static final String TAG = "MarkerMenuOnMapFragment";

	private IconsCache iconsCache;
	private MapMarker marker;

	private boolean night;
	private boolean portrait;

	private int previousMapPosition;

	private Float heading;
	private Location location;

	private ImageView arrowIv;
	private TextView distanceTv;
	private View dividerPoint;

	public void setMarker(MapMarker marker) {
		this.marker = marker;
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final OsmandApplication app = (OsmandApplication) getActivity().getApplication();
		night = app.getDaynightHelper().isNightModeForMapControls();
		iconsCache = app.getIconsCache();
		portrait = AndroidUiHelper.isOrientationPortrait(getActivity());
		final int themeRes = night ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;

		final View mainView = View.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.fragment_marker_menu_on_map, null);
		mainView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dismiss();
			}
		});

		((ImageView) mainView.findViewById(R.id.marker_icon))
				.setImageDrawable(iconsCache.getIcon(R.drawable.ic_action_flag_dark, MapMarker.getColorId(marker.colorIndex)));
		((ImageView) mainView.findViewById(R.id.rename_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_edit_dark));
		((ImageView) mainView.findViewById(R.id.delete_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_delete_dark));

		((TextView) mainView.findViewById(R.id.marker_title)).setText(marker.getName(getActivity()));

		arrowIv = (ImageView) mainView.findViewById(R.id.marker_direction_icon);
		distanceTv = (TextView) mainView.findViewById(R.id.marker_distance);
		dividerPoint = mainView.findViewById(R.id.marker_divider_point);

		String descr;
		if ((descr = marker.groupName) != null) {
			if (descr.equals("")) {
				descr = getActivity().getString(R.string.shared_string_favorites);
			}
		} else {
			Date date = new Date(marker.creationDate);
			String month = new SimpleDateFormat("MMM", Locale.getDefault()).format(date);
			if (month.length() > 1) {
				month = Character.toUpperCase(month.charAt(0)) + month.substring(1);
			}
			month = month.replaceAll("\\.", "");
			String day = new SimpleDateFormat("d", Locale.getDefault()).format(date);
			descr = month + " " + day;
		}
		((TextView) mainView.findViewById(R.id.marker_description)).setText(descr);

		ImageButton visitedBtn = (ImageButton) mainView.findViewById(R.id.marker_visited_button);
		visitedBtn.setBackgroundDrawable(ContextCompat.getDrawable(getContext(),
				night ? R.drawable.marker_circle_background_dark_with_inset : R.drawable.marker_circle_background_light_with_inset));
		visitedBtn.setImageDrawable(iconsCache.getIcon(R.drawable.ic_action_marker_passed, night ? 0 : R.color.icon_color));
		visitedBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				app.getMapMarkersHelper().moveMapMarkerToHistory(marker);
				MapActivity mapActivity = getMapActivity();
				if (mapActivity != null) {
					Snackbar.make(mapActivity.findViewById(R.id.bottomFragmentContainer), R.string.marker_moved_to_history, Snackbar.LENGTH_LONG)
							.setAction(R.string.shared_string_undo, new View.OnClickListener() {
								@Override
								public void onClick(View view) {
									app.getMapMarkersHelper().restoreMarkerFromHistory(marker, 0);
								}
							})
							.show();
				}
				dismiss();
			}
		});

		mainView.findViewById(R.id.rename_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				MapActivity mapActivity = getMapActivity();
				if (mapActivity != null) {
					RenameMarkerBottomSheetDialogFragment fragment = new RenameMarkerBottomSheetDialogFragment();
					fragment.setMarker(marker);
					fragment.setRetainInstance(true);
					fragment.show(mapActivity.getSupportFragmentManager(), RenameMarkerBottomSheetDialogFragment.TAG);
				}
			}
		});

		mainView.findViewById(R.id.delete_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				app.getMapMarkersHelper().removeMarker(marker);
				MapActivity mapActivity = getMapActivity();
				if (mapActivity != null) {
					Snackbar.make(mapActivity.findViewById(R.id.bottomFragmentContainer), R.string.item_removed, Snackbar.LENGTH_LONG)
							.setAction(R.string.shared_string_undo, new View.OnClickListener() {
								@Override
								public void onClick(View view) {
									app.getMapMarkersHelper().addMarker(marker);
								}
							})
							.show();
				}
				dismiss();
			}
		});

		mainView.findViewById(R.id.back_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				MapActivity mapActivity = getMapActivity();
				if (mapActivity != null) {
					MapMarkersDialogFragment.showInstance(mapActivity);
				}
				dismiss();
			}
		});

		return mainView;
	}

	@Override
	public void onResume() {
		super.onResume();
		enterMenuMode();
		startLocationUpdate();
	}

	@Override
	public void onPause() {
		super.onPause();
		exitMenuMode();
		stopLocationUpdate();
	}

	@Override
	public void updateLocation(Location location) {
		boolean newLocation = this.location == null && location != null;
		boolean locationChanged = this.location != null && location != null
				&& this.location.getLatitude() != location.getLatitude()
				&& this.location.getLongitude() != location.getLongitude();
		if (newLocation || locationChanged) {
			this.location = location;
			updateLocationUi();
		}
	}

	@Override
	public void updateCompassValue(float value) {
		// 99 in next line used to one-time initialize arrows (with reference vs. fixed-north direction)
		// on non-compass devices
		float lastHeading = heading != null ? heading : 99;
		heading = value;
		if (Math.abs(MapUtils.degreesDiff(lastHeading, heading)) > 5) {
			updateLocationUi();
		} else {
			heading = lastHeading;
		}
	}

	private OsmandApplication getMyApplication() {
		if (getActivity() != null) {
			return ((MapActivity) getActivity()).getMyApplication();
		}
		return null;
	}

	private MapActivity getMapActivity() {
		return (MapActivity) getActivity();
	}

	private Drawable getContentIcon(@DrawableRes int id) {
		return iconsCache.getIcon(id, night ? R.color.ctx_menu_info_text_dark : R.color.on_map_icon_color);
	}

	private void startLocationUpdate() {
		OsmandApplication app = getMyApplication();
		if (app != null) {
			app.getLocationProvider().removeCompassListener(app.getLocationProvider().getNavigationInfo());
			app.getLocationProvider().addCompassListener(this);
			app.getLocationProvider().addLocationListener(this);
			updateLocationUi();
		}
	}

	private void stopLocationUpdate() {
		OsmandApplication app = getMyApplication();
		if (app != null) {
			app.getLocationProvider().removeLocationListener(this);
			app.getLocationProvider().removeCompassListener(this);
			app.getLocationProvider().addCompassListener(app.getLocationProvider().getNavigationInfo());
		}
	}

	private void updateLocationUi() {
		final MapActivity mapActivity = (MapActivity) getActivity();
		if (mapActivity != null) {
			mapActivity.getMyApplication().runInUIThread(new Runnable() {
				@Override
				public void run() {
					if (location == null) {
						location = mapActivity.getMyApplication().getLocationProvider().getLastKnownLocation();
					}
					if (location != null) {
						mark(View.VISIBLE, arrowIv, distanceTv, dividerPoint);
						DashLocationFragment.updateLocationView(false,
								new LatLon(location.getLatitude(), location.getLongitude()),
								heading != null ? heading : 0f,
								arrowIv,
								distanceTv,
								marker.getLatitude(),
								marker.getLongitude(),
								DashLocationFragment.getScreenOrientation(mapActivity),
								mapActivity.getMyApplication(),
								mapActivity);
					} else {
						mark(View.GONE, arrowIv, distanceTv, dividerPoint);
					}
				}
			});
		}
	}

	private void mark(int visibility, View... views) {
		for (View v : views) {
			v.setVisibility(visibility);
		}
	}

	private void enterMenuMode() {
		final MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.disableDrawer();
			mapActivity.getMapLayers().getMapControlsLayer().hideMapControls();
			OsmandMapTileView tileView = mapActivity.getMapView();
			previousMapPosition = tileView.getMapPosition();
			tileView.setMapPosition(portrait ? MIDDLE_TOP_CONSTANT : LANDSCAPE_MIDDLE_RIGHT_CONSTANT);
			mapActivity.refreshMap();
		}
	}

	private void exitMenuMode() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.enableDrawer();
			mapActivity.getMapLayers().getMapControlsLayer().showMapControls();
			mapActivity.getMapView().setMapPosition(previousMapPosition);
			mapActivity.refreshMap();
		}
	}

	public void dismiss() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getSupportFragmentManager().popBackStackImmediate(TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
		}
	}

	public static boolean showInstance(MapActivity mapActivity, @NonNull MapMarker marker) {
		try {
			MarkerMenuOnMapFragment fragment = new MarkerMenuOnMapFragment();
			fragment.setRetainInstance(true);
			fragment.setMarker(marker);
			mapActivity.getSupportFragmentManager().beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}
