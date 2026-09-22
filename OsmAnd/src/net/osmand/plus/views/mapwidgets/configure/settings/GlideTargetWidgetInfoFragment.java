package net.osmand.plus.views.mapwidgets.configure.settings;

import android.view.ViewGroup;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.views.mapwidgets.WidgetType;

public class GlideTargetWidgetInfoFragment extends BaseSimpleWidgetInfoFragment {

	@NonNull
	@Override
	public WidgetType getWidget() {
		return WidgetType.GLIDE_TARGET;
	}

	@Override
	protected void setupMainContent(@NonNull ViewGroup container) {
		inflate(R.layout.fragment_widget_settings_glide_target, container);
	}
}
