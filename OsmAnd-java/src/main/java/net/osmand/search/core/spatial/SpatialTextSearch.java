package net.osmand.search.core.spatial;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import com.google.protobuf.ByteString;

import gnu.trove.map.hash.TLongObjectHashMap;
import net.osmand.binary.BinaryMapAddressReaderAdapter.CityBlocks;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.CommonWords;
import net.osmand.binary.OsmandOdb.AddressNameIndexDataAtom;
import net.osmand.binary.OsmandOdb.OsmAndPoiNameIndexDataAtom;
import net.osmand.map.OsmandRegions;
import net.osmand.search.core.HashQuadTree;
import net.osmand.util.MapUtils;
import net.osmand.util.SearchAlgorithms;



// FIXME merge uniq references for POI to make id 

// TODO Lazy load tokens from full name index !
// TODO global cache - Read common words for files
// TODO Read all top poi categories for files
// TODO read buildings
// TODO duplicate words
// TODO collator +replace last dot as incomplete
// TODO sort tokens by actual frequency
// TODO Special split by -
// special cases
// 1. Abbrevations
// 2. Street intersection match
// -------------
// - Sugggestion-correction
// - Progress / cancel
//////////////SEARCH ALGORITHM //////////////
// 1. Split words
// 2.Reinit caches
// 3. Sort words & meta information for words
// 3.1 Calculate poi categories for words & combinations!
// 3.2 Calculate common & frequent numbers based on files
// 3.3 Assign Common & frequent labels from Global file

// 4. Read 
// 4.1 Incomplete words? assign prefix ?
// 4.2 Read Per word List<AddressNameIndexDataAtom>, List<OsmAndPoiNameIndexDataAtom>
// 4.3 Read & Cache for non-frequent common words

// After search operations - Expand POI Type filters for results
// 5.1 Run rare words (by counts & labels)
// 5.2 Run with frequent words
// 5.3 Expand poi categories

// Search categories
// Phase I - only rare + words replaced abbrrevations
// Phase II - all words + replaced abbrevations
// Phase III - all words + replaced abbrevations
public class SpatialTextSearch {
	
	
	static class NameIndexAtom {
		String name;
		AddressNameIndexDataAtom addr;
		OsmAndPoiNameIndexDataAtom poi;
		long id;
		
		int[] bbox31; // if exists [xleft, yleft, xright, yright]
		long bboxTileId; // encodes zoom, tileX, tileY
		int bboxTileZoom;
		int x16, y16;

		NameIndexAtom(String name, AddressNameIndexDataAtom addr, OsmAndPoiNameIndexDataAtom poi, long id) {
			this.name = name;
			this.addr = addr;
			this.poi = poi;
			this.id = id;
			calculateBbox(addr, poi);
		}
		
		
		private void calculateBbox(AddressNameIndexDataAtom addr, OsmAndPoiNameIndexDataAtom poi) {
			if (addr != null && addr.getXy16Count() >= 1) {
				int xy16 = addr.getXy16(0);
				this.x16 = (xy16 >>> 16);
				this.y16 = (xy16 & ((1 << 16) - 1));
				ByteString bbox = addr.getBbox();
				bboxTileZoom = 15;
				bboxTileId = HashQuadTree.encodeTileId(bboxTileZoom, x16 / 2, y16 / 2);
				if (bbox != null && addr.hasBbox()) {
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
			} else if (poi != null) {
				this.x16 = poi.getX();
				this.y16 = poi.getY();
				bboxTileZoom = 16;
				bboxTileId = HashQuadTree.encodeTileId(bboxTileZoom, x16, y16);
			}			
		}
		
		public String getType() {
			String type = "";
			if (addr != null) {
				type = CityBlocks.getByType(addr.getType()).toString();
			} else if (poi != null) {
				type = "POI";
			}
			return type;
		}
		
		
		@Override
		public final String toString() {
			return String.format("%s (%.4f, %.4f) ", getType() + " " + name + " " + (id % 0xffff), 
					MapUtils.get31LatitudeY(y16 << 15),
					MapUtils.get31LongitudeX(x16 << 15));
		}
	};
	
	
	static class SpatialSearchToken {
		int originalOrder = 0;
		int sortedOrder = 0;
		boolean incomplete;
		String originalWord;
		String word;
		List<NameIndexAtom> atoms = new ArrayList<>();
		TLongObjectHashMap<NameIndexAtom> index = new TLongObjectHashMap<>();
		HashQuadTree<NameIndexAtom> quadTree = new HashQuadTree<>(16);
		
		
		public SpatialSearchToken(String w, String original, int order) {
			originalWord = original;
			word = w;
			this.originalOrder = order;
		}
		
