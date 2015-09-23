package net.osmand.plus.download.newimplementation;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import net.osmand.Collator;
import net.osmand.OsmAndCollator;
import net.osmand.PlatformUtil;
import net.osmand.map.OsmandRegions;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.helpers.HasName;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class IndexItemCategoryWithSubcat implements Comparable<IndexItemCategoryWithSubcat>,
		Parcelable, HasName {
	private static final Log LOG = PlatformUtil.getLog(IndexItemCategoryWithSubcat.class);

	public final List<IndexItem> items;
	public final List<IndexItemCategoryWithSubcat> subcats;
	public final CategoryStaticData categoryStaticData;

	public IndexItemCategoryWithSubcat(CategoryStaticData categoryStaticData) {
		this.categoryStaticData = categoryStaticData;
		items = new ArrayList<>();
		subcats = new ArrayList<>();
	}

	@Override
	public int compareTo(@NonNull IndexItemCategoryWithSubcat another) {
		final int BEFORE = -1;
		final int EQUAL = 0;
		final int AFTER = 1;

		if (this == another) return EQUAL;

		if (this.categoryStaticData.getOrder() < another.categoryStaticData.getOrder())
			return BEFORE;
		if (this.categoryStaticData.getOrder() > another.categoryStaticData.getOrder())
			return AFTER;

		return EQUAL;
	}

	public static List<IndexItemCategoryWithSubcat> categorizeIndexItems(final OsmandApplication ctx,
																		 Collection<IndexItem> indexItems) {
		boolean skipWiki = Version.isFreeVersion(ctx);
		final Map<String, IndexItemCategoryWithSubcat> cats = new TreeMap<>();
		ArrayList<IndexItemCategoryWithSubcat> mainList = new ArrayList<>();
		for (IndexItem i : indexItems) {
			String lowerCase = i.getFileName().toLowerCase();
			CategoryStaticData categoryStaticData;
			if (lowerCase.endsWith(".voice.zip")) {
				categoryStaticData = CategoryStaticData.NAME_VOICE;
			} else if (lowerCase.contains(".ttsvoice.zip")) {
				categoryStaticData = CategoryStaticData.TTS_VOICE;
			} else if (lowerCase.contains("_wiki_")) {
				if (skipWiki) {
					continue;
				}
				categoryStaticData = CategoryStaticData.WIKI;
			} else if (lowerCase.startsWith("us") ||
					(lowerCase.contains("united states") && lowerCase.startsWith("north-america"))) {
				categoryStaticData = CategoryStaticData.US;
			} else if (lowerCase.startsWith("canada")) {
				categoryStaticData = CategoryStaticData.CANADA;
			} else if (lowerCase.contains("openmaps")) {
				categoryStaticData = CategoryStaticData.OPENMAPS;
			} else if (lowerCase.contains("northamerica") || lowerCase.contains("north-america")) {
				categoryStaticData = CategoryStaticData.NORTH_AMERICA;
			} else if (lowerCase.contains("centralamerica") || lowerCase.contains("central-america")
					|| lowerCase.contains("caribbean")) {
				categoryStaticData = CategoryStaticData.CENTRAL_AMERICA;
			} else if (lowerCase.contains("southamerica") || lowerCase.contains("south-america")) {
				categoryStaticData = CategoryStaticData.SOUTH_AMERICA;
			} else if (lowerCase.contains("germany")) {
				categoryStaticData = CategoryStaticData.GERMANY;
			} else if (lowerCase.startsWith("france_")) {
				categoryStaticData = CategoryStaticData.FRANCE;
			} else if (lowerCase.startsWith("italy_")) {
				categoryStaticData = CategoryStaticData.ITALY;
			} else if (lowerCase.startsWith("gb_") || lowerCase.startsWith("british")) {
				categoryStaticData = CategoryStaticData.GB;
			} else if (lowerCase.contains("netherlands")) {
				categoryStaticData = CategoryStaticData.NETHERLANDS;
			} else if (lowerCase.contains("russia")) {
				categoryStaticData = CategoryStaticData.RUSSIA;
			} else if (lowerCase.contains("europe")) {
				categoryStaticData = CategoryStaticData.EUROPE;
			} else if (lowerCase.contains("africa") && !lowerCase.contains("_wiki_")) {
				categoryStaticData = CategoryStaticData.AFRICA;
			} else if (lowerCase.contains("_asia") || lowerCase.startsWith("asia")) {
				categoryStaticData = CategoryStaticData.ASIA;
			} else if (lowerCase.contains("oceania") || lowerCase.contains("australia")) {
				categoryStaticData = CategoryStaticData.OCEANIA;
			} else if (lowerCase.contains("tour")) {
				categoryStaticData = CategoryStaticData.TOURS;
			} else {
				categoryStaticData = CategoryStaticData.WORLD_WIDE_AND_TOPIC;
			}
			String name = ctx.getString(categoryStaticData.getNameId());
			categoryStaticData.setName(name);
			final IndexItemCategoryWithSubcat category =
					new IndexItemCategoryWithSubcat(categoryStaticData);
			if (!cats.containsKey(name)) {
				cats.put(name, category);
				LOG.debug("category=" + category.categoryStaticData);
				if (!categoryStaticData.hasParent()) {
					mainList.add(category);
				} else {
					final CategoryStaticData parent = categoryStaticData.getParent();
					if (cats.get(parent.getName()) == null) {
						cats.put(parent.getName(), new IndexItemCategoryWithSubcat(parent));
					} else {
						cats.get(parent.getName()).subcats.add(category);
					}
				}
			}
			cats.get(name).items.add(i);
		}
		final Collator collator = OsmAndCollator.primaryCollator();
		for (IndexItemCategoryWithSubcat ct : mainList) {
			final OsmandRegions osmandRegions = ctx.getResourceManager().getOsmandRegions();
			Collections.sort(ct.items, new Comparator<IndexItem>() {
				@Override
				public int compare(IndexItem lhs, IndexItem rhs) {
					return collator.compare(lhs.getVisibleName(ctx, osmandRegions),
							rhs.getVisibleName(ctx, osmandRegions));
				}
			});
		}
		Collections.sort(mainList);
		return mainList;
	}

	@Override
	public String getName() {
		return categoryStaticData.getName();
	}

	public enum CategoryStaticData {
		WORLD_WIDE_AND_TOPIC(R.string.index_name_other, 0),
		NAME_VOICE(R.string.index_name_voice, 1),
		TTS_VOICE(R.string.index_name_tts_voice, 2),
		WIKI(R.string.index_name_wiki, 10),
		OPENMAPS(R.string.index_name_openmaps, 90),
		NORTH_AMERICA(R.string.index_name_north_america, 30),
		US(R.string.index_name_us, 31, NORTH_AMERICA),
		CANADA(R.string.index_name_canada, 32, NORTH_AMERICA),
		CENTRAL_AMERICA(R.string.index_name_central_america, 40),
		SOUTH_AMERICA(R.string.index_name_south_america, 45),
		RUSSIA(R.string.index_name_russia, 25),
		EUROPE(R.string.index_name_europe, 15),
		GERMANY(R.string.index_name_germany, 16, EUROPE),
		FRANCE(R.string.index_name_france, 17, EUROPE),
		ITALY(R.string.index_name_italy, 18, EUROPE),
		GB(R.string.index_name_gb, 19, EUROPE),
		NETHERLANDS(R.string.index_name_netherlands, 20, EUROPE),
		AFRICA(R.string.index_name_africa, 80),
		ASIA(R.string.index_name_asia, 50),
		OCEANIA(R.string.index_name_oceania, 70),
		TOURS(R.string.index_tours, 0);

		private final int nameId;
		private final int order;
		private final CategoryStaticData parent;
		private String name;

		CategoryStaticData(int nameId, int order) {
			this.nameId = nameId;
			this.order = order;
			parent = null;
		}

		CategoryStaticData(int nameId, int order, CategoryStaticData parent) {
			this.nameId = nameId;
			this.order = order;
			this.parent = parent;
		}

		public int getNameId() {
			return nameId;
		}

		public int getOrder() {
			return order;
		}

		public CategoryStaticData getParent() {
			return parent;
		}

		public boolean hasParent() {
			return parent != null;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return "CategoryStaticData{" +
					"nameId=" + nameId +
					", order=" + order +
					", parent=" + parent +
					", name='" + name + '\'' +
					'}';
		}
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeList(this.items);
		dest.writeList(this.subcats);
		dest.writeInt(this.categoryStaticData.ordinal());
		dest.writeString(this.categoryStaticData.getName());
	}

	protected IndexItemCategoryWithSubcat(Parcel in) {
		this.items = new ArrayList<IndexItem>();
		in.readList(this.items, List.class.getClassLoader());
		this.subcats = new ArrayList<IndexItemCategoryWithSubcat>();
		in.readList(this.subcats, List.class.getClassLoader());
		int tmpCategoryStaticData = in.readInt();
		this.categoryStaticData = CategoryStaticData.values()[tmpCategoryStaticData];
		this.categoryStaticData.setName(in.readString());
	}

	public static final Parcelable.Creator<IndexItemCategoryWithSubcat> CREATOR =
			new Parcelable.Creator<IndexItemCategoryWithSubcat>() {
		public IndexItemCategoryWithSubcat createFromParcel(Parcel source) {
			return new IndexItemCategoryWithSubcat(source);
		}

		public IndexItemCategoryWithSubcat[] newArray(int size) {
			return new IndexItemCategoryWithSubcat[size];
		}
	};
}
