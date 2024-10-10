package net.osmand.plus.mapcontextmenu.gallery.imageview;

import android.widget.ImageView;

import androidx.annotation.NonNull;

public class ZoomParams {
	public float scale;
	public float focusX;
	public float focusY;
	public ImageView.ScaleType scaleType;

	public ZoomParams(float scale, float focusX, float focusY, @NonNull ImageView.ScaleType scaleType) {
		this.scale = scale;
		this.focusX = focusX;
		this.focusY = focusY;
		this.scaleType = scaleType;
	}
}
