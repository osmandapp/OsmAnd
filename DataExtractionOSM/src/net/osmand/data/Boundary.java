package net.osmand.data;



public class Boundary 
	extends Multipolygon 
{
	
	private long boundaryId;
	private String name;
	private int adminLevel;
	
	private long adminCenterId;
	
	public Boundary() {
	}
	
	public long getBoundaryId() {
		return boundaryId;
	}
	
	public void setBoundaryId(long boundaryId) {
		this.boundaryId = boundaryId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getAdminLevel() {
		return adminLevel;
	}

	public void setAdminLevel(int adminLevel) {
		this.adminLevel = adminLevel;
	}
	
	@Override
	public String toString() {
		return  getName() + " alevel:" + getAdminLevel() + " type: has opened polygons:" + hasOpenedPolygons() + " no. of outer polygons:" + countOuterPolygons();
	}

	public void setAdminCenterId(long l) {
		this.adminCenterId = l;
	}
	
	public long getAdminCenterId() {
		return adminCenterId;
	}

}
