package net.osmand.plus.osmo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandSettings.OsmandPreference;
import net.osmand.plus.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.Color;

public class OsMoGroupsStorage {
	private static final String GROUPS = "groups";
	private static final String USERS = "users";
	private static final String TRACKS = "tracks";
	private static final String SERVER_NAME = "serverName";
	private static final String GROUP_ID = "group_id";
	private static final String TRACKER_ID = "trackerId";
	private static final String USER_NAME = "userName";
	private static final String USER_COLOR = "userSetColor";
	private static final String GEN_COLOR = "genColor";
	private static final String SERVER_COLOR = "serverColor";
	private static final String DESCRIPTION = "description";
	private static final String POLICY = "policy";
	private static final String NAME = "name";
	private static final String ENABLED = "enabled";
	private static final String EXPIRE_TIME = "expireTime";
	private static final String DELETED = "deleted";
	
	private OsmandPreference<String> pref;
	private OsMoGroups service;
	private ConcurrentHashMap<String, OsMoGroup> groups = new ConcurrentHashMap<String, OsMoGroup>();

	public OsMoGroupsStorage(OsMoGroups service, OsmandPreference<String> pref) {
		this.service = service;
		this.pref = pref;
	}
	
	public Collection<OsMoGroup> getGroups() {
		return groups.values();
	}
	

	public void load() {
		String grp = pref.get();
		try {
			JSONObject obj = new JSONObject(grp);
			if(!obj.has(GROUPS)) {
				return;
			}
			JSONArray groups = obj.getJSONArray(GROUPS);
			for (int i = 0; i < groups.length(); i++) {
				JSONObject o = (JSONObject) groups.get(i);
				OsMoGroup group = new OsMoGroup();
				group.groupId = o.getString(GROUP_ID);
				if(o.has(NAME)) {
					group.name = o.getString(NAME);
				}
				if(o.has(EXPIRE_TIME)) {
					group.expireTime = o.getLong(EXPIRE_TIME);
				}
				if(o.has(USER_NAME)) {
					group.userName = o.getString(USER_NAME);
				}
				if(o.has(DESCRIPTION)) {
					group.description = o.getString(DESCRIPTION);
				}
				if(o.has(POLICY)) {
					group.policy = o.getString(POLICY);
				}
				if(o.has(ENABLED) && o.getBoolean(ENABLED)) {
					group.enabled = true;
				}
				parseGroupUsers(group, o);
//				parseGroupTracks(group, obj);
				this.groups.put(group.groupId, group);
			}
		} catch (JSONException e) {
			e.printStackTrace();
			service.showErrorMessage(e.getMessage());
		}
	}
	
