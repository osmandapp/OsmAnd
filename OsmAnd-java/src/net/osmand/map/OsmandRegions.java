package net.osmand.map;


import gnu.trove.list.array.TIntArrayList;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.util.MapAlgorithms;
import net.osmand.util.MapUtils;

import java.io.*;
import java.util.*;

public class OsmandRegions {

	Map<String, LinkedList<BinaryMapDataObject>> countries = new HashMap<String, LinkedList<BinaryMapDataObject>>();
	QuadTree<String> quadTree = new QuadTree<String>(new QuadRect(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE),
			8, 0.55f);
	Integer downloadNameType = null;
	Integer prefixType = null;
	Integer suffixType = null;

	public OsmandRegions() {
	}


	Integer getDownloadNameType(){
		return downloadNameType;
	}

	Integer getPrefixType() {
		return prefixType;
	}

	Integer getSuffixType() {
		return suffixType;
	}



	public boolean contain(BinaryMapDataObject bo, int tx, int ty) {
		int t = 0;
		for (int i = 1; i < bo.getPointsLength(); i++) {
			int fx = MapAlgorithms.ray_intersect_x(bo.getPoint31XTile(i - 1),
					bo.getPoint31YTile(i - 1),
					bo.getPoint31XTile(i),
					bo.getPoint31YTile(i), ty);
			if (Integer.MIN_VALUE != fx && tx >= fx) {
				t++;
			}
		}
		return t % 2 == 1;
	}

	public List<BinaryMapDataObject> getCountries(int tile31x, int tile31y) {
		HashSet<String> set = new HashSet<String>(quadTree.queryInBox(new QuadRect(tile31x, tile31y, tile31x, tile31y),
				new ArrayList<String>()));
		List<BinaryMapDataObject> result = new ArrayList<BinaryMapDataObject>();
		Iterator<String> it = set.iterator();

		while (it.hasNext()) {
			String cname = it.next();
			BinaryMapDataObject container = null;
			int count = 0;
			for (BinaryMapDataObject bo : countries.get(cname)) {
				if (contain(bo, tile31x, tile31y)) {
					count++;
					container = bo;
					break;
				}
			}
			if (count % 2 == 1) {
				result.add(container);
			}
		}
		return result;
	}


	private void init(String fileName) throws IOException {

		BinaryMapIndexReader reader = new BinaryMapIndexReader(new RandomAccessFile(fileName, "r"));
		BinaryMapIndexReader.SearchRequest<BinaryMapDataObject> sr = BinaryMapIndexReader.buildSearchRequest(0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE,
				5, new BinaryMapIndexReader.SearchFilter() {
					@Override
					public boolean accept(TIntArrayList types, BinaryMapIndexReader.MapIndex index) {
						return true;
					}
				}, new ResultMatcher<BinaryMapDataObject>() {


					@Override
					public boolean publish(BinaryMapDataObject object) {
						if (object.getPointsLength() < 1) {
							return false;
						}
						if (downloadNameType == null) {
							downloadNameType = object.getMapIndex().getRule("download_name", null);
							prefixType = object.getMapIndex().getRule("region_prefix", null);
							suffixType = object.getMapIndex().getRule("region_suffix", null);
							if (downloadNameType == null) {
								throw new IllegalStateException();
							}
						}
						String nm = object.getNameByType(downloadNameType);
						if (!countries.containsKey(nm)) {
							LinkedList<BinaryMapDataObject> ls = new LinkedList<BinaryMapDataObject>();
							countries.put(nm, ls);
							ls.add(object);
						} else {
							countries.get(nm).add(object);
						}

						int maxx = object.getPoint31XTile(0);
						int maxy = object.getPoint31YTile(0);
						int minx = maxx;
						int miny = maxy;
						for (int i = 1; i < object.getPointsLength(); i++) {
							int x = object.getPoint31XTile(i);
							int y = object.getPoint31YTile(i);
							if (y < miny) {
								miny = y;
							} else if (y > maxy) {
								maxy = y;
							}
							if (x < minx) {
								minx = x;
							} else if (x > maxx) {
								maxx = x;
							}
						}
						quadTree.insert(nm, new QuadRect(minx, miny, maxx, maxy));
						return false;
					}

					@Override
					public boolean isCancelled() {
						return false;
					}
				}
		);
		reader.searchMapIndex(sr);
	}


	private static void testCountry(OsmandRegions or, double lat, double lon, String test) {
		List<BinaryMapDataObject> cs = or.getCountries(MapUtils.get31TileNumberX(15.8), MapUtils.get31TileNumberY(23.09));
		if(cs.size() != 1) {
			StringBuilder found = new StringBuilder();
			for(BinaryMapDataObject b : cs) {
				found.append(b.getName()).append(' ');
			}
			throw new IllegalStateException(" Expected " + test + " - Lat " + lat + " lon " + lon + ", but found : " + found);
		}
		String nm = cs.get(0).getName();
		if(!test.equals(nm)) {
			throw new IllegalStateException(" Expected " + test + " but was " + nm);
		}
	}


	public static void main(String[] args) throws IOException {
		OsmandRegions or = new OsmandRegions();
		or.init("/home/victor/projects/osmand/osm-gen/Osmand_regions.obf");

		testCountry(or, 15.8, 23.09, "chad");

	}
}
