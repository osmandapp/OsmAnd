package net.osmand.map;

import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.List;

import net.osmand.map.OsmandRegionInfo.OsmAndRegion;
import net.osmand.util.Algorithms;

public class RegionCountry {
	public String continentName;
	public TIntArrayList tiles = new TIntArrayList();
	public int left, right, top, bottom;
	public String name;
	public RegionCountry parent;
	
	private List<RegionCountry> regions = new ArrayList<RegionCountry>();

	public void add(int xdeg, int ydeg) {
		if (tiles.size() == 0) {
			left = right = xdeg;
			top = bottom = ydeg;
		}
		left = Math.min(xdeg, left);
		right = Math.max(xdeg, right);
		bottom = Math.min(ydeg, bottom);
		top = Math.max(ydeg, top);
		tiles.add(xdeg);
		tiles.add(ydeg);
	}
	
	public int getTileSize() {
		return tiles.size()/2;
	}

	public int getLon(int i) {
		return tiles.get(i*2);
	}

	public int getLat(int i) {
		return tiles.get(i*2 + 1);
	}
	
	public void addSubregion(RegionCountry c) {
		c.parent = this;
		regions.add(c);
	}
	
	public List<RegionCountry> getSubRegions() {
		return regions;
	}

	/*public TLongHashSet calculateTileSet(TLongHashSet t, int z) {
		for (int j = 0; j < tiles.size(); j++) {
			int kx = (int) MapUtils.getTileNumberX(z, getLon(j));
			int ex = (int) MapUtils.getTileNumberX(z, getLon(j) + 0.9999f);
			int ky = (int) MapUtils.getTileNumberY(z, getLat(j));
			int ey = (int) MapUtils.getTileNumberY(z, getLat(j) - 0.9999f);
			for (int x = kx; x <= ex; x++) {
				for (int y = ky; y <= ey; y++) {
					long v = (((long) y) << 31) + x;
					t.add(v);
				}
			}
		}
		return t;
	}*/
	
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