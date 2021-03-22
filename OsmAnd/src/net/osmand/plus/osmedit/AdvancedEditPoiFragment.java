package net.osmand.plus.osmedit;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
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
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static net.osmand.plus.osmedit.EditPoiDialogFragment.AMENITY_TEXT_LENGTH;

public class AdvancedEditPoiFragment extends BaseOsmAndFragment
		implements EditPoiDialogFragment.OnFragmentActivatedListener,
		EditPoiDialogFragment.OnSaveButtonClickListener {
	private static final String TAG = "AdvancedEditPoiFragment";
	private static final Log LOG = PlatformUtil.getLog(AdvancedEditPoiFragment.class);

	private TagAdapterLinearLayoutHack mAdapter;
	private EditPoiData.TagsChangedListener mTagsChangedListener;
	private Drawable deleteDrawable;
	private TextView nameTextView;
	private TextView amenityTagTextView;
	private TextView amenityTextView;
	private EditText currentTagEditText;

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		int themeRes = requireMyApplication().getSettings().isLightActionBar() ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme;
		Context themedContext = new ContextThemeWrapper(getContext(), themeRes);
		final View view = inflater.cloneInContext(themedContext).inflate(R.layout.fragment_edit_poi_advanced, container, false);

		OsmandApplication app = requireMyApplication();
		deleteDrawable = app.getUIUtilities().getIcon(R.drawable.ic_action_remove_dark, app.getSettings().isLightContent());
		nameTextView = (TextView) view.findViewById(R.id.nameTextView);
		amenityTagTextView = (TextView) view.findViewById(R.id.amenityTagTextView);
		amenityTextView = (TextView) view.findViewById(R.id.amenityTextView);
		LinearLayout editTagsLineaLayout =
				(LinearLayout) view.findViewById(R.id.editTagsList);

		final MapPoiTypes mapPoiTypes = app.getPoiTypes();
		mAdapter = new TagAdapterLinearLayoutHack(editTagsLineaLayout, getData());
		// It is possible to not restart initialization every time, and probably move initialization to appInit
		Map<String, PoiType> translatedTypes = getData().getAllTranslatedSubTypes();
		HashSet<String> tagKeys = new HashSet<>();
		HashSet<String> valueKeys = new HashSet<>();
		for (AbstractPoiType abstractPoiType : translatedTypes.values()) {
			addPoiToStringSet(abstractPoiType, tagKeys, valueKeys);
		}
		addPoiToStringSet(mapPoiTypes.getOtherMapCategory(), tagKeys, valueKeys);
		mAdapter.setTagData(tagKeys.toArray(new String[0]));
		mAdapter.setValueData(valueKeys.toArray(new String[0]));
		Button addTagButton = (Button) view.findViewById(R.id.addTagButton);
		addTagButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mAdapter.addTagView("", "");
			}
		});
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		mAdapter.updateViews();
		updateName();
		updatePoiType();
		mTagsChangedListener = new EditPoiData.TagsChangedListener() {
			@Override
			public void onTagsChanged(String anyTag) {
				if (Algorithms.objectEquals(anyTag, OSMSettings.OSMTagKey.NAME.getValue())) {
					updateName();
				}
				if (Algorithms.objectEquals(anyTag, Entity.POI_TYPE_TAG)) {
					updatePoiType();
				}
			}
		};
		getData().addListener(mTagsChangedListener);
	}

	@Override
	public void onPause() {
		super.onPause();
		getData().deleteListener(mTagsChangedListener);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	private EditPoiDialogFragment getEditPoiFragment() {
		return (EditPoiDialogFragment) getParentFragment();
	}

	private EditPoiData getData() {
		return getEditPoiFragment().getEditPoiData();
	}

	@Override
	public void onFragmentActivated() {
		if(mAdapter != null) {
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
		private final ArrayAdapter<String> tagAdapter;
		private final ArrayAdapter<String> valueAdapter;

		public TagAdapterLinearLayoutHack(LinearLayout linearLayout,
										  EditPoiData editPoiData) {
			this.linearLayout = linearLayout;
			this.editPoiData = editPoiData;

			tagAdapter = new ArrayAdapter<>(linearLayout.getContext(), R.layout.list_textview);
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
						|| tag.getKey().equals(currentPoiTypeKey))
					continue;
				addTagView(tag.getKey(), tag.getValue());
			}
			if (editPoiData.hasEmptyValue() && linearLayout.findViewById(R.id.valueEditText) != null) {
				linearLayout.findViewById(R.id.valueEditText).requestFocus();
			}
			editPoiData.setIsInEdit(false);
		}

		public void addTagView(String tg, String vl) {
			View convertView = LayoutInflater.from(linearLayout.getContext())
					.inflate(R.layout.poi_tag_list_item, null, false);
			final AutoCompleteTextView tagEditText = convertView.findViewById(R.id.tagEditText);
			ImageButton deleteItemImageButton =
					(ImageButton) convertView.findViewById(R.id.deleteItemImageButton);
			deleteItemImageButton.setImageDrawable(deleteDrawable);
			final String[] previousTag = new String[]{tg};
			deleteItemImageButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					linearLayout.removeView((View) v.getParent());
					editPoiData.removeTag(tagEditText.getText().toString());
				}
			});
			final AutoCompleteTextView valueEditText = convertView.findViewById(R.id.valueEditText);
			valueEditText.setFilters(new InputFilter[]{
					new InputFilter.LengthFilter(AMENITY_TEXT_LENGTH)
			});
			tagEditText.setText(tg);
			tagEditText.setAdapter(tagAdapter);
			tagEditText.setThreshold(1);
			tagEditText.setOnFocusChangeListener(new OnFocusChangeListener() {
				@Override
				public void onFocusChange(View v, boolean hasFocus) {
					if (!hasFocus) {
						if (!editPoiData.isInEdit()) {
							String s = tagEditText.getText().toString();
							if (!previousTag[0].equals(s)) {
								editPoiData.removeTag(previousTag[0]);
								editPoiData.putTag(s, valueEditText.getText().toString());
								previousTag[0] = s;
							}
						}
					} else {
						currentTagEditText = tagEditText;
						tagAdapter.getFilter().filter(tagEditText.getText());
					}
				}
			 });

			valueEditText.setText(vl);
			valueEditText.setAdapter(valueAdapter);
			valueEditText.setThreshold(3);
			valueEditText.addTextChangedListener(new TextWatcher() {
				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				}

				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
				}

				@Override
				public void afterTextChanged(Editable s) {
					if (!editPoiData.isInEdit()) {
						editPoiData.putTag(tagEditText.getText().toString(), s.toString());
					}
				}
			});

			initAutocompleteTextView(valueEditText, valueAdapter);

			linearLayout.addView(convertView);
		}

		public void setTagData(String[] tags) {
			tagAdapter.clear();
			for (String s : tags) {
				tagAdapter.add(s);
			}
			tagAdapter.sort(String.CASE_INSENSITIVE_ORDER);
			tagAdapter.notifyDataSetChanged();
		}

		public void setValueData(String[] values) {
			valueAdapter.clear();
			for (String s : values) {
				valueAdapter.add(s);
			}
			valueAdapter.sort(String.CASE_INSENSITIVE_ORDER);
			valueAdapter.notifyDataSetChanged();
		}
	}

	private static void initAutocompleteTextView(final AutoCompleteTextView textView,
												 final ArrayAdapter<String> adapter) {
		
		textView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					adapter.getFilter().filter(textView.getText());
				}
			}
		});
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
	
}
