package net.osmand.access.tasker;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.twofortyfouram.locale.api.Intent;
import com.twofortyfouram.locale.sdk.client.receiver.AbstractPluginConditionReceiver;

public class TaskerPluginReceiver extends AbstractPluginConditionReceiver {
	public TaskerPluginReceiver() {
	}

	@Override
	protected boolean isBundleValid(@NonNull Bundle bundle) {
		return PluginBundleValues.isBundleValid(bundle);
	}

	@Override
	protected boolean isAsync() {
		return true;
	}

	@Override
	protected int getPluginConditionResult(@NonNull Context context, @NonNull Bundle bundle) {
		return Intent.RESULT_CONDITION_UNSATISFIED;
	}
}
