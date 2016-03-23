package net.osmand.plus.osmo;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.IconsCache;
import net.osmand.plus.NavigationService;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dashboard.DashLocationFragment;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.dashboard.tools.DashFragmentData;
import net.osmand.plus.osmo.OsMoGroupsStorage.OsMoDevice;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Denis
 * on 20.01.2015.
 */
public class DashOsMoFragment extends DashLocationFragment implements OsMoGroups.OsMoGroupsUIListener {

	public static final String TAG = "DASH_OSMO_FRAGMENT";

	private static final DashFragmentData.ShouldShowFunction SHOULD_SHOW_FUNCTION =
			new DashboardOnMap.DefaultShouldShow() {
				@Override
				public int getTitleId() {
					return R.string.osmo_plugin_name;
				}
			};
	static final DashFragmentData FRAGMENT_DATA = new DashFragmentData(
			DashOsMoFragment.TAG, DashOsMoFragment.class, SHOULD_SHOW_FUNCTION, 120, null);
	private Handler uiHandler = new Handler();

	OsMoPlugin plugin;
	private CompoundButton trackr;

	CompoundButton.OnCheckedChangeListener trackerCheckedChatgedListener = new CompoundButton.OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			plugin.getService().connect(true);

			if (isChecked) {
				if (plugin != null && plugin.getTracker() != null) {
					plugin.getTracker().enableTracker();
				}
				getMyApplication().startNavigationService(NavigationService.USED_BY_LIVE);
				//interval setting not needed here, handled centrally in app.startNavigationService
				//app.getSettings().SERVICE_OFF_INTERVAL.set(0);
			} else {
				if (plugin != null && plugin.getTracker() != null) {
					plugin.getTracker().disableTracker();
				}
				if (getMyApplication().getNavigationService() != null) {
					getMyApplication().getNavigationService()
							.stopIfNeeded(getMyApplication(), NavigationService.USED_BY_LIVE);
				}
			}

