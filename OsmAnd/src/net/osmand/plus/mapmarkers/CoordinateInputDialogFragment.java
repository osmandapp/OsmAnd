package net.osmand.plus.mapmarkers;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
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
import net.osmand.plus.OsmandTextFieldBoxes;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MapViewTrackingUtilities;
import net.osmand.plus.dashboard.DashLocationFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapmarkers.adapters.CoordinateInputAdapter;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;

import studio.carbonylgroup.textfieldboxes.ExtendedEditText;

import static android.content.ClipDescription.MIMETYPE_TEXT_PLAIN;
import static android.content.Context.CLIPBOARD_SERVICE;
import static net.osmand.plus.MapMarkersHelper.MAP_MARKERS_COLORS_COUNT;

public class CoordinateInputDialogFragment extends DialogFragment implements OsmAndCompassListener, OsmAndLocationListener {

	public static final String TAG = "CoordinateInputDialogFragment";

	public static final String COORDINATE_FORMAT = "coordinate_format";
	public static final String USE_OSMAND_KEYBOARD = "use_osmand_keyboard";

	private static final int CLEAR_BUTTON_POSITION = 9;
	private static final int DELETE_BUTTON_POSITION = 11;
	private static final int DEGREES_MAX_LENGTH = 6;
	private static final int MINUTES_MAX_LENGTH = 9;
	private static final int SECONDS_MAX_LENGTH = 12;
	private static final String DEGREES_HINT = "50.000";
	private static final String MINUTES_HINT = "50:00.000";
	private static final String SECONDS_HINT = "50:00:00.000";
	private static final String LATITUDE_LABEL = "latitude";
	private static final String LONGITUDE_LABEL = "longitude";
	private static final String NAME_LABEL = "name";

	private OnMapMarkersSavedListener listener;
	private List<MapMarker> mapMarkers = new ArrayList<>();
	private CoordinateInputAdapter adapter;
	private boolean lightTheme;
	private boolean useOsmandKeyboard = true;
	private int coordinateFormat = -1;
	private List<OsmandTextFieldBoxes> textFieldBoxes;
	private List<ExtendedEditText> extendedEditTexts;
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

		CoordinateInputBottomSheetDialogFragment fragment = new CoordinateInputBottomSheetDialogFragment();
		fragment.setListener(createCoordinateInputFormatChangeListener());
		fragment.show(getMapActivity().getSupportFragmentManager(), CoordinateInputBottomSheetDialogFragment.TAG);
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		return new Dialog(getContext(), getTheme()) {
			@Override
			public void onBackPressed() {
				saveMarkers();
				super.onBackPressed();
			}
		};
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
					if (orientationPortrait && isOsmandKeyboardCurrentlyVisible()) {
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

		final View mapMarkersLayout = mainView.findViewById(R.id.map_markers_layout);

		RecyclerView recyclerView = (RecyclerView) mainView.findViewById(R.id.markers_recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		adapter = new CoordinateInputAdapter(mapActivity, mapMarkers);
		adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
			@Override
			public void onChanged() {
				super.onChanged();
				mapMarkersLayout.setVisibility(adapter.isEmpty() ? View.GONE : View.VISIBLE);
			}
		});
		recyclerView.setAdapter(adapter);
		recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
				super.onScrollStateChanged(recyclerView, newState);
				compassUpdateAllowed = newState == RecyclerView.SCROLL_STATE_IDLE;
			}
		});

		TextView addMarkerButton = (TextView) mainView.findViewById(R.id.add_marker_button);
		addMarkerButton.setBackgroundResource(lightTheme ? R.drawable.keyboard_item_light_bg : R.drawable.keyboard_item_dark_bg);
		addMarkerButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				String latitude = latitudeEditText.getText().toString();
				String longitude = longitudeEditText.getText().toString();
				String locPhrase = latitude + ", " + longitude;
				LatLon latLon = MapUtils.parseLocation(locPhrase);
				if (latLon != null) {
					String name = nameEditText.getText().toString();
					addMapMarker(latLon, name);
				} else {
					Toast.makeText(getContext(), getString(R.string.wrong_format), Toast.LENGTH_SHORT).show();
				}
			}
		});

		View keyboardLayout = mainView.findViewById(R.id.keyboard_layout);
		if (orientationPortrait) {
			AndroidUtils.setBackground(mapActivity, keyboardLayout, !lightTheme, R.drawable.bg_bottom_menu_light, R.drawable.bg_bottom_menu_dark);
		}

		String[] keyboardItems = new String[] { "1", "2", "3",
				"4", "5", "6",
				"7", "8", "9",
				getString(R.string.shared_string_clear), "0", "\u21e6" };
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
						case CLEAR_BUTTON_POSITION:
							extendedEditText.setText("");
							break;
						case DELETE_BUTTON_POSITION:
							String str = extendedEditText.getText().toString();
							if (str.length() > 0) {
								str = str.substring(0, str.length() - 1);
								extendedEditText.setText(str);
								extendedEditText.setSelection(str.length());
							}
							break;
						default:
							extendedEditText.append(keyboardAdapter.getItem(i));
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
	public void onSaveInstanceState(Bundle outState) {
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
				if (orientationPortrait && !useOsmandKeyboard && isOsmandKeyboardCurrentlyVisible()) {
					changeOsmandKeyboardVisibility(false);
				}
				return false;
			}
		};

		for (OsmandTextFieldBoxes textFieldBox : textFieldBoxes) {
			textFieldBox.getPanel().setOnTouchListener(textFieldBoxOnTouchListener);
		}
		changeKeyboardInBoxes();
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
				if (focusedView != null && focusedView instanceof EditText) {
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
					if (orientationPortrait && isOsmandKeyboardCurrentlyVisible()) {
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
					}
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
	}

	private CoordinateInputBottomSheetDialogFragment.CoordinateInputFormatChangeListener createCoordinateInputFormatChangeListener() {
		return new CoordinateInputBottomSheetDialogFragment.CoordinateInputFormatChangeListener() {
			@Override
			public void onCoordinateFormatChanged(int format) {
				coordinateFormat = format;
				changeEditTextHints();
				changeEditTextLengths();
			}

			@Override
			public void onKeyboardChanged(boolean useOsmandKeyboard) {
				CoordinateInputDialogFragment.this.useOsmandKeyboard = useOsmandKeyboard;
				if (orientationPortrait && !useOsmandKeyboard && isOsmandKeyboardCurrentlyVisible()) {
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
		for (ExtendedEditText extendedEditText : extendedEditTexts) {
			if (extendedEditText.getId() != R.id.name_edit_text) {
				extendedEditText.setFilters(filtersArray);
			}
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

	public void addMapMarker(LatLon latLon, String name) {
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
			if (!orientationPortrait) {
				int keyboardViewHeight = mainView.findViewById(R.id.keyboard_grid_view).getMeasuredHeight();
				int dividerHeight = AndroidUtils.dpToPx(getContext(), 1);
				int spaceForKeys = keyboardViewHeight - 3 * dividerHeight;
				convertView.setMinimumHeight(spaceForKeys / 4);
			}
			TextView keyboardItem = (TextView) convertView.findViewById(R.id.keyboard_item);
			if (position == CLEAR_BUTTON_POSITION) {
				TextViewCompat.setAutoSizeTextTypeWithDefaults(keyboardItem, TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE);
				keyboardItem.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.default_list_text_size));
			} else {
				TextViewCompat.setAutoSizeTextTypeWithDefaults(keyboardItem, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM);
			}
			keyboardItem.setText(getItem(position));

			return convertView;
		}
	}

	interface OnMapMarkersSavedListener {
		void onMapMarkersSaved();
	}

}
