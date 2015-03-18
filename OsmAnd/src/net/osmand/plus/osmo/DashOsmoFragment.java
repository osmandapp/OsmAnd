package net.osmand.plus.osmo;

import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.Location;
import net.osmand.data.PointDescription;
import net.osmand.plus.IconsCache;
import net.osmand.plus.NavigationService;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dashboard.DashBaseFragment;
import net.osmand.plus.helpers.FontCache;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Denis
 * on 20.01.2015.
 */
public class DashOsmoFragment extends DashBaseFragment {

	public static final String TAG = "DASH_OSMO_FRAGMENT";

	OsMoPlugin plugin;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		plugin = OsmandPlugin.getEnabledPlugin(OsMoPlugin.class);

		View view = getActivity().getLayoutInflater().inflate(R.layout.dash_osmo_fragment, container, false);
		Typeface typeface = FontCache.getRobotoMedium(getActivity());
		((TextView) view.findViewById(R.id.osmo_text)).setTypeface(typeface);
		view.findViewById(R.id.manage).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(getActivity(), OsMoGroupsActivity.class);
				getActivity().startActivity(intent);
			}
		});

		setupHader(view);
		return view;
	}

	@Override
	public void onOpenDash() {
		plugin = OsmandPlugin.getEnabledPlugin(OsMoPlugin.class);

		setupOsMoView();
	}

	private void setupOsMoView() {
		View mainView = getView();

		boolean show = plugin != null;
		if (show) {
			show = plugin.getService().isEnabled();
		}
		if (!show) {
			mainView.setVisibility(View.GONE);
			return;
		} else {
			mainView.setVisibility(View.VISIBLE);
		}
		updateStatus();
	}

	private void setupHader(final View header) {
		CompoundButton trackr = (CompoundButton) header.findViewById(R.id.check_item);

		final OsmandApplication app = getMyApplication();
		trackr.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					if (plugin != null && plugin.getTracker() != null) {
						plugin.getTracker().enableTracker();
					}
					app.startNavigationService(NavigationService.USED_BY_LIVE);
					//interval setting not needed here, handled centrally in app.startNavigationService
					//app.getSettings().SERVICE_OFF_INTERVAL.set(0);
				} else {
					if (plugin != null && plugin.getTracker() != null) {
						plugin.getTracker().disableTracker();
					}
					if (app.getNavigationService() != null) {
						app.getNavigationService().stopIfNeeded(app, NavigationService.USED_BY_LIVE);
					}
				}
				updateStatus();
			}
		});
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
		if (getView() == null) {
			return;
		}
		CompoundButton trackr = (CompoundButton) header.findViewById(R.id.check_item);
		if (plugin != null && plugin.getTracker() != null) {
			trackr.setChecked(plugin.getTracker().isEnabledTracker());
		}

		updateConnectedDevices(header);
	}

	private void updateConnectedDevices(View mainView) {
		OsMoGroups grps = plugin.getGroups();
		OsMoGroupsStorage.OsMoGroup mainGroup = null;
		for (OsMoGroupsStorage.OsMoGroup grp : grps.getGroups()) {
			if (grp.getGroupId() == null) {
				mainGroup = grp;
				break;
			}
		}
		LinearLayout contentList = (LinearLayout) mainView.findViewById(R.id.items);
		contentList.removeAllViews();
		if (mainGroup == null) {
			return;
		}

		List<OsMoGroupsStorage.OsMoDevice> devices =
				new ArrayList<>(mainGroup.getVisibleGroupUsers(plugin.getService().getMyGroupTrackerId()));

		while (devices.size() > 3){
			devices.remove(devices.size() - 1);
		}


		Drawable markerIcon = getMyApplication().getIconsCache().getContentIcon(R.drawable.ic_action_marker_dark);
		LayoutInflater inflater = getActivity().getLayoutInflater();
		for (final OsMoGroupsStorage.OsMoDevice device : devices) {
			View v = inflater.inflate(R.layout.dash_osmo_item, null, false);
			ImageButton showOnMap = (ImageButton)v.findViewById(R.id.show_on_map);
			showOnMap.setImageDrawable(markerIcon);
			final String name = device.getVisibleName();
			showOnMap.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Location loc = device.getLastLocation();
					getMyApplication().getSettings().setMapLocationToShow(loc.getLatitude(),
							loc.getLongitude(), 15,
							new PointDescription(PointDescription.POINT_TYPE_MARKER, name),
							false, device); //$NON-NLS-1$
					MapActivity.launchMapActivityMoveToTop(getActivity());
				}
			});

			((TextView)v.findViewById(R.id.name)).setText(name);
			contentList.addView(v);
		}
	}


}
