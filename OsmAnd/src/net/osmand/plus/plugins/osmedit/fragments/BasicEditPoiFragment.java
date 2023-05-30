package net.osmand.plus.plugins.osmedit.fragments;

import static net.osmand.plus.plugins.osmedit.dialogs.EditPoiDialogFragment.AMENITY_TEXT_LENGTH;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputFilter.LengthFilter;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.osmand.PlatformUtil;
import net.osmand.osm.edit.OSMSettings.OSMTagKey;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.LocaleHelper;
import net.osmand.plus.plugins.osmedit.data.EditPoiData;
import net.osmand.plus.plugins.osmedit.dialogs.EditPoiDialogFragment;
import net.osmand.plus.plugins.osmedit.dialogs.EditPoiDialogFragment.OnFragmentActivatedListener;
import net.osmand.plus.plugins.osmedit.dialogs.OpeningHoursDaysDialogFragment;
import net.osmand.plus.plugins.osmedit.dialogs.OpeningHoursHoursDialogFragment;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.tools.SimpleTextWatcher;
import net.osmand.util.Algorithms;
import net.osmand.util.OpeningHoursParser;
import net.osmand.util.OpeningHoursParser.BasicOpeningHourRule;
import net.osmand.util.OpeningHoursParser.OpeningHours;
import net.osmand.util.OpeningHoursParser.UnparseableRule;

import org.apache.commons.logging.Log;

import java.util.Locale;
import java.util.Map;

import gnu.trove.list.array.TIntArrayList;

public class BasicEditPoiFragment extends BaseOsmAndFragment implements OnFragmentActivatedListener {

	private static final Log LOG = PlatformUtil.getLog(BasicEditPoiFragment.class);
	private static final String OPENING_HOURS = "opening_hours";

	private EditText streetEditText;
	private EditText houseNumberEditText;
	private EditText phoneEditText;
	private EditText webSiteEditText;
	private EditText descriptionEditText;
	private OpeningHoursAdapter openingHoursAdapter;

	private boolean basicTagsInitialized;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = themedInflater.inflate(R.layout.fragment_edit_poi_normal, container, false);

