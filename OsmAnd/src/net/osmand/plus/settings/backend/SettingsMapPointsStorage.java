package net.osmand.plus.settings.backend;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.api.SettingsAPI;
import net.osmand.plus.helpers.SearchHistoryHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

abstract class SettingsMapPointsStorage {

	private final OsmandSettings osmandSettings;
	private final boolean lastModifiedTimeStored;
	protected String pointsKey;
	protected String descriptionsKey;

	public SettingsMapPointsStorage(OsmandSettings osmandSettings, boolean storeLastModifiedTime) {
		this.osmandSettings = osmandSettings;
		this.lastModifiedTimeStored = storeLastModifiedTime;
	}

	protected SettingsAPI getSettingsAPI() {
		return osmandSettings.getSettingsAPI();
	}

	protected OsmandSettings getOsmandSettings() {
		return osmandSettings;
	}

	public boolean isLastModifiedTimeStored() {
		return lastModifiedTimeStored;
	}

	public long getLastModifiedTime() {
		if (!lastModifiedTimeStored) {
			throw new IllegalStateException(pointsKey + " is not granted to store last modified time");
		}
		return getSettingsAPI().getLong(osmandSettings.getGlobalPreferences(), pointsKey + "_last_modified", 0);
	}

	public void setLastModifiedTime(long lastModifiedTime) {
		if (!lastModifiedTimeStored) {
			throw new IllegalStateException(pointsKey + " is not granted to store last modified time");
		}
		getSettingsAPI().edit(osmandSettings.getGlobalPreferences())
				.putLong(pointsKey + "_last_modified", lastModifiedTime).commit();
	}

	public List<String> getPointDescriptions(int sz) {
		List<String> list = new ArrayList<>();
		String ip = getSettingsAPI().getString(osmandSettings.getGlobalPreferences(), descriptionsKey, "");
		if (ip.trim().length() > 0) {
			list.addAll(Arrays.asList(ip.split("--")));
		}
		while (list.size() > sz) {
			list.remove(list.size() - 1);
		}
		while (list.size() < sz) {
			list.add("");
		}
		return list;
	}

	public List<LatLon> getPoints() {
		List<LatLon> list = new ArrayList<>();
		String ip = getSettingsAPI().getString(osmandSettings.getGlobalPreferences(), pointsKey, "");
		if (ip.trim().length() > 0) {
			StringTokenizer tok = new StringTokenizer(ip, ",");
			while (tok.hasMoreTokens()) {
				String lat = tok.nextToken();
				if (!tok.hasMoreTokens()) {
					break;
				}
				String lon = tok.nextToken();
				list.add(new LatLon(Float.parseFloat(lat), Float.parseFloat(lon)));
			}
		}
		return list;
	}

	public boolean insertPoint(double latitude, double longitude, PointDescription historyDescription, int index) {
		List<LatLon> ps = getPoints();
		List<String> ds = getPointDescriptions(ps.size());
		ps.add(index, new LatLon(latitude, longitude));
		ds.add(index, PointDescription.serializeToString(historyDescription));
		if (historyDescription != null && !historyDescription.isSearchingAddress(osmandSettings.getContext())) {
			SearchHistoryHelper.getInstance(osmandSettings.getContext()).addNewItemToHistory(latitude, longitude, historyDescription);
		}
		return savePoints(ps, ds);
	}

	public boolean updatePoint(double latitude, double longitude, PointDescription historyDescription) {
		List<LatLon> ps = getPoints();
		List<String> ds = getPointDescriptions(ps.size());
		int i = ps.indexOf(new LatLon(latitude, longitude));
		if (i != -1) {
			ds.set(i, PointDescription.serializeToString(historyDescription));
			if (historyDescription != null && !historyDescription.isSearchingAddress(osmandSettings.getContext())) {
				SearchHistoryHelper.getInstance(osmandSettings.getContext()).addNewItemToHistory(latitude, longitude, historyDescription);
			}
			return savePoints(ps, ds);
		} else {
			return false;
		}
	}

	public boolean deletePoint(int index) {
		List<LatLon> ps = getPoints();
		List<String> ds = getPointDescriptions(ps.size());
		if (index < ps.size()) {
			ps.remove(index);
			ds.remove(index);
			return savePoints(ps, ds);
		} else {
			return false;
		}
	}

	public boolean deletePoint(LatLon latLon) {
		List<LatLon> ps = getPoints();
		List<String> ds = getPointDescriptions(ps.size());
		int index = ps.indexOf(latLon);
		if (index != -1) {
			ps.remove(index);
			ds.remove(index);
			return savePoints(ps, ds);
		} else {
			return false;
		}
	}

	public boolean savePoints(List<LatLon> ps, List<String> ds) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < ps.size(); i++) {
			if (i > 0) {
				sb.append(",");
			}
			sb.append(((float) ps.get(i).getLatitude() + "")).append(",").append(((float) ps.get(i).getLongitude() + ""));
		}
		StringBuilder tb = new StringBuilder();
		for (int i = 0; i < ds.size(); i++) {
			if (i > 0) {
				tb.append("--");
			}
			if (ds.get(i) == null) {
				tb.append("");
			} else {
				tb.append(ds.get(i));
			}
		}
		if (lastModifiedTimeStored) {
			getSettingsAPI().edit(osmandSettings.getGlobalPreferences())
					.putLong(pointsKey + "_last_modified", System.currentTimeMillis()).commit();
		}
		return getSettingsAPI().edit(osmandSettings.getGlobalPreferences())
				.putString(pointsKey, sb.toString())
				.putString(descriptionsKey, tb.toString())
				.commit();
	}

	public boolean movePoint(LatLon latLonEx, LatLon latLonNew) {
		List<LatLon> ps = getPoints();
		List<String> ds = getPointDescriptions(ps.size());
		int i = ps.indexOf(latLonEx);
		if (i != -1) {
			ps.set(i, latLonNew);
			return savePoints(ps, ds);
		} else {
			return false;
		}
	}
}