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
import net.osmand.plus.download.DownloadActivityType;
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
import java.util.TreeSet;

public class IndexItemCategoryWithSubcat implements Comparable<IndexItemCategoryWithSubcat>,
		HasName, Parcelable {
	private static final Log LOG = PlatformUtil.getLog(IndexItemCategoryWithSubcat.class);

	public final List<IndexItem> items;
	public final List<IndexItemCategoryWithSubcat> subcats;
	public final CategoryStaticData categoryStaticData;
	public final TreeSet<Integer> types;

	public IndexItemCategoryWithSubcat(CategoryStaticData categoryStaticData) {
		this.categoryStaticData = categoryStaticData;
		items = new ArrayList<>();
		subcats = new ArrayList<>();
		types = new TreeSet<>();
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

			IndexItemCategoryWithSubcat category = cats.get(name);
			if (category == null) {
				category = new IndexItemCategoryWithSubcat(categoryStaticData);
				cats.put(name, category);
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

			IndexItemCategoryWithSubcat region;
			region = cats.get(i.getBasename());
			final String visibleName = i.getVisibleName(ctx, ctx.getRegions());
			i.setName(visibleName);
			if (region == null) {
				final CategoryStaticData regionStaticData = new CategoryStaticData(0, 0);
				regionStaticData.setName(visibleName);
				region = new IndexItemCategoryWithSubcat(regionStaticData);
				cats.put(i.getBasename(), region);
				category.subcats.add(region);
			}
			region.items.add(i);

			if (i.getType() == DownloadActivityType.NORMAL_FILE) {
				region.types.add(R.string.shared_string_map);
			}
			if (i.getType() == DownloadActivityType.WIKIPEDIA_FILE) {
				region.types.add(R.string.shared_string_wikipedia);
			}
			if (i.getType() == DownloadActivityType.ROADS_FILE) {
				region.types.add(R.string.roads);
			}

			final CategoryStaticData parent = category.categoryStaticData.getParent();
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


	@Override
	public String toString() {
		return "IndexItemCategoryWithSubcat{" +
				"items=" + items +
				", subcats=" + subcats +
				", categoryStaticData=" + categoryStaticData +
				'}';
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeList(this.items);
		dest.writeList(this.subcats);
		dest.writeParcelable(this.categoryStaticData, 0);
		dest.writeSerializable(this.types);
	}

	protected IndexItemCategoryWithSubcat(Parcel in) {
		this.items = new ArrayList<IndexItem>();
		in.readList(this.items, List.class.getClassLoader());
		this.subcats = new ArrayList<IndexItemCategoryWithSubcat>();
		in.readList(this.subcats, List.class.getClassLoader());
		this.categoryStaticData = in.readParcelable(CategoryStaticData.class.getClassLoader());
		this.types = (TreeSet<Integer>) in.readSerializable();
	}

	public static final Parcelable.Creator<IndexItemCategoryWithSubcat> CREATOR = new Parcelable.Creator<IndexItemCategoryWithSubcat>() {
		public IndexItemCategoryWithSubcat createFromParcel(Parcel source) {
			return new IndexItemCategoryWithSubcat(source);
		}

		public IndexItemCategoryWithSubcat[] newArray(int size) {
			return new IndexItemCategoryWithSubcat[size];
		}
	};
}
