package net.osmand.plus.osmedit;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TimePicker;

import net.osmand.osm.edit.OSMSettings;
import net.osmand.plus.IconsCache;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.osmedit.data.EditPoiData;
import net.osmand.plus.osmedit.data.Tag;
import net.osmand.util.OpeningHoursParser.BasicOpeningHourRule;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class NormalDataFragment extends Fragment {
	private static final String TAG = "NormalDataFragment";
	private EditText streetEditText;
	private EditText houseNumberEditText;
	private EditText phoneEditText;
	private EditText webSiteEditText;
	private EditText descriptionEditText;
	private EditPoiData.TagsChangedListener mTagsChangedListener;
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
		Button addOpeningHoursButton = (Button) view.findViewById(R.id.addOpeningHoursButton);
		addOpeningHoursButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				BasicOpeningHourRule r = new BasicOpeningHourRule();
				DaysDialogFragment fragment = DaysDialogFragment.createInstance(r, -1);
				fragment.show(getChildFragmentManager(), "OpenTimeDialogFragment");
			}
		});
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

	public static class DaysDialogFragment extends DialogFragment {
		public static final String POSITION_TO_ADD = "position_to_add";
		public static final String ITEM = "item";

		@NonNull
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			Log.v(TAG, "onCreateDialog(" + "savedInstanceState=" + savedInstanceState + ")" + getArguments());
			final BasicOpeningHourRule item =
					(BasicOpeningHourRule) getArguments().getSerializable(ITEM);
			final int positionToAdd = getArguments().getInt(POSITION_TO_ADD);

			AlertDialog.Builder b = new AlertDialog.Builder(getActivity());

			boolean add = positionToAdd > -1;
			Calendar inst = Calendar.getInstance();
			final int first = inst.getFirstDayOfWeek();
			final boolean[] dayToShow = new boolean[7];
			String[] daysToShow = new String[7];
			for (int i = 0; i < 7; i++) {
				int d = (first + i - 1) % 7 + 1;
				inst.set(Calendar.DAY_OF_WEEK, d);
				daysToShow[i] = DateFormat.format("EEEE", inst).toString(); //$NON-NLS-1$
				final int pos = (d + 5) % 7;
				dayToShow[i] = item.getDays()[pos];
			}
			b.setMultiChoiceItems(daysToShow, dayToShow, new DialogInterface.OnMultiChoiceClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which, boolean isChecked) {
					dayToShow[which] = isChecked;

				}

			});
			b.setPositiveButton(add ? getActivity().getString(R.string.shared_string_add)
							: getActivity().getString(R.string.shared_string_apply),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							boolean[] days = item.getDays();
							for (int i = 0; i < 7; i++) {
								days[(first + 5 + i) % 7] = dayToShow[i];
							}
							TimePickerDialogFragment.createInstance(null, true)
									.show(getFragmentManager(), "TimePickerDialogFragment");
							if (positionToAdd != -1) {

//								time.insert(item, positionToAdd);
//								selectedRule = positionToAdd;
							} else {
//								time.notifyDataSetChanged();
							}
//							updateTimePickers();

						}

					});

			b.setNegativeButton(getActivity().getString(R.string.shared_string_cancel), null);

			return b.create();
		}

		public static DaysDialogFragment createInstance(final BasicOpeningHourRule item,
														final int positionToAdd) {
			DaysDialogFragment daysDialogFragment = new DaysDialogFragment();
			Bundle bundle = new Bundle();
			bundle.putSerializable(ITEM, item);
			bundle.putInt(POSITION_TO_ADD, positionToAdd);
			daysDialogFragment.setArguments(bundle);
			return daysDialogFragment;
		}
	}

	public static class TimePickerDialogFragment extends DialogFragment {
		public static final String INITIAL_TIME = "initial_time";
		public static final String IS_START = "is_start";

		@NonNull
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			Bundle args = getArguments();
			Calendar initialState = (Calendar) args.getSerializable(INITIAL_TIME);
			if (initialState == null) initialState = Calendar.getInstance();
			TimePickerDialog.OnTimeSetListener callback = new TimePickerDialog.OnTimeSetListener() {
				@Override
				public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
					TimePickerDialogFragment.createInstance(null, false)
							.show(getFragmentManager(), "TimePickerDialogFragment");
				}
			};
			TimePickerDialog timePickerDialog = new TimePickerDialog(getActivity(), callback,
					initialState.get(Calendar.HOUR_OF_DAY),
					initialState.get(Calendar.MINUTE),
					DateFormat.is24HourFormat(getActivity()));
			boolean isStart = args.getBoolean(IS_START);
			timePickerDialog.setTitle(isStart ? "Opening" : " Closing");
			return timePickerDialog;
		}

		public static TimePickerDialogFragment createInstance(Calendar initialTime,
															  boolean isStart) {
			TimePickerDialogFragment fragment = new TimePickerDialogFragment();
			Bundle bundle = new Bundle();
			bundle.putBoolean(IS_START, isStart);
			bundle.putSerializable(INITIAL_TIME, initialTime);
			fragment.setArguments(bundle);
			return fragment;
		}
	}
}
