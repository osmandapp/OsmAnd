package net.osmand.plus.mapmarkers;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridView;

import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandTextFieldBoxes;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.widgets.TextViewEx;

import java.util.ArrayList;
import java.util.List;

import studio.carbonylgroup.textfieldboxes.ExtendedEditText;

public class CoordinateInputDialogFragment extends DialogFragment {

	public static final String TAG = "CoordinateInputDialogFragment";

	public static final String COORDINATE_FORMAT = "coordinate_format";

	private static final int DELETE_BUTTON_POSITION = 9;
	private static final int CLEAR_BUTTON_POSITION = 11;
	private static final int DEGREES_MAX_LENGTH = 8;
	private static final int MINUTES_MAX_LENGTH = 10;
	private static final int SECONDS_MAX_LENGTH = 13;

	private boolean lightTheme;
	private EditText focusedEditText;
	private boolean useOsmandKeyboard = true;
	private List<OsmandTextFieldBoxes> textFieldBoxes;
	private ExtendedEditText nameEditText;
	private List<ExtendedEditText> extendedLatLonEditTexts;
	private int coordinateFormat = -1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		OsmandApplication app = getMyApplication();
		lightTheme = app.getSettings().OSMAND_THEME.get() == OsmandSettings.OSMAND_LIGHT_THEME;
		int themeId = lightTheme ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme;
		setStyle(STYLE_NO_FRAME, themeId);

		if (coordinateFormat == -1) {
			CoordinateInputBottomSheetDialogFragment fragment = new CoordinateInputBottomSheetDialogFragment();
			fragment.setListener(createCoordinateInputFormatChangeListener());
			fragment.show(getMapActivity().getSupportFragmentManager(), CoordinateInputBottomSheetDialogFragment.TAG);
		}
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		final View mainView = inflater.inflate(R.layout.fragment_coordinate_input_dialog, container);
		final MapActivity mapActivity = getMapActivity();

		if (coordinateFormat == -1) {
			Fragment coordinateInputBottomSheetDialogFragment = mapActivity.getSupportFragmentManager().findFragmentByTag(CoordinateInputBottomSheetDialogFragment.TAG);
			if (coordinateInputBottomSheetDialogFragment != null) {
				((CoordinateInputBottomSheetDialogFragment) coordinateInputBottomSheetDialogFragment).setListener(createCoordinateInputFormatChangeListener());
			}
		}

		Toolbar toolbar = (Toolbar) mainView.findViewById(R.id.coordinate_input_toolbar);

