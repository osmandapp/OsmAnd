package net.osmand.plus.settings.backend.storages;

import androidx.annotation.NonNull;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.avoidroads.AvoidRoadInfo;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.backend.preferences.StringPreference;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

public class ImpassableRoadsStorage extends SettingsMapPointsStorage {

	protected final CommonPreference<String> ROADS_IDS;
	protected final CommonPreference<String> DIRECTIONS;
	protected final CommonPreference<String> APP_MODES;

	public ImpassableRoadsStorage(@NonNull OsmandSettings settings) {
		super(settings);
		POINTS.storeLastModifiedTime();

		ROADS_IDS = new StringPreference(settings, "impassable_roads_ids", "").makeGlobal().makeShared();
		DIRECTIONS = new StringPreference(settings, "impassable_roads_directions", "").makeGlobal().makeShared();
		APP_MODES = new StringPreference(settings, "impassable_roads_app_mode_keys", "").makeGlobal().makeShared();
	}

	@NonNull
	@Override
	protected String getPointsKey() {
		return "impassable_road_points";
	}

	@NonNull
	@Override
	protected String getDescriptionsKey() {
		return "impassable_roads_descriptions";
	}

	@NonNull
	public List<Long> getRoadIds(int size) {
		List<Long> list = new ArrayList<>();
		String roadIds = ROADS_IDS.get();
		if (roadIds.trim().length() > 0) {
			StringTokenizer tok = new StringTokenizer(roadIds, ",");
			while (tok.hasMoreTokens() && list.size() <= size) {
				list.add(Long.parseLong(tok.nextToken()));
			}
		}
		while (list.size() < size) {
			list.add(0L);
		}
		return list;
	}

	@NonNull
	public List<Double> getDirections(int size) {
		List<Double> list = new ArrayList<>();
		String directions = DIRECTIONS.get();
		if (directions.trim().length() > 0) {
			StringTokenizer tok = new StringTokenizer(directions, ",");
			while (tok.hasMoreTokens() && list.size() <= size) {
				list.add(Double.parseDouble(tok.nextToken()));
			}
		}
		while (list.size() < size) {
			list.add(0.0);
		}
		return list;
	}

	@NonNull
	public List<String> getAppModeKeys(int size) {
		List<String> list = new ArrayList<>();
		String roadIds = APP_MODES.get();
		if (roadIds.trim().length() > 0) {
			StringTokenizer tok = new StringTokenizer(roadIds, ",");
			while (tok.hasMoreTokens() && list.size() <= size) {
				list.add(tok.nextToken());
			}
		}
		while (list.size() < size) {
			list.add("");
		}
		return list;
	}

	@NonNull
	public List<AvoidRoadInfo> getImpassableRoadsInfo() {
		List<LatLon> points = getPoints();
		List<Long> roadIds = getRoadIds(points.size());
		List<Double> directions = getDirections(points.size());
		List<String> appModeKeys = getAppModeKeys(points.size());
		List<String> descriptions = getPointDescriptions(points.size());

		List<AvoidRoadInfo> avoidRoadsInfo = new ArrayList<>();

		for (int i = 0; i < points.size(); i++) {
			LatLon latLon = points.get(i);
			PointDescription description = PointDescription.deserializeFromString(descriptions.get(i), null);

			AvoidRoadInfo avoidRoadInfo = new AvoidRoadInfo();
			avoidRoadInfo.id = roadIds.get(i);
			avoidRoadInfo.latitude = latLon.getLatitude();
			avoidRoadInfo.longitude = latLon.getLongitude();
			avoidRoadInfo.direction = directions.get(i);
			avoidRoadInfo.name = description.getName();
			avoidRoadInfo.appModeKey = appModeKeys.get(i);
			avoidRoadsInfo.add(avoidRoadInfo);
		}

		return avoidRoadsInfo;
	}

	public boolean addImpassableRoadInfo(@NonNull AvoidRoadInfo avoidRoadInfo) {
		List<LatLon> points = getPoints();
		List<Long> roadIds = getRoadIds(points.size());
		List<Double> directions = getDirections(points.size());
		List<String> appModeKeys = getAppModeKeys(points.size());
		List<String> descriptions = getPointDescriptions(points.size());

		roadIds.add(0, avoidRoadInfo.id);
		directions.add(0, avoidRoadInfo.direction);
		points.add(0, new LatLon(avoidRoadInfo.latitude, avoidRoadInfo.longitude));
		appModeKeys.add(0, avoidRoadInfo.appModeKey);
		descriptions.add(0, PointDescription.serializeToString(new PointDescription("", avoidRoadInfo.name)));

		return saveAvoidRoadData(points, descriptions, roadIds, appModeKeys, directions);
	}

