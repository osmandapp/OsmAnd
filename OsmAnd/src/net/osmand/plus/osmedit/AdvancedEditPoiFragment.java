package net.osmand.plus.osmedit;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import net.osmand.PlatformUtil;
import net.osmand.StringMatcher;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiFilter;
import net.osmand.osm.PoiType;
import net.osmand.osm.edit.OSMSettings;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class AdvancedEditPoiFragment extends Fragment
		implements EditPoiDialogFragment.OnFragmentActivatedListener {
	private static final String TAG = "AdvancedEditPoiFragment";
	private static final Log LOG = PlatformUtil.getLog(AdvancedEditPoiFragment.class);

	private TagAdapterLinearLayoutHack mAdapter;
	private EditPoiData.TagsChangedListener mTagsChangedListener;
	private Drawable deleteDrawable;
	private TextView nameTextView;
	private TextView amenityTagTextView;
	private TextView amenityTextView;

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.fragment_edit_poi_advanced, container, false);

		Display display = getActivity().getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		int height = size.y;
		view.findViewById(R.id.screenFiller).setMinimumHeight(height);

		deleteDrawable = ((OsmandApplication) getActivity().getApplication()).getIconsCache()
				.getPaintedContentIcon(R.drawable.ic_action_remove_dark,
						getActivity().getResources().getColor(R.color.dash_search_icon_dark));
		nameTextView = (TextView) view.findViewById(R.id.nameTextView);
		amenityTagTextView = (TextView) view.findViewById(R.id.amenityTagTextView);
		amenityTextView = (TextView) view.findViewById(R.id.amenityTextView);
		LinearLayout editTagsLineaLayout =
				(LinearLayout) view.findViewById(R.id.editTagsList);

		final MapPoiTypes mapPoiTypes = ((OsmandApplication) getActivity().getApplication()).getPoiTypes();
		mAdapter = new TagAdapterLinearLayoutHack(editTagsLineaLayout, getData());
		// It is possible to not restart initialization every time, and probably move initialization to appInit
		Map<String, PoiType> translatedTypes = getData().getAllTranslatedSubTypes();
		HashSet<String> tagKeys = new HashSet<>();
		HashSet<String> valueKeys = new HashSet<>();
		for (AbstractPoiType abstractPoiType : translatedTypes.values()) {
			addPoiToStringSet(abstractPoiType, tagKeys, valueKeys);
		}
		addPoiToStringSet(mapPoiTypes.getOtherMapCategory(), tagKeys, valueKeys);
		mAdapter.setTagData(tagKeys.toArray(new String[tagKeys.size()]));
		mAdapter.setValueData(valueKeys.toArray(new String[valueKeys.size()]));
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
				if (Algorithms.objectEquals(anyTag, EditPoiData.POI_TYPE_TAG)) {
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
			amenityTagTextView.setText(pt.getOsmTag());
			amenityTextView.setText(pt.getOsmValue());
		} else {
			amenityTagTextView.setText(getData().getPoiCategory().getDefaultTag());
			amenityTextView.setText(getData().getPoiTypeString());
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
			for (Entry<String, String> tag : editPoiData.getTagValues().entrySet()) {
				if (tag.getKey().equals(EditPoiData.POI_TYPE_TAG)
						|| tag.getKey().equals(OSMSettings.OSMTagKey.NAME.getValue()))
					continue;
				addTagView(tag.getKey(), tag.getValue());
			}
			editPoiData.setIsInEdit(false);
		}

		public void addTagView(String tg, String vl) {
			View convertView = LayoutInflater.from(linearLayout.getContext())
					.inflate(R.layout.poi_tag_list_item, null, false);
			final AutoCompleteTextView tagEditText =
					(AutoCompleteTextView) convertView.findViewById(R.id.tagEditText);
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
			final AutoCompleteTextView valueEditText =
					(AutoCompleteTextView) convertView.findViewById(R.id.valueEditText);
			tagEditText.setText(tg);
			tagEditText.addTextChangedListener(new TextWatcher() {
				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {
					if (!editPoiData.isInEdit()) {
						editPoiData.removeTag(s.toString());
					}
				}

				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
				}

				@Override
				public void afterTextChanged(Editable s) {
					if (!editPoiData.isInEdit()) {
						editPoiData.removeTag(previousTag[0]);
						editPoiData.putTag(s.toString(), valueEditText.getText().toString());
						previousTag[0] = s.toString();
					}
				}
			});
			initAutocompleteTextView(tagEditText, tagAdapter);

			valueEditText.setText(vl);
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
			tagEditText.requestFocus();
		}

		public void setTagData(String[] tags) {
			tagAdapter.clear();
			tagAdapter.addAll(tags);
			tagAdapter.sort(String.CASE_INSENSITIVE_ORDER);
			tagAdapter.notifyDataSetChanged();
		}

		public void setValueData(String[] values) {
			valueAdapter.clear();
			valueAdapter.addAll(values);
			valueAdapter.sort(String.CASE_INSENSITIVE_ORDER);
			valueAdapter.notifyDataSetChanged();
		}
	}

	private static void initAutocompleteTextView(final AutoCompleteTextView textView,
												 final ArrayAdapter<String> adapter) {
		textView.setAdapter(adapter);
		textView.setThreshold(1);
		textView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
				builder.setAdapter(adapter, new Dialog.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						textView.setText(adapter.getItem(which));
					}

				});
				builder.create().show();
			}
		});
		textView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					adapter.getFilter().filter(textView.getText());
				}
			}
		});
	}

	private static void addPoiToStringSet(AbstractPoiType abstractPoiType, Set<String> stringSet,
										  Set<String> values) {
		if (abstractPoiType instanceof PoiType) {
			PoiType poiType = (PoiType) abstractPoiType;
			if (poiType.isNotEditableOsm() || poiType.getBaseLangType() != null) {
				return;
			}
			if (poiType.getOsmTag() != null &&
					!poiType.getOsmTag().equals(OSMSettings.OSMTagKey.NAME.getValue())) {
				stringSet.add(poiType.getOsmTag());
				if (poiType.getOsmTag2() != null) {
					stringSet.add(poiType.getOsmTag2());
				}

			}
			if (poiType.getOsmValue() != null) {
				values.add(poiType.getOsmValue());
			}
			if (poiType.getOsmValue2() != null) {
				values.add(poiType.getOsmValue2());
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
