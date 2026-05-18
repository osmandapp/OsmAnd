package net.osmand.plus.render;

import android.graphics.Color;
import android.graphics.Path;

import androidx.annotation.NonNull;

import net.osmand.data.QuadRect;
import net.osmand.plus.render.OsmandRenderer.RenderingContext;
import net.osmand.render.RenderingRuleSearchRequest;

public class TextDrawInfo {

	String text;
	Path drawOnPath;
	QuadRect bounds;
	float vOffset;
	float centerX;
	float pathRotate;
	float centerY;
	float textSize;
	float minDistance;
	int textColor = Color.BLACK;
	int textShadow;
	int textWrap;
	boolean bold;
	boolean italic;
	String shieldRes;
	String shieldResIcon;
	int textOrder = 100;
	int textShadowColor = Color.WHITE;

	public TextDrawInfo(@NonNull String text) {
		this.text = text;
	}

	public void fillProperties(RenderingContext rc, RenderingRuleSearchRequest render, float centerX, float centerY) {
		this.centerX = centerX;
		// used only for draw on path where centerY doesn't play role
		this.vOffset = (int) rc.getComplexValue(render, render.ALL.R_TEXT_DY);
		this.centerY = centerY + this.vOffset;
		textColor = render.getIntPropertyValue(render.ALL.R_TEXT_COLOR);
		if (textColor == 0) {
			textColor = Color.BLACK;
		}
		textSize = rc.getComplexValue(render, render.ALL.R_TEXT_SIZE);
		textShadow = (int) rc.getComplexValue(render, render.ALL.R_TEXT_HALO_RADIUS);
		textShadowColor = render.getIntPropertyValue(render.ALL.R_TEXT_HALO_COLOR);
		if (textShadowColor == 0) {
			textShadowColor = Color.WHITE;
		}
		textWrap = (int) rc.getComplexValue(render, render.ALL.R_TEXT_WRAP_WIDTH);
		bold = render.getIntPropertyValue(render.ALL.R_TEXT_BOLD, 0) > 0;
		italic = render.getIntPropertyValue(render.ALL.R_TEXT_ITALIC, 0) > 0;
		minDistance = rc.getComplexValue(render, render.ALL.R_TEXT_MIN_DISTANCE);
		if (render.isSpecified(render.ALL.R_TEXT_SHIELD)) {
			shieldRes = render.getStringPropertyValue(render.ALL.R_TEXT_SHIELD);
		}
		if (render.isSpecified(render.ALL.R_ICON)) {
			shieldResIcon = render.getStringPropertyValue(render.ALL.R_ICON);
		}
		textOrder = render.getIntPropertyValue(render.ALL.R_TEXT_ORDER, 100);
	}

	public float getCenterX() {
		return centerX;
	}

	public void setCenterX(float centerX) {
		this.centerX = centerX;
	}

	public float getCenterY() {
		return centerY;
	}

	public void setCenterY(float centerY) {
		this.centerY = centerY;
	}

	public String getShieldResIcon() {
		return shieldResIcon;
	}

	public void setShieldResIcon(String shieldResIcon) {
		this.shieldResIcon = shieldResIcon;
	}
}