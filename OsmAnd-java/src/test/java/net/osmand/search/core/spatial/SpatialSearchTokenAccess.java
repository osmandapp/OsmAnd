package net.osmand.search.core.spatial;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import gnu.trove.list.array.TIntArrayList;
import net.osmand.CollatorStringMatcher;
import net.osmand.binary.NameIndexReader.NameIndexReaderMatcher;
import net.osmand.data.MapObject;
import net.osmand.search.core.HashQuadTree;
import net.osmand.search.core.HashSkipTileQuadTree;
import net.osmand.search.core.spatial.SpatialSearchContext.SpatialSearchStats;
import net.osmand.search.core.spatial.SpatialSearchToken.NameIndexAtom;
import net.osmand.search.core.spatial.SpatialSearchToken.NameIndexAtomXY;

/**
 * The package-private part of {@link SpatialSearchToken} for the compat test of its copy in
 * OsmAnd-shared, {@code net.osmand.shared.compat.SpatialSearchTokenCompatTest}.
 */
public final class SpatialSearchTokenAccess {

	private SpatialSearchTokenAccess() {
	}

	public static String word(SpatialSearchToken t) {
		return t.word;
	}

	public static String originalWord(SpatialSearchToken t) {
		return t.originalWord;
	}

	public static String wordAligned(SpatialSearchToken t) {
		return t.wordAligned;
	}

	public static String wordNoDot(SpatialSearchToken t) {
		return t.wordNoDot;
	}

	public static boolean incomplete(SpatialSearchToken t) {
		return t.incomplete;
	}

	public static boolean categoryMatchMode(SpatialSearchToken t) {
		return t.categoryMatchMode;
	}

	public static Set<String> bldWordSplit(SpatialSearchToken t) {
		return t.bldWordSplit;
	}

	public static CollatorStringMatcher noDotCollatorMain(SpatialSearchToken t) {
		return t.noDotCollatorMain;
	}

	public static CollatorStringMatcher noHyphenCollatorMain(SpatialSearchToken t) {
		return t.noHyphenCollatorMain;
	}

	public static void setBroad(SpatialSearchToken t, boolean broad) {
		t.broad = broad;
	}

	public static void setNumberNamedByOther(SpatialSearchToken t, boolean numberNamedByOther) {
		t.numberNamedByOther = numberNamedByOther;
	}

	public static List<NameIndexAtom> atoms(SpatialSearchToken t) {
		return t.atoms;
	}

	public static int indexSize(SpatialSearchToken t) {
		return t.index.size();
	}

	public static int indexByOsmIdsSize(SpatialSearchToken t) {
		return t.indexByOsmIds.size();
	}

	public static int[] deletedAtoms(SpatialSearchToken t) {
		return t.getDeletedAtoms().toArray();
	}

	public static Set<String> poiCategoryKeysToAutocomplete(SpatialSearchToken t) {
		return t.poiCategoryKeysToAutocomplete;
	}

	public static Set<Integer> poiCategoryIds(SpatialSearchToken t) {
		return t.poiCategoryIds;
	}

	public static HashQuadTree<Integer> quadTree(SpatialSearchToken t) {
		return t.quadTree;
	}

	public static HashSkipTileQuadTree<Integer> quadTreeSkip(SpatialSearchToken t) {
		return t.quadTreeSkip;
	}

	public static NameIndexReaderMatcher prefixMatcher(SpatialSearchToken t, SpatialSearchStats stats, String locale) {
		return locale == null ? t.getPrefixMatcher(stats) : t.getPrefixMatcher(stats, locale);
	}

	public static boolean matchName(SpatialSearchToken t, String name, TIntArrayList poiTypes, String locale, int type) {
		return t.matchName(name, poiTypes, locale, type);
	}

	public static String[] matchSplitName(SpatialSearchToken t, String name) {
		return t.matchSplitName(name);
	}

	public static String ruleOwner(SpatialSearchToken t, int type) {
		return t.ruleOwner(type).name();
	}

	public static boolean addAtom(SpatialSearchToken t, NameIndexAtom atom) {
		return t.addAtom(atom);
	}

	public static void removeAtom(SpatialSearchToken t, NameIndexAtom atom) {
		t.removeAtom(atom);
	}

	public static void enlargeBbox(SpatialSearchToken t, NameIndexAtom atom, double mult) {
		t.enlargeBbox(atom, mult);
	}

	public static void addPoiCategoryMatch(SpatialSearchToken t, int id) {
		t.addPoiCategoryMatch(id);
	}

	public static NameIndexAtom atom(String name, int type, long id, long pid, MapObject obj, boolean cityAsStreet,
			int otherWordsCnt, int otherFoundCnt, NameIndexAtomXY coords, int nearbyRadius, int buildingInd,
			String locale, boolean ruleSpelling, int distinctFoundCnt, TIntArrayList poiTypes) {
		NameIndexAtom atom = new NameIndexAtom(name, type, id, pid, obj, cityAsStreet, otherWordsCnt, otherFoundCnt, coords,
				nearbyRadius, buildingInd);
		atom.locale = locale;
		atom.ruleSpelling = ruleSpelling;
		atom.distinctFoundCnt = distinctFoundCnt;
		atom.poiTypes = poiTypes;
		return atom;
	}

	public static NameIndexAtom categoryAtom(String name, long id, int total) {
		return new NameIndexAtom(name, id, total);
	}

	public static NameIndexAtom copy(NameIndexAtom atom) {
		return new NameIndexAtom(atom);
	}

	public static void setBldObject(NameIndexAtom atom, MapObject bld) {
		atom.bldObject = bld;
	}

	public static String describe(NameIndexAtom a) {
		return a.name + "/" + a.type + "/" + a.id + "/" + a.parentid + "/" + a.cityAsStreet + "/" + a.otherWordsCnt + "/"
				+ a.otherFoundCnt + "/" + a.distinctFoundCnt + "/" + a.indexInToken + "/" + a.nearbyRadius + "/"
				+ a.buildingOrRefInd + "/" + a.locale + "/" + a.ruleSpelling + "/"
				+ (a.poiTypes == null ? "null" : Arrays.toString(a.poiTypes.toArray())) + "/" + a.elo;
	}

	public static int indexInToken(NameIndexAtom atom) {
		return atom.indexInToken;
	}

	public static NameIndexAtom atomToken(SpatialSearchToken t, NameIndexAtom atom) {
		return t.getAtomToken(atom);
	}

	/** A place known only by its tile, as an atom of a name index without a bbox has it. */
	public static NameIndexAtomXY tilePlace(int zoom, long tileId) {
		NameIndexAtomXY xy = new NameIndexAtomXY(null, null, null);
		xy.bbox31 = null;
		xy.bboxTileZoom = zoom;
		xy.bboxTileId = tileId;
		return xy;
	}

	public static NameIndexAtomXY coords(NameIndexAtom atom) {
		return atom.coords;
	}

	public static int[] bbox31(NameIndexAtomXY xy) {
		return xy.bbox31;
	}

	public static long bboxTileId(NameIndexAtomXY xy) {
		return xy.bboxTileId;
	}

	public static int bboxTileZoom(NameIndexAtomXY xy) {
		return xy.bboxTileZoom;
	}

	public static int x16(NameIndexAtomXY xy) {
		return xy.x16;
	}

	public static int y16(NameIndexAtomXY xy) {
		return xy.y16;
	}
}
