package net.osmand.data.preparation;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import net.osmand.Algoritms;
import net.osmand.IProgress;
import net.osmand.binary.BinaryMapIndexWriter;
import net.osmand.data.Boundary;
import net.osmand.data.Building;
import net.osmand.data.City;
import net.osmand.data.DataTileManager;
import net.osmand.data.Street;
import net.osmand.data.City.CityType;
import net.osmand.osm.Entity;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapUtils;
import net.osmand.osm.Node;
import net.osmand.osm.Relation;
import net.osmand.osm.Way;
import net.osmand.osm.Entity.EntityId;
import net.osmand.osm.OSMSettings.OSMTagKey;
import net.osmand.swing.Messages;
import net.sf.junidecode.Junidecode;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class IndexAddressCreator extends AbstractIndexPartCreator{
	
	private static final Log log = LogFactory.getLog(IndexAddressCreator.class);
	
	
	private PreparedStatement addressCityStat;
	private PreparedStatement addressStreetStat;
	private PreparedStatement addressBuildingStat;
	private PreparedStatement addressStreetNodeStat;

	// MEMORY address : choose what to use ?
	private boolean loadInMemory = true;
	private PreparedStatement addressSearchStreetStat;
	private PreparedStatement addressSearchBuildingStat;
	private PreparedStatement addressSearchStreetNodeStat;

	private Map<String, Long> addressStreetLocalMap = new LinkedHashMap<String, Long>();
	private Set<Long> addressBuildingLocalSet = new LinkedHashSet<Long>();
	private Set<Long> addressStreetNodeLocalSet = new LinkedHashSet<Long>();

	// MEMORY address : address structure
	// load it in memory
	private Map<EntityId, City> cities = new LinkedHashMap<EntityId, City>();
	private DataTileManager<City> cityVillageManager = new DataTileManager<City>(13);
	private DataTileManager<City> cityManager = new DataTileManager<City>(10);
	private List<Relation> postalCodeRelations = new ArrayList<Relation>();
	private Map<City, Boundary> citiBoundaries = new LinkedHashMap<City, Boundary>();
	private Set<Long> visitedBoundaryWays = new HashSet<Long>();
	
	private boolean normalizeStreets; 
	private String[] normalizeDefaultSuffixes;
	private String[] normalizeSuffixes;
	
	private String cityAdminLevel;
	private boolean saveAddressWays;
	
	// TODO
	Connection mapConnection;
	

	public IndexAddressCreator(){
	}
	
	
	public void initSettings(boolean normalizeStreets, String[] normalizeDefaultSuffixes, String[] normalizeSuffixes,
			boolean saveAddressWays, String cityAdminLevel) {
		cities.clear();
		cityManager.clear();
		postalCodeRelations.clear();
		citiBoundaries.clear();
		this.normalizeStreets = normalizeStreets;
		this.normalizeDefaultSuffixes = normalizeDefaultSuffixes;
		this.normalizeSuffixes = normalizeSuffixes;
		this.cityAdminLevel = cityAdminLevel;
		this.saveAddressWays = saveAddressWays;
	}

	public void registerCityIfNeeded(Entity e) {
		if (e instanceof Node && e.getTag(OSMTagKey.PLACE) != null) {
			City city = new City((Node) e);
			if (city.getType() != null && !Algoritms.isEmpty(city.getName())) {
				if (city.getType() == CityType.CITY || city.getType() == CityType.TOWN) {
					cityManager.registerObject(((Node) e).getLatitude(), ((Node) e).getLongitude(), city);
				} else {
					cityVillageManager.registerObject(((Node) e).getLatitude(), ((Node) e).getLongitude(), city);
				}
				cities.put(city.getEntityId(), city);
			}
		}
	}
	
	public void indexBoundariesRelation(Entity e, OsmDbAccessorContext ctx) throws SQLException {
		if ("administrative".equals(e.getTag(OSMTagKey.BOUNDARY)) && (e instanceof Relation || e instanceof Way)) {
			String adminLevel = e.getTag("admin_level");
			Boundary boundary = null;
			if (cityAdminLevel.equals(adminLevel)) {
				if (e instanceof Relation) {
					Relation i = (Relation) e;
					ctx.loadEntityData(i, true);
					boundary = new Boundary();
					if (i.getTag(OSMTagKey.NAME) != null) {
						boundary.setName(i.getTag(OSMTagKey.NAME));
					}
					boundary.setBoundaryId(i.getId());
					Map<Entity, String> entities = i.getMemberEntities();
					for (Entity es : entities.keySet()) {
						if (es instanceof Way) {
							boolean inner = "inner".equals(entities.get(es)); //$NON-NLS-1$
							if (inner) {
								boundary.getInnerWays().add((Way) es);
							} else {
								String wName = es.getTag(OSMTagKey.NAME);
								// if name are not equal keep the way for further check (it could be different suburb)
								if (Algoritms.objectEquals(wName, boundary.getName()) || wName == null) {
									visitedBoundaryWays.add(es.getId());
								}
								boundary.getOuterWays().add((Way) es);
							}
						}
					}
				} else if (e instanceof Way) {
					if (!visitedBoundaryWays.contains(e.getId())) {
						boundary = new Boundary();
						if (e.getTag(OSMTagKey.NAME) != null) {
							boundary.setName(e.getTag(OSMTagKey.NAME));
						}
						boundary.setBoundaryId(e.getId());
						boundary.getOuterWays().add((Way) e);

					}

				}
			}

			if (boundary != null && boundary.getCenterPoint() != null) {
				LatLon point = boundary.getCenterPoint();
				boolean cityFound = false;
				boolean containsCityInside = false;
				for (City c : cityManager.getClosestObjects(point.getLatitude(), point.getLongitude(), 3)) {
					if (boundary.containsPoint(c.getLocation())) {
						if (boundary.getName() == null || boundary.getName().equalsIgnoreCase(c.getName())) {
							citiBoundaries.put(c, boundary);
							cityFound = true;
							containsCityInside = true;
						}
					}
				}
				// TODO mark all suburbs inside city as is_in tag (!) or use another solution
				if (!cityFound) {
					for (City c : cityVillageManager.getClosestObjects(point.getLatitude(), point.getLongitude(), 3)) {
						if (boundary.containsPoint(c.getLocation())) {
							if (boundary.getName() == null || boundary.getName().equalsIgnoreCase(c.getName())) {
								citiBoundaries.put(c, boundary);
								cityFound = true;
							}
						}
					}
				}
				if (!cityFound && boundary.getName() != null) {
					// / create new city for named boundary very rare case that's why do not proper generate id
					// however it could be a problem
					City nCity = new City(containsCityInside ? CityType.CITY : CityType.SUBURB);
					nCity.setLocation(point.getLatitude(), point.getLongitude());
					nCity.setId(-boundary.getBoundaryId());
					nCity.setName(boundary.getName());
					citiBoundaries.put(nCity, boundary);
					cityVillageManager.registerObject(point.getLatitude(), point.getLongitude(), nCity);

					writeCity(nCity);
					// commit to put all cities
					if (pStatements.get(addressCityStat) > 0) {
						addressCityStat.executeBatch();
						pStatements.put(addressCityStat, 0);
					}
				}
			}
		}
	}
	

	public void indexAddressRelation(Relation i, OsmDbAccessorContext ctx) throws SQLException {
		if (i instanceof Relation && "address".equals(i.getTag(OSMTagKey.TYPE))) { //$NON-NLS-1$
			String type = i.getTag(OSMTagKey.ADDRESS_TYPE);
			boolean house = "house".equals(type); //$NON-NLS-1$
			boolean street = "a6".equals(type); //$NON-NLS-1$
			if (house || street) {
				// try to find appropriate city/street
				City c = null;
				// load with member ways with their nodes and tags !
				ctx.loadEntityData(i, true);

				Collection<Entity> members = i.getMembers("is_in"); //$NON-NLS-1$
				Relation a3 = null;
				Relation a6 = null;
				if (!members.isEmpty()) {
					if (street) {
						a6 = i;
					}
					Entity in = members.iterator().next();
					ctx.loadEntityData(in, true);
					if (in instanceof Relation) {
						// go one level up for house
						if (house) {
							a6 = (Relation) in;
							members = ((Relation) in).getMembers("is_in"); //$NON-NLS-1$
							if (!members.isEmpty()) {
								in = members.iterator().next();
								ctx.loadEntityData(in, true);
								if (in instanceof Relation) {
									a3 = (Relation) in;
								}
							}

						} else {
							a3 = (Relation) in;
						}
					}
				}

				if (a3 != null) {
					Collection<EntityId> memberIds = a3.getMemberIds("label"); //$NON-NLS-1$
					if (!memberIds.isEmpty()) {
						c = cities.get(memberIds.iterator().next());
					}
				}
				if (c != null && a6 != null) {
					String name = a6.getTag(OSMTagKey.NAME);

					if (name != null) {
						LatLon location = c.getLocation();
						for (Entity e : i.getMembers(null)) {
							if (e instanceof Way) {
								LatLon l = ((Way) e).getLatLon();
								if (l != null) {
									location = l;
									break;
								}
							}
						}

						Long streetId = getStreetInCity(c, name, location, (a6.getId() << 2) | 2);
						if (streetId == null) {
							return;
						}
						if (street) {
							for (Map.Entry<Entity, String> r : i.getMemberEntities().entrySet()) {
								if ("street".equals(r.getValue())) { //$NON-NLS-1$
									if (r.getKey() instanceof Way && saveAddressWays) {
										writeStreetWayNodes(streetId, (Way) r.getKey());
										if (loadInMemory) {
											addressStreetNodeLocalSet.add(r.getKey().getId());
										}
									}
								} else if ("house".equals(r.getValue())) { //$NON-NLS-1$
									// will be registered further in other case
									if (!(r.getKey() instanceof Relation)) {
										String hno = r.getKey().getTag(OSMTagKey.ADDR_HOUSE_NUMBER);
										if (hno != null) {
											Building building = new Building(r.getKey());
											building.setName(hno);
											writeBuilding(streetId, building);
											if (loadInMemory) {
												addressBuildingLocalSet.add(r.getKey().getId());
											}
										}
									}
								}
							}
						} else {
							String hno = i.getTag(OSMTagKey.ADDRESS_HOUSE);
							if (hno == null) {
								hno = i.getTag(OSMTagKey.ADDR_HOUSE_NUMBER);
							}
							if (hno == null) {
								hno = i.getTag(OSMTagKey.NAME);
							}
							members = i.getMembers("border"); //$NON-NLS-1$
							if (!members.isEmpty()) {
								Entity border = members.iterator().next();
								if (border != null) {
									EntityId id = EntityId.valueOf(border);
									// special check that address do not contain twice in a3 - border and separate a6
									if (!a6.getMemberIds().contains(id)) {
										Building building = new Building(border);
										if (building.getLocation() != null) {
											building.setName(hno);
											writeBuilding(streetId, building);
											if (loadInMemory) {
												addressBuildingLocalSet.add(id.getId());
											}
										} else {
											log.error("Strange border " + id + " location couldn't be found");
										}
									}
								}
							} else {
								log.info("For relation " + i.getId() + " border not found"); //$NON-NLS-1$ //$NON-NLS-2$
							}

						}
					}
				}
			}
		}
	}
	

	public String normalizeStreetName(String name) {
		name = name.trim();
		if (normalizeStreets) {
			String newName = name;
			boolean processed = newName.length() != name.length();
			for (String ch : normalizeDefaultSuffixes) {
				int ind = checkSuffix(newName, ch);
				if (ind != -1) {
					newName = cutSuffix(newName, ind, ch.length());
					processed = true;
					break;
				}
			}

			if (!processed) {
				for (String ch : normalizeSuffixes) {
					int ind = checkSuffix(newName, ch);
					if (ind != -1) {
						newName = putSuffixToEnd(newName, ind, ch.length());
						processed = true;
						break;
					}
				}
			}
			if (processed) {
				return newName;
			}
		}
		return name;
	}

	private int checkSuffix(String name, String suffix) {
		int i = -1;
		boolean searchAgain = false;
		do {
			i = name.indexOf(suffix, i);
			searchAgain = false;
			if (i > 0) {
				if (Character.isLetterOrDigit(name.charAt(i - 1))) {
					i++;
					searchAgain = true;
				}
			}
		} while (searchAgain);
		return i;
	}

	private String cutSuffix(String name, int ind, int suffixLength) {
		String newName = name.substring(0, ind);
		if (name.length() > ind + suffixLength + 1) {
			newName += name.substring(ind + suffixLength + 1);
		}
		return newName.trim();
	}

	private String putSuffixToEnd(String name, int ind, int suffixLength) {
		if (name.length() <= ind + suffixLength) {
			return name;

		}
		String newName;
		if (ind > 0) {
			newName = name.substring(0, ind);
			newName += name.substring(ind + suffixLength);
			newName += name.substring(ind - 1, ind + suffixLength);
		} else {
			newName = name.substring(suffixLength + 1) + name.charAt(suffixLength) + name.substring(0, suffixLength);
		}

		return newName.trim();
	}

	public Long getStreetInCity(City city, String name, LatLon location, long initId) throws SQLException {
		if (name == null || city == null) {
			return null;
		}
		Long foundId = null;

		name = normalizeStreetName(name);
		if (loadInMemory) {
			foundId = addressStreetLocalMap.get(name + "_" + city.getId()); //$NON-NLS-1$
		} else {
			addressSearchStreetStat.setLong(1, city.getId());
			addressSearchStreetStat.setString(2, name);
			ResultSet rs = addressSearchStreetStat.executeQuery();
			if (rs.next()) {
				foundId = rs.getLong(1);
			}
			rs.close();
		}

		if (foundId == null) {
			insertStreetData(addressStreetStat, initId, name, Junidecode.unidecode(name), 
					location.getLatitude(), location.getLongitude(), city.getId());
			if (loadInMemory) {
				addBatch(addressStreetStat);
				addressStreetLocalMap.put(name + "_" + city.getId(), initId); //$NON-NLS-1$
			} else {
				addressStreetStat.execute();
				// commit immediately to search after
				mapConnection.commit();
			}
			foundId = initId;
		}
		return foundId;
	}

	public City getClosestCity(LatLon point) {
		if (point == null) {
			return null;
		}
		
		for (City c : cityManager.getClosestObjects(point.getLatitude(), point.getLongitude(), 3)) {
			Boundary boundary = citiBoundaries.get(c);
			if(boundary != null){
				if(boundary.containsPoint(point)){
					return c;
				}
			}
		}
		for (City c : cityVillageManager.getClosestObjects(point.getLatitude(), point.getLongitude(), 3)) {
			Boundary boundary = citiBoundaries.get(c);
			if(boundary != null){
				if(boundary.containsPoint(point)){
					return c;
				}
			}
		}
		City closest = null;
		double relDist = Double.POSITIVE_INFINITY;
		for (City c : cityManager.getClosestObjects(point.getLatitude(), point.getLongitude(), 3)) {
			double rel = MapUtils.getDistance(c.getLocation(), point) / c.getType().getRadius();
			if (rel < relDist) {
				closest = c;
				relDist = rel;
				if (relDist < 0.2d) {
					break;
				}
			}
		}
		if (relDist < 0.2d) {
			return closest;
		}
		for (City c : cityVillageManager.getClosestObjects(point.getLatitude(), point.getLongitude(), 3)) {
			double rel = MapUtils.getDistance(c.getLocation(), point) / c.getType().getRadius();
			if (rel < relDist) {
				closest = c;
				relDist = rel;
				if (relDist < 0.2d) {
					break;
				}
			}
		}
		return closest;
	}
	
	
	public void iterateMainEntity(Entity e, OsmDbAccessorContext ctx) throws SQLException {
		// index not only buildings but also nodes that belongs to addr:interpolation ways
		if (e.getTag(OSMTagKey.ADDR_HOUSE_NUMBER) != null && e.getTag(OSMTagKey.ADDR_STREET) != null) {
			// TODO e.getTag(OSMTagKey.ADDR_CITY) could be used to find city however many cities could have same name!
			// check that building is not registered already
			boolean exist = false;
			if (loadInMemory) {
				exist = addressBuildingLocalSet.contains(e.getId());
			} else {
				addressSearchBuildingStat.setLong(1, e.getId());
				ResultSet rs = addressSearchBuildingStat.executeQuery();
				exist = rs.next();
				rs.close();

			}
			if (!exist) {
				ctx.loadEntityData(e, false);
				LatLon l = e.getLatLon();
				City city = getClosestCity(l);
				Long idStreet = getStreetInCity(city, e.getTag(OSMTagKey.ADDR_STREET), l, (e.getId() << 2));
				if (idStreet != null) {
					Building building = new Building(e);
					building.setName(e.getTag(OSMTagKey.ADDR_HOUSE_NUMBER));
					writeBuilding(idStreet, building);
				}
			}
		} else if (e instanceof Way /* && OSMSettings.wayForCar(e.getTag(OSMTagKey.HIGHWAY)) */
				&& e.getTag(OSMTagKey.HIGHWAY) != null && e.getTag(OSMTagKey.NAME) != null) {
			// suppose that streets with names are ways for car
			// Ignore all ways that have house numbers and highway type
			boolean exist = false;

			// if we saved address ways we could checked that we registered before
			if (saveAddressWays) {
				if (loadInMemory) {
					exist = addressStreetNodeLocalSet.contains(e.getId());
				} else {
					addressSearchStreetNodeStat.setLong(1, e.getId());
					ResultSet rs = addressSearchStreetNodeStat.executeQuery();
					exist = rs.next();
					rs.close();
				}
			}

			// check that street way is not registered already
			if (!exist) {
				ctx.loadEntityData(e, false);
				LatLon l = e.getLatLon();
				City city = getClosestCity(l);
				Long idStreet = getStreetInCity(city, e.getTag(OSMTagKey.NAME), l, (e.getId() << 2) | 1);
				if (idStreet != null && saveAddressWays) {
					writeStreetWayNodes(idStreet, (Way) e);
				}
			}
		}
		if (e instanceof Relation) {
			if (e.getTag(OSMTagKey.POSTAL_CODE) != null) {
				ctx.loadEntityData(e, false);
				postalCodeRelations.add((Relation) e);
			}
		}
	}
	
	
	private void writeStreetWayNodes(Long streetId, Way way)
			throws SQLException {
		for (Node n : way.getNodes()) {
			if (n == null) {
				continue;
			}
			addressStreetNodeStat.setLong(1, n.getId());
			addressStreetNodeStat.setDouble(2, n.getLatitude());
			addressStreetNodeStat.setDouble(3, n.getLongitude());
			addressStreetNodeStat.setLong(5, way.getId());
			addressStreetNodeStat.setLong(4, streetId);
			addBatch(addressStreetNodeStat);
		}
	}
	

	private void writeBuilding(Long streetId, Building building) throws SQLException {
		addressBuildingStat.setLong(1, building.getId());
		addressBuildingStat.setDouble(2, building.getLocation().getLatitude());
		addressBuildingStat.setDouble(3, building.getLocation().getLongitude());
		addressBuildingStat.setString(4, building.getName());
		addressBuildingStat.setString(5, building.getEnName());
		addressBuildingStat.setLong(6, streetId);
		addressBuildingStat.setString(7, building.getPostcode() == null ? null : building.getPostcode().toUpperCase());
		addBatch(addressBuildingStat);
	}
	
	
	private void writeCity(City city) throws SQLException {
		addressCityStat.setLong(1, city.getId());
		addressCityStat.setDouble(2, city.getLocation().getLatitude());
		addressCityStat.setDouble(3, city.getLocation().getLongitude());
		addressCityStat.setString(4, city.getName());
		addressCityStat.setString(5, city.getEnName());
		addressCityStat.setString(6, CityType.valueToString(city.getType()));
		addBatch(addressCityStat);
	}
	
	
	private void insertStreetData(PreparedStatement addressStreetStat, long id, String name, String nameEn, double latitude,
			double longitude, Long cityId) throws SQLException {
		addressStreetStat.setLong(1, id);
		addressStreetStat.setString(4, name);
		addressStreetStat.setString(5, nameEn);
		addressStreetStat.setDouble(2, latitude);
		addressStreetStat.setDouble(3, longitude);
		addressStreetStat.setLong(6, cityId);
	}
	

	public void writeCitiesIntoDb() throws SQLException {
		for (City c : cities.values()) {
			writeCity(c);
		}
		// commit to put all cities
		if (pStatements.get(addressCityStat) > 0) {
			addressCityStat.executeBatch();
			pStatements.put(addressCityStat, 0);
			mapConnection.commit();
		}
	}
	
	public void processingPostcodes() throws SQLException {
		if (pStatements.get(addressBuildingStat) > 0) {
			addressBuildingStat.executeBatch();
			pStatements.put(addressBuildingStat, 0);
			mapConnection.commit();
		}
		PreparedStatement pstat = mapConnection.prepareStatement("UPDATE building SET postcode = ? WHERE id = ?");
		pStatements.put(pstat, 0);
		for (Relation r : postalCodeRelations) {
			String tag = r.getTag(OSMTagKey.POSTAL_CODE);
			for (EntityId l : r.getMemberIds()) {
				pstat.setString(1, tag);
				pstat.setLong(2, l.getId());
				addBatch(pstat);
			}
		}
		if (pStatements.get(pstat) > 0) {
			pstat.executeBatch();
		}
		pStatements.remove(pstat);
	}


	public void writeBinaryAddressIndex(BinaryMapIndexWriter writer, String regionName, IProgress progress) throws IOException, SQLException {
		closePreparedStatements(addressCityStat, addressStreetStat, addressStreetNodeStat, addressBuildingStat);
		mapConnection.commit();
		boolean readWayNodes = saveAddressWays;

		writer.startWriteAddressIndex(regionName);
		List<City> cities = readCities(mapConnection);
		List<Street> streets = new ArrayList<Street>();
		Collections.sort(cities, new Comparator<City>() {

			@Override
			public int compare(City o1, City o2) {
				if (o1.getType() != o2.getType()) {
					return (o1.getType().ordinal() - o2.getType().ordinal());
				}
				return Collator.getInstance().compare(o1.getName(), o2.getName());
			}
		});
		PreparedStatement streetstat = mapConnection.prepareStatement("SELECT A.id, A.name, A.name_en, A.latitude, A.longitude, "+ //$NON-NLS-1$
				"B.id, B.name, B.name_en, B.latitude, B.longitude, B.postcode "+ //$NON-NLS-1$
				"FROM street A left JOIN building B ON B.street = A.id WHERE A.city = ?"); //$NON-NLS-1$
		PreparedStatement waynodesStat = null;
		if (readWayNodes) {
			waynodesStat = mapConnection.prepareStatement("SELECT A.id, A.latitude, A.longitude FROM street_node A WHERE A.street = ? "); //$NON-NLS-1$
		}

		int j = 0;
		for (; j < cities.size(); j++) {
			City c = cities.get(j);
			if (c.getType() != CityType.CITY && c.getType() != CityType.TOWN) {
				break;
			}
		}
		progress.startTask(Messages.getString("IndexCreator.SERIALIZING_ADRESS"), j + ((cities.size() - j) / 100 + 1)); //$NON-NLS-1$

		Map<String, Set<Street>> postcodes = new TreeMap<String, Set<Street>>();
		boolean writeCities = true;
		
		// collect suburbs with is in value
		List<City> suburbs = new ArrayList<City>();
		for(City s : cities){
			if(s.getType() == CityType.SUBURB && s.getIsInValue() != null){
				suburbs.add(s);
			}
		}

		// write cities and after villages
		writer.startCityIndexes(false);
		for (int i = 0; i < cities.size(); i++) {
			City c = cities.get(i);
			List<City> listSuburbs = null;
			for (City suburb : suburbs) {
				if (suburb.getIsInValue().contains(c.getName().toLowerCase())) {
					if(listSuburbs == null){
						listSuburbs = new ArrayList<City>();
					}
					listSuburbs.add(suburb);
				}
			}
			if (writeCities) {
				progress.progress(1);
			} else if ((cities.size() - i) % 100 == 0) {
				progress.progress(1);
			}
			if (writeCities && c.getType() != CityType.CITY && c.getType() != CityType.TOWN) {
				writer.endCityIndexes(false);
				writer.startCityIndexes(true);
				writeCities = false;
			}

			streets.clear();
			Map<Street, List<Node>> streetNodes = null;
			if (readWayNodes) {
				streetNodes = new LinkedHashMap<Street, List<Node>>();
			}
			long time = System.currentTimeMillis();
			readStreetsBuildings(streetstat, c, streets, waynodesStat, streetNodes, listSuburbs);
			long f = System.currentTimeMillis() - time;
			writer.writeCityIndex(c, streets, streetNodes);
			int bCount = 0;
			for (Street s : streets) {
				bCount++;
				for (Building b : s.getBuildings()) {
					bCount++;
					if (b.getPostcode() != null) {
						if (!postcodes.containsKey(b.getPostcode())) {
							postcodes.put(b.getPostcode(), new LinkedHashSet<Street>(3));
						}
						postcodes.get(b.getPostcode()).add(s);
					}
				}
			}
			if (f > 500) {
				System.out.println("! " + c.getName() + " ! " + f + " " + bCount + " streets " + streets.size()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			}
		}
		writer.endCityIndexes(!writeCities);

		// write postcodes
		writer.startPostcodes();
		for (String s : postcodes.keySet()) {
			writer.writePostcode(s, postcodes.get(s));
		}
		writer.endPostcodes();

		progress.finishTask();

		writer.endWriteAddressIndex();
		writer.flush();
		streetstat.close();
		if (readWayNodes) {
			waynodesStat.close();
		}

	}

	public void commitToPutAllCities() throws SQLException {
		// commit to put all cities
		if (pStatements.get(addressBuildingStat) > 0) {
			addressBuildingStat.executeBatch();
			pStatements.put(addressBuildingStat, 0);
		}
		if (pStatements.get(addressStreetNodeStat) > 0) {
			addressStreetNodeStat.executeBatch();
			pStatements.put(addressStreetNodeStat, 0);
		}
		mapConnection.commit();
		
	}

	public void createDatabaseStructure(Connection mapConnection, DBDialect dialect) throws SQLException {
		this.mapConnection = mapConnection;
		createAddressIndexStructure(mapConnection, dialect);
		addressCityStat = mapConnection.prepareStatement("insert into city (id, latitude, longitude, name, name_en, city_type) values (?, ?, ?, ?, ?, ?)");
		addressStreetStat = mapConnection.prepareStatement("insert into street (id, latitude, longitude, name, name_en, city) values (?, ?, ?, ?, ?, ?)");
		addressBuildingStat = mapConnection.prepareStatement("insert into building (id, latitude, longitude, name, name_en, street, postcode) values (?, ?, ?, ?, ?, ?, ?)");
		addressStreetNodeStat = mapConnection.prepareStatement("insert into street_node (id, latitude, longitude, street, way) values (?, ?, ?, ?, ?)");
		
		addressSearchStreetStat = mapConnection.prepareStatement("SELECT ID FROM street WHERE ? = city AND ? = name");
		addressSearchBuildingStat = mapConnection.prepareStatement("SELECT id FROM building where ? = id");
		addressSearchStreetNodeStat = mapConnection.prepareStatement("SELECT way FROM street_node WHERE ? = way");

		pStatements.put(addressCityStat, 0);
		pStatements.put(addressStreetStat, 0);
		pStatements.put(addressStreetNodeStat, 0);
		pStatements.put(addressBuildingStat, 0);
		// put search statements to close them after all
		pStatements.put(addressSearchBuildingStat, 0);
		pStatements.put(addressSearchStreetNodeStat, 0);
		pStatements.put(addressSearchStreetStat, 0);
		
	}
	
	private void createAddressIndexStructure(Connection conn, DBDialect dialect) throws SQLException{
		Statement stat = conn.createStatement();
		
        stat.executeUpdate("create table city (id bigint primary key, latitude double, longitude double, " +
        			"name varchar(255), name_en varchar(255), city_type varchar(32))");
        stat.executeUpdate("create index city_ind on city (id, city_type)");
        
        stat.executeUpdate("create table street (id bigint primary key, latitude double, longitude double, " +
					"name varchar(255), name_en varchar(255), city bigint)");
        stat.executeUpdate("create index street_city on street (city)");
        stat.executeUpdate("create index street_id on street (id)");
        // create index on name ?

        stat.executeUpdate("create table building (id bigint, latitude double, longitude double, " +
						"name varchar(255), name_en varchar(255), street bigint, postcode varchar(255), primary key(street, id))");
        stat.executeUpdate("create index building_postcode on building (postcode)");
        stat.executeUpdate("create index building_street on building (street)");
        stat.executeUpdate("create index building_id on building (id)");
        
        
        stat.executeUpdate("create table street_node (id bigint, latitude double, longitude double, " +
						"street bigint, way bigint)");
        stat.executeUpdate("create index street_node_street on street_node (street)");
        stat.executeUpdate("create index street_node_way on street_node (way)");
        
//        if(dialect == DBDialect.SQLITE){
//        	stat.execute("PRAGMA user_version = " + IndexConstants.ADDRESS_TABLE_VERSION); //$NON-NLS-1$
//        }
        stat.close();
	}
	
	private List<Street> readStreetsBuildings(PreparedStatement streetBuildingsStat, City city, List<Street> streets,
			PreparedStatement waynodesStat, Map<Street, List<Node>> streetNodes, List<City> citySuburbs) throws SQLException {
		Map<Long, Street> visitedStreets = new LinkedHashMap<Long, Street>();
		//read streets for city
		readStreatsByBuildingsForCity(streetBuildingsStat, city, streets,
				waynodesStat, streetNodes, visitedStreets);
		//read streets for suburbs of the city
		if (citySuburbs != null) {
			for (City suburb : citySuburbs) {
				readStreatsByBuildingsForCity(streetBuildingsStat, suburb, streets, waynodesStat, streetNodes, visitedStreets);
			}
		}
		return streets;
	}


	private void readStreatsByBuildingsForCity(
			PreparedStatement streetBuildingsStat, City city,
			List<Street> streets, PreparedStatement waynodesStat,
			Map<Street, List<Node>> streetNodes,
			Map<Long, Street> visitedStreets) throws SQLException {
		streetBuildingsStat.setLong(1, city.getId());
		ResultSet set = streetBuildingsStat.executeQuery();
		while (set.next()) {
			long streetId = set.getLong(1);
			if (!visitedStreets.containsKey(streetId)) {
				Street street = new Street(null);
				street.setName(set.getString(2));
				street.setEnName(set.getString(3));
				street.setLocation(set.getDouble(4), set.getDouble(5));
				street.setId(streetId);
				streets.add(street);
				visitedStreets.put(streetId, street);
				if (waynodesStat != null && streetNodes != null) {
					ArrayList<Node> list = new ArrayList<Node>();
					streetNodes.put(street, list);
					waynodesStat.setLong(1, street.getId());
					ResultSet rs = waynodesStat.executeQuery();
					while (rs.next()) {
						list.add(new Node(rs.getDouble(2), rs.getDouble(3), rs.getLong(1)));
					}
					rs.close();
				}
			}
			if (set.getObject(6) != null) {
				Street s = visitedStreets.get(streetId);
				Building b = new Building();
				b.setId(set.getLong(6));
				b.setName(set.getString(7));
				b.setEnName(set.getString(8));
				b.setLocation(set.getDouble(9), set.getDouble(10));
				b.setPostcode(set.getString(11));
				s.registerBuilding(b);
			}
		}

		set.close();
	}
	

	public List<City> readCities(Connection c) throws SQLException{
		List<City> cities = new ArrayList<City>();
		Statement stat = c.createStatement();
		ResultSet set = stat.executeQuery("select id, latitude, longitude , name , name_en , city_type from city"); //$NON-NLS-1$
		while(set.next()){
			City city = new City(CityType.valueFromString(set.getString(6)));
			city.setName(set.getString(4));
			city.setEnName(set.getString(5));
			city.setLocation(set.getDouble(2), 
					set.getDouble(3));
			city.setId(set.getLong(1));
			cities.add(city);
			
		}
		set.close();
		stat.close();
		return cities;
	}	


}
