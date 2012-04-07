package net.osmand.data.preparation;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TLongObjectHashMap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


import net.osmand.Algoritms;
import net.osmand.binary.OsmandOdb.MapData;
import net.osmand.binary.OsmandOdb.MapDataBlock;
import net.osmand.data.MapAlgorithms;
import net.osmand.data.preparation.MapZooms.MapZoomPair;
import net.osmand.osm.Entity;
import net.osmand.osm.MapRenderingTypes;
import net.osmand.osm.MapUtils;
import net.osmand.osm.Node;
import net.osmand.osm.Way;
import net.osmand.osm.WayChain;
import net.osmand.osm.OSMSettings.OSMTagKey;

import org.apache.commons.logging.Log;
import org.apache.tools.bzip2.CBZip2InputStream;

public class BasemapProcessor {
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
	private BitSet seaTileInfo = new BitSet(BITS_COUNT);
	private BitSet landTileInfo = new BitSet(BITS_COUNT);
	
	private final int zoomWaySmothness;
	private final MapRenderingTypes renderingTypes;
	private final MapZooms mapZooms;
	private final Log logMapDataWarn;
	private SimplisticQuadTree[] quadTrees;
	
	private static class SimplisticQuadTree {
		int zoom;
		int x;
		int y;
		boolean ocean;
		boolean land;
		
		public SimplisticQuadTree(int x, int y, int zoom) {
			this.x = x;
			this.y = y;
			this.zoom = zoom;
		}
		
		SimplisticQuadTree[] children = null;
		Map<MapZoomPair,List<Way>> coastlines = null;
		
		
		public SimplisticQuadTree[] getAllChildren(){
			initChildren();
			return children;
		}
		
		public boolean areChildrenDefined(){
			return children != null;
		}
		
		public void addCoastline(MapZoomPair p, Way w){
			
			if(coastlines == null) {
				coastlines = new LinkedHashMap<MapZooms.MapZoomPair, List<Way>>();
			}
			if(!coastlines.containsKey(p)){
				coastlines.put(p, new ArrayList<Way>());
			}
			coastlines.get(p).add(w);
		}
		
		public boolean coastlinesDefined(MapZoomPair p){
			return coastlines != null && coastlines.get(p) != null;
		}
		
