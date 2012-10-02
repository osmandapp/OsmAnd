package net.osmand.osm;

import java.util.ArrayList;
import java.util.List;

public class WayChain  {
	private List<Way> ways = new ArrayList<Way>(1);
	private List<Node> chainNodes;
	
	public WayChain(Way w) {
		ways.add(w);
	}
	
	public List<Way> getWays() {
		return ways;
	}
	
	public void append(Way w) {
		ways.add(w);
	}
	
	public void append(WayChain w) {
		ways.addAll(w.ways);
	}
	
	public void prepend(Way w) {
		ways.add(0, w);
	}
	
	public long getFistNode(){
		return ways.get(0).getFirstNodeId();
	}
	
	public long getLastNode() {
		return ways.get(ways.size() - 1).getLastNodeId();
	}
	
	public boolean isIncomplete() {
		for (int j = 0; j < ways.size(); j++) {
			if(ways.get(j).getNodes().size() != ways.get(j).getNodeIds().size()) {
				return true;
			}
		}
		return false;
	}
	
	public List<Node> getChainNodes(){
		if(chainNodes == null) {
			chainNodes = new ArrayList<Node>();
			for(int j = 0; j< ways.size(); j++) {
				List<Node> ns = ways.get(j).getNodes();
				if(j == 0 && ns.get(0) != null) {
					chainNodes.add(ns.get(0));
				}
				for(int i=1; i<ns.size(); i++){
					if(ns.get(i) != null) {
						chainNodes.add(ns.get(i));
					}
				}
			}
			
		}
		return chainNodes;
	}
	
}
