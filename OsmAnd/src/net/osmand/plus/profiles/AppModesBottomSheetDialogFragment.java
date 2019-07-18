package net.osmand.plus.profiles;

import static net.osmand.plus.profiles.SettingsProfileFragment.IS_USER_PROFILE;
import static net.osmand.plus.profiles.SettingsProfileFragment.PROFILE_STRING_KEY;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ContextThemeWrapper;
import android.view.View;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.profiles.ProfileMenuAdapter.ProfileMenuAdapterListener;

public class AppModesBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	private List<ApplicationMode> allModes = new ArrayList<>();
	private Set<ApplicationMode> selectedModes = new HashSet<>();

	private int themeRes;
	private ProfileMenuAdapter adapter;
	private RecyclerView recyclerView;

	private ProfileMenuAdapterListener listener;
	private UpdateMapRouteMenuListener updateMapRouteMenuListener;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setDismissButtonTextId(R.string.shared_string_close);
		getData();
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
		if (listener == null) {
			listener = new ProfileMenuAdapterListener() {
				@Override
				public void onProfileSelected(ApplicationMode item, boolean selected) {
					if (selected) {
						selectedModes.add(item);
					} else {
						selectedModes.remove(item);
					}
					ApplicationMode.changeProfileAvailability(item, selected, getMyApplication());
				}

				@Override
				public void onProfilePressed(ApplicationMode item) {
					Intent intent = new Intent(getActivity(), EditProfileActivity.class);
					intent.putExtra(PROFILE_STRING_KEY, item.getStringKey());
					if (item.isCustomProfile()) {
						intent.putExtra(IS_USER_PROFILE, true);
					}
					startActivity(intent);
				}

				@Override
				public void onButtonPressed() {
					OsmandApplication app = requiredMyApplication();
					Intent intent = new Intent(app, SettingsProfileActivity.class);
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					app.startActivity(intent);
				}
			};
		}
		adapter.setListener(listener);
		allModes = new ArrayList<>(ApplicationMode.allPossibleValues());
		allModes.remove(ApplicationMode.DEFAULT);
		adapter.updateItemsList(allModes,
			new LinkedHashSet<>(ApplicationMode.values(getMyApplication())));
		setupHeightAndBackground(getView());
	}

	private void getData() {
		allModes.addAll(ApplicationMode.allPossibleValues());
		allModes.remove(ApplicationMode.DEFAULT);
		selectedModes.addAll(ApplicationMode.values(getMyApplication()));
		selectedModes.remove(ApplicationMode.DEFAULT);
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {

		themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		adapter = new ProfileMenuAdapter(allModes, selectedModes, getMyApplication(), getString(R.string.shared_string_manage));
		recyclerView = new RecyclerView(getContext());
		recyclerView = (RecyclerView) View
			.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.recyclerview, null);
		recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		recyclerView.setAdapter(adapter);


		items.add(new TitleItem(getString(R.string.application_profiles)));
		items.add(new BaseBottomSheetItem.Builder().setCustomView(recyclerView).create());
	}

	public void setUpdateMapRouteMenuListener(
		UpdateMapRouteMenuListener updateMapRouteMenuListener) {
		this.updateMapRouteMenuListener = updateMapRouteMenuListener;
	}

	public interface UpdateMapRouteMenuListener {
		void updateAppModeMenu();
	}
}