		@Override
		public String toString() {
			return String.format("%d. %s - %d atoms", originalOrder, originalWord, atoms.size());
		}
		

		void addAtom(String name, NameIndexAtom atom) {
			if (match(name)) {
				index.put(atom.id, atom);
				atoms.add(atom);
				quadTree.put(atom.bboxTileZoom, atom.bboxTileId, atom);
			}
		}

		private boolean match(String name) {
			// TODO collator from space
			if (word.endsWith(".")) {
				
				return name.toLowerCase().startsWith(word.substring(0, word.length() - 1));
			}
			return word.equalsIgnoreCase(name);
		}
	}
	
	

	private List<SpatialSearchToken> splitWords(String input) {
		List<String> owords =new ArrayList<String>();
		List<String> words = SearchAlgorithms.splitAndNormalizeSearchQuery(input, owords);
		List<SpatialSearchToken> tokens = new ArrayList<>();
		for (int order = 0; order < words.size(); order++) {
			String w = words.get(order);
			tokens.add(new SpatialSearchToken(w, owords.get(order), order));
		}
		return tokens;
	}
	
	private void sortTokens(List<SpatialSearchToken> tokens) {
		Collections.sort(tokens, new Comparator<SpatialSearchToken>() {
			@Override
			public int compare(SpatialSearchToken o1, SpatialSearchToken o2) {
				int c1 = CommonWords.getCommonSearch(o1.word);
				int c2 = CommonWords.getCommonSearch(o2.word);
				if(c1 != c2) {
					return Integer.compare(c1, c2);
				}
				c1 = o1.atoms.size();
				c2 = o2.atoms.size();
				if(c1 != c2) {
					return Integer.compare(c1, c2);
				}
				return o1.word.compareTo(o2.word);
			}
			
		});
		for(int i = 0; i < tokens.size(); i++) {
			tokens.get(i).sortedOrder = i;
		}
	}
	

	private List<SpatialSearchResultsList> findObjCombinations(List<SpatialSearchToken> tokens) {
		LinkedList<SpatialSearchResultsList> candidates = new LinkedList<>();
		candidates.add(new SpatialSearchResultsList());
		List<SpatialSearchResultsList> result = new ArrayList<>();
//		System.out.println("TOKENS " + tokens);
		while (!candidates.isEmpty()) {
			SpatialSearchResultsList parent = candidates.removeFirst();
			if (parent.getCombinations() > 0) {
				result.add(parent);
			}
			for (SpatialSearchToken token : tokens) {
				if (parent.getTokenCount() == 0 || token.sortedOrder < parent.getFirstToken().sortedOrder) {
//					System.out.println("ITERATION Token [ " + token + " ] + " + parent);
					SpatialSearchResultsList next = new SpatialSearchResultsList(token, parent);
					if (parent.getTokenCount() == 0) {
						next.addResult(token.atoms);
					} else {
						for (int i = 0; i < parent.tileIds.size(); i++) {
							long tileId = parent.tileIds.get(i);
							int zoom = parent.tileZooms.get(i);
							final int indx = i;
							token.quadTree.forEachMatch(zoom, tileId, t -> {
								next.addResult(parent, indx, t);
							});
						}
					}
					// reverse search quad tree 
					final SpatialSearchResultsList p = parent;
					for (final NameIndexAtom a : token.atoms) {
						parent.quadTree.forEachMatchHigherZoom(a.bboxTileZoom, a.bboxTileId, indxs -> {
							for (int indx : indxs) {
								next.addResult(p, indx, a);
							}
						});
					}
					
					candidates.add(next);
				}
			}
		}
		return result;
		
	}


