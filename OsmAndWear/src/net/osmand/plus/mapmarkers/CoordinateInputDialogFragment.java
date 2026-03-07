package net.osmand.plus.mapmarkers;

import static android.content.ClipDescription.MIMETYPE_TEXT_PLAIN;
import static android.content.Context.CLIPBOARD_SERVICE;

import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import net.osmand.IndexConstants;
import net.osmand.Location;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.plus.OsmAndLocationProvider.OsmAndCompassListener;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.shared.gpx.TrackItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapmarkers.CoordinateInputBottomSheetDialogFragment.CoordinateInputFormatChangeListener;
import net.osmand.plus.mapmarkers.CoordinateInputFormats.DDM;
import net.osmand.plus.mapmarkers.CoordinateInputFormats.DMS;
import net.osmand.plus.mapmarkers.CoordinateInputFormats.Format;
import net.osmand.plus.mapmarkers.adapters.CoordinateInputAdapter;
import net.osmand.plus.plugins.monitoring.SavingTrackHelper;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.track.GpxSelectionParams;
import net.osmand.plus.track.fragments.TrackMenuFragment;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FileUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.EditTextEx;
import net.osmand.plus.widgets.tools.SimpleTextWatcher;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.util.Algorithms;
import net.osmand.util.LocationParser;
import net.osmand.util.MapUtils;

