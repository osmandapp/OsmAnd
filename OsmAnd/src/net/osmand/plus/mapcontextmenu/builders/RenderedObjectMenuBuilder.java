package net.osmand.plus.mapcontextmenu.builders;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.NativeLibrary.RenderedObject;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuBuilder;

public class RenderedObjectMenuBuilder extends MenuBuilder {

	private final RenderedObject renderedObject;

	public RenderedObjectMenuBuilder(@NonNull MapActivity mapActivity, @NonNull RenderedObject renderedObject) {
		super(mapActivity);
		this.renderedObject = renderedObject;
	}

	@Nullable
	@Override
	public Object getObject() {
		return renderedObject;
	}
}
