package net.osmand.plus.measurementtool;

import android.view.View;

import androidx.annotation.NonNull;

import net.osmand.plus.mapcontextmenu.other.TrackDetailsMenu;

import java.util.function.BooleanSupplier;

class GraphDetailsMenu extends TrackDetailsMenu {

	private final View view;
	private final BooleanSupplier mapSyncState;

	public GraphDetailsMenu(@NonNull View view, @NonNull BooleanSupplier mapSyncState) {
		this.view = view;
		this.mapSyncState = mapSyncState;
	}

	@Override
	protected boolean isMapSyncActive() {
		return mapSyncState.getAsBoolean();
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
