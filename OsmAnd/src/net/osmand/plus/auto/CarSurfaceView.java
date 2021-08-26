package net.osmand.plus.auto;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;

@SuppressLint("ViewConstructor")
public class CarSurfaceView extends View {

	private final SurfaceRenderer surfaceRenderer;

	public CarSurfaceView(Context context, SurfaceRenderer surfaceRenderer) {
		super(context);
		this.surfaceRenderer = surfaceRenderer;
	}

	public void setWidthHeight(int width, int height) {
		setRight(width);
		setBottom(height);
	}

	public SurfaceRenderer getSurfaceRenderer() {
		return surfaceRenderer;
	}

	@Override
	public boolean isShown() {
		return true;
	}
}
