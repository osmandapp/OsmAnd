package net.osmand.map;

import gnu.trove.list.array.TLongArrayList;
import gnu.trove.set.hash.TLongHashSet;

import java.util.ArrayList;
import java.util.List;

import net.osmand.Algoritms;
import net.osmand.map.OsmandRegionInfo.OsmAndRegion;
import net.osmand.osm.MapUtils;

public class RegionCountry {
	public String continentName;
	public TLongArrayList tiles = new TLongArrayList();
	public int left, right, top, bottom;
	public String name;
	public RegionCountry parent;
	
	private List<RegionCountry> regions = new ArrayList<RegionCountry>();
	private final static int SHIFT = 5;

	public void add(int xdeg, int ydeg) {
		if (tiles.size() == 0) {
			left = right = xdeg;
			top = bottom = ydeg;
		}
		left = Math.min(xdeg, left);
		right = Math.max(xdeg, right);
		bottom = Math.min(ydeg, bottom);
		top = Math.max(ydeg, top);
		tiles.add((xdeg << SHIFT) + ydeg);
	}

	public int getLon(int i) {
		return (int) (tiles.get(i) >> SHIFT);
	}

	public int getLat(int i) {
		return (int) (tiles.get(i) - ((tiles.get(i) >> SHIFT) << SHIFT));
	}
	
	public void addSubregion(RegionCountry c) {
		c.parent = this;
		regions.add(c);
	}
	
	public List<RegionCountry> getSubRegions() {
		return regions;
	}

	public TLongHashSet calculateTileSet(TLongHashSet t, int z) {
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
	}
	
	public static RegionCountry construct(OsmAndRegion reg) {
		RegionCountry rc = new RegionCountry();
		if (reg.hasContinentName()) {
			rc.continentName = reg.getContinentName();
		}
		rc.name = reg.getName();
		for (int i = 0; i < reg.getDegXCount(); i++) {
			rc.add(reg.getDegX(i), reg.getDegY(i));
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
		for (int i = 0; i < this.tiles.size(); i++) {
			reg.addDegX(this.getLon(i) - px);
			reg.addDegY(this.getLat(i) - py);
			px = this.getLon(i);
			py = this.getLat(i);
		}
		String n = Algoritms.capitalizeFirstLetterAndLowercase(this.name.replace('_', ' '));
		reg.setName(n);
		if(this.continentName != null) {
			reg.setContinentName(Algoritms.capitalizeFirstLetterAndLowercase(this.continentName));
		}
		for (RegionCountry c : this.regions) {
			reg.addSubregions(c.convert());
		}
		return reg.build();
	}
}