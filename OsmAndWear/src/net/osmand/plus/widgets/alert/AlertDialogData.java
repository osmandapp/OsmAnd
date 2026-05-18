package net.osmand.plus.widgets.alert;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.utils.UiUtilities;

import java.util.HashMap;
import java.util.Map;

public class AlertDialogData {

	public static final int INVALID_ID = -1;

	private final Context ctx;
	private final boolean nightMode;
	@Nullable private Integer controlsColor;

	@Nullable private String title;
	@Nullable private Integer titleId;

	@Nullable private String negativeButtonTitle;
	@Nullable private Integer negativeButtonTitleId;
	@Nullable private OnClickListener negativeButtonListener;

	@Nullable private String neutralButtonTitle;
	@Nullable private Integer neutralButtonTitleId;
	@Nullable private OnClickListener neutralButtonListener;

	@Nullable private String positiveButtonTitle;
	@Nullable private Integer positiveButtonTitleId;
	@Nullable private OnClickListener positiveButtonListener;
	@Nullable @ColorInt private Integer positiveButtonTextColor;

	@Nullable private DialogInterface.OnDismissListener onDismissListener;

	private final Map<AlertDialogExtra, Object> extras = new HashMap<>();

	public AlertDialogData(@NonNull Context ctx, boolean nightMode) {
		this.ctx = UiUtilities.getThemedContext(ctx, nightMode);
		this.nightMode = nightMode;
	}

	public void putExtra(@NonNull AlertDialogExtra key, @NonNull Object value) {
		extras.put(key, value);
	}

	@Nullable
	public Object getExtra(@NonNull AlertDialogExtra key) {
		return extras.get(key);
	}

	public AlertDialogData setTitle(@Nullable String title) {
		this.title = title;
		this.titleId = null;
		return this;
	}

	public AlertDialogData setTitle(@StringRes @Nullable Integer titleId) {
		this.titleId = titleId;
		this.title = null;
		return this;
	}

	public AlertDialogData setNegativeButton(@NonNull String title, @Nullable OnClickListener listener) {
		this.negativeButtonTitle = title;
		this.negativeButtonTitleId = null;
		this.negativeButtonListener = listener;
		return this;
	}

	public AlertDialogData setNegativeButton(@StringRes int titleId, @Nullable OnClickListener listener) {
		this.negativeButtonTitleId = titleId;
		this.negativeButtonTitle = null;
		this.negativeButtonListener = listener;
		return this;
	}

	public AlertDialogData setNeutralButton(@NonNull String title, @Nullable OnClickListener listener) {
		this.neutralButtonTitle = title;
		this.neutralButtonTitleId = null;
		this.neutralButtonListener = listener;
		return this;
	}

	public AlertDialogData setNeutralButton(@StringRes int titleId, @Nullable OnClickListener listener) {
		this.neutralButtonTitleId = titleId;
		this.neutralButtonTitle = null;
		this.neutralButtonListener = listener;
		return this;
	}

	public AlertDialogData setPositiveButton(@NonNull String title, @Nullable OnClickListener listener) {
		this.positiveButtonTitle = title;
		this.positiveButtonTitleId = null;
		this.positiveButtonListener = listener;
		return this;
	}

	public AlertDialogData setPositiveButton(@StringRes int titleId, @Nullable OnClickListener listener) {
		this.positiveButtonTitleId = titleId;
		this.positiveButtonTitle = null;
		this.positiveButtonListener = listener;
		return this;
	}

	public AlertDialogData setPositiveButtonTextColor(@Nullable Integer positiveButtonTextColor) {
		this.positiveButtonTextColor = positiveButtonTextColor;
		return this;
	}

	public AlertDialogData setControlsColor(@Nullable @ColorInt Integer controlsColor) {
		this.controlsColor = controlsColor;
		return this;
	}

	public AlertDialogData setOnDismissListener(@Nullable OnDismissListener onDismissListener) {
		this.onDismissListener = onDismissListener;
		return this;
	}

	public Context getContext() {
		return ctx;
	}

	public boolean isNightMode() {
		return nightMode;
	}

	@Nullable
	public String getTitle() {
		return title;
	}

	@Nullable
	public Integer getTitleId() {
		return titleId;
	}

	@Nullable
	public String getNegativeButtonTitle() {
		return negativeButtonTitle;
	}

	@Nullable
	public Integer getNegativeButtonTitleId() {
		return negativeButtonTitleId;
	}

	@Nullable
	public OnClickListener getNegativeButtonListener() {
		return negativeButtonListener;
	}

	@Nullable
	public String getNeutralButtonTitle() {
		return neutralButtonTitle;
	}

	@Nullable
	public Integer getNeutralButtonTitleId() {
		return neutralButtonTitleId;
	}

	@Nullable
	public OnClickListener getNeutralButtonListener() {
		return neutralButtonListener;
	}

	@Nullable
	public String getPositiveButtonTitle() {
		return positiveButtonTitle;
	}

	@Nullable
	public Integer getPositiveButtonTitleId() {
		return positiveButtonTitleId;
	}

	@Nullable
	public OnClickListener getPositiveButtonListener() {
		return positiveButtonListener;
	}

	@Nullable
	@ColorInt
	public Integer getPositiveButtonTextColor() {
		return positiveButtonTextColor;
	}

	@Nullable
	@ColorInt
	public Integer getControlsColor() {
		return controlsColor;
	}

	@Nullable
	public OnDismissListener getOnDismissListener() {
		return onDismissListener;
	}
}
