package net.osmand.plus.osmo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.osmand.data.LatLon;
import net.osmand.plus.OsmandSettings;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class OsMoGroups implements OsMoReactor {
	
	private static final String TRACKER_ID = "trackerId";
	private static final String GROUP_TRACKERS = "group_trackers";
	private static final String GROUP_ID = "group_id";
	private static final String NAME = "name";
	private static final String USER_NAME = "userName";
	private static final String GROUP_NAME = "group_name";
	private static final String MY_GROUP_TRACKER_ID = "my_group_tracker_id";
	private OsMoTracker tracker;
	private ConcurrentHashMap<String, OsMoGroup> groups = new ConcurrentHashMap<String, OsMoGroup>();
	private OsMoGroup mainGroup;
	private OsmandSettings settings;
	private OsMoService service;

	public static class OsMoGroup {
		public String name;
		public String userName;
		public String groupId;
		public String myGroupTrackerId;
		public boolean active;
		
		public Map<String, OsMoUser> users = new ConcurrentHashMap<String, OsMoGroups.OsMoUser>(); 
		
		public boolean isMainGroup() {
			return groupId == null;
		}
		
		public String getVisibleName(){
			if(userName != null && userName.length() > 0) {
				return userName;
			}
			return name;
		}
	}
	
	public static class OsMoMessage {
		public long timestamp;
		public LatLon location;
		public String text;
	}
	
	public static class OsMoUser {
		public String serverName;
		public String userName;
		public String trackerId;
		public List<OsMoMessage> messages = new ArrayList<OsMoMessage>();
		
		public String getVisibleName(){
			if(userName != null && userName.length() > 0) {
				return userName;
			}
			return serverName;
		}
	}

	public OsMoGroups(OsMoService service, OsMoTracker tracker, OsmandSettings settings) {
		this.service = service;
		this.tracker = tracker;
		this.settings = settings;
//		service.registerSender(this);
		service.registerReactor(this);
		mainGroup = new OsMoGroup();
		groups.put("", mainGroup);
		
		parseGroups();
	}

	private void parseGroups() {
		String grp = settings.OSMO_GROUPS.get();
		try {
			JSONObject obj = new JSONObject(grp);
			parseGroupUsers(mainGroup, obj);
			if(!obj.has("groups")) {
				return;
			}
			JSONArray groups = obj.getJSONArray("groups");
			for (int i = 0; i < groups.length(); i++) {
				JSONObject o = (JSONObject) groups.get(i);
				OsMoGroup group = new OsMoGroup();
				group.groupId = o.getString(GROUP_ID);
				if(o.has(MY_GROUP_TRACKER_ID)) {
					group.myGroupTrackerId = o.getString(MY_GROUP_TRACKER_ID);
				}
				if(o.has(NAME)) {
					group.name = o.getString(NAME);
				}
				if(o.has(USER_NAME)) {
					group.userName = o.getString(USER_NAME);
				}
				parseGroupUsers(group, o);
				this.groups.put(group.groupId, group);
			}
		} catch (JSONException e) {
			e.printStackTrace();
			service.showErrorMessage(e.getMessage());
		}
	}
	
	public void saveGroups() {
		JSONObject mainObj = new JSONObject();
		try {
			saveGroupUsers(mainGroup, mainObj);
			JSONArray ar = new JSONArray();
			for(OsMoGroup gr : groups.values()) {
				if(gr.isMainGroup()) {
					continue;
				}
				JSONObject obj = new JSONObject();
				if (gr.userName != null) {
					obj.put(USER_NAME, gr.userName);
				}
				if (gr.name != null) {
					obj.put(NAME, gr.name);
				}
				obj.put(GROUP_ID, gr.groupId);
				ar.put(obj);	
			}
		} catch (JSONException e) {
			e.printStackTrace();
			service.showErrorMessage(e.getMessage());
		}
		settings.OSMO_GROUPS.set(mainObj.toString());
	}

	private void saveGroupUsers(OsMoGroup gr, JSONObject grObj) throws JSONException {
		JSONArray ar = new JSONArray();
		for(OsMoUser u : gr.users.values()) {
			JSONObject obj = new JSONObject();
			if (u.userName != null) {
				obj.put(USER_NAME, u.userName);
			}
			if (u.serverName != null) {
				obj.put("serverName", u.serverName);
			}
			obj.put(TRACKER_ID, u.trackerId);
			
			ar.put(obj);
		}
		grObj.put("users", ar);
	}

	private void parseGroupUsers(OsMoGroup gr, JSONObject obj) throws JSONException {
		if(!obj.has("users")) {
			return;
		}
		JSONArray users = obj.getJSONArray("users");
		for (int i = 0; i < users.length(); i++) {
			JSONObject o = (JSONObject) users.get(i);
			OsMoUser user = new OsMoUser();
			if(o.has("serverName")) {
				user.serverName = o.getString("serverName");
			}
			if(o.has(USER_NAME)) {
				user.userName = o.getString(USER_NAME);
			}
			user.trackerId = o.getString(TRACKER_ID);
		}
	}

	@Override
	public boolean acceptCommand(String command, String data, JSONObject obj, OsMoThread thread) {
		if(command.startsWith("ON_GROUP_CHANGE:")) {
			String gid = command.substring(command.indexOf(':') + 1);
			OsMoGroup gr = groups.get(gid);
			if(gr != null) {
				mergeGroup(gr, obj, false);
			}
			return true;
		} else if(command.startsWith("CREATE_GROUP:")) {
			String gid = command.substring(command.indexOf(':') + 1);
			OsMoGroup gr = new OsMoGroup();
			gr.groupId = gid;
			try {
				gr.name = obj.getString(GROUP_NAME);
			} catch (JSONException e) {
				e.printStackTrace();
				service.showErrorMessage(e.getMessage());
			}
			joinGroup(gid);
		} else if(command.startsWith("JOIN_GROUP:")) {
			String gid = command.substring(command.indexOf(':') + 1);
			OsMoGroup gr = groups.get(gid);
			if(gr != null) {
				mergeGroup(gr, obj, true);
				gr.active = true;
				for(String key : gr.users.keySet()) {
					if (!key.equals(gr.myGroupTrackerId)) {
						tracker.startTrackingId(key);
					}
				}
			}
			return true;
		} else if(command.startsWith("LEAVE_GROUP:")) {
			String gid = command.substring(command.indexOf(':') + 1);
			OsMoGroup gr = groups.get(gid);
			if(gr != null) {
				gr.active = false;
				for(String key : gr.users.keySet()) {
					if (!key.equals(gr.myGroupTrackerId)) {
						tracker.stopTrackingId(key);
					}
				}
			}
			return true;
		}
		return false;
	}
	
	
	private void mergeGroup(OsMoGroup gr, JSONObject obj, boolean deleteUsers) {
		try {
			if(obj.has(GROUP_NAME)) {
				gr.name = obj.getString(GROUP_NAME);
			}
			if(obj.has(MY_GROUP_TRACKER_ID)) {
				gr.myGroupTrackerId = obj.getString(MY_GROUP_TRACKER_ID);
			}
			JSONArray arr = obj.getJSONArray(GROUP_TRACKERS);
			Set<String> toDelete = new HashSet<String>(gr.users.keySet());
			for (int i = 0; i < arr.length(); i++) {
				JSONObject o = (JSONObject) arr.get(i);
				String tid = o.getString(TRACKER_ID);
				toDelete.remove(tid);
				OsMoUser us = gr.users.get(tid);
				if (us == null) {
					us = new OsMoUser();
					us.trackerId = tid;
					gr.users.put(tid, us);
					if(gr.active) {
						if (!tid.equals(gr.myGroupTrackerId)) {
							tracker.startTrackingId(tid);
						}
					}
				}
				if (o.has(NAME)) {
					us.serverName = o.getString(NAME);
				}
			}
			if(deleteUsers) {
				for(String s : toDelete) {
					gr.users.remove(s);
					if(gr.active) {
						tracker.stopTrackingId(s);
					}
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
			service.showErrorMessage(e.getMessage());
		}
	}

	public void joinGroup(String groupId) {
		service.pushCommand("JOIN_GROUP|"+groupId);
	}
	
	public void createGroup(String groupName) {
		service.pushCommand("CREATE_GROUP|{\"group_name\":\"" + groupName + "\"}");
	}
	
	
	public void leaveGroup(OsMoGroup group) {
		service.pushCommand("LEAVE_GROUP|"+group.groupId);
	}

}
