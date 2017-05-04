package net.osmand.plus.mapcontextmenu.builders.cards;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

public abstract class AbstractCard {

	protected View view;

	public abstract int getCardLayoutId();

	public View build(Context ctx) {
		view = LayoutInflater.from(ctx).inflate(getCardLayoutId(), null);
		update();
		return view;
	}

	public abstract void update();

}
