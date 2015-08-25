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

public class NormalDataFragment extends Fragment {
	private static final String TAG = "NormalDataFragment";
	private static final Log LOG = PlatformUtil.getLog(NormalDataFragment.class);
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
		if (savedInstanceState != null && savedInstanceState.containsKey(OPENING_HOURS)) {
			mOpeningHoursAdapter = new OpeningHoursAdapter(
					(OpeningHoursParser.OpeningHours) savedInstanceState.getSerializable(OPENING_HOURS),
					openHoursContainer, getData());
			mOpeningHoursAdapter.updateViews();
		} else {
			mOpeningHoursAdapter = new OpeningHoursAdapter(new OpeningHoursParser.OpeningHours(),
					openHoursContainer, getData());
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

	private static class OpeningHoursAdapter {
		private final OpeningHoursParser.OpeningHours openingHours;
		private final LinearLayout linearLayout;
		private final EditPoiData data;

		public OpeningHoursAdapter(OpeningHoursParser.OpeningHours openingHours,
								   LinearLayout linearLayout, EditPoiData data) {
			this.openingHours = openingHours;
			this.linearLayout = linearLayout;
			this.data = data;
		}

		public void addOpeningHoursRule(OpeningHoursParser.BasicOpeningHourRule rule) {
			openingHours.addRule(rule);
			updateViews();
		}

		public void updateViews() {
			linearLayout.removeAllViews();
			for (int i = 0; i < openingHours.getRules().size(); i++) {
				linearLayout.addView(getView(i));
			}
			Tag openHours = new Tag(OSMSettings.OSMTagKey.OPENING_HOURS.getValue(),
					openingHours.toString());
			data.tags.remove(openHours);
			data.tags.add(openHours);
			data.notifyDatasetChanged(null);
		}

		private View getView(final int position) {
			OpeningHoursParser.BasicOpeningHourRule rule =
					(BasicOpeningHourRule) openingHours.getRules().get(position);

			final View view = LayoutInflater.from(linearLayout.getContext())
					.inflate(R.layout.open_time_list_item, null, false);
			ImageView clockIconImageView = (ImageView) view.findViewById(R.id.clockIconImageView);

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
			deleteItemImageButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					openingHours.getRules().remove(position);
					updateViews();
				}
			});
			return view;
		}

		private static String formatTime(int h, int t){
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
}
