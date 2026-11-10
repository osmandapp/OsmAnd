package net.osmand.plus.views.mapwidgets.widgets;

import static net.osmand.plus.views.mapwidgets.WidgetType.TSS;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;

public class TSSWidget extends SimpleWidget {

	private Float cachedTSS;
	private final NormalizedPowerCalculator normalizedPowerCalculator;

	public TSSWidget(@NonNull MapActivity mapActivity, @Nullable String customId, @Nullable WidgetsPanel widgetsPanel) {
		super(mapActivity, TSS, customId, widgetsPanel);
		this.normalizedPowerCalculator = NormalizedPowerCalculator.getInstance();
	}

	@Override
	protected void setupView(@NonNull View view) {
		super.setupView(view);
		setIcons(TSS);
		setText(null, null);
	}

	@Override
	protected void updateSimpleWidgetInfo(@Nullable DrawSettings drawSettings) {
		float tss = calculateTSS();
		if (isUpdateNeeded() || cachedTSS == null || Math.abs(cachedTSS - tss) > 0.01f) {
			cachedTSS = tss;
			if (cachedTSS > 0) {
				setText(String.format("%.1f", cachedTSS), null);
			} else {
				setText(NO_VALUE, null);
			}
		}
	}

	private float calculateTSS() {
		if (normalizedPowerCalculator != null) {
			int ftp = settings.TRAINING_FTP.get();
			return normalizedPowerCalculator.getTSS(ftp);
		}
		return 0;
	}

	@Override
	public boolean isMetricSystemDepended() {
		return false;
	}
}