	public void searchTest(String input, SpatialSearchContext ctx) throws IOException {
		// 1. prepare tokens
		List<SpatialSearchToken> tokens = splitWords(input);
		// 2. read atoms
		for (SpatialSearchToken t : tokens) {
			ctx.readAtoms(t);
		}
		// 3. sort tokens 
		sortTokens(tokens);
		// 4. find combinations
		ctx.stats.computeTime -= System.nanoTime();
		List<SpatialSearchResultsList> combinations = findObjCombinations(tokens);
		ctx.stats.computeTime += System.nanoTime();
		
		Collections.sort(combinations);
		if (combinations.size() > 0) {
			SpatialSearchResultsList resList = combinations.get(0);
			System.out.println("--------");
			System.out.println("Main: " + resList);
			for (SpatialSearchResult r : resList.getResult()) {
				System.out.println(r);
			}
			System.out.println("--------");
		}
		
		System.out.println("\nTokens: " + tokens);
		System.out.println("All Results: ");
		for (SpatialSearchResultsList s : combinations) {
			if (s.getTokenCount() >= 2) {
				System.out.println("  " + s.toString(false));
			}
		}
		
		ctx.stats.finish();
		System.out.println(ctx.stats);
		System.out.println();
	}

	
	
	public static void mainTest(String[] subArgsArray) throws FileNotFoundException, IOException {
		long t = System.nanoTime();
		String query = subArgsArray[0];
		List<BinaryMapIndexReader> ls = new ArrayList<BinaryMapIndexReader>();
		for(int i = 1; i < subArgsArray.length; i++) {
			File fl = new File(subArgsArray[i]);
			if (fl.isFile()) {
				if (i == 1) {
					initFile(ls, new File(fl.getParentFile(), OsmandRegions.REGIONS_OCBF));
				}
				initFile(ls, fl);
			} else {
				for (File f : fl.listFiles()) {
					initFile(ls, f);
				}
			}
		}
		System.out.println(String.format("Index files %.1f ms", (System.nanoTime() - t) / 1e6));
		SpatialTextSearch a = new SpatialTextSearch();
		SpatialSearchContext searchContext = new SpatialSearchContext(ls);
		a.searchTest(query, searchContext);
	}

	private static void initFile(List<BinaryMapIndexReader> ls, File f) throws IOException, FileNotFoundException {
		if (f.exists() && (f.getName().endsWith(".obf") || f.getName().equals(OsmandRegions.REGIONS_OCBF))) {
			BinaryMapIndexReader bir = new BinaryMapIndexReader(new RandomAccessFile(f, "r"), f);
			ls.add(bir);
		}
	}
	
	public static void main(String[] args) throws IOException {
		File folder = new File("/Users/victorshcherb/osmand/maps/");
		String pattern = "Germany_baden";
		String query = "Berlin hauptstrasse";
		query = "Kelterstraße Kernen im Remstal";
		query = "Germany Kelter. Kernen im Remstal";
		
		pattern = "Us_";
		query = "Salt Lake City Pennsylvania Street";
		
		pattern = "Liechtenstein_europe.obf";
		query = "Vaduz Lettstrasse";
		query = "Vaduz ";
//		query = "Jugendheim Malbun";
		
//		query = "USA Salt Lake City Pennsylvania Street 41";
		
//		pattern = "Ukraine_kyi/v-ci";
//		query = "бровари Сільпо";
//		query = "kyiv";
//		query = "пузата хата mcdonal.";
//		query = "нова пошта 53"; // TODO number?
		long t = System.nanoTime();
		
		List<BinaryMapIndexReader> ls = new ArrayList<BinaryMapIndexReader>();
		for (File f : folder.listFiles()) {
			if (f.getName().startsWith(pattern) || f.getName().equals(OsmandRegions.REGIONS_OCBF)) {
				initFile(ls, f);
			}
		}
		System.out.println(String.format("Index files %.1f ms", (System.nanoTime() - t) / 1e6));
		SpatialSearchContext searchContext = new SpatialSearchContext(ls);
		SpatialTextSearch a = new SpatialTextSearch();
		a.searchTest(query, searchContext);
		
		searchContext = new SpatialSearchContext(ls, searchContext.cache);
		a.searchTest(query, searchContext);
	}
	
	

	
}