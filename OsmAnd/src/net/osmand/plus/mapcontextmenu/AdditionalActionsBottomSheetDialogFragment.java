package net.osmand.plus.mapcontextmenu;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v4.content.ContextCompat;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;

public class AdditionalActionsBottomSheetDialogFragment extends BottomSheetDialogFragment {

	public static final String TAG = "AdditionalActionsBottomSheetDialogFragment";

	private boolean nightMode;
	private boolean portrait;
	private ContextMenuAdapter adapter;
	private ContextMenuItemClickListener listener;

	public void setAdapter(ContextMenuAdapter adapter, ContextMenuItemClickListener listener) {
		this.adapter = adapter;
		this.listener = listener;
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

		View mainView = View.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.fragment_context_menu_actions_bottom_sheet_dialog, null);

		AndroidUtils.setBackground(getActivity(), mainView, nightMode,
				portrait ? R.drawable.bg_bottom_menu_light : R.drawable.bg_bottom_sheet_topsides_landscape_light,
				portrait ? R.drawable.bg_bottom_menu_dark : R.drawable.bg_bottom_sheet_topsides_landscape_dark);

		mainView.findViewById(R.id.cancel_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dismiss();
			}
		});

		TextView headerTitle = (TextView) mainView.findViewById(R.id.header_title);
		if (nightMode) {
			headerTitle.setTextColor(ContextCompat.getColor(getActivity(), R.color.ctx_menu_info_text_dark));
		}
		headerTitle.setText(getString(R.string.additional_actions));

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

		View.OnClickListener onClickListener = new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (listener != null) {
					listener.onItemClick((int) view.getTag());
				}
				dismiss();
			}
		};

		LinearLayout itemsLinearLayout = (LinearLayout) mainView.findViewById(R.id.context_menu_items_container);
		for (int i = 0; i < adapter.length(); i++) {
			ContextMenuItem item = adapter.getItem(i);
			int layoutResId = item.getLayout();
			layoutResId = layoutResId != ContextMenuItem.INVALID_ID ? layoutResId : R.layout.bottom_sheet_dialog_fragment_item;
			View row = View.inflate(new ContextThemeWrapper(getContext(), themeRes), layoutResId, null);
			if (layoutResId != R.layout.bottom_sheet_dialog_fragment_divider) {
				if (item.getIcon() != ContextMenuItem.INVALID_ID) {
					((ImageView) row.findViewById(R.id.icon)).setImageDrawable(getContentIcon(item.getIcon()));
				}
				((TextView) row.findViewById(R.id.title)).setText(item.getTitle());
			}
			row.setTag(i);
			row.setOnClickListener(onClickListener);
			itemsLinearLayout.addView(row);
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

	private OsmandApplication getMyApplication() {
		return ((MapActivity) getActivity()).getMyApplication();
	}

	private Drawable getContentIcon(@DrawableRes int id) {
		return getMyApplication().getIconsCache().getIcon(id, nightMode ? R.color.ctx_menu_info_text_dark : R.color.on_map_icon_color);
	}

	public interface ContextMenuItemClickListener {
		void onItemClick(int position);
	}
}
