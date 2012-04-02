package net.osmand.data.preparation;

import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.zip.ZipInputStream;

import javax.xml.stream.XMLStreamException;

import org.apache.tools.bzip2.CBZip2InputStream;

import rtree.Element;
import rtree.LeafElement;
import rtree.NonLeafElement;
import rtree.RTree;
import rtree.RTreeException;
import rtree.Rect;


import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.procedure.TObjectProcedure;
import net.osmand.Algoritms;
import net.osmand.binary.OsmandOdb.MapData;
import net.osmand.binary.OsmandOdb.MapDataBlock;
import net.osmand.data.MapAlgorithms;
import net.osmand.osm.Entity;
import net.osmand.osm.Entity.EntityId;
import net.osmand.osm.MapRenderingTypes.MapRulType;
import net.osmand.osm.EntityInfo;
import net.osmand.osm.MapRenderingTypes;
import net.osmand.osm.MapUtils;
import net.osmand.osm.Node;
import net.osmand.osm.Way;
import net.osmand.osm.WayChain;
import net.osmand.osm.io.OsmBaseStorage;
import net.osmand.osm.io.OsmStorageWriter;

public class CoastlineProcessor {
	TLongObjectHashMap<WayChain> coastlinesEndPoint = new TLongObjectHashMap<WayChain>();
	TLongObjectHashMap<WayChain> coastlinesStartPoint = new TLongObjectHashMap<WayChain>();
	
	private static final byte SEA = 0x2;
	private static final byte LAND = 0x1;

	/**
	 * The zoom level for which the tile info is valid.
	 */
	public static final byte TILE_ZOOMLEVEL = 12;
	private static final byte BITMASK = 0x3;
	private static final int BITS_COUNT = (1 << TILE_ZOOMLEVEL) * (1 << TILE_ZOOMLEVEL);

	private final BitSet seaTileInfo = new BitSet(BITS_COUNT);
	private final BitSet landTileInfo = new BitSet(BITS_COUNT);
	
	private static class SimplisticQuadTree {
		public boolean ocean;
		public boolean land;
		int zoom;
		int x;
		int y;
		
		public SimplisticQuadTree(int x, int y, int zoom) {
			this.x = x;
			this.y = y;
			this.zoom = zoom;
		}
		
		SimplisticQuadTree[] children = null;
		
		public SimplisticQuadTree[] getAllChildren(){
			initChildren();
			return children;
		}
		
		public boolean areChildrenDefined(){
			return children != null;
		}
		
		public SimplisticQuadTree getOrCreateSubTree(int x, int y, int zm) {
			if (zm <= zoom) {
				return this;
			} else {
				initChildren();
				int nx = (x >> (zm - zoom - 1)) - (this.x << 1);
				int ny = (y >> (zm - zoom - 1)) - (this.y << 1);
				if (nx > 1 || nx < 0 || ny > 1 || ny < 0) {
					return null;
				}
				return children[nx * 2 + ny].getOrCreateSubTree(x, y, zm);
			}
		}