	public boolean updateImpassableRoadInfo(@NonNull AvoidRoadInfo avoidRoadInfo) {
		List<LatLon> points = getPoints();

		int index = points.indexOf(new LatLon(avoidRoadInfo.latitude, avoidRoadInfo.longitude));
		if (index != -1) {
			List<Long> roadIds = getRoadIds(points.size());
			List<Double> directions = getDirections(points.size());
			List<String> appModeKeys = getAppModeKeys(points.size());
			List<String> descriptions = getPointDescriptions(points.size());

			roadIds.set(index, avoidRoadInfo.id);
			directions.set(index, avoidRoadInfo.direction);
			appModeKeys.set(index, avoidRoadInfo.appModeKey);
			descriptions.set(index, PointDescription.serializeToString(new PointDescription("", avoidRoadInfo.name)));
			return saveAvoidRoadData(points, descriptions, roadIds, appModeKeys, directions);
		}
		return false;
	}

	@Override
	public boolean deletePoint(int index) {
		List<LatLon> points = getPoints();
		List<Long> roadIds = getRoadIds(points.size());
		List<Double> directions = getDirections(points.size());
		List<String> appModeKeys = getAppModeKeys(points.size());
		List<String> descriptions = getPointDescriptions(points.size());

		if (index < points.size()) {
			points.remove(index);
			roadIds.remove(index);
			directions.remove(index);
			appModeKeys.remove(index);
			descriptions.remove(index);
			return saveAvoidRoadData(points, descriptions, roadIds, appModeKeys, directions);
		}
		return false;
	}

	@Override
	public boolean deletePoint(@NonNull LatLon latLon) {
		List<LatLon> points = getPoints();
		List<Long> roadIds = getRoadIds(points.size());
		List<Double> directions = getDirections(points.size());
		List<String> appModeKeys = getAppModeKeys(points.size());
		List<String> descriptions = getPointDescriptions(points.size());

		int index = points.indexOf(latLon);
		if (index != -1) {
			points.remove(index);
			roadIds.remove(index);
			directions.remove(index);
			appModeKeys.remove(index);
			descriptions.remove(index);
			return saveAvoidRoadData(points, descriptions, roadIds, appModeKeys, directions);
		}
		return false;
	}

	@Override
	public boolean movePoint(@NonNull LatLon latLonEx, @NonNull LatLon latLonNew) {
		List<LatLon> points = getPoints();
		List<Long> roadIds = getRoadIds(points.size());
		List<Double> directions = getDirections(points.size());
		List<String> appModeKeys = getAppModeKeys(points.size());
		List<String> descriptions = getPointDescriptions(points.size());

		int i = points.indexOf(latLonEx);
		if (i != -1) {
			points.set(i, latLonNew);
			return saveAvoidRoadData(points, descriptions, roadIds, appModeKeys, directions);
		} else {
			return false;
		}
	}

	public boolean saveAvoidRoadData(@NonNull List<LatLon> points, @NonNull List<String> descriptions,
	                                 @NonNull List<Long> roadIds, @NonNull List<String> appModeKeys,
	                                 @NonNull List<Double> directions) {
		return savePoints(points, descriptions) && saveRoadIds(roadIds)
				&& saveAppModeKeys(appModeKeys) && saveDirections(directions);
	}

	public boolean saveRoadIds(@NonNull List<Long> roadIds) {
		StringBuilder builder = new StringBuilder();
		Iterator<Long> iterator = roadIds.iterator();
		while (iterator.hasNext()) {
			builder.append(iterator.next());
			if (iterator.hasNext()) {
				builder.append(",");
			}
		}
		return ROADS_IDS.set(builder.toString());
	}

	public boolean saveDirections(@NonNull List<Double> directions) {
		StringBuilder builder = new StringBuilder();
		Iterator<Double> iterator = directions.iterator();
		while (iterator.hasNext()) {
			builder.append(iterator.next());
			if (iterator.hasNext()) {
				builder.append(",");
			}
		}
		return DIRECTIONS.set(builder.toString());
	}

	public boolean saveAppModeKeys(@NonNull List<String> appModeKeys) {
		StringBuilder builder = new StringBuilder();
		Iterator<String> iterator = appModeKeys.iterator();
		while (iterator.hasNext()) {
			builder.append(iterator.next());
			if (iterator.hasNext()) {
				builder.append(",");
			}
		}
		return APP_MODES.set(builder.toString());
	}
}