package net.osmand.plus.audionotes;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;

import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;

public class SortBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = "SortBottomSheetDialogFragment";

	private SortFragmentListener listener;

	public void setListener(SortFragmentListener listener) {
		this.listener = listener;
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;

		final View mainView = View.inflate(new ContextThemeWrapper(getContext(), themeRes),
				R.layout.fragment_notes_sort_bottom_sheet_dialog, null);

		((ImageView) mainView.findViewById(R.id.by_type_icon)).setImageDrawable(getContentIcon(R.drawable.ic_groped_by_type));
		((ImageView) mainView.findViewById(R.id.by_date_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_sort_by_date));

		mainView.findViewById(R.id.by_type_row).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				notifyListenerAndDismiss();
			}
		});
		mainView.findViewById(R.id.by_date_row).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				notifyListenerAndDismiss();
			}
		});
		mainView.findViewById(R.id.close_row).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				dismiss();
			}
		});

		setupHeightAndBackground(mainView, R.id.scroll_view);

		return mainView;
	}

	private void notifyListenerAndDismiss() {
		if (listener != null) {
			listener.onSortModeChanged();
		}
		dismiss();
	}

	interface SortFragmentListener {
		void onSortModeChanged();
	}
}
