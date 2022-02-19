package net.osmand.binary;

import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import net.osmand.binary.BinaryMapIndexReader.MapIndex;
import net.osmand.binary.BinaryMapIndexReader.TagValuePair;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.util.Algorithms;

public class BinaryMapDataObject {
	protected int[] coordinates = null;
	protected int[][] polygonInnerCoordinates = null;
	protected boolean area = false;
	protected int[] types = null;
	protected int[] additionalTypes = null;
	protected int objectType = RenderingRulesStorage.POINT_RULES;
	protected int labelX;
	protected int labelY;
	private static final int SHIFT_ID = 7;
	
	protected TIntObjectHashMap<String> objectNames = null;
	protected TIntArrayList namesOrder = null;
	protected long id = 0;
	
	protected MapIndex mapIndex = null;

	public BinaryMapDataObject() {
	}

	public BinaryMapDataObject(long id, int[] coordinates, int[][] polygonInnerCoordinates, int objectType, boolean area, 
			int[] types, int[] additionalTypes, int labelX, int labelY) {
		this.polygonInnerCoordinates = polygonInnerCoordinates;
		this.coordinates = coordinates;
		this.additionalTypes = additionalTypes;
		this.types = types;
		this.id = id;
		this.objectType = objectType;
		this.area = area;
		this.labelX = labelX;
		this.labelY = labelY;
	}
	
	protected void setCoordinates(int[] coordinates) {
		this.coordinates = coordinates;
	}
	
	
	public String getName() {
		if(objectNames == null){
			return "";
		}
		String name = objectNames.get(mapIndex.nameEncodingType);
		if(name == null){
			return "";
		}
		return name;
	}

	public TIntObjectHashMap<String> getObjectNames() {
		return objectNames;
	}
	
	public Map<Integer, String> getOrderedObjectNames() {
		if (namesOrder == null) {
			return null;
		}
		LinkedHashMap<Integer, String> lm = new LinkedHashMap<Integer, String> ();
		for (int i = 0; i < namesOrder.size(); i++) {
			int nm = namesOrder.get(i);
			lm.put(nm, objectNames.get(nm));
		}
		return lm;
	}
	
	public void putObjectName(int type, String name){
		if(objectNames == null){
			objectNames = new TIntObjectHashMap<String>();
			namesOrder = new TIntArrayList();
		}
		objectNames.put(type, name);
		namesOrder.add(type);
	}
	
	public int[][] getPolygonInnerCoordinates() {
		return polygonInnerCoordinates;
	}
	
	public int[] getTypes(){
		return types;
	}
	
