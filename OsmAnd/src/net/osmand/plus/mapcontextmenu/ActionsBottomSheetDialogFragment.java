package net.osmand.plus.mapcontextmenu;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;

public class ActionsBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = "ActionsBottomSheetDialogFragment";

	private ArrayAdapter<ContextMenuItem> adapter;
	private AdapterView.OnItemClickListener listener;

	public void setAdapter(ArrayAdapter<ContextMenuItem> adapter, AdapterView.OnItemClickListener listener) {
		this.adapter = adapter;
		this.listener = listener;
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;

		final View mainView = View.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.fragment_context_menu_actions_bottom_sheet_dialog, container);

		final ListView listView = (ListView) mainView.findViewById(R.id.list_view);
		View headerView = inflater.inflate(R.layout.bottom_sheet_dialog_fragment_title, listView, false);
		TextView headerTitle = (TextView) headerView.findViewById(R.id.header_title);
		if (nightMode) {
			headerTitle.setTextColor(ContextCompat.getColor(getActivity(), R.color.ctx_menu_info_text_dark));
		}
		headerTitle.setText(getString(R.string.additional_actions));
		listView.addHeaderView(headerView);
		listView.setDivider(null);
		if (adapter != null) {
			listView.setAdapter(adapter);
		}
		if (listener != null) {
			listView.setOnItemClickListener(listener);
		}

		mainView.findViewById(R.id.cancel_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dismiss();
			}
		});

		setupHeightAndBackground(mainView, R.id.list_view);

		return mainView;
	}
}
