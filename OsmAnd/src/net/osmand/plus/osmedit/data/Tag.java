package net.osmand.plus.osmedit.data;

import java.io.Serializable;

public class Tag implements Serializable {
	public String tag;
	public String value;

	public Tag(String tag, String value) {
		this.tag = tag;
		this.value = value;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Tag tag1 = (Tag) o;
		return tag.equals(tag1.tag);
	}

	@Override
	public int hashCode() {
		return tag.hashCode();
	}

	@Override
	public String toString() {
		return "Tag{" +
				"tag='" + tag + '\'' +
				", value='" + value + '\'' +
				'}';
	}
}