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
		
		public void groupsListChange(String operation);
	}

	public OsMoGroups(OsMoService service, OsMoTracker tracker, OsmandSettings settings) {
		this.service = service;
		this.tracker = tracker;
		service.registerReactor(this);
		tracker.setTrackerListener(this);
		storage = new OsMoGroupsStorage(this, settings.OSMO_GROUPS);
		storage.load();
		for(OsMoDevice d : storage.getMainGroup().getGroupUsers()) {
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
	
	public void setUiListener(OsMoGroupsUIListener uiListener) {
		this.uiListener = uiListener;
	}
	
	public OsMoGroupsUIListener getUiListener() {
		return uiListener;
	}
	
	private void connectDeviceImpl(OsMoDevice d) {
		d.enabled =  true;
		if(!service.getMyGroupTrackerId().equals(d.getTrackerId())) {
			tracker.startTrackingId(d.trackerId);
		}
		
	}

	private String connectGroupImpl(OsMoGroup g) {
		g.enabled = true;
		String operation = "CONNECT_GROUP:" + g.groupId;
		service.pushCommand("CONNECT_GROUP:" + g.groupId);
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
		for(OsMoDevice d : model.getGroupUsers()) {
			tracker.startTrackingId(d.trackerId);
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
		tracker.stopTrackingId(model.trackerId);
	}

		
	public Collection<OsMoGroup> getGroups() {
		return storage.getGroups();
	}
	
	public void showErrorMessage(String message) {
		service.showErrorMessage(message);
	}

	@Override
	public void locationChange(String trackerId, Location location) {
		for(OsMoGroup  g: getGroups()) {
			g.updateLastLocation(trackerId, location);
		}
	}


	@Override
	public boolean acceptCommand(String command, String data, JSONObject obj, OsMoThread thread) {
		boolean processed = false;
		String operation = command;
		if(command.startsWith("GROUP_CHANGE:")) {
			String gid = command.substring(command.indexOf(':') + 1);
			OsMoGroup gr = storage.getGroup(gid);
			if(gr != null) {
				List<OsMoDevice> delta = mergeGroup(gr, obj, false);
				for(OsMoDevice d :delta) {
					if(d.getDeletedTimestamp() != 0 && d.isEnabled()) {
						disconnectImpl(d);
					} else if(d.isEnabled() && !d.isActive()) {
						connectDeviceImpl(d);
					}
				}
				storage.save();
			}
			processed = true;
		} else if(command.startsWith("GROUP_DISCONNECT:")) {
			String gid = command.substring(command.indexOf(':') + 1);
			OsMoGroup gr = storage.getGroup(gid);
			if(gr != null) {
				disconnectAllGroupUsers(gr);
			}
			processed = true;
		} else if(command.startsWith("GROUP_CONNECT:")) {
			String gid = command.substring(command.indexOf(':') + 1);
			OsMoGroup gr = storage.getGroup(gid);
			if(gr != null) {
				mergeGroup(gr, obj, true);
				gr.active = true;
				// connect to all devices in group
				for(OsMoDevice d : storage.getMainGroup().getGroupUsers()) {
					connectDeviceImpl(d);
				}
				storage.save();
			}
			processed = true;
		} else if(command.startsWith("GROUP_CREATE:") || command.startsWith("GROUP_JOIN:") ) {
			if(command.startsWith("GROUP_CREATE:")) {
				operation = "GROUP_CREATE";
			}
			String gid = command.substring(command.indexOf(':') + 1);
			OsMoGroup gr = storage.getGroup(gid);
			if(gr == null) {
				gr = new OsMoGroup();
				gr.groupId = gid;
			}
			parseGroup(obj, gr);
			connectGroupImpl(gr);
			storage.save();
			processed = true;
		} else if(command.startsWith("LEAVE_GROUP:")) {
			String gid = command.substring(command.indexOf(':') + 1);
			OsMoGroup gr = storage.getGroup(gid);
			if(gr != null) {
				disconnectAllGroupUsers(gr);
				storage.deleteGroup(gr);
				storage.save();
			}
			processed = true;
		}
		if(processed && uiListener != null) {
			uiListener.groupsListChange(operation);
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
		for(OsMoDevice d : gr.getGroupUsers()) {
			disconnectImpl(d);
		}
	}

	
	private List<OsMoDevice> mergeGroup(OsMoGroup gr, JSONObject obj, boolean deleteUsers) {
		List<OsMoDevice> delta = new ArrayList<OsMoDevice>();
		try {
			parseGroup(obj, gr);
			JSONArray arr = obj.getJSONArray(USERS);
			Map<String, OsMoDevice> toDelete = new HashMap<String, OsMoDevice>(gr.users);
			for (int i = 0; i < arr.length(); i++) {
				JSONObject o = (JSONObject) arr.get(i);
				String tid = o.getString(GROUP_TRACKER_ID);
				OsMoDevice device = toDelete.remove(tid);
				if (device == null) {
					device = new OsMoDevice();
					device.group =  gr;
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
	public String joinGroup(String groupId, String userName) {
		final String op = "GROUP_JOIN:"+groupId;
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
		service.pushCommand(op);
		return op;
	}
	



}
