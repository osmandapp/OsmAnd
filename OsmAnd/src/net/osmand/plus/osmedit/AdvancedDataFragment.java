package net.osmand.plus.osmedit;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import net.osmand.plus.R;
import net.osmand.plus.osmedit.EditPoiFragment.Tag;

public class AdvancedDataFragment extends Fragment {
	private static final String TAG = "AdvancedDataFragment";

	private TagAdapterLinearLayoutHack mAdapter;

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
		LinearLayout editTagsLineaLayout =
				(LinearLayout) view.findViewById(R.id.editTagsList);
		Log.v(TAG, "arguments=" + savedInstanceState + "; ll=" + editTagsLineaLayout);
		Log.v(TAG, "not restored");
		mAdapter = new TagAdapterLinearLayoutHack(editTagsLineaLayout, getData());
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
		getEditPoiFragment().addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			@Override
			public void onPageScrolled(int i, float v, int i1) {
			}

			@Override
			public void onPageSelected(int i) {
				if (i == 1) mAdapter.updateViews();
			}

			@Override
			public void onPageScrollStateChanged(int i) {
			}
		});
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		Log.v(TAG, "onSaveInstanceState(" + "outState=" + outState + ")");
		super.onSaveInstanceState(outState);
	}

	public static class TagAdapterLinearLayoutHack {
		private final LinearLayout linearLayout;
		private final EditPoiFragment.EditPoiData editPoiData;

		public TagAdapterLinearLayoutHack(LinearLayout linearLayout,
										  EditPoiFragment.EditPoiData editPoiData) {
			this.linearLayout = linearLayout;
			this.editPoiData = editPoiData;
		}

		public void addTag(Tag tag) {
			View view = getView(tag);
			editPoiData.tags.add(tag);
			EditText valueEditText = (EditText) view.findViewById(R.id.valueEditText);
			Log.v(TAG, "valueEditText text=" + valueEditText.getText());
			linearLayout.addView(view);
		}

		public void updateViews() {
			linearLayout.removeAllViews();
			Log.v(TAG, "editPoiData.tags=" + editPoiData.tags);
			for (Tag tag : editPoiData.tags) {
				Log.v(TAG, "tag=" + tag);
				View view = getView(tag);
				EditText valueEditText = (EditText) view.findViewById(R.id.valueEditText);
				Log.v(TAG, "valueEditText text=" + valueEditText.getText());
				linearLayout.addView(view);
			}
		}

		private View getView(final Tag tag) {
			Log.v(TAG, "getView(" + "tag=" + tag + ")");
			final View convertView = LayoutInflater.from(linearLayout.getContext())
					.inflate(R.layout.poi_tag_list_item, null, false);
			EditText tagEditText = (EditText) convertView.findViewById(R.id.tagEditText);
			EditText valueEditText = (EditText) convertView.findViewById(R.id.valueEditText);
			ImageButton deleteItemImageButton =
					(ImageButton) convertView.findViewById(R.id.deleteItemImageButton);
			tagEditText.setText(tag.tag);
			valueEditText.setText(tag.value);
			deleteItemImageButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					linearLayout.removeView((View) v.getParent());
					editPoiData.tags.remove(tag);
				}
			});
			Log.v(TAG, "convertView=" + convertView);
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
