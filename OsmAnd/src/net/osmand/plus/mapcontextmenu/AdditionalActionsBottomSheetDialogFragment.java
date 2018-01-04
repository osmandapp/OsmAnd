package net.osmand.plus.mapcontextmenu;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.widgets.tools.ExtendedBottomSheetBehavior;
import net.osmand.plus.widgets.tools.ExtendedBottomSheetBehavior.BottomSheetCallback;

public class AdditionalActionsBottomSheetDialogFragment extends net.osmand.plus.base.BottomSheetDialogFragment {

	public static final String TAG = "AdditionalActionsBottomSheetDialogFragment";

	private boolean nightMode;
	private boolean portrait;
	private ContextMenuAdapter adapter;
	private ContextMenuItemClickListener listener;

	public void setAdapter(ContextMenuAdapter adapter, ContextMenuItemClickListener listener) {
		this.adapter = adapter;
		this.listener = listener;
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		nightMode = getMyApplication().getDaynightHelper().isNightModeForMapControls();
		portrait = AndroidUiHelper.isOrientationPortrait(getActivity());
		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;

		View mainView = View.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.fragment_context_menu_actions_bottom_sheet_dialog, null);

//		AndroidUtils.setBackground(getActivity(), mainView, nightMode,
//				portrait ? R.drawable.bg_bottom_menu_light : R.drawable.bg_bottom_sheet_topsides_landscape_light,
//				portrait ? R.drawable.bg_bottom_menu_dark : R.drawable.bg_bottom_sheet_topsides_landscape_dark);

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
		headerTitle.setText(R.string.additional_actions);

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

		ExtendedBottomSheetBehavior.from(mainView.findViewById(R.id.bottom_sheet_scroll_view)).setBottomSheetCallback(new BottomSheetCallback() {
			@Override
			public void onStateChanged(@NonNull View bottomSheet, int newState) {
				if (newState == ExtendedBottomSheetBehavior.STATE_HIDDEN) {
					dismiss();
				}
			}

			@Override
			public void onSlide(@NonNull View bottomSheet, float slideOffset) {

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
		return getMyApplication().getIconsCache().getIcon(id, nightMode ? R.color.ctx_menu_info_text_dark : R.color.on_map_icon_color);
	}

	public interface ContextMenuItemClickListener {
		void onItemClick(int position);
	}
}
