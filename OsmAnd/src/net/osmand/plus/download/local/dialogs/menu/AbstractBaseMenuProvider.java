package net.osmand.plus.download.local.dialogs.menu;

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

import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.local.LocalItem;
import net.osmand.plus.download.local.LocalOperationTask;
import net.osmand.plus.download.local.OperationType;
import net.osmand.plus.download.local.dialogs.LocalBaseFragment;
import net.osmand.plus.utils.UiUtilities;

public abstract class AbstractBaseMenuProvider implements MenuProvider {

	protected final OsmandApplication app;
	protected final DownloadActivity activity;
	protected final BaseOsmAndFragment fragment;
	protected final UiUtilities iconsCache;
	protected final boolean nightMode;
	@ColorRes
	protected int iconColorId;

	public AbstractBaseMenuProvider(@NonNull DownloadActivity activity,
	                                @NonNull BaseOsmAndFragment fragment) {
		this.activity = activity;
		this.fragment = fragment;
		app = activity.getApp();
		nightMode = fragment.isNightMode();
		iconsCache = app.getUIUtilities();
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

	public void setIconColorId(@ColorRes int iconColorId) {
		this.iconColorId = iconColorId;
	}

	protected void addOperationItem(@NonNull Menu menu, @NonNull OperationType type,
	                                @NonNull LocalItem... localItem) {
		MenuItem menuItem = menu.add(0, type.getTitleId(), Menu.NONE, type.getTitleId());
		menuItem.setIcon(getIcon(type.getIconId(), iconColorId));
		menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		menuItem.setOnMenuItemClickListener(item -> {
			performOperation(type, localItem);
			return true;
		});
	}

	public void performOperation(@NonNull OperationType type, @NonNull LocalItem... localItems) {
		if (fragment instanceof LocalBaseFragment localFragment) {
			OsmAndTaskManager.executeTask(new LocalOperationTask(app, type, localFragment), localItems);
		}
	}

	@Nullable
	protected Drawable getIcon(@DrawableRes int id, @ColorRes int colorId) {
		return iconsCache.getIcon(id, colorId);
	}
}
