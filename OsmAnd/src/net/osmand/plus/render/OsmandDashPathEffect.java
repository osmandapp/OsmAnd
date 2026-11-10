package net.osmand.plus.render;

import android.graphics.DashPathEffect;
import android.graphics.PathEffect;

import androidx.annotation.Nullable;

public class OsmandDashPathEffect extends DashPathEffect {
   private final float[] intervals;

   public OsmandDashPathEffect(float[] intervals, float phase) {
      super(intervals, phase);
      this.intervals = intervals;
   }

   public float[] getIntervals() {
      return intervals;
   }

   @Nullable
   public static float[] getIntervals(@Nullable PathEffect pathEffect) {
      return pathEffect instanceof OsmandDashPathEffect effect ? effect.getIntervals() : null;
   }
}
