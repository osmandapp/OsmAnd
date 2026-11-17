package net.osmand.plus.mapmarkers;

import static net.osmand.plus.utils.UiUtilities.CompoundButtonType.PROFILE_DEPENDENT;
import static net.osmand.plus.views.mapwidgets.WidgetType.MARKERS_TOP_BAR;
import static net.osmand.plus.views.mapwidgets.WidgetsPanel.TOP;

import android.app.Activity;
import android.content.Context;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.ListPopupWindow;
import androidx.appcompat.widget.Toolbar;

import com.github.ksoichiro.android.observablescrollview.ObservableScrollView;
import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks;
import com.github.ksoichiro.android.observablescrollview.ScrollState;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.mapwidgets.WidgetsVisibilityHelper;

import java.util.LinkedList;

public class DirectionIndicationDialogFragment extends BaseOsmAndDialogFragment {

	public static final String TAG = DirectionIndicationDialogFragment.class.getSimpleName();

	private View mainView;

	private int helpImgHeight;
	private boolean shadowVisible;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		helpImgHeight = getResources().getDimensionPixelSize(R.dimen.action_bar_image_height);

		updateNightMode();
		mainView = themedInflater.inflate(R.layout.fragment_direction_indication_dialog, container);

		Toolbar toolbar = mainView.findViewById(R.id.toolbar);
		toolbar.setNavigationIcon(getIcon(AndroidUtils.getNavigationIconResId(getContext())));
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(view -> dismiss());

