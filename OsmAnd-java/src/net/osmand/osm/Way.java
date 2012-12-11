package net.osmand.osm;

import gnu.trove.list.array.TLongArrayList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Way extends Entity {
	
	// lazy loading
	private TLongArrayList nodeIds = null;
	private List<Node> nodes = null;

	public Way(long id) {
		super(id);
	}
	
	public Way(Way w) {
		super(w.getId());
		if (w.nodeIds != null) {
			nodeIds = new TLongArrayList(w.nodeIds);
		}
		if (w.nodes != null) {
			nodes = new ArrayList<Node>(w.nodes);
		}
	}
	
	public Way(long id, List<Node> nodes) {
		super(id);
		this.nodes = new ArrayList<Node>(nodes);
		nodeIds = new TLongArrayList(nodes.size());
		for (Node n : nodes) {
			nodeIds.add(n.getId());
		}
	}
	
	public void addNode(long id){
		if(nodeIds == null){
			nodeIds = new TLongArrayList();
		}
		nodeIds.add(id);
	}
	
	public long getFirstNodeId(){
		if(nodeIds == null){
			return -1;
		}
		return nodeIds.get(0);
	}
	
	public long getLastNodeId(){
		if(nodeIds == null){
			return -1;
		}
		return nodeIds.get(nodeIds.size() - 1);
	}
	
	public void addNode(Node n){
		if(nodeIds == null){
			nodeIds = new TLongArrayList();
		}
		if(nodes == null){
			nodes = new ArrayList<Node>();
		}
		nodeIds.add(n.getId());
		nodes.add(n);
	}
	
	public void addNode(Node n, int index){
		if(nodeIds == null){
			nodeIds = new TLongArrayList();
		}
		if(nodes == null){
			nodes = new ArrayList<Node>();
		}
		nodeIds.insert(index, n.getId());
		nodes.add(index, n);
	}
	
	public long removeNodeByIndex(int i){
		if(nodeIds == null){
			return -1;
		}
		long toReturn = nodeIds.removeAt(i);
		if(nodes != null && nodes.size() > i){
			nodes.remove(i);
		}
		return toReturn;
	}
	
	public TLongArrayList getNodeIds(){
		if(nodeIds == null){
			return new TLongArrayList(0);
		}
		return nodeIds;
	}
	
	public List<EntityId> getEntityIds(){
		if(nodeIds == null){
			return Collections.emptyList();
		}
		List<EntityId> ls = new ArrayList<EntityId>();
		for (int i = 0; i < nodeIds.size(); i++) {
			ls.add(new EntityId(EntityType.NODE, nodeIds.get(i)));
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
