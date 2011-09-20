package net.osmand.osm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Way extends Entity {
	
	// lazy loading
	private List<Long> nodeIds = null;
	private List<Node> nodes = null;

	public Way(long id) {
		super(id);
	}
	
	public void addNode(long id){
		if(nodeIds == null){
			nodeIds = new ArrayList<Long>();
		}
		nodeIds.add(id);
	}
	
	public void addNode(Node n){
		if(nodeIds == null){
			nodeIds = new ArrayList<Long>();
		}
		if(nodes == null){
			nodes = new ArrayList<Node>();
		}
		nodeIds.add(n.getId());
		nodes.add(n);
	}
	
	public void addNode(Node n, int index){
		if(nodeIds == null){
			nodeIds = new ArrayList<Long>();
		}
		if(nodes == null){
			nodes = new ArrayList<Node>();
		}
		nodeIds.add(index, n.getId());
		nodes.add(index, n);
	}
	
	public Long removeNodeByIndex(int i){
		if(nodeIds == null){
			return null;
		}
		Long toReturn = nodeIds.remove(i);
		if(nodes != null && nodes.size() > i){
			nodes.remove(i);
		}
		return toReturn;
	}
	
	public List<Long> getNodeIds(){
		if(nodeIds == null){
			return Collections.emptyList();
		}
		return nodeIds;
	}
	
	public List<EntityId> getEntityIds(){
		if(nodeIds == null){
			return Collections.emptyList();
		}
		List<EntityId> ls = new ArrayList<EntityId>();
		for(Long l : nodeIds){
			ls.add(new EntityId(EntityType.NODE, l));
		}
		return ls;
	}
	
	public List<Node> getNodes() {
		if(nodes == null){
			return Collections.emptyList();
		}
		return nodes;
	}
	
	@Override
	public void initializeLinks(Map<EntityId, Entity> entities) {
		if (nodeIds != null) {
			if(nodes == null){
				 nodes = new ArrayList<Node>();
			} else {
				nodes.clear();
			}
			int nIsize = nodeIds.size();
			for (int i = 0; i < nIsize; i++) {
				nodes.add((Node) entities.get(new EntityId(EntityType.NODE,nodeIds.get(i))));
			}
		}
	}
	
	@Override
	public LatLon getLatLon() {
		if(nodes == null){
			return null;
		}
		return MapUtils.getWeightCenterForNodes(nodes);
	}
	
	

}
