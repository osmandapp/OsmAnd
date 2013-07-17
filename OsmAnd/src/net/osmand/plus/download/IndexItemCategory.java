package net.osmand.plus.download;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.osmand.plus.ClientContext;
import net.osmand.plus.R;
import net.osmand.plus.Version;

public class IndexItemCategory implements Comparable<IndexItemCategory> {
	public final String name;
	public final List<IndexItem> items = new ArrayList<IndexItem>();
	private final int order;

	public IndexItemCategory(String name, int order) {
		this.name = name;
		this.order = order;
	}

	@Override
	public int compareTo(IndexItemCategory another) {
		return order < another.order ? -1 : 1;
	}

	public static List<IndexItemCategory> categorizeIndexItems(ClientContext ctx, Collection<IndexItem> indexItems) {
		boolean skipWiki = Version.isFreeVersion(ctx);
		final Map<String, IndexItemCategory> cats = new TreeMap<String, IndexItemCategory>();
		for (IndexItem i : indexItems) {
			int nameId = R.string.index_name_other;
			int order = 0;
			String lc = i.getFileName().toLowerCase();
			if (lc.endsWith(".voice.zip")) {
				nameId = R.string.index_name_voice;
				order = 1;
			} else if (lc.contains(".ttsvoice.zip")) {
				nameId = R.string.index_name_tts_voice;
				order = 2;
			} else if (lc.startsWith("us") || 
					(lc.contains("united states") && lc.startsWith("north-america")) ) {
				nameId = R.string.index_name_us;
				order = 31;
			} else if (lc.contains("openmaps")) {
				nameId = R.string.index_name_openmaps;
				order = 90;
			} else if (lc.contains("northamerica") || lc.contains("north-america")) {
				nameId = R.string.index_name_north_america;
				order = 30;
			} else if (lc.contains("centralamerica") || lc.contains("central-america")
					|| lc.contains("caribbean")) {
				nameId = R.string.index_name_central_america;
				order = 40;
			} else if (lc.contains("southamerica") || lc.contains("south-america")) {
				nameId = R.string.index_name_south_america;
				order = 45;
			} else if (lc.startsWith("france_")) {
				nameId = R.string.index_name_france;
				order = 17;
			} else if ( lc.contains("germany")) {
				nameId = R.string.index_name_germany;
				order = 16;
			} else if (lc.contains("europe")) {
				nameId = R.string.index_name_europe;
				order = 15;
			} else if (lc.contains("russia")) {
				nameId = R.string.index_name_russia;
				order = 18;
			} else if (lc.contains("africa") && !lc.contains("_wiki_")) {
				nameId = R.string.index_name_africa;
				order = 80;
			} else if (lc.contains("_asia")|| lc.startsWith("asia")) {
				nameId = R.string.index_name_asia;
				order = 50;
			} else if (lc.contains("oceania") || lc.contains("australia")) {
				nameId = R.string.index_name_oceania;
				order = 70;
			} else if (lc.contains("_wiki_")) {
				if(skipWiki) {
					continue;
				}
				nameId = R.string.index_name_wiki;
				order = 10;
			}

			String name = ctx.getString(nameId);
			if (!cats.containsKey(name)) {
				cats.put(name, new IndexItemCategory(name, order));
			}
			cats.get(name).items.add(i);
		}
		ArrayList<IndexItemCategory> r = new ArrayList<IndexItemCategory>(cats.values());
		Collections.sort(r);
		return r;
	}
}