		private void initChildren() {
			if (children == null) {
				children = new SimplisticQuadTree[4];
				for (int i = 0; i < 2; i++) {
					for (int j = 0; j < 2; j++) {
						children[i * 2 + j] = new SimplisticQuadTree(((this.x << 1) + i), ((this.y << 1) + j), zoom + 1);
					}
				}
			}
		}
		
	}
	
	
	public CoastlineProcessor() {
		try {
			InputStream stream = CoastlineProcessor.class.getResourceAsStream("oceantiles_12.dat.bz2");
			if (stream.read() != 'B' || stream.read() != 'Z') {
				throw new RuntimeException("The source stream must start with the characters BZ if it is to be read as a BZip2 stream."); //$NON-NLS-1$
			}
			InputStream dis = new CBZip2InputStream(stream);
			int currentByte;
			for (int i = 0; i < BITS_COUNT / 4; i++) {
				currentByte = dis.read();
				if (((currentByte >> 6) & BITMASK) == SEA) {
					this.seaTileInfo.set(i * 4);
				} else if (((currentByte >> 6) & BITMASK) == LAND) {
					this.landTileInfo.set(i * 4);
				}
				if (((currentByte >> 4) & BITMASK) == SEA) {
					this.seaTileInfo.set(i * 4 + 1);
				} else if (((currentByte >> 4) & BITMASK) == LAND) {
					this.landTileInfo.set(i * 4 + 1);
				}
				if (((currentByte >> 2) & BITMASK) == SEA) {
					this.seaTileInfo.set(i * 4 + 2);
				} else if (((currentByte >> 2) & BITMASK) == LAND) {
					this.landTileInfo.set(i * 4 + 2);
				}
				if ((currentByte & BITMASK) == SEA) {
					this.seaTileInfo.set(i * 4 + 3);
				} else if (((currentByte >> 0) & BITMASK) == LAND) {
					this.landTileInfo.set(i * 4 + 3);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("File with coastline tiles was not found ");
		}
	}
	
	public boolean isWaterTile(int x, int y, int zoom) {
		if (zoom >= TILE_ZOOMLEVEL) {
			int x1 = x >> (zoom - TILE_ZOOMLEVEL);
			int y1 = y >> (zoom - TILE_ZOOMLEVEL);
			if (!this.seaTileInfo.get(y1 * 4096 + x1)) {
				return false;
			}
			return true;
		} else {
			int x1 = x << (TILE_ZOOMLEVEL - zoom);
			int y1 = y << (TILE_ZOOMLEVEL - zoom);
			int max = 1 << TILE_ZOOMLEVEL - zoom;
			for (int i = 0; i < max; i++) {
				for (int j = 0; j < max; j++) {
					if (!this.seaTileInfo.get((y1 + i) * 4096 + (x1 + i))) {
						return false;
					}
				}
			}
			return true;
		}
	}
	
	public boolean isLandTile(int x, int y, int zoom) {
		if (zoom >= TILE_ZOOMLEVEL) {
			int x1 = x >> (zoom - TILE_ZOOMLEVEL);
			int y1 = y >> (zoom - TILE_ZOOMLEVEL);
			if (!this.landTileInfo.get(y1 * 4096 + x1)) {
				return false;
			}
			return true;
		} else {
			int x1 = x << (TILE_ZOOMLEVEL - zoom);
			int y1 = y << (TILE_ZOOMLEVEL - zoom);
			int max = 1 << TILE_ZOOMLEVEL - zoom;
			for (int i = 0; i < max; i++) {
				for (int j = 0; j < max; j++) {
					if (!this.landTileInfo.get((y1 + i) * 4096 + (x1 + i))) {
						return false;
					}
				}
			}
			return true;
		}
	}
	
	public SimplisticQuadTree calculateTilesQuadTree(){
		SimplisticQuadTree rootTree = new SimplisticQuadTree(0, 0, 0);

		int baseZoom = 4;
		int tiles = 1 << baseZoom;
		ArrayList<SimplisticQuadTree> toVisit = new ArrayList<SimplisticQuadTree>();
		int cnt = 0;
		for (int x = 0; x < tiles; x++) {
			for (int y = 0; y < tiles; y++) {
				toVisit.add(rootTree.getOrCreateSubTree(x, y, baseZoom));
			}
		}
		int ntc = 0;
		for (int zoom = baseZoom; zoom <= TILE_ZOOMLEVEL && !toVisit.isEmpty(); zoom++) {
			cnt = 0;
			ArrayList<SimplisticQuadTree> newToVisit = new ArrayList<SimplisticQuadTree>();
			for (SimplisticQuadTree subtree : toVisit) {
				int x = subtree.x;
				int y = subtree.y;
				if (isWaterTile(x, y, zoom)) {
					cnt++;
					rootTree.getOrCreateSubTree(x, y, zoom).ocean = true;
				} else if (isLandTile(x, y, zoom)) {
					rootTree.getOrCreateSubTree(x, y, zoom).land = true;
					cnt++;
				} else if(zoom < TILE_ZOOMLEVEL){
					SimplisticQuadTree[] vis = rootTree.getOrCreateSubTree(x, y, zoom).getOrCreateSubTree(x, y, zoom)
							.getAllChildren();
					for (SimplisticQuadTree t : vis) {
						newToVisit.add(t);
					}
				} else {
					ntc ++;
				}
			}
			System.out.println(" Zoom " + zoom + " count " + cnt);
			toVisit = newToVisit;
		}
		System.out.println("Not covered " + ntc + " from " + (float) ntc / ((1 << TILE_ZOOMLEVEL) * (1<<TILE_ZOOMLEVEL)));
		return rootTree;

	}
	
	

	
	public void processCoastline(Way e) {
		WayChain chain = null;
		if(coastlinesEndPoint.contains(e.getFirstNodeId())){
			chain = coastlinesEndPoint.remove(e.getFirstNodeId());
			chain.append(e);
			coastlinesEndPoint.put(chain.getLastNode(), chain);
		}
		if(coastlinesStartPoint.contains(e.getLastNodeId())) {
			WayChain chain2 = coastlinesStartPoint.remove(e.getLastNodeId());
			if(chain == null) {
				chain = chain2;
				chain.prepend(e);
				coastlinesStartPoint.put(chain.getFistNode(), chain);
			} else if(chain2 != chain) {
				// remove chain 2
				coastlinesEndPoint.remove(chain2.getLastNode());
				chain.append(chain2);
				coastlinesEndPoint.put(chain.getLastNode(), chain);
			} else {
				// cycle detected : skip it
			}
		}
		if(chain == null) {
			chain = new WayChain(e);
			coastlinesEndPoint.put(chain.getLastNode(), chain);
			coastlinesStartPoint.put(chain.getFistNode(), chain);
		}
	}
	
	private long nodeId = 1 << 100;
	
	private class CoastlineTile {
		List<List<Node>> chains = new ArrayList<List<Node>>();
		List<List<Node>> islands = new ArrayList<List<Node>>();
		
		double lleft = 0;
		double lright = 0;
		double ltop = 0;
		double lbottom = 0;

		private CoastlineTile(List<Node> chain) {
			ltop = lbottom = chain.get(0).getLatitude();
			lleft = lright = chain.get(0).getLongitude();
			addChain(chain);
		}
		
		public boolean intersect(CoastlineTile t) {
			if (lleft > t.lright || lright < t.lleft || ltop < t.lbottom || lbottom > t.ltop) {
				return false;
			}
			return true;
		}
		
		public void combineTiles(CoastlineTile t) {
			for(List<Node> chain : t.chains) {
				addSimpleChain(chain);
			}
			for(List<Node> island : t.islands) {
				addIsland(island);
			}
		}

		public boolean contains(List<Node> chain) {
			for (int i = 0; i < chain.size(); i++) {
				if (lleft <= chain.get(i).getLongitude() && lright >= chain.get(i).getLongitude()) {
					if (lbottom <= chain.get(i).getLatitude() && ltop >= chain.get(i).getLatitude()) {
						return true;
					}
				}
			}
			return false;
		}
		
		private void addSimpleChain(List<Node> chain) {
			updateBoundaries(chain);
			chains.add(chain);
		}
		
		public void addChain(List<Node> chain) {
			updateBoundaries(chain);
			int ind = 0;
			while (ind < chain.size() - 1) {
				List<Node> subChain = new ArrayList<Node>();
				Node first = chain.get(ind);
				boolean directionToRight = chain.get(ind + 1).getLongitude() > first.getLongitude();
				int nextLonMaximum = ind + 1;
				double lonEnd = first.getLongitude();
				double latPeek = first.getLatitude();
				int latPeekInd = ind;
				int latLocPeekInd = ind;
				for (int j = ind + 1; j < chain.size(); j++) {
					if (directionToRight) {
						if (chain.get(j).getLatitude() <= latPeek) {
							latPeek = chain.get(j).getLatitude();
							latPeekInd = j;
						}
						if (chain.get(j).getLongitude() >= lonEnd) {
							nextLonMaximum = j;
							lonEnd = chain.get(j).getLongitude();
							latLocPeekInd = latPeekInd;
						} else if (chain.get(j).getLongitude() < first.getLongitude()) {
							break;
						}
					} else {
						if (chain.get(j).getLatitude() >= latPeek) {
							latPeek = chain.get(j).getLatitude();
							latPeekInd = j;
						}
						if (chain.get(j).getLongitude() <= lonEnd) {
							nextLonMaximum = j;
							lonEnd = chain.get(j).getLongitude();
							latLocPeekInd = latPeekInd;
						} else if (chain.get(j).getLongitude() > first.getLongitude()) {
							break;
						}
					}
				}
				if(latLocPeekInd > ind) {
					for (int i = ind; i <= latLocPeekInd; i++) {
						subChain.add(chain.get(i));
					}
					Node ned = new Node(chain.get(latLocPeekInd).getLatitude(), first.getLongitude(), nodeId++);
					subChain.add(ned);
					subChain.add(first);
					chains.add(subChain);
					subChain= new ArrayList<Node>();
				}
				
				for (int i = latLocPeekInd; i <= nextLonMaximum; i++) {
					subChain.add(chain.get(i));
				}
				Node ned = new Node(chain.get(latLocPeekInd).getLatitude(), lonEnd, nodeId++);
				subChain.add(ned);
				ind = nextLonMaximum;
				subChain.add(chain.get(latLocPeekInd));
				chains.add(subChain);

			}
		}
		
		public void addIsland(List<Node> chain){
			updateBoundaries(chain);
			islands.add(chain);
		}
		
		public void updateBoundaries(List<Node> chain) {
			for (int i = 0; i < chain.size(); i++) {
				lleft = Math.min(lleft, chain.get(i).getLongitude());
				lright = Math.max(lright, chain.get(i).getLongitude());
				ltop = Math.max(ltop, chain.get(i).getLatitude());
				lbottom = Math.min(lbottom, chain.get(i).getLatitude());
			}
		}
	
	}
	
	
	
	public void processCoastlines() {
		System.out.println("Way chains " + coastlinesStartPoint.size());
		final List<CoastlineTile> processed = new ArrayList<CoastlineTile>();
		final List<List<Node>> islands = new ArrayList<List<Node>>();
		coastlinesStartPoint.forEachValue(new TObjectProcedure<WayChain>() {
			@Override
			public boolean execute(WayChain object) {
				boolean closed = object.getFistNode() == object.getLastNode();
				if (!closed) {
					List<Node> ns = object.getChainNodes();
					boolean update = true;
					CoastlineTile tile = new CoastlineTile(ns);
					while (update) {
						Iterator<CoastlineTile> it = processed.iterator();
						update = false;
						while (it.hasNext()) {
							CoastlineTile newTile = it.next();
							if (newTile.intersect(tile)) {
								it.remove();
								newTile.combineTiles(tile);
								tile = newTile;
								update = true;
								break;
							}
						}
					}
					processed.add(0, tile);
					
					System.out.println((closed ? "Closed " : "Not closed ") + "way sizes " + object.getWays().size() + " ids "
							+ object.getWays());
				} else {
					List<Node> nodes = object.getChainNodes();
					Way w = new Way(-1, nodes);
					if(w.getFirstNodeId() != w.getLastNodeId()) {
						w.addNode(w.getNodes().get(0));
					}
					if(MapAlgorithms.isClockwiseWay(w)) {
						if(!object.isIncomplete()) {
							System.out.println("??? Lake " + object.getWays());
						}
					} else {
						islands.add(w.getNodes());
					}
				}
				return true;
			}
		});
		for(List<Node> island : islands) {
			boolean log = true;
			for(CoastlineTile ts : processed) {
				if(ts.contains(island)){
					ts.addIsland(island);
					log = false;
					break;
				}
			}
			if(log) {
				System.out.println("Island missed");
			}
		}
		OsmBaseStorage st = new OsmBaseStorage();
		OsmStorageWriter writer = new OsmStorageWriter();
		Map<EntityId, Entity> entities = st.getRegisteredEntities();
		for(CoastlineTile ts : processed) {
			System.out.println("Coastline Tile  left,top,right,bottom : " +((float)ts.lleft)+","
					+((float)ts.ltop)+","+((float)ts.lright)+","+((float)ts.lbottom));
			System.out.println(" Chains " + ts.chains.size() + " islands " + ts.islands.size());
			for(List<Node> ns : ts.chains) {
				registerWay(entities, st.getRegisteredEntityInfo(),  ns);
			}
			for(List<Node> ns : ts.islands) {
				registerWay(entities, st.getRegisteredEntityInfo(), ns);
			}
		}
		try {
			writer.saveStorage(new FileOutputStream("/home/victor/projects/OsmAnd/data/osm-maps/check_coastline.osm"), st, null, true);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (XMLStreamException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}



	long id = 10000;
	private void registerWay(Map<EntityId, Entity> entities, Map<EntityId, EntityInfo> map, List<Node> ns) {
		Way w = new Way(id++, ns);
		for(Node n : ns) {
			entities.put(EntityId.valueOf(n), n);
			map.put(EntityId.valueOf(n), new EntityInfo("1"));
		}
		entities.put(EntityId.valueOf(w), w);
		map.put(EntityId.valueOf(w), new EntityInfo("1"));
	}
	
	public static void main(String[] args) throws IOException {

		CoastlineProcessor proc = new CoastlineProcessor();
		BinaryMapIndexWriter writer = new BinaryMapIndexWriter(new RandomAccessFile(
				"/home/victor/projects/OsmAnd/data/osm-gen/coastline.obf", "rw"));
		proc.writeCoastlinesFile( writer,
				proc.calculateTilesQuadTree());
		writer.close();
	}

	private void writeCoastlinesFile(BinaryMapIndexWriter writer, SimplisticQuadTree simplisticQuadTree) throws IOException {
		writer.startWriteMapIndex("Coastline");
		// write map encoding rules
		writer.writeMapEncodingRules(MapRenderingTypes.getDefault().getEncodingRuleTypes());

		// write map levels and map index
		writer.startWriteMapLevelIndex(1, 12, 0, 0, 1 << 31, 1 << 31);
		
		Map<SimplisticQuadTree, BinaryFileReference> refs = new LinkedHashMap<CoastlineProcessor.SimplisticQuadTree, BinaryFileReference>();
		writeBinaryMapTree(simplisticQuadTree, writer, refs);

		// without data blocks
//		writeBinaryMapBlock(root, rootBounds, rtree, writer, selectData, treeHeader, new LinkedHashMap<String, Integer>(),
//				new LinkedHashMap<MapRenderingTypes.MapRulType, String>());

		writer.endWriteMapLevelIndex();

		writer.endWriteMapIndex();
		writer.flush();

	}

	private void writeBinaryMapTree(SimplisticQuadTree quadTree, BinaryMapIndexWriter writer,
			Map<SimplisticQuadTree, BinaryFileReference> refs) throws IOException {
		int xL = (quadTree.x) << (31 - quadTree.zoom);
		int xR = (quadTree.x + 1) << (31 - quadTree.zoom) - 1;
		int yT = (quadTree.y) << (31 - quadTree.zoom);
		int yB = (quadTree.y + 1) << (31 - quadTree.zoom) - 1;
		long fp = writer.getFilePointer();
		BinaryFileReference ref = writer.startMapTreeElement(xL, xR, yT, yB, false, 
				quadTree.ocean, quadTree.land);
		if (ref != null) {
			refs.put(quadTree, ref);
		}
		
		if (quadTree.areChildrenDefined()) {
			SimplisticQuadTree[] allChildren = quadTree.getAllChildren();

			for (SimplisticQuadTree ch : allChildren) {
				writeBinaryMapTree(ch, writer, refs);
			}
		}
		writer.endWriteMapTreeElement();
		
	}
	
	
}
