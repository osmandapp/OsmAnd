package net.osmand.osm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Relation extends Entity {
	
	// lazy loading
	Map<EntityId, String> members = null;
	Map<Entity, String> memberEntities = null;
	
	
	public Relation(long id) {
		super(id);
	}
	
	public void addMember(Long id, EntityType type, String role){
		if(members == null){
			members = new LinkedHashMap<EntityId, String>(); 
		}
		members.put(new EntityId(type, id), role);
	}
	
	public String removeMember(EntityType e, Long id){
		if(members == null){
			return null; 
		}
		return members.remove(id);
	}
	
	public String getRole(Entity e){
		return members.get(e.getId());
	}
	
	public String getRole(Long id){
		return members.get(id);
	}
	
	public Collection<EntityId> getMemberIds() {
		return getMemberIds(null);
	}
	
	public Map<EntityId, String> getMembersMap() {
		if(members == null){
			return Collections.emptyMap();
		}
		return Collections.unmodifiableMap(members);
	}
	
	public Collection<EntityId> getMemberIds(String role) {
		if (members == null) {
			return Collections.emptyList();
		}
		if (role == null) {
			return members.keySet();
		}
		List<EntityId> l = new ArrayList<EntityId>();
		for (EntityId m : members.keySet()) {
			if (role.equals(members.get(m))) {
				l.add(m);
			}
		}
		return l;
	}
	
	public Map<Entity, String> getMemberEntities() {
		if(memberEntities == null){
			return Collections.emptyMap();
		}
		return memberEntities;
	}
	
	public Collection<Entity> getMembers(String role) {
		if (memberEntities == null) {
			return Collections.emptyList();
		}
		if (role == null) {
			return memberEntities.keySet();
		}
		List<Entity> l = new ArrayList<Entity>();
		for (Entity m : memberEntities.keySet()) {
			if (role.equals(memberEntities.get(m))) {
				l.add(m);
			}
		}
		return l;
	}
	
	@Override
	public void initializeLinks(Map<EntityId, Entity> entities){
		if (members != null) {
			if(memberEntities == null){
				memberEntities = new LinkedHashMap<Entity, String>();
			} else {
				memberEntities.clear();
			}
			for(EntityId l : members.keySet()){
				if(l != null && entities.get(l) != null){
					memberEntities.put(entities.get(l), members.get(l));
				}
			}
		}
	}
	

	@Override
	public LatLon getLatLon() {
		return null;
	}

}
