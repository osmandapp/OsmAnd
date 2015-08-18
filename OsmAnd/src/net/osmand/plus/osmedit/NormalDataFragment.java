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
import android.widget.EditText;
import android.widget.ImageView;

import net.osmand.osm.edit.OSMSettings;
import net.osmand.plus.IconsCache;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.osmedit.EditPoiFragment.Tag;

import java.util.HashMap;
import java.util.Map;

public class NormalDataFragment extends Fragment {
	private static final String TAG = "NormalDataFragment";
	private EditText streetEditText;
	private EditText houseNumberEditText;
	private EditText phoneEditText;
	private EditText webSiteEditText;
	private EditText descriptionEditText;
	private EditPoiFragment.EditPoiData.TagsChangedListener mTagsChangedListener;
	private boolean mIsUserInput = true;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		IconsCache iconsCache = ((MapActivity) getActivity()).getMyApplication().getIconsCache();
		View view = inflater.inflate(R.layout.fragment_edit_poi_normal, container, false);

		ImageView streetImageView = (ImageView) view.findViewById(R.id.streetImageView);
		streetImageView.setImageDrawable(iconsCache.getContentIcon(R.drawable.ic_action_street_name));
		ImageView houseNumberImageView = (ImageView) view.findViewById(R.id.houseNumberImageView);
		houseNumberImageView.setImageDrawable(iconsCache.getContentIcon(R.drawable.ic_action_building_number));
		ImageView phoneImageView = (ImageView) view.findViewById(R.id.phoneImageView);
		phoneImageView.setImageDrawable(iconsCache.getContentIcon(R.drawable.ic_action_call_dark));
		ImageView webSiteImageView = (ImageView) view.findViewById(R.id.webSiteImageView);
		webSiteImageView.setImageDrawable(iconsCache.getContentIcon(R.drawable.ic_world_globe_dark));
		ImageView descriptionImageView = (ImageView) view.findViewById(R.id.descriptionImageView);
		descriptionImageView.setImageDrawable(iconsCache.getContentIcon(R.drawable.ic_action_description));

		streetEditText = (EditText) view.findViewById(R.id.streetEditText);
		streetEditText.addTextChangedListener(new MyOnFocusChangeListener(getData(),
				OSMSettings.OSMTagKey.ADDR_STREET.getValue()));
		houseNumberEditText = (EditText) view.findViewById(R.id.houseNumberEditText);
		houseNumberEditText.addTextChangedListener(new MyOnFocusChangeListener(getData(),
				OSMSettings.OSMTagKey.ADDR_HOUSE_NUMBER.getValue()));
		phoneEditText = (EditText) view.findViewById(R.id.phoneEditText);
		phoneEditText.addTextChangedListener(new MyOnFocusChangeListener(getData(),
				OSMSettings.OSMTagKey.PHONE.getValue()));
		webSiteEditText = (EditText) view.findViewById(R.id.webSiteEditText);
		webSiteEditText.addTextChangedListener(new MyOnFocusChangeListener(getData(),
				OSMSettings.OSMTagKey.WEBSITE.getValue()));
		descriptionEditText = (EditText) view.findViewById(R.id.descriptionEditText);
		descriptionEditText.addTextChangedListener(new MyOnFocusChangeListener(getData(),
				OSMSettings.OSMTagKey.DESCRIPTION.getValue()));
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		mTagsChangedListener = new EditPoiFragment.EditPoiData.TagsChangedListener() {
			@Override
			public void onTagsChanged() {
				TagMapProcessor tagMapProcessor = new TagMapProcessor();
				tagMapProcessor.addFilter(OSMSettings.OSMTagKey.ADDR_STREET.getValue(),
						streetEditText);
				tagMapProcessor.addFilter(OSMSettings.OSMTagKey.ADDR_HOUSE_NUMBER.getValue(),
						houseNumberEditText);
				tagMapProcessor.addFilter(OSMSettings.OSMTagKey.PHONE.getValue(),
						phoneEditText);
				tagMapProcessor.addFilter(OSMSettings.OSMTagKey.WEBSITE.getValue(),
						webSiteEditText);
				tagMapProcessor.addFilter(OSMSettings.OSMTagKey.DESCRIPTION.getValue(),
						descriptionEditText);

				mIsUserInput = false;
				for (Tag tag : getData().tags) {
					tagMapProcessor.process(tag);
				}
				tagMapProcessor.clearEmptyFields();
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

	private static class TagMapProcessor {
		private final Map<String, EditText> mFilters = new HashMap<>();

		public void addFilter(String tag, EditText editText) {
			mFilters.put(tag, editText);
		}

		public void process(Tag tag) {
			if (mFilters.containsKey(tag.tag)) {
				final EditText editText = mFilters.get(tag.tag);
				editText.setText(tag.value);
				mFilters.remove(tag.tag);
			}
		}

		public void clearEmptyFields() {
			for (String tag : mFilters.keySet()) {
				mFilters.get(tag).setText(null);
			}
		}
	}

	private EditPoiFragment getEditPoiFragment() {
		return (EditPoiFragment) getParentFragment();
	}

	private EditPoiFragment.EditPoiData getData() {
		return getEditPoiFragment().getEditPoiData();
	}

	private class MyOnFocusChangeListener implements TextWatcher {
		private final EditPoiFragment.EditPoiData data;
		private final String tagName;

		public MyOnFocusChangeListener(EditPoiFragment.EditPoiData data,
									   String tagName) {
			this.data = data;
			this.tagName = tagName;
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
		}

		@Override
		public void afterTextChanged(Editable s) {
			if (mIsUserInput) {
				String string = s.toString();
				if (!TextUtils.isEmpty(string)) {
					Tag tag = new Tag(tagName, string);
					data.tags.remove(tag);
					data.tags.add(tag);
					data.notifyDatasetChanged(mTagsChangedListener);
				}
			}
		}
	}
}
