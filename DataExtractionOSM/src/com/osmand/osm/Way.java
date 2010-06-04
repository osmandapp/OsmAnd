package com.osmand.osm;

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
	
	public Long removeNodeByIndex(int i){
		if(nodeIds == null){
			return null;
		}
		return nodeIds.remove(i);
	}
	
	public List<Long> getNodeIds(){
		if(nodeIds == null){
			return Collections.emptyList();
		}
		return nodeIds;
	}
	
	public List<Node> getNodes() {
		if(nodes == null){
			return Collections.emptyList();
		}
		return nodes;
	}
	
	@Override
	public void initializeLinks(Map<Long, Entity> entities) {
		if (nodeIds != null) {
			if(nodes == null){
				 nodes = new ArrayList<Node>();
			} else {
				nodes.clear();
			}
			for (int i = 0; i < nodeIds.size(); i++) {
				if(entities.get(nodeIds.get(i)) instanceof Node){
					nodes.add((Node) entities.get(nodeIds.get(i)));
				} else {
					nodes.add(null);
				}
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
