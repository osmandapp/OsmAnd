package net.osmand.binary;

import net.osmand.search.core.SearchPhrase;
import net.osmand.util.SearchAlgorithms;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;


public class Abbreviations {

    private Abbreviations() {
    }

    private static final Map<String, String> abbreviations = new HashMap<>();
    // 2nd version search abbrevations for spatial search
    private static final Map<String, String> searchAbbreviations = new HashMap<>();
    // set of words to check for buidlings
    private static final Map<String, String> buildingAbbreviations = new HashMap<>();
	private static final Set<String> conjunctions = new TreeSet<>();
	
	private static final Set<String> commonSkipOtherCnt = new TreeSet<>();

	private static void addDirectionWord(String key, String full) {
		abbreviations.put(key, full);
		commonSkipOtherCnt.add(key);
		commonSkipOtherCnt.add(full.toLowerCase());
	}

	private static void addStreetStatus(String key, String full) {
		abbreviations.put(key, full);
		commonSkipOtherCnt.add(key);
		commonSkipOtherCnt.add(full.toLowerCase());
	}

	private static void addSearchAbbreviation(String full, String... keys) {
		for (String key : keys) {
			searchAbbreviations.put(key, full);
		}
	}

	private static void addConjunction(String key) {
		conjunctions.add(key);
		commonSkipOtherCnt.add(key);
	}

	static {
		// articles
		addConjunction("the");
		addConjunction("de");
		addConjunction("du");
		addConjunction("der");
		addConjunction("den");
		addConjunction("die");
		addConjunction("das");
		addConjunction("la");
		addConjunction("le");
		addConjunction("el");
		addConjunction("il");
		addConjunction("of");

		// and
		addConjunction("and");
		addConjunction("und");
		addConjunction("en");
		addConjunction("et");
		addConjunction("y");
		addConjunction("и");
		
		

		// direction
		addDirectionWord("e", "East");
		addDirectionWord("w", "West");
		addDirectionWord("s", "South");
		addDirectionWord("n", "North");
		addDirectionWord("sw", "Southwest");
		addDirectionWord("se", "Southeast");
		addDirectionWord("nw", "Northwest");
		addDirectionWord("ne", "Northeast");

		// street status
		addStreetStatus("ln", "Lane");
		addStreetStatus("dr", "Drive");
		addStreetStatus("rd", "Road");
		addStreetStatus("av", "Avenue");
		addStreetStatus("st", "Street"); // 2 values could be saint
		addStreetStatus("hwy", "Highway");
		addStreetStatus("blvd", "Boulevard");
	}

