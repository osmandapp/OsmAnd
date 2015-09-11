package net.osmand.plus.osmedit;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.PlatformUtil;
import net.osmand.osm.PoiType;
import net.osmand.osm.edit.OSMSettings;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.osmedit.data.EditPoiData;
import net.osmand.plus.osmedit.data.Tag;

import org.apache.commons.logging.Log;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class AdvancedDataFragment extends Fragment {
	private static final String TAG = "AdvancedDataFragment";
	private static final Log LOG = PlatformUtil.getLog(AdvancedDataFragment.class);

	private TagAdapterLinearLayoutHack mAdapter;
	private EditPoiData.TagsChangedListener mTagsChangedListener;
	private boolean mIsUserInput = true;
	private Drawable deleteDrawable;

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
					mAdapter.addTag(new Tag("", ""));
			}
		});
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		// TODO read more about lifecycle
		mAdapter.updateViews();
		mTagsChangedListener = new EditPoiData.TagsChangedListener() {
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

	private EditPoiFragment getEditPoiFragment() {
		return (EditPoiFragment) getParentFragment();
	}

	private EditPoiData getData() {
		return getEditPoiFragment().getEditPoiData();
	}

	public class TagAdapterLinearLayoutHack {
		private final LinearLayout linearLayout;
		private final EditPoiData editPoiData;

		private final TextView nameTextView;
		private final TextView amenityTagTextView;
		private final TextView amenityTextView;
		private final Map<String, PoiType> allTranslatedSubTypes;

		public TagAdapterLinearLayoutHack(LinearLayout linearLayout,
										  EditPoiData editPoiData,
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
			editPoiData.tags.add(tag);
			if (mIsUserInput)
				editPoiData.notifyDatasetChanged(mTagsChangedListener);
			updateViews();
		}

		public void updateViews() {
			linearLayout.removeAllViews();
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
			final AutoCompleteTextView tagEditText =
					(AutoCompleteTextView) convertView.findViewById(R.id.tagEditText);
			tagEditText.setText(tag.tag);
			final EditText valueEditText = (EditText) convertView.findViewById(R.id.valueEditText);
			ImageButton deleteItemImageButton =
					(ImageButton) convertView.findViewById(R.id.deleteItemImageButton);
			valueEditText.setText(tag.value);
			deleteItemImageButton.setImageDrawable(deleteDrawable);
			deleteItemImageButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					LOG.debug("onClick(" + "v=" + v + ") tag=" + tag
							+ "; editPoiData.tags" + editPoiData.tags);
					linearLayout.removeView((View) v.getParent());
					editPoiData.tags.remove(tag);
					LOG.debug("editPoiData.tags" + editPoiData.tags + " mIsUserInput=" + mIsUserInput);
					if (mIsUserInput)
						editPoiData.notifyDatasetChanged(null);
					LOG.debug("editPoiData.tags" + editPoiData.tags);
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
			final Set<String> tagKeys = new TreeSet<>();
			for (OSMSettings.OSMTagKey t : OSMSettings.OSMTagKey.values()) {
				if ((t != OSMSettings.OSMTagKey.NAME) && (t != OSMSettings.OSMTagKey.OPENING_HOURS) && (t != OSMSettings.OSMTagKey.PHONE)
						&& (t != OSMSettings.OSMTagKey.WEBSITE)) {
					tagKeys.add(t.getValue());
				}
			}

			ArrayAdapter<Object> adapter = new ArrayAdapter<>(linearLayout.getContext(),
					R.layout.list_textview, tagKeys.toArray());
			tagEditText.setAdapter(adapter);
			tagEditText.setThreshold(1);
			tagEditText.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					// TODO: 8/29/15 Rewrite as dialog fragment
					AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
					final String[] tags = tagKeys.toArray(new String[tagKeys.size()]);
					builder.setItems(tags, new Dialog.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							tagEditText.setText(tags[which]);
						}

					});
					builder.create();
					builder.show();
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
}
