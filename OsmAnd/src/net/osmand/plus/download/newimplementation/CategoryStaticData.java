package net.osmand.plus.download.newimplementation;

import android.os.Parcel;
import android.os.Parcelable;

import net.osmand.plus.R;

public class CategoryStaticData implements Parcelable {
	public static final CategoryStaticData WORLD_WIDE_AND_TOPIC =
			new CategoryStaticData(R.string.index_name_other, 0);
	public static final CategoryStaticData NAME_VOICE =
			new CategoryStaticData(R.string.index_name_voice, 1);
	public static final CategoryStaticData TTS_VOICE =
			new CategoryStaticData(R.string.index_name_tts_voice, 2);
	public static final CategoryStaticData WIKI =
			new CategoryStaticData(R.string.index_name_wiki, 10);
	public static final CategoryStaticData OPENMAPS =
			new CategoryStaticData(R.string.index_name_openmaps, 90);
	public static final CategoryStaticData NORTH_AMERICA =
			new CategoryStaticData(R.string.index_name_north_america, 30);
	public static final CategoryStaticData US =
			new CategoryStaticData(R.string.index_name_us, 31, NORTH_AMERICA);
	public static final CategoryStaticData CANADA =
			new CategoryStaticData(R.string.index_name_canada, 32, NORTH_AMERICA);
	public static final CategoryStaticData CENTRAL_AMERICA =
			new CategoryStaticData(R.string.index_name_central_america, 40);
	public static final CategoryStaticData SOUTH_AMERICA =
			new CategoryStaticData(R.string.index_name_south_america, 45);
	public static final CategoryStaticData RUSSIA =
			new CategoryStaticData(R.string.index_name_russia, 25);
	public static final CategoryStaticData EUROPE =
			new CategoryStaticData(R.string.index_name_europe, 15);
	public static final CategoryStaticData GERMANY =
			new CategoryStaticData(R.string.index_name_germany, 16, EUROPE);
	public static final CategoryStaticData FRANCE =
			new CategoryStaticData(R.string.index_name_france, 17, EUROPE);
	public static final CategoryStaticData ITALY =
			new CategoryStaticData(R.string.index_name_italy, 18, EUROPE);
	public static final CategoryStaticData GB =
			new CategoryStaticData(R.string.index_name_gb, 19, EUROPE);
	public static final CategoryStaticData NETHERLANDS =
			new CategoryStaticData(R.string.index_name_netherlands, 20, EUROPE);
	public static final CategoryStaticData AFRICA =
			new CategoryStaticData(R.string.index_name_africa, 80);
	public static final CategoryStaticData ASIA =
			new CategoryStaticData(R.string.index_name_asia, 50);
	public static final CategoryStaticData OCEANIA =
			new CategoryStaticData(R.string.index_name_oceania, 70);
	public static final CategoryStaticData TOURS =
			new CategoryStaticData(R.string.index_tours, 0);

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

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(this.nameId);
		dest.writeInt(this.order);
		dest.writeParcelable(this.parent, flags);
		dest.writeString(this.name);
	}

	protected CategoryStaticData(Parcel in) {
		this.nameId = in.readInt();
		this.order = in.readInt();
		this.parent = in.readParcelable(CategoryStaticData.class.getClassLoader());
		this.name = in.readString();
	}

	public static final Parcelable.Creator<CategoryStaticData> CREATOR = new Parcelable.Creator<CategoryStaticData>() {
		public CategoryStaticData createFromParcel(Parcel source) {
			return new CategoryStaticData(source);
		}

		public CategoryStaticData[] newArray(int size) {
			return new CategoryStaticData[size];
		}
	};
}
