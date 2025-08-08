package net.osmand.plus.plugins.osmedit.fragments;

import static net.osmand.plus.plugins.osmedit.fragments.EditPoiContentAdapter.PAYLOAD_AMENITY;
import static net.osmand.plus.plugins.osmedit.fragments.EditPoiContentAdapter.PAYLOAD_FOCUS_ON_ITEM;
import static net.osmand.plus.plugins.osmedit.fragments.EditPoiContentAdapter.PAYLOAD_NAME;
import static net.osmand.plus.plugins.osmedit.fragments.EditPoiContentAdapter.TYPE_ADD_TAG;
import static net.osmand.plus.plugins.osmedit.fragments.EditPoiContentAdapter.TYPE_DESCRIPTION_ITEM;

import android.content.Context;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiFilter;
import net.osmand.osm.PoiType;
import net.osmand.osm.edit.Entity;
import net.osmand.osm.edit.OSMSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseFullScreenFragment;
import net.osmand.plus.plugins.osmedit.data.EditPoiData;
import net.osmand.plus.plugins.osmedit.dialogs.EditPoiDialogFragment;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AdvancedEditPoiFragment extends BaseFullScreenFragment implements EditPoiDialogFragment.OnFragmentActivatedListener,
		EditPoiDialogFragment.OnSaveButtonClickListener {

	private EditPoiData.TagsChangedListener mTagsChangedListener;

	private String[] allTags;
	private EditPoiContentAdapter contentAdapter;
	private RecyclerView recyclerView;

	public OsmTagsArrayAdapter tagAdapter;
	private ArrayAdapter<String> valueAdapter;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		updateNightMode();
		View view = inflate(R.layout.fragment_edit_poi_advanced_new, container, false);

		recyclerView = view.findViewById(R.id.content_recycler_view);

		tagAdapter = new OsmTagsArrayAdapter(app, R.layout.list_textview);
		valueAdapter = new ArrayAdapter<>(app, R.layout.list_textview);

		Set<String> tagKeys = new HashSet<>();
		Set<String> valueKeys = new HashSet<>();
		fillKeysValues(tagKeys, valueKeys);

		setTagData(tagKeys.toArray(new String[0]));
		setValueData(valueKeys.toArray(new String[0]));
		allTags = tagKeys.toArray(new String[0]);

		EditPoiContentAdapter.EditPoiListener editPoiListener = new EditPoiContentAdapter.EditPoiListener() {
			@Override
			public void onAddNewItem(int position, int buttonType) {
				long id = System.currentTimeMillis();
				contentAdapter.getItems().add(position, new TagItem("", "", id));
				contentAdapter.notifyItemInserted(position);
				recyclerView.postDelayed(() -> {
					recyclerView.scrollToPosition(contentAdapter.getItemCount() - 1);
					contentAdapter.notifyItemChanged(position, PAYLOAD_FOCUS_ON_ITEM);
				}, 300);
			}

			@Override
			public void onDeleteItem(int position) {
				LinearLayoutManager manager = (LinearLayoutManager) recyclerView.getLayoutManager();
				boolean clearFocus = false;
				View focusedView = recyclerView.getFocusedChild();
				LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
				if (focusedView != null && manager != null && layoutManager != null) {
					int firstVisible = layoutManager.findFirstCompletelyVisibleItemPosition();
					int lastVisible = layoutManager.findLastCompletelyVisibleItemPosition();
					clearFocus = !(position >= firstVisible && position <= lastVisible);
				}

				if (clearFocus) {
					AndroidUtils.hideSoftKeyboard(requireActivity(), focusedView);
					focusedView.clearFocus();
				}
			}

			@Override
			public InputFilter[] getLengthLimit() {
				return new InputFilter[0];
			}

			@Override
			public FragmentManager getChildFragmentManager() {
				return AdvancedEditPoiFragment.this.getChildFragmentManager();
			}
		};

		contentAdapter = new EditPoiContentAdapter(requireActivity(), getContentList(),
				valueAdapter, tagAdapter, null, nightMode, getEditPoiFragment(), editPoiListener);
		recyclerView.setLayoutManager(new LinearLayoutManager(app));
		recyclerView.setAdapter(contentAdapter);

		return view;
	}

	public record TagItem(@NonNull String tag, @NonNull String value, long id) {
	}

	public void setValueData(@NonNull String[] values) {
		valueAdapter.clear();
		for (String s : values) {
			valueAdapter.add(s);
		}
		valueAdapter.sort(String.CASE_INSENSITIVE_ORDER);
		valueAdapter.notifyDataSetChanged();
	}

	public void setTagData(@NonNull String[] tags) {
		tagAdapter.clear();
		for (String s : tags) {
			tagAdapter.add(s);
		}
		tagAdapter.sort(String.CASE_INSENSITIVE_ORDER);
		tagAdapter.notifyDataSetChanged();
	}

	private void updateViews() {
		if (contentAdapter != null) {
			contentAdapter.setItems(getContentList());
		}
	}

	private List<Object> getContentList() {
		List<Object> list = new ArrayList<>();
		list.add(TYPE_DESCRIPTION_ITEM);

		EditPoiData editPoiData = getData();

		editPoiData.setIsInEdit(true);
		PoiType pt = editPoiData.getCurrentPoiType();
		String currentPoiTypeKey = "";
		if (pt != null) {
			currentPoiTypeKey = pt.getEditOsmTag();
		}
		for (Map.Entry<String, String> tag : editPoiData.getTagValues().entrySet()) {
			if (tag.getKey().equals(Entity.POI_TYPE_TAG)
					|| tag.getKey().equals(OSMSettings.OSMTagKey.NAME.getValue())
					|| tag.getKey().startsWith(Entity.REMOVE_TAG_PREFIX)
					|| tag.getKey().equals(currentPoiTypeKey)) {
				continue;
			}
			list.add(new TagItem(tag.getKey(), tag.getValue(), System.currentTimeMillis()));
		}

		editPoiData.setIsInEdit(false);
		list.add(TYPE_ADD_TAG);

		return list;
	}

	private void fillKeysValues(@NonNull Set<String> tagKeys, @NonNull Set<String> valueKeys) {
		MapPoiTypes mapPoiTypes = app.getPoiTypes();
		Map<String, PoiType> translatedTypes = getData().getAllTranslatedSubTypes();
		for (AbstractPoiType abstractPoiType : translatedTypes.values()) {
			addPoiToStringSet(abstractPoiType, tagKeys, valueKeys);
		}
		addPoiToStringSet(mapPoiTypes.getOtherMapCategory(), tagKeys, valueKeys);
	}

	@Override
	public void onResume() {
		super.onResume();
		updateName();
		updatePoiType();
		mTagsChangedListener = anyTag -> {
			if (Algorithms.objectEquals(anyTag, OSMSettings.OSMTagKey.NAME.getValue())) {
				updateName();
			}
			if (Algorithms.objectEquals(anyTag, Entity.POI_TYPE_TAG)) {
				updatePoiType();
			}
		};
		getData().addListener(mTagsChangedListener);
	}

	@Override
	public void onPause() {
		super.onPause();
		getData().deleteListener(mTagsChangedListener);
	}

	private EditPoiDialogFragment getEditPoiFragment() {
		return (EditPoiDialogFragment) getParentFragment();
	}

	private EditPoiData getData() {
		return getEditPoiFragment().getEditPoiData();
	}

	@Override
	public void onFragmentActivated() {
		updateViews();
	}

	private void updateName() {
		contentAdapter.notifyItemChanged(contentAdapter.getItems().indexOf(TYPE_DESCRIPTION_ITEM), PAYLOAD_NAME);
	}

	private void updatePoiType() {
		contentAdapter.notifyItemChanged(contentAdapter.getItems().indexOf(TYPE_DESCRIPTION_ITEM), PAYLOAD_AMENITY);
	}

	@Override
	public void onSaveButtonClick() {
		contentAdapter.clearFocus();
	}

	public static void addPoiToStringSet(AbstractPoiType abstractPoiType, Set<String> stringSet,
	                                     Set<String> values) {
		if (abstractPoiType instanceof PoiType) {
			PoiType poiType = (PoiType) abstractPoiType;
			if (poiType.isNotEditableOsm() || poiType.getBaseLangType() != null) {
				return;
			}
			if (poiType.getEditOsmTag() != null &&
					!poiType.getEditOsmTag().equals(OSMSettings.OSMTagKey.NAME.getValue())) {
				String editOsmTag = poiType.getEditOsmTag();
				stringSet.add(editOsmTag);
				if (poiType.getOsmTag2() != null) {
					stringSet.add(poiType.getOsmTag2());
				}
				if (poiType.getEditOsmTag2() != null) {
					stringSet.add(poiType.getEditOsmTag2());
				}
			}
			if (poiType.getEditOsmValue() != null) {
				values.add(poiType.getEditOsmValue());
			}
			if (poiType.getOsmValue2() != null) {
				values.add(poiType.getOsmValue2());
			}
			for (PoiType type : poiType.getPoiAdditionals()) {
				addPoiToStringSet(type, stringSet, values);
			}
		} else if (abstractPoiType instanceof PoiCategory) {
			PoiCategory poiCategory = (PoiCategory) abstractPoiType;
			for (PoiFilter filter : poiCategory.getPoiFilters()) {
				addPoiToStringSet(filter, stringSet, values);
			}
			for (PoiType poiType : poiCategory.getPoiTypes()) {
				addPoiToStringSet(poiType, stringSet, values);
			}
			for (PoiType poiType : poiCategory.getPoiAdditionals()) {
				addPoiToStringSet(poiType, stringSet, values);
			}
		} else if (abstractPoiType instanceof PoiFilter) {
			PoiFilter poiFilter = (PoiFilter) abstractPoiType;
			for (PoiType poiType : poiFilter.getPoiTypes()) {
				addPoiToStringSet(poiType, stringSet, values);
			}
		} else {
			throw new IllegalArgumentException("abstractPoiType can't be instance of class "
					+ abstractPoiType.getClass());
		}
	}

	public class OsmTagsArrayAdapter extends ArrayAdapter implements Filterable {

		private OsmTagsFilter filter;

		public OsmTagsArrayAdapter(Context context, int resource) {
			super(context, resource);
		}

		@Override
		public Filter getFilter() {
			if (filter == null) {
				filter = new OsmTagsFilter();
			}
			return filter;
		}
	}

	private class OsmTagsFilter extends Filter {

		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			FilterResults results = new FilterResults();
			List<String> filteredTags = new ArrayList<String>();
			String query = constraint.toString().trim();
			for (String tag : allTags) {
				if (tag.startsWith(query) || tag.contains(":" + query)) {
					filteredTags.add(tag);
				}
			}
			results.values = filteredTags;
			results.count = filteredTags.size();
			return results;
		}

		@SuppressWarnings("unchecked")
		@Override
		protected void publishResults(CharSequence constraint, FilterResults results) {
			if (results.values != null) {
				String[] filteredHints = ((List<String>) results.values).toArray(new String[0]);
				setTagData(filteredHints);
				tagAdapter.notifyDataSetChanged();
			}
		}
	}
}