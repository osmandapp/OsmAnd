package net.osmand.plus.osmedit;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.osm.PoiType;
import net.osmand.osm.edit.OSMSettings;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.osmedit.EditPoiFragment.Tag;

import java.util.Iterator;
import java.util.Map;

public class AdvancedDataFragment extends Fragment {
	private static final String TAG = "AdvancedDataFragment";

	private TagAdapterLinearLayoutHack mAdapter;
	private EditPoiFragment.EditPoiData.TagsChangedListener mTagsChangedListener;
	private boolean mIsUserInput = true;

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.fragment_edit_poi_advanced, container, false);
		final EditText tagEditText = (EditText) view.findViewById(R.id.tagEditText);
		final EditText valueEditText = (EditText) view.findViewById(R.id.valueEditText);

		ImageButton deleteItemImageButton =
				(ImageButton) view.findViewById(R.id.deleteItemImageButton);
		deleteItemImageButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				tagEditText.setText(null);
				tagEditText.clearFocus();
				valueEditText.setText(null);
				valueEditText.clearFocus();
			}
		});
		TextView nameTextView = (TextView) view.findViewById(R.id.nameTextView);
		TextView amenityTagTextView = (TextView) view.findViewById(R.id.amenityTagTextView);
		TextView amenityTextView = (TextView) view.findViewById(R.id.amenityTextView);
		LinearLayout editTagsLineaLayout =
				(LinearLayout) view.findViewById(R.id.editTagsList);
		mAdapter = new TagAdapterLinearLayoutHack(editTagsLineaLayout, getData(),
				nameTextView, amenityTagTextView, amenityTextView,
				((OsmandApplication) getActivity().getApplication()).getPoiTypes().getAllTranslatedNames());
//		setListViewHeightBasedOnChildren(editTagsLineaLayout);
		Button addTagButton = (Button) view.findViewById(R.id.addTagButton);
		addTagButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String tag = String.valueOf(tagEditText.getText());
				String value = String.valueOf(valueEditText.getText());
				if (!TextUtils.isEmpty(tag) && !TextUtils.isEmpty(value)) {
					mAdapter.addTag(new Tag(tag, value));
//					setListViewHeightBasedOnChildren(editTagsLineaLayout);
					tagEditText.setText(null);
					tagEditText.clearFocus();
					valueEditText.setText(null);
					valueEditText.clearFocus();
				}
			}
		});
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		// TODO read more about lifecycle
		mAdapter.updateViews();
		mTagsChangedListener = new EditPoiFragment.EditPoiData.TagsChangedListener() {
			@Override
			public void onTagsChanged() {
				mIsUserInput = false;
				mAdapter.updateViews();
				mIsUserInput = true;
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

	public class TagAdapterLinearLayoutHack {
		private final LinearLayout linearLayout;
		private final EditPoiFragment.EditPoiData editPoiData;

		private final TextView nameTextView;
		private final TextView amenityTagTextView;
		private final TextView amenityTextView;
		private final Map<String, PoiType> allTranslatedSubTypes;

		public TagAdapterLinearLayoutHack(LinearLayout linearLayout,
										  EditPoiFragment.EditPoiData editPoiData,
										  TextView nameTextView,
										  TextView amenityTagTextView,
										  TextView amenityTextView,
										  Map<String, PoiType> allTranslatedSubTypes) {
			this.linearLayout = linearLayout;
			this.editPoiData = editPoiData;
			this.nameTextView = nameTextView;
			this.amenityTagTextView = amenityTagTextView;
			this.amenityTextView = amenityTextView;
			this.allTranslatedSubTypes = allTranslatedSubTypes;
		}

		public void addTag(Tag tag) {
			View view = getView(tag);
			editPoiData.tags.add(tag);
			if (mIsUserInput)
				editPoiData.notifyDatasetChanged(mTagsChangedListener);
			EditText valueEditText = (EditText) view.findViewById(R.id.valueEditText);
			linearLayout.addView(view);
		}

		public void updateViews() {
			linearLayout.removeAllViews();
			Iterator<Tag> iterator = editPoiData.tags.iterator();
			for (Tag tag : editPoiData.tags) {
				if (tag.tag.equals(OSMSettings.OSMTagKey.NAME.getValue())) {
					nameTextView.setText(tag.value);
				} else if (tag.tag.equals(EditPoiFragment.POI_TYPE_TAG)) {
					String subType = tag.value.trim().toLowerCase();
					if (allTranslatedSubTypes.get(subType) != null) {
						PoiType pt = allTranslatedSubTypes.get(subType);
						amenityTagTextView.setText(pt.getOsmTag());
						amenityTextView.setText(pt.getOsmValue());
					} else {
						amenityTagTextView.setText(editPoiData.amenity.getType().getDefaultTag());
						amenityTextView.setText(subType);
					}
				} else {
					View view = getView(tag);
					linearLayout.addView(view);
				}
			}
		}

		private View getView(final Tag tag) {
			final View convertView = LayoutInflater.from(linearLayout.getContext())
					.inflate(R.layout.poi_tag_list_item, null, false);
			final EditText tagEditText = (EditText) convertView.findViewById(R.id.tagEditText);
			tagEditText.setText(tag.tag);
			final EditText valueEditText = (EditText) convertView.findViewById(R.id.valueEditText);
			ImageButton deleteItemImageButton =
					(ImageButton) convertView.findViewById(R.id.deleteItemImageButton);
			valueEditText.setText(tag.value);
			deleteItemImageButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					linearLayout.removeView((View) v.getParent());
					editPoiData.tags.remove(tag);
					if (mIsUserInput)
						editPoiData.notifyDatasetChanged(mTagsChangedListener);
				}
			});
			tagEditText.addTextChangedListener(new TextWatcher() {
				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				}

				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
				}

				@Override
				public void afterTextChanged(Editable s) {
					editPoiData.tags.remove(tag);
					tag.tag = tagEditText.getText().toString();
					editPoiData.tags.add(tag);
					if (mIsUserInput)
						editPoiData.notifyDatasetChanged(mTagsChangedListener);
				}
			});
			valueEditText.addTextChangedListener(new TextWatcher() {
				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				}

				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
				}

				@Override
				public void afterTextChanged(Editable s) {
					editPoiData.tags.remove(tag);
					tag.value = valueEditText.getText().toString();
					editPoiData.tags.add(tag);
					if (mIsUserInput)
						editPoiData.notifyDatasetChanged(mTagsChangedListener);
				}
			});
			return convertView;
		}
	}

	private EditPoiFragment getEditPoiFragment() {
		return (EditPoiFragment) getParentFragment();
	}

	private EditPoiFragment.EditPoiData getData() {
		return getEditPoiFragment().getEditPoiData();
	}
}