			updateStatus();
		}
	};

	@Override
	public void onCloseDash() {
		if (plugin != null && plugin.getGroups() != null) {
			plugin.getGroups().removeUiListener(this);
		}
	}

	@Override
	public View initView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		plugin = OsmandPlugin.getEnabledPlugin(OsMoPlugin.class);

		View view = getActivity().getLayoutInflater().inflate(R.layout.dash_osmo_fragment, container, false);
		view.findViewById(R.id.manage).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				launchOsMoGroupsActivity();
			}
		});
		if(plugin != null) {
			plugin.setGroupsActivity(getActivity());
		}
		setupHader(view);
		return view;
	}

	@Override
	public void onOpenDash() {
		plugin = OsmandPlugin.getEnabledPlugin(OsMoPlugin.class);
		if (plugin != null) {
			plugin.getGroups().addUiListeners(this);
			plugin.setGroupsActivity(getActivity());

			trackr.setChecked(plugin.getTracker().isEnabledTracker());
			trackr.setOnCheckedChangeListener(trackerCheckedChatgedListener);
		}
		setupOsMoView();
	}

	@Override
	public void onDetach() {
		super.onDetach();
		if (plugin != null) {
			plugin.setGroupsActivity(null);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (plugin != null) {
			plugin.setGroupsActivity(null);
		}
	}

	private void setupOsMoView() {
		View mainView = getView();

		if (plugin == null) {
			mainView.setVisibility(View.GONE);
			return;
		} else {
			mainView.setVisibility(View.VISIBLE);
		}

		updateStatus();
	}

	private void setupHader(final View header) {
		trackr = (CompoundButton) header.findViewById(R.id.card_content).findViewById(R.id.toggle_item);

		ImageButton share = (ImageButton) header.findViewById(R.id.share);
		IconsCache cache = getMyApplication().getIconsCache();
		share.setImageDrawable(cache.getContentIcon(R.drawable.ic_action_gshare_dark));
		share.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				OsMoGroupsActivity.shareSessionUrl(plugin, getActivity());
			}
		});
		updateStatus();
	}

	private void updateStatus() {

		View header = getView();
		if (getView() == null ) {
			return;
		}

		View cardContent = header.findViewById(R.id.card_content);
		View enableOsmo = header.findViewById(R.id.header_layout).findViewById(R.id.toggle_item);
		View manage = header.findViewById(R.id.manage);
		if (plugin != null && plugin.getService().isEnabled() ) {
			cardContent.setVisibility(View.VISIBLE);
			enableOsmo.setVisibility(View.GONE);
			manage.setVisibility(View.VISIBLE);
		} else {
			cardContent.setVisibility(View.GONE);
			enableOsmo.setVisibility(View.VISIBLE);
			manage.setVisibility(View.GONE);
			getClearContentList(header);
			return;
		}

		CompoundButton trackr = (CompoundButton) header.findViewById(R.id.toggle_item);
		if (plugin != null && plugin.getTracker() != null) {
			trackr.setChecked(plugin.getTracker().isEnabledTracker());
		}

		updateConnectedDevices(header);
	}

	private void updateConnectedDevices(View mainView) {
		OsMoGroups grps = plugin.getGroups();

		LinearLayout contentList = getClearContentList(mainView);
		ArrayList<OsMoGroupsStorage.OsMoGroup> groups = new ArrayList<>(grps.getGroups());

		List<OsMoGroupsStorage.OsMoDevice> devices = getOsMoDevices(groups);
		setupDeviceViews(contentList, devices);
	}

	private List<OsMoGroupsStorage.OsMoDevice> getOsMoDevices(ArrayList<OsMoGroupsStorage.OsMoGroup> groups) {
		String trackerId = plugin.getService().getMyGroupTrackerId();
		List<OsMoGroupsStorage.OsMoDevice> devices = new ArrayList<>();
		if (groups.size() > 0) {
			for (OsMoGroupsStorage.OsMoGroup grp : groups) {
				for (OsMoGroupsStorage.OsMoDevice device : grp.getVisibleGroupUsers(trackerId)) {
					devices.add(device);
				}
			}
		}

		//remove all inactive devices
		Iterator<OsMoDevice> it = devices.iterator();
		while (it.hasNext()) {
			if (devices.size() < 4) {
				break;
			}
			OsMoGroupsStorage.OsMoDevice device = it.next();
			if (!device.isActive() && !device.isEnabled() && devices.size() > 2) {
				it.remove();
			}

		}

		sortDevices(devices);

		if (devices.size() > 3) {
			while (devices.size() > 3) {
				devices.remove(devices.size() - 1);
			}
		}

		return devices;
	}

	private void sortDevices(List<OsMoGroupsStorage.OsMoDevice> devices) {
		try {
			Collections.sort(devices, new Comparator<OsMoDevice>() {

				@Override
				public int compare(OsMoDevice lhs, OsMoDevice rhs) {
					Location ll = lhs.getLastLocation();
					Location rl = rhs.getLastLocation();
					double maxDist = 50000;
					double ld = ll == null || lastUpdatedLocation == null ? maxDist :
							MapUtils.getDistance(lastUpdatedLocation, ll.getLatitude(), ll.getLongitude());
					double rd = ll == null || lastUpdatedLocation == null ? maxDist :
						MapUtils.getDistance(lastUpdatedLocation, rl.getLatitude(), rl.getLongitude());
					if(ld == rd) {
						return lhs.getVisibleName().compareTo(rhs.getVisibleName());
					}
					return Double.compare(ld, rd);
				}
			});
		} catch (RuntimeException e) {
			// sorting could be unstable due to location change
			e.printStackTrace();
		}
	}

	private LinearLayout getClearContentList(View mainView) {
		LinearLayout contentList = (LinearLayout) mainView.findViewById(R.id.items);
		contentList.removeAllViews();
		return contentList;
	}

	private void setupDeviceViews(LinearLayout contentList, List<OsMoGroupsStorage.OsMoDevice> devices) {
		LayoutInflater inflater = getActivity().getLayoutInflater();
		List<DashLocationFragment.DashLocationView> distances = new ArrayList<>();
		for (final OsMoGroupsStorage.OsMoDevice device : devices) {
			View v = inflater.inflate(R.layout.dash_osmo_item, null, false);
			v.findViewById(R.id.people_icon).setVisibility(View.GONE);
			v.findViewById(R.id.people_count).setVisibility(View.GONE);
			v.findViewById(R.id.show_on_map).setVisibility(View.GONE);
			final String name = device.getVisibleName();
			final Location loc = device.getLastLocation();

			ImageView direction = (ImageView) v.findViewById(R.id.direction_icon);
			direction.setVisibility(View.VISIBLE);
			TextView label = (TextView) v.findViewById(R.id.distance);
			DashLocationFragment.DashLocationView dv = new DashLocationFragment.DashLocationView(direction, label, loc != null ? new LatLon(loc.getLatitude(),
					loc.getLongitude()) : null);
			distances.add(dv);

			final CompoundButton enableDevice = (CompoundButton) v.findViewById(R.id.toggle_item);
			enableDevice.setVisibility(View.GONE);
			ImageView icon = (ImageView) v.findViewById(R.id.icon);
			if (device.isEnabled()) {
				icon.setImageDrawable(getMyApplication().getIconsCache().
						getPaintedContentIcon(R.drawable.ic_person, device.getColor()));
			} else {
				icon.setImageDrawable(getMyApplication().getIconsCache().
						getContentIcon(R.drawable.ic_person));
			}

			((TextView) v.findViewById(R.id.name)).setText(name);
			v.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (loc == null || !device.isEnabled()) {
						launchOsMoGroupsActivity();
					} else {
						MapActivity.getSingleMapViewTrackingUtilities().setMapLinkedToLocation(false);
						getMyApplication().getSettings().setMapLocationToShow(loc.getLatitude(), loc.getLongitude(), getMyApplication().getSettings().getLastKnownMapZoom(),
								new PointDescription(PointDescription.POINT_TYPE_MARKER, device.getVisibleName()), false,
								device);
						OsMoPositionLayer.setFollowTrackerId(device, loc);
						MapActivity.launchMapActivityMoveToTop(getActivity());
					}
				}
			});
			contentList.addView(v);
		}
		this.distances = distances;
	}


	private void launchOsMoGroupsActivity() {
		Intent intent = new Intent(getActivity(), OsMoGroupsActivity.class);
		getActivity().startActivity(intent);
		closeDashboard();
	}

	@Override
	public void groupsListChange(String operation, OsMoGroupsStorage.OsMoGroup group) {
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				updateStatus();
			}
		});
	}

	@Override
	public void deviceLocationChanged(OsMoGroupsStorage.OsMoDevice device) {
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				updateConnectedDevices(getView());
				updateAllWidgets();
			}
		});
	}
}
