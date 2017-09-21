package net.osmand.plus.mapmarkers;

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
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.MapMarkersOrderByMode;
import net.osmand.plus.R;
import net.osmand.plus.base.BottomSheetDialogFragment;
import net.osmand.plus.helpers.AndroidUiHelper;

public class OrderByBottomSheetDialogFragment extends BottomSheetDialogFragment {

	public final static String TAG = "OrderByBottomSheetDialogFragment";

	private boolean portrait;
	private OsmandSettings settings;
	private OrderByFragmentListener listener;

	public void setListener(OrderByFragmentListener listener) {
		this.listener = listener;
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		portrait = AndroidUiHelper.isOrientationPortrait(getActivity());
		settings = getMyApplication().getSettings();
		boolean night = !settings.isLightContent();
		final int themeRes = night ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;

		final View mainView = View.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.fragment_marker_order_by_bottom_sheet_dialog, container);
		if (portrait) {
			AndroidUtils.setBackground(getActivity(), mainView, night, R.drawable.bg_bottom_menu_light, R.drawable.bg_bottom_menu_dark);
		}

		if (night) {
			((TextView) mainView.findViewById(R.id.order_by_title)).setTextColor(getResources().getColor(R.color.ctx_menu_info_text_dark));
		}
		((TextView) mainView.findViewById(R.id.order_by_title)).setText(getString(R.string.order_by));

		ImageView distanceIcon = (ImageView) mainView.findViewById(R.id.distance_icon);
		distanceIcon.setImageDrawable(getContentIcon(R.drawable.ic_action_markers_dark));

		ImageView nameIcon = (ImageView) mainView.findViewById(R.id.name_icon);
		nameIcon.setImageDrawable(getContentIcon(R.drawable.ic_action_sort_by_name));

		ImageView dateAddedAscIcon = (ImageView) mainView.findViewById(R.id.date_added_asc_icon);
		dateAddedAscIcon.setImageDrawable(getContentIcon(R.drawable.ic_action_sort_by_date));

		ImageView dateAddedDescIcon = (ImageView) mainView.findViewById(R.id.date_added_desc_icon);
		dateAddedDescIcon.setImageDrawable(getContentIcon(R.drawable.ic_action_sort_by_date));

		((TextView) mainView.findViewById(R.id.date_added_asc_text)).setText(getString(R.string.date_added) + " (" + getString(R.string.ascendingly) + ")");
		((TextView) mainView.findViewById(R.id.date_added_desc_text)).setText(getString(R.string.date_added) + " (" + getString(R.string.descendingly) + ")");

		mainView.findViewById(R.id.distance_row).setOnClickListener(orderByModeOnClickListener);
		mainView.findViewById(R.id.name_row).setOnClickListener(orderByModeOnClickListener);
		mainView.findViewById(R.id.date_added_asc_row).setOnClickListener(orderByModeOnClickListener);
		mainView.findViewById(R.id.date_added_desc_row).setOnClickListener(orderByModeOnClickListener);

		mainView.findViewById(R.id.close_row).setOnClickListener(new View.OnClickListener() {
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
				final View scrollView = mainView.findViewById(R.id.marker_order_by_scroll_view);
				int scrollViewHeight = scrollView.getHeight();
				int dividerHeight = AndroidUtils.dpToPx(getContext(), 1);
				int cancelButtonHeight = getContext().getResources().getDimensionPixelSize(R.dimen.bottom_sheet_cancel_button_height);
				int spaceForScrollView = screenHeight - statusBarHeight - navBarHeight - dividerHeight - cancelButtonHeight;
				if (scrollViewHeight > spaceForScrollView) {
					scrollView.getLayoutParams().height = spaceForScrollView;
					scrollView.requestLayout();
				}

				if (!portrait) {
					if (screenHeight - statusBarHeight - mainView.getHeight()
							>= AndroidUtils.dpToPx(getActivity(), 8)) {
						AndroidUtils.setBackground(getActivity(), mainView, false,
								R.drawable.bg_bottom_sheet_topsides_landscape_light, R.drawable.bg_bottom_sheet_topsides_landscape_dark);
					} else {
						AndroidUtils.setBackground(getActivity(), mainView, false,
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
		return getIcon(id, settings.isLightContent() ? R.color.on_map_icon_color : R.color.ctx_menu_info_text_dark);
	}

	private View.OnClickListener orderByModeOnClickListener = new View.OnClickListener() {

		@Override
		public void onClick(View view) {
			MapMarkersOrderByMode currentOrderByMode = settings.MAP_MARKERS_ORDER_BY_MODE.get();
			MapMarkersOrderByMode modeToSet;
			switch (view.getId()) {
				case R.id.distance_row:
					if (currentOrderByMode == MapMarkersOrderByMode.DISTANCE_ASC) {
						modeToSet = MapMarkersOrderByMode.DISTANCE_DESC;
					} else {
						modeToSet = MapMarkersOrderByMode.DISTANCE_ASC;
					}
					break;
				case R.id.name_row:
					modeToSet = MapMarkersOrderByMode.NAME;
					break;
				case R.id.date_added_asc_row:
					modeToSet = MapMarkersOrderByMode.DATE_ADDED_ASC;
					break;
				case R.id.date_added_desc_row:
					modeToSet = MapMarkersOrderByMode.DATE_ADDED_DESC;
					break;
				default:
					modeToSet = currentOrderByMode;
			}
			settings.MAP_MARKERS_ORDER_BY_MODE.set(modeToSet);
			if (listener != null) {
				listener.onMapMarkersOrderByModeChanged(modeToSet);
			}
			dismiss();
		}
	};

	interface OrderByFragmentListener {
		void onMapMarkersOrderByModeChanged(MapMarkersOrderByMode orderByMode);
	}
}