		public List<Way> getCoastlines(MapZoomPair p) {
			return coastlines.get(p);
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
	
	
	public BasemapProcessor(Log logMapDataWarn, MapZooms mapZooms, MapRenderingTypes renderingTypes, int zoomWaySmothness) {
		this.logMapDataWarn = logMapDataWarn;
		this.mapZooms = mapZooms;
		this.renderingTypes = renderingTypes;
		this.zoomWaySmothness = zoomWaySmothness;
		constructBitSetInfo();
		quadTrees = new SimplisticQuadTree[mapZooms.getLevels().size()];
		for (int i=0;i< mapZooms.getLevels().size(); i++) {
			MapZoomPair p = mapZooms.getLevels().get(i);
			quadTrees[i] = constructTilesQuadTree(Math.min(p.getMaxZoom() - 1, 12));
		}
	}

	private void constructBitSetInfo() {
		try {
			
			InputStream stream = BasemapProcessor.class.getResourceAsStream("oceantiles_12.dat.bz2");
			if (stream.read() != 'B' || stream.read() != 'Z') {
				throw new RuntimeException("The source stream must start with the characters BZ if it is to be read as a BZip2 stream."); //$NON-NLS-1$
			}
			InputStream dis = new CBZip2InputStream(stream);
			int currentByte;
			for (int i = 0; i < BITS_COUNT / 4; i++) {
				currentByte = dis.read();
				if (((currentByte >> 6) & BITMASK) == SEA) {
					seaTileInfo.set(i * 4);
				} else if (((currentByte >> 6) & BITMASK) == LAND) {
					landTileInfo.set(i * 4);
				}
				if (((currentByte >> 4) & BITMASK) == SEA) {
					seaTileInfo.set(i * 4 + 1);
				} else if (((currentByte >> 4) & BITMASK) == LAND) {
					landTileInfo.set(i * 4 + 1);
				}
				if (((currentByte >> 2) & BITMASK) == SEA) {
					seaTileInfo.set(i * 4 + 2);
				} else if (((currentByte >> 2) & BITMASK) == LAND) {
					landTileInfo.set(i * 4 + 2);
				}
				if ((currentByte & BITMASK) == SEA) {
					seaTileInfo.set(i * 4 + 3);
				} else if (((currentByte >> 0) & BITMASK) == LAND) {
					landTileInfo.set(i * 4 + 3);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("File with coastline tiles was not found ");
		}
	}
	
	private boolean isWaterTile(int x, int y, int zoom) {
		if (zoom >= TILE_ZOOMLEVEL) {
			int x1 = x >> (zoom - TILE_ZOOMLEVEL);
			int y1 = y >> (zoom - TILE_ZOOMLEVEL);
			if (!seaTileInfo.get(y1 * 4096 + x1)) {
				return false;
			}
			return true;
		} else {
			int x1 = x << (TILE_ZOOMLEVEL - zoom);
			int y1 = y << (TILE_ZOOMLEVEL - zoom);
			int max = 1 << TILE_ZOOMLEVEL - zoom;
			for (int i = 0; i < max; i++) {
				for (int j = 0; j < max; j++) {
					if (!seaTileInfo.get((y1 + i) * 4096 + (x1 + i))) {
						return false;
					}
				}
			}
			return true;
		}
	}
	
	private boolean isLandTile(int x, int y, int zoom) {
		if (zoom >= TILE_ZOOMLEVEL) {
			int x1 = x >> (zoom - TILE_ZOOMLEVEL);
			int y1 = y >> (zoom - TILE_ZOOMLEVEL);
			if (!landTileInfo.get(y1 * 4096 + x1)) {
				return false;
			}
			return true;
		} else {
			int x1 = x << (TILE_ZOOMLEVEL - zoom);
			int y1 = y << (TILE_ZOOMLEVEL - zoom);
			int max = 1 << TILE_ZOOMLEVEL - zoom;
			for (int i = 0; i < max; i++) {
				for (int j = 0; j < max; j++) {
					if (!landTileInfo.get((y1 + i) * 4096 + (x1 + i))) {
						return false;
					}
				}
			}
			return true;
		}
	}
	
	public SimplisticQuadTree constructTilesQuadTree(int maxZoom){
		SimplisticQuadTree rootTree = new SimplisticQuadTree(0, 0, 0);

		
		int baseZoom = 4;
		int tiles = 1 << baseZoom;
		ArrayList<SimplisticQuadTree> toVisit = new ArrayList<SimplisticQuadTree>();
		for (int x = 0; x < tiles; x++) {
			for (int y = 0; y < tiles; y++) {
				toVisit.add(rootTree.getOrCreateSubTree(x, y, baseZoom));
			}
		}
		initializeQuadTree(rootTree, baseZoom, maxZoom, toVisit);
		return rootTree;

	}

	protected ArrayList<SimplisticQuadTree> initializeQuadTree(SimplisticQuadTree rootTree, int baseZoom, int maxZoom,
			ArrayList<SimplisticQuadTree> toVisit) {
		for (int zoom = baseZoom; zoom <= maxZoom && !toVisit.isEmpty(); zoom++) {
			ArrayList<SimplisticQuadTree> newToVisit = new ArrayList<SimplisticQuadTree>();
			for (SimplisticQuadTree subtree : toVisit) {
				int x = subtree.x;
				int y = subtree.y;
				if (isWaterTile(x, y, zoom)) {
					rootTree.getOrCreateSubTree(x, y, zoom).ocean = true;
				} else if (isLandTile(x, y, zoom)) {
					rootTree.getOrCreateSubTree(x, y, zoom).land = true;
				} else if(zoom < TILE_ZOOMLEVEL){
					SimplisticQuadTree[] vis = rootTree.getOrCreateSubTree(x, y, zoom).getOrCreateSubTree(x, y, zoom)
							.getAllChildren();
					for (SimplisticQuadTree t : vis) {
						newToVisit.add(t);
					}
				}
			}
			toVisit = newToVisit;
		}
		return toVisit;
	}
	
	

	public void writeCoastlinesFile(BinaryMapIndexWriter writer, String regionName) throws IOException {
		writer.startWriteMapIndex(regionName);
		// write map encoding rules
		writer.writeMapEncodingRules(renderingTypes.getEncodingRuleTypes());

		int i = 0;
		for (MapZoomPair p : mapZooms.getLevels()) {
			// write map levels and map index
			writer.startWriteMapLevelIndex(p.getMinZoom(), p.getMaxZoom(), 0, (1 << 31) - 1, 0, (1 << 31) - 1);

			Map<SimplisticQuadTree, BinaryFileReference> refs = new LinkedHashMap<BasemapProcessor.SimplisticQuadTree, BinaryFileReference>();
			writeBinaryMapTree(quadTrees[i], writer, refs, p);

			// without data blocks
			writeBinaryMapBlock(quadTrees[i], writer, refs, p);

			writer.endWriteMapLevelIndex();
			i++;
		}
		writer.endWriteMapIndex();
		writer.flush();

	}

	private void writeBinaryMapBlock(SimplisticQuadTree simplisticQuadTree, BinaryMapIndexWriter writer,
			Map<SimplisticQuadTree, BinaryFileReference> refs, MapZoomPair p) throws IOException {
		Iterator<Entry<SimplisticQuadTree, BinaryFileReference>> it = refs.entrySet().iterator();
		TIntArrayList type = new TIntArrayList();
		type.add(renderingTypes.getCoastlineRuleType().getTargetId());
		
		while(it.hasNext()) {
			Entry<SimplisticQuadTree, BinaryFileReference> e = it.next();
			MapDataBlock.Builder dataBlock = MapDataBlock.newBuilder();
			SimplisticQuadTree quad = e.getKey();
			
			for (Way w : quad.getCoastlines(p)) {
				dataBlock.setBaseId(w.getId());
				List<Node> res = new ArrayList<Node>();
				MapAlgorithms.simplifyDouglasPeucker(w.getNodes(), p.getMaxZoom() - 1 + 8 + zoomWaySmothness, 3, res);
				ByteArrayOutputStream bcoordinates = new ByteArrayOutputStream();
				for (Node n : res) {
					if (n != null) {
						int y = MapUtils.get31TileNumberY(n.getLatitude());
						int x = MapUtils.get31TileNumberX(n.getLongitude());
						Algoritms.writeInt(bcoordinates, x);
						Algoritms.writeInt(bcoordinates, y);
					}
				}
				MapData mapData = writer.writeMapData(0,
						quad.x << (31 - quad.zoom), quad.y << (31 - quad.zoom), false,
						bcoordinates.toByteArray(), null, type, null, null, null, dataBlock);
				if (mapData != null) {
					dataBlock.addDataObjects(mapData);
				}
			}
			
			writer.writeMapDataBlock(dataBlock, null, e.getValue());
		}
	}

	private void writeBinaryMapTree(SimplisticQuadTree quadTree, BinaryMapIndexWriter writer,
			Map<SimplisticQuadTree, BinaryFileReference> refs, MapZoomPair p) throws IOException {
		int xL = (quadTree.x) << (31 - quadTree.zoom);
		int xR = ((quadTree.x + 1) << (31 - quadTree.zoom)) - 1;
		int yT = (quadTree.y) << (31 - quadTree.zoom);
		int yB = ((quadTree.y + 1) << (31 - quadTree.zoom)) - 1;
		boolean defined = quadTree.coastlinesDefined(p);
		boolean ocean = false;
		boolean land = false;
		if (!defined) {
			ocean = quadTree.ocean ||  isWaterTile(quadTree.x, quadTree.y, quadTree.zoom);
			land = quadTree.land || isLandTile(quadTree.x, quadTree.y, quadTree.zoom);
		}
		BinaryFileReference ref = writer.startMapTreeElement(xL, xR, yT, yB, defined, ocean, land);
		if (ref != null) {
			refs.put(quadTree, ref);
		}

		if (quadTree.areChildrenDefined()) {
			SimplisticQuadTree[] allChildren = quadTree.getAllChildren();

			for (SimplisticQuadTree ch : allChildren) {
				writeBinaryMapTree(ch, writer, refs, p);
			}
		}
		writer.endWriteMapTreeElement();

	}
	
	public void processEntity(Entity e) {
		if(e instanceof Way && "coastline".equals(e.getTag(OSMTagKey.NATURAL))) {
			processCoastline((Way) e);
		}
	}
	
	public void processCoastline(Way e) {
		renderingTypes.getCoastlineRuleType().updateFreq();
		int ij = 0;
		for(MapZoomPair zoomPair : mapZooms.getLevels()) {
			SimplisticQuadTree quadTree = quadTrees[ij++];
			int z = Math.min((zoomPair.getMinZoom() + zoomPair.getMaxZoom()) / 2 - 1, zoomPair.getMinZoom() + 1);
			List<Node> ns = e.getNodes();
			if(ns.size() < 2) {
				return;
			}
			int i = 1;
			Node prevNode = ns.get(0);
			int px31 = MapUtils.get31TileNumberX(prevNode.getLongitude());
			int py31 = MapUtils.get31TileNumberY(prevNode.getLatitude());
			while(i<ns.size()) {
				Way w = new Way(-1000);
				w.addNode(prevNode);
				int tilex = px31 >> (31 - z);
				int tiley = py31 >> (31 - z);
				boolean sameTile = true;
				wayConstruct : while(sameTile && i<ns.size()) {
				    Node next = ns.get(i);
					int ntilex = (int) MapUtils.getTileNumberX(z, next.getLongitude());
					int ntiley = (int) MapUtils.getTileNumberY(z, next.getLatitude());
					if(ntilex == tilex && tiley == ntiley) {
						sameTile = true;
						w.addNode(next);
						prevNode = next;
						px31 = MapUtils.get31TileNumberX(prevNode.getLongitude());
						py31 = MapUtils.get31TileNumberY(prevNode.getLatitude());
						i++;
					} else {
						int nx31 = MapUtils.get31TileNumberX(next.getLongitude());
						int ny31 = MapUtils.get31TileNumberY(next.getLatitude());
						// increase boundaries to drop into another tile
						int leftX = (tilex << (31 - z)) - 1;
						int rightX = (tilex + 1) << (31 - z);
						if (rightX < 0) {
							rightX = Integer.MAX_VALUE;
						}
						int topY = (tiley << (31 - z)) - 1;
						int bottomY = (tiley + 1) << (31 - z);
						if (bottomY < 0) {
							bottomY = Integer.MAX_VALUE;
						}

						long inter = MapAlgorithms.calculateIntersection(px31, py31, nx31, ny31, leftX, rightX, bottomY, topY);
						int cy31 = (int) inter;
						int cx31 = (int) (inter >> 32l);
						if (inter == -1) {
							cx31 = nx31;
							cy31 = ny31;
							i++;
							logMapDataWarn.warn("Can't find intersection for " + MapUtils.get31LongitudeX(px31) + ","
									+ MapUtils.get31LatitudeY(py31) + " - " + +MapUtils.get31LongitudeX(nx31) + ","
									+ MapUtils.get31LatitudeY(ny31));
						}

						prevNode = new Node(MapUtils.get31LatitudeY(cy31), MapUtils.get31LongitudeX(cx31), -1000);
						px31 = cx31;
						py31 = cy31;
						w.addNode(prevNode);
						break wayConstruct;
					}
				}
				SimplisticQuadTree quad = quadTree.getOrCreateSubTree(tilex, tiley, z);
				if (quad == null) {
					if (logMapDataWarn != null) {
						logMapDataWarn.error("Tile " + tilex + " / " + tiley + " at " + z + " can not be found");
					} else {
						System.err.println("Tile " + tilex + " / " + tiley + " at " + z + " can not be found");
					}
				}
				quad.addCoastline(zoomPair, w);
			
			}
		}
	}

	
}
