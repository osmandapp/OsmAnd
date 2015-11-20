package net.osmand.plus.tasker;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.twofortyfouram.locale.sdk.client.receiver.AbstractPluginConditionReceiver;

public class TaskerPluginReceiver extends AbstractPluginConditionReceiver {
	public TaskerPluginReceiver() {
	}

	@Override
	protected boolean isBundleValid(@NonNull Bundle bundle) {
		return false;
	}

	@Override
	protected boolean isAsync() {
		return true;
	}

	@Override
	protected int getPluginConditionResult(@NonNull Context context, @NonNull Bundle bundle) {
		return com.twofortyfouram.locale.api.Intent.RESULT_CONDITION_UNKNOWN;
	}
}
