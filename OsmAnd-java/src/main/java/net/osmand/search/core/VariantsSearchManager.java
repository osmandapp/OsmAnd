package net.osmand.search.core;

import net.osmand.PlatformUtil;
import net.osmand.binary.Abbreviations;
import net.osmand.binary.CommonWords;
import net.osmand.map.OsmandRegions;
import net.osmand.map.WorldRegion;
import net.osmand.search.core.SearchSettings.SearchVersion;
import net.osmand.util.Algorithms;

import java.io.IOException;
import java.util.*;

public class VariantsSearchManager {
    
    public VariantsSearchManager(SearchVersion version) {
        init(version);
    }

    public static void main(String[] args) {
        VariantsSearchManager m = new VariantsSearchManager(SearchVersion.EXPAND_COMMON_WORDS);
        m.expandCommonWords();
    }

    private void init(SearchVersion version) {
        switch (version) {
            case EXPAND_COMMON_WORDS -> {
                expandCommonWords();
            }
            case EXPAND_ABBREVIATIONS -> {
                expandAbbreviations();
            }
        }
    }

    private void expandCommonWords() {
        OsmandRegions osmandRegions = null;
        try {
            osmandRegions = PlatformUtil.getOsmandRegions();
            System.out.println(osmandRegions);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (osmandRegions != null) {
            Set<String> names = new HashSet<>();
            parseRegionNames(osmandRegions.getWorldRegion(), names);
            for (String name : names) {
                CommonWords.addExternalFrequentlyUsed(name);
            }            
        }
        for (String abbr : abbreviations.values()) {
            CommonWords.addExternalCommon(abbr);
        }
    }
    
    private void parseRegionNames(WorldRegion region, Set<String> result) {
        List<WorldRegion> subregions = region.getSubregions();
        for (WorldRegion s : subregions) {
            String t = s.getRegionSearchText();
            if (t != null) {
                String[] ns = t.split(" ");
                for (String n : ns) {
                    if (n.length() > 1) {
                        result.add(n);
                    } else if (!Algorithms.isEmpty(n)) {
                        SearchPhrase.expandConjunction(n);
                    }
                }                
            }            
            parseRegionNames(s, result);
        }
    }
    
    private void expandAbbreviations() {
        for (Map.Entry<String, String> entry : abbreviations.entrySet()) {
            Abbreviations.expandAbbreviations(entry.getKey(), entry.getValue());
        }
    }

    public Map<String, String> abbreviations = new HashMap<>() {{
        put("aly", "Alley");
        put("allee", "Alley");
        put("ally", "Alley");
        put("anx", "Annex");
        put("anex", "Annex");
        put("annx", "Annex");
        put("arc", "Arcade");
        put("av", "Avenue");
        put("aven", "Avenue");
        put("avenu", "Avenue");
        put("avn", "Avenue");
        put("avnue", "Avenue");
        put("byu", "Bayou");
        put("bayoo", "Bayou");
        put("bch", "Beach");
        put("bnd", "Bend");
        put("blf", "Bluff");
        put("bluf", "Bluff");
        put("blfs", "Bluffs");
        put("btm", "Bottom");
        put("bot", "Bottom");
        put("blvd", "Boulevard");
        put("boul", "Boulevard");
        put("br", "Branch");
        put("brnch", "Branch");
        put("brg", "Bridge");
        put("brdge", "Bridge");
        put("brk", "Brook");
        put("brks", "Brooks");
        put("bg", "Burg");
        put("bgs", "Burgs");
        put("byp", "Bypass");
        put("bypa", "Bypass");
        put("cp", "Camp");
        put("cmp", "Camp");
        put("cyn", "Canyon");
        put("canyn", "Canyon");
        put("cpe", "Cape");
        put("cswy", "Causeway");
        put("causwa", "Causeway");
        put("ctr", "Center");
        put("cen", "Center");
        put("ctrs", "Centers");
        put("cir", "Circle");
        put("circ", "Circle");
        put("cirs", "Circles");
        put("clf", "Cliff");
        put("clfs", "Cliffs");
        put("clb", "Club");
        put("cmn", "Common");
        put("cmns", "Commons");
        put("cor", "Corner");
        put("cors", "Corners");
        put("crse", "Course");
        put("ct", "Court");
        put("cts", "Courts");
        put("cv", "Cove");
        put("cvs", "Coves");
        put("crk", "Creek");
        put("cres", "Crescent");
        put("crsent", "Crescent");
        put("crst", "Crest");
        put("xing", "Crossing");
        put("crssng", "Crossing");
        put("xrd", "Crossroad");
        put("xrds", "Crossroads");
        put("curv", "Curve");
        put("dl", "Dale");
        put("dm", "Dam");
        put("dv", "Divide");
        put("div", "Divide");
        put("dr", "Drive");
        put("driv", "Drive");
        put("drs", "Drives");
        put("est", "Estate");
        put("ests", "Estates");
        put("expy", "Expressway");
        put("exp", "Expressway");
        put("ext", "Extension");
        put("extn", "Extension");
        put("exts", "Extensions");
        put("fls", "Falls");
        put("fry", "Ferry");
        put("frry", "Ferry");
        put("fld", "Field");
        put("flds", "Fields");
        put("flt", "Flat");
        put("flts", "Flats");
        put("frd", "Ford");
        put("frds", "Fords");
        put("frst", "Forest");
        put("frg", "Forge");
        put("forg", "Forge");
        put("frgs", "Forges");
        put("frk", "Fork");
        put("frks", "Forks");
        put("ft", "Fort");
        put("frt", "Fort");
        put("fwy", "Freeway");
        put("freewy", "Freeway");
        put("gdn", "Garden");
        put("gardn", "Garden");
        put("gdns", "Gardens");
        put("grdns", "Gardens");
        put("gtwy", "Gateway");
        put("gatewy", "Gateway");
        put("gln", "Glen");
        put("glns", "Glens");
        put("grn", "Green");
        put("grns", "Greens");
        put("grv", "Grove");
        put("grov", "Grove");
        put("grvs", "Groves");
        put("hbr", "Harbor");
        put("harb", "Harbor");
        put("hbrs", "Harbors");
        put("hvn", "Haven");
        put("hts", "Heights");
        put("ht", "Heights");
        put("hwy", "Highway");
        put("highwy", "Highway");
        put("hl", "Hill");
        put("hls", "Hills");
        put("holw", "Hollow");
        put("hllw", "Hollow");
        put("inlt", "Inlet");
        put("is", "Island");
        put("islnd", "Island");
        put("iss", "Islands");
        put("islnds", "Islands");
        put("isle", "Isles");
        put("jct", "Junction");
        put("jction", "Junction");
        put("jcts", "Junctions");
        put("jctns", "Junctions");
        put("ky", "Key");
        put("kys", "Keys");
        put("knl", "Knoll");
        put("knol", "Knoll");
        put("knls", "Knolls");
        put("lk", "Lake");
        put("lks", "Lakes");
        put("lndg", "Landing");
        put("lndng", "Landing");
        put("ln", "Lane");
        put("lgt", "Light");
        put("lgts", "Lights");
        put("lf", "Loaf");
        put("lck", "Lock");
        put("lcks", "Locks");
        put("ldg", "Lodge");
        put("ldge", "Lodge");
        put("loop", "Loops");
        put("lp", "Loops");
        put("mnr", "Manor");
        put("mnrs", "Manors");
        put("mdw", "Meadow");
        put("mdws", "Meadows");
        put("ml", "Mill");
        put("mls", "Mills");
        put("msn", "Mission");
        put("missn", "Mission");
        put("mtwy", "Motorway");
        put("mt", "Mount");
        put("mnt", "Mount");
        put("mtn", "Mountain");
        put("mntain", "Mountain");
        put("mtns", "Mountains");
        put("mntns", "Mountains");
        put("nck", "Neck");
        put("orch", "Orchard");
        put("orchrd", "Orchard");
        put("oval", "Oval");
        put("ovl", "Oval");
        put("opas", "Overpass");
        put("park", "Park");
        put("pk", "Park");
        put("pkwy", "Parkway");
        put("parkwy", "Parkway");
        put("psge", "Passage");
        put("pike", "Pike");
        put("pke", "Pike");
        put("pne", "Pine");
        put("pnes", "Pines");
        put("pl", "Place");
        put("pln", "Plain");
        put("plns", "Plains");
        put("plz", "Plaza");
        put("plza", "Plaza");
        put("pt", "Point");
        put("pts", "Points");
        put("prt", "Port");
        put("prts", "Ports");
        put("pr", "Prairie");
        put("prr", "Prairie");
        put("radl", "Radial");
        put("rad", "Radial");
        put("rnch", "Ranch");
        put("rpd", "Rapid");
        put("rpds", "Rapids");
        put("rdg", "Ridge");
        put("rdge", "Ridge");
        put("rdgs", "Ridges");
        put("riv", "River");
        put("rvr", "River");
        put("rd", "Road");
        put("rds", "Roads");
        put("rte", "Route");
        put("shl", "Shoal");
        put("shls", "Shoals");
        put("shr", "Shore");
        put("shoar", "Shore");
        put("shrs", "Shores");
        put("shoars", "Shores");
        put("skwy", "Skyway");
        put("spg", "Spring");
        put("spng", "Spring");
        put("spgs", "Springs");
        put("spngs", "Springs");
        put("sq", "Square");
        put("sqr", "Square");
        put("sqs", "Squares");
        put("sqrs", "Squares");
        put("sta", "Station");
        put("statn", "Station");
        put("stra", "Stravenue");
        put("strav", "Stravenue");
        put("strm", "Stream");
        put("st", "Street");
        put("strt", "Street");
        put("sts", "Streets");
        put("smt", "Summit");
        put("sumit", "Summit");
        put("ter", "Terrace");
        put("terr", "Terrace");
        put("trwy", "Throughway");
        put("trce", "Trace");
        put("trak", "Track");
        put("trfy", "Trafficway");
        put("trl", "Trail");
        put("trlr", "Trailer");
        put("trlrs", "Trailer");
        put("tunl", "Tunnel");
        put("tunel", "Tunnel");
        put("tpke", "Turnpike");
        put("trnpk", "Turnpike");
        put("upas", "Underpass");
        put("un", "Union");
        put("uns", "Unions");
        put("vly", "Valley");
        put("vally", "Valley");
        put("vlys", "Valleys");
        put("via", "Viaduct");
        put("vdct", "Viaduct");
        put("vw", "View");
        put("vws", "Views");
        put("vlg", "Village");
        put("vill", "Village");
        put("vlgs", "Villages");
        put("vl", "Ville");
        put("vis", "Vista");
        put("vist", "Vista");
        put("walk", "Walk");
        put("wlk", "Walk");
        put("way", "Way");
        put("wy", "Way");
        put("ways", "Ways");
        put("wys", "Ways");
        put("wl", "Well");
        put("wls", "Wells");
    }};
}