		TextView appModeTv = mainView.findViewById(R.id.app_mode_text_view);
		ApplicationMode appMode = settings.APPLICATION_MODE.get();
		appModeTv.setText(appMode.toHumanString());
		appModeTv.setCompoundDrawablesWithIntrinsicBounds(null, null, getIcon(appMode.getIconRes()), null);

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
			});
		}

		updateHelpImage();

		TextView menuTv = mainView.findViewById(R.id.active_markers_text_view);
		menuTv.setText(settings.DISPLAYED_MARKERS_WIDGETS_COUNT.get() == 1 ? R.string.shared_string_one : R.string.shared_string_two);
		menuTv.setCompoundDrawablesWithIntrinsicBounds(null, null, getContentIcon(R.drawable.ic_action_arrow_drop_down), null);
		menuTv.setOnClickListener(view -> {
			Context themedContext = UiUtilities.getThemedContext(getActivity(), !settings.isLightContent());
			CharSequence[] titles = getMenuTitles();
			Paint paint = new Paint();
			paint.setTextSize(getResources().getDimensionPixelSize(R.dimen.default_list_text_size));
			float titleTextWidth = Math.max(paint.measureText(titles[0].toString()), paint.measureText(titles[1].toString()));
			float itemWidth = titleTextWidth + AndroidUtils.dpToPx(themedContext, 32);
			float minWidth = AndroidUtils.dpToPx(themedContext, 100);
			ListPopupWindow listPopupWindow = new ListPopupWindow(themedContext);
			listPopupWindow.setAnchorView(menuTv);
			listPopupWindow.setContentWidth((int) (Math.max(itemWidth, minWidth)));
			listPopupWindow.setDropDownGravity(Gravity.END | Gravity.TOP);
			listPopupWindow.setHorizontalOffset(AndroidUtils.dpToPx(themedContext, 8));
			listPopupWindow.setVerticalOffset(-menuTv.getHeight());
			listPopupWindow.setModal(true);
			listPopupWindow.setAdapter(new ArrayAdapter<>(themedContext, R.layout.popup_list_text_item, titles));
			listPopupWindow.setOnItemClickListener((parent, v, position, id) -> {
				updateDisplayedMarkersCount(position == 0 ? 1 : 2);
				listPopupWindow.dismiss();
			});
			listPopupWindow.show();
		});

		updateHelpImage();

		CompoundButton showArrowsToggle = mainView.findViewById(R.id.show_arrows_switch);
		showArrowsToggle.setChecked(settings.SHOW_ARROWS_TO_FIRST_MARKERS.get());
		mainView.findViewById(R.id.show_arrows_row).setOnClickListener(view ->
				updateChecked(settings.SHOW_ARROWS_TO_FIRST_MARKERS, showArrowsToggle));
		UiUtilities.setupCompoundButton(showArrowsToggle, nightMode, PROFILE_DEPENDENT);

		CompoundButton showLinesToggle = mainView.findViewById(R.id.show_guide_line_switch);
		showLinesToggle.setChecked(settings.SHOW_LINES_TO_FIRST_MARKERS.get());
		mainView.findViewById(R.id.show_guide_line_row).setOnClickListener(view ->
				updateChecked(settings.SHOW_LINES_TO_FIRST_MARKERS, showLinesToggle));
		UiUtilities.setupCompoundButton(showLinesToggle, nightMode, PROFILE_DEPENDENT);

		CompoundButton oneTapActiveToggle = mainView.findViewById(R.id.one_tap_active_switch);
		oneTapActiveToggle.setChecked(settings.SELECT_MARKER_ON_SINGLE_TAP.get());
		mainView.findViewById(R.id.one_tap_active_row).setOnClickListener(view ->
				updateChecked(settings.SELECT_MARKER_ON_SINGLE_TAP, oneTapActiveToggle));
		UiUtilities.setupCompoundButton(oneTapActiveToggle, nightMode, PROFILE_DEPENDENT);

		CompoundButton keepPassedToggle = mainView.findViewById(R.id.keep_passed_switch);
		keepPassedToggle.setChecked(settings.KEEP_PASSED_MARKERS_ON_MAP.get());
		mainView.findViewById(R.id.keep_passed_row).setOnClickListener(v ->
				updateChecked(settings.KEEP_PASSED_MARKERS_ON_MAP, keepPassedToggle));
		UiUtilities.setupCompoundButton(keepPassedToggle, nightMode, PROFILE_DEPENDENT);

		return mainView;
	}

	@Override
	protected Drawable getContentIcon(int id) {
		return getIcon(id, ColorUtilities.getDefaultIconColorId(!settings.isLightContent()));
	}

	private MapActivity getMapActivity() {
		Activity activity = getActivity();
		return activity instanceof MapActivity ? ((MapActivity) activity) : null;
	}

	private CharSequence[] getMenuTitles() {
		if (settings.DISPLAYED_MARKERS_WIDGETS_COUNT.get() == 1) {
			return new CharSequence[] {getActiveString(R.string.shared_string_one), getString(R.string.shared_string_two)};
		}
		return new CharSequence[] {getString(R.string.shared_string_one), getActiveString(R.string.shared_string_two)};
	}

	private SpannableString getActiveString(int id) {
		SpannableString res = new SpannableString(getString(id));
		int activeColor = ColorUtilities.getActiveColor(getActivity(), !settings.isLightContent());
		res.setSpan(new ForegroundColorSpan(activeColor), 0, res.length(), 0);
		return res;
	}

	private void updateHelpImage() {
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
		MapActivity activity = getMapActivity();
		if (activity != null && WidgetsVisibilityHelper.isWidgetEnabled(activity, TOP, MARKERS_TOP_BAR.id)) {
			imgList.add(getTopBar1Img());
			if (count == 2) {
				imgList.add(getTopBar2Img());
			}
		}
		((ImageView) mainView.findViewById(R.id.action_bar_image))
				.setImageDrawable(new LayerDrawable(imgList.toArray(new Drawable[0])));
	}

	private Drawable getTopBar2Img() {
		return getIcon(settings.isLightContent()
				? R.drawable.img_help_markers_direction_topbar_2_day : R.drawable.img_help_markers_direction_topbar_2_night);
	}

	private Drawable getTopBar1Img() {
		return getIcon(settings.isLightContent()
				? R.drawable.img_help_markers_direction_topbar_1_day : R.drawable.img_help_markers_direction_topbar_1_night);
	}

	private Drawable getArrowOneImg() {
		return getIcon(settings.isLightContent()
				? R.drawable.img_help_markers_direction_arrow_one_day : R.drawable.img_help_markers_direction_arrow_one_night);
	}

	private Drawable getArrowTwoImg() {
		return getIcon(settings.isLightContent()
				? R.drawable.img_help_markers_direction_arrow_two_day : R.drawable.img_help_markers_direction_arrow_two_night);
	}

	private Drawable getGuideLineOneImg() {
		return getIcon(settings.isLightContent()
				? R.drawable.img_help_markers_direction_guideline_one_day : R.drawable.img_help_markers_direction_guideline_one_night);
	}

	private Drawable getGuideLineTwoImg() {
		return getIcon(settings.isLightContent()
				? R.drawable.img_help_markers_direction_guideline_two_day : R.drawable.img_help_markers_direction_guideline_two_night);
	}

	private Drawable getDeviceImg() {
		return getIcon(settings.isLightContent()
				? R.drawable.img_help_markers_direction_device_day : R.drawable.img_help_markers_direction_device_night);
	}

	private void updateDisplayedMarkersCount(int count) {
		((TextView) mainView.findViewById(R.id.active_markers_text_view))
				.setText(count == 1 ? R.string.shared_string_one : R.string.shared_string_two);
		settings.DISPLAYED_MARKERS_WIDGETS_COUNT.set(count);
		updateHelpImage();
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
}
