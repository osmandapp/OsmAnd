package net.osmand.plus.views.layers.geometry;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Paint;

import net.osmand.AndroidUtils;
import net.osmand.plus.R;

import androidx.core.content.ContextCompat;

public class GpxGeometryWayContext extends GeometryWayContext {

	private final Bitmap specialArrowBitmap;

	public GpxGeometryWayContext(Context ctx, float density) {
		super(ctx, density);
		Paint paint = getPaintIcon();
		paint.setStrokeCap(Paint.Cap.ROUND);
		specialArrowBitmap = AndroidUtils.drawableToBitmap(ContextCompat.getDrawable(ctx, R.drawable.mm_special_arrow_up));
	}

	@Override
	protected int getArrowBitmapResId() {
		return R.drawable.ic_action_direction_arrow;
	}

	public Bitmap getSpecialArrowBitmap() {
		return specialArrowBitmap;
	}
}
