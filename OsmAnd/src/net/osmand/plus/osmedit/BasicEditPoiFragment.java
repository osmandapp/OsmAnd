package net.osmand.plus.osmedit;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
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
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.osmedit.dialogs.OpeningHoursDaysDialogFragment;
import net.osmand.plus.osmedit.dialogs.OpeningHoursHoursDialogFragment;
import net.osmand.util.Algorithms;
import net.osmand.util.OpeningHoursParser;
import net.osmand.util.OpeningHoursParser.BasicOpeningHourRule;

import org.apache.commons.logging.Log;

import gnu.trove.list.array.TIntArrayList;

public class BasicEditPoiFragment extends BaseOsmAndFragment
		implements EditPoiDialogFragment.OnFragmentActivatedListener {
	private static final Log LOG = PlatformUtil.getLog(BasicEditPoiFragment.class);
	private static final String OPENING_HOURS = "opening_hours";
	private EditText streetEditText;
	private EditText houseNumberEditText;
	private EditText phoneEditText;
	private EditText webSiteEditText;
	private EditText descriptionEditText;
	OpeningHoursAdapter mOpeningHoursAdapter;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_edit_poi_normal, container, false);

		TypedValue typedValue = new TypedValue();
		Resources.Theme theme = getActivity().getTheme();
		theme.resolveAttribute(android.R.attr.textColorSecondary, typedValue, true);
		int iconColor = typedValue.data;

		ImageView streetImageView = (ImageView) view.findViewById(R.id.streetImageView);
		streetImageView.setImageDrawable(
				getPaintedContentIcon(R.drawable.ic_action_street_name, iconColor));
		ImageView houseNumberImageView = (ImageView) view.findViewById(R.id.houseNumberImageView);
		houseNumberImageView.setImageDrawable(
				getPaintedContentIcon(R.drawable.ic_action_building_number, iconColor));
		ImageView phoneImageView = (ImageView) view.findViewById(R.id.phoneImageView);
		phoneImageView.setImageDrawable(
				getPaintedContentIcon(R.drawable.ic_action_call_dark, iconColor));
		ImageView webSiteImageView = (ImageView) view.findViewById(R.id.webSiteImageView);
		webSiteImageView.setImageDrawable(
				getPaintedContentIcon(R.drawable.ic_world_globe_dark, iconColor));
		ImageView descriptionImageView = (ImageView) view.findViewById(R.id.descriptionImageView);
		descriptionImageView.setImageDrawable(
				getPaintedContentIcon(R.drawable.ic_action_description, iconColor));
		ImageView openingHoursImageView = (ImageView) view.findViewById(R.id.openingHoursImageView);
		openingHoursImageView.setImageDrawable(
				getPaintedContentIcon(R.drawable.ic_action_time, iconColor));

		streetEditText = (EditText) view.findViewById(R.id.streetEditText);
		houseNumberEditText = (EditText) view.findViewById(R.id.houseNumberEditText);
		phoneEditText = (EditText) view.findViewById(R.id.phoneEditText);
		webSiteEditText = (EditText) view.findViewById(R.id.webSiteEditText);
		descriptionEditText = (EditText) view.findViewById(R.id.descriptionEditText);
		addTextWatcher(OSMSettings.OSMTagKey.ADDR_STREET.getValue(), streetEditText);
		addTextWatcher(OSMSettings.OSMTagKey.WEBSITE.getValue(), webSiteEditText);
		addTextWatcher(OSMSettings.OSMTagKey.PHONE.getValue(), phoneEditText);
		addTextWatcher(OSMSettings.OSMTagKey.ADDR_HOUSE_NUMBER.getValue(), houseNumberEditText);
		addTextWatcher(OSMSettings.OSMTagKey.DESCRIPTION.getValue(), descriptionEditText);
		Button addOpeningHoursButton = (Button) view.findViewById(R.id.addOpeningHoursButton);
		addOpeningHoursButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				BasicOpeningHourRule rule = new BasicOpeningHourRule();
				rule.setStartTime(9 * 60);
				rule.setEndTime(18 * 60);
				OpeningHoursDaysDialogFragment fragment = OpeningHoursDaysDialogFragment.createInstance(rule, -1);
				fragment.show(getChildFragmentManager(), "OpenTimeDialogFragment");
			}
		});
		LinearLayout openHoursContainer = (LinearLayout) view.findViewById(R.id.openHoursContainer);
		Drawable clockDrawable = getPaintedContentIcon(R.drawable.ic_action_time, iconColor);
		Drawable deleteDrawable = getPaintedContentIcon(R.drawable.ic_action_remove_dark, iconColor);
		if (savedInstanceState != null && savedInstanceState.containsKey(OPENING_HOURS)) {
			mOpeningHoursAdapter = new OpeningHoursAdapter(
					(OpeningHoursParser.OpeningHours) savedInstanceState.getSerializable(OPENING_HOURS),
					openHoursContainer, getData(), clockDrawable, deleteDrawable);
			mOpeningHoursAdapter.updateViews();
		} else {
			mOpeningHoursAdapter = new OpeningHoursAdapter(new OpeningHoursParser.OpeningHours(),
					openHoursContainer, getData(), clockDrawable, deleteDrawable);
		}
		onFragmentActivated();
		return view;
	}

	protected void addTextWatcher(final String tag, final EditText e) {
		e.addTextChangedListener(new TextWatcher() {

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				if (!getData().isInEdit()) {
					if (!TextUtils.isEmpty(s)) {
						getData().putTag(tag, s.toString());
					} else {
						getData().removeTag(tag);
					}
				}
			}

		});
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putSerializable(OPENING_HOURS, mOpeningHoursAdapter.openingHours);
		super.onSaveInstanceState(outState);
	}

	public void setBasicOpeningHoursRule(BasicOpeningHourRule item, int position) {
		mOpeningHoursAdapter.setOpeningHoursRule(item, position);
	}


	private EditPoiDialogFragment getEditPoiFragment() {
		return (EditPoiDialogFragment) getParentFragment();
	}

	private EditPoiData getData() {
		return getEditPoiFragment().getEditPoiData();
	}

	@Override
	public void onFragmentActivated() {
		streetEditText.setText(getData().getTagValues()
				.get(OSMSettings.OSMTagKey.ADDR_STREET.getValue()));
		houseNumberEditText.setText(getData().getTagValues()
				.get(OSMSettings.OSMTagKey.ADDR_HOUSE_NUMBER.getValue()));
		phoneEditText.setText(getData().getTagValues()
				.get(OSMSettings.OSMTagKey.PHONE.getValue()));
		webSiteEditText.setText(getData().getTagValues()
				.get(OSMSettings.OSMTagKey.WEBSITE.getValue()));
		descriptionEditText.setText(getData().getTagValues()
				.get(OSMSettings.OSMTagKey.DESCRIPTION.getValue()));

		OpeningHoursParser.OpeningHours openingHours =
				OpeningHoursParser.parseOpenedHoursHandleErrors(getData().getTagValues()
						.get(OSMSettings.OSMTagKey.OPENING_HOURS.getValue()));
		if (openingHours == null) {
			openingHours = new OpeningHoursParser.OpeningHours();
		}
		mOpeningHoursAdapter.replaceOpeningHours(openingHours);
		mOpeningHoursAdapter.updateViews();
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
			if (!data.isInEdit()) {
				String openingHoursString = openingHours.toStringNoMonths();
				if (!TextUtils.isEmpty(openingHoursString)) {
					data.putTag(OSMSettings.OSMTagKey.OPENING_HOURS.getValue(),
							openingHoursString);
				} else {
					data.removeTag(OSMSettings.OSMTagKey.OPENING_HOURS.getValue());
				}
			}
		}

		private View getView(final int position) {
			final View view = LayoutInflater.from(linearLayout.getContext())
					.inflate(R.layout.open_time_list_item, null, false);

			ImageView clockIconImageView = (ImageView) view.findViewById(R.id.clockIconImageView);
			clockIconImageView.setImageDrawable(clockDrawable);

			TextView daysTextView = (TextView) view.findViewById(R.id.daysTextView);
			LinearLayout timeListContainer = (LinearLayout) view.findViewById(R.id.timeListContainer);

			ImageButton deleteItemImageButton = (ImageButton) view.findViewById(R.id.deleteItemImageButton);
			Button addTimeSpanButton = (Button) view.findViewById(R.id.addTimeSpanButton);

			if (openingHours.getRules().get(position) instanceof BasicOpeningHourRule) {
				final OpeningHoursParser.BasicOpeningHourRule rule =
						(BasicOpeningHourRule) openingHours.getRules().get(position);
				StringBuilder stringBuilder = new StringBuilder();
				rule.appendDaysString(stringBuilder);

				daysTextView.setText(stringBuilder.toString());
				daysTextView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						OpeningHoursDaysDialogFragment fragment =
								OpeningHoursDaysDialogFragment.createInstance(rule, position);
						fragment.show(getChildFragmentManager(), "OpenTimeDialogFragment");
					}
				});

				final TIntArrayList startTimes = rule.getStartTimes();
				final TIntArrayList endTimes = rule.getEndTimes();
				for (int i = 0; i < startTimes.size(); i++) {
					View timeFromToLayout = LayoutInflater.from(linearLayout.getContext())
							.inflate(R.layout.time_from_to_layout, timeListContainer, false);
					TextView openingTextView =
							(TextView) timeFromToLayout.findViewById(R.id.openingTextView);
					openingTextView.setText(Algorithms.formatMinutesDuration(startTimes.get(i)));

					TextView closingTextView =
							(TextView) timeFromToLayout.findViewById(R.id.closingTextView);
					closingTextView.setText(Algorithms.formatMinutesDuration(endTimes.get(i)));

					openingTextView.setTag(i);
					openingTextView.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							int index = (int) v.getTag();
							OpeningHoursHoursDialogFragment.createInstance(rule, position, true, index)
									.show(getChildFragmentManager(), "OpeningHoursHoursDialogFragment");
						}
					});
					closingTextView.setTag(i);
					closingTextView.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							int index = (int) v.getTag();
							OpeningHoursHoursDialogFragment.createInstance(rule, position, false, index)
									.show(getChildFragmentManager(), "OpeningHoursHoursDialogFragment");
						}
					});

					ImageButton deleteTimespanImageButton = (ImageButton) timeFromToLayout
							.findViewById(R.id.deleteTimespanImageButton);
					deleteTimespanImageButton.setImageDrawable(deleteDrawable);
					final int timespanPosition = i;
					deleteTimespanImageButton.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							if (startTimes.size() == 1) {
								openingHours.getRules().remove(position);
								updateViews();
							} else {
								rule.deleteTimeRange(timespanPosition);
								updateViews();
							}
						}
					});
					timeListContainer.addView(timeFromToLayout);
				}

				deleteItemImageButton.setVisibility(View.GONE);
				addTimeSpanButton.setVisibility(View.VISIBLE);
				addTimeSpanButton.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						OpeningHoursHoursDialogFragment.createInstance(rule, position, true,
								startTimes.size()).show(getChildFragmentManager(),
								"TimePickerDialogFragment");
					}
				});
			} else if (openingHours.getRules().get(position) instanceof OpeningHoursParser.UnparseableRule) {
				daysTextView.setText(openingHours.getRules().get(position).toRuleString(false));
				timeListContainer.removeAllViews();

				deleteItemImageButton.setVisibility(View.VISIBLE);
				deleteItemImageButton.setImageDrawable(deleteDrawable);
				deleteItemImageButton.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						openingHours.getRules().remove(position);
						updateViews();
					}
				});
				addTimeSpanButton.setVisibility(View.GONE);
			}
			return view;
		}
	}
}
