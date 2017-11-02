package net.osmand.plus.mapmarkers;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import net.osmand.AndroidUtils;
import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.IconsCache;
import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.OsmAndLocationProvider.OsmAndCompassListener;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MapViewTrackingUtilities;
import net.osmand.plus.dashboard.DashLocationFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapmarkers.adapters.CoordinateInputAdapter;
import net.osmand.plus.widgets.OsmandTextFieldBoxes;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;

import static android.content.ClipDescription.MIMETYPE_TEXT_PLAIN;
import static android.content.Context.CLIPBOARD_SERVICE;
import static net.osmand.plus.MapMarkersHelper.MAP_MARKERS_COLORS_COUNT;

public class CoordinateInputDialogFragment extends DialogFragment implements OsmAndCompassListener, OsmAndLocationListener {

	public static final String TAG = "CoordinateInputDialogFragment";

	public static final String USE_OSMAND_KEYBOARD = "use_osmand_keyboard";
	public static final String GO_TO_NEXT_FIELD = "go_to_next_field";
	public static final String ACCURACY = "accuracy";

	private static final int CLEAR_BUTTON_POSITION = 3;
	private static final int MINUS_BUTTON_POSITION = 7;
	private static final int BACKSPACE_BUTTON_POSITION = 11;
	private static final int SWITCH_TO_NEXT_INPUT_BUTTON_POSITION = 15;
	private static final String LATITUDE_LABEL = "latitude";
	private static final String LONGITUDE_LABEL = "longitude";
	private static final String NAME_LABEL = "name";

	private OnMapMarkersSavedListener listener;
	private List<MapMarker> mapMarkers = new ArrayList<>();
	private CoordinateInputAdapter adapter;
	private boolean lightTheme;
	private boolean useOsmandKeyboard = true;
	private boolean goToNextField;
	private int accuracy = 4;
	private List<OsmandTextFieldBoxes> textFieldBoxes;
	private List<EditText> inputEditTexts;
	private View mainView;
	private IconsCache iconsCache;
	private Location location;
	private Float heading;
	private boolean locationUpdateStarted;
	private boolean compassUpdateAllowed = true;
	private MapMarkersHelper mapMarkersHelper;
	private boolean orientationPortrait;

	public void setListener(OnMapMarkersSavedListener listener) {
		this.listener = listener;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		OsmandApplication app = getMyApplication();
		lightTheme = app.getSettings().OSMAND_THEME.get() == OsmandSettings.OSMAND_LIGHT_THEME;
		int themeId = lightTheme ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme;
		setStyle(STYLE_NO_FRAME, themeId);
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Dialog dialog = new Dialog(getContext(), getTheme()) {
			@Override
			public void onBackPressed() {
				saveMarkers();
				super.onBackPressed();
			}
		};
		Window window = dialog.getWindow();
		if (window != null) {
			window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
				window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
				window.setStatusBarColor(ContextCompat.getColor(getContext(), R.color.coordinate_input_status_bar_color));
			}
		}
		return dialog;
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		mainView = inflater.inflate(R.layout.fragment_coordinate_input_dialog, container);
		final MapActivity mapActivity = getMapActivity();
		iconsCache = getMyApplication().getIconsCache();
		mapMarkersHelper = getMyApplication().getMapMarkersHelper();
		orientationPortrait = AndroidUiHelper.isOrientationPortrait(mapActivity);

		Fragment coordinateInputBottomSheetDialogFragment = mapActivity.getSupportFragmentManager().findFragmentByTag(CoordinateInputBottomSheetDialogFragment.TAG);
		if (coordinateInputBottomSheetDialogFragment != null) {
			((CoordinateInputBottomSheetDialogFragment) coordinateInputBottomSheetDialogFragment).setListener(createCoordinateInputFormatChangeListener());
		}

