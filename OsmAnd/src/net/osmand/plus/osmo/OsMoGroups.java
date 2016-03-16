package net.osmand.plus.osmo;

import com.google.gson.Gson;

import net.osmand.Location;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.osmo.OsMoGroupsStorage.OsMoDevice;
import net.osmand.plus.osmo.OsMoGroupsStorage.OsMoGroup;
import net.osmand.plus.osmo.OsMoTracker.OsmoTrackerListener;
import net.osmand.util.Algorithms;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OsMoGroups implements OsMoReactor, OsmoTrackerListener {

	private static final String GROUP_NAME = "name";
	private static final String ACTIVE = "active";
	private static final String GROUP_ID = "group_id";
	private static final String EXPIRE_TIME = "expireTime";
	private static final String DESCRIPTION = "description";
	private static final String POLICY = "policy";
	private static final String USERS = "users";
	private static final String USER_NAME = "name";
	private static final String USER_COLOR = "color";
	private static final String DELETED = "deleted";
	private static final String GROUP_TRACKER_ID = "group_tracker_id";
	private static final String LAST_ONLINE = "last_online";
	private static final String TRACK = "track";
	private static final String POINT = "point";

	private OsMoTracker tracker;
	private OsMoService service;
	private OsMoGroupsStorage storage;
	private ArrayList<OsMoGroupsUIListener> uiListeners = new ArrayList<>();
	private OsMoPlugin plugin;
	private OsmandApplication app;

	private Gson gson = new Gson();

	public interface OsMoGroupsUIListener {

		public void groupsListChange(String operation, OsMoGroup group);

		public void deviceLocationChanged(OsMoDevice device);
	}

	public OsMoGroups(OsMoPlugin plugin, OsMoService service, OsMoTracker tracker, OsmandApplication app) {
		this.plugin = plugin;
		this.service = service;
		this.tracker = tracker;
		this.app = app;
		service.registerReactor(this);
		tracker.setTrackerListener(this);
		storage = new OsMoGroupsStorage(this, app.getSettings().OSMO_GROUPS);
		if (!service.isLoggedIn()) {
			storage.load();
		}
	}

	public void addUiListeners(OsMoGroupsUIListener uiListener) {
		if (!uiListeners.contains(uiListener)) {
			uiListeners.add(uiListener);
		}
	}

	public void removeUiListener(OsMoGroupsUIListener uiListener) {
		uiListeners.remove(uiListener);
	}

	private void connectDeviceImpl(OsMoDevice d) {
		d.enabled = true;
		d.active = true;
		String mid = service.getMyTrackerId();
		String mgid = service.getMyGroupTrackerId();
		if ((mid == null || !mid.equals(d.getTrackerId()))
				&& (mgid == null || !mgid.equals(d.getTrackerId()))) {
			tracker.startTrackingId(d);
		}
	}

	@Override
	public void onConnected() {
		if (service.isLoggedIn()) {
			service.pushCommand("GROUP_GET_ALL");
		}
	}

	@Override
	public void onDisconnected(String msg) {
	}

	private String connectGroupImpl(OsMoGroup g) {
		g.enabled = true;
		String operation = "GROUP_CONNECT:" + g.groupId;
		service.pushCommand("GROUP_CONNECT:" + g.groupId);
		return operation;
	}

	public String connectGroup(OsMoGroup model) {
		String op = connectGroupImpl(model);
		storage.save();
		return op;
	}

	public String disconnectGroup(OsMoGroup model) {
		model.enabled = false;
		String operation = "GROUP_DISCONNECT:" + model.groupId;
		service.pushCommand(operation);
		for (OsMoDevice d : model.getGroupUsers(null)) {
			tracker.stopTrackingId(d);
		}
		storage.save();
		return operation;
	}


	private void disconnectImpl(OsMoDevice model) {
		model.active = false;
		tracker.stopTrackingId(model);
	}


	public Collection<OsMoGroup> getGroups() {
		return storage.getGroups();
	}

	public void showErrorMessage(String message) {
		service.showErrorMessage(message);
	}

	@Override
	public void locationChange(String trackerId, Location location) {
		for (OsMoGroup g : getGroups()) {
			OsMoDevice d = g.updateLastLocation(trackerId, location);
			if (d != null && uiListeners != null) {
				for (OsMoGroupsUIListener listener : uiListeners) {
					listener.deviceLocationChanged(d);
				}
			}
		}
	}


	@Override
	public boolean acceptCommand(String command, String gid, String data, JSONObject obj, OsMoThread thread) {
		boolean processed = false;
		String operation = command + ":" + gid;
		OsMoGroup group = null;
		if (command.equalsIgnoreCase("GP")) {
			group = storage.getGroup(gid);
			if (group != null && gid.length() > 0) {
				List<OsMoDevice> delta = mergeGroup(group, obj, false, false);
				String mygid = service.getMyGroupTrackerId();
				StringBuilder b = new StringBuilder();
				for (OsMoDevice d : delta) {
					if (d.getDeletedTimestamp() != 0 && d.isEnabled()) {
						if (group.name != null && !d.getTrackerId().equals(mygid)) {
							b.append(app.getString(R.string.osmo_user_left, d.getVisibleName(), group.getVisibleName(app))).append("\n");
						}
						disconnectImpl(d);
					} else if (!d.isActive()) {
						if (group.name != null && !d.getTrackerId().equals(mygid)) {
							b.append(app.getString(R.string.osmo_user_joined, d.getVisibleName(), group.getVisibleName(app))).append("\n");
						}
						connectDeviceImpl(d);
					}
				}
				if (b.length() > 0 && app.getSettings().OSMO_SHOW_GROUP_NOTIFICATIONS.get()) {
					app.showToastMessage(b.toString().trim());
				}
				storage.save();
			}
			processed = true;
		} else if (command.equalsIgnoreCase("GROUP_DISCONNECT")) {
			group = storage.getGroup(gid);
			if (group != null) {
				if (obj == null || !obj.has("error")) {
					disconnectAllGroupUsers(group);
					disableGroupTracks(group, group.groupTracks);
					disableGroupTracks(group, Collections.singleton(group.name + " points.gpx"));
				}
				processed = true;
			}
		} else if (command.equalsIgnoreCase("GROUP_CONNECT")) {
			group = storage.getGroup(gid);
			if (group != null) {
				mergeGroup(group, obj, true, true);
				group.active = true;
				// connect to enabled devices in group
				for (OsMoDevice d : group.getGroupUsers(null)) {
					if (d.isEnabled()) {
						d.active = false;
						connectDeviceImpl(d);
					}
				}
				storage.save();
			}
			processed = true;
		} else if (command.equalsIgnoreCase("GROUP_CREATE") || command.equalsIgnoreCase("GROUP_JOIN")) {
			if (command.equalsIgnoreCase("GROUP_CREATE")) {
				operation = command;
				try {
					gid = obj.getString(GROUP_ID);
				} catch (JSONException e) {
					e.printStackTrace();
					service.showErrorMessage(e.getMessage());
					return true;
				}
			}
			group = storage.getGroup(gid);
			if (group == null) {
				group = new OsMoGroup();
				group.groupId = gid;
				storage.addGroup(group);
			}
			connectGroupImpl(group);
			storage.save();
			processed = true;
		} else if (command.equals("GROUP_LEAVE")) {
			group = storage.getGroup(gid);
			if (group != null) {
				storage.deleteGroup(group);
			}
			storage.save();
			processed = true;
		} else if (command.equals("GROUP_GET_ALL")) {
			try {
				JSONArray arr = new JSONArray(data);
				int arrLength = arr.length();
				if (arrLength > 0) {
					Map<String, OsMoGroup> receivedGroups = new HashMap<String, OsMoGroupsStorage.OsMoGroup>();
					for (int i = 0; i < arrLength; i++) {
						JSONObject o = arr.getJSONObject(i);
						OsMoGroup parsedGroup = parseGroup(o);
						receivedGroups.put(parsedGroup.getGroupId(), parsedGroup);
					}

					storage.mergeGroups(receivedGroups);
					storage.save();
				}
				processed = true;
				for (OsMoGroup g : storage.getGroups()) {
					if (!g.isMainGroup() && g.isEnabled()) {
						connectGroupImpl(g);
					}
				}
			} catch (JSONException e) {
				e.printStackTrace();
				service.showErrorMessage(e.getMessage());
				return processed;
			}
		}
		if (processed && uiListeners != null) {
			for (OsMoGroupsUIListener listener : uiListeners) {
				listener.groupsListChange(operation, group);
			}
		}
		return processed;
	}

	private void disableGroupTracks(OsMoGroup group, Collection<String> tracks) {
		if (!tracks.isEmpty()) {
			GpxSelectionHelper helper = app.getSelectedGpxHelper();
			for (String t : tracks) {
				SelectedGpxFile sg = helper.getSelectedFileByName("osmo/" + t);
				if (sg != null && sg.getGpxFile() != null) {
					helper.selectGpxFile(sg.getGpxFile(), false, false);
				}
			}
			plugin.refreshMap();
		}
	}

	private OsMoGroup parseGroup(JSONObject obj) throws JSONException {
		OsMoGroup groupe = new OsMoGroup();
		groupe.enabled = !(obj.has(ACTIVE) && ("0".equals(obj.getString(ACTIVE)) ||
				"false".equals(obj.getString(ACTIVE))));
		groupe.name = obj.optString(GROUP_NAME);
		groupe.groupId = obj.getString(GROUP_ID);
		groupe.description = obj.optString(DESCRIPTION);
		groupe.policy = obj.optString(POLICY);
		return groupe;
	}


	private void parseGroup(JSONObject obj, OsMoGroup gr) {
		try {
			if (obj.has(GROUP_NAME)) {
				gr.name = obj.getString(GROUP_NAME);
			}
			if (obj.has(DESCRIPTION)) {
				gr.description = obj.getString(DESCRIPTION);
			}
			if (obj.has(POLICY)) {
				gr.policy = obj.getString(POLICY);
			}
			if (obj.has(EXPIRE_TIME)) {
				gr.expireTime = obj.getLong(EXPIRE_TIME);
			}
		} catch (JSONException e) {
			e.printStackTrace();
			service.showErrorMessage(e.getMessage());
		}

	}

	private void disconnectAllGroupUsers(OsMoGroup gr) {
		gr.active = false;
		for (OsMoDevice d : gr.getGroupUsers(null)) {
			disconnectImpl(d);
		}
	}


	private List<OsMoDevice> mergeGroup(OsMoGroup gr, JSONObject obj, boolean deleteUsers,
										boolean isGroupConnect) {
		List<OsMoDevice> delta = new ArrayList<OsMoDevice>();
		try {
			if (obj.has("group")) {
				parseGroup(obj.getJSONObject("group"), gr);
			}
			Map<String, OsMoDevice> toDelete = new HashMap<String, OsMoDevice>(gr.users);
			if (obj.has(USERS)) {
				JSONArray arr = obj.getJSONArray(USERS);
				for (int i = 0; i < arr.length(); i++) {
					JSONObject o = (JSONObject) arr.get(i);
					String tid = o.getString(GROUP_TRACKER_ID);
					OsMoDevice device = toDelete.remove(tid);
					if (device == null) {
						device = new OsMoDevice();
						device.group = gr;
						device.trackerId = tid;
						device.enabled = true;
						gr.users.put(tid, device);
					}
					if (o.has(USER_NAME)) {
						device.serverName = o.getString(USER_NAME);
					}
					if (o.has(DELETED)) {
						device.deleted = System.currentTimeMillis();
					} else {
						device.deleted = 0;
					}

					if (o.has(LAST_ONLINE)) {
						device.lastOnline = o.getLong(LAST_ONLINE);
					}
					if (o.has(USER_COLOR)) {
						device.serverColor = net.osmand.util.Algorithms.parseColor(o.getString(USER_COLOR));
					}
					delta.add(device);
				}
			}

			if (obj.has(TRACK)) {
				JSONArray ar = obj.getJSONArray(TRACK);
				List<JSONObject> a = new ArrayList<>(ar.length());
				Set<String> toDeleteT = new HashSet<String>(gr.groupTracks);
				gr.groupTracks.clear();
				for (int i = 0; i < ar.length(); i++) {
					JSONObject trackJson = (JSONObject) ar.get(i);
					if (!trackJson.has(DELETED)) {
						String track = trackJson.getString("name") + ".gpx";
						gr.groupTracks.add(track);
						toDeleteT.remove(track);
						a.add(trackJson);
					}
				}
				plugin.getDownloadGpxTask(true).execute(a.toArray(new JSONObject[a.size()]));
				disableGroupTracks(gr, toDeleteT);
			}

			if (obj.has(POINT)) {
				JSONArray ar = obj.getJSONArray(POINT);
				ArrayList<WptPt> points = new ArrayList<WptPt>();
				long modify = obj.has("point_modify") ? obj.getLong("point_modify") : System.currentTimeMillis();
				JSONObject[] a = new JSONObject[ar.length()];
				for (int i = 0; i < a.length; i++) {
					a[i] = (JSONObject) ar.get(i);
					final JSONObject jobj = a[i];
					WptPt pt = new WptPt();
					if (jobj.has("deleted")) {
						pt.deleted = true;
					} else {
						pt.lat = a[i].getDouble("lat");
						pt.lon = a[i].getDouble("lon");
						if (jobj.has("name")) {
							pt.name = jobj.getString("name");
						}
						if (jobj.has("color")) {
							pt.setColor(Algorithms.parseColor(jobj.getString("color")));
						}
						if (jobj.has("description")) {
							pt.desc = jobj.getString("description");
						}
						if (jobj.has("created")) {
							pt.getExtensionsToWrite().put("created", a[i].getLong("created") + "");
						}
						if (jobj.has("visible")) {
							pt.getExtensionsToWrite().put("visible", a[i].getBoolean("visible") + "");
						}
					}
					if (jobj.has("u")) {
						pt.getExtensionsToWrite().put("u", String.valueOf(a[i].getLong("u")));
					}
					points.add(pt);
				}
				if (points.size() > 0) {
					plugin.getSaveGpxTask(gr.name + " points", modify, false, isGroupConnect)
							.execute(points.toArray(new WptPt[points.size()]));
				}
			}
			if (deleteUsers) {
				for (OsMoDevice s : toDelete.values()) {
					s.deleted = System.currentTimeMillis();
					delta.add(s);
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
			service.showErrorMessage(e.getMessage());
		}
		return delta;
	}


	public String createGroup(String groupName, boolean onlyByInvite, String description, String policy) {
		Protocol.CreateGroupData obj = new Protocol.CreateGroupData(groupName,
				onlyByInvite, description, policy);
		service.pushCommand("GROUP_CREATE|" + gson.toJson(obj));
		return "GROUP_CREATE";
	}


	public void setGenColor(OsMoDevice device, int genColor) {
		device.genColor = genColor;
		storage.save();
	}

	public String joinGroup(String groupId, String userName, String nick) {
		final String op = "GROUP_JOIN:" + groupId + "|" + nick;
		OsMoGroup g = storage.getGroup(groupId);
		if (g == null) {
			g = new OsMoGroup();
			g.groupId = groupId;
			storage.addGroup(g);
		}
		g.userName = userName;
		service.pushCommand(op);
		return "GROUP_JOIN:" + groupId;
	}

	public String leaveGroup(OsMoGroup group) {
		final String op = "GROUP_LEAVE:" + group.groupId;
		storage.deleteGroup(group);
		storage.save();
		service.pushCommand(op);
		if (group.isEnabled()) {
			group.enabled = false;
			disconnectAllGroupUsers(group);
		}
		return op;
	}


	public void setDeviceProperties(OsMoDevice model, String name, int color) {
		model.userName = name;
		model.userColor = color;
		storage.save();
	}

	@Override
	public String nextSendCommand(OsMoThread tracker) {
		return null;
	}

	public void clearGroups() {
		storage.clearGroups();
		storage.save();
	}
}