package net.osmand.plus.osmo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.osmand.Location;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.osmo.OsMoGroupsStorage.OsMoDevice;
import net.osmand.plus.osmo.OsMoGroupsStorage.OsMoGroup;
import net.osmand.plus.osmo.OsMoTracker.OsmoTrackerListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class OsMoGroups implements OsMoReactor, OsmoTrackerListener {
	
	private static final String GROUP_NAME = "name";
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
	
	private OsMoTracker tracker;
	private OsMoService service;
	private OsMoGroupsStorage storage;
	private OsMoGroupsUIListener uiListener;
	
	public interface OsMoGroupsUIListener {
		
		public void groupsListChange(String operation, OsMoGroup group);
		
		public void deviceLocationChanged(OsMoDevice device);
	}

	public OsMoGroups(OsMoService service, OsMoTracker tracker, OsmandSettings settings) {
		this.service = service;
		this.tracker = tracker;
		service.registerReactor(this);
		tracker.setTrackerListener(this);
		storage = new OsMoGroupsStorage(this, settings.OSMO_GROUPS);
		storage.load();
	}
	
	public void setUiListener(OsMoGroupsUIListener uiListener) {
		this.uiListener = uiListener;
	}
	
	public OsMoGroupsUIListener getUiListener() {
		return uiListener;
	}
	
	private void connectDeviceImpl(OsMoDevice d) {
		d.enabled =  true;
		d.active = true;
		String mid = service.getMyGroupTrackerId();
		if(mid == null || !mid.equals(d.getTrackerId())) {
			tracker.startTrackingId(d);
		}
	}
	
	@Override
	public void reconnect() {
		for(OsMoDevice d : storage.getMainGroup().getGroupUsers(null)) {
			if(d.isEnabled()) {
				connectDeviceImpl(d);
			}
		}
		for(OsMoGroup g : storage.getGroups()) {
			if(!g.isMainGroup() && g.isEnabled()) {
				connectGroupImpl(g);
			}
		}
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
	
	public void connectDevice(OsMoDevice model) {
		connectDeviceImpl(model);
		storage.save();
	}
	

	public String disconnectGroup(OsMoGroup model) {
		model.enabled = false;
		String operation = "GROUP_DISCONNECT:"+model.groupId;
		service.pushCommand(operation);
		for(OsMoDevice d : model.getGroupUsers(null)) {
			tracker.stopTrackingId(d);
		}
		storage.save();
		return operation;
	}
	
	public void disconnectDevice(OsMoDevice model) {
		disconnectImpl(model);
		storage.save();
	}

	private void disconnectImpl(OsMoDevice model) {
		model.enabled = false;
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
			if (d != null && uiListener != null) {
				uiListener.deviceLocationChanged(d);
			}
		}
	}


	@Override
	public boolean acceptCommand(String command, String gid, String data, JSONObject obj, OsMoThread thread) {
		boolean processed = false;
		String operation = command + ":" + gid;
		OsMoGroup group = null;
		if(command.equalsIgnoreCase("GP")) {
			group = storage.getGroup(gid);
			if(group != null) {
				List<OsMoDevice> delta = mergeGroup(group, obj, false);
				for(OsMoDevice d : delta) {
					if(d.getDeletedTimestamp() != 0 && d.isEnabled()) {
						disconnectImpl(d);
					} else if(!d.isActive()) {
						connectDeviceImpl(d);
					}
				}
				storage.save();
			}
			processed = true;
		} else if(command.equalsIgnoreCase("GROUP_DISCONNECT")) {
			group = storage.getGroup(gid);
			if(group != null) {
				disconnectAllGroupUsers(group);
			}
			processed = true;
		} else if(command.equalsIgnoreCase("GROUP_CONNECT")) {
			group = storage.getGroup(gid);
			if(group != null) {
				mergeGroup(group, obj, true);
				group.active = true;
				// connect to all devices in group
				for(OsMoDevice d : group.getGroupUsers(null)) {
					connectDeviceImpl(d);
				}
				storage.save();
			}
			processed = true;
		} else if(command.equalsIgnoreCase("GROUP_CREATE") || command.equalsIgnoreCase("GROUP_JOIN") ) {
			if(command.equalsIgnoreCase("GROUP_CREATE")) {
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
			if(group == null) {
				group = new OsMoGroup();
				group.groupId = gid;
				storage.addGroup(group);
			}
			connectGroupImpl(group);
			storage.save();
			processed = true;
		} else if(command.equals("GROUP_LEAVE")) {
			group = storage.getGroup(gid);
			if(group != null) {
				storage.deleteGroup(group);
			}
			storage.save();
			processed = true;
		}
		if(processed && uiListener != null) {
			uiListener.groupsListChange(operation, group);
		}
		return processed;
	}

	private void parseGroup(JSONObject obj, OsMoGroup gr) {
		try {
			if(obj.has(GROUP_NAME)) {
				gr.name = obj.getString(GROUP_NAME);
			}
			if(obj.has(DESCRIPTION)) {
				gr.description = obj.getString(DESCRIPTION);
			}
			if(obj.has(POLICY)) {
				gr.description = obj.getString(POLICY);
			}
			if(obj.has(EXPIRE_TIME)) {
				gr.expireTime = obj.getLong(EXPIRE_TIME);
			}
		} catch (JSONException e) {
			e.printStackTrace();
			service.showErrorMessage(e.getMessage());
		}
		
	}

	private void disconnectAllGroupUsers(OsMoGroup gr) {
		gr.active = false;
		for(OsMoDevice d : gr.getGroupUsers(null)) {
			disconnectImpl(d);
		}
	}

	
	private List<OsMoDevice> mergeGroup(OsMoGroup gr, JSONObject obj, boolean deleteUsers) {
		List<OsMoDevice> delta = new ArrayList<OsMoDevice>();
		try {
			if(obj.has("group")) {
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
					if (o.has(DELETED) && o.getBoolean(DELETED)) {
						device.deleted = System.currentTimeMillis();
					} else {
						device.deleted = 0;
					}

					if (o.has(LAST_ONLINE)) {
						device.lastOnline = o.getLong(LAST_ONLINE);
					}
					if (o.has(USER_COLOR)) {
						device.serverColor = o.getInt(USER_COLOR);
					}
					delta.add(device);
				}
			}
			if(deleteUsers) {
				for(OsMoDevice s : toDelete.values()) {
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
	
	
	public String createGroup(String groupName, long expireTime, String description, String policy) {
		JSONObject obj = new JSONObject();
		try {
			obj.put("name", groupName);
			obj.put("expireTime", expireTime);
			obj.put("description", description);
			obj.put("policy", policy);
			service.pushCommand("GROUP_CREATE|" + obj.toString());
			return "GROUP_CREATE";
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	public void deleteDevice(OsMoDevice device) {
		storage.getMainGroup().users.remove(device.trackerId);
		if(device.isEnabled()) {
			disconnectImpl(device);
		}
		storage.save();
	}
	
	public OsMoDevice addConnectedDevice(String trackerId, String nameUser, int userColor) {
		OsMoDevice us = new OsMoDevice();
		us.group = storage.getMainGroup();
		us.trackerId = trackerId;
		us.userName = nameUser;
		us.userColor = userColor;
		us.group.users.put(trackerId, us);
		connectDeviceImpl(us);
		storage.save();
		return us;
	}
	public String joinGroup(String groupId, String userName, String nick) {
		final String op = "GROUP_JOIN:"+groupId+"|"+nick;
		OsMoGroup g = storage.getGroup(groupId);
		if(g == null){
			g = new OsMoGroup();
			g.groupId = groupId;
			storage.addGroup(g);
		}
		g.userName = userName;		
		service.pushCommand(op);
		return op; 
	}
	
	public String leaveGroup(OsMoGroup group) {
		final String op = "GROUP_LEAVE:"+group.groupId;
		storage.deleteGroup(group);
		storage.save();
		service.pushCommand(op);
		if(group.isEnabled()) {
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


}
