package net.osmand.plus.views.controls;

import android.content.Context;
import android.support.annotation.NonNull;
import android.widget.ArrayAdapter;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class StableArrayAdapter extends ArrayAdapter<Object> {

	final int INVALID_ID = -1;

	List<Object> objects;
	List<Object> activeObjects;
	HashMap<Object, Integer> mIdMap = new HashMap<>();

	public StableArrayAdapter(Context context, int textViewResourceId, int titleId,
							  List<Object> objects, List<Object> activeObjects) {
		super(context, textViewResourceId, titleId, objects);
		updateObjects(objects, activeObjects);
	}

	public List<Object> getObjects() {
		return objects;
	}

	public List<Object> getActiveObjects() {
		return activeObjects;
	}

	public void updateObjects(List<Object> objects, List<Object> activeObjects) {
		this.objects = objects;
		this.activeObjects = activeObjects;

		HashMap<Object, Integer> idMap = new HashMap<>();
		for (int i = 0; i < objects.size(); ++i) {
			idMap.put(objects.get(i), i);
		}
		mIdMap = idMap;
	}

	@Override
	public Object getItem(int position) {
		return objects.get(position);
	}

	@Override
	public long getItemId(int position) {
		if (position < 0 || position >= mIdMap.size()) {
			return INVALID_ID;
		}
		Object item = getItem(position);
		if (mIdMap.containsKey(item)) {
			return mIdMap.get(item);
		} else {
			return INVALID_ID;
		}
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}
}