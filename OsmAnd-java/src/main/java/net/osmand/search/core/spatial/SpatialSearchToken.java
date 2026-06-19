package net.osmand.search.core.spatial;

import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.ByteString;

import gnu.trove.map.hash.TLongObjectHashMap;
import net.osmand.CollatorStringMatcher;
import net.osmand.CollatorStringMatcher.StringMatcherMode;
import net.osmand.binary.BinaryMapAddressReaderAdapter.CityBlocks;
import net.osmand.binary.ObfConstants;
import net.osmand.binary.OsmandOdb.AddressNameIndexDataAtom;
import net.osmand.binary.OsmandOdb.OsmAndPoiNameIndexDataAtom;
import net.osmand.data.MapObject;
import net.osmand.data.Street;
import net.osmand.search.core.HashQuadTree;
import net.osmand.util.MapUtils;
import net.osmand.util.SearchAlgorithms;

public class SpatialSearchToken {
	public static final int POI_TYPE = -1;
	public static final int STREET_TYPE = CityBlocks.STREET_TYPE.index;
	public static final String DOT_INCOMPLETE_STRING = ".";

	int originalOrder = 0;
	int sortedOrder = 0;
	
	String originalWord;
	String word;
	List<NameIndexAtom> atoms = new ArrayList<>();
	TLongObjectHashMap<NameIndexAtom> index = new TLongObjectHashMap<>();
	TLongObjectHashMap<NameIndexAtom> indexByOsmIds = new TLongObjectHashMap<>();
	HashQuadTree<NameIndexAtom> quadTree = new HashQuadTree<>(16);
	CollatorStringMatcher collator;
	

	public SpatialSearchToken(String w, String original, int order) {
		originalWord = original;
		word = w;
		this.originalOrder = order;
		// alreadyn in collator w.endsWith(DOT_INCOMPLETE_STRING)
		collator = new CollatorStringMatcher(w, StringMatcherMode.CHECK_EQUALS_FROM_SPACE);
	}

	@Override
	public String toString() {
		return String.format("%d. %s - %d atoms", sortedOrder, 
				originalWord, atoms.size());
	}

	void addAtom(NameIndexAtom atom) {
		if (atom.object != null && !(atom.object instanceof Street) && atom.object.getId() > 0) {
			long osmId = ObfConstants.getOsmIdFromMapObjectId(atom.object.getId());
			NameIndexAtom ex = indexByOsmIds.get(osmId);
			if (ex != null) {
				return;
			}
			indexByOsmIds.put(osmId, atom);
		}

		NameIndexAtom aa = index.get(atom.id);
		if (aa != null) {
			if (aa != atom) {
				// ignore duplicates object per token
//				System.out.println(aa.name + " != " + atom.name  + " " + aa + " " + aa.object.getLocation());
			}
			return;
		}
		index.put(atom.id, atom);
		atoms.add(atom);
		quadTree.put(atom.coords.bboxTileZoom, atom.coords.bboxTileId, atom);
	}

	boolean acceptName(String name) {
		return collator.matches(name);
	}

	public static class NameIndexAtomXY {
		int[] bbox31; // if exists [xleft, yleft, xright, yright]
		long bboxTileId; // encodes zoom, tileX, tileY
		int bboxTileZoom;
		int x16, y16;

		public NameIndexAtomXY(AddressNameIndexDataAtom a, OsmAndPoiNameIndexDataAtom b) {
			if (a != null) {
				init(a);
			} else {
				init(b);
			}
		}

		public boolean intersects(NameIndexAtomXY a) {
			if (bbox31 == null && a.bbox31 == null) {
				int z1 = bboxTileZoom, z2 = a.bboxTileZoom;
				long tid1 = bboxTileId, tid2 = a.bboxTileId;
				while (z1 > z2) {
					tid1 >>= 2;
					z1--;
				}
				while (z2 > z1) {
					tid2 >>= 2;
					z2--;
				}
				return tid1 == tid2;
			} else if (bbox31 == null && a.bbox31 != null) {
				return a.intersects(this);
			} else if (bbox31 != null && a.bbox31 == null) {
				int xleft = this.bbox31[0] >> (31 - a.bboxTileZoom);
				int xright = this.bbox31[2] >> (31 - a.bboxTileZoom);
				int ytop = this.bbox31[1] >> (31 - a.bboxTileZoom);
				int ybottom = this.bbox31[3] >> (31 - a.bboxTileZoom);
				long x = MapUtils.deinterleaveX(a.bboxTileId);
				long y = MapUtils.deinterleaveY(a.bboxTileId);
				return xleft <= x && x <= xright && ytop <= y && y <= ybottom;
			} else {
				// if exists [xleft, ytop, xright, ybottom]
				return this.bbox31[0] <= a.bbox31[2] && this.bbox31[2] >= a.bbox31[0] 
						&& this.bbox31[1] <= a.bbox31[3] && this.bbox31[3] >= a.bbox31[1];
				
			}
		}
		

		public String tileIdString() {
			return this.bboxTileZoom + " "
					+ MapUtils.deinterleaveX(bboxTileId) + " "
					+ MapUtils.deinterleaveY(bboxTileId);
		}

		private void init(AddressNameIndexDataAtom addr) {
			if (addr.getXy16Count() >= 1) {
				int xy16 = addr.getXy16(0);
				this.x16 = (xy16 >>> 16);
				this.y16 = (xy16 & ((1 << 16) - 1));
				bboxTileZoom = 15;
				bboxTileId = HashQuadTree.encodeTileId(bboxTileZoom, x16 / 2, y16 / 2);
				decodeBBox(addr.hasBbox() ? addr.getBbox() : null);
			}
			
			
		}

		private void decodeBBox(ByteString bbox) {
			if (bbox != null) {
				bbox31 = SearchAlgorithms.decodeBboxForNameAtomsBytes(bbox, x16, y16);
				if (bbox31 != null) {
					int z = 31;
					int xleft = bbox31[0], xright = bbox31[2];
					int ytop = bbox31[1], ybottom = bbox31[3];
					while (xleft != xright || ytop != ybottom) {
						z--;
						xleft >>= 1;
						xright >>= 1;
						ytop >>= 1;
						ybottom >>= 1;
					}
					bboxTileZoom = z;
					bboxTileId = HashQuadTree.encodeTileId(z, xleft, ytop);
				}
			}
		}

		private void init(OsmAndPoiNameIndexDataAtom poi) {
			this.x16 = poi.getX();
			this.y16 = poi.getY();
			bboxTileZoom = 16;
			bboxTileId = HashQuadTree.encodeTileId(bboxTileZoom, x16, y16);
			decodeBBox(poi.hasBbox() ? poi.getBbox() : null);
		}
	}

	public static class NameIndexAtom {
		String name;

		int type; //
		long id; // used to read object
		long parentid; // used to read object
		MapObject object;
		int otherWordsCnt = 0;
		NameIndexAtomXY coords;

		NameIndexAtom(String name, int type, long id, long pid, MapObject obj, int otherWordsCnt,
				NameIndexAtomXY coords) {
			this.name = name;
			this.id = id;
			this.parentid = pid;
			this.object = obj;
			this.type = type;
			this.otherWordsCnt = otherWordsCnt;
			this.coords = coords;
		}

		public boolean atomicObject() {
			return type == STREET_TYPE || type == POI_TYPE;
		}

		String typeStr() {
			String typeS = "";
			if (type == POI_TYPE) {
				typeS = "POI";
			} else {
				typeS = CityBlocks.getByType(type).toString();
			}
			return typeS;
		}

		String simpleName(String name) {
			return String.format("%s %s %d (%.4f, %.4f)", typeStr(), name, (id % 0xffff),
					MapUtils.get31LatitudeY(coords.y16 << 15), MapUtils.get31LongitudeX(coords.x16 << 15));
		}

		@Override
		public final String toString() {
			return object != null ? object.toString() : simpleName(name);
		}

	};

}