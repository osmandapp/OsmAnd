package net.osmand.plus.plugins.osmedit.fragments;

import static net.osmand.plus.plugins.osmedit.dialogs.EditPoiDialogFragment.AMENITY_TEXT_LENGTH;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiFilter;
import net.osmand.osm.PoiType;
import net.osmand.osm.edit.Entity;
import net.osmand.osm.edit.OSMSettings;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.plugins.osmedit.data.EditPoiData;
import net.osmand.plus.plugins.osmedit.data.EditPoiData.TagsChangedListener;
import net.osmand.plus.plugins.osmedit.dialogs.EditPoiDialogFragment;
import net.osmand.plus.plugins.osmedit.dialogs.EditPoiDialogFragment.OnFragmentActivatedListener;
import net.osmand.plus.plugins.osmedit.dialogs.EditPoiDialogFragment.OnSaveButtonClickListener;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.widgets.OsmandTextFieldBoxes;
import net.osmand.plus.widgets.tools.SimpleTextWatcher;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import studio.carbonylgroup.textfieldboxes.ExtendedEditText;

public class AdvancedEditPoiFragment extends BaseOsmAndFragment implements OnFragmentActivatedListener,
		OnSaveButtonClickListener {

	private static final Log LOG = PlatformUtil.getLog(AdvancedEditPoiFragment.class);

	private TagAdapterLinearLayoutHack mAdapter;
	private TagsChangedListener mTagsChangedListener;

	private TextView nameTextView;
	private TextView amenityTagTextView;
	private TextView amenityTextView;
	private EditText currentTagEditText;

	private Drawable deleteDrawable;
	private String[] allTags;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		updateNightMode();
		View view = themedInflater.inflate(R.layout.fragment_edit_poi_advanced, container, false);

		deleteDrawable = app.getUIUtilities().getIcon(R.drawable.ic_action_remove_dark, !nightMode);

		nameTextView = view.findViewById(R.id.nameTextView);
		amenityTagTextView = view.findViewById(R.id.amenityTagTextView);
		amenityTextView = view.findViewById(R.id.amenityTextView);
		LinearLayout editTagsLineaLayout = view.findViewById(R.id.editTagsList);

		Set<String> tagKeys = new HashSet<>();
		Set<String> valueKeys = new HashSet<>();
		fillKeysValues(tagKeys, valueKeys);

		mAdapter = new TagAdapterLinearLayoutHack(editTagsLineaLayout, getData());
		mAdapter.setTagData(tagKeys.toArray(new String[0]));
		mAdapter.setValueData(valueKeys.toArray(new String[0]));
		allTags = tagKeys.toArray(new String[0]);

		View addTagButton = view.findViewById(R.id.addTagButton);
		addTagButton.setOnClickListener(v -> {
			mAdapter.addTagView("", "", true);
			scrollToBottom(view);
		});

		return view;
	}

	private void fillKeysValues(@NonNull Set<String> tagKeys, @NonNull Set<String> valueKeys) {
		MapPoiTypes mapPoiTypes = app.getPoiTypes();
		Map<String, PoiType> translatedTypes = getData().getAllTranslatedSubTypes();
		for (AbstractPoiType abstractPoiType : translatedTypes.values()) {
			addPoiToStringSet(abstractPoiType, tagKeys, valueKeys);
		}
		addPoiToStringSet(mapPoiTypes.getOtherMapCategory(), tagKeys, valueKeys);
	}

	private void scrollToBottom(@NonNull View view) {
		view.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
				EditPoiDialogFragment parentFragment = getEditPoiFragment();
				parentFragment.smoothScrollToBottom();
			}
		});
	}

	@Override
	public void onResume() {
		super.onResume();
		mAdapter.updateViews();
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
		if (mAdapter != null) {
			mAdapter.updateViews();
		}
	}

	private void updateName() {
		nameTextView.setText(getData().getTag(OSMSettings.OSMTagKey.NAME.getValue()));
	}

	private void updatePoiType() {
		PoiType pt = getData().getPoiTypeDefined();
		if (pt != null) {
			amenityTagTextView.setText(pt.getEditOsmTag());
			amenityTextView.setText(pt.getEditOsmValue());
		} else {
			PoiCategory category = getData().getPoiCategory();
			if (category != null) {
				amenityTagTextView.setText(category.getDefaultTag());
			} else {
				amenityTagTextView.setText(R.string.tag_poi_amenity);
			}
			amenityTextView.setText(getData().getPoiTypeString());
		}
	}

	@Override
	public void onSaveButtonClick() {
		if (currentTagEditText != null) {
			currentTagEditText.clearFocus();
		}
	}

	public class TagAdapterLinearLayoutHack {

		private final LinearLayout linearLayout;
		private final EditPoiData editPoiData;
		public final OsmTagsArrayAdapter tagAdapter;
		private final ArrayAdapter<String> valueAdapter;

		public TagAdapterLinearLayoutHack(LinearLayout linearLayout,
		                                  EditPoiData editPoiData) {
			this.linearLayout = linearLayout;
			this.editPoiData = editPoiData;

			tagAdapter = new OsmTagsArrayAdapter(linearLayout.getContext(), R.layout.list_textview);
			valueAdapter = new ArrayAdapter<>(linearLayout.getContext(), R.layout.list_textview);
		}

		public void updateViews() {
			linearLayout.removeAllViews();
			editPoiData.setIsInEdit(true);
			PoiType pt = editPoiData.getCurrentPoiType();
			String currentPoiTypeKey = "";
			if (pt != null) {
				currentPoiTypeKey = pt.getEditOsmTag();
			}
			for (Entry<String, String> tag : editPoiData.getTagValues().entrySet()) {
				if (tag.getKey().equals(Entity.POI_TYPE_TAG)
						|| tag.getKey().equals(OSMSettings.OSMTagKey.NAME.getValue())
						|| tag.getKey().startsWith(Entity.REMOVE_TAG_PREFIX)
						|| tag.getKey().equals(currentPoiTypeKey)) {
					continue;
				}
				addTagView(tag.getKey(), tag.getValue(), false);
			}
			if (linearLayout.getChildCount() != 0) {
				View v = linearLayout.getChildAt(0);
				View tagEditText = v.findViewById(R.id.tagEditText);
				if (tagEditText != null) {
					tagEditText.post(() -> showKeyboard(tagEditText));
				}
			}
			if (editPoiData.hasEmptyValue() && linearLayout.findViewById(R.id.valueEditText) != null) {
				linearLayout.findViewById(R.id.valueEditText).requestFocus();
			}
			editPoiData.setIsInEdit(false);
		}

		public void addTagView(@NonNull String tag, @NonNull String value, boolean isNew) {
			View convertView = LayoutInflater.from(linearLayout.getContext())
					.inflate(R.layout.list_item_poi_tag, null, false);

			OsmandTextFieldBoxes tagFB = convertView.findViewById(R.id.tag_fb);
			tagFB.setClearButton(deleteDrawable);
			tagFB.post(tagFB::hideClearButton);

			OsmandTextFieldBoxes valueFB = convertView.findViewById(R.id.value_fb);
			valueFB.setClearButton(deleteDrawable);
			valueFB.post(valueFB::hideClearButton);

			ExtendedEditText tagEditText = convertView.findViewById(R.id.tagEditText);
			AutoCompleteTextView valueEditText = convertView.findViewById(R.id.valueEditText);

			tagEditText.setText(tag);
			tagEditText.setAdapter(tagAdapter);
			tagEditText.setThreshold(1);
			if (isNew) {
				showKeyboard(tagEditText);
			}

			String[] previousTag = {tag};
			tagEditText.setOnFocusChangeListener((v, hasFocus) -> {
				if (!hasFocus) {
					tagFB.hideClearButton();
					if (!editPoiData.isInEdit()) {
						String s = tagEditText.getText().toString();
						if (!previousTag[0].equals(s)) {
							editPoiData.removeTag(previousTag[0]);
							editPoiData.putTag(s, valueEditText.getText().toString());
							previousTag[0] = s;
						}
					}
				} else {
					tagFB.showClearButton();
					currentTagEditText = tagEditText;
					tagAdapter.getFilter().filter(tagEditText.getText());
				}
			});

			valueEditText.setFilters(new InputFilter[] {new InputFilter.LengthFilter(AMENITY_TEXT_LENGTH)});
			valueEditText.setText(value);
			valueEditText.setAdapter(valueAdapter);
			valueEditText.setThreshold(3);
			valueEditText.addTextChangedListener(new SimpleTextWatcher() {
				@Override
				public void afterTextChanged(Editable s) {
					if (!editPoiData.isInEdit()) {
						editPoiData.putTag(tagEditText.getText().toString(), s.toString());
					}
				}
			});

			valueEditText.setOnFocusChangeListener((v, hasFocus) -> {
				if (hasFocus) {
					valueFB.showClearButton();
					valueAdapter.getFilter().filter(valueEditText.getText());
				} else {
					valueFB.hideClearButton();
				}
			});

			View deleteButton = convertView.findViewById(R.id.delete_button);
			deleteButton.setOnClickListener(v -> {
				linearLayout.removeView(convertView);
				editPoiData.removeTag(tagEditText.getText().toString());
			});

			linearLayout.addView(convertView);
		}

		private void showKeyboard(@NonNull View view) {
			view.requestFocus();
			Activity activity = getActivity();
			if (activity != null) {
				AndroidUtils.softKeyboardDelayed(activity, view);
			}
		}

		public void setTagData(@NonNull String[] tags) {
			tagAdapter.clear();
			for (String s : tags) {
				tagAdapter.add(s);
			}
			tagAdapter.sort(String.CASE_INSENSITIVE_ORDER);
			tagAdapter.notifyDataSetChanged();
		}

		public void setValueData(@NonNull String[] values) {
			valueAdapter.clear();
			for (String s : values) {
				valueAdapter.add(s);
			}
			valueAdapter.sort(String.CASE_INSENSITIVE_ORDER);
			valueAdapter.notifyDataSetChanged();
		}
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

	private class OsmTagsArrayAdapter extends ArrayAdapter implements Filterable {

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
				mAdapter.setTagData(filteredHints);
				mAdapter.tagAdapter.notifyDataSetChanged();
			}
		}
	}
}