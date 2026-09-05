package net.osmand.plus.settings.backend.storages;

import androidx.annotation.NonNull;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.helpers.SearchHistoryHelper;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.backend.preferences.StringPreference;
import net.osmand.plus.settings.enums.HistorySource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

abstract class SettingsMapPointsStorage {

	protected final OsmandSettings settings;

	protected final CommonPreference<String> POINTS;
	protected final CommonPreference<String> DESCRIPTIONS;


	public SettingsMapPointsStorage(@NonNull OsmandSettings settings) {
		this.settings = settings;

		POINTS = new StringPreference(settings, getPointsKey(), "").makeGlobal().makeShared();
		DESCRIPTIONS = new StringPreference(settings, getDescriptionsKey(), "").makeGlobal().makeShared();
	}

	@NonNull
	protected abstract String getPointsKey();

	@NonNull
	protected abstract String getDescriptionsKey();

	@NonNull
	protected OsmandSettings getSettings() {
		return settings;
	}

	public long getLastModifiedTime() {
		if (!POINTS.isLastModifiedTimeStored()) {
			throw new IllegalStateException(getPointsKey() + " is not granted to store last modified time");
		}
		return POINTS.getLastModifiedTime();
	}

	public void setLastModifiedTime(long lastModifiedTime) {
		if (!POINTS.isLastModifiedTimeStored()) {
			throw new IllegalStateException(getPointsKey() + " is not granted to store last modified time");
		}
		POINTS.setLastModifiedTime(lastModifiedTime);
	}

	@NonNull
	public List<String> getPointDescriptions(int sz) {
		List<String> list = new ArrayList<>();
		String ip = DESCRIPTIONS.get();
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

	@NonNull
	public List<LatLon> getPoints() {
		List<LatLon> list = new ArrayList<>();
		String ip = POINTS.get();
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
		if (historyDescription != null && !historyDescription.isSearchingAddress(settings.getContext())) {
			SearchHistoryHelper.getInstance(settings.getContext()).addNewItemToHistory(latitude, longitude, historyDescription, HistorySource.NAVIGATION);
		}
		return savePoints(ps, ds);
	}

	public boolean updatePoint(double latitude, double longitude, PointDescription historyDescription) {
		List<LatLon> ps = getPoints();
		List<String> ds = getPointDescriptions(ps.size());
		int i = ps.indexOf(new LatLon(latitude, longitude));
		if (i != -1) {
			ds.set(i, PointDescription.serializeToString(historyDescription));
			if (historyDescription != null && !historyDescription.isSearchingAddress(settings.getContext())) {
				SearchHistoryHelper.getInstance(settings.getContext()).addNewItemToHistory(latitude, longitude, historyDescription, HistorySource.NAVIGATION);
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

	public boolean deletePoint(@NonNull LatLon latLon) {
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

	public boolean savePoints(@NonNull List<LatLon> points, @NonNull List<String> descriptions) {
		StringBuilder pointsBuilder = new StringBuilder();
		for (int i = 0; i < points.size(); i++) {
			if (i > 0) {
				pointsBuilder.append(",");
			}
			pointsBuilder.append(((float) points.get(i).getLatitude() + "")).append(",").append(((float) points.get(i).getLongitude() + ""));
		}
		StringBuilder descriptionsBuilder = new StringBuilder();
		for (int i = 0; i < descriptions.size(); i++) {
			if (i > 0) {
				descriptionsBuilder.append("--");
			}
			if (descriptions.get(i) == null) {
				descriptionsBuilder.append("");
			} else {
				descriptionsBuilder.append(descriptions.get(i));
			}
		}
		return POINTS.set(pointsBuilder.toString()) && DESCRIPTIONS.set(descriptionsBuilder.toString());
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