package net.osmand.plus.plugins.topoprint.widgets;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.topoprint.TopoPrintPlugin;
import net.osmand.plus.views.controls.maphudbuttons.MapButton;
import net.osmand.plus.views.mapwidgets.configure.buttons.MapButtonState;

/**
 * Map-hud button that opens the print map generation dialog.
 */
public class PrintMapButton extends MapButton {

	private final TopoPrintPlugin plugin;
	@Nullable
	private final PrintMapButtonState buttonState;

	public PrintMapButton(@NonNull Context context) {
		this(context, null);
	}

	public PrintMapButton(@NonNull Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public PrintMapButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		plugin = PluginsHelper.getPlugin(TopoPrintPlugin.class);
		buttonState = plugin != null ? plugin.getPrintMapButtonState() : null;

		setOnClickListener(v -> {
			if (mapActivity != null && plugin != null) {
				plugin.showGenerateDialog(mapActivity);
			}
		});
	}

	@Nullable
	@Override
	public MapButtonState getButtonState() {
		return buttonState;
	}

	@NonNull
	@Override
	public String getButtonId() {
		return "print_map";
	}

	@Override
	protected boolean shouldShow() {
		return plugin != null && plugin.isActive() && visibilityHelper.shouldShowTopButtons();
	}
}
