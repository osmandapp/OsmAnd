package net.osmand.plus.profiles;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ContextThemeWrapper;
import android.view.View;

import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;

import static net.osmand.plus.profiles.SettingsProfileFragment.IS_USER_PROFILE;
import static net.osmand.plus.profiles.SettingsProfileFragment.PROFILE_STRING_KEY;

public abstract class AppModesBottomSheetDialogFragment<T extends AbstractProfileMenuAdapter> extends MenuBottomSheetDialogFragment {

	private UpdateMapRouteMenuListener updateMapRouteMenuListener;
	protected AbstractProfileMenuAdapter.ButtonPressedListener buttonPressedListener;
	protected AbstractProfileMenuAdapter.ProfilePressedListener profilePressedListener;
	
	private int themeRes;
	protected T adapter;
	private RecyclerView recyclerView;

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
		adapter.setButtonPressedListener(getButtonPressedListener());
		adapter.setProfilePressedListener(getProfilePressedListener());
	}

	public AbstractProfileMenuAdapter.ButtonPressedListener getButtonPressedListener() {
		if (buttonPressedListener == null) {
			buttonPressedListener = new AbstractProfileMenuAdapter.ButtonPressedListener() {
				@Override
				public void onButtonPressed() {
					OsmandApplication app = requiredMyApplication();
					Intent intent = new Intent(app, SettingsProfileActivity.class);
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					app.startActivity(intent);
				}
			};
		}
		return buttonPressedListener;
	}

	public AbstractProfileMenuAdapter.ProfilePressedListener getProfilePressedListener() {
		if (profilePressedListener == null) {
			profilePressedListener = new AbstractProfileMenuAdapter.ProfilePressedListener() {
				@Override
				public void onProfilePressed(ApplicationMode item) {
					Intent intent = new Intent(getActivity(), EditProfileActivity.class);
					intent.putExtra(PROFILE_STRING_KEY, item.getStringKey());
					if (item.isCustomProfile()) {
						intent.putExtra(IS_USER_PROFILE, true);
					}
					startActivity(intent);
				}
			};
		}
		return profilePressedListener;
	}
}
