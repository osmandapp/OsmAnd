package net.osmand.plus.keyevent.commands;

import android.view.KeyEvent;

import androidx.annotation.NonNull;

import net.osmand.plus.helpers.MapScrollHelper;
import net.osmand.plus.helpers.MapScrollHelper.ScrollDirection;

public class MapScrollCommand extends KeyEventCommand {

	public static final String SCROLL_UP_ID = "map_scroll_up";
	public static final String SCROLL_DOWN_ID = "map_scroll_down";
	public static final String SCROLL_LEFT_ID = "map_scroll_left";
	public static final String SCROLL_RIGHT_ID = "map_scroll_right";

	private final ScrollDirection direction;

	public MapScrollCommand(@NonNull ScrollDirection direction) {
		this.direction = direction;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		MapScrollHelper scrollHelper = requireMapActivity().getMapScrollHelper();
		scrollHelper.startScrolling(direction);
		return true;
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		MapScrollHelper scrollHelper = requireMapActivity().getMapScrollHelper();
		scrollHelper.removeDirection(direction);
		return true;
	}

}