	static {
		searchAbbreviations.putAll(abbreviations);
		searchAbbreviations.put("ave", "Avenue"); // extra
		searchAbbreviations.put("st", "Street Saint"); // 2 values could be saint
		// duplicates - synonyms and not abbrevations actually
		searchAbbreviations.put("о", "Остров");
		searchAbbreviations.put("остров", "о.");
		searchAbbreviations.put("1st", "First");
		searchAbbreviations.put("2nd", "Second");
		searchAbbreviations.put("3rd", "Third");
		searchAbbreviations.put("first", "1st");
		searchAbbreviations.put("second", "2nd");
		searchAbbreviations.put("third", "3rd");
		searchAbbreviations.put("fourth", "4th");
		searchAbbreviations.put("fifth", "5th");
		searchAbbreviations.put("sixth", "6th");
		searchAbbreviations.put("seventh", "7th");

		// Unambiguous English address variants used by Nominatim.
		addSearchAbbreviation("access", "accs");
		addSearchAbbreviation("alley", "aly");
		addSearchAbbreviation("alleyway", "alwy");
		addSearchAbbreviation("anex annex", "anx");
		addSearchAbbreviation("beach", "bch");
		addSearchAbbreviation("bend", "bnd");
		addSearchAbbreviation("bluff", "blf");
		addSearchAbbreviation("bluffs", "blfs");
		addSearchAbbreviation("bridge", "bdge", "brdg", "brg");
		addSearchAbbreviation("bypass", "bps", "byp", "bypa");
		addSearchAbbreviation("byway", "bywy");
		addSearchAbbreviation("canyon", "cyn");
		addSearchAbbreviation("causeway", "caus", "cswy", "cway");
		addSearchAbbreviation("center centre", "cen", "ctr");
		addSearchAbbreviation("circle", "cir");
		addSearchAbbreviation("cliff", "clf");
		addSearchAbbreviation("cliffs", "clfs");
		addSearchAbbreviation("corner", "cnr", "crn");
		addSearchAbbreviation("course", "crse");
		addSearchAbbreviation("court", "crt");
		addSearchAbbreviation("creek", "crk");
		addSearchAbbreviation("crescent", "cres");
		addSearchAbbreviation("crossing", "crsg", "csg", "xing");
		addSearchAbbreviation("crossroad", "crd", "xrd");
		addSearchAbbreviation("crossroads", "xrds");
		addSearchAbbreviation("cul-de-sac", "cds", "csac");
		addSearchAbbreviation("dale", "dle");
		addSearchAbbreviation("drive", "drv");
		addSearchAbbreviation("driveway", "drwy", "dvwy", "dwy");
		addSearchAbbreviation("entrance", "ent");
		addSearchAbbreviation("esplanade", "espl");
		addSearchAbbreviation("expressway", "expy", "expwy", "xway");
		addSearchAbbreviation("extension", "ext", "exten", "exts");
		addSearchAbbreviation("falls", "fls");
		addSearchAbbreviation("field", "fld");
		addSearchAbbreviation("fields", "flds");
		addSearchAbbreviation("ford", "frd");
		addSearchAbbreviation("fords", "frds");
		addSearchAbbreviation("forest", "frst");
		addSearchAbbreviation("forge", "frg");
		addSearchAbbreviation("forges", "frgs");
		addSearchAbbreviation("fork", "frk");
		addSearchAbbreviation("forks", "frks");
		addSearchAbbreviation("freeway", "frwy", "fwy");
		addSearchAbbreviation("gardens", "gdns");
		addSearchAbbreviation("gate gates", "gte");
		addSearchAbbreviation("gateway", "gtwy");
		addSearchAbbreviation("glade", "gld", "glde");
		addSearchAbbreviation("glen", "gln");
		addSearchAbbreviation("green", "grn");
		addSearchAbbreviation("greens", "grns");
		addSearchAbbreviation("grove", "gro", "grv");
		addSearchAbbreviation("groves", "grvs");
		addSearchAbbreviation("gully", "gly");
		addSearchAbbreviation("harbor harbour", "hbr", "harbr");
		addSearchAbbreviation("harbors", "hbrs");
		addSearchAbbreviation("haven", "hvn");
		addSearchAbbreviation("heights", "hgts", "hts");
		addSearchAbbreviation("hill", "hl");
		addSearchAbbreviation("hills", "hls");
		addSearchAbbreviation("hollow", "holw");
		addSearchAbbreviation("inlet", "inlt");
		addSearchAbbreviation("junction", "jct", "jctn", "jnc");
		addSearchAbbreviation("junctions", "jcts");
		addSearchAbbreviation("knoll", "knl");
		addSearchAbbreviation("knolls", "knls");
		addSearchAbbreviation("lagoon", "lgn");
		addSearchAbbreviation("lakes", "lks");
		addSearchAbbreviation("laneway", "lnwy");
		addSearchAbbreviation("light", "lgt");
		addSearchAbbreviation("manor", "mnr");
		addSearchAbbreviation("manors", "mnrs");
		addSearchAbbreviation("meadow", "mdw");
		addSearchAbbreviation("meadows", "mdws");
		addSearchAbbreviation("motorway", "mtwy", "mwy");
		addSearchAbbreviation("mountain", "mtn");
		addSearchAbbreviation("mountains", "mtns");
		addSearchAbbreviation("orchard", "orch");
		addSearchAbbreviation("parade", "pde");
		addSearchAbbreviation("parkway", "pkwy", "pky");
		addSearchAbbreviation("passage", "psge");
		addSearchAbbreviation("pathway", "phwy", "ptway");
		addSearchAbbreviation("pines", "pnes");
		addSearchAbbreviation("plain", "pln");
		addSearchAbbreviation("plains", "plns");
		addSearchAbbreviation("plaza", "plz", "plza");
		addSearchAbbreviation("point", "pnt");
		addSearchAbbreviation("port", "prt");
		addSearchAbbreviation("promenade", "prm");
		addSearchAbbreviation("ranch", "rnch");
		addSearchAbbreviation("range", "rge", "rnge");
		addSearchAbbreviation("rapids", "rpds");
		addSearchAbbreviation("ridge", "rdg", "rdge");
		addSearchAbbreviation("ridges", "rdgs");
		addSearchAbbreviation("river", "riv", "rvr");
		addSearchAbbreviation("roads", "rds");
		addSearchAbbreviation("roadway", "rdwy");
		addSearchAbbreviation("route", "rte");
		addSearchAbbreviation("shoal", "shl");
		addSearchAbbreviation("shore", "shr");
		addSearchAbbreviation("shores", "shrs");
		addSearchAbbreviation("skyway", "skwy");
		addSearchAbbreviation("slope", "slpe");
		addSearchAbbreviation("sound", "snd");
		addSearchAbbreviation("spring", "spg");
		addSearchAbbreviation("springs", "spgs");
		addSearchAbbreviation("square", "sq");
		addSearchAbbreviation("station", "sta", "stn");
		addSearchAbbreviation("stream", "strm");
		addSearchAbbreviation("summit", "smt");
		addSearchAbbreviation("terrace", "tce", "terr");
		addSearchAbbreviation("track", "trak", "trk");
		addSearchAbbreviation("trail", "trl");
		addSearchAbbreviation("tunnel", "tun", "tunl");
		addSearchAbbreviation("turnpike", "tpk", "tpke");
		addSearchAbbreviation("underpass", "upas", "ups");
		addSearchAbbreviation("valley", "vly");
		addSearchAbbreviation("view", "vw");
		addSearchAbbreviation("views", "vws");
		addSearchAbbreviation("village", "vlg", "villge");
		addSearchAbbreviation("vista", "vis", "vst", "vsta");
		addSearchAbbreviation("walk", "wlk");
		addSearchAbbreviation("walkway", "wkwy", "wky");
		addSearchAbbreviation("waters", "wtr");
		addSearchAbbreviation("way", "wy");
		addSearchAbbreviation("wharf", "whrf");
		addSearchAbbreviation("wynd", "wyn");

		// Country-specific single-word address components.
		addSearchAbbreviation("barangay", "bgy", "brgy");
		addSearchAbbreviation("compound", "cmpd", "cpd");
		addSearchAbbreviation("department", "dept");
		addSearchAbbreviation("province", "prov");
		addSearchAbbreviation("subdivision", "subd", "subdiv");
		addSearchAbbreviation("bazaar", "bazar", "bzr");
		addSearchAbbreviation("cantonment", "cant", "cantt");
		addSearchAbbreviation("district", "dist", "distt");
		addSearchAbbreviation("government", "govt");
		addSearchAbbreviation("markaz", "mkz");
		addSearchAbbreviation("sector", "sec", "sect", "sectr");
		addSearchAbbreviation("service", "ser", "serv");
	}

