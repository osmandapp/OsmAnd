package net.osmand.data.preparation;

import gnu.trove.set.hash.TLongHashSet;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.osmand.data.Building;
import net.osmand.data.City;
import net.osmand.data.preparation.DBStreetDAO.SimpleStreet;
import net.osmand.osm.Entity;
import net.osmand.osm.LatLon;
import net.osmand.osm.Way;

public class CachedDBStreetDAO extends DBStreetDAO
{
	private Map<String, SimpleStreet> addressStreetLocalMap = new HashMap<String, SimpleStreet>();
	private TLongHashSet addressBuildingLocalSet = new TLongHashSet();
	private TLongHashSet addressStreetNodeLocalSet = new TLongHashSet();

	@Override
	public SimpleStreet findStreet(String name, City city, String cityPart) {
		return addressStreetLocalMap.get(createStreetUniqueName(name, city, cityPart)); //$NON-NLS-1$
	}

	@Override
	public SimpleStreet findStreet(String name, City city) {
		return addressStreetLocalMap.get(createStreetUniqueName(name, city)); //$NON-NLS-1$
	}

	private String createStreetUniqueName(String name, City city, String cityPart) {
		return new StringBuilder().append(name).append('_').append(city.getId()).append('_').append(cityPart).toString();
	}

	private String createStreetUniqueName(String name, City city) {
		return new StringBuilder().append(name).append('_').append(city.getId()).toString();
	}

	@Override
	protected void writeStreetWayNodes(Set<Long> streetId, Way way)
			throws SQLException {
		super.writeStreetWayNodes(streetId, way);
		addressStreetNodeLocalSet.add(way.getId());
	}
	
	@Override
	protected void writeBuilding(Set<Long> streetId, Building building)
			throws SQLException {
		super.writeBuilding(streetId, building);
		addressBuildingLocalSet.add(building.getId());
	}
	
	@Override
	public long insertStreet(String name, String nameEn, LatLon location, City city, String cityPart) throws SQLException {
		//batch the insert
		long streetId = fillInsertStreetStatement(name, nameEn, location, city, cityPart);
		addBatch(addressStreetStat);
		SimpleStreet ss = new SimpleStreet(streetId,name,cityPart,location);
		addressStreetLocalMap.put(createStreetUniqueName(name, city, cityPart), ss); 
		addressStreetLocalMap.put(createStreetUniqueName(name, city), ss);
		return streetId;
	}
	
	@Override
	public SimpleStreet updateStreetCityPart(SimpleStreet street, City city, String cityPart) throws SQLException {
		commit(); //we are doing batch updates, so we must commit before this update
		super.updateStreetCityPart(street, city, cityPart);
		SimpleStreet updatedSS = new SimpleStreet(street.getId(),street.getName(),cityPart,street.getLocation());
		addressStreetLocalMap.put(createStreetUniqueName(street.getName(), city), updatedSS);
		addressStreetLocalMap.put(createStreetUniqueName(street.getName(), city, cityPart), updatedSS);
		return updatedSS;
	}
	
	@Override
	public boolean findBuilding(Entity e) {
		return addressBuildingLocalSet.contains(e.getId());
	}
	
	@Override
	public boolean findStreetNode(Entity e) {
		return addressStreetNodeLocalSet.contains(e.getId());
	}
}