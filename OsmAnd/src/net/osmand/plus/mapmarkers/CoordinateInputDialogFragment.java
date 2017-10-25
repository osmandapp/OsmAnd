package net.osmand.plus.mapmarkers;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.data.PointDescription;
import net.osmand.plus.IconsCache;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandTextFieldBoxes;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.widgets.IconPopupMenu;

import java.util.ArrayList;
import java.util.List;

import studio.carbonylgroup.textfieldboxes.ExtendedEditText;

import static android.content.ClipDescription.MIMETYPE_TEXT_PLAIN;
import static android.content.Context.CLIPBOARD_SERVICE;

public class CoordinateInputDialogFragment extends DialogFragment {

	public static final String TAG = "CoordinateInputDialogFragment";

	public static final String COORDINATE_FORMAT = "coordinate_format";
	public static final String USE_OSMAND_KEYBOARD = "use_osmand_keyboard";

	private static final int DELETE_BUTTON_POSITION = 9;
	private static final int CLEAR_BUTTON_POSITION = 11;
	private static final int DEGREES_MAX_LENGTH = 6;
	private static final int MINUTES_MAX_LENGTH = 9;
	private static final int SECONDS_MAX_LENGTH = 12;
	private static final String DEGREES_HINT = "50.000";
	private static final String MINUTES_HINT = "50:00.000";
	private static final String SECONDS_HINT = "50:00:00.000";
	private static final String LATITUDE_LABEL = "latitude";
	private static final String LONGITUDE_LABEL = "longitude";
	private static final String NAME_LABEL = "name";

	private boolean lightTheme;
	private boolean useOsmandKeyboard = true;
	private int coordinateFormat = -1;
	private List<OsmandTextFieldBoxes> textFieldBoxes;
	private List<ExtendedEditText> extendedEditTexts;
	private View mainView;
	private IconsCache iconsCache;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		OsmandApplication app = getMyApplication();
		lightTheme = app.getSettings().OSMAND_THEME.get() == OsmandSettings.OSMAND_LIGHT_THEME;
		int themeId = lightTheme ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme;
		setStyle(STYLE_NO_FRAME, themeId);

		CoordinateInputBottomSheetDialogFragment fragment = new CoordinateInputBottomSheetDialogFragment();
		fragment.setListener(createCoordinateInputFormatChangeListener());
		fragment.show(getMapActivity().getSupportFragmentManager(), CoordinateInputBottomSheetDialogFragment.TAG);
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		mainView = inflater.inflate(R.layout.fragment_coordinate_input_dialog, container);
		final MapActivity mapActivity = getMapActivity();
		iconsCache = getMyApplication().getIconsCache();