	// common housenumber additions
	static {
		// french
		buildingAbbreviations.put("bis", "Bis");
		buildingAbbreviations.put("ter", "Ter");
		buildingAbbreviations.put("quater", "Quater");
		// american
		buildingAbbreviations.put("bldg", "Building");
		buildingAbbreviations.put("bldgs", "buildings");
		buildingAbbreviations.put("bldngs", "buildings");
		buildingAbbreviations.put("ste", "Suite");
		buildingAbbreviations.put("unt", "Unit");
		buildingAbbreviations.put("apt", "Apartment");
		buildingAbbreviations.put("apts", "apartments");
		buildingAbbreviations.put("blk", "block");
		buildingAbbreviations.put("flts", "flats");
		buildingAbbreviations.put("hse", "house");
		buildingAbbreviations.put("fl", "Floor");
		buildingAbbreviations.put("flr", "Floor");
		buildingAbbreviations.put("bsmt", "Basement");
	}

	public static boolean likelyPartOfRef(String word, Set<String> wordSplit) {
		int limit = 2;
		int letters = SearchAlgorithms.letters(word, limit + 1);
		if (letters < limit || (letters == limit && SearchAlgorithms.startsWithDigit(word))) {
			return true;
		}
		for (String s : wordSplit) {
			letters = SearchAlgorithms.letters(s, limit + 1);
			if (!(letters < limit || (letters == limit && SearchAlgorithms.startsWithDigit(s)))) {
				return false;
			}
		}
		return true;
	}
	
	// search v-2
	public static boolean likelyPartOfBuilding(String word, Set<String> wordSplit) {
		boolean bldNum = (SearchAlgorithms.isNumber2Letters(word) || word.length() == 1
				|| buildingAbbreviations.containsKey(word));
		if (bldNum) {
			return true;
		}
		if (wordSplit != null) {
			// recursion for 2bis
			for (String w : wordSplit) {
				boolean likely = likelyPartOfBuilding(w, null);
				if (!likely) {
					return false;
				}
			}
			return true;
		}
		return false;
	}
    
    
    // search-v2
    public static Map<String, String> getSearchabbreviations() {
		return searchAbbreviations;
	}
    
    // search-v2
 	public static boolean isCommonSkipOtherCnt(String lowerCase) {
 		return commonSkipOtherCnt.contains(lowerCase);
 	}

    // Indexing data
    public static String replaceAll(String phrase) {
        String[] words = phrase.split(SearchPhrase.DELIMITER);
        StringBuilder r = new StringBuilder();
        boolean changed = false;
        for (String w : words) {
            if (r.length() > 0) {
                r.append(SearchPhrase.DELIMITER);
            }
            String abbrRes = abbreviations.get(w.toLowerCase());
            if (abbrRes == null) {
                r.append(w);
            } else {
                changed = true;
                r.append(abbrRes);
            }
        }
        return changed ? r.toString() : phrase;
    }
    
	// search-v1
    public static Map<String, String> getAbbreviations() {
		return abbreviations;
	}

	// search v-1
    public static String replace(String word) {
        String value = abbreviations.get(word.toLowerCase());
        return value != null ? value : word;
    }
    
    // search-v1
	public static boolean isConjunction(String lowerCase) {
		return conjunctions.contains(lowerCase);
	}
	
    
}
