package net.osmand.plus.measurementtool;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialog;
import android.support.v4.content.ContextCompat;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;

import java.util.ArrayList;
import java.util.List;

public class SnapToRoadBottomSheetDialogFragment extends android.support.design.widget.BottomSheetDialogFragment {

	public static final String TAG = "SnapToRoadBottomSheetDialogFragment";

	private SnapToRoadFragmentListener listener;
	private boolean nightMode;
	private boolean portrait;
	private boolean snapToRoadEnabled;
	private boolean removeDefaultMode = true;

	public void setListener(SnapToRoadFragmentListener listener) {
		this.listener = listener;
	}

	public void setRemoveDefaultMode(boolean removeDefaultMode) {
		this.removeDefaultMode = removeDefaultMode;
	}

	@Override
	public void setupDialog(Dialog dialog, int style) {
		super.setupDialog(dialog, style);
		if (getMyApplication().getSettings().DO_NOT_USE_ANIMATIONS.get()) {
			dialog.getWindow().setWindowAnimations(R.style.Animations_NoAnimation);
		}

		nightMode = getMyApplication().getDaynightHelper().isNightModeForMapControls();
		portrait = AndroidUiHelper.isOrientationPortrait(getActivity());
		final OsmandSettings settings = getMyApplication().getSettings();
		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;

		final View mainView = View.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.fragment_snap_to_road_bottom_sheet_dialog, null);

		AndroidUtils.setBackground(getActivity(), mainView, nightMode,
				portrait ? R.drawable.bg_bottom_menu_light : R.drawable.bg_bottom_sheet_topsides_landscape_light,
				portrait ? R.drawable.bg_bottom_menu_dark : R.drawable.bg_bottom_sheet_topsides_landscape_dark);

		mainView.findViewById(R.id.cancel_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dismiss();
			}
		});

		if (nightMode) {
			((TextView) mainView.findViewById(R.id.choose_navigation_title))
					.setTextColor(ContextCompat.getColor(getActivity(), R.color.ctx_menu_info_text_dark));
		}

		LinearLayout container = (LinearLayout) mainView.findViewById(R.id.navigation_types_container);
		final List<ApplicationMode> modes = new ArrayList<>(ApplicationMode.values(settings));
		if (removeDefaultMode) {
			modes.remove(ApplicationMode.DEFAULT);
		}

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
			container.addView(row);
		}

		if (!portrait) {
			dialog.setOnShowListener(new DialogInterface.OnShowListener() {
				@Override
				public void onShow(DialogInterface dialogInterface) {
					BottomSheetDialog dialog = (BottomSheetDialog) dialogInterface;
					FrameLayout bottomSheet = (FrameLayout) dialog.findViewById(android.support.design.R.id.design_bottom_sheet);
					BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED);
				}
			});
		}

		dialog.setContentView(mainView);
		((View) mainView.getParent()).setBackgroundResource(0);
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

	private OsmandApplication getMyApplication() {
		return ((MapActivity) getActivity()).getMyApplication();
	}

	private Drawable getContentIcon(@DrawableRes int id) {
		return getMyApplication().getIconsCache()
				.getIcon(id, nightMode ? R.color.ctx_menu_info_text_dark : R.color.on_map_icon_color);
	}

	public interface SnapToRoadFragmentListener {

		void onDestroyView(boolean snapToRoadEnabled);

		void onApplicationModeItemClick(ApplicationMode mode);
	}
}
