package net.osmand.plus.render;

import android.graphics.DashPathEffect;

public class OsmandDashPathEffect extends DashPathEffect {
   private final float[] intervals;

   public OsmandDashPathEffect(float[] intervals, float phase) {
      super(intervals, phase);
      this.intervals = intervals;
   }

   public float[] getIntervals() {
      return intervals;
   }
}
