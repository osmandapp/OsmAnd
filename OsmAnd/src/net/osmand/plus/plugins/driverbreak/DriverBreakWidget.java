/*
 * OsmAnd, OsmAnd BV <info@osmand.net>, 2010–present
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.osmand.plus.plugins.driverbreak;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.enums.WidgetSize;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.widgets.SimpleWidget;

/**
 * On-map widget showing distance or time to the next recommended break.
 */
public class DriverBreakWidget extends SimpleWidget {

	public static final String WIDGET_ID = "driver_break_next_stop";

	private final DriverBreakPlugin plugin;

	public DriverBreakWidget(@NonNull MapActivity mapActivity, @Nullable String customId,
			@Nullable WidgetsPanel panel, @NonNull DriverBreakPlugin plugin) {
		super(mapActivity, WidgetType.DRIVER_BREAK_NEXT_STOP, customId, panel);
		this.plugin = plugin;
	}

	@Override
	protected void updateSimpleWidgetInfo(@Nullable DrawSettings drawSettings) {
		BreakStatus status = plugin.getBreakStatus();
		if (status == null) {
			setText(null, null);
			return;
		}
		TravelMode mode = status.getMode();
		String text;
		if (mode == TravelMode.HIKING || mode == TravelMode.CYCLING) {
			double remainM = status.getNextBreakDueMs();
			text = OsmAndFormatter.getFormattedDistance((float) Math.max(0, remainM), app);
		} else {
			long remainMs = status.getNextBreakDueMs();
			text = OsmAndFormatter.getFormattedDuration(remainMs / 1000, app);
		}
		setText(text, null);
	}

	@NonNull
	@Override
	protected WidgetSize getDefaultWidgetSize() {
		return WidgetSize.SMALL;
	}
}
