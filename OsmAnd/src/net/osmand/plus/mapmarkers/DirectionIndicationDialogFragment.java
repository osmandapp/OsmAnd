package net.osmand.plus.mapmarkers;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.CompoundButtonCompat;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.RadioButton;
import android.widget.TextView;

import com.github.ksoichiro.android.observablescrollview.ObservableScrollView;
import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks;
import com.github.ksoichiro.android.observablescrollview.ScrollState;

import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.MapMarkersMode;
import net.osmand.plus.OsmandSettings.OsmandPreference;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndDialogFragment;

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

		mainView = inflater.inflate(R.layout.fragment_direction_indication_dialog, container);

		Toolbar toolbar = (Toolbar) mainView.findViewById(R.id.toolbar);
		toolbar.setNavigationIcon(getIconsCache().getIcon(R.drawable.ic_arrow_back));
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dismiss();
			}
		});

		TextView appModeTv = (TextView) mainView.findViewById(R.id.app_mode_text_view);
		ApplicationMode appMode = settings.APPLICATION_MODE.get();
		appModeTv.setText(appMode.getStringResource());
		appModeTv.setCompoundDrawablesWithIntrinsicBounds(null, null, getIconsCache().getIcon(appMode.getSmallIconDark()), null);

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

		updateHelpImage();

		final TextView menuTv = (TextView) mainView.findViewById(R.id.active_markers_text_view);
		menuTv.setText(settings.DISPLAYED_MARKERS_WIDGETS_COUNT.get() == 1 ? R.string.shared_string_one : R.string.shared_string_two);
		menuTv.setCompoundDrawablesWithIntrinsicBounds(null, null, getContentIcon(R.drawable.ic_action_arrow_drop_down), null);
		menuTv.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				int count = settings.DISPLAYED_MARKERS_WIDGETS_COUNT.get();
				PopupMenu popupMenu = new PopupMenu(getActivity(), menuTv);
				Menu menu = popupMenu.getMenu();
				popupMenu.getMenuInflater().inflate(R.menu.active_markers_menu, menu);
				setupActiveMenuItem(menu.findItem(count == 1 ? R.id.action_one : R.id.action_two));
				popupMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						switch (item.getItemId()) {
							case R.id.action_one:
								updateDisplayedMarkersCount(1);
								return true;
							case R.id.action_two:
								updateDisplayedMarkersCount(2);
								return true;
						}
						return false;
					}
				});
				popupMenu.show();
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

		return mainView;
	}

	@Override
	protected Drawable getContentIcon(int id) {
		return getIcon(id, getSettings().isLightContent() ? R.color.icon_color : R.color.ctx_menu_info_text_dark);
	}

	private MapActivity getMapActivity() {
		Activity activity = getActivity();
		if (activity != null && activity instanceof MapActivity) {
			return (MapActivity) activity;
		}
		return null;
	}

	private void updateHelpImage() {
		OsmandSettings settings = getSettings();
		int count = settings.DISPLAYED_MARKERS_WIDGETS_COUNT.get();
		LinkedList<Drawable> imgList = new LinkedList<>();
		imgList.add(getDeviceImg());
		if (settings.SHOW_LINES_TO_FIRST_MARKERS.get()) {
			imgList.add(getGuideLineTwoImg());
			if (count == 2) {
				imgList.add(getGuideLineOneImg());
			}
		}
		if (settings.SHOW_ARROWS_TO_FIRST_MARKERS.get()) {
			imgList.add(getArrowTwoImg());
			if (count == 2) {
				imgList.add(getArrowOneImg());
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

	private void setupActiveMenuItem(MenuItem item) {
		int stringId = item.getItemId() == R.id.action_one ? R.string.shared_string_one : R.string.shared_string_two;
		SpannableString title = new SpannableString(getString(stringId));
		title.setSpan(new ForegroundColorSpan(ContextCompat.getColor(getActivity(), getSettings().isLightContent()
				? R.color.dashboard_blue : R.color.osmand_orange)), 0, title.length(), 0);
		item.setTitle(title);
	}

	private Drawable getIconBackground(boolean active) {
		return active ? getIcon(R.drawable.ic_action_device_top, R.color.dashboard_blue)
				: getContentIcon(R.drawable.ic_action_device_top);
	}

	private Drawable getIconTop(int id, boolean active) {
		return active ? getIcon(id, R.color.osmand_orange)
				: getContentIcon(id);
	}

	private void updateDisplayedMarkersCount(int count) {
		((TextView) mainView.findViewById(R.id.active_markers_text_view))
				.setText(count == 1 ? R.string.shared_string_one : R.string.shared_string_two);
		getSettings().DISPLAYED_MARKERS_WIDGETS_COUNT.set(count);
		notifyListener();
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

	private void notifyListener() {
		if (listener != null) {
			listener.onMapMarkersModeChanged(getSettings().MARKERS_DISTANCE_INDICATION_ENABLED.get());
		}
	}

	private void updateSelection(boolean notifyListener) {
		OsmandSettings settings = getSettings();
		MapMarkersMode mode = settings.MAP_MARKERS_MODE.get();
		boolean distIndEnabled = settings.MARKERS_DISTANCE_INDICATION_ENABLED.get();
		updateIcon(R.id.top_bar_icon, R.drawable.ic_action_device_topbar, mode.isToolbar() && distIndEnabled);
		updateIcon(R.id.widget_icon, R.drawable.ic_action_device_widget, mode.isWidgets() && distIndEnabled);
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
		int colorId = active ? night ? R.color.osmand_orange : R.color.dashboard_blue
				: night ? R.color.ctx_menu_info_text_dark : R.color.icon_color;
		rb.setChecked(checked);
		CompoundButtonCompat.setButtonTintList(rb, ColorStateList.valueOf(ContextCompat.getColor(getContext(), colorId)));
		mainView.findViewById(rowId).setEnabled(active);
	}

	public interface DirectionIndicationFragmentListener {
		void onMapMarkersModeChanged(boolean showDirectionEnabled);
	}
}
