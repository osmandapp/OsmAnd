package net.osmand.plus.card.base.multistate;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

public class CardState {

	private final static int INVALID_ID = -1;

	private final String title;
	private final int titleId;

	private boolean showTopDivider;
	private Object tag;

	public CardState(@NonNull String title) {
		this.title = title;
		this.titleId = INVALID_ID;
	}

	public CardState(@StringRes int titleId) {
		this.titleId = titleId;
		this.title = null;
	}

	@NonNull
	public String toHumanString(@NonNull Context context) {
		return title != null ? title : context.getString(titleId);
	}

	@NonNull
	public CardState setTag(@Nullable Object tag) {
		this.tag = tag;
		return this;
	}

	@Nullable
	public Object getTag() {
		return tag;
	}

	public boolean isShowTopDivider() {
		return showTopDivider;
	}

	@NonNull
	public CardState setShowTopDivider(boolean showTopDivider) {
		this.showTopDivider = showTopDivider;
		return this;
	}
}