		mainView.findViewById(R.id.back_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				saveMarkers();
				dismiss();
			}
		});
		final View optionsButton = mainView.findViewById(R.id.options_button);
		optionsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				View focusedView = getDialog().getCurrentFocus();
				if (focusedView != null && focusedView instanceof EditText) {
					focusedView.clearFocus();
					AndroidUtils.hideSoftKeyboard(getMapActivity(), focusedView);
				}
				CoordinateInputBottomSheetDialogFragment fragment = new CoordinateInputBottomSheetDialogFragment();
				Bundle args = new Bundle();
				args.putBoolean(USE_OSMAND_KEYBOARD, useOsmandKeyboard);
				args.putBoolean(GO_TO_NEXT_FIELD, goToNextField);
				args.putInt(ACCURACY, accuracy);
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
					if (orientationPortrait && isOsmandKeyboardCurrentlyVisible()) {
						changeOsmandKeyboardVisibility(false);
					}
					AndroidUtils.showSoftKeyboard(focusedView);
				}
			}
		});
		textFieldBoxes.add(nameBox);

		registerTextFieldBoxes();

		inputEditTexts = new ArrayList<>();
		final EditText latitudeEditText = (EditText) mainView.findViewById(R.id.latitude_edit_text);
		inputEditTexts.add(latitudeEditText);
		final EditText longitudeEditText = (EditText) mainView.findViewById(R.id.longitude_edit_text);
		inputEditTexts.add(longitudeEditText);
		final EditText nameEditText = (EditText) mainView.findViewById(R.id.name_edit_text);
		inputEditTexts.add(nameEditText);

		registerInputTextViews();

		if (savedInstanceState == null) {
			latitudeBox.select();
		}

		final View mapMarkersLayout = mainView.findViewById(R.id.map_markers_layout);

		RecyclerView recyclerView = (RecyclerView) mainView.findViewById(R.id.markers_recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		adapter = new CoordinateInputAdapter(mapActivity, mapMarkers);
		if (mapMarkersLayout != null) {
			adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
				@Override
				public void onChanged() {
					super.onChanged();
					mapMarkersLayout.setVisibility(adapter.isEmpty() ? View.GONE : View.VISIBLE);
				}
			});
		}
		recyclerView.setAdapter(adapter);
		recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
				super.onScrollStateChanged(recyclerView, newState);
				compassUpdateAllowed = newState == RecyclerView.SCROLL_STATE_IDLE;
			}
		});

		mainView.findViewById(R.id.add_marker_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				addMapMarker();
			}
		});

		View keyboardLayout = mainView.findViewById(R.id.keyboard_layout);
		if (orientationPortrait) {
			AndroidUtils.setBackground(mapActivity, keyboardLayout, !lightTheme, R.drawable.bg_bottom_menu_light, R.drawable.bg_bottom_menu_dark);
		}

		Object[] keyboardItems = new Object[] { "1", "2", "3", getString(R.string.shared_string_clear),
				"4", "5", "6", "-",
				"7", "8", "9", R.drawable.ic_keyboard_backspace,
				".", "0", ":", R.drawable.ic_keyboard_next_field};
		final GridView keyboardGrid = (GridView) mainView.findViewById(R.id.keyboard_grid_view);
		final KeyboardAdapter keyboardAdapter = new KeyboardAdapter(mapActivity, keyboardItems);
		keyboardGrid.setAdapter(keyboardAdapter);
		keyboardGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
				View focusedView = getDialog().getCurrentFocus();
				if (focusedView != null && focusedView instanceof EditText) {
					EditText focusedEditText = (EditText) focusedView;
					switch (i) {
						case CLEAR_BUTTON_POSITION:
							focusedEditText.setText("");
							break;
						case BACKSPACE_BUTTON_POSITION:
							String str = focusedEditText.getText().toString();
							if (str.length() > 0) {
								str = str.substring(0, str.length() - 1);
								focusedEditText.setText(str);
								focusedEditText.setSelection(str.length());
							}
							break;
						case SWITCH_TO_NEXT_INPUT_BUTTON_POSITION:
							switchToNextInput(focusedEditText.getId());
							break;
						default:
							focusedEditText.append((String) keyboardAdapter.getItem(i));
					}
				}
			}
		});

		if (orientationPortrait) {
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
					if (orientationPortrait) {
						changeOsmandKeyboardVisibility(!isCurrentlyVisible);
					}
				}
			});
		}

		return mainView;
	}

	@Override
	public void onResume() {
		super.onResume();
		adapter.setScreenOrientation(DashLocationFragment.getScreenOrientation(getActivity()));
		startLocationUpdate();
	}

	@Override
	public void onPause() {
		super.onPause();
		stopLocationUpdate();
	}

	@Override
	public void onDestroyView() {
		Dialog dialog = getDialog();
		if (dialog != null && getRetainInstance()) {
			dialog.setDismissMessage(null);
		}
		super.onDestroyView();
	}

	private void saveMarkers() {
		mapMarkersHelper.addMarkers(mapMarkers);
		if (listener != null) {
			listener.onMapMarkersSaved();
		}
	}

	private void registerTextFieldBoxes() {
		View.OnTouchListener textFieldBoxOnTouchListener = new View.OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent motionEvent) {
				if (orientationPortrait) {
					if (!useOsmandKeyboard && isOsmandKeyboardCurrentlyVisible()) {
						changeOsmandKeyboardVisibility(false);
					} else if (useOsmandKeyboard && !isOsmandKeyboardCurrentlyVisible()) {
						changeOsmandKeyboardVisibility(true);
					}
				}
				return false;
			}
		};

		for (OsmandTextFieldBoxes textFieldBox : textFieldBoxes) {
			textFieldBox.getPanel().setOnTouchListener(textFieldBoxOnTouchListener);
		}
		changeKeyboardInBoxes();
	}

	private void registerInputTextViews() {
		TextWatcher textWatcher = new TextWatcher() {
			int len = 0;
			String strBeforeChanging = "";

			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
				len = charSequence.length();
				strBeforeChanging = charSequence.toString();
			}

			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void afterTextChanged(Editable editable) {
				View focusedView = getDialog().getCurrentFocus();
				if (focusedView != null && focusedView instanceof EditText) {
					EditText focusedEditText = (EditText) focusedView;
					String str = focusedEditText.getText().toString();
				}
			}
		};

		View.OnTouchListener inputEditTextOnTouchListener = new View.OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent motionEvent) {
				if (useOsmandKeyboard || !orientationPortrait) {
					if (orientationPortrait && !isOsmandKeyboardCurrentlyVisible()) {
						changeOsmandKeyboardVisibility(true);
					}
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

		View.OnLongClickListener inputEditTextOnLongClickListener = new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(final View view) {
				final EditText inputEditText = (EditText) view;
				PopupMenu popupMenu = new PopupMenu(getContext(), inputEditText);
				Menu menu = popupMenu.getMenu();
				popupMenu.getMenuInflater().inflate(R.menu.copy_paste_menu, menu);
				final ClipboardManager clipboardManager = ((ClipboardManager) getContext().getSystemService(CLIPBOARD_SERVICE));
				MenuItem pasteMenuItem = menu.findItem(R.id.action_paste);
				if (clipboardManager == null || !clipboardManager.hasPrimaryClip() ||
						!clipboardManager.getPrimaryClipDescription().hasMimeType(MIMETYPE_TEXT_PLAIN)) {
					pasteMenuItem.setEnabled(false);
				} else {
					pasteMenuItem.setEnabled(true);
				}
				if (clipboardManager != null) {
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
									ClipData clip = ClipData.newPlainText(labelText, inputEditText.getText().toString());
									clipboardManager.setPrimaryClip(clip);
									return true;
								case R.id.action_paste:
									ClipData.Item pasteItem = clipboardManager.getPrimaryClip().getItemAt(0);
									CharSequence pasteData = pasteItem.getText();
									if (pasteData != null) {
										String str = inputEditText.getText().toString();
										inputEditText.setText(str + pasteData.toString());
										inputEditText.setSelection(inputEditText.getText().length());
									}
									return true;
							}
							return false;
						}
					});
					popupMenu.show();
				}
				return true;

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
						} else if (orientationPortrait && isOsmandKeyboardCurrentlyVisible()) {
							changeOsmandKeyboardVisibility(false);
						}
						textFieldBox.setHasFocus(false);
					}
				}
			}
		};

		TextView.OnEditorActionListener inputTextViewOnEditorActionListener = new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
				if (i == EditorInfo.IME_ACTION_NEXT) {
					switchToNextInput(textView.getId());
				} else if (i == EditorInfo.IME_ACTION_DONE) {
					addMapMarker();
				}
				return false;
			}
		};

		for (TextView textView : inputEditTexts) {
			if (textView.getId() != R.id.name_edit_text) {
				textView.addTextChangedListener(textWatcher);
			}
			textView.setOnTouchListener(inputEditTextOnTouchListener);
			textView.setOnLongClickListener(inputEditTextOnLongClickListener);
			textView.setOnFocusChangeListener(focusChangeListener);
			textView.setOnEditorActionListener(inputTextViewOnEditorActionListener);
		}

		changeInputEditTextHints();
		changeInputEditTextLengths();
		changeKeyboardInEditTexts();
	}

	private CoordinateInputBottomSheetDialogFragment.CoordinateInputFormatChangeListener createCoordinateInputFormatChangeListener() {
		return new CoordinateInputBottomSheetDialogFragment.CoordinateInputFormatChangeListener() {
			@Override
			public void onKeyboardChanged(boolean useOsmandKeyboard) {
				CoordinateInputDialogFragment.this.useOsmandKeyboard = useOsmandKeyboard;
				if (orientationPortrait && !useOsmandKeyboard && isOsmandKeyboardCurrentlyVisible()) {
					changeOsmandKeyboardVisibility(false);
				}
				changeKeyboardInBoxes();
				changeKeyboardInEditTexts();
			}

			@Override
			public void onGoToNextFieldChanged(boolean goToNextField) {
				CoordinateInputDialogFragment.this.goToNextField = goToNextField;
			}

			@Override
			public void onAccuracyChanged(int accuracy) {
				CoordinateInputDialogFragment.this.accuracy = accuracy;
				changeInputEditTextHints();
				changeInputEditTextLengths();
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

	private void changeKeyboardInBoxes() {
		for (OsmandTextFieldBoxes textFieldBox : textFieldBoxes) {
			textFieldBox.setUseOsmandKeyboard(useOsmandKeyboard);
		}
	}

	private void changeKeyboardInEditTexts() {
//		int coordinateInputType = useOsmandKeyboard ? InputType.TYPE_NULL : InputType.TYPE_CLASS_NUMBER;
//		int nameInputType = useOsmandKeyboard ? InputType.TYPE_NULL : InputType.TYPE_CLASS_TEXT;
//		for (TextView inputTextView : inputEditTexts) {
//			inputTextView.setInputType(inputTextView.getId() == R.id.name_edit_text ? nameInputType : coordinateInputType);
//		}
  	}

	private void changeInputEditTextLengths() {
//		int maxLength;
//		if (accuracy == PointDescription.FORMAT_DEGREES) {
//			maxLength = DEGREES_MAX_LENGTH;
//		} else if (accuracy == PointDescription.FORMAT_MINUTES) {
//			maxLength = MINUTES_MAX_LENGTH;
//		} else {
//			maxLength = SECONDS_MAX_LENGTH;
//		}
//		InputFilter[] filtersArray = new InputFilter[] {new InputFilter.LengthFilter(maxLength)};
//		for (EditText editText : inputEditTexts) {
//			if (editText.getId() != R.id.name_edit_text) {
//				editText.setFilters(filtersArray);
//			}
//		}
	}

	private void changeInputEditTextHints() {
//		String hint;
//		if (accuracy == PointDescription.FORMAT_DEGREES) {
//			hint = DEGREES_HINT;
//		} else if (accuracy == PointDescription.FORMAT_MINUTES) {
//			hint = MINUTES_HINT;
//		} else {
//			hint = SECONDS_HINT;
//		}
//		for (EditText editText : inputEditTexts) {
//			if (editText.getId() != R.id.name_edit_text) {
//				editText.setHint(hint);
//			}
//		}
	}

	private void switchToNextInput(int id) {
		if (id == R.id.latitude_edit_text) {
			((OsmandTextFieldBoxes) mainView.findViewById(R.id.longitude_box)).select();
		} else if (id == R.id.longitude_edit_text) {
			((OsmandTextFieldBoxes) mainView.findViewById(R.id.name_box)).select();
		}
	}

	private void addMapMarker() {
		String latitude = ((EditText) mainView.findViewById(R.id.latitude_edit_text)).getText().toString();
		String longitude = ((EditText) mainView.findViewById(R.id.longitude_edit_text)).getText().toString();
		String locPhrase = latitude + ", " + longitude;
		LatLon latLon = MapUtils.parseLocation(locPhrase);
		if (latLon != null) {
			String name = ((EditText) mainView.findViewById(R.id.name_edit_text)).getText().toString();
			addMapMarker(latLon, name);
		} else {
			Toast.makeText(getContext(), getString(R.string.wrong_format), Toast.LENGTH_SHORT).show();
		}
	}

	private void addMapMarker(LatLon latLon, String name) {
		PointDescription pointDescription = new PointDescription(PointDescription.POINT_TYPE_MAP_MARKER, name);
		int colorIndex = mapMarkers.size() > 0 ? mapMarkers.get(mapMarkers.size() - 1).colorIndex : -1;
		if (colorIndex == -1) {
			colorIndex = 0;
		} else {
			colorIndex = (colorIndex + 1) % MAP_MARKERS_COLORS_COUNT;
		}
		MapMarker mapMarker = new MapMarker(latLon, pointDescription, colorIndex, false, 0);
		mapMarker.history = false;
		mapMarker.nextKey = MapMarkersDbHelper.TAIL_NEXT_VALUE;
		mapMarkers.add(mapMarker);
		adapter.notifyDataSetChanged();
		clearInputs();
		((OsmandTextFieldBoxes) mainView.findViewById(R.id.latitude_box)).select();
	}

	private void clearInputs() {
		for (EditText inputEditText : inputEditTexts) {
			inputEditText.setText("");
		}
		for (OsmandTextFieldBoxes osmandTextFieldBox : textFieldBoxes) {
			osmandTextFieldBox.deactivate();
		}
	}

	private MapActivity getMapActivity() {
		return (MapActivity) getActivity();
	}

	private OsmandApplication getMyApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}

	@Override
	public void updateLocation(Location location) {
		boolean newLocation = this.location == null && location != null;
		boolean locationChanged = this.location != null && location != null
				&& this.location.getLatitude() != location.getLatitude()
				&& this.location.getLongitude() != location.getLongitude();
		if (newLocation || locationChanged) {
			this.location = location;
			updateLocationUi();
		}
	}

	@Override
	public void updateCompassValue(float value) {
		// 99 in next line used to one-time initialize arrows (with reference vs. fixed-north direction)
		// on non-compass devices
		float lastHeading = heading != null ? heading : 99;
		heading = value;
		if (Math.abs(MapUtils.degreesDiff(lastHeading, heading)) > 5) {
			updateLocationUi();
		} else {
			heading = lastHeading;
		}
	}

	private void updateLocationUi() {
		if (!compassUpdateAllowed) {
			return;
		}
		final MapActivity mapActivity = (MapActivity) getActivity();
		if (mapActivity != null && adapter != null) {
			mapActivity.getMyApplication().runInUIThread(new Runnable() {
				@Override
				public void run() {
					if (location == null) {
						location = mapActivity.getMyApplication().getLocationProvider().getLastKnownLocation();
					}
					MapViewTrackingUtilities utilities = mapActivity.getMapViewTrackingUtilities();
					boolean useCenter = !(utilities.isMapLinkedToLocation() && location != null);

					adapter.setUseCenter(useCenter);
					adapter.setLocation(useCenter ? mapActivity.getMapLocation() : new LatLon(location.getLatitude(), location.getLongitude()));
					adapter.setHeading(useCenter ? -mapActivity.getMapRotate() : heading != null ? heading : 99);
					adapter.notifyDataSetChanged();
				}
			});
		}
	}

	private void startLocationUpdate() {
		OsmandApplication app = getMyApplication();
		if (app != null && !locationUpdateStarted) {
			locationUpdateStarted = true;
			app.getLocationProvider().removeCompassListener(app.getLocationProvider().getNavigationInfo());
			app.getLocationProvider().addCompassListener(this);
			app.getLocationProvider().addLocationListener(this);
			updateLocationUi();
		}
	}

	private void stopLocationUpdate() {
		OsmandApplication app = getMyApplication();
		if (app != null && locationUpdateStarted) {
			locationUpdateStarted = false;
			app.getLocationProvider().removeLocationListener(this);
			app.getLocationProvider().removeCompassListener(this);
			app.getLocationProvider().addCompassListener(app.getLocationProvider().getNavigationInfo());
		}
	}

	private class KeyboardAdapter extends ArrayAdapter<Object> {

		KeyboardAdapter(@NonNull Context context, @NonNull Object[] objects) {
			super(context, 0, objects);
		}

		@NonNull
		@Override
		public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
			if (convertView == null) {
				convertView = LayoutInflater.from(getContext()).inflate(R.layout.input_coordinate_keyboard_item, parent, false);
			}
			if (!orientationPortrait) {
				int keyboardViewHeight = mainView.findViewById(R.id.keyboard_grid_view).getMeasuredHeight();
				int dividerHeight = AndroidUtils.dpToPx(getContext(), 1);
				int spaceForKeys = keyboardViewHeight - 3 * dividerHeight;
				convertView.setMinimumHeight(spaceForKeys / 4);
			}
			boolean controlButton = position == CLEAR_BUTTON_POSITION
					|| position == MINUS_BUTTON_POSITION
					|| position == BACKSPACE_BUTTON_POSITION
					|| position == SWITCH_TO_NEXT_INPUT_BUTTON_POSITION;
			if (controlButton) {
				convertView.setBackgroundResource(lightTheme ? R.drawable.keyboard_item_control_light_bg : R.drawable.keyboard_item_control_dark_bg);
			} else {
				convertView.setBackgroundResource(lightTheme ? R.drawable.keyboard_item_light_bg : R.drawable.keyboard_item_dark_bg);
			}
			TextView keyboardItemText = (TextView) convertView.findViewById(R.id.keyboard_item_text);
			ImageView keyboardItemImage = (ImageView) convertView.findViewById(R.id.keyboard_item_image);
			Object item = getItem(position);
			if (item instanceof String) {
				if (position == CLEAR_BUTTON_POSITION) {
					TextViewCompat.setAutoSizeTextTypeWithDefaults(keyboardItemText, TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE);
					keyboardItemText.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.default_list_text_size));
				} else {
					TextViewCompat.setAutoSizeTextTypeWithDefaults(keyboardItemText, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM);
				}
				keyboardItemImage.setVisibility(View.GONE);
				keyboardItemText.setVisibility(View.VISIBLE);
				keyboardItemText.setText((String) getItem(position));
			} else if (item instanceof Integer) {
				keyboardItemText.setVisibility(View.GONE);
				keyboardItemImage.setVisibility(View.VISIBLE);
				keyboardItemImage.setImageResource((Integer) item);
			}

			return convertView;
		}
	}

	interface OnMapMarkersSavedListener {
		void onMapMarkersSaved();
	}

}
