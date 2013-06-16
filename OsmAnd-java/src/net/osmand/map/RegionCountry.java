package net.osmand.map;

import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.List;

import net.osmand.map.OsmandRegionInfo.OsmAndRegion;
import net.osmand.util.Algorithms;

public class RegionCountry {
	public String continentName;
	public int left, right, top, bottom;
	public boolean subregionsVerified = true;
	public String name;
	public RegionCountry parent;
	
	private TIntArrayList singleTiles = new TIntArrayList();
	private TIntArrayList boxTiles = new TIntArrayList();
	
	private TIntArrayList allTiles;
	
	private List<RegionCountry> regions = new ArrayList<RegionCountry>();
	
	public TIntArrayList getSingleTiles() {
		return singleTiles;
	}
	public TIntArrayList getBoxTiles() {
		return boxTiles;
	}
	
	public String serializeTilesArray(){
		StringBuilder bld = new StringBuilder();
		for(int j = 0; j<boxTiles.size(); j+=4) {
			if (j > 0) {
				bld.append(";");
			}
			bld.append(boxTiles.get(j)).append(" ").append(boxTiles.get(j + 1));
			bld.append(" x ");
			bld.append(boxTiles.get(j + 2)).append(" ").append(boxTiles.get(j + 3));
		}
		for (int i = 0; i < singleTiles.size(); i += 2) {
			if (i > 0 || boxTiles.size() > 0) {
				bld.append(";");
			}
			bld.append(singleTiles.get(i)).append(" ").append(singleTiles.get(i + 1));
		}
		
		return bld.toString();
	}
	
	public String serializeFlatTilesArray(){
		TIntArrayList tiles = calcAllTiles();
		StringBuilder bld = new StringBuilder();
		for (int i = 0; i < tiles.size(); i += 2) {
			if (i > 0) {
				bld.append(";");
			}
			bld.append(tiles.get(i)).append(" ").append(tiles.get(i + 1));
		}
		
		return bld.toString();
	}

	
	public void removeSingle(int x, int y) {
		for (int i = 0; i < singleTiles.size(); i += 2) {
			if (singleTiles.get(i) == x && singleTiles.get(i + 1) == y) {
				singleTiles.removeAt(i);
				singleTiles.removeAt(i);
				break;
			}
		}
	}
	
	public void add(int xdeg, int ydeg) {
		if (isEmpty()) {
			left = right = xdeg;
			top = bottom = ydeg;
		}
		left = Math.min(xdeg, left);
		right = Math.max(xdeg, right);
		bottom = Math.min(ydeg, bottom);
		top = Math.max(ydeg, top);
		singleTiles.add(xdeg);
		singleTiles.add(ydeg);
		allTiles = null;
	}
	
	public void add(int xleft, int ytop, int xright, int ybottom) {
		if (isEmpty()) {
			left = xleft;
			right = xright;
			top = ytop;
			bottom = ybottom;
		}
		left = Math.min(xleft, left);
		right = Math.max(xright, right);
		bottom = Math.min(ybottom, bottom);
		top = Math.max(ytop, top);
		boxTiles.add(xleft);
		boxTiles.add(ytop);
		boxTiles.add(xright);
		boxTiles.add(ybottom);
		allTiles = null;
	}

	public boolean isEmpty() {
		return singleTiles.size() == 0 && boxTiles.isEmpty();
	}
	
	public int getTileSize() {
		calcAllTiles();
		return allTiles.size() / 2;
	}

	protected TIntArrayList calcAllTiles() {
		if (allTiles == null) {
			allTiles = new TIntArrayList(singleTiles);
			for (int k = 0; k < boxTiles.size(); k += 4) {
				for (int x = boxTiles.get(k); x <= boxTiles.get(k + 2); x++) {
					for (int y = boxTiles.get(k + 1); y >= boxTiles.get(k + 3); y--) {
						allTiles.add(x);
						allTiles.add(y);
					}
				}
			}
		}
		return allTiles;
	}

	public int getLon(int i) {
		return calcAllTiles().get(i * 2);
	}

	public int getLat(int i) {
		return calcAllTiles().get(i * 2 + 1);
	}
	
	public void addSubregion(RegionCountry c) {
		c.parent = this;
		regions.add(c);
	}
	
	public List<RegionCountry> getSubRegions() {
		return regions;
	}

	public static RegionCountry construct(OsmAndRegion reg) {
		RegionCountry rc = new RegionCountry();
		if (reg.hasContinentName()) {
			rc.continentName = reg.getContinentName();
		}
		rc.name = reg.getName();
		int px = 0;
		int py = 0;
		for (int i = 0; i < reg.getDegXCount(); i++) {
			px = reg.getDegX(i) + px;
			py = reg.getDegY(i) + py;
			rc.add(px, py);
		}
		for (int i = 0; i < reg.getSubregionsCount(); i++) {
			rc.addSubregion(construct(reg.getSubregions(i)));
		}
		return rc;
	}
	
	public OsmAndRegion convert() {
		OsmAndRegion.Builder reg = OsmAndRegion.newBuilder();
		// System.out.println(r.name + " " + r.tiles.size() + " ?= " + r.calculateTileSet(new TLongHashSet(), 8).size());
		int px = 0;
		int py = 0;
		for (int i = 0; i < this.getTileSize(); i++) {
			reg.addDegX(this.getLon(i) - px);
			reg.addDegY(this.getLat(i) - py);
			px = this.getLon(i);
			py = this.getLat(i);
		}
		String n = Algorithms.capitalizeFirstLetterAndLowercase(this.name.replace('_', ' '));
		reg.setName(n);
		if(this.continentName != null) {
			reg.setContinentName(Algorithms.capitalizeFirstLetterAndLowercase(this.continentName));
		}
		for (RegionCountry c : this.regions) {
			reg.addSubregions(c.convert());
		}
		return reg.build();
	}
	
}