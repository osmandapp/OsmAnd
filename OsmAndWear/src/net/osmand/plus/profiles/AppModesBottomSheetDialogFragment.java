package net.osmand.plus.profiles;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.settings.fragments.SettingsScreenType;

public abstract class AppModesBottomSheetDialogFragment<T extends AbstractProfileMenuAdapter> extends MenuBottomSheetDialogFragment 
		implements AbstractProfileMenuAdapter.ButtonPressedListener, AbstractProfileMenuAdapter.ProfilePressedListener {

	private UpdateMapRouteMenuListener updateMapRouteMenuListener;
	
	private int themeRes;
	protected T adapter;
	private RecyclerView recyclerView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getData();
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_close;
	}

	@Override
	public void onDismiss(DialogInterface dialog) {
		super.onDismiss(dialog);
		if (updateMapRouteMenuListener != null) {
			updateMapRouteMenuListener.updateAppModeMenu();
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		adapter = getMenuAdapter();
		recyclerView = new RecyclerView(getContext());
		recyclerView = (RecyclerView) View
				.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.recyclerview, null);
		recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		recyclerView.setAdapter(adapter);
		items.add(new TitleItem(getTitle()));
		items.add(new BaseBottomSheetItem.Builder().setCustomView(recyclerView).create());
	}
	
	protected abstract String getTitle();

	protected abstract void getData();
	
	protected abstract T getMenuAdapter();
	
	public void setUpdateMapRouteMenuListener(
		UpdateMapRouteMenuListener updateMapRouteMenuListener) {
		this.updateMapRouteMenuListener = updateMapRouteMenuListener;
	}

	public interface UpdateMapRouteMenuListener {
		void updateAppModeMenu();
	}

	@Override
	public void onResume() {
		super.onResume();
		adapter.setButtonPressedListener(this);
		adapter.setProfilePressedListener(this);
	}

	@Override
	public void onProfilePressed(ApplicationMode item) {
		this.dismiss();
		BaseSettingsFragment.showInstance(getActivity(), SettingsScreenType.CONFIGURE_PROFILE, item);
	}

	@Override
	public void onButtonPressed() {
		this.dismiss();
		BaseSettingsFragment.showInstance(getActivity(), SettingsScreenType.MAIN_SETTINGS);
	}
}
