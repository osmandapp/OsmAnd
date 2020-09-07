package net.osmand.plus.server;

import android.graphics.Bitmap;
import net.osmand.data.RotatedTileBox;

public interface IMapOnImageDrawn {
	void onDraw(RotatedTileBox viewport, Bitmap bmp);
}