		toolbar.setNavigationIcon(getMyApplication().getIconsCache().getIcon(R.drawable.ic_arrow_back));
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dismiss();
			}
		});
		final View optionsButton = mainView.findViewById(R.id.options_button);
		optionsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				CoordinateInputBottomSheetDialogFragment fragment = new CoordinateInputBottomSheetDialogFragment();
				Bundle args = new Bundle();
				args.putInt(COORDINATE_FORMAT, coordinateFormat);
				fragment.setArguments(args);
				fragment.setListener(createCoordinateInputFormatChangeListener());
				fragment.show(getMapActivity().getSupportFragmentManager(), CoordinateInputBottomSheetDialogFragment.TAG);
			}
		});

		textFieldBoxes = new ArrayList<>();
		final OsmandTextFieldBoxes latitudeBox = (OsmandTextFieldBoxes) mainView.findViewById(R.id.latitude_box);
		textFieldBoxes.add(latitudeBox);
		final OsmandTextFieldBoxes longitudeBox = (OsmandTextFieldBoxes) mainView.findViewById(R.id.longitude_box);
		textFieldBoxes.add(longitudeBox);
		final OsmandTextFieldBoxes nameBox = (OsmandTextFieldBoxes) mainView.findViewById(R.id.name_box);
		textFieldBoxes.add(nameBox);

		extendedLatLonEditTexts = new ArrayList<>();
		final ExtendedEditText latitudeEditText = (ExtendedEditText) mainView.findViewById(R.id.latitude_edit_text);
		extendedLatLonEditTexts.add(latitudeEditText);
		final ExtendedEditText longitudeEditText = (ExtendedEditText) mainView.findViewById(R.id.longitude_edit_text);
		extendedLatLonEditTexts.add(longitudeEditText);
		nameEditText = (ExtendedEditText) mainView.findViewById(R.id.name_edit_text);

		final View.OnFocusChangeListener focusChangeListener = new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View view, boolean b) {
				ExtendedEditText editText = null;
				OsmandTextFieldBoxes fieldBox = null;
				switch (view.getId()) {
					case R.id.latitude_edit_text:
						editText = latitudeEditText;
						fieldBox = latitudeBox;
						break;
					case R.id.longitude_edit_text:
						editText = longitudeEditText;
						fieldBox = longitudeBox;
						break;
					case R.id.name_edit_text:
						editText = nameEditText;
						fieldBox = nameBox;
						break;
				}
				if (fieldBox != null) {
					if (b) {
						fieldBox.setHasFocus(true);
						focusedEditText = editText;
					} else {
						fieldBox.setHasFocus(false);
						focusedEditText = null;
					}
				}
			}
		};

		TextWatcher textWatcher = new TextWatcher() {
			int len = 0;

			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
				if (focusedEditText != null) {
					String str = focusedEditText.getText().toString();
					len = str.length();
				}
			}

			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void afterTextChanged(Editable editable) {
				if (focusedEditText != null && focusedEditText != nameEditText) {
					String str = focusedEditText.getText().toString();
					int strLength = str.length();
					if (strLength == 2 && len < strLength) {
						String strToAppend;
						if (coordinateFormat == PointDescription.FORMAT_DEGREES) {
							strToAppend = ".";
						} else {
							strToAppend = ":";
						}
						focusedEditText.append(strToAppend);
					} else if (strLength == 5 && coordinateFormat != PointDescription.FORMAT_DEGREES && len < strLength) {
						String strToAppend;
						if (coordinateFormat == PointDescription.FORMAT_MINUTES) {
							strToAppend = ".";
						} else {
							strToAppend = ":";
						}
						focusedEditText.append(strToAppend);
					} else if (strLength == 8 && coordinateFormat == PointDescription.FORMAT_SECONDS && len < strLength) {
						focusedEditText.append(".");
					} else if ((strLength == DEGREES_MAX_LENGTH && coordinateFormat == PointDescription.FORMAT_DEGREES)
							|| (strLength == MINUTES_MAX_LENGTH && coordinateFormat == PointDescription.FORMAT_MINUTES)
							|| (strLength == SECONDS_MAX_LENGTH && coordinateFormat == PointDescription.FORMAT_SECONDS)) {
						if (focusedEditText == latitudeEditText) {
							longitudeBox.select();
						} else {
							nameBox.select();
						}
					}
				}
			}
		};

		latitudeEditText.setOnFocusChangeListener(focusChangeListener);
		longitudeEditText.setOnFocusChangeListener(focusChangeListener);
		nameEditText.setOnFocusChangeListener(focusChangeListener);

		latitudeEditText.addTextChangedListener(textWatcher);
		longitudeEditText.addTextChangedListener(textWatcher);
		nameEditText.addTextChangedListener(textWatcher);

		changeKeyboardInBoxes(useOsmandKeyboard);
		changeKeyboardInEditTexts(useOsmandKeyboard);

		String[] keyboardItems = new String[] { "1", "2", "3",
				"4", "5", "6",
				"7", "8", "9",
				getString(R.string.shared_string_delete), "0", getString(R.string.shared_string_clear) };
		final GridView keyboardGrid = (GridView) mainView.findViewById(R.id.keyboard_grid_view);
		final KeyboardAdapter keyboardAdapter = new KeyboardAdapter(mapActivity, keyboardItems);
		keyboardGrid.setAdapter(keyboardAdapter);
		keyboardGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
				if (focusedEditText != null) {
					switch (i) {
						case DELETE_BUTTON_POSITION:
							String str = focusedEditText.getText().toString();
							if (str.length() > 0) {
								str = str.substring(0, str.length() - 1);
								focusedEditText.setText(str);
							}
							break;
						case CLEAR_BUTTON_POSITION:
							focusedEditText.setText("");
							break;
						default:
							focusedEditText.append(keyboardAdapter.getItem(i));
					}
				}
			}
		});

		return mainView;
	}

	@Override
	public void onDestroyView() {
		focusedEditText = null;
		if (getDialog() != null && getRetainInstance()) {
			getDialog().setDismissMessage(null);
		}
		super.onDestroyView();
	}

	private CoordinateInputBottomSheetDialogFragment.CoordinateInputFormatChangeListener createCoordinateInputFormatChangeListener() {
		return new CoordinateInputBottomSheetDialogFragment.CoordinateInputFormatChangeListener() {
			@Override
			public void onCoordinateFormatChanged(int format) {
				coordinateFormat = format;
				changeEditTextLengths();
			}
		};
	}

	private void changeEditTextLengths() {
		int maxLength;
		if (coordinateFormat == PointDescription.FORMAT_DEGREES) {
			maxLength = DEGREES_MAX_LENGTH;
		} else if (coordinateFormat == PointDescription.FORMAT_MINUTES) {
			maxLength = MINUTES_MAX_LENGTH;
		} else {
			maxLength = SECONDS_MAX_LENGTH;
		}
		InputFilter[] filtersArray = new InputFilter[] {new InputFilter.LengthFilter(maxLength)};
		for (ExtendedEditText extendedEditText : extendedLatLonEditTexts) {
			extendedEditText.setFilters(filtersArray);
		}
	}

	public void changeKeyboardInBoxes(boolean useOsmandKeyboard) {
		for (OsmandTextFieldBoxes textFieldBox : textFieldBoxes) {
			textFieldBox.setUseOsmandKeyboard(useOsmandKeyboard);
		}
	}

	public void changeKeyboardInEditTexts(boolean useOsmandKeyboard) {
		for (ExtendedEditText extendedEditText : extendedLatLonEditTexts) {
			extendedEditText.setInputType(useOsmandKeyboard ? InputType.TYPE_NULL : InputType.TYPE_CLASS_TEXT);
		}
		nameEditText.setInputType(useOsmandKeyboard ? InputType.TYPE_NULL : InputType.TYPE_CLASS_TEXT);
	}

	private MapActivity getMapActivity() {
		return (MapActivity) getActivity();
	}

	private OsmandApplication getMyApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}

	public static boolean showInstance(@NonNull MapActivity mapActivity) {
		try {
			if (mapActivity.isActivityDestroyed()) {
				return false;
			}
			CoordinateInputDialogFragment fragment = new CoordinateInputDialogFragment();
			fragment.setRetainInstance(true);
			fragment.show(mapActivity.getSupportFragmentManager(), TAG);
			return true;
		} catch (RuntimeException e) {
			return false;
		}
	}

	private class KeyboardAdapter extends ArrayAdapter<String> {

		KeyboardAdapter(@NonNull Context context, @NonNull String[] objects) {
			super(context, 0, objects);
		}

		@NonNull
		@Override
		public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
			if (convertView == null) {
				convertView = LayoutInflater.from(getContext()).inflate(R.layout.input_coordinate_keyboard_item, parent, false);
			}
			convertView.setBackgroundResource(lightTheme ? R.drawable.keyboard_item_light_bg : R.drawable.keyboard_item_dark_bg);

			TextViewEx keyboardItem = (TextViewEx) convertView.findViewById(R.id.keyboard_item);
//			if (position == DELETE_BUTTON_POSITION || position == CLEAR_BUTTON_POSITION) {
//				keyboardItem.setTextSize(getResources().getDimension(R.dimen.default_list_text_size));
//			} else {
//				keyboardItem.setTextSize(getResources().getDimension(R.dimen.map_button_text_size));
//			}

			keyboardItem.setText(getItem(position));

			return convertView;
		}
	}

}
