package net.osmand.plus.osmedit.data;

import net.osmand.data.Amenity;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class EditPoiData {
	private Set<TagsChangedListener> mListeners = new HashSet<>();
	public LinkedHashSet<Tag> tags;
	public Amenity amenity;

	public void notifyDatasetChanged(TagsChangedListener listenerToSkip) {
		for (TagsChangedListener listener : mListeners) {
			if (listener != listenerToSkip) listener.onTagsChanged();
		}
	}

	public void addListener(TagsChangedListener listener) {
		mListeners.add(listener);
	}

	public void deleteListener(TagsChangedListener listener) {
		mListeners.remove(listener);
	}

	public interface TagsChangedListener {
		void onTagsChanged();
	}
}