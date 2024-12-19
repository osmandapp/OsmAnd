package net.osmand.plus.widgets;

import android.content.Context;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.view.SupportMenuInflater;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.appcompat.view.menu.MenuPresenter;
import androidx.appcompat.view.menu.SubMenuBuilder;

/**
 * Copied from android.support.v7.widget.PopupMenu.
 * "mPopup.setForceShowIcon(true);" in the constructor does the trick :)
 *
 * @author maikvlcek
 * @since 5:00 PM - 1/27/14
 */
public class IconPopupMenu implements MenuBuilder.Callback, MenuPresenter.Callback {
	private final Context mContext;
	private final MenuBuilder mMenu;
	private final View mAnchor;
	private final MenuPopupHelper mPopup;
	private OnMenuItemClickListener mMenuItemClickListener;
	private OnDismissListener mDismissListener;

	/**
	 * Callback interface used to notify the application that the menu has closed.
	 */
	public interface OnDismissListener {
		/**
		 * Called when the associated menu has been dismissed.
		 *
		 * @param menu The PopupMenu that was dismissed.
		 */
		void onDismiss(IconPopupMenu menu);
	}

	/**
	 * Construct a new PopupMenu.
	 *
	 * @param context Context for the PopupMenu.
	 * @param anchor  Anchor view for this popup. The popup will appear below the anchor if there
	 *                is room, or above it if there is not.
	 */
	public IconPopupMenu(Context context, View anchor) {
		mContext = context;
		mMenu = new MenuBuilder(context);
		mMenu.setCallback(this);
		mAnchor = anchor;
		mPopup = new MenuPopupHelper(context, mMenu, anchor);
		mPopup.setPresenterCallback(this);
		mPopup.setForceShowIcon(true);
	}

	/**
	 * @return the {@link android.view.Menu} associated with this popup. Populate the returned Menu with
	 * items before calling {@link #show()}.
	 * @see #show()
	 * @see #getMenuInflater()
	 */
	public Menu getMenu() {
		return mMenu;
	}

	/**
	 * @return a {@link android.view.MenuInflater} that can be used to inflate menu items from XML into the
	 * menu returned by {@link #getMenu()}.
	 * @see #getMenu()
	 */
	public MenuInflater getMenuInflater() {
		return new SupportMenuInflater(mContext);
	}

	/**
	 * Inflate a menu resource into this PopupMenu. This is equivalent to calling
	 * popupMenu.getMenuInflater().inflate(menuRes, popupMenu.getMenu()).
	 *
	 * @param menuRes Menu resource to inflate
	 */
	public void inflate(int menuRes) {
		getMenuInflater().inflate(menuRes, mMenu);
	}

	/**
	 * Show the menu popup anchored to the view specified during construction.
	 *
	 * @see #dismiss()
	 */
	public void show() {
		mPopup.show();
	}

	/**
	 * Dismiss the menu popup.
	 *
	 * @see #show()
	 */
	public void dismiss() {
		mPopup.dismiss();
	}

	/**
	 * Set a listener that will be notified when the user selects an item from the menu.
	 *
	 * @param listener Listener to notify
	 */
	public void setOnMenuItemClickListener(OnMenuItemClickListener listener) {
		mMenuItemClickListener = listener;
	}

	/**
	 * Set a listener that will be notified when this menu is dismissed.
	 *
	 * @param listener Listener to notify
	 */
	public void setOnDismissListener(OnDismissListener listener) {
		mDismissListener = listener;
	}

	public boolean onMenuItemSelected(MenuBuilder menu, MenuItem item) {
		return mMenuItemClickListener != null && mMenuItemClickListener.onMenuItemClick(item);
	}

	public void onCloseMenu(MenuBuilder menu, boolean allMenusAreClosing) {
		if (mDismissListener != null) {
			mDismissListener.onDismiss(this);
		}
	}

	public boolean onOpenSubMenu(MenuBuilder subMenu) {
		if (subMenu == null) return false;

		if (!subMenu.hasVisibleItems()) {
			return true;
		}

		// Current menu will be dismissed by the normal helper, submenu will be shown in its place.
		MenuPopupHelper menuPopupHelper = new MenuPopupHelper(mContext, subMenu, mAnchor);
		menuPopupHelper.show();
		return true;
	}

	public void onCloseSubMenu(SubMenuBuilder menu) {
	}

	public void onMenuModeChange(MenuBuilder menu) {
	}

	/**
	 * Interface responsible for receiving menu item click events if the items themselves
	 * do not have individual item click listeners.
	 */
	public interface OnMenuItemClickListener {
		/**
		 * This method will be invoked when a menu item is clicked if the item itself did
		 * not already handle the event.
		 *
		 * @param item {@link MenuItem} that was clicked
		 * @return <code>true</code> if the event was handled, <code>false</code> otherwise.
		 */
		boolean onMenuItemClick(MenuItem item);
	}

}