package net.osmand.plus.measurementtool;

import android.app.Dialog;
import android.graphics.drawable.Drawable;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.plus.R;
import net.osmand.plus.base.BaseBottomSheetDialogFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;

import java.util.ArrayList;
import java.util.List;

public class SnapToRoadBottomSheetDialogFragment extends BaseBottomSheetDialogFragment {

	public static final String TAG = "SnapToRoadBottomSheetDialogFragment";
	public static final int STRAIGHT_LINE_TAG = -1;

	private SnapToRoadFragmentListener listener;
	private boolean nightMode;
	private boolean portrait;
	private boolean snapToRoadEnabled;
	private boolean removeDefaultMode = true;
	private boolean showStraightLine;

	public void setListener(SnapToRoadFragmentListener listener) {
		this.listener = listener;
	}

	public void setRemoveDefaultMode(boolean removeDefaultMode) {
		this.removeDefaultMode = removeDefaultMode;
	}

	public void setShowStraightLine(boolean showStraightLine) {
		this.showStraightLine = showStraightLine;
	}

	@Override
	public void setupDialog(Dialog dialog, int style) {
		super.setupDialog(dialog, style);
		if (settings.DO_NOT_USE_ANIMATIONS.get()) {
			dialog.getWindow().setWindowAnimations(R.style.Animations_NoAnimation);
		}

		nightMode = isNightMode(true);
		portrait = AndroidUiHelper.isOrientationPortrait(getActivity());
		int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;

		View mainView = View.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.fragment_snap_to_road_bottom_sheet_dialog, null);

		AndroidUtils.setBackground(getActivity(), mainView, nightMode,
				portrait ? R.drawable.bg_bottom_menu_light : R.drawable.bg_bottom_sheet_topsides_landscape_light,
				portrait ? R.drawable.bg_bottom_menu_dark : R.drawable.bg_bottom_sheet_topsides_landscape_dark);

		mainView.findViewById(R.id.cancel_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dismiss();
			}
		});

		LinearLayout container = mainView.findViewById(R.id.navigation_types_container);
		List<ApplicationMode> modes = new ArrayList<>(ApplicationMode.values(app));
		if (removeDefaultMode) {
			modes.remove(ApplicationMode.DEFAULT);
		}

		View.OnClickListener onClickListener = new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				snapToRoadEnabled = false;
				if (listener != null) {
					ApplicationMode mode = null;
					if ((int) view.getTag() != STRAIGHT_LINE_TAG) {
						mode = modes.get((int) view.getTag());
						snapToRoadEnabled = true;
					}
					listener.onApplicationModeItemClick(mode);
				}
				dismiss();
			}
		};

		if (showStraightLine) {
			Drawable icon = app.getUIUtilities().getIcon(R.drawable.ic_action_split_interval, nightMode);
			addProfileView(container, onClickListener, STRAIGHT_LINE_TAG, icon,
					app.getText(R.string.routing_profile_straightline));
		}

		for (int i = 0; i < modes.size(); i++) {
			ApplicationMode mode = modes.get(i);
			Drawable icon = app.getUIUtilities().getPaintedIcon(mode.getIconRes(), mode.getProfileColor(nightMode));
			addProfileView(container, onClickListener, i, icon, mode.toHumanString());
		}
		dialog.setContentView(mainView);
		((View) mainView.getParent()).setBackgroundResource(0);
	}

	private void addProfileView(LinearLayout container, View.OnClickListener onClickListener, Object tag, Drawable icon, CharSequence title) {
		View row = UiUtilities.getInflater(getContext(), nightMode).inflate(R.layout.list_item_icon_and_title, null);
		((ImageView) row.findViewById(R.id.icon)).setImageDrawable(icon);
		((TextView) row.findViewById(R.id.title)).setText(title);
		row.setOnClickListener(onClickListener);
		row.setTag(tag);
		container.addView(row);
	}

	@Override
	public void onStart() {
		super.onStart();
		if (!portrait) {
			Window window = getDialog().getWindow();
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

	public interface SnapToRoadFragmentListener {

		void onDestroyView(boolean snapToRoadEnabled);

		void onApplicationModeItemClick(ApplicationMode mode);
	}
}
