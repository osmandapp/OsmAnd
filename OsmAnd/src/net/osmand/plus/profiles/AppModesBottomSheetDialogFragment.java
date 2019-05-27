package net.osmand.plus.profiles;

import static net.osmand.plus.profiles.SettingsProfileFragment.IS_USER_PROFILE;
import static net.osmand.plus.profiles.SettingsProfileFragment.PROFILE_STRING_KEY;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.profiles.ProfileMenuAdapter.ProfileListener;
import net.osmand.util.Algorithms;

public class AppModesBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	private List<ApplicationMode> allModes = new ArrayList<>();
	private Set<ApplicationMode> selectedModes = new HashSet<>();

	protected boolean nightMode;

	private ProfileMenuAdapter adapter;
	private RecyclerView recyclerView;

	private ProfileListener listener;
	private UpdateMapRouteMenuListener updateMapRouteMenuListener;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		allModes.addAll(ApplicationMode.allPossibleValues());
		allModes.remove(ApplicationMode.DEFAULT);
		selectedModes.addAll(ApplicationMode.values(getMyApplication()));
		selectedModes.remove(ApplicationMode.DEFAULT);
	}

	@Override
	public void onDismiss(DialogInterface dialog) {
		super.onDismiss(dialog);
		if (updateMapRouteMenuListener != null) {
			updateMapRouteMenuListener.updateAppModeMenu();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		listener = new ProfileListener() {
			@Override
			public void changeProfileStatus(ApplicationMode item, boolean isSelected) {
				if(isSelected) {
					selectedModes.add(item);
				} else {
					selectedModes.remove(item);
				}
				ApplicationMode.changeProfileStatus(item, isSelected, getMyApplication());
			}

			@Override
			public void editProfile(ApplicationMode item) {
				Intent intent = new Intent(getActivity(), EditProfileActivity.class);
				intent.putExtra(PROFILE_STRING_KEY, item.getStringKey());
				if (!Algorithms.isEmpty(item.getUserProfileName())) {
					intent.putExtra(IS_USER_PROFILE, true);
				}
				startActivity(intent);
			}
		};

		adapter.setListener(listener);
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {

		adapter = new ProfileMenuAdapter(allModes, selectedModes, getMyApplication(), listener);

		recyclerView = new RecyclerView(getContext());
		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		recyclerView = (RecyclerView) View
			.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.recyclerview, null);
		recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		recyclerView.setAdapter(adapter);

		final View textButtonView = View.inflate(new ContextThemeWrapper(getContext(), themeRes),
			R.layout.bottom_sheet_item_simple, null);
		TextView textView = (TextView) textButtonView.findViewById(R.id.title);

		int dpPadding = (int) (8 * getResources().getDisplayMetrics().density + 0.5f);
		textView.setPadding(dpPadding, 0,0,0);
		textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16.0f);
		textView.setTextColor(nightMode
			? getResources().getColor(R.color.active_buttons_and_links_dark)
			: getResources().getColor(R.color.active_buttons_and_links_light));
		textView.setText(R.string.shared_string_manage);

		items.add(new TitleItem(getString(R.string.application_profiles)));
		items.add(new BaseBottomSheetItem.Builder().setCustomView(recyclerView).create());
		items.add(new BaseBottomSheetItem.Builder().setCustomView(textButtonView)
			.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					startActivity(new Intent(getContext(), SettingsProfileActivity.class));
				}
			})
			.create());
	}

	public void setUpdateMapRouteMenuListener(
		UpdateMapRouteMenuListener updateMapRouteMenuListener) {
		this.updateMapRouteMenuListener = updateMapRouteMenuListener;
	}

	public interface UpdateMapRouteMenuListener{
		void updateAppModeMenu();
	}
}
