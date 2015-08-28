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
import net.osmand.plus.osmedit.dialogs.OpeningHoursHoursDialogFragment;
import net.osmand.util.OpeningHoursParser;
import net.osmand.util.OpeningHoursParser.BasicOpeningHourRule;

import org.apache.commons.logging.Log;

import java.util.Arrays;
import java.util.Calendar;
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
		ImageView openingHoursImageView = (ImageView) view.findViewById(R.id.openingHoursImageView);
		openingHoursImageView.setImageDrawable(iconsCache.getContentIcon(R.drawable.ic_action_time));

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
				BasicOpeningHourRule rule = new BasicOpeningHourRule();
				// TODO: 8/27/15 Figure out some better defauls or leave it as it is
				rule.setStartTime(9 * 60);
				rule.setEndTime(18 * 60);
				OpeningHoursDaysDialogFragment fragment = OpeningHoursDaysDialogFragment.createInstance(rule, -1);
				fragment.show(getChildFragmentManager(), "OpenTimeDialogFragment");
			}
		});
		LinearLayout openHoursContainer = (LinearLayout) view.findViewById(R.id.openHoursContainer);
		Drawable clockDrawable = iconsCache.getContentIcon(R.drawable.ic_action_time);
		Drawable deleteDrawable = iconsCache
				.getPaintedContentIcon(R.drawable.ic_action_remove_dark,
						getActivity().getResources().getColor(R.color.icon_color_light));
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

	public void setBasicOpeningHoursRule(BasicOpeningHourRule item, int position) {
		LOG.debug("item=" + item.toRuleString(false));
		mOpeningHoursAdapter.setOpeningHoursRule(item, position);
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
			LOG.debug("openingHoursString=" + openingHoursString);
			OpeningHoursParser.OpeningHours openingHours =
					parseOpenedHoursHandleErrors(openingHoursString);
			if (openingHours == null) {
				openingHours = new OpeningHoursParser.OpeningHours();
				// TODO show error message
			}
			LOG.debug("openingHours=" + openingHours);
			adapter.replaceOpeningHours(openingHours);
			adapter.updateViews();
		}

		@Override
		public void onUntriggered() {
			adapter.replaceOpeningHours(new OpeningHoursParser.OpeningHours());
			adapter.updateViews();
		}
	}

	private EditPoiFragment getEditPoiFragment() {
		return (EditPoiFragment) getParentFragment();
	}

	private EditPoiData getData() {
		return getEditPoiFragment().getEditPoiData();
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

	private static OpeningHoursParser.OpeningHours parseOpenedHoursHandleErrors(String format){
		if(format == null) {
			return null;
		}
		String[] rules = format.split(";"); //$NON-NLS-1$
		OpeningHoursParser.OpeningHours rs = new OpeningHoursParser.OpeningHours();
		for(String r : rules){
			r = r.trim();
			if (r.length() == 0) {
				continue;
			}
			// check if valid
			rs.addRule(parseRule(r));
		}
		return rs;
	}

	// TODO: 8/27/15 Consider refactoring OpeningHoursParser
	public static OpeningHoursParser.OpeningHoursRule parseRule(final String r){
		// replace words "sunrise" and "sunset" by real hours
		final String[] daysStr = new String[] {"Mo", "Tu", "We", "Th", "Fr", "Sa", "Su"};
		final String[] monthsStr = new String[] {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
		String sunrise = "07:00";
		String sunset = "21:00";
		String endOfDay = "24:00";

		String localRuleString = r.replaceAll("sunset", sunset).replaceAll("sunrise", sunrise)
				.replaceAll("\\+", "-" + endOfDay);
		int startDay      = -1;
		int previousDay   = -1;
		int startMonth    = -1;
		int previousMonth = -1;
		int k             = 0; // Position in opening_hours string

		BasicOpeningHourRule basic = new BasicOpeningHourRule();
		boolean[] days   = basic.getDays();
		boolean[] months = basic.getMonths();
		// check 24/7
		if("24/7".equals(localRuleString)){
			Arrays.fill(days, true);
			Arrays.fill(months, true);
			basic.addTimeRange(0, 24 * 60);
			return basic;
		}

		for (; k < localRuleString.length(); k++) {
			char ch = localRuleString.charAt(k);
			if (Character.isDigit(ch)) {
				// time starts
				break;
			}
			if ((k + 2 < localRuleString.length())
					&& localRuleString.substring(k, k + 3).equals("off")) {
				// value "off" is found
				break;
			}
			if(Character.isWhitespace(ch) || ch == ','){
			} else if (ch == '-') {
				if(previousDay != -1){
					startDay = previousDay;
				} else if (previousMonth != -1) {
					startMonth = previousMonth;
				} else {
					return new UnparseableRule(r);
				}
			} else if (k < r.length() - 1) {
				int i = 0;
				for(String s : daysStr){
					if(s.charAt(0) == ch && s.charAt(1) == r.charAt(k+1)){
						break;
					}
					i++;
				}
				if(i < daysStr.length){
					if(startDay != -1){
						for (int j = startDay; j <= i; j++) {
							days[j] = true;
						}
						if(startDay > i){// overflow handling, e.g. Su-We
							for (int j = startDay; j <= 6; j++) {
								days[j] = true;
							}
							for (int j = 0; j <= i; j++){
								days[j] = true;
							}
						}
						startDay = -1;
					} else {
						days[i] = true;
					}
					previousDay = i;
				} else {
					// Read Month
					int m = 0;
					for (String s : monthsStr) {
						if (s.charAt(0) == ch && s.charAt(1) == r.charAt(k + 1)
								&& s.charAt(2) == r.charAt(k + 2)) {
							break;
						}
						m++;
					}
					if (m < monthsStr.length) {
						if (startMonth != -1) {
							for (int j = startMonth; j <= m; j++) {
								months[j] = true;
							}
							if (startMonth > m) {// overflow handling, e.g. Oct-Mar
								for (int j = startMonth; j <= 11; j++) {
									months[j] = true;
								}
								for (int j = 0; j <= m; j++) {
									months[j] = true;
								}
							}
							startMonth = -1;
						} else {
							months[m] = true;
						}
						previousMonth = m;
					}
				}
			} else {
				return new UnparseableRule(r);
			}
		}
		if(previousDay == -1){
			// no days given => take all days.
			for (int i = 0; i<7; i++){
				days[i] = true;
			}
		}
		if (previousMonth == -1) {
			// no month given => take all months.
			for (int i = 0; i < 12; i++) {
				months[i] = true;
			}
		}
		String timeSubstr = localRuleString.substring(k);
		String[] times = timeSubstr.split(",");
		boolean timesExist = true;
		for (int i = 0; i < times.length; i++) {
			String time = times[i];
			time = time.trim();
			if(time.length() == 0){
				continue;
			}
			if(time.equals("off")){
				break; // add no time values
			}
			if(time.equals("24/7")){
				// for some reason, this is used. See tagwatch.
				basic.addTimeRange(0, 24*60);
				break;
			}
			String[] stEnd = time.split("-"); //$NON-NLS-1$
			if (stEnd.length != 2) {
				if (i == times.length - 1 && basic.getStartTime() == 0 && basic.getEndTime() == 0) {
					return new UnparseableRule(r);
				}
				continue;
			}
			timesExist = true;
			int st;
			int end;
			try {
				int i1 = stEnd[0].indexOf(':');
				int i2 = stEnd[1].indexOf(':');
				int startHour, startMin, endHour, endMin;
				if(i1 == -1) {
					// if no minutes are given, try complete value as hour
					startHour = Integer.parseInt(stEnd[0].trim());
					startMin  = 0;
				} else {
					startHour = Integer.parseInt(stEnd[0].substring(0, i1).trim());
					startMin  = Integer.parseInt(stEnd[0].substring(i1 + 1).trim());
				}
				if(i2 == -1) {
					// if no minutes are given, try complete value as hour
					endHour = Integer.parseInt(stEnd[1].trim());
					endMin  = 0;
				} else {
					endHour = Integer.parseInt(stEnd[1].substring(0, i2).trim());
					endMin  = Integer.parseInt(stEnd[1].substring(i2 + 1).trim());
				}
				st  = startHour * 60 + startMin;
				end = endHour   * 60 + endMin;
			} catch (NumberFormatException e) {
				return new UnparseableRule(r);
			}
			basic.addTimeRange(st, end);
		}
		if(!timesExist){
			return new UnparseableRule(r);
		}
		return basic;
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

		public void setOpeningHoursRule(OpeningHoursParser.BasicOpeningHourRule rule, int position) {
			if (position == -1) {
				openingHours.addRule(rule);
			} else {
				openingHours.getRules().set(position, rule);
			}
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
			final View view = LayoutInflater.from(linearLayout.getContext())
					.inflate(R.layout.open_time_list_item, null, false);

			ImageView clockIconImageView = (ImageView) view.findViewById(R.id.clockIconImageView);
			clockIconImageView.setImageDrawable(clockDrawable);

			TextView daysTextView = (TextView) view.findViewById(R.id.daysTextView);
			View timeContainer = view.findViewById(R.id.timeContainer);

			if (openingHours.getRules().get(position) instanceof BasicOpeningHourRule) {
				final OpeningHoursParser.BasicOpeningHourRule rule =
						(BasicOpeningHourRule) openingHours.getRules().get(position);
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
				timeContainer.setVisibility(View.VISIBLE);

				daysTextView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						OpeningHoursDaysDialogFragment fragment =
								OpeningHoursDaysDialogFragment.createInstance(rule, position);
						fragment.show(getChildFragmentManager(), "OpenTimeDialogFragment");
					}
				});
				openingTextView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						OpeningHoursHoursDialogFragment fragment =
								OpeningHoursHoursDialogFragment.createInstance(rule, position, true);
						fragment.show(getChildFragmentManager(), "OpeningHoursHoursDialogFragment");
					}
				});
				closingTextView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						OpeningHoursHoursDialogFragment fragment =
								OpeningHoursHoursDialogFragment.createInstance(rule, position, false);
						fragment.show(getChildFragmentManager(), "OpeningHoursHoursDialogFragment");
					}
				});
			} else if (openingHours.getRules().get(position) instanceof UnparseableRule) {
				daysTextView.setText(openingHours.getRules().get(position).toRuleString(false));
				timeContainer.setVisibility(View.GONE);
			}

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

	// TODO: 8/27/15 Consider moving to OpeningHoursParser
	private static class UnparseableRule implements OpeningHoursParser.OpeningHoursRule {
		private String ruleString;

		public UnparseableRule(String ruleString) {
			this.ruleString = ruleString;
		}

		@Override
		public boolean isOpenedForTime(Calendar cal, boolean checkPrevious) {
			return false;
		}

		@Override
		public boolean containsPreviousDay(Calendar cal) {
			return false;
		}

		@Override
		public boolean containsDay(Calendar cal) {
			return false;
		}

		@Override
		public boolean containsMonth(Calendar cal) {
			return false;
		}

		@Override
		public String toRuleString(boolean avoidMonths) {
			return ruleString;
		}
	}
}