		InputFilter[] lengthLimit = {
				new LengthFilter(AMENITY_TEXT_LENGTH)
		};
		streetEditText = view.findViewById(R.id.streetEditText);
		houseNumberEditText = view.findViewById(R.id.houseNumberEditText);
		phoneEditText = view.findViewById(R.id.phoneEditText);
		webSiteEditText = view.findViewById(R.id.webSiteEditText);
		descriptionEditText = view.findViewById(R.id.descriptionEditText);
		addTextWatcher(OSMTagKey.ADDR_STREET.getValue(), streetEditText);
		addTextWatcher(OSMTagKey.WEBSITE.getValue(), webSiteEditText);
		addTextWatcher(OSMTagKey.PHONE.getValue(), phoneEditText);
		addTextWatcher(OSMTagKey.ADDR_HOUSE_NUMBER.getValue(), houseNumberEditText);
		addTextWatcher(OSMTagKey.DESCRIPTION.getValue(), descriptionEditText);
		streetEditText.setFilters(lengthLimit);
		houseNumberEditText.setFilters(lengthLimit);
		phoneEditText.setFilters(lengthLimit);
		webSiteEditText.setFilters(lengthLimit);
		descriptionEditText.setFilters(lengthLimit);
		AndroidUtils.setTextHorizontalGravity(streetEditText, Gravity.START);
		AndroidUtils.setTextHorizontalGravity(houseNumberEditText, Gravity.START);
		AndroidUtils.setTextHorizontalGravity(phoneEditText, Gravity.START);
		AndroidUtils.setTextHorizontalGravity(webSiteEditText, Gravity.START);
		AndroidUtils.setTextHorizontalGravity(descriptionEditText, Gravity.START);
		Button addOpeningHoursButton = view.findViewById(R.id.addOpeningHoursButton);
		addOpeningHoursButton.setOnClickListener(v -> {
			BasicOpeningHourRule rule = new BasicOpeningHourRule();
			rule.setStartTime(9 * 60);
			rule.setEndTime(18 * 60);
			if (openingHoursAdapter.openingHours.getRules().isEmpty()) {
				rule.setDays(new boolean[] {true, true, true, true, true, false, false});
			}
			OpeningHoursDaysDialogFragment fragment = OpeningHoursDaysDialogFragment.createInstance(rule, -1);
			fragment.show(getChildFragmentManager(), "OpenTimeDialogFragment");
		});
		int iconColor = ColorUtilities.getSecondaryTextColor(app, nightMode);
		Drawable clockDrawable = getPaintedContentIcon(R.drawable.ic_action_time, iconColor);
		Drawable deleteDrawable = getPaintedContentIcon(R.drawable.ic_action_remove_dark, iconColor);
		LinearLayout openHoursContainer = view.findViewById(R.id.openHoursContainer);
		if (savedInstanceState != null && savedInstanceState.containsKey(OPENING_HOURS)) {
			OpeningHours openingHours = AndroidUtils.getSerializable(savedInstanceState, OPENING_HOURS, OpeningHours.class);
			openingHoursAdapter = new OpeningHoursAdapter(app, openingHours,
					openHoursContainer, getData(), clockDrawable, deleteDrawable);
			openingHoursAdapter.updateViews();
		} else {
			openingHoursAdapter = new OpeningHoursAdapter(app, new OpeningHours(),
					openHoursContainer, getData(), clockDrawable, deleteDrawable);
		}
		onFragmentActivated();
		return view;
	}

	protected void addTextWatcher(String tag, EditText e) {
		e.addTextChangedListener(new SimpleTextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				EditPoiData data = getData();
				if (data != null && !data.isInEdit()) {
					if (!TextUtils.isEmpty(s)) {
						data.putTag(tag, s.toString());
					} else if (basicTagsInitialized && isResumed()) {
						data.removeTag(tag);
					}
				}
			}

		});
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putSerializable(OPENING_HOURS, openingHoursAdapter.openingHours);
		super.onSaveInstanceState(outState);
	}

	public void setBasicOpeningHoursRule(BasicOpeningHourRule item, int position) {
		if (item.getStartTime() == 0 && item.getEndTime() == 0 && item.isOpenedEveryDay()) {
			item.setEndTime(24 * 60);
		}
		openingHoursAdapter.setOpeningHoursRule(item, position);
	}

	public void removeUnsavedOpeningHours() {
		EditPoiData data = getData();
		if (data != null) {
			OpeningHours openingHours = OpeningHoursParser.parseOpenedHoursHandleErrors(data.getTagValues()
					.get(OSMTagKey.OPENING_HOURS.getValue()));
			if (openingHours == null) {
				openingHours = new OpeningHours();
			}
			openingHoursAdapter.replaceOpeningHours(openingHours);
			openingHoursAdapter.updateViews();
		}
	}

	private EditPoiData getData() {
		Fragment parent = getParentFragment();
		if (parent instanceof EditPoiDialogFragment) {
			return ((EditPoiDialogFragment) parent).getEditPoiData();
		}
		return null;
	}

	@Override
	public void onFragmentActivated() {
		EditPoiData data = getData();
		if (data == null) {
			return;
		}
		basicTagsInitialized = false;
		Map<String, String> tagValues = data.getTagValues();
		streetEditText.setText(tagValues.get(OSMTagKey.ADDR_STREET.getValue()));
		houseNumberEditText.setText(tagValues.get(OSMTagKey.ADDR_HOUSE_NUMBER.getValue()));
		phoneEditText.setText(tagValues.get(OSMTagKey.PHONE.getValue()));
		webSiteEditText.setText(tagValues.get(OSMTagKey.WEBSITE.getValue()));
		descriptionEditText.setText(tagValues.get(OSMTagKey.DESCRIPTION.getValue()));

		OpeningHours openingHours = OpeningHoursParser.parseOpenedHoursHandleErrors(tagValues.get(OSMTagKey.OPENING_HOURS.getValue()));
		if (openingHours == null) {
			openingHours = new OpeningHours();
		}
		openingHoursAdapter.replaceOpeningHours(openingHours);
		openingHoursAdapter.updateViews();
		basicTagsInitialized = true;
	}

	private class OpeningHoursAdapter {

		private final OsmandApplication app;

		private OpeningHours openingHours;

		private final LinearLayout linearLayout;
		private final EditPoiData data;
		private final Drawable clockDrawable;
		private final Drawable deleteDrawable;

		public OpeningHoursAdapter(@NonNull OsmandApplication app,
		                           @NonNull OpeningHours openingHours,
		                           @NonNull LinearLayout linearLayout,
		                           @NonNull EditPoiData data,
		                           @NonNull Drawable clockDrawable,
		                           @NonNull Drawable deleteDrawable) {
			this.app = app;
			this.openingHours = openingHours;
			this.linearLayout = linearLayout;
			this.data = data;
			this.clockDrawable = clockDrawable;
			this.deleteDrawable = deleteDrawable;
		}

		public void setOpeningHoursRule(BasicOpeningHourRule rule, int position) {
			if (position == -1) {
				openingHours.addRule(rule);
			} else {
				openingHours.getRules().set(position, rule);
			}
			updateViews();
		}

		public void replaceOpeningHours(OpeningHours openingHours) {
			this.openingHours = openingHours;
		}

		public void updateViews() {
			linearLayout.removeAllViews();
			for (int i = 0; i < openingHours.getRules().size(); i++) {
				linearLayout.addView(getView(i));
			}
			if (!data.isInEdit()) {
				LocaleHelper helper = app.getLocaleHelper();
				helper.updateTimeFormatting(false, Locale.getDefault());
				String openingHoursString = openingHours.toString();
				helper.updateTimeFormatting();

				if (!TextUtils.isEmpty(openingHoursString)) {
					if (openingHours.getOriginal() == null ||
							!OpeningHoursParser.parseOpenedHoursHandleErrors(openingHours.getOriginal()).toString().equals(openingHoursString)) {
						data.putTag(OSMTagKey.OPENING_HOURS.getValue(), openingHoursString);
					}
				} else if (basicTagsInitialized && isResumed()) {
					data.removeTag(OSMTagKey.OPENING_HOURS.getValue());
				}
			}
		}

		private View getView(int position) {
			View view = LayoutInflater.from(linearLayout.getContext())
					.inflate(R.layout.open_time_list_item, null, false);

			ImageView clockIconImageView = view.findViewById(R.id.clockIconImageView);
			clockIconImageView.setImageDrawable(clockDrawable);

			TextView daysTextView = view.findViewById(R.id.daysTextView);
			LinearLayout timeListContainer = view.findViewById(R.id.timeListContainer);

			ImageButton deleteItemImageButton = view.findViewById(R.id.deleteItemImageButton);
			Button addTimeSpanButton = view.findViewById(R.id.addTimeSpanButton);

			if (openingHours.getRules().get(position) instanceof BasicOpeningHourRule) {
				BasicOpeningHourRule rule = (BasicOpeningHourRule) openingHours.getRules().get(position);
				StringBuilder stringBuilder = new StringBuilder();
				rule.appendDaysString(stringBuilder);

				daysTextView.setText(stringBuilder.toString());
				daysTextView.setOnClickListener(v -> {
					OpeningHoursDaysDialogFragment fragment =
							OpeningHoursDaysDialogFragment.createInstance(rule, position);
					fragment.show(getChildFragmentManager(), "OpenTimeDialogFragment");
				});

				TIntArrayList startTimes = rule.getStartTimes();
				TIntArrayList endTimes = rule.getEndTimes();
				for (int i = 0; i < startTimes.size(); i++) {
					View timeFromToLayout = LayoutInflater.from(linearLayout.getContext())
							.inflate(R.layout.time_from_to_layout, timeListContainer, false);
					TextView openingTextView = timeFromToLayout.findViewById(R.id.openingTextView);
					openingTextView.setText(Algorithms.formatMinutesDuration(startTimes.get(i)));

					TextView closingTextView = timeFromToLayout.findViewById(R.id.closingTextView);
					closingTextView.setText(Algorithms.formatMinutesDuration(endTimes.get(i)));

					openingTextView.setTag(i);
					openingTextView.setOnClickListener(v -> {
						int index = (int) v.getTag();
						OpeningHoursHoursDialogFragment.createInstance(rule, position, true, index)
								.show(getChildFragmentManager(), "OpeningHoursHoursDialogFragment");
					});
					closingTextView.setTag(i);
					closingTextView.setOnClickListener(v -> {
						int index = (int) v.getTag();
						OpeningHoursHoursDialogFragment.createInstance(rule, position, false, index)
								.show(getChildFragmentManager(), "OpeningHoursHoursDialogFragment");
					});

					ImageButton deleteTimeSpanImageButton = timeFromToLayout
							.findViewById(R.id.deleteTimespanImageButton);
					deleteTimeSpanImageButton.setImageDrawable(deleteDrawable);
					int timeSpanPosition = i;
					deleteTimeSpanImageButton.setOnClickListener(v -> {
						if (startTimes.size() == 1) {
							openingHours.getRules().remove(position);
						} else {
							rule.deleteTimeRange(timeSpanPosition);
						}
						updateViews();
					});
					timeListContainer.addView(timeFromToLayout);
				}

				deleteItemImageButton.setVisibility(View.GONE);
				addTimeSpanButton.setVisibility(View.VISIBLE);
				addTimeSpanButton.setOnClickListener(v -> OpeningHoursHoursDialogFragment.createInstance(rule, position, true,
						startTimes.size()).show(getChildFragmentManager(),
						"TimePickerDialogFragment"));
			} else if (openingHours.getRules().get(position) instanceof UnparseableRule) {
				daysTextView.setText(openingHours.getRules().get(position).toRuleString());
				timeListContainer.removeAllViews();

				deleteItemImageButton.setVisibility(View.VISIBLE);
				deleteItemImageButton.setImageDrawable(deleteDrawable);
				deleteItemImageButton.setOnClickListener(v -> {
					openingHours.getRules().remove(position);
					updateViews();
				});
				addTimeSpanButton.setVisibility(View.GONE);
			}
			return view;
		}
	}
}