	public void save() {
		JSONObject mainObj = new JSONObject();
		try {
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
				if(gr.description != null) {
					obj.put(DESCRIPTION, gr.description);
				}
				if(gr.policy != null) {
					obj.put(POLICY, gr.policy);
				}
				if(gr.expireTime != 0) {
					obj.put(EXPIRE_TIME, gr.expireTime);
				}
				if (gr.enabled) {
					obj.put(ENABLED, true);
				}
				obj.put(GROUP_ID, gr.groupId);
				ar.put(obj);
				saveGroupUsers(gr, obj);
				saveGroupTracks(gr, obj);
			}
			mainObj.put(GROUPS, ar);
		} catch (JSONException e) {
			e.printStackTrace();
			service.showErrorMessage(e.getMessage());
		}
		pref.set(mainObj.toString());
	}

	private void saveGroupUsers(OsMoGroup gr, JSONObject grObj) throws JSONException {
		JSONArray ar = new JSONArray();
		for(OsMoDevice u : gr.users.values()) {
			JSONObject obj = new JSONObject();
			if (u.userName != null) {
				obj.put(USER_NAME, u.userName);
			}
			if (u.serverName != null) {
				obj.put(SERVER_NAME, u.serverName);
			}
			if (u.userColor != 0) {
				obj.put(USER_COLOR, u.userColor);
			}
			if (u.genColor != 0) {
				obj.put(GEN_COLOR, u.genColor);
			}
			if (u.serverColor != 0) {
				obj.put(SERVER_COLOR, u.serverColor);
			}
			if (u.deleted != 0) {
				obj.put(DELETED, u.deleted);
			}
			if(u.enabled) {
				obj.put(ENABLED, u.enabled);
			}
			obj.put(TRACKER_ID, u.trackerId);
			
			ar.put(obj);
		}
		grObj.put(USERS, ar);
	}
	
	private void saveGroupTracks(OsMoGroup gr, JSONObject grObj) throws JSONException {
		if (gr.groupTracks.size() > 0) {
			JSONArray ar = new JSONArray();
			for (String u : gr.groupTracks) {
				JSONObject obj = new JSONObject();
				obj.put(NAME, u);
				ar.put(obj);
			}
			grObj.put(TRACKS, ar);
		}
	}

	protected void parseGroupUsers(OsMoGroup gr, JSONObject obj) throws JSONException {
		if(!obj.has(USERS)) {
			return;
		}
		JSONArray users = obj.getJSONArray(USERS);
		for (int i = 0; i < users.length(); i++) {
			JSONObject o = (JSONObject) users.get(i);
			OsMoDevice user = new OsMoDevice();
			user.group = gr;
			if(o.has(SERVER_NAME)) {
				user.serverName = o.getString(SERVER_NAME);
			}
			if(o.has(USER_NAME)) {
				user.userName = o.getString(USER_NAME);
			}
			if(o.has(SERVER_COLOR)) {
				user.serverColor = o.getInt(SERVER_COLOR);
			}
			if(o.has(USER_COLOR)) {
				user.userColor = o.getInt(USER_COLOR);
			}
			if(o.has(GEN_COLOR)) {
				user.genColor = o.getInt(GEN_COLOR);
			}
			if(o.has(DELETED)) {
				user.deleted = o.getLong(DELETED);
			}
			if(o.has(ENABLED) && o.getBoolean(ENABLED)) {
				user.enabled = true;
			}
			user.trackerId = o.getString(TRACKER_ID);
			gr.users.put(user.trackerId, user);
		}
	}
	
	protected void parseGroupTracks(OsMoGroup gr, JSONObject obj) throws JSONException {
		if(!obj.has(TRACKS)) {
			return;
		}
		JSONArray tracks = obj.getJSONArray(TRACKS);
		for (int i = 0; i < tracks.length(); i++) {
			JSONObject o = (JSONObject) tracks.get(i);
			if(o.has(NAME)) {
				gr.groupTracks.add(NAME);
			}
		}
	}

	public static class OsMoGroup {
		protected String name;
		protected String userName;
		protected String description;
		protected String policy;
		protected String groupId;
		protected boolean enabled;
		protected long expireTime;
		protected boolean active;
		protected boolean deleted;
		
		protected Map<String, OsMoDevice> users = new ConcurrentHashMap<String, OsMoDevice>();
		protected List<String> groupTracks = new ArrayList<String>();
		
		public List<OsMoDevice> getGroupUsers(String mygid) {
			// filter deleted
			List<OsMoDevice> dvs = new ArrayList<OsMoDevice>(users.size());
			for(OsMoDevice d : users.values()) {
				if(mygid != null && mygid.equals(d.trackerId)) {
					continue;
				}
				if(d.getDeletedTimestamp() == 0) {
					dvs.add(d);
				}
			}
			return dvs;
		}
		
		public List<String> getGroupTracks() {
			return groupTracks;
		}
		
		public List<OsMoDevice> getVisibleGroupUsers(String mygid) {
			if(!isActive() && !isMainGroup()) {
				return Collections.emptyList();
			}
			return getGroupUsers(mygid);
		}
		
		public boolean isDeleted() {
			return deleted;
		}
		
		public String getDescription() {
			return description;
		}
		
		public String getPolicy() {
			return policy;
		}
		
		public long getExpireTime() {
			return expireTime;
		}
		
		public boolean isMainGroup() {
			return groupId == null;
		}
		
		public String getGroupId() {
			return groupId;
		}
		
		public boolean isActive() {
			return active;
		}
		
		public boolean isEnabled() {
			return enabled;
		}
		
		public String getVisibleName(Context ctx){
			if(isMainGroup()) {
				return ctx.getString(R.string.osmo_connected_devices);
			}
			if(userName != null && userName.length() > 0) {
				return userName;
			}
			if(name == null) {
				return "";
			}
			return name;
		}

		public OsMoDevice updateLastLocation(String trackerId, Location location) {
			OsMoDevice d = users.get(trackerId);
			if(d != null) {
				d.setLastLocation(location);
			}
			return d;
		}
	}
	
	public static class OsMoMessage {
		protected long timestamp;
		protected LatLon location;
		protected String text;
		protected OsMoDevice user;
		
		public String getText() {
			return text;
		}
		
		public long getTimestamp() {
			return timestamp;
		}
		
		public LatLon getLocation() {
			return location;
		}
		
		public OsMoDevice getUser() {
			return user;
		}
	}
	
	public static class OsMoDevice {
		protected String serverName;
		protected String userName;
		protected int serverColor;
		protected int userColor;
		protected int genColor;
		protected String trackerId;
		protected boolean enabled;
		protected boolean active;
		protected long deleted;
		protected OsMoGroup group ;
		
		protected ConcurrentLinkedQueue<Location> previousLocations = new java.util.concurrent.ConcurrentLinkedQueue<Location>();
		protected List<OsMoMessage> messages = new ArrayList<OsMoMessage>();
		protected long lastOnline;
		protected Location lastLocation;

		
		public List<OsMoMessage> getMessages() {
			return messages;
		}
		
		public ConcurrentLinkedQueue<Location> getPreviousLocations(long threshold) {
			while(!previousLocations.isEmpty() && previousLocations.peek().getTime() < threshold) {
				previousLocations.poll();
			}
			return previousLocations;
		}
		
		public void setLastLocation(Location lastLocation) {
			if(this.lastLocation != null) {
				previousLocations.add(this.lastLocation);
			}
			this.lastLocation = lastLocation;
			if (lastLocation != null) {
				setLastOnline(lastLocation.getTime());
			}
		}
		
		public Location getLastLocation() {
			return lastLocation;
		}
		
		public void setLastOnline(long lastOnline) {
			this.lastOnline = lastOnline;
		}
		
		public long getLastOnline() {
			return lastOnline;
		}
		
		public boolean isEnabled() {
			return enabled;
		}
		
		public boolean isActive() {
			return active;
		}
		
		public long getDeletedTimestamp() {
			return deleted;
		}
		
		public OsMoGroup getGroup() {
			return group;
		}
		
		public String getTrackerId() {
			return trackerId;
		}
		
		
		
		public int getColor() {
			if(userColor != 0) {
				return userColor;
			}
			if(serverColor != 0) {
				return serverColor ;
			}
			return genColor;
		}

		public int getNonActiveColor() {
			int color = getColor();
		    int alpha = Math.round(Color.alpha(color) * 0.3f);
		    int red = Color.red(color);
		    int green = Color.green(color);
		    int blue = Color.blue(color);
		    return Color.argb(alpha, red, green, blue);
		}

		public String getVisibleName(){
			if(userName != null && userName.length() > 0) {
				return userName;
			}
			if(serverName == null || serverName.length() == 0) {
				return trackerId;
			}
			return serverName;
		}
	}

	public OsMoGroup getGroup(String gid) {
		return groups.get(gid);
	}

	public void deleteGroup(OsMoGroup gr) {
		groups.remove(gr.groupId);
		gr.deleted = true;
	}

	public void addGroup(OsMoGroup g) {
		groups.put(g.groupId, g);
		g.deleted = false;
		
	}

	public void mergeGroups(Map<String, OsMoGroup> groupsToMerge) {
		Set<String> toDelete = new HashSet<String>(groups.keySet());
		if (groupsToMerge != null) {
			Set<String> toMerge = groupsToMerge.keySet();
			for (String id: toMerge) {
				if (toDelete.contains(id)) {
					toDelete.remove(id);
				} else {
					addGroup(groupsToMerge.get(id));
				}
			}
		}
		for (String id: toDelete) {
			OsMoGroup group = getGroup(id); 
			if (group != null) {
				deleteGroup(group);
			}
		}
	}
	
	public void clearGroups() {
		groups.clear();
	}
	
}