	public boolean containsType(int cachedType) {
		if(cachedType != -1) {
			for(int i=0; i<types.length; i++){
				if(types[i] == cachedType) {
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean containsAdditionalType(int cachedType) {
		if (cachedType != -1 && additionalTypes != null) {
			for (int i = 0; i < additionalTypes.length; i++) {
				if (additionalTypes[i] == cachedType) {
					return true;
				}
			}
		}
		return false;
	}
	
	public String getNameByType(int type) {
		if(type != -1 && objectNames != null) {
			return objectNames.get(type);
		}
		return null;
	}
	
	public int[] getAdditionalTypes() {
		return additionalTypes;
	}
	
	public boolean isArea() {
		return area;
	}
	
	public boolean isCycle() {
		if(coordinates == null || coordinates.length < 2) {
			return false;
		}
		return coordinates[0] == coordinates[coordinates.length - 2] && 
				coordinates[1] == coordinates[coordinates.length - 1];
	}
	
	public void setArea(boolean area) {
		this.area = area;
	}
	
	public long getId() {
		return id;
	}
	
	protected void setId(long id) {
		this.id = id;
	}
	
	protected void setTypes(int[] types) {
		this.types = types;
	}
	
	
	public int getSimpleLayer() {
		if (mapIndex != null) {
			if (additionalTypes != null) {
				for (int i = 0; i < additionalTypes.length; i++) {
					if (mapIndex.positiveLayers.contains(additionalTypes[i])) {
						return 1;
					} else if (mapIndex.negativeLayers.contains(additionalTypes[i])) {
						return -1;
					}
				}
			}
		}
		return 0;
	}
	
	public TIntArrayList getNamesOrder() {
		return namesOrder;
	}
	
	public MapIndex getMapIndex() {
		return mapIndex;
	}
	
	public void setMapIndex(MapIndex mapIndex) {
		this.mapIndex = mapIndex;
	}
	
	public int getPointsLength() {
		if(coordinates == null){
			return 0;
		}
		return coordinates.length / 2;
	}

	public int getPoint31YTile(int ind) {
		return coordinates[2 * ind + 1];
	}

	public int getPoint31XTile(int ind) {
		return coordinates[2 * ind];
	}
	
	public boolean compareBinary(BinaryMapDataObject thatObj, int coordinatesPrecision) {
		if(this.objectType == thatObj.objectType
				&& this.id == thatObj.id
				&& this.area == thatObj.area 
				&& compareCoordinates(this.coordinates, thatObj.coordinates, coordinatesPrecision) ) {
			if (mapIndex == null) {
				throw new IllegalStateException("Illegal binary object: " + id);
			}
			if (thatObj.mapIndex == null) {
				throw new IllegalStateException("Illegal binary object: " + thatObj.id);
			}
			
			boolean equals = true;
			if (equals) {
				if (polygonInnerCoordinates == null || thatObj.polygonInnerCoordinates == null) {
					equals = polygonInnerCoordinates == thatObj.polygonInnerCoordinates;
				} else if (polygonInnerCoordinates.length != thatObj.polygonInnerCoordinates.length) {
					equals = false;
				} else {
					for (int i = 0; i < polygonInnerCoordinates.length && equals; i++) {
						if (polygonInnerCoordinates[i] == null || thatObj.polygonInnerCoordinates[i] == null) {
							equals = polygonInnerCoordinates[i] == thatObj.polygonInnerCoordinates[i];
						} else if (polygonInnerCoordinates[i].length != thatObj.polygonInnerCoordinates[i].length) {
							equals = false;
						} else {
							equals = compareCoordinates(polygonInnerCoordinates[i], thatObj.polygonInnerCoordinates[i],
									coordinatesPrecision);
						}
					}
				}
			}
			
			if (equals) {
				if (types == null || thatObj.types == null) {
					equals = types == thatObj.types;
				} else if (types.length != thatObj.types.length) {
					equals = false;
				} else {
					for (int i = 0; i < types.length && equals; i++) {
						TagValuePair o = mapIndex.decodeType(types[i]);
						TagValuePair s = thatObj.mapIndex.decodeType(thatObj.types[i]);
						equals = o.equals(s) && equals;
					}
				}
			}
			if (equals) {
				if (additionalTypes == null || thatObj.additionalTypes == null) {
					equals = additionalTypes == thatObj.additionalTypes;
				} else if (additionalTypes.length != thatObj.additionalTypes.length) {
					equals = false;
				} else {
					for (int i = 0; i < additionalTypes.length && equals; i++) {
						TagValuePair o = mapIndex.decodeType(additionalTypes[i]);
						TagValuePair s = thatObj.mapIndex.decodeType(thatObj.additionalTypes[i]);
						equals = o.equals(s);
					}
				}
			}
			if (equals) {
				if (namesOrder == null || thatObj.namesOrder == null) {
					equals = namesOrder == thatObj.namesOrder;
				} else if (namesOrder.size() != thatObj.namesOrder.size()) {
					equals = false;
				} else {
					for (int i = 0; i < namesOrder.size() && equals; i++) {
						TagValuePair o = mapIndex.decodeType(namesOrder.get(i));
						TagValuePair s = thatObj.mapIndex.decodeType(thatObj.namesOrder.get(i));
						equals = o.equals(s);
					}
				}
			}
			if (equals) {
				// here we know that name indexes are equal & it is enough to check the value sets
				if (objectNames == null || thatObj.objectNames == null) {
					equals = objectNames == thatObj.objectNames;
				} else if (objectNames.size() != thatObj.objectNames.size()) {
					equals = false;
				} else {
					for (int i = 0; i < namesOrder.size() && equals; i++) {
						String o = objectNames.get(namesOrder.get(i));
						String s = thatObj.objectNames.get(thatObj.namesOrder.get(i));
						equals = Algorithms.objectEquals(o, s);
					}
				}
			}
			
			return equals;
		}
//		thatObj.mapIndex.decodeType(thatObj.types[0])
//		mapIndex.decodeType(types[0]) id >>7
		return false;
	}


	private static boolean compareCoordinates(int[] coordinates, int[] coordinates2, int precision) {
		if(precision == 0) {
			return Arrays.equals(coordinates, coordinates2);
		}
		TIntArrayList cd = simplify(coordinates, precision);
		TIntArrayList cd2 = simplify(coordinates2, precision);
		return cd.equals(cd2);
	}



	private static TIntArrayList simplify(int[] c, int precision) {
		int len = c.length / 2;
		TIntArrayList lt = new TIntArrayList(len * 3);
		for (int i = 0; i < len; i++) {
			lt.add(0);
			lt.add(c[i * 2]);
			lt.add(c[i * 2 + 1]);
		}
		lt.set(0, 1);
		lt.set((len - 1) * 3, 1);
		simplifyLine(lt, precision, 0, len - 1);

		TIntArrayList res = new TIntArrayList(len * 2);
		for (int i = 0; i < len; i++) {
			if (lt.get(i * 3) == 1) {
				res.add(lt.get(i * 3 + 1));
				res.add(lt.get(i * 3 + 2));
			}
		}
		return res;
	}

	private static double orthogonalDistance(int x, int y, int x1, int y1, int x2, int y2) {
		long A = (x - x1);
		long B = (y - y1);
		long C = (x2 - x1);
		long D = (y2 - y1);
		return Math.abs(A * D - C * B) / Math.sqrt(C * C + D * D);
	}

	private static void simplifyLine(TIntArrayList lt, int precision, int start, int end) {
		if(start == end - 1) {
			return;
		}
		int x = lt.get(start*3 + 1);
		int y = lt.get(start*3 + 2);
		int ex = lt.get(end*3 + 1);
		int ey = lt.get(end*3 + 2);
		double max = 0;
		int maxK = -1;
		for(int k = start + 1; k < end ; k++) {
			double ld = orthogonalDistance(lt.get(k*3 + 1), lt.get(k*3 + 2), x, y, ex, ey);
			if(maxK == -1 || max < ld) {
				maxK = k;
				max = ld;
			}
		}
		if(max < precision) {
			return;
		}
		lt.set(maxK*3, 1); // keep point
		simplifyLine(lt, precision, start, maxK);
		simplifyLine(lt, precision, maxK, end);
		
	}

	public boolean isLabelSpecified() {
		return (labelX != 0 || labelY != 0) && coordinates.length > 0;
	}

	public int getLabelX() {
		long sum = 0;
		int LABEL_SHIFT = 31 - BinaryMapIndexReader.LABEL_ZOOM_ENCODE;
		int len = coordinates.length / 2;
		for(int i = 0; i < len; i++) {
			sum += coordinates[2 * i];
		}
		int average = ((int) (sum >> BinaryMapIndexReader.SHIFT_COORDINATES)/ len) 
				<< (BinaryMapIndexReader.SHIFT_COORDINATES - LABEL_SHIFT);
		int label31X = (average + this.labelX) << LABEL_SHIFT;
		return label31X;
	}
	
	public int getLabelY() {
		long sum = 0;
		int LABEL_SHIFT = 31 - BinaryMapIndexReader.LABEL_ZOOM_ENCODE;
		int len = coordinates.length / 2;
		for(int i = 0; i < len; i++) {
			sum += coordinates[2 * i + 1];
		}
		int average = ((int) (sum >> BinaryMapIndexReader.SHIFT_COORDINATES)/ len) 
				<< (BinaryMapIndexReader.SHIFT_COORDINATES - LABEL_SHIFT);
		int label31Y = (average + this.labelY) << LABEL_SHIFT;
		return label31Y;
	}

	public int[] getCoordinates() {
		return coordinates;
	}
	
	public int getObjectType() {
		return objectType;
	}

	public String getTagValue(String tag) {
		if (mapIndex == null) {
			return "";
		}
		TIntObjectIterator<String> it = objectNames.iterator();
		while (it.hasNext()) {
			it.advance();
			BinaryMapIndexReader.TagValuePair tp = mapIndex.decodeType(it.key());
			if (tp.tag.equals(tag)) {
				return it.value();
			}
		}
		return "";
	}

	public String getAdditionalTagValue(String tag) {
		if (mapIndex == null) {
			return "";
		}
		for (int type : additionalTypes) {
			TagValuePair tp = mapIndex.decodeType(type);
			if (tag.equals(tp.tag)) {
				return tp.value;
			}
		}
		return "";
	}
	
	@Override
	public String toString() {
		String obj = "Point";
		if(objectType == RenderingRulesStorage.LINE_RULES) {
			obj = "Line";
		} else if(objectType == RenderingRulesStorage.POLYGON_RULES) {
			obj = "Polygon";
		}
		return obj + " " + (getId() >> SHIFT_ID);
	}
}