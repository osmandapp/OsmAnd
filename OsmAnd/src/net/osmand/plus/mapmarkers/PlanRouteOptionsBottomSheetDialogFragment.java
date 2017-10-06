package net.osmand.plus.mapmarkers;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.base.BottomSheetDialogFragment;
import net.osmand.plus.helpers.AndroidUiHelper;

public class PlanRouteOptionsBottomSheetDialogFragment extends BottomSheetDialogFragment {

	public final static String TAG = "PlanRouteOptionsBottomSheetDialogFragment";

	private boolean portrait;
	private boolean night;
	private PlanRouteOptionsFragmentListener listener;
	private boolean selectAll;

	public void setListener(PlanRouteOptionsFragmentListener listener) {
		this.listener = listener;
	}

	public void setSelectAll(boolean selectAll) {
		this.selectAll = selectAll;
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		portrait = AndroidUiHelper.isOrientationPortrait(getActivity());
		night = getMyApplication().getDaynightHelper().isNightModeForMapControls();
		final int themeRes = night ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;

		final View mainView = View.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.fragment_plan_route_options_bottom_sheet_dialog, container);
		if (portrait) {
			AndroidUtils.setBackground(getActivity(), mainView, night, R.drawable.bg_bottom_menu_light, R.drawable.bg_bottom_menu_dark);
		}

		if (night) {
			((TextView) mainView.findViewById(R.id.title)).setTextColor(ContextCompat.getColor(getActivity(), R.color.ctx_menu_info_text_dark));
		}

		((ImageView) mainView.findViewById(R.id.navigate_icon)).setImageDrawable(getContentIcon(R.drawable.map_directions));
		((ImageView) mainView.findViewById(R.id.make_round_trip_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_trip_round));
		((ImageView) mainView.findViewById(R.id.door_to_door_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_sort_door_to_door));
		((ImageView) mainView.findViewById(R.id.reverse_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_sort_reverse_order));

		if (!portrait) {
			((ImageView) mainView.findViewById(R.id.select_icon))
					.setImageDrawable(getContentIcon(selectAll ? R.drawable.ic_action_select_all : R.drawable.ic_action_deselect_all));

			((TextView) mainView.findViewById(R.id.select_title))
					.setText(getString(selectAll ? R.string.shared_string_select_all : R.string.shared_string_deselect_all));

			View selectRow = mainView.findViewById(R.id.select_row);
			selectRow.setVisibility(View.VISIBLE);
			selectRow.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					listener.selectOnClick();
					dismiss();
				}
			});
		}
		mainView.findViewById(R.id.navigate_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (listener != null) {
					listener.navigateOnClick();
					dismiss();
				}
			}
		});
		mainView.findViewById(R.id.make_round_trip_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (listener != null) {
					listener.makeRoundTripOnClick();
					dismiss();
				}
			}
		});
		mainView.findViewById(R.id.door_to_door_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (listener != null) {
					listener.doorToDoorOnClick();
					dismiss();
				}
			}
		});
		mainView.findViewById(R.id.reverse_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (listener != null) {
					listener.reverseOrderOnClick();
					dismiss();
				}
			}
		});

		mainView.findViewById(R.id.cancel_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dismiss();
			}
		});

		final int screenHeight = AndroidUtils.getScreenHeight(getActivity());
		final int statusBarHeight = AndroidUtils.getStatusBarHeight(getActivity());
		final int navBarHeight = AndroidUtils.getNavBarHeight(getActivity());

		mainView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				final View scrollView = mainView.findViewById(R.id.sort_by_scroll_view);
				int scrollViewHeight = scrollView.getHeight();
				int dividerHeight = AndroidUtils.dpToPx(getContext(), 1);
				int cancelButtonHeight = getContext().getResources().getDimensionPixelSize(R.dimen.bottom_sheet_cancel_button_height);
				int spaceForScrollView = screenHeight - statusBarHeight - navBarHeight - dividerHeight - cancelButtonHeight;
				if (scrollViewHeight > spaceForScrollView) {
					scrollView.getLayoutParams().height = spaceForScrollView;
					scrollView.requestLayout();
				}

				if (!portrait) {
					if (screenHeight - statusBarHeight - mainView.getHeight() >= AndroidUtils.dpToPx(getActivity(), 8)) {
						AndroidUtils.setBackground(getActivity(), mainView, night,
								R.drawable.bg_bottom_sheet_topsides_landscape_light, R.drawable.bg_bottom_sheet_topsides_landscape_dark);
					} else {
						AndroidUtils.setBackground(getActivity(), mainView, night,
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
	protected Drawable getContentIcon(@DrawableRes int id) {
		return getIcon(id, night ? R.color.ctx_menu_info_text_dark : R.color.on_map_icon_color);
	}

	interface PlanRouteOptionsFragmentListener {

		void selectOnClick();

		void navigateOnClick();

		void makeRoundTripOnClick();

		void doorToDoorOnClick();

		void reverseOrderOnClick();
	}
}
