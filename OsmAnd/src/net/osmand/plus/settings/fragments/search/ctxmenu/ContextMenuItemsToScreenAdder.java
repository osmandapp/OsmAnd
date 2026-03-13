package net.osmand.plus.settings.fragments.search.ctxmenu;

import android.content.Context;

import androidx.preference.PreferenceScreen;

import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;

import java.util.List;

public class ContextMenuItemsToScreenAdder {

	public static void addContextMenuItemsToScreen(final List<ContextMenuItem> contextMenuItems,
												   final PreferenceScreen screen,
												   final Context context) {
		new PreferencesToScreenAdder(screen).addPreferencesToScreen(
				new ItemToPreferenceConverter(context).asPreferences(
						ContextMenuItemToItemConverter.asItems(contextMenuItems)));
	}
}
