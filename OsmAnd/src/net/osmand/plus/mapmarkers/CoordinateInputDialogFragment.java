package net.osmand.plus.mapmarkers;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.GestureDetector;
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
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import net.osmand.AndroidUtils;
import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.OsmAndLocationProvider.OsmAndCompassListener;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MapViewTrackingUtilities;
import net.osmand.plus.dashboard.DashLocationFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapmarkers.adapters.CoordinateInputAdapter;
import net.osmand.util.LocationParser;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;

import static android.content.ClipDescription.MIMETYPE_TEXT_PLAIN;
import static android.content.Context.CLIPBOARD_SERVICE;
import static net.osmand.plus.MapMarkersHelper.MAP_MARKERS_COLORS_COUNT;

public class CoordinateInputDialogFragment extends DialogFragment implements OsmAndCompassListener, OsmAndLocationListener {

	public static final String TAG = "CoordinateInputDialogFragment";

	public static final String USE_OSMAND_KEYBOARD = "use_osmand_keyboard";

	private static final int SWITCH_TO_NEXT_INPUT_BUTTON_POSITION = 3;
	private static final int MINUS_BUTTON_POSITION = 7;
	private static final int BACKSPACE_BUTTON_POSITION = 11;
	private static final int COLON_BUTTON_POSITION = 12;
	private static final int POINT_BUTTON_POSITION = 14;
	private static final int CLEAR_BUTTON_POSITION = 15;
	private static final String LATITUDE_LABEL = "latitude";
	private static final String LONGITUDE_LABEL = "longitude";
	private static final String NAME_LABEL = "name";

	private List<MapMarker> mapMarkers = new ArrayList<>();
	private OnMapMarkersSavedListener listener;

	private View mainView;
	private List<EditText> editTexts = new ArrayList<>();
	private CoordinateInputAdapter adapter;

	private boolean lightTheme;
	private boolean orientationPortrait;

	private boolean useOsmandKeyboard = true;

	private Location location;
	private Float heading;
	private boolean locationUpdateStarted;
	private boolean compassUpdateAllowed = true;

	public void setListener(OnMapMarkersSavedListener listener) {
		this.listener = listener;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		lightTheme = getMyApplication().getSettings().isLightContent();
		setStyle(STYLE_NO_FRAME, lightTheme ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme);
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Context ctx = getContext();
		Dialog dialog = new Dialog(ctx, getTheme()) {
			@Override
			public void onBackPressed() {
				saveMarkers();
				super.onBackPressed();
			}
		};
		Window window = dialog.getWindow();
		if (window != null) {
			window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
			if (!lightTheme && Build.VERSION.SDK_INT >= 21) {
				window.setStatusBarColor(getResolvedColor(R.color.status_bar_coordinate_input_dark));
			}
		}
		return dialog;
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		orientationPortrait = AndroidUiHelper.isOrientationPortrait(getActivity());

		Fragment optionsFragment = getChildFragmentManager().findFragmentByTag(CoordinateInputBottomSheetDialogFragment.TAG);
		if (optionsFragment != null) {
			((CoordinateInputBottomSheetDialogFragment) optionsFragment).setListener(createCoordinateInputFormatChangeListener());
		}

		mainView = inflater.inflate(R.layout.fragment_coordinate_input_dialog, container);

		ImageButton backBtn = (ImageButton) mainView.findViewById(R.id.back_button);
		backBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				saveMarkers();
				dismiss();
			}
		});

		if (orientationPortrait) {
			backBtn.setImageDrawable(getActiveIcon(R.drawable.ic_arrow_back));
			((TextView) mainView.findViewById(R.id.toolbar_text))
					.setTextColor(getResolvedColor(lightTheme ? R.color.color_black : R.color.color_white));
			setBackgroundColor(R.id.app_bar, lightTheme ? R.color.route_info_bg_light : R.color.route_info_bg_dark);
		} else {
			setBackgroundColor(R.id.app_bar, lightTheme ? R.color.actionbar_light_color : R.color.route_info_bottom_view_bg_dark);
		}

		mainView.findViewById(R.id.options_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				View focusedView = getDialog().getCurrentFocus();
				if (focusedView != null && focusedView instanceof EditText) {
					focusedView.clearFocus();
					AndroidUtils.hideSoftKeyboard(getMapActivity(), focusedView);
				}

				Bundle args = new Bundle();
				args.putBoolean(USE_OSMAND_KEYBOARD, useOsmandKeyboard);

				CoordinateInputBottomSheetDialogFragment fragment = new CoordinateInputBottomSheetDialogFragment();
				fragment.setUsedOnMap(false);
				fragment.setArguments(args);
				fragment.setListener(createCoordinateInputFormatChangeListener());
				fragment.show(getChildFragmentManager(), CoordinateInputBottomSheetDialogFragment.TAG);
			}
		});

		registerMainView();

		return mainView;
	}

	private void registerMainView() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}

		final Context ctx = getContext();

		if (orientationPortrait) {
			View.OnClickListener backspaceOnClickListener = new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Toast.makeText(ctx, "backspace", Toast.LENGTH_SHORT).show();
				}
			};

			ImageView latBackspaceBtn = (ImageView) mainView.findViewById(R.id.lat_backspace_btn);
			latBackspaceBtn.setImageDrawable(getActiveIcon(R.drawable.ic_keyboard_backspace));
			latBackspaceBtn.setOnClickListener(backspaceOnClickListener);

			ImageView lonBackspaceBtn = (ImageView) mainView.findViewById(R.id.lon_backspace_btn);
			lonBackspaceBtn.setImageDrawable(getActiveIcon(R.drawable.ic_keyboard_backspace));
			lonBackspaceBtn.setOnClickListener(backspaceOnClickListener);
		} else {
			boolean rightHand = getMyApplication().getSettings().COORDS_INPUT_USE_RIGHT_SIDE.get();
			LinearLayout handContainer = (LinearLayout) mainView.findViewById(R.id.hand_container);

			int leftLayoutResId = rightHand ? R.layout.coordinate_input_land_data_area : R.layout.coordinate_input_land_keyboard_and_list;
			int rightLayoutResId = rightHand ? R.layout.coordinate_input_land_keyboard_and_list : R.layout.coordinate_input_land_data_area;
			View leftView = View.inflate(ctx, leftLayoutResId, null);
			View rightView = View.inflate(ctx, rightLayoutResId, null);
			setBackgroundColor(leftView, lightTheme ? R.color.route_info_bg_light : R.color.route_info_bg_dark);
			((FrameLayout) handContainer.findViewById(R.id.left_container)).addView(leftView, 0);
			((FrameLayout) handContainer.findViewById(R.id.right_container)).addView(rightView, 0);

			handContainer.findViewById(R.id.input_area_top_padding).setVisibility(View.VISIBLE);
			handContainer.findViewById(R.id.point_name_top_space).setVisibility(View.VISIBLE);
			handContainer.findViewById(R.id.right_shadow).setVisibility(rightHand ? View.VISIBLE : View.GONE);
			handContainer.findViewById(R.id.left_shadow).setVisibility(rightHand ? View.GONE : View.VISIBLE);

			handContainer.findViewById(R.id.lat_backspace_btn).setVisibility(View.GONE);
			handContainer.findViewById(R.id.lon_backspace_btn).setVisibility(View.GONE);
			handContainer.findViewById(R.id.lat_end_padding).setVisibility(View.VISIBLE);
			handContainer.findViewById(R.id.lon_end_padding).setVisibility(View.VISIBLE);
		}

		registerInputs();

		View latSideOfTheWorldBtn = mainView.findViewById(R.id.lat_side_of_the_world_btn);
		latSideOfTheWorldBtn.setBackgroundResource(lightTheme
				? R.drawable.context_menu_controller_bg_light : R.drawable.context_menu_controller_bg_dark);
		latSideOfTheWorldBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Toast.makeText(ctx, "lat side of the world", Toast.LENGTH_SHORT).show();
			}
		});
		((TextView) latSideOfTheWorldBtn.findViewById(R.id.lat_side_of_the_world_tv)).setText(getString(R.string.north_abbreviation));
		ImageView northSideIv = latSideOfTheWorldBtn.findViewById(R.id.north_side_iv);
		northSideIv.setImageDrawable(getColoredIcon(R.drawable.ic_action_coordinates_longitude, R.color.dashboard_blue));

		View lonSideOfTheWorldBtn = mainView.findViewById(R.id.lon_side_of_the_world_btn);
		lonSideOfTheWorldBtn.setBackgroundResource(lightTheme
				? R.drawable.context_menu_controller_bg_light : R.drawable.context_menu_controller_bg_dark);
		lonSideOfTheWorldBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Toast.makeText(ctx, "lon side of the world", Toast.LENGTH_SHORT).show();
			}
		});
		((TextView) lonSideOfTheWorldBtn.findViewById(R.id.lon_side_of_the_world_tv)).setText(getString(R.string.west_abbreviation));
		ImageView westSideIv = lonSideOfTheWorldBtn.findViewById(R.id.west_side_iv);
		westSideIv.setImageDrawable(getColoredIcon(R.drawable.ic_action_coordinates_latitude, R.color.dashboard_blue));

		setBackgroundColor(R.id.point_name_divider, lightTheme ? R.color.route_info_divider_light : R.color.route_info_divider_dark);
		setBackgroundColor(R.id.point_name_et_container, lightTheme ? R.color.keyboard_item_control_light_bg : R.color.route_info_bottom_view_bg_dark);

		ImageView pointNameKeyboardBtn = (ImageView) mainView.findViewById(R.id.point_name_keyboard_btn);
		pointNameKeyboardBtn.setImageDrawable(getActiveIcon(R.drawable.ic_action_keyboard));
		pointNameKeyboardBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				View focusedView = getDialog().getCurrentFocus();
				if (focusedView != null) {
					useOsmandKeyboard = false;
					changeKeyboard();
					AndroidUtils.showSoftKeyboard(focusedView);
				}
			}
		});

		adapter = new CoordinateInputAdapter(mapActivity, mapMarkers);
		RecyclerView recyclerView = (RecyclerView) mainView.findViewById(R.id.markers_recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(ctx));
		recyclerView.setAdapter(adapter);
		recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
				super.onScrollStateChanged(recyclerView, newState);
				compassUpdateAllowed = newState == RecyclerView.SCROLL_STATE_IDLE;
			}
		});

		setBackgroundColor(R.id.bottom_controls_container, lightTheme
				? R.color.keyboard_item_control_light_bg : R.color.keyboard_item_control_dark_bg);
		TextView addButton = (TextView) mainView.findViewById(R.id.add_marker_button);
		addButton.setBackgroundResource(lightTheme ? R.drawable.route_info_go_btn_bg_light : R.drawable.route_info_go_btn_bg_dark);
		addButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				addMapMarker();
			}
		});

		mainView.findViewById(R.id.keyboard_layout).setBackgroundResource(lightTheme ? R.drawable.bg_bottom_menu_light : R.drawable.bg_bottom_menu_dark);

		Object[] keyboardItems = new Object[]{
				"1", "2", "3", R.drawable.ic_keyboard_next_field,
				"4", "5", "6", "-",
				"7", "8", "9", R.drawable.ic_keyboard_backspace,
				":", "0", ".", getString(R.string.shared_string_clear)
		};
		final GridView keyboardGrid = (GridView) mainView.findViewById(R.id.keyboard_grid_view);
		int dividersColorResId = lightTheme ? R.color.keyboard_divider_light : R.color.keyboard_divider_dark;
		setBackgroundColor(keyboardGrid, dividersColorResId);
		setBackgroundColor(R.id.keyboard_divider, dividersColorResId);
		final KeyboardAdapter keyboardAdapter = new KeyboardAdapter(mapActivity, keyboardItems);
		keyboardGrid.setAdapter(keyboardAdapter);
		keyboardGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
				if (useOsmandKeyboard) {
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
								} else {
									switchEditText(focusedEditText.getId(), false);
								}
								break;
							case SWITCH_TO_NEXT_INPUT_BUTTON_POSITION:
								switchEditText(focusedEditText.getId(), true);
								break;
							default:
								focusedEditText.setText(focusedEditText.getText().toString() + keyboardAdapter.getItem(i));
								focusedEditText.setSelection(focusedEditText.getText().length());
						}
					}
				}
			}
		});

		final ImageView showHideKeyboardIcon = (ImageView) mainView.findViewById(R.id.show_hide_keyboard_icon);
		showHideKeyboardIcon.setImageDrawable(getActiveIcon(R.drawable.ic_action_keyboard_hide));
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

	@ColorInt
	private int getResolvedColor(@ColorRes int resId) {
		return ContextCompat.getColor(getContext(), resId);
	}

	private void setBackgroundColor(@IdRes int viewResId, @ColorRes int colorResId) {
		setBackgroundColor(mainView.findViewById(viewResId), colorResId);
	}

	private void setBackgroundColor(View v, @ColorRes int colorResId) {
		v.setBackgroundColor(getResolvedColor(colorResId));
	}

	private void saveMarkers() {
		getMyApplication().getMapMarkersHelper().addMarkers(mapMarkers);
		if (listener != null) {
			listener.onMapMarkersSaved();
		}
	}

	private void addEditTexts(@IdRes int... ids) {
		editTexts.clear();
		for (int id : ids) {
			View v = mainView.findViewById(id);
			if (v != null && v instanceof EditText && v.getVisibility() == View.VISIBLE) {
				editTexts.add((EditText) mainView.findViewById(id));
			}
		}
	}

	private void registerInputs() {
		TextWatcher textWatcher = new TextWatcher() {

			private int strLength;

			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
				strLength = charSequence.length();
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
					int currentLength = str.length(); // todo
//					if (currentLength > strLength) {
//						int pointIndex = str.indexOf(".");
//						if (pointIndex != -1) {
//							int currentAccuracy = str.substring(pointIndex + 1).length();
//							if (currentAccuracy >= accuracy) {
//								switchEditText(focusedEditText.getId(), true);
//							}
//						}
//					}
				}
			}
		};

		final GestureDetector gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
			@Override
			public boolean onDoubleTap(MotionEvent e) {
				return true;
			}
		});

		View.OnTouchListener inputEditTextOnTouchListener = new View.OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent motionEvent) {
				if (useOsmandKeyboard) {
					if (!isOsmandKeyboardCurrentlyVisible()) {
						changeOsmandKeyboardVisibility(true);
					}
					EditText editText = (EditText) view;
					int inputType = editText.getInputType();
					editText.setInputType(InputType.TYPE_NULL);
					boolean doubleTap = gestureDetector.onTouchEvent(motionEvent);
					if (!doubleTap) {
						editText.onTouchEvent(motionEvent);
						editText.setSelection(editText.getText().length());
					}
					editText.setInputType(inputType);
					return true;
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
				if (useOsmandKeyboard) {
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
										switch (view.getId()) { // todo
//											case R.id.latitude_edit_text:
//												labelText = LATITUDE_LABEL;
//												break;
//											case R.id.longitude_edit_text:
//												labelText = LONGITUDE_LABEL;
//												break;
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
									default:
										return false;
								}
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

		TextView.OnEditorActionListener inputTextViewOnEditorActionListener = new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
				if (i == EditorInfo.IME_ACTION_NEXT) {
					switchEditText(textView.getId(), true);
				} else if (i == EditorInfo.IME_ACTION_DONE) {
					addMapMarker();
				}
				return false;
			}
		};

		clearInputs();

		int format = getMyApplication().getSettings().COORDS_INPUT_FORMAT.get();

		String firstSeparator = CoordinateInputFormats.getFirstSeparator(format);
		int secondPartSymbols = CoordinateInputFormats.getSecondPartSymbolsCount(format);

		((TextView) mainView.findViewById(R.id.lat_first_separator_tv)).setText(firstSeparator);
		((TextView) mainView.findViewById(R.id.lon_first_separator_tv)).setText(firstSeparator);

		InputFilter[] secondInputFilters = {new InputFilter.LengthFilter(secondPartSymbols)};

		EditText latSecondEt = (EditText) mainView.findViewById(R.id.lat_second_input_et);
		EditText lonSecondEt = (EditText) mainView.findViewById(R.id.lon_second_input_et);
		latSecondEt.setFilters(secondInputFilters);
		lonSecondEt.setFilters(secondInputFilters);
		latSecondEt.setHint(createHint(secondPartSymbols, 'x'));
		lonSecondEt.setHint(createHint(secondPartSymbols, 'y'));

		boolean containsThirdPart = CoordinateInputFormats.containsThirdPart(format);

		int visibility = containsThirdPart ? View.VISIBLE : View.GONE;
		mainView.findViewById(R.id.lat_second_separator_tv).setVisibility(visibility);
		mainView.findViewById(R.id.lon_second_separator_tv).setVisibility(visibility);
		mainView.findViewById(R.id.lat_third_input_et).setVisibility(visibility);
		mainView.findViewById(R.id.lon_third_input_et).setVisibility(visibility);

		if (containsThirdPart) {
			String secondSeparator = CoordinateInputFormats.getSecondSeparator(format);
			int thirdPartSymbols = CoordinateInputFormats.getThirdPartSymbolsCount(format);

			((TextView) mainView.findViewById(R.id.lat_second_separator_tv)).setText(secondSeparator);
			((TextView) mainView.findViewById(R.id.lon_second_separator_tv)).setText(secondSeparator);

			InputFilter[] thirdInputFilters = {new InputFilter.LengthFilter(thirdPartSymbols)};

			EditText latThirdEt = (EditText) mainView.findViewById(R.id.lat_third_input_et);
			EditText lonThirdEt = (EditText) mainView.findViewById(R.id.lon_third_input_et);
			latThirdEt.setFilters(thirdInputFilters);
			lonThirdEt.setFilters(thirdInputFilters);
			latThirdEt.setHint(createHint(thirdPartSymbols, 'x'));
			lonThirdEt.setHint(createHint(thirdPartSymbols, 'y'));
		}

		addEditTexts(R.id.lat_first_input_et, R.id.lat_second_input_et, R.id.lat_third_input_et,
				R.id.lon_first_input_et, R.id.lon_second_input_et, R.id.lon_third_input_et, R.id.point_name_et);

		for (EditText et : editTexts) {
			if (et.getId() != R.id.point_name_et) {
				et.addTextChangedListener(textWatcher);
			}
			et.setOnTouchListener(inputEditTextOnTouchListener);
			et.setOnLongClickListener(inputEditTextOnLongClickListener);
			et.setOnEditorActionListener(inputTextViewOnEditorActionListener);
		}

		changeEditTextSelections();

		editTexts.get(0).requestFocus();
	}

	private String createHint(int symbolsCount, char symbol) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < symbolsCount; i++) {
			sb.append(symbol);
		}
		return sb.toString();
	}

	private void changeKeyboard() {
		if (!useOsmandKeyboard && isOsmandKeyboardCurrentlyVisible()) {
			changeOsmandKeyboardVisibility(false);
		}
		changeEditTextSelections();
	}

	private CoordinateInputBottomSheetDialogFragment.CoordinateInputFormatChangeListener createCoordinateInputFormatChangeListener() {
		return new CoordinateInputBottomSheetDialogFragment.CoordinateInputFormatChangeListener() {

			@Override
			public void onKeyboardChanged(boolean useOsmandKeyboard) {
				CoordinateInputDialogFragment.this.useOsmandKeyboard = useOsmandKeyboard;
				changeKeyboard();
			}

			@Override
			public void onHandChanged() {
				changeHand();
			}

			@Override
			public void onFormatChanged() {
				registerInputs();
			}
		};
	}

	private void changeHand() {
		((FrameLayout) mainView.findViewById(R.id.left_container)).removeViewAt(0);
		((FrameLayout) mainView.findViewById(R.id.right_container)).removeViewAt(0);
		registerMainView();
	}

	private void changeEditTextSelections() {
		for (EditText et : editTexts) {
			et.setSelection(et.getText().length());
		}
	}

	private boolean isOsmandKeyboardCurrentlyVisible() {
		return orientationPortrait
				? mainView.findViewById(R.id.keyboard_grid_view).getVisibility() == View.VISIBLE
				: mainView.findViewById(R.id.keyboard_layout).getVisibility() == View.VISIBLE;
	}

	private void changeOsmandKeyboardVisibility(boolean show) {
		int visibility = show ? View.VISIBLE : View.GONE;
		if (orientationPortrait) {
			mainView.findViewById(R.id.keyboard_grid_view).setVisibility(visibility);
			mainView.findViewById(R.id.keyboard_divider).setVisibility(visibility);
		} else {
			mainView.findViewById(R.id.keyboard_layout).setVisibility(visibility);
		}
		((ImageView) mainView.findViewById(R.id.show_hide_keyboard_icon)).setImageDrawable(getActiveIcon(show
				? R.drawable.ic_action_keyboard_hide : R.drawable.ic_action_keyboard_show));
	}

	private void switchEditText(int currentId, boolean toNext) {
		int currentInd = getEditTextIndById(currentId);
		int newInd = currentInd + (toNext ? 1 : -1);
		if (currentInd >= 0 && currentInd < editTexts.size() && newInd >= 0 && newInd < editTexts.size()) {
			editTexts.get(newInd).requestFocus();
		}
	}

	private int getEditTextIndById(int id) {
		int res = -1;
		for (int i = 0; i < editTexts.size(); i++) {
			if (id == editTexts.get(i).getId()) {
				return i;
			}
		}
		return res;
	}

	private void addMapMarker() { // todo
//		final String latitude = ((EditText) mainView.findViewById(R.id.latitude_edit_text)).getText().toString();
//		final String longitude = ((EditText) mainView.findViewById(R.id.longitude_edit_text)).getText().toString();
//		double lat = parseCoordinate(latitude);
//		double lon = parseCoordinate(longitude);
//		if (lat == 0 || lon == 0) {
//			if (lon == 0) {
//				((OsmandTextFieldBoxes) mainView.findViewById(R.id.latitude_box)).setError("", true);
//			}
//			if (lat == 0) {
//				((OsmandTextFieldBoxes) mainView.findViewById(R.id.longitude_box)).setError("", true);
//			}
//		} else {
//			String name = ((EditText) mainView.findViewById(R.id.name_edit_text)).getText().toString();
//			addMapMarker(new LatLon(lat, lon), name);
//		}
	}

	private double parseCoordinate(String s) {
		List<Double> d = new ArrayList<>();
		List<Object> all = new ArrayList<>();
		List<String> strings = new ArrayList<>();
		LocationParser.splitObjects(s, d, all, strings);
		double coordinate = LocationParser.parse1Coordinate(all, 0, all.size());
		if (coordinate == 0 && d.size() == 1) {
			coordinate = d.get(0);
		}
		return coordinate;
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
	}

	private void clearInputs() {
		for (EditText et : editTexts) {
			et.setText("");
			et.clearFocus();
		}
		if (editTexts.size() > 0) {
			editTexts.get(0).requestFocus();
		}
	}

	private Drawable getActiveIcon(@DrawableRes int resId) {
		return getColoredIcon(resId, lightTheme ? R.color.icon_color : R.color.coordinate_input_active_icon_dark);
	}

	private Drawable getColoredIcon(@DrawableRes int resId, @ColorRes int colorResId) {
		return getMyApplication().getIconsCache().getIcon(resId, colorResId);
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

		private ColorStateList dividerControlColorStateList = AndroidUtils.createColorStateList(getContext(), false,
				R.color.keyboard_item_divider_control_color_light, R.color.keyboard_item_divider_control_color_light_pressed,
				0, 0);
		private ColorStateList numberColorStateList = AndroidUtils.createColorStateList(getContext(), false,
				R.color.keyboard_item_text_color_light, R.color.keyboard_item_text_color_light_pressed,
				0, 0);

		KeyboardAdapter(@NonNull Context context, @NonNull Object[] objects) {
			super(context, 0, objects);
		}

		@NonNull
		@Override
		public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
			if (convertView == null) {
				convertView = LayoutInflater.from(getContext()).inflate(R.layout.coordinate_input_keyboard_item, parent, false);
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
			View keyboardItemTopSpacing = convertView.findViewById(R.id.keyboard_item_top_spacing);
			View keyboardItemBottomSpacing = convertView.findViewById(R.id.keyboard_item_bottom_spacing);
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
				boolean dividerControlButton = position == CLEAR_BUTTON_POSITION
						|| position == MINUS_BUTTON_POSITION
						|| position == POINT_BUTTON_POSITION
						|| position == COLON_BUTTON_POSITION;
				if (lightTheme) {
					keyboardItemText.setTextColor(dividerControlButton ? dividerControlColorStateList : numberColorStateList);
				} else {
					keyboardItemText.setTextColor(getResolvedColor(dividerControlButton
							? R.color.keyboard_item_divider_control_color_dark : R.color.keyboard_item_text_color_dark));
				}
				keyboardItemImage.setVisibility(View.GONE);
				keyboardItemTopSpacing.setVisibility(View.VISIBLE);
				keyboardItemBottomSpacing.setVisibility(View.VISIBLE);
				keyboardItemText.setVisibility(View.VISIBLE);
				keyboardItemText.setText((String) getItem(position));
			} else if (item instanceof Integer) {
				keyboardItemTopSpacing.setVisibility(View.GONE);
				keyboardItemBottomSpacing.setVisibility(View.GONE);
				keyboardItemText.setVisibility(View.GONE);
				keyboardItemImage.setVisibility(View.VISIBLE);
				Drawable icon;
				if (lightTheme) {
					icon = DrawableCompat.wrap(getResources().getDrawable((Integer) item));
					DrawableCompat.setTintList(icon, numberColorStateList);
				} else {
					icon = getColoredIcon((Integer) item, R.color.keyboard_item_divider_control_color_dark);
				}
				keyboardItemImage.setImageDrawable(icon);
			}

			return convertView;
		}
	}

	interface OnMapMarkersSavedListener {
		void onMapMarkersSaved();
	}
}
