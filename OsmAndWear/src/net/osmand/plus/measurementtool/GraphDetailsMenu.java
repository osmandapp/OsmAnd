package net.osmand.plus.measurementtool;

import android.view.View;

import androidx.annotation.NonNull;

import net.osmand.plus.mapcontextmenu.other.TrackDetailsMenu;

class GraphDetailsMenu extends TrackDetailsMenu {

	private final View view;

	public GraphDetailsMenu(@NonNull View view) {
		this.view = view;
	}

	@Override
	protected int getFragmentWidth() {
		return view.getWidth();
	}

	@Override
	protected int getFragmentHeight() {
		return view.getHeight();
	}

	public boolean shouldShowXAxisPoints() {
		return false;
	}
}
