package net.osmand.plus.mapmarkers;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.CompoundButtonCompat;
import android.support.v7.widget.ListPopupWindow;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.github.ksoichiro.android.observablescrollview.ObservableScrollView;
import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks;
import com.github.ksoichiro.android.observablescrollview.ScrollState;

import net.osmand.AndroidUtils;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.MapMarkersMode;
import net.osmand.plus.OsmandSettings.OsmandPreference;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.plus.helpers.AndroidUiHelper;

import java.util.LinkedList;

public class DirectionIndicationDialogFragment extends BaseOsmAndDialogFragment {

	public final static String TAG = "DirectionIndicationDialogFragment";

	private DirectionIndicationFragmentListener listener;
	private View mainView;

	private int helpImgHeight;
	private boolean shadowVisible;

	public void setListener(DirectionIndicationFragmentListener listener) {
		this.listener = listener;
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		final OsmandSettings settings = getSettings();
		helpImgHeight = getResources().getDimensionPixelSize(R.dimen.action_bar_image_height);

		mainView = UiUtilities.getInflater(getContext(), !settings.isLightContent()).inflate(R.layout.fragment_direction_indication_dialog, container);

		Toolbar toolbar = (Toolbar) mainView.findViewById(R.id.toolbar);
		toolbar.setNavigationIcon(getIconsCache().getIcon(R.drawable.ic_arrow_back));
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dismiss();
			}
		});

		TextView appModeTv = (TextView) mainView.findViewById(R.id.app_mode_text_view);
		ApplicationMode appMode = settings.APPLICATION_MODE.get();
		appModeTv.setText(appMode.toHumanString(getContext()));
		appModeTv.setCompoundDrawablesWithIntrinsicBounds(null, null, getIconsCache().getIcon(
			appMode.getIconRes()), null);

		if (AndroidUiHelper.isOrientationPortrait(getActivity())) {
			((ObservableScrollView) mainView.findViewById(R.id.scroll_view)).setScrollViewCallbacks(new ObservableScrollViewCallbacks() {
				@Override
				public void onScrollChanged(int scrollY, boolean firstScroll, boolean dragging) {
					if (scrollY >= helpImgHeight) {
						if (!shadowVisible) {
							mainView.findViewById(R.id.app_bar_shadow).setVisibility(View.VISIBLE);
							shadowVisible = true;
						}
					} else if (shadowVisible) {
						mainView.findViewById(R.id.app_bar_shadow).setVisibility(View.GONE);
						shadowVisible = false;
					}
				}

				@Override
				public void onDownMotionEvent() {

				}

				@Override
				public void onUpOrCancelMotionEvent(ScrollState scrollState) {

				}
			});
		}

		updateHelpImage();

		final TextView menuTv = (TextView) mainView.findViewById(R.id.active_markers_text_view);
		menuTv.setText(settings.DISPLAYED_MARKERS_WIDGETS_COUNT.get() == 1 ? R.string.shared_string_one : R.string.shared_string_two);
		menuTv.setCompoundDrawablesWithIntrinsicBounds(null, null, getContentIcon(R.drawable.ic_action_arrow_drop_down), null);
		menuTv.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Context themedContext = UiUtilities.getThemedContext(getActivity(), !settings.isLightContent());
				CharSequence[] titles = getMenuTitles();
				Paint paint = new Paint();
				paint.setTextSize(getResources().getDimensionPixelSize(R.dimen.default_list_text_size));
				float titleTextWidth = Math.max(paint.measureText(titles[0].toString()), paint.measureText(titles[1].toString()));
				float itemWidth = titleTextWidth + AndroidUtils.dpToPx(themedContext, 32);
				float minWidth = AndroidUtils.dpToPx(themedContext, 100);
				final ListPopupWindow listPopupWindow = new ListPopupWindow(themedContext);
				listPopupWindow.setAnchorView(menuTv);
				listPopupWindow.setContentWidth((int) (Math.max(itemWidth, minWidth)));
				listPopupWindow.setDropDownGravity(Gravity.END | Gravity.TOP);
				listPopupWindow.setHorizontalOffset(AndroidUtils.dpToPx(themedContext, 8));
				listPopupWindow.setVerticalOffset(-menuTv.getHeight());
				listPopupWindow.setModal(true);
				listPopupWindow.setAdapter(new ArrayAdapter<>(themedContext, R.layout.popup_list_text_item, titles));
				listPopupWindow.setOnItemClickListener(new AdapterView.OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
						updateDisplayedMarkersCount(position == 0 ? 1 : 2);
						listPopupWindow.dismiss();
					}
				});
				listPopupWindow.show();
			}
		});

		final CompoundButton distanceIndicationToggle = (CompoundButton) mainView.findViewById(R.id.distance_indication_switch);
		distanceIndicationToggle.setChecked(settings.MARKERS_DISTANCE_INDICATION_ENABLED.get());
		mainView.findViewById(R.id.distance_indication_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				updateChecked(settings.MARKERS_DISTANCE_INDICATION_ENABLED, distanceIndicationToggle);
				updateSelection(true);
			}
		});

		mainView.findViewById(R.id.top_bar_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				settings.MAP_MARKERS_MODE.set(MapMarkersMode.TOOLBAR);
				updateSelection(true);
			}
		});

		mainView.findViewById(R.id.widget_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				settings.MAP_MARKERS_MODE.set(MapMarkersMode.WIDGETS);
				updateSelection(true);
			}
		});

		updateSelection(false);

		final CompoundButton showArrowsToggle = (CompoundButton) mainView.findViewById(R.id.show_arrows_switch);
		showArrowsToggle.setChecked(settings.SHOW_ARROWS_TO_FIRST_MARKERS.get());
		mainView.findViewById(R.id.show_arrows_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				updateChecked(settings.SHOW_ARROWS_TO_FIRST_MARKERS, showArrowsToggle);
			}
		});

		final CompoundButton showLinesToggle = (CompoundButton) mainView.findViewById(R.id.show_guide_line_switch);
		showLinesToggle.setChecked(settings.SHOW_LINES_TO_FIRST_MARKERS.get());
		mainView.findViewById(R.id.show_guide_line_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				updateChecked(settings.SHOW_LINES_TO_FIRST_MARKERS, showLinesToggle);
			}
		});

		final CompoundButton oneTapActiveToggle = (CompoundButton) mainView.findViewById(R.id.one_tap_active_switch);
		oneTapActiveToggle.setChecked(settings.SELECT_MARKER_ON_SINGLE_TAP.get());
		mainView.findViewById(R.id.one_tap_active_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				updateChecked(settings.SELECT_MARKER_ON_SINGLE_TAP, oneTapActiveToggle);
			}
		});

		final CompoundButton keepPassedToggle = (CompoundButton) mainView.findViewById(R.id.keep_passed_switch);
		keepPassedToggle.setChecked(settings.KEEP_PASSED_MARKERS_ON_MAP.get());
		mainView.findViewById(R.id.keep_passed_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				updateChecked(settings.KEEP_PASSED_MARKERS_ON_MAP, keepPassedToggle);
			}
		});

		return mainView;
	}

	@Override
	protected Drawable getContentIcon(int id) {
		return getIcon(id, getSettings().isLightContent() ? R.color.icon_color_default_light : R.color.icon_color_default_dark);
	}

	private MapActivity getMapActivity() {
		Activity activity = getActivity();
		if (activity != null && activity instanceof MapActivity) {
			return (MapActivity) activity;
		}
		return null;
	}

	private CharSequence[] getMenuTitles() {
		if (getSettings().DISPLAYED_MARKERS_WIDGETS_COUNT.get() == 1) {
			return new CharSequence[]{getActiveString(R.string.shared_string_one), getString(R.string.shared_string_two)};
		}
		return new CharSequence[]{getString(R.string.shared_string_one), getActiveString(R.string.shared_string_two)};
	}

	private SpannableString getActiveString(int id) {
		SpannableString res = new SpannableString(getString(id));
		res.setSpan(new ForegroundColorSpan(ContextCompat.getColor(getActivity(), getSettings().isLightContent()
				? R.color.active_color_primary_light : R.color.active_color_primary_dark)), 0, res.length(), 0);
		return res;
	}

	private void updateHelpImage() {
		if (Build.VERSION.SDK_INT >= 18) {
			OsmandSettings settings = getSettings();
			int count = settings.DISPLAYED_MARKERS_WIDGETS_COUNT.get();
			LinkedList<Drawable> imgList = new LinkedList<>();
			imgList.add(getDeviceImg());
			if (settings.SHOW_LINES_TO_FIRST_MARKERS.get()) {
				imgList.add(getGuideLineOneImg());
				if (count == 2) {
					imgList.add(getGuideLineTwoImg());
				}
			}
			if (settings.SHOW_ARROWS_TO_FIRST_MARKERS.get()) {
				imgList.add(getArrowOneImg());
				if (count == 2) {
					imgList.add(getArrowTwoImg());
				}
			}
			if (settings.MARKERS_DISTANCE_INDICATION_ENABLED.get()) {
				if (settings.MAP_MARKERS_MODE.get().isWidgets()) {
					imgList.add(getWidget1Img());
					if (count == 2) {
						imgList.add(getWidget2Img());
					}
				} else {
					imgList.add(getTopBar1Img());
					if (count == 2) {
						imgList.add(getTopBar2Img());
					}
				}
			}
			((ImageView) mainView.findViewById(R.id.action_bar_image))
					.setImageDrawable(new LayerDrawable(imgList.toArray(new Drawable[imgList.size()])));
		} else {
			mainView.findViewById(R.id.action_bar_image_container).setVisibility(View.GONE);
		}
	}

	private Drawable getTopBar2Img() {
		return getIconsCache().getIcon(getSettings().isLightContent()
				? R.drawable.img_help_markers_direction_topbar_2_day : R.drawable.img_help_markers_direction_topbar_2_night);
	}

	private Drawable getTopBar1Img() {
		return getIconsCache().getIcon(getSettings().isLightContent()
				? R.drawable.img_help_markers_direction_topbar_1_day : R.drawable.img_help_markers_direction_topbar_1_night);
	}

	private Drawable getWidget2Img() {
		return getIconsCache().getIcon(getSettings().isLightContent()
				? R.drawable.img_help_markers_direction_widget_2_day : R.drawable.img_help_markers_direction_widget_2_night);
	}

	private Drawable getWidget1Img() {
		return getIconsCache().getIcon(getSettings().isLightContent()
				? R.drawable.img_help_markers_direction_widget_1_day : R.drawable.img_help_markers_direction_widget_1_night);
	}

	private Drawable getArrowOneImg() {
		return getIconsCache().getIcon(getSettings().isLightContent()
				? R.drawable.img_help_markers_direction_arrow_one_day : R.drawable.img_help_markers_direction_arrow_one_night);
	}

	private Drawable getArrowTwoImg() {
		return getIconsCache().getIcon(getSettings().isLightContent()
				? R.drawable.img_help_markers_direction_arrow_two_day : R.drawable.img_help_markers_direction_arrow_two_night);
	}

	private Drawable getGuideLineOneImg() {
		return getIconsCache().getIcon(getSettings().isLightContent()
				? R.drawable.img_help_markers_direction_guideline_one_day : R.drawable.img_help_markers_direction_guideline_one_night);
	}

	private Drawable getGuideLineTwoImg() {
		return getIconsCache().getIcon(getSettings().isLightContent()
				? R.drawable.img_help_markers_direction_guideline_two_day : R.drawable.img_help_markers_direction_guideline_two_night);
	}

	private Drawable getDeviceImg() {
		return getIconsCache().getIcon(getSettings().isLightContent()
				? R.drawable.img_help_markers_direction_device_day : R.drawable.img_help_markers_direction_device_night);
	}

	private Drawable getIconBackground(boolean active) {
		return active ? getIcon(R.drawable.ic_action_device_top, R.color.active_color_primary_light)
				: getContentIcon(R.drawable.ic_action_device_top);
	}

	private Drawable getIconTop(int id, boolean active) {
		return active ? getIcon(id, R.color.active_color_primary_dark) : getContentIcon(id);
	}

	private void updateDisplayedMarkersCount(int count) {
		((TextView) mainView.findViewById(R.id.active_markers_text_view))
				.setText(count == 1 ? R.string.shared_string_one : R.string.shared_string_two);
		getSettings().DISPLAYED_MARKERS_WIDGETS_COUNT.set(count);
		updateSelection(true);
	}

	private void updateChecked(OsmandPreference<Boolean> setting, CompoundButton button) {
		boolean newState = !setting.get();
		setting.set(newState);
		button.setChecked(newState);
		refreshMap();
		updateHelpImage();
	}

	private void refreshMap() {
		if (getMapActivity() != null) {
			getMapActivity().refreshMap();
		}
	}

	private void notifyListener() {
		if (listener != null) {
			listener.onMapMarkersModeChanged(getSettings().MARKERS_DISTANCE_INDICATION_ENABLED.get());
		}
	}

	private void updateSelection(boolean notifyListener) {
		OsmandSettings settings = getSettings();
		MapMarkersMode mode = settings.MAP_MARKERS_MODE.get();
		boolean distIndEnabled = settings.MARKERS_DISTANCE_INDICATION_ENABLED.get();
		int count = settings.DISPLAYED_MARKERS_WIDGETS_COUNT.get();
		int topBarIconId = count == 1 ? R.drawable.ic_action_device_topbar : R.drawable.ic_action_device_topbar_two;
		int widgetIconId = count == 1 ? R.drawable.ic_action_device_widget : R.drawable.ic_action_device_widget_two;
		updateIcon(R.id.top_bar_icon, topBarIconId, mode.isToolbar() && distIndEnabled);
		updateIcon(R.id.widget_icon, widgetIconId, mode.isWidgets() && distIndEnabled);
		updateMarkerModeRow(R.id.top_bar_row, R.id.top_bar_radio_button, mode.isToolbar(), distIndEnabled);
		updateMarkerModeRow(R.id.widget_row, R.id.widget_radio_button, mode.isWidgets(), distIndEnabled);
		if (notifyListener) {
			notifyListener();
		}
		updateHelpImage();
	}

	private void updateIcon(int imageViewId, int drawableId, boolean active) {
		ImageView iv = (ImageView) mainView.findViewById(imageViewId);
		iv.setBackgroundDrawable(getIconBackground(active));
		iv.setImageDrawable(getIconTop(drawableId, active));
	}

	private void updateMarkerModeRow(int rowId, int radioButtonId, boolean checked, boolean active) {
		boolean night = !getSettings().isLightContent();
		RadioButton rb = (RadioButton) mainView.findViewById(radioButtonId);
		int colorId = active ? night ? R.color.active_color_primary_dark : R.color.active_color_primary_light
				: night ? R.color.icon_color_default_dark : R.color.icon_color_default_light;
		rb.setChecked(checked);
		CompoundButtonCompat.setButtonTintList(rb, ColorStateList.valueOf(ContextCompat.getColor(getContext(), colorId)));
		mainView.findViewById(rowId).setEnabled(active);
	}

	public interface DirectionIndicationFragmentListener {
		void onMapMarkersModeChanged(boolean showDirectionEnabled);
	}
}
