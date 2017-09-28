package net.osmand.plus.mapmarkers;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandTextFieldBoxes;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.widgets.TextViewEx;

import java.util.ArrayList;
import java.util.List;

import studio.carbonylgroup.textfieldboxes.ExtendedEditText;

public class CoordinateInputDialogFragment extends DialogFragment {

	private static final int DELETE_BUTTON_POSITION = 9;
	private static final int CLEAR_BUTTON_POSITION = 11;

	public static final String TAG = "CoordinateInputDialogFragment";

	private boolean lightTheme;
	private EditText focusedEditText;
	private boolean useOsmandKeyboard = true;
	private List<OsmandTextFieldBoxes> textFieldBoxes = new ArrayList<>();
	private List<ExtendedEditText> extendedEditTexts = new ArrayList<>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		OsmandApplication app = getMyApplication();
		lightTheme = app.getSettings().OSMAND_THEME.get() == OsmandSettings.OSMAND_LIGHT_THEME;
		int themeId = lightTheme ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme;
		setStyle(STYLE_NO_FRAME, themeId);
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		final View mainView = inflater.inflate(R.layout.fragment_coordinate_input_dialog, container);
		final MapActivity mapActivity = getMapActivity();

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

			}
		});

		final OsmandTextFieldBoxes latitudeBox = (OsmandTextFieldBoxes) mainView.findViewById(R.id.latitude_box);
		textFieldBoxes.add(latitudeBox);
		final OsmandTextFieldBoxes longitudeBox = (OsmandTextFieldBoxes) mainView.findViewById(R.id.longitude_box);
		textFieldBoxes.add(longitudeBox);
		final OsmandTextFieldBoxes nameBox = (OsmandTextFieldBoxes) mainView.findViewById(R.id.name_box);
		textFieldBoxes.add(nameBox);

		final ExtendedEditText latitudeEditText = (ExtendedEditText) mainView.findViewById(R.id.latitude_edit_text);
		extendedEditTexts.add(latitudeEditText);
		final ExtendedEditText longitudeEditText = (ExtendedEditText) mainView.findViewById(R.id.longitude_edit_text);
		extendedEditTexts.add(longitudeEditText);
		final ExtendedEditText nameEditText = (ExtendedEditText) mainView.findViewById(R.id.name_edit_text);
		extendedEditTexts.add(nameEditText);

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
				if (focusedEditText != null) {
					String str = focusedEditText.getText().toString();
					if(str.length() == 2 && len < str.length()){
						focusedEditText.append(":");
					} else if (str.length() == 5 && len < str.length()) {
						focusedEditText.append(".");
					} else if (str.length() == 10) {
						if (focusedEditText == latitudeEditText) {
							longitudeBox.select();
						} else if (focusedEditText == longitudeEditText) {
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

	public void changeKeyboardInBoxes(boolean useOsmandKeyboard) {
		for (OsmandTextFieldBoxes textFieldBox : textFieldBoxes) {
			textFieldBox.setUseOsmandKeyboard(useOsmandKeyboard);
		}
	}

	public void changeKeyboardInEditTexts(boolean useOsmandKeyboard) {
		for (ExtendedEditText extendedEditText : extendedEditTexts) {
			extendedEditText.setInputType(useOsmandKeyboard ? InputType.TYPE_NULL : InputType.TYPE_CLASS_TEXT);
		}
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
