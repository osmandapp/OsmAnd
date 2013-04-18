package net.osmand.data;

import java.util.ArrayList;
import java.util.List;

import net.osmand.util.MapUtils;

public class TransportRoute extends MapObject {
	private List<TransportStop> forwardStops = new ArrayList<TransportStop>();
	private List<TransportStop> backwardStops = new ArrayList<TransportStop>();
	private String ref;
	private String operator;
	private String type;
	private Integer dist = null;
	
	public TransportRoute(){
	}
	
	public List<TransportStop> getForwardStops() {
		return forwardStops;
	}
	
	public List<TransportStop> getBackwardStops() {
		return backwardStops;
	}
	
	public String getRef() {
		return ref;
	}
	
	public void setRef(String ref) {
		this.ref = ref;
	}
	
	public String getOperator() {
		return operator;
	}
	
	public void setOperator(String operator) {
		this.operator = operator;
	}
	
	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public int getDistance(){
		if(dist == null){
			dist = getAvgBothDistance();
		}
		return dist;
	}
	
	public void setDistance(Integer dist) {
		this.dist = dist;
	}
	
	public int getAvgBothDistance(){
		int d = 0;
		int bSsize = backwardStops.size(); 
		int fSsize = forwardStops.size(); 
		for(int i=1; i< bSsize; i++){
			d += MapUtils.getDistance(backwardStops.get(i-1).getLocation(), backwardStops.get(i).getLocation());
		}
		for(int i=1; i< fSsize; i++){
			d += MapUtils.getDistance(forwardStops.get(i-1).getLocation(), forwardStops.get(i).getLocation());
		}
		return d;
	}
}