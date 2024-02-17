package net.osmand.data;

public class IncompleteTransportRoute {
	private long routeId;
	private long routeOffset = -1;
	private String operator;
	private String type;
	private String ref;
	private IncompleteTransportRoute nextLinkedRoute;
	
	public IncompleteTransportRoute getNextLinkedRoute() {
		return nextLinkedRoute;
	}
	
	public void setNextLinkedRoute(IncompleteTransportRoute nextLinkedRoute) {
		if (this.nextLinkedRoute == null) {
			this.nextLinkedRoute = nextLinkedRoute;
		} else {
			this.nextLinkedRoute.setNextLinkedRoute(nextLinkedRoute);
		}
	}
	
	public long getRouteId() {
		return routeId;
	}

	public void setRouteId(long routeId) {
		this.routeId = routeId;
	}

	public long getRouteOffset() {
		return routeOffset;
	}

	public void setRouteOffset(long routeOffset) {
		this.routeOffset = routeOffset;
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

	public String getRef() {
		return ref;
	}

	public void setRef(String ref) {
		this.ref = ref;
	}

}