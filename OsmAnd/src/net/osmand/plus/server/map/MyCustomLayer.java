package net.osmand.plus.server.map;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import net.osmand.data.RotatedTileBox;

public class MyCustomLayer extends OsmandMapMiniLayer{

    protected Paint paintBitmap;


    @Override
    public void initLayer(OsmandMapTileMiniView view) {

        paintBitmap = new Paint();
        paintBitmap.setFilterBitmap(true);
        paintBitmap.setColor(Color.BLACK);
    }

    @Override
    public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
        //canvas.drawCircle(canvas.getWidth()/2,canvas.getHeight()/2,40,paintBitmap);
    }

    @Override
    public void destroyLayer() {

    }

    @Override
    public boolean drawInScreenPixels() {
        return false;
    }
}