		Fragment coordinateInputBottomSheetDialogFragment = mapActivity.getSupportFragmentManager().findFragmentByTag(CoordinateInputBottomSheetDialogFragment.TAG);
		if (coordinateInputBottomSheetDialogFragment != null) {
			((CoordinateInputBottomSheetDialogFragment) coordinateInputBottomSheetDialogFragment).setListener(createCoordinateInputFormatChangeListener());
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
				View focusedView = getDialog().getCurrentFocus();
				if (focusedView != null && focusedView instanceof ExtendedEditText) {
					focusedView.clearFocus();
					AndroidUtils.hideSoftKeyboard(getMapActivity(), focusedView);
				}
				CoordinateInputBottomSheetDialogFragment fragment = new CoordinateInputBottomSheetDialogFragment();
				Bundle args = new Bundle();
				args.putInt(COORDINATE_FORMAT, coordinateFormat);
				args.putBoolean(USE_OSMAND_KEYBOARD, useOsmandKeyboard);
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
		nameBox.setEndIcon(iconsCache.getThemedIcon(R.drawable.ic_action_keyboard));
		nameBox.getEndIconImageButton().setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				View focusedView = getDialog().getCurrentFocus();
				if (focusedView != null) {
					if (isOsmandKeyboardCurrentlyVisible()) {
						changeOsmandKeyboardVisibility(false);
					}
					AndroidUtils.showSoftKeyboard(focusedView);
				}
			}
		});
		textFieldBoxes.add(nameBox);

		registerTextFieldBoxes();

		extendedEditTexts = new ArrayList<>();
		final ExtendedEditText latitudeEditText = (ExtendedEditText) mainView.findViewById(R.id.latitude_edit_text);
		extendedEditTexts.add(latitudeEditText);
		final ExtendedEditText longitudeEditText = (ExtendedEditText) mainView.findViewById(R.id.longitude_edit_text);
		extendedEditTexts.add(longitudeEditText);
		final ExtendedEditText nameEditText = (ExtendedEditText) mainView.findViewById(R.id.name_edit_text);
		extendedEditTexts.add(nameEditText);

		registerEditTexts();

		changeKeyboardInBoxes();

		View keyboardLayout = mainView.findViewById(R.id.keyboard_layout);
		AndroidUtils.setBackground(mapActivity, keyboardLayout, !lightTheme, R.drawable.bg_bottom_menu_light, R.drawable.bg_bottom_menu_dark);

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
				View focusedView = getDialog().getCurrentFocus();
				if (focusedView != null && focusedView instanceof ExtendedEditText) {
					ExtendedEditText extendedEditText = (ExtendedEditText) focusedView;
					switch (i) {
						case DELETE_BUTTON_POSITION:
							String str = extendedEditText.getText().toString();
							if (str.length() > 0) {
								str = str.substring(0, str.length() - 1);
								extendedEditText.setText(str);
								extendedEditText.setSelection(str.length());
							}
							break;
						case CLEAR_BUTTON_POSITION:
							extendedEditText.setText("");
							break;
						default:
							extendedEditText.append(keyboardAdapter.getItem(i));
					}
				}
			}
		});

		final ImageView showHideKeyboardIcon = (ImageView) mainView.findViewById(R.id.show_hide_keyboard_icon);
		showHideKeyboardIcon.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_arrow_down));
		showHideKeyboardIcon.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				boolean isCurrentlyVisible = isOsmandKeyboardCurrentlyVisible();
				View focusedView = getDialog().getCurrentFocus();
				if (focusedView != null && !isCurrentlyVisible) {
					AndroidUtils.hideSoftKeyboard(getActivity(), focusedView);
				}
				changeOsmandKeyboardVisibility(!isCurrentlyVisible);
			}
		});

		return mainView;
	}

	@Override
	public void onDestroyView() {
		if (getDialog() != null && getRetainInstance()) {
			getDialog().setDismissMessage(null);
		}
		super.onDestroyView();
	}

	private void registerTextFieldBoxes() {
		View.OnTouchListener textFieldBoxOnTouchListener = new View.OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent motionEvent) {
				if (!useOsmandKeyboard && isOsmandKeyboardCurrentlyVisible()) {
					changeOsmandKeyboardVisibility(false);
				}
				return false;
			}
		};

		for (OsmandTextFieldBoxes textFieldBox : textFieldBoxes) {
			textFieldBox.getPanel().setOnTouchListener(textFieldBoxOnTouchListener);
		}
	}

	private void registerEditTexts() {
		TextWatcher textWatcher = new TextWatcher() {
			int len = 0;

			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
				View focusedView = getDialog().getCurrentFocus();
				if (focusedView != null && focusedView instanceof ExtendedEditText) {
					String str = ((ExtendedEditText) focusedView).getText().toString();
					len = str.length();
				}
			}

			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void afterTextChanged(Editable editable) {
				View focusedView = getDialog().getCurrentFocus();
				if (focusedView != null) {
					ExtendedEditText focusedEditText = (ExtendedEditText) focusedView;
					String str = focusedEditText.getText().toString();
					int strLength = str.length();
					if (strLength == 2 && len < strLength) {
						String strToAppend;
						if (coordinateFormat == PointDescription.FORMAT_DEGREES) {
							strToAppend = ".";
						} else {
							strToAppend = ":";
						}
						focusedEditText.setText(str + strToAppend);
						focusedEditText.setSelection(strLength + 1);
					} else if (strLength == 5 && coordinateFormat != PointDescription.FORMAT_DEGREES && len < strLength) {
						String strToAppend;
						if (coordinateFormat == PointDescription.FORMAT_MINUTES) {
							strToAppend = ".";
						} else {
							strToAppend = ":";
						}
						focusedEditText.setText(str + strToAppend);
						focusedEditText.setSelection(strLength + 1);
					} else if (strLength == 8 && coordinateFormat == PointDescription.FORMAT_SECONDS && len < strLength) {
						focusedEditText.setText(str + ".");
						focusedEditText.setSelection(strLength + 1);
					} else if ((strLength == DEGREES_MAX_LENGTH && coordinateFormat == PointDescription.FORMAT_DEGREES)
							|| (strLength == MINUTES_MAX_LENGTH && coordinateFormat == PointDescription.FORMAT_MINUTES)
							|| (strLength == SECONDS_MAX_LENGTH && coordinateFormat == PointDescription.FORMAT_SECONDS)) {
						if (focusedEditText.getId() == R.id.latitude_edit_text) {
							((OsmandTextFieldBoxes) mainView.findViewById(R.id.longitude_box)).select();
						} else {
							((OsmandTextFieldBoxes) mainView.findViewById(R.id.name_box)).select();
						}
					}
				}
			}
		};

		View.OnTouchListener editTextOnTouchListener = new View.OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent motionEvent) {
				if (useOsmandKeyboard) {
					EditText editText = (EditText) view;
					int inType = editText.getInputType();       // Backup the input type
					editText.setInputType(InputType.TYPE_NULL); // Disable standard keyboard
					editText.onTouchEvent(motionEvent);               // Call native handler
					editText.setInputType(inType);              // Restore input type
					return true; // Consume touch event
				} else {
					if (isOsmandKeyboardCurrentlyVisible()) {
						changeOsmandKeyboardVisibility(false);
					}
					return false;
				}
			}
		};

		View.OnLongClickListener editTextOnLongClickListener = new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(final View view) {
				if (useOsmandKeyboard) {
					final EditText editText = (EditText) view;
					PopupMenu popupMenu = new PopupMenu(getContext(), editText);
					Menu menu = popupMenu.getMenu();
					popupMenu.getMenuInflater().inflate(R.menu.copy_paste_menu, menu);
					final ClipboardManager clipboardManager = ((ClipboardManager) getContext().getSystemService(CLIPBOARD_SERVICE));
					MenuItem pasteMenuItem = menu.findItem(R.id.action_paste);
					if (!(clipboardManager.hasPrimaryClip())) {
						pasteMenuItem.setEnabled(false);
					} else if (!(clipboardManager.getPrimaryClipDescription().hasMimeType(MIMETYPE_TEXT_PLAIN))) {
						pasteMenuItem.setEnabled(false);
					} else {
						pasteMenuItem.setEnabled(true);
					}
					popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
						@Override
						public boolean onMenuItemClick(MenuItem item) {
							switch (item.getItemId()) {
								case R.id.action_copy:
									String labelText;
									switch (view.getId()) {
										case R.id.latitude_edit_text:
											labelText = LATITUDE_LABEL;
											break;
										case R.id.longitude_edit_text:
											labelText = LONGITUDE_LABEL;
											break;
										case R.id.name_edit_text:
											labelText = NAME_LABEL;
											break;
										default:
											labelText = "";
											break;
									}
									ClipData clip = ClipData.newPlainText(labelText, editText.getText().toString());
									clipboardManager.setPrimaryClip(clip);
									return true;
								case R.id.action_paste:
									ClipData.Item pasteItem = clipboardManager.getPrimaryClip().getItemAt(0);
									CharSequence pasteData = pasteItem.getText();
									if (pasteData != null) {
										String str = editText.getText().toString();
										editText.setText(str + pasteData.toString());
										editText.setSelection(editText.getText().length());
									}
									return true;
							}
							return false;
						}
					});
					popupMenu.show();
					return true;
				} else {
					return false;
				}
			}
		};

		View.OnFocusChangeListener focusChangeListener = new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View view, boolean b) {
				int resId;
				switch (view.getId()) {
					case R.id.latitude_edit_text:
						resId = R.id.latitude_box;
						break;
					case R.id.longitude_edit_text:
						resId = R.id.longitude_box;
						break;
					case R.id.name_edit_text:
						resId = R.id.name_box;
						break;
					default:
						resId = 0;
				}
				if (resId != 0) {
					OsmandTextFieldBoxes textFieldBox = mainView.findViewById(resId);
					if (b) {
						textFieldBox.setHasFocus(true);
					} else {
						if (useOsmandKeyboard) {
							AndroidUtils.hideSoftKeyboard(getActivity(), view);
						}
						textFieldBox.setHasFocus(false);
					}
				}
			}
		};

		for (ExtendedEditText editText : extendedEditTexts) {
			if (editText.getId() != R.id.name_edit_text) {
				editText.addTextChangedListener(textWatcher);
			}
			editText.setOnTouchListener(editTextOnTouchListener);
			editText.setOnLongClickListener(editTextOnLongClickListener);
			editText.setOnFocusChangeListener(focusChangeListener);
		}
		changeEditTextHints();
	}

	private CoordinateInputBottomSheetDialogFragment.CoordinateInputFormatChangeListener createCoordinateInputFormatChangeListener() {
		return new CoordinateInputBottomSheetDialogFragment.CoordinateInputFormatChangeListener() {
			@Override
			public void onCoordinateFormatChanged(int format) {
				coordinateFormat = format;
				changeEditTextHints();
			}

			@Override
			public void onKeyboardChanged(boolean useOsmandKeyboard) {
				CoordinateInputDialogFragment.this.useOsmandKeyboard = useOsmandKeyboard;
				if (!useOsmandKeyboard && isOsmandKeyboardCurrentlyVisible()) {
					changeOsmandKeyboardVisibility(false);
				}
				changeKeyboardInBoxes();
			}

			@Override
			public void onCancel() {
				dismiss();
			}
		};
	}

	private boolean isOsmandKeyboardCurrentlyVisible() {
		return mainView.findViewById(R.id.keyboard_grid_view).getVisibility() == View.VISIBLE;
	}

	private void changeOsmandKeyboardVisibility(boolean show) {
		int visibility = show ? View.VISIBLE : View.GONE;
		mainView.findViewById(R.id.keyboard_grid_view).setVisibility(visibility);
		mainView.findViewById(R.id.keyboard_divider).setVisibility(visibility);
		((ImageView) mainView.findViewById(R.id.show_hide_keyboard_icon))
				.setImageDrawable(iconsCache.getThemedIcon(show ? R.drawable.ic_action_arrow_down : R.drawable.ic_action_arrow_up));
	}

	public void changeKeyboardInBoxes() {
		for (OsmandTextFieldBoxes textFieldBox : textFieldBoxes) {
			textFieldBox.setUseOsmandKeyboard(useOsmandKeyboard);
		}
	}

	private void changeEditTextHints() {
		String hint;
		if (coordinateFormat == PointDescription.FORMAT_DEGREES) {
			hint = DEGREES_HINT;
		} else if (coordinateFormat == PointDescription.FORMAT_MINUTES) {
			hint = MINUTES_HINT;
		} else {
			hint = SECONDS_HINT;
		}
		for (ExtendedEditText editText : extendedEditTexts) {
			if (editText.getId() != R.id.name_edit_text) {
				editText.setHint(hint);
			}
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
				convertView.setBackgroundResource(lightTheme ? R.drawable.keyboard_item_light_bg : R.drawable.keyboard_item_dark_bg);
			}
			TextView keyboardItem = (TextView) convertView.findViewById(R.id.keyboard_item);
			if (position == DELETE_BUTTON_POSITION || position == CLEAR_BUTTON_POSITION) {
				TextViewCompat.setAutoSizeTextTypeWithDefaults(keyboardItem, TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE);
				keyboardItem.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.default_list_text_size));
			} else {
				TextViewCompat.setAutoSizeTextTypeWithDefaults(keyboardItem, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM);
			}
			keyboardItem.setText(getItem(position));

			return convertView;
		}
	}

}
