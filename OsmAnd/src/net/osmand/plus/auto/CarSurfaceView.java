package net.osmand.plus.auto;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;

@SuppressLint("ViewConstructor")
public class CarSurfaceView extends View {

	public static final float TEXT_SCALE_DIVIDER_160 = 0.5f;
	public static final float MAP_DENSITY_DIVIDER_160 = 0.5f;

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
		return density;
	}

	@Override
	public boolean isShown() {
		return true;
	}
}
