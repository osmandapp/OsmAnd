package net.osmand.plus.download.local.dialogs.livegroup;

import static net.osmand.plus.download.local.dialogs.livegroup.LiveGroupItemsFragment.*;

import android.graphics.drawable.Drawable;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.view.MenuCompat;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.local.dialogs.LocalBaseFragment;
import net.osmand.plus.utils.UiUtilities;

public class LiveGroupMenuProvider implements MenuProvider {

	private final UiUtilities uiUtilities;
	private final LocalBaseFragment fragment;

	private LiveGroupItem liveGroupItem;

	@ColorRes
	private int colorId;
	private boolean showInfoItem = true;

	public LiveGroupMenuProvider(@NonNull DownloadActivity activity, @NonNull LocalBaseFragment fragment) {
		this.fragment = fragment;
		uiUtilities = activity.getMyApplication().getUIUtilities();
	}

	public void setColorId(@ColorRes int colorId) {
		this.colorId = colorId;
	}

	public void setLiveGroupItem(@NonNull LiveGroupItem liveGroupItem) {
		this.liveGroupItem = liveGroupItem;
	}

	public void setShowInfoItem(boolean showInfoItem) {
		this.showInfoItem = showInfoItem;
	}

	public void showMenu(@NonNull View view) {
		PopupMenu popupMenu = new PopupMenu(view.getContext(), view);
		Menu menu = popupMenu.getMenu();
		if (menu instanceof MenuBuilder) {
			((MenuBuilder) menu).setOptionalIconsVisible(true);
		}
		MenuCompat.setGroupDividerEnabled(menu, true);

		onCreateMenu(menu, new MenuInflater(view.getContext()));
		popupMenu.show();
	}

	@Override
	public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
		MenuItem menuItem;
		if (showInfoItem) {
			menuItem = menu.add(0, 0, Menu.NONE, R.string.info_button);
			menuItem.setIcon(getIcon(R.drawable.ic_action_info_outlined, colorId));
			menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			menuItem.setOnMenuItemClickListener(item -> {
				FragmentManager manager = fragment.getFragmentManager();
				if (manager != null) {
					LiveGroupItemFragment.showInstance(manager, liveGroupItem, fragment);
				}
				return true;
			});
		}
	}

	@Override
	public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
		return false;
	}

	@Nullable
	private Drawable getIcon(@DrawableRes int id, @ColorRes int colorId) {
		return uiUtilities.getIcon(id, colorId);
	}
}