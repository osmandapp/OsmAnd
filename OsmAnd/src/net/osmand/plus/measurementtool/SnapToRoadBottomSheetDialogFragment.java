package net.osmand.plus.measurementtool;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.base.BottomSheetDialogFragment;
import net.osmand.plus.helpers.AndroidUiHelper;

import java.util.ArrayList;
import java.util.List;

public class SnapToRoadBottomSheetDialogFragment extends BottomSheetDialogFragment {

	public static final String TAG = "SnapToRoadBottomSheetDialogFragment";

	private SnapToRoadListener listener;
	private boolean nightMode;
	private boolean portrait;
	private boolean snapToRoadEnabled;

	public void setListener(SnapToRoadListener listener) {
		this.listener = listener;
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		nightMode = getMyApplication().getDaynightHelper().isNightModeForMapControls();
		portrait = AndroidUiHelper.isOrientationPortrait(getActivity());
		final OsmandSettings settings = getMyApplication().getSettings();
		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;

		final View mainView = View.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.fragment_snap_to_road_bottom_sheet_dialog, container);

		mainView.findViewById(R.id.cancel_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dismiss();
			}
		});
		if (nightMode) {
			((TextView) mainView.findViewById(R.id.choose_navigation_title)).setTextColor(getResources().getColor(R.color.ctx_menu_info_text_dark));
		}

		LinearLayout navContainer = (LinearLayout) mainView.findViewById(R.id.navigation_types_container);
		final List<ApplicationMode> modes = new ArrayList<>(ApplicationMode.values(settings));
		modes.remove(ApplicationMode.DEFAULT);
		View.OnClickListener onClickListener = new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				snapToRoadEnabled = true;
				if (listener != null) {
					listener.onApplicationModeItemClick(modes.get((int) view.getTag()));
				}
				dismiss();
			}
		};
		for (int i = 0; i < modes.size(); i++) {
			ApplicationMode mode = modes.get(i);
			View row = View.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.list_item_icon_and_title, null);
			((ImageView) row.findViewById(R.id.icon)).setImageDrawable(getContentIcon(mode.getSmallIconDark()));
			((TextView) row.findViewById(R.id.title)).setText(mode.toHumanString(getContext()));
			row.setOnClickListener(onClickListener);
			row.setTag(i);
			navContainer.addView(row);
		}

		final int screenHeight = AndroidUtils.getScreenHeight(getActivity());
		final int statusBarHeight = AndroidUtils.getStatusBarHeight(getActivity());
		final int navBarHeight = AndroidUtils.getNavBarHeight(getActivity());

		mainView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				final View scrollView = mainView.findViewById(R.id.navigation_types_scroll_view);
				int scrollViewHeight = scrollView.getHeight();
				int dividerHeight = AndroidUtils.dpToPx(getContext(), 1);
				int cancelButtonHeight = getContext().getResources().getDimensionPixelSize(R.dimen.measure_distance_bottom_sheet_cancel_button_height);
				int spaceForScrollView = screenHeight - statusBarHeight - navBarHeight - dividerHeight - cancelButtonHeight;
				if (scrollViewHeight > spaceForScrollView) {
					scrollView.getLayoutParams().height = spaceForScrollView;
					scrollView.requestLayout();
				}

				if (!portrait) {
					if (screenHeight - statusBarHeight - mainView.getHeight()
							>= AndroidUtils.dpToPx(getActivity(), 8)) {
						AndroidUtils.setBackground(getActivity(), mainView, nightMode,
								R.drawable.bg_bottom_sheet_topsides_landscape_light, R.drawable.bg_bottom_sheet_topsides_landscape_dark);
					} else {
						AndroidUtils.setBackground(getActivity(), mainView, nightMode,
								R.drawable.bg_bottom_sheet_sides_landscape_light, R.drawable.bg_bottom_sheet_sides_landscape_dark);
					}
				}

				ViewTreeObserver obs = mainView.getViewTreeObserver();
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
					obs.removeOnGlobalLayoutListener(this);
				} else {
					obs.removeGlobalOnLayoutListener(this);
				}
			}
		});

		return mainView;
	}

	@Override
	public void onStart() {
		super.onStart();
		if (!portrait) {
			final Window window = getDialog().getWindow();
			WindowManager.LayoutParams params = window.getAttributes();
			params.width = getActivity().getResources().getDimensionPixelSize(R.dimen.landscape_bottom_sheet_dialog_fragment_width);
			window.setAttributes(params);
		}
	}

	@Override
	public void onDestroyView() {
		if (listener != null) {
			listener.onDestroyView(snapToRoadEnabled);
		}
		super.onDestroyView();
	}

	@Override
	protected Drawable getContentIcon(@DrawableRes int id) {
		return getIcon(id, nightMode ? R.color.ctx_menu_info_text_dark : R.color.on_map_icon_color);
	}

	interface SnapToRoadListener {

		void onDestroyView(boolean snapToRoadEnabled);

		void onApplicationModeItemClick(ApplicationMode mode);
	}
}