import java.io.File;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CoordinateInputDialogFragment extends DialogFragment implements OsmAndCompassListener, OsmAndLocationListener {

	public static final String TAG = "CoordinateInputDialogFragment";
	public static final String ADDED_POINTS_NUMBER_KEY = "added_points_number_key";

	private static final String SELECTED_POINT_KEY = "selected_point_key";
	public static final double SOFT_KEYBOARD_MIN_DETECTION_SIZE = 0.15;

	private GpxFile newGpxFile;
	private OnPointsSavedListener listener;
	private WptPt selectedWpt;
	private SavingTrackHelper savingTrackHelper;
	private GpxSelectionHelper selectedGpxHelper;
	private RecyclerView recyclerView;

	private View mainView;
	private final List<EditTextEx> editTexts = new ArrayList<>();
	private CoordinateInputAdapter adapter;
	private Snackbar snackbar;

	private boolean lightTheme;
	private boolean orientationPortrait;
	private boolean hasUnsavedChanges;

	private boolean softKeyboardShown;
	private boolean shouldShowOsmandKeyboard;
	private int keyboardViewHeight;

	private boolean north = true;
	private boolean east = true;

	private Location location;
	private Float heading;
	private boolean locationUpdateStarted;
	private boolean compassUpdateAllowed = true;


	public void setListener(OnPointsSavedListener listener) {
		this.listener = listener;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		OsmandApplication app = getMyApplication();

		lightTheme = app.getSettings().isLightContent();
		setStyle(STYLE_NO_FRAME, lightTheme ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme);
		newGpxFile = new GpxFile(Version.getFullVersion(app));
		savingTrackHelper = app.getSavingTrackHelper();
		selectedGpxHelper = app.getSelectedGpxHelper();
	}

	@Nullable
	private GpxFile getGpx() {
		return newGpxFile;
	}

	private void syncGpx(GpxFile gpxFile) {
		MapMarkersHelper helper = getMyApplication().getMapMarkersHelper();
		MapMarkersGroup group = helper.getMarkersGroup(gpxFile);
		if (group != null) {
			helper.runSynchronization(group);
		}
	}

	protected void addWpt(GpxFile gpx, String description, String name, String category, int color, double lat, double lon) {
		if (gpx != null) {
			if (gpx.isShowCurrentTrack()) {
				savingTrackHelper.insertPointData(lat, lon, description, name, category, color);
				selectedGpxHelper.setGpxFileToDisplay(gpx);
			} else {
				WptPt point = WptPt.Companion.createAdjustedPoint(lat, lon, description, name, category, color, null, null, null, null);
				gpx.addPoint(point);
			}
		}
	}

	protected void updateWpt(GpxFile gpx, @Nullable String description, String name, String category, int color, double lat, double lon) {
		if (gpx != null) {
			if (gpx.isShowCurrentTrack()) {
				savingTrackHelper.updatePointData(selectedWpt, lat, lon, description, name, category, color, null, null);
				selectedGpxHelper.setGpxFileToDisplay(gpx);
			} else {
				WptPt wptInfo = new WptPt(lat, lon, description, name, category,
						Algorithms.colorToString(color), null, null);
				gpx.updateWptPt(selectedWpt, wptInfo, true);
			}
		}
	}

	private void quit() {
		if (getGpx().hasWptPt() && hasUnsavedChanges) {
			if (Algorithms.isEmpty(getGpx().getPath())) {
				showSaveDialog();
			} else {
				GpxFile gpx = getGpx();
				new SaveGpxAsyncTask(getMyApplication(), gpx, null, false).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				syncGpx(gpx);
				if (listener != null) {
					listener.onPointsSaved();
				}
				dismiss();
			}
		} else {
			dismiss();
		}
	}

	private void showSaveDialog() {
		hasUnsavedChanges = false;
		SaveAsTrackBottomSheetDialogFragment fragment = new SaveAsTrackBottomSheetDialogFragment();
		Bundle args = new Bundle();
		args.putInt(ADDED_POINTS_NUMBER_KEY, getGpx().getPointsSize());
		args.putBoolean(SaveAsTrackBottomSheetDialogFragment.COORDINATE_INPUT_MODE_KEY, true);
		fragment.setArguments(args);
		fragment.setListener(createSaveAsTrackFragmentListener());
		fragment.show(getChildFragmentManager(), SaveAsTrackBottomSheetDialogFragment.TAG);
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Context ctx = getContext();
		Dialog dialog = new Dialog(ctx, getTheme()) {
			@Override
			public void onBackPressed() {
				if (isOsmandKeyboardCurrentlyVisible()) {
					changeOsmandKeyboardVisibility(false);
				} else {
					quit();
				}
			}
		};
		Window window = dialog.getWindow();
		if (window != null) {
			window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
			if (!lightTheme) {
				window.setStatusBarColor(getResolvedColor(R.color.status_bar_main_dark));
			}
		}
		return dialog;
	}

	public void setGpx(GpxFile gpx) {
		this.newGpxFile = gpx;
		adapter.setGpx(gpx);
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			int pos = savedInstanceState.getInt(SELECTED_POINT_KEY, -1);
			if (pos != -1) {
				selectedWpt = adapter.getItem(pos);
			}
		}
		orientationPortrait = AndroidUiHelper.isOrientationPortrait(getActivity());

		Fragment optionsFragment = getChildFragmentManager().findFragmentByTag(CoordinateInputBottomSheetDialogFragment.TAG);
		if (optionsFragment != null) {
			((CoordinateInputBottomSheetDialogFragment) optionsFragment).setListener(createCoordinateInputFormatChangeListener());
		}

		mainView = inflater.inflate(R.layout.fragment_coordinate_input_dialog, container);

		ImageButton backBtn = mainView.findViewById(R.id.back_button);
		backBtn.setOnClickListener(view -> quit());

		TextView optionsButton = mainView.findViewById(R.id.options_button);

		if (orientationPortrait) {
			Drawable icBack = getColoredIcon(AndroidUtils.getNavigationIconResId(getContext()), lightTheme ? R.color.activity_background_color_dark : R.color.active_buttons_and_links_text_dark);
			backBtn.setImageDrawable(icBack);
			optionsButton.setTextColor(ColorUtilities.getActiveColor(getContext(), !lightTheme));
			TextView toolbar = mainView.findViewById(R.id.toolbar_text);
			toolbar.setTextColor(ColorUtilities.getPrimaryTextColor(getContext(), !lightTheme));
			toolbar.setText(R.string.coord_input_add_point);
			setBackgroundColor(R.id.app_bar, lightTheme ? R.color.card_and_list_background_light : R.color.card_and_list_background_dark);
			setBackgroundColor(mainView, ColorUtilities.getActivityBgColorId(!lightTheme));
		} else {
			int resId = AndroidUtils.getNavigationIconResId(getContext());
			int color = ColorUtilities.getActiveButtonsAndLinksTextColorId(!lightTheme);
			Drawable icBack = getColoredIcon(resId, color);
			backBtn.setImageDrawable(icBack);
			optionsButton.setTextColor(getResolvedColor(lightTheme ? R.color.card_and_list_background_light : R.color.active_color_primary_dark));
			TextView toolbar = mainView.findViewById(R.id.toolbar_text);
			toolbar.setTextColor(getResolvedColor(lightTheme ? R.color.card_and_list_background_light : R.color.text_color_primary_dark));
			toolbar.setText(R.string.coord_input_add_point);
			setBackgroundColor(R.id.app_bar, lightTheme ? R.color.app_bar_main_light : R.color.card_and_list_background_dark);
		}

		optionsButton.setOnClickListener(view -> {
			CoordinateInputBottomSheetDialogFragment fragment = new CoordinateInputBottomSheetDialogFragment();
			fragment.setUsedOnMap(false);
			fragment.setListener(createCoordinateInputFormatChangeListener());
			fragment.show(getChildFragmentManager(), CoordinateInputBottomSheetDialogFragment.TAG);
		});

		registerMainView();

		return mainView;
	}

	private void registerMainView() {
		Context ctx = getContext();
		if (ctx == null) {
			return;
		}

		if (orientationPortrait) {
			View.OnClickListener backspaceOnClickListener = v -> {
				if (v.getId() == R.id.lat_backspace_btn) {
					clearInputs(R.id.lat_first_input_et, R.id.lat_second_input_et, R.id.lat_third_input_et);
				} else {
					clearInputs(R.id.lon_first_input_et, R.id.lon_second_input_et, R.id.lon_third_input_et);
				}
			};

			ImageView latBackspaceBtn = mainView.findViewById(R.id.lat_backspace_btn);
			latBackspaceBtn.setImageDrawable(getActiveIcon(R.drawable.ic_action_clear_all_fields));
			latBackspaceBtn.setOnClickListener(backspaceOnClickListener);

			ImageView lonBackspaceBtn = mainView.findViewById(R.id.lon_backspace_btn);
			lonBackspaceBtn.setImageDrawable(getActiveIcon(R.drawable.ic_action_clear_all_fields));
			lonBackspaceBtn.setOnClickListener(backspaceOnClickListener);

		} else {
			boolean rightHand = getMyApplication().getSettings().COORDS_INPUT_USE_RIGHT_SIDE.get();
			LinearLayout handContainer = mainView.findViewById(R.id.hand_container);

			View dataAreaView = View.inflate(ctx, R.layout.coordinate_input_land_data_area, null);
			View keyboardAndListView = View.inflate(ctx, R.layout.coordinate_input_land_keyboard_and_list, null);
			setBackgroundColor(dataAreaView, lightTheme ? R.color.card_and_list_background_light : R.color.card_and_list_background_dark);
			setBackgroundColor(keyboardAndListView, ColorUtilities.getActivityBgColorId(!lightTheme));
			((FrameLayout) handContainer.findViewById(R.id.left_container)).addView(rightHand ? dataAreaView : keyboardAndListView, 0);
			((FrameLayout) handContainer.findViewById(R.id.right_container)).addView(rightHand ? keyboardAndListView : dataAreaView, 0);

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

		setupSideOfTheWorldBtns(R.id.lat_side_of_the_world_btn, R.id.lon_side_of_the_world_btn);

		setBackgroundColor(R.id.point_name_divider, lightTheme ? R.color.divider_color_light : R.color.divider_color_dark);
		setBackgroundColor(R.id.point_name_et_container, lightTheme ? R.color.activity_background_color_light : R.color.card_and_list_background_dark);

		ImageView pointNameKeyboardBtn = mainView.findViewById(R.id.point_name_keyboard_btn);
		pointNameKeyboardBtn.setImageDrawable(getColoredIcon(R.drawable.ic_action_keyboard, R.color.icon_color_default_light));
		pointNameKeyboardBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (isOsmandKeyboardCurrentlyVisible()) {
					changeOsmandKeyboardVisibility(false);
					if (isOsmandKeyboardOn()) {
						shouldShowOsmandKeyboard = true;
					}
				}
				for (EditText et : editTexts) {
					if (et.getId() == R.id.point_name_et) {
						et.requestFocus();
					}
				}
				View focusedView = getDialog().getCurrentFocus();
				if (focusedView != null) {
					AndroidUtils.softKeyboardDelayed(getActivity(), focusedView);
				}
			}
		});
		adapter = new CoordinateInputAdapter(ctx, getGpx());
		recyclerView = mainView.findViewById(R.id.markers_recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(ctx));
		recyclerView.setAdapter(adapter);
		recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
				super.onScrollStateChanged(recyclerView, newState);
				compassUpdateAllowed = newState == RecyclerView.SCROLL_STATE_IDLE;
			}
		});
		adapter.setOnClickListener(v -> {
			int pos = recyclerView.getChildAdapterPosition(v);
			if (pos == RecyclerView.NO_POSITION) {
				return;
			}
			enterEditingMode(adapter.getItem(pos));
		});
		adapter.setOnActionsClickListener(v -> {
			RecyclerView.ViewHolder viewHolder = recyclerView.findContainingViewHolder(v);
			if (viewHolder != null) {
				int pos = viewHolder.getAdapterPosition();
				if (pos == RecyclerView.NO_POSITION) {
					return;
				}
				Bundle args = new Bundle();
				args.putInt(CoordinateInputAdapter.ADAPTER_POSITION_KEY, pos);
				CoordinateInputActionsBottomSheet fragment = new CoordinateInputActionsBottomSheet();
				fragment.setUsedOnMap(false);
				fragment.setArguments(args);
				fragment.setListener(createCoordinateInputActionsListener());
				fragment.show(getChildFragmentManager(), CoordinateInputActionsBottomSheet.TAG);
			}
		});

		setBackgroundColor(R.id.bottom_controls_container, lightTheme
				? R.color.activity_background_color_light : R.color.card_and_list_background_dark);
		TextView addButton = mainView.findViewById(R.id.add_marker_button);
		@ColorRes int colorId = lightTheme ? R.color.active_color_primary_light : R.color.active_color_primary_dark;
		addButton.setCompoundDrawablesWithIntrinsicBounds(null, null, getColoredIcon(R.drawable.ic_action_type_add, colorId), null);
		addButton.setText(R.string.shared_string_add);
		addButton.setOnClickListener(view -> {
			addWptPt();
			hasUnsavedChanges = true;
		});

		TextView cancelButton = mainView.findViewById(R.id.cancel_button);
		cancelButton.setText(R.string.shared_string_cancel);
		cancelButton.setOnClickListener(view -> {
			dismissEditingMode();
			clearInputs();
		});
		View keyboardLayout = mainView.findViewById(R.id.keyboard_layout);
		keyboardLayout.setBackgroundResource(lightTheme
				? R.drawable.bg_bottom_menu_light : R.drawable.bg_coordinate_input_keyboard_dark);

		View keyboardView = mainView.findViewById(R.id.keyboard_view);

		int dividersColorResId = lightTheme ? R.color.divider_color_light : R.color.divider_color_dark;
		setBackgroundColor(keyboardView, dividersColorResId);
		setBackgroundColor(R.id.keyboard_divider, dividersColorResId);

		View.OnClickListener onClickListener = v -> {
			if (isOsmandKeyboardOn()) {
				View focusedView = getDialog().getCurrentFocus();
				if (focusedView != null && focusedView instanceof EditText) {
					EditText focusedEditText = (EditText) focusedView;
					int id = v.getId();
					if (id == R.id.keyboard_item_clear) {
						focusedEditText.setText("");
					} else if (id == R.id.keyboard_item_backspace) {
						String str = focusedEditText.getText().toString();
						if (str.length() > 0) {
							str = str.substring(0, str.length() - 1);
							focusedEditText.setText(str);
							focusedEditText.setSelection(str.length());
						} else {
							switchEditText(focusedEditText.getId(), false);
						}
					} else if (id == R.id.keyboard_item_next_field) {
						switchEditText(focusedEditText.getId(), true);
					} else if (id == R.id.keyboard_item_hide) {
						changeOsmandKeyboardVisibility(false);
					} else {
						focusedEditText.setText(focusedEditText.getText().toString() + getItemObjectById(id));
						focusedEditText.setSelection(focusedEditText.getText().length());
					}
				}
			}
		};

		setupKeyboardItems(keyboardView, onClickListener,
				R.id.keyboard_item_0,
				R.id.keyboard_item_1,
				R.id.keyboard_item_2,
				R.id.keyboard_item_3,
				R.id.keyboard_item_4,
				R.id.keyboard_item_5,
				R.id.keyboard_item_6,
				R.id.keyboard_item_7,
				R.id.keyboard_item_8,
				R.id.keyboard_item_9,
				R.id.keyboard_item_clear,
				R.id.keyboard_item_next_field,
				R.id.keyboard_item_backspace,
				R.id.keyboard_item_hide);

		if (!isOsmandKeyboardOn() && isOsmandKeyboardCurrentlyVisible()) {
			changeOsmandKeyboardVisibility(false);
		}
		if (selectedWpt == null) {
			if ((isOsmandKeyboardCurrentlyVisible() || softKeyboardShown)) {
				scrollToLastPoint();
			}
		} else {
			enterEditingMode(selectedWpt);
		}

		mainView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
			Rect r = new Rect();
			mainView.getWindowVisibleDisplayFrame(r);
			int screenHeight = mainView.getRootView().getHeight();
			int keypadHeight = screenHeight - r.bottom;
			boolean softKeyboardVisible = keypadHeight > screenHeight * SOFT_KEYBOARD_MIN_DETECTION_SIZE;
			if (softKeyboardShown && !softKeyboardVisible) {
				if (shouldShowOsmandKeyboard) {
					changeOsmandKeyboardVisibility(true);
					shouldShowOsmandKeyboard = false;
				}
			} else if (!softKeyboardShown && softKeyboardVisible && selectedWpt == null) {
				scrollToLastPoint();
			}
			softKeyboardShown = softKeyboardVisible;

			int height = keyboardLayout.getHeight();

			if (height > keyboardViewHeight) {
				keyboardViewHeight = height;
				if (isOsmandKeyboardCurrentlyVisible()) {
					if (selectedWpt == null && adapter.getItemCount() > 1) {
						scrollToLastPoint();
					} else {
						setPaddingToRecyclerViewBottom(keyboardViewHeight);
					}
				}
			}
		});
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (selectedWpt != null) {
			outState.putInt(SELECTED_POINT_KEY, adapter.getItemPosition(selectedWpt));
		}
	}

	private void scrollToLastPoint() {
		scrollToPoint(adapter.getItemCount() - 1);
	}

	private void scrollToPoint(WptPt point) {
		if (point != null) {
			scrollToPoint(adapter.getItemPosition(point));
		}
	}

	private void scrollToPoint(int position) {
		int itemsSize = adapter.getItemCount();
		if ((position < 0) || !(itemsSize > 1) || (itemsSize < position)) {
			return;
		}
		if (isOsmandKeyboardCurrentlyVisible() && keyboardViewHeight > 0) {
			setPaddingToRecyclerViewBottom(keyboardViewHeight);
		}
		((LinearLayoutManager) recyclerView.getLayoutManager()).scrollToPositionWithOffset(position, 0);
	}

	private void setPaddingToRecyclerViewBottom(int padding) {
		if (recyclerView.getPaddingBottom() == padding) {
			return;
		}
		recyclerView.setPadding(recyclerView.getPaddingLeft(), recyclerView.getPaddingTop(), recyclerView.getPaddingRight(), padding);
	}

	private void setupKeyboardItems(View keyboardView, View.OnClickListener listener, @IdRes int... itemsIds) {
		@DrawableRes int itemBg = lightTheme ? R.drawable.keyboard_item_light_bg : R.drawable.keyboard_item_dark_bg;
		@DrawableRes int controlItemBg = lightTheme ? R.drawable.keyboard_item_control_light_bg : R.drawable.keyboard_item_control_dark_bg;

		Context ctx = requireContext();
		ColorStateList clearItemTextColorStateList = AndroidUtils.createPressedColorStateList(ctx,
				R.color.icon_color_default_light, R.color.keyboard_item_divider_control_color_light_pressed);
		ColorStateList numberColorStateList = AndroidUtils.createPressedColorStateList(ctx,
				R.color.keyboard_item_text_color_light, R.color.keyboard_item_text_color_light_pressed);

		@ColorInt int textColorDark = getResolvedColor(R.color.keyboard_item_text_color_dark);

		for (@IdRes int id : itemsIds) {
			View itemView = keyboardView.findViewById(id);
			Object item = getItemObjectById(id);
			boolean controlItem = id == R.id.keyboard_item_next_field
					|| id == R.id.keyboard_item_backspace
					|| id == R.id.keyboard_item_hide;

			itemView.setBackgroundResource(controlItem ? controlItemBg : itemBg);
			itemView.setOnClickListener(listener);

			View itemTopSpace = itemView.findViewById(R.id.keyboard_item_top_spacing);
			View itemBottomSpace = itemView.findViewById(R.id.keyboard_item_bottom_spacing);
			TextView itemTv = itemView.findViewById(R.id.keyboard_item_text);
			ImageView itemIv = itemView.findViewById(R.id.keyboard_item_image);

			if (item instanceof String) {
				boolean clearItem = id == R.id.keyboard_item_clear;
				if (clearItem) {
					TextViewCompat.setAutoSizeTextTypeWithDefaults(itemTv, TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE);
					itemTv.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.default_list_text_size));
				} else {
					TextViewCompat.setAutoSizeTextTypeWithDefaults(itemTv, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM);
				}
				if (lightTheme) {
					itemTv.setTextColor(clearItem ? clearItemTextColorStateList : numberColorStateList);
				} else {
					itemTv.setTextColor(clearItem ? getResolvedColor(R.color.icon_color_default_dark) : textColorDark);
				}
				itemTopSpace.setVisibility(View.VISIBLE);
				itemTv.setVisibility(View.VISIBLE);
				itemTv.setText((String) item);
				itemIv.setVisibility(View.GONE);
				itemBottomSpace.setVisibility(View.VISIBLE);
			} else if (item instanceof Drawable) {
				itemTopSpace.setVisibility(View.GONE);
				itemTv.setVisibility(View.GONE);
				itemIv.setVisibility(View.VISIBLE);
				itemBottomSpace.setVisibility(View.GONE);
				Drawable icon = DrawableCompat.wrap((Drawable) item);
				if (lightTheme) {
					DrawableCompat.setTintList(icon, numberColorStateList);
				} else {
					int color = ContextCompat.getColor(ctx, R.color.icon_color_default_dark);
					DrawableCompat.setTint(icon, color);
				}
				itemIv.setImageDrawable(icon);
			}
		}
	}

	private Object getItemObjectById(@IdRes int id) {
		Context ctx = requireContext();
		if (id == R.id.keyboard_item_0) {
			return "0";
		} else if (id == R.id.keyboard_item_1) {
			return "1";
		} else if (id == R.id.keyboard_item_2) {
			return "2";
		} else if (id == R.id.keyboard_item_3) {
			return "3";
		} else if (id == R.id.keyboard_item_4) {
			return "4";
		} else if (id == R.id.keyboard_item_5) {
			return "5";
		} else if (id == R.id.keyboard_item_6) {
			return "6";
		} else if (id == R.id.keyboard_item_7) {
			return "7";
		} else if (id == R.id.keyboard_item_8) {
			return "8";
		} else if (id == R.id.keyboard_item_9) {
			return "9";
		} else if (id == R.id.keyboard_item_clear) {
			return getString(R.string.shared_string_clear);
		} else if (id == R.id.keyboard_item_next_field) {
			Drawable normal = AppCompatResources.getDrawable(ctx, R.drawable.ic_action_next_field_stroke);
			Drawable pressed = AppCompatResources.getDrawable(ctx, R.drawable.ic_action_next_field_fill);
			return AndroidUtils.createPressedStateListDrawable(normal, pressed);
		} else if (id == R.id.keyboard_item_backspace) {
			Drawable normal = AppCompatResources.getDrawable(ctx, R.drawable.ic_action_backspace_stroke);
			Drawable pressed = AppCompatResources.getDrawable(ctx, R.drawable.ic_action_backspace_fill);
			return AndroidUtils.createPressedStateListDrawable(normal, pressed);
		} else if (id == R.id.keyboard_item_hide) {
			return AppCompatResources.getDrawable(ctx, R.drawable.ic_action_keyboard_hide);
		}
		return -1;
	}

	@Override
	public void onResume() {
		super.onResume();

		startLocationUpdate();

		View focusedView = getDialog().getCurrentFocus();
		if (focusedView != null) {
			if (!isOsmandKeyboardOn()) {
				AndroidUtils.softKeyboardDelayed(getActivity(), focusedView);
			}
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		keyboardViewHeight = 0;
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

	private void setupSideOfTheWorldBtns(int... ids) {
		boolean doNotUseAnimations = getMyApplication().getSettings().DO_NOT_USE_ANIMATIONS.get();
		for (int id : ids) {
			View sideOfTheWorldBtn = mainView.findViewById(id);
			if (doNotUseAnimations) {
				((LinearLayout) sideOfTheWorldBtn).setLayoutTransition(null);
			}
			sideOfTheWorldBtn.setBackgroundResource(lightTheme
					? R.drawable.context_menu_controller_bg_light : R.drawable.context_menu_controller_bg_dark);
			sideOfTheWorldBtn.setOnClickListener(v -> updateSideOfTheWorldBtn(v, true));

			int colorId = ColorUtilities.getActiveColorId(!lightTheme);
			boolean lat = id == R.id.lat_side_of_the_world_btn;
			Drawable icon = getColoredIcon(
					lat ? R.drawable.ic_action_coordinates_longitude : R.drawable.ic_action_coordinates_latitude, colorId
			);
			((ImageView) sideOfTheWorldBtn.findViewById(lat ? R.id.north_side_iv : R.id.west_side_iv)).setImageDrawable(icon);
			((ImageView) sideOfTheWorldBtn.findViewById(lat ? R.id.south_side_iv : R.id.east_side_iv)).setImageDrawable(icon);
			((TextView) sideOfTheWorldBtn.findViewById(lat ? R.id.lat_side_of_the_world_tv : R.id.lon_side_of_the_world_tv))
					.setTextColor(getResolvedColor(colorId));

			updateSideOfTheWorldBtn(sideOfTheWorldBtn, false);
		}
	}

	private void updateSideOfTheWorldBtn(View view, boolean changeSide) {
		if (view.getId() == R.id.lat_side_of_the_world_btn) {
			if (changeSide) {
				north = !north;
			}
			String text = getString(north ? R.string.north_abbreviation : R.string.south_abbreviation);
			((TextView) view.findViewById(R.id.lat_side_of_the_world_tv)).setText(text);
			view.findViewById(R.id.north_side_iv).setVisibility(north ? View.VISIBLE : View.GONE);
			view.findViewById(R.id.south_side_iv).setVisibility(north ? View.GONE : View.VISIBLE);
		} else {
			if (changeSide) {
				east = !east;
			}
			String text = getString(east ? R.string.east_abbreviation : R.string.west_abbreviation);
			((TextView) view.findViewById(R.id.lon_side_of_the_world_tv)).setText(text);
			view.findViewById(R.id.west_side_iv).setVisibility(east ? View.GONE : View.VISIBLE);
			view.findViewById(R.id.east_side_iv).setVisibility(east ? View.VISIBLE : View.GONE);
		}
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

	private void addEditTexts(@IdRes int... ids) {
		editTexts.clear();
		for (int id : ids) {
			View v = mainView.findViewById(id);
			if (v instanceof EditTextEx && v.getVisibility() == View.VISIBLE) {
				editTexts.add(mainView.findViewById(id));
			}
		}
	}

	private void registerInputs() {
		TextWatcher textWatcher = new SimpleTextWatcher() {

			private int strLength;

			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
				strLength = charSequence.length();
			}

			@Override
			public void afterTextChanged(Editable editable) {
				View focusedView = getDialog().getCurrentFocus();
				if (focusedView != null && focusedView instanceof EditTextEx) {
					EditTextEx et = (EditTextEx) focusedView;
					int currentLength = et.getText().length();
					if (et.getId() == R.id.lon_first_input_et) {
						String lonFirstInput = et.getText().toString();
						if (currentLength == 2) {
							if (lonFirstInput.charAt(0) != '1' && lonFirstInput.charAt(0) != '0') {
								switchEditText(et.getId(), true);
							}
						}
					}
					if (et.getMaxSymbolsCount() > 0 && currentLength > strLength && currentLength >= et.getMaxSymbolsCount()) {
						switchEditText(et.getId(), true);
					}
				}
			}
		};

		GestureDetector gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
			@Override
			public boolean onDoubleTap(@NonNull MotionEvent e) {
				return true;
			}
		});

		View.OnTouchListener inputEditTextOnTouchListener = new View.OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent motionEvent) {
				if (isOsmandKeyboardOn()) {
					if (!isOsmandKeyboardCurrentlyVisible()) {
						if (softKeyboardShown) {
							if (view.getId() != R.id.point_name_et) {
								AndroidUtils.hideSoftKeyboard(getActivity(), view);
								shouldShowOsmandKeyboard = true;
							} else {
								return false;
							}
						} else {
							changeOsmandKeyboardVisibility(true);
						}
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

		View.OnLongClickListener inputEditTextOnLongClickListener = view -> {
			if (isOsmandKeyboardOn()) {
				EditText inputEditText = (EditText) view;
				PopupMenu popupMenu = new PopupMenu(getContext(), inputEditText);
				Menu menu = popupMenu.getMenu();
				popupMenu.getMenuInflater().inflate(R.menu.copy_paste_menu, menu);
				ClipboardManager clipboardManager = ((ClipboardManager) getContext().getSystemService(CLIPBOARD_SERVICE));
				MenuItem pasteMenuItem = menu.findItem(R.id.action_paste);
				pasteMenuItem.setEnabled(clipboardManager != null && clipboardManager.hasPrimaryClip() &&
						clipboardManager.getPrimaryClipDescription().hasMimeType(MIMETYPE_TEXT_PLAIN));
				if (clipboardManager != null) {
					popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
						@Override
						public boolean onMenuItemClick(MenuItem item) {
							int i = item.getItemId();
							if (i == R.id.action_copy) {
								ClipData clip = ClipData.newPlainText("", inputEditText.getText().toString());
								clipboardManager.setPrimaryClip(clip);
								return true;
							} else if (i == R.id.action_paste) {
								ClipData.Item pasteItem = clipboardManager.getPrimaryClip().getItemAt(0);
								CharSequence pasteData = pasteItem.getText();
								if (pasteData != null) {
									String str = inputEditText.getText().toString();
									inputEditText.setText(str + pasteData);
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
			} else {
				return false;
			}
		};

		TextView.OnEditorActionListener inputTextViewOnEditorActionListener = (textView, i, keyEvent) -> {
			if (i == EditorInfo.IME_ACTION_NEXT) {
				switchEditText(textView.getId(), true);
			} else if (i == EditorInfo.IME_ACTION_DONE) {
				addWptPt();
				hasUnsavedChanges = true;
			}
			return false;
		};

		clearInputs();

		Format format = getMyApplication().getSettings().COORDS_INPUT_FORMAT.get();
		boolean useTwoDigitsLongtitude = getMyApplication().getSettings().COORDS_INPUT_TWO_DIGITS_LONGTITUDE.get();
		setupEditTextEx(R.id.lat_first_input_et, format.getFirstPartSymbolsCount(true, useTwoDigitsLongtitude), true);
		setupEditTextEx(R.id.lon_first_input_et, format.getFirstPartSymbolsCount(false, useTwoDigitsLongtitude), false);

		String firstSeparator = format.getFirstSeparator();
		((TextView) mainView.findViewById(R.id.lat_first_separator_tv)).setText(firstSeparator);
		((TextView) mainView.findViewById(R.id.lon_first_separator_tv)).setText(firstSeparator);

		int secondPartSymbols = format.getSecondPartSymbolsCount();
		setupEditTextEx(R.id.lat_second_input_et, secondPartSymbols, true);
		setupEditTextEx(R.id.lon_second_input_et, secondPartSymbols, false);

		boolean containsThirdPart = format.isContainsThirdPart();

		int visibility = containsThirdPart ? View.VISIBLE : View.GONE;
		mainView.findViewById(R.id.lat_second_separator_tv).setVisibility(visibility);
		mainView.findViewById(R.id.lon_second_separator_tv).setVisibility(visibility);
		mainView.findViewById(R.id.lat_third_input_et).setVisibility(visibility);
		mainView.findViewById(R.id.lon_third_input_et).setVisibility(visibility);

		if (containsThirdPart) {
			String secondSeparator = format.getSecondSeparator();
			((TextView) mainView.findViewById(R.id.lat_second_separator_tv)).setText(secondSeparator);
			((TextView) mainView.findViewById(R.id.lon_second_separator_tv)).setText(secondSeparator);

			int thirdPartSymbols = format.getThirdPartSymbolsCount();
			setupEditTextEx(R.id.lat_third_input_et, thirdPartSymbols, true);
			setupEditTextEx(R.id.lon_third_input_et, thirdPartSymbols, false);
		}

		addEditTexts(R.id.lat_first_input_et, R.id.lat_second_input_et, R.id.lat_third_input_et,
				R.id.lon_first_input_et, R.id.lon_second_input_et, R.id.lon_third_input_et, R.id.point_name_et);

		for (EditText et : editTexts) {
			if (et.getId() == R.id.lon_first_input_et) {
				((LinearLayout.LayoutParams) et.getLayoutParams()).weight = editTexts.get(0).getMaxSymbolsCount();
				et.requestLayout();
			}
			if (et.getId() != R.id.point_name_et) {
				et.addTextChangedListener(textWatcher);
			} else {
				et.setOnFocusChangeListener((v, hasFocus) -> {
					if (!hasFocus && isOsmandKeyboardOn() && (isOsmandKeyboardCurrentlyVisible() || softKeyboardShown)) {
						AndroidUtils.hideSoftKeyboard(getActivity(), v);
					}
				});
			}
			et.setOnTouchListener(inputEditTextOnTouchListener);
			et.setOnLongClickListener(inputEditTextOnLongClickListener);
			et.setOnEditorActionListener(inputTextViewOnEditorActionListener);
		}

		changeEditTextSelections();

		editTexts.get(0).requestFocus();
	}

	private void setupEditTextEx(@IdRes int etId, int symbols, boolean lat) {
		EditTextEx et = mainView.findViewById(etId);
		et.setMaxSymbolsCount(symbols);
		et.setHint(createString(symbols, lat ? 'x' : 'y'));
		((LinearLayout.LayoutParams) et.getLayoutParams()).weight = symbols;
		et.requestLayout();
	}

	private String createString(int symbolsCount, char symbol) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < symbolsCount; i++) {
			sb.append(symbol);
		}
		return sb.toString();
	}

	private boolean isOsmandKeyboardOn() {
		return getMyApplication().getSettings().COORDS_INPUT_USE_OSMAND_KEYBOARD.get();
	}

	private void changeOsmandKeyboardSetting() {
		OsmandPreference<Boolean> pref = getMyApplication().getSettings().COORDS_INPUT_USE_OSMAND_KEYBOARD;
		pref.set(!pref.get());
	}

	private void changeKeyboard() {
		changeOsmandKeyboardSetting();
		boolean useOsmandKeyboard = isOsmandKeyboardOn();
		changeOsmandKeyboardVisibility(useOsmandKeyboard);
		View focusedView = getDialog().getCurrentFocus();
		if (focusedView != null) {
			if (useOsmandKeyboard) {
				AndroidUtils.hideSoftKeyboard(getActivity(), focusedView);
			} else {
				new Handler().postDelayed(() -> AndroidUtils.showSoftKeyboard(getActivity(), focusedView), 200);
			}
		}
		changeEditTextSelections();
	}

	private CoordinateInputFormatChangeListener createCoordinateInputFormatChangeListener() {
		return new CoordinateInputFormatChangeListener() {

			@Override
			public void onKeyboardChanged() {
				changeKeyboard();
			}

			@Override
			public void onHandChanged() {
				changeHand();
			}

			@Override
			public void onInputSettingsChanged() {
				dismissEditingMode();
				registerInputs();
			}

			@Override
			public void saveAsTrack() {
				OsmandApplication app = getMyApplication();
				if (app != null) {
					if (!getGpx().hasWptPt()) {
						Toast.makeText(app, getString(R.string.plan_route_no_markers_toast), Toast.LENGTH_SHORT).show();
					} else {
						showSaveDialog();
					}
				}

			}
		};
	}

	private SaveAsTrackBottomSheetDialogFragment.MarkerSaveAsTrackFragmentListener createSaveAsTrackFragmentListener() {
		return new SaveAsTrackBottomSheetDialogFragment.MarkerSaveAsTrackFragmentListener() {

			final OsmandApplication app = getMyApplication();

			@Override
			public void saveGpx(String fileName) {
				new SaveGpxAsyncTask(app, getGpx(), fileName, false).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				hasUnsavedChanges = false;
				app.getMapMarkersHelper().addOrEnableGroup(getGpx());
				if (listener != null) {
					listener.onPointsSaved();
				}
				snackbar = Snackbar.make(mainView, String.format(getString(R.string.shared_string_file_is_saved), fileName) + ".", Snackbar.LENGTH_LONG)
						.setAction(R.string.shared_string_show, view -> TrackMenuFragment.openTrack(app, new File(getGpx().getPath()), null));
				UiUtilities.setupSnackbar(snackbar, !lightTheme);
				snackbar.show();
			}
		};
	}

	private CoordinateInputActionsBottomSheet.CoordinateInputActionsListener createCoordinateInputActionsListener() {
		return new CoordinateInputActionsBottomSheet.CoordinateInputActionsListener() {

			@Override
			public void removeItem(int position) {
				WptPt wpt = adapter.getItem(position);
				if (selectedWpt == wpt) {
					dismissEditingMode();
					clearInputs();
					showKeyboard();
				}
				adapter.removeItem(position);
				hasUnsavedChanges = true;
				snackbar = Snackbar.make(mainView, getString(R.string.point_deleted, wpt.getName()), Snackbar.LENGTH_LONG)
						.setAction(R.string.shared_string_undo, view -> {
							getGpx().addPoint(position, wpt);
							adapter.notifyDataSetChanged();
						});
				UiUtilities.setupSnackbar(snackbar, !lightTheme);
				snackbar.show();
			}

			@Override
			public void editItem(int position) {
				enterEditingMode(adapter.getItem(position));
			}
		};
	}

	private void changeHand() {
		dismissEditingMode();
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
				? mainView.findViewById(R.id.keyboard_view).getVisibility() == View.VISIBLE
				: mainView.findViewById(R.id.keyboard_layout).getVisibility() == View.VISIBLE;
	}

	private void changeOsmandKeyboardVisibility(boolean show) {
		int visibility = show ? View.VISIBLE : View.GONE;
		if (orientationPortrait) {
			mainView.findViewById(R.id.keyboard_view).setVisibility(visibility);
			mainView.findViewById(R.id.keyboard_divider).setVisibility(visibility);
		} else {
			mainView.findViewById(R.id.keyboard_layout).setVisibility(visibility);
		}
		if (show) {
			if (selectedWpt == null) {
				scrollToLastPoint();
			}
		} else {
			setPaddingToRecyclerViewBottom(AndroidUtils.dpToPx(getMyApplication(), 72));
		}
	}

	private void switchEditText(int currentId, boolean toNext) {
		int currentInd = getEditTextIndById(currentId);
		int newInd = currentInd + (toNext ? 1 : -1);
		if (currentInd >= 0 && currentInd < editTexts.size() && newInd >= 0 && newInd < editTexts.size()) {
			editTexts.get(newInd).requestFocus();
		}
	}

	private int getEditTextIndById(int id) {
		for (int i = 0; i < editTexts.size(); i++) {
			if (id == editTexts.get(i).getId()) {
				return i;
			}
		}
		return -1;
	}

	@Nullable
	private EditText getEditTextById(int id) {
		for (EditText et : editTexts) {
			if (et.getId() == id) {
				return et;
			}
		}
		return null;
	}

	private DecimalFormat createDecimalFormat(int accuracy) {
		return new DecimalFormat("###." + createString(accuracy, '#'), new DecimalFormatSymbols(Locale.US));
	}

	private void dismissEditingMode() {
		selectedWpt = null;
		TextView addButton = mainView.findViewById(R.id.add_marker_button);
		addButton.setText(R.string.shared_string_add);
		@ColorRes int colorId = lightTheme ? R.color.active_color_primary_light : R.color.active_color_primary_dark;
		addButton.setCompoundDrawablesWithIntrinsicBounds(null, null, getColoredIcon(R.drawable.ic_action_type_add, colorId), null);
		((TextView) mainView.findViewById(R.id.toolbar_text)).setText(R.string.coord_input_add_point);
	}

	private void enterEditingMode(WptPt wptPt) {
		selectedWpt = wptPt;
		Format format = getMyApplication().getSettings().COORDS_INPUT_FORMAT.get();
		double lat = Math.abs(wptPt.getLat());
		double lon = Math.abs(wptPt.getLon());
		if (format == Format.DD_MM_MMM || format == Format.DD_MM_MMMM) {
			int accuracy = format.getThirdPartSymbolsCount();
			updateInputsDdm(true, CoordinateInputFormats.ddToDdm(lat), accuracy);
			updateInputsDdm(false, CoordinateInputFormats.ddToDdm(lon), accuracy);
		} else if (format == Format.DD_DDDDD || format == Format.DD_DDDDDD) {
			int accuracy = format.getSecondPartSymbolsCount();
			updateInputsDd(true, lat, accuracy);
			updateInputsDd(false, lon, accuracy);
		} else if (format == Format.DD_MM_SS) {
			updateInputsDms(true, CoordinateInputFormats.ddToDms(lat));
			updateInputsDms(false, CoordinateInputFormats.ddToDms(lon));
		}
		boolean latPositive = wptPt.getLat() > 0;
		if ((latPositive && !north) || (!latPositive && north)) {
			updateSideOfTheWorldBtn(mainView.findViewById(R.id.lat_side_of_the_world_btn), true);
		}
		boolean lonPositive = wptPt.getLon() > 0;
		if ((lonPositive && !east) || (!lonPositive && east)) {
			updateSideOfTheWorldBtn(mainView.findViewById(R.id.lon_side_of_the_world_btn), true);
		}
		((EditText) mainView.findViewById(R.id.point_name_et)).setText(wptPt.getName());
		((TextView) mainView.findViewById(R.id.toolbar_text)).setText(R.string.coord_input_edit_point);
		TextView addButton = mainView.findViewById(R.id.add_marker_button);
		addButton.setText(R.string.shared_string_apply);
		@ColorRes int colorId = lightTheme ? R.color.active_color_primary_light : R.color.active_color_primary_dark;
		addButton.setCompoundDrawablesWithIntrinsicBounds(null, null, getColoredIcon(R.drawable.ic_action_type_apply, colorId), null);
		showKeyboard();
	}

	private void showKeyboard() {
		View focusedView = null;
		for (EditText et : editTexts) {
			if (et.getId() == R.id.lat_first_input_et) {
				et.requestFocus();
				focusedView = getDialog().getCurrentFocus();
			}
		}
		if (isOsmandKeyboardOn()) {
			if (!isOsmandKeyboardCurrentlyVisible()) {
				if (softKeyboardShown && focusedView != null) {
					AndroidUtils.hideSoftKeyboard(getActivity(), focusedView);
					shouldShowOsmandKeyboard = true;
					return;
				}
				changeOsmandKeyboardVisibility(true);
			}
		} else if (!softKeyboardShown && focusedView != null) {
			AndroidUtils.softKeyboardDelayed(getActivity(), focusedView);
		}
	}

	private void updateInputsDdm(boolean lat, DDM ddm, int accuracy) {
		String[] minutes = createDecimalFormat(accuracy).format(ddm.decimalMinutes).split("\\.");
		updateText(getFirstEt(lat), String.valueOf(ddm.degrees));
		updateText(getSecondEt(lat), minutes[0]);
		if (minutes.length > 1) {
			updateText(getThirdEt(lat), minutes[1]);
		}
	}

	private void updateInputsDd(boolean lat, double decimalDegrees, int accuracy) {
		String[] degrees = createDecimalFormat(accuracy).format(decimalDegrees).split("\\.");
		updateText(getFirstEt(lat), degrees[0]);
		if (degrees.length > 1) {
			updateText(getSecondEt(lat), degrees[1]);
		}
	}

	private void updateInputsDms(boolean lat, DMS dms) {
		updateText(getFirstEt(lat), String.valueOf(dms.degrees));
		updateText(getSecondEt(lat), String.valueOf(dms.minutes));
		updateText(getThirdEt(lat), String.valueOf((int) dms.seconds));
	}

	private void updateText(@Nullable EditText et, @NonNull String text) {
		if (et != null) {
			et.setText(text);
		}
	}

	@Nullable
	private EditText getFirstEt(boolean lat) {
		return getEditTextById(lat ? R.id.lat_first_input_et : R.id.lon_first_input_et);
	}

	@Nullable
	private EditText getSecondEt(boolean lat) {
		return getEditTextById(lat ? R.id.lat_second_input_et : R.id.lon_second_input_et);
	}

	@Nullable
	private EditText getThirdEt(boolean lat) {
		return getEditTextById(lat ? R.id.lat_third_input_et : R.id.lon_third_input_et);
	}

	private void addWptPt() {
		String latitude = getStringCoordinate(true);
		String longitude = getStringCoordinate(false);
		if (latitude.isEmpty() && longitude.isEmpty()) {
			Toast.makeText(getContext(), R.string.enter_lat_and_lon, Toast.LENGTH_SHORT).show();
		} else if (latitude.isEmpty()) {
			Toast.makeText(getContext(), R.string.enter_lat, Toast.LENGTH_SHORT).show();
		} else if (longitude.isEmpty()) {
			Toast.makeText(getContext(), R.string.enter_lon, Toast.LENGTH_SHORT).show();
		} else {
			double lat = parseCoordinate(latitude);
			double lon = parseCoordinate(longitude);
			String name = ((EditText) mainView.findViewById(R.id.point_name_et)).getText().toString();
			if (name.trim().isEmpty()) {
				name = getString(R.string.short_location_on_map, latitude, longitude).replace('\n', ' ');
			}
			if (selectedWpt != null) {
				updateWpt(getGpx(), null, name, null, 0, lat, lon);
				scrollToPoint(selectedWpt);
				dismissEditingMode();
			} else {
				addWpt(getGpx(), null, name, null, 0, lat, lon);
				scrollToLastPoint();
			}
			adapter.notifyDataSetChanged();
			clearInputs();
		}
	}

	private String getStringCoordinate(boolean latitude) {
		String firstPart = ((EditText) mainView.findViewById(latitude
				? R.id.lat_first_input_et : R.id.lon_first_input_et)).getText().toString();
		String secondPart = ((EditText) mainView.findViewById(latitude
				? R.id.lat_second_input_et : R.id.lon_second_input_et)).getText().toString();
		String thirdPart = ((EditText) mainView.findViewById(latitude
				? R.id.lat_third_input_et : R.id.lon_third_input_et)).getText().toString();

		if (firstPart.isEmpty() && secondPart.isEmpty() && thirdPart.isEmpty()) {
			return "";
		}

		if (firstPart.isEmpty()) {
			firstPart = "0";
		}
		if (secondPart.isEmpty()) {
			secondPart = "0";
		}

		Format format = getMyApplication().getSettings().COORDS_INPUT_FORMAT.get();
		StringBuilder res = new StringBuilder();
		if ((latitude && !north) || (!latitude && !east)) {
			res.append("-");
		}
		res.append(firstPart);
		if (!secondPart.isEmpty()) {
			res.append(format.getFirstSeparator()).append(secondPart);
		}
		if (!thirdPart.isEmpty() && format.isContainsThirdPart()) {
			res.append(format.getSecondSeparator()).append(thirdPart);
		}
		return res.toString();
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

	private void clearInputs(int... ids) {
		for (int id : ids) {
			View v = mainView.findViewById(id);
			if (v != null && v instanceof EditText) {
				EditText et = (EditText) v;
				et.setText("");
				et.clearFocus();
			}
		}
	}

	private void clearInputs() {
		for (EditText et : editTexts) {
			et.setText("");
			et.clearFocus();
		}
	}

	private Drawable getActiveIcon(@DrawableRes int resId) {
		return getColoredIcon(resId, lightTheme ? R.color.icon_color_default_light : R.color.coordinate_input_active_icon_dark);
	}

	private Drawable getColoredIcon(@DrawableRes int resId, @ColorRes int colorResId) {
		return getMyApplication().getUIUtilities().getIcon(resId, colorResId);
	}

	private OsmandApplication getMyApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}

	@Override
	public void updateLocation(Location location) {
		if (!MapUtils.areLatLonEqual(this.location, location)) {
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
		Activity activity = getActivity();
		if (activity != null && adapter != null) {
			OsmandApplication app = (OsmandApplication) activity.getApplication();
			app.runInUIThread(adapter::notifyDataSetChanged);
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

	private static class SaveGpxAsyncTask extends AsyncTask<Void, Void, Void> {
		private final OsmandApplication app;
		private final GpxFile gpx;
		private final boolean gpxSelected;
		private final String fileName;

		SaveGpxAsyncTask(OsmandApplication app, GpxFile gpx, String fileName, boolean gpxSelected) {
			this.app = app;
			this.gpx = gpx;
			this.fileName = fileName;
			this.gpxSelected = gpxSelected;
		}

		@Override
		protected Void doInBackground(Void... params) {
			if (Algorithms.isEmpty(gpx.getPath())) {
				if (!Algorithms.isEmpty(fileName)) {
					String dirName = IndexConstants.GPX_INDEX_DIR + IndexConstants.MAP_MARKERS_INDEX_DIR;
					File dir = app.getAppPath(dirName);
					if (!dir.exists()) {
						dir.mkdirs();
					}
					String uniqueFileName = FileUtils.createUniqueFileName(app, fileName, dirName, IndexConstants.GPX_FILE_EXT);
					File fout = new File(dir, uniqueFileName + IndexConstants.GPX_FILE_EXT);
					SharedUtil.writeGpxFile(fout, gpx);
				}
			} else {
				SharedUtil.writeGpxFile(new File(gpx.getPath()), gpx);
			}
			app.getSmartFolderHelper().addTrackItemToSmartFolder(new TrackItem(gpx));
			return null;
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			if (!gpxSelected) {
				GpxSelectionParams params = GpxSelectionParams.newInstance()
						.showOnMap().syncGroup().selectedByUser().addToHistory().saveSelection();
				app.getSelectedGpxHelper().selectGpxFile(gpx, params);
			}
		}
	}

	public interface OnPointsSavedListener {
		void onPointsSaved();
	}
}
