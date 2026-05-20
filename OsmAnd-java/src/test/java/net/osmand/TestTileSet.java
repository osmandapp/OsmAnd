package net.osmand;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXUtilities;
import net.osmand.gpx.GPXUtilities.Track;
import net.osmand.gpx.GPXUtilities.TrkSegment;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.util.MapUtils;


public class TestTileSet {

	
	public static void main(String[] args) {
		
		File ls = new File("gpx/Export_03-01-25/tracks");
		
		int ZOOM = 16;
		int targetLen = 0;
		Set<String> alltiles = new TreeSet<String>();
		LinkedList<File> lst = new LinkedList<File>();
		lst.add(ls);
		while(!lst.isEmpty()) {
			File l = lst.pop();
			if (l == null) {
				continue;
			}
			if (l.isDirectory()) {
				for (File f : l.listFiles()) {
					lst.add(f);
				}
				continue;
			}
			if (!l.getName().endsWith(".gpx")) {
				continue;
			}
			
			List<List<WptPt>> segments = new ArrayList<>();
			GPXFile gpxFile = GPXUtilities.loadGPXFile(l);
			for (GPXUtilities.Track track : gpxFile.tracks) {
				if (track.generalTrack) continue;
				for (GPXUtilities.TrkSegment segment : track.segments) {
					if (segment.generalSegment) continue;
					segments.add(segment.points);
				}
			}
			
			Set<String> tiles = new TreeSet<String>();
			for(List<WptPt> pnts : segments) {
				for(WptPt p : pnts) {
					String lni = MapUtils.createShortLinkString(p.lat, p.lon, ZOOM);
					targetLen = lni.length();
					tiles.add(lni);
				}
			}
			alltiles.addAll(tiles);
			StringBuilder serialized = new StringBuilder();
			serialize(buildTrie(tiles), serialized);
			System.out.printf("%s, %d tiles, %d len\n", l.getName(),tiles.size(), 
					serialized.length());
		}
		System.out.printf("No compression %,d tiles - %,d bytes\n", alltiles.size(), alltiles.toString().length());
		TrieNode root = buildTrie(alltiles);
        
		StringBuilder serialized = new StringBuilder();
		serialize(root, serialized);
		System.out.println(serialized);
		System.out.printf("Compressed %,d tiles - %,d bytes\n", alltiles.size(), serialized.length());

		List<String> recovered = new ArrayList<>();
		deserialize(serialized.toString(), "", recovered);
        Collections.sort(recovered);
		System.out.println(alltiles.size() + " == " + recovered.size());
	}
	static class TrieNode {
        Map<Character, TrieNode> children = new TreeMap<>();
    }

    static TrieNode buildTrie(Collection<String> strings) {
        TrieNode root = new TrieNode();
        for (String s : strings) {
            TrieNode curr = root;
            for (char c : s.toCharArray()) {
                curr = curr.children.computeIfAbsent(c, k -> new TrieNode());
            }
        }
        return root;
    }

    static void serialize(TrieNode node, StringBuilder sb) {
        for (Map.Entry<Character, TrieNode> entry : node.children.entrySet()) {
            sb.append(entry.getKey());
            if (!entry.getValue().children.isEmpty()) {
                sb.append("^");
                serialize(entry.getValue(), sb);
                sb.append(":");
            }
        }
    }

    static void deserialize(String s, String prefix, List<String> res) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (i + 1 < s.length() && s.charAt(i + 1) == '^') {
                int start = i + 2;
                int balance = 1;
                int j = start;
                while (j < s.length() && balance > 0) {
                    if (s.charAt(j) == '^') balance++;
                    if (s.charAt(j) == ':') balance--;
                    j++;
                }
                deserialize(s.substring(start, j - 1), prefix + c, res);
                i = j - 1; 
            } else {
                res.add(prefix + c);
            }
        }
    }
}
