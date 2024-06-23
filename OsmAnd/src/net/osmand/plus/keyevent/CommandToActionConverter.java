package net.osmand.plus.keyevent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.quickaction.actions.ContinuousMapZoomInAction;
import net.osmand.plus.quickaction.actions.ContinuousMapZoomOutAction;
import net.osmand.plus.quickaction.actions.MapScrollDownAction;
import net.osmand.plus.quickaction.actions.MapScrollLeftAction;
import net.osmand.plus.quickaction.actions.MapScrollRightAction;
import net.osmand.plus.quickaction.actions.MapScrollUpAction;
import net.osmand.plus.quickaction.actions.MapZoomInAction;
import net.osmand.plus.quickaction.actions.MapZoomOutAction;
import net.osmand.plus.quickaction.actions.MoveToMyLocationAction;
import net.osmand.plus.quickaction.actions.NextAppProfileAction;
import net.osmand.plus.quickaction.actions.PreviousAppProfileAction;

import java.util.HashMap;
import java.util.Map;

public class CommandToActionConverter {

	private static final Map<String, QuickActionType> map = new HashMap<>();

	static {
		map.put("back_to_location", MoveToMyLocationAction.TYPE);
//		map.put("switch_compass_forward", null);
//		map.put("open_navigation_dialog", null);
//		map.put("open_quick_search_dialog", null);
		map.put("switch_app_mode_forward", NextAppProfileAction.TYPE);
		map.put("switch_app_mode_backward", PreviousAppProfileAction.TYPE);

		map.put("map_scroll_up", MapScrollUpAction.TYPE);
		map.put("map_scroll_down", MapScrollDownAction.TYPE);
		map.put("map_scroll_left", MapScrollLeftAction.TYPE);
		map.put("map_scroll_right", MapScrollRightAction.TYPE);
		map.put("zoom_in", MapZoomInAction.TYPE);
		map.put("zoom_out", MapZoomOutAction.TYPE);
		map.put("continuous_zoom_in", ContinuousMapZoomInAction.TYPE);
		map.put("continuous_zoom_out", ContinuousMapZoomOutAction.TYPE);

//		map.put("emit_navigation_hint", null);
//		map.put("toggle_drawer", null);
//		map.put("activity_back_pressed", null);
//		map.put("take_media_note", null);
//		map.put("open_wunderlinq_datagrid", null);
	}

	@Nullable
	public static QuickAction createQuickAction(@NonNull String commandId) {
		QuickActionType type = map.get(commandId);
		return type != null ? type.createNew() : null;
	}

}
