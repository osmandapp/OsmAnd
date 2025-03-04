package net.osmand.osm.edit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.osmand.data.LatLon;

import static net.osmand.osm.edit.OSMSettings.*;

public class Relation extends Entity {
	
	public static class RelationMember {
		private EntityId entityId;
		private Entity entity;
		private String role;
		
		public RelationMember(EntityId entityId, String role) {
			this.entityId = entityId;
			this.role = role;
		}

		public EntityId getEntityId() {
			if(entityId == null && entity != null) {
				return EntityId.valueOf(entity);
			}
			return entityId;
		}
		
		public String getRole() {
			return role;
		}
		
		public Entity getEntity() {
			return entity;
		}

		public boolean hasName() {
			return entity != null && entity.getTag(OSMTagKey.NAME) != null;
		}

		@Override
		public String toString() {
			return entityId.toString() + " " + role;
		}
		
	}
	
	// lazy loading
	List<RelationMember> members = null;
	
	public Relation(long id) {
		super(id);
	}
	
	public void addMember(Long id, EntityType type, String role){
		addMember(new EntityId(type, id), role);
	}
	
	public void addMember(EntityId id, String role){
		if(members == null){
			members = new ArrayList<>(); 
		}
		members.add(new RelationMember(id, role));
	}
	
	public List<RelationMember> getMembers(String role) {
		if (members == null) {
			return Collections.emptyList();
		}
		if (role == null) {
			return members;
		}
		List<RelationMember> l = new ArrayList<>();
		for (RelationMember m : members) {
			if (role.equals(m.role)) {
				l.add(m);
			}
		}
		return l;
	}
	
	public List<Entity> getMemberEntities(String role) {
		if (members == null) {
			return Collections.emptyList();
		}
		List<Entity> l = new ArrayList<>();
		for (RelationMember m : members) {
			if (role == null || role.equals(m.role)) {
				if(m.entity != null) {
					l.add(m.entity);
				}
			}
		}
		return l;
	}
	
	public List<RelationMember> getMembers() {
		if(members == null){
			return Collections.emptyList();
		}
		return members;
	}
	
	
	@Override
	public void initializeLinks(Map<EntityId, Entity> entities){
		if (members != null) {
			for (RelationMember rm : members) {
				if (rm.entityId != null && entities.containsKey(rm.entityId)) {
					rm.entity = entities.get(rm.entityId);
				}
			}
		}
	}
	

	@Override
	public LatLon getLatLon() {
		return null;
	}
	
	public void update(RelationMember r, EntityId newEntityId) {
		r.entity = null;
		r.entityId = newEntityId;
	}
	
	public void updateRole(RelationMember r, String newRole) {
		r.role = newRole;
	}
	
	public boolean remove(EntityId key) {
		if(members != null) {
			Iterator<RelationMember> it = members.iterator();
			while(it.hasNext()) {
				RelationMember rm = it.next();
				if(key.equals(rm.getEntityId())) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean remove(RelationMember key) {
		if(members != null) {
			Iterator<RelationMember> it = members.iterator();
			while(it.hasNext()) {
				RelationMember rm = it.next();
				if(rm == key) {
					it.remove();
					return true;
				}
			}
		}
		return false;
	}

}
