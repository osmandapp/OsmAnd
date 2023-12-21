package net.osmand.plus.auto.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;

import net.osmand.plus.auto.SurfaceRenderer;

@SuppressLint("ViewConstructor")
public class CarSurfaceView extends View {

	private static final float CAR_DENSITY_SCALE = 1.325f;

	private final SurfaceRenderer surfaceRenderer;
	private int dpi;
	private float density;

	public CarSurfaceView(Context context, SurfaceRenderer surfaceRenderer) {
		super(context);
		this.surfaceRenderer = surfaceRenderer;
	}

	public void setSurfaceParams(int width, int height, int dpi) {
		setRight(width);
		setBottom(height);
		this.dpi = dpi;
		this.density = dpi / 160f;
	}

	public SurfaceRenderer getSurfaceRenderer() {
		return surfaceRenderer;
	}

	public int getDpi() {
		return dpi;
	}

	public float getDensity() {
		return density * CAR_DENSITY_SCALE;
	}

	@Override
	public boolean isShown() {
		return true;
	}
}
