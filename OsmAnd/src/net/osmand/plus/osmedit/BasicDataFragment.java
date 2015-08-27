package net.osmand.plus.osmedit;

import android.graphics.drawable.Drawable;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.PlatformUtil;
import net.osmand.osm.edit.OSMSettings;
import net.osmand.plus.IconsCache;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.osmedit.data.EditPoiData;
import net.osmand.plus.osmedit.data.Tag;
import net.osmand.plus.osmedit.dialogs.OpeningHoursDaysDialogFragment;
import net.osmand.util.OpeningHoursParser;
import net.osmand.util.OpeningHoursParser.BasicOpeningHourRule;

import org.apache.commons.logging.Log;

import java.util.HashMap;
import java.util.Map;

public class BasicDataFragment extends Fragment {
	private static final String TAG = "BasicDataFragment";
	private static final Log LOG = PlatformUtil.getLog(BasicDataFragment.class);
	private static final String OPENING_HOURS = "opening_hours";
	private EditText streetEditText;
	private EditText houseNumberEditText;
	private EditText phoneEditText;
	private EditText webSiteEditText;
	private EditText descriptionEditText;
	private EditPoiData.TagsChangedListener mTagsChangedListener;
	private boolean mIsUserInput = true;
	OpeningHoursAdapter mOpeningHoursAdapter;

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
		Button addOpeningHoursButton = (Button) view.findViewById(R.id.addOpeningHoursButton);
		addOpeningHoursButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				BasicOpeningHourRule r = new BasicOpeningHourRule();
				OpeningHoursDaysDialogFragment fragment = OpeningHoursDaysDialogFragment.createInstance(r, -1);
				fragment.show(getChildFragmentManager(), "OpenTimeDialogFragment");
			}
		});
		LinearLayout openHoursContainer = (LinearLayout) view.findViewById(R.id.openHoursContainer);
		Drawable clockDrawable = iconsCache.getContentIcon(R.drawable.ic_action_time);
		Drawable deleteDrawable = iconsCache.getContentIcon(R.drawable.ic_action_remove_dark);
		if (savedInstanceState != null && savedInstanceState.containsKey(OPENING_HOURS)) {
			mOpeningHoursAdapter = new OpeningHoursAdapter(
					(OpeningHoursParser.OpeningHours) savedInstanceState.getSerializable(OPENING_HOURS),
					openHoursContainer, getData(), clockDrawable, deleteDrawable);
			mOpeningHoursAdapter.updateViews();
		} else {
			mOpeningHoursAdapter = new OpeningHoursAdapter(new OpeningHoursParser.OpeningHours(),
					openHoursContainer, getData(), clockDrawable, deleteDrawable);
		}
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		mTagsChangedListener = new EditPoiData.TagsChangedListener() {
			@Override
			public void onTagsChanged() {
				TagMapProcessor tagMapProcessor = new TagMapProcessor();
				tagMapProcessor.addFilter(OSMSettings.OSMTagKey.ADDR_STREET.getValue(),
						new EditTextTagFilter(streetEditText));
				tagMapProcessor.addFilter(OSMSettings.OSMTagKey.ADDR_HOUSE_NUMBER.getValue(),
						new EditTextTagFilter(houseNumberEditText));
				tagMapProcessor.addFilter(OSMSettings.OSMTagKey.PHONE.getValue(),
						new EditTextTagFilter(phoneEditText));
				tagMapProcessor.addFilter(OSMSettings.OSMTagKey.WEBSITE.getValue(),
						new EditTextTagFilter(webSiteEditText));
				tagMapProcessor.addFilter(OSMSettings.OSMTagKey.DESCRIPTION.getValue(),
						new EditTextTagFilter(descriptionEditText));
				tagMapProcessor.addFilter(OSMSettings.OSMTagKey.OPENING_HOURS.getValue(),
						new OpenHoursTagFilter(mOpeningHoursAdapter));

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

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putSerializable(OPENING_HOURS, mOpeningHoursAdapter.openingHours);
		super.onSaveInstanceState(outState);
	}

	public void addBasicOpeningHoursRule(BasicOpeningHourRule item) {
		LOG.debug("item=" + item.toRuleString(false));
		mOpeningHoursAdapter.addOpeningHoursRule(item);
	}

	private static class TagMapProcessor {
		private final Map<String, TagFilter> mFilters = new HashMap<>();

		public void addFilter(String tag, TagFilter filter) {
			mFilters.put(tag, filter);
		}

		public void process(Tag tag) {
			if (mFilters.containsKey(tag.tag)) {
				final TagFilter filter = mFilters.get(tag.tag);
				filter.process(tag);
				mFilters.remove(tag.tag);
			}
		}

		public void clearEmptyFields() {
			for (String tag : mFilters.keySet()) {
				mFilters.get(tag).onUntriggered();
			}
		}
	}

	private interface TagFilter {
		void process(Tag tag);

		void onUntriggered();
	}

	private static class EditTextTagFilter implements TagFilter {
		private final EditText editText;

		private EditTextTagFilter(EditText editText) {
			this.editText = editText;
		}

		@Override
		public void process(Tag tag) {
			editText.setText(tag.value);
		}

		@Override
		public void onUntriggered() {
			editText.setText(null);
		}
	}

	private static class OpenHoursTagFilter implements TagFilter {
		private final OpeningHoursAdapter adapter;

		private OpenHoursTagFilter(OpeningHoursAdapter adapter) {
			this.adapter = adapter;
		}

		@Override
		public void process(Tag tag) {
			String openingHoursString = tag.value;
			OpeningHoursParser.OpeningHours openingHours =
					OpeningHoursParser.parseOpenedHours(openingHoursString);
			if (openingHours == null) {
				openingHours = new OpeningHoursParser.OpeningHours();
				// TODO show error message
			}
			adapter.replaceOpeningHours(openingHours);
			adapter.updateViews();
		}

		@Override
		public void onUntriggered() {

		}
	}

	private EditPoiFragment getEditPoiFragment() {
		return (EditPoiFragment) getParentFragment();
	}

	private EditPoiData getData() {
		return getEditPoiFragment().getEditPoiData();
	}

	private class MyOnFocusChangeListener implements TextWatcher {
		private final EditPoiData data;
		private final String tagName;

		public MyOnFocusChangeListener(EditPoiData data,
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

	private class OpeningHoursAdapter {
		private OpeningHoursParser.OpeningHours openingHours;
		private final LinearLayout linearLayout;
		private final EditPoiData data;
		private final Drawable clockDrawable;
		private final Drawable deleteDrawable;

		public OpeningHoursAdapter(OpeningHoursParser.OpeningHours openingHours,
								   LinearLayout linearLayout, EditPoiData data,
								   Drawable clockDrawable, Drawable deleteDrawable) {
			this.openingHours = openingHours;
			this.linearLayout = linearLayout;
			this.data = data;
			this.clockDrawable = clockDrawable;
			this.deleteDrawable = deleteDrawable;
		}

		public void addOpeningHoursRule(OpeningHoursParser.BasicOpeningHourRule rule) {
			openingHours.addRule(rule);
			updateViews();
		}

		public void replaceOpeningHours(OpeningHoursParser.OpeningHours openingHours) {
			this.openingHours = openingHours;
		}

		public void updateViews() {
			linearLayout.removeAllViews();
			for (int i = 0; i < openingHours.getRules().size(); i++) {
				linearLayout.addView(getView(i));
			}
			if (mIsUserInput) {
				Tag openHours = new Tag(OSMSettings.OSMTagKey.OPENING_HOURS.getValue(),
						openingHours.toStringNoMonths());
				data.tags.remove(openHours);
				data.tags.add(openHours);
				data.notifyDatasetChanged(null);
			}
		}

		private View getView(final int position) {
			OpeningHoursParser.BasicOpeningHourRule rule =
					(BasicOpeningHourRule) openingHours.getRules().get(position);

			final View view = LayoutInflater.from(linearLayout.getContext())
					.inflate(R.layout.open_time_list_item, null, false);
			ImageView clockIconImageView = (ImageView) view.findViewById(R.id.clockIconImageView);
			clockIconImageView.setImageDrawable(clockDrawable);

			TextView daysTextView = (TextView) view.findViewById(R.id.daysTextView);
			StringBuilder stringBuilder = new StringBuilder();
			rule.appendDaysString(stringBuilder);
			daysTextView.setText(stringBuilder.toString());

			TextView openingTextView = (TextView) view.findViewById(R.id.openingTextView);
			final int openingHour = rule.getStartTime() / 60;
			int openingMinute = rule.getStartTime() - openingHour * 60;
			openingTextView.setText(formatTime(openingHour, openingMinute));

			TextView closingTextView = (TextView) view.findViewById(R.id.closingTextView);
			int enHour = rule.getEndTime() / 60;
			int enTime = rule.getEndTime() - enHour * 60;
			closingTextView.setText(formatTime(enHour, enTime));

			ImageButton deleteItemImageButton = (ImageButton) view.findViewById(R.id.deleteItemImageButton);
			deleteItemImageButton.setImageDrawable(deleteDrawable);
			deleteItemImageButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					openingHours.getRules().remove(position);
					updateViews();
				}
			});
			return view;
		}
	}
	private static String formatTime(int h, int t) {
		StringBuilder b = new StringBuilder();
		if (h < 10) {
			b.append("0"); //$NON-NLS-1$
		}
		b.append(h).append(":"); //$NON-NLS-1$
		if (t < 10) {
			b.append("0"); //$NON-NLS-1$
		}
		b.append(t);
		return b.toString();
	}
}
