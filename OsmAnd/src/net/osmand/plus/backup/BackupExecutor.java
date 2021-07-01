package net.osmand.plus.backup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class BackupExecutor extends ThreadPoolExecutor {

	private final OsmandApplication app;
	private final List<BackupExecutorListener> listeners = Collections.synchronizedList(new ArrayList<>());
	private final AtomicInteger aState = new AtomicInteger(State.IDLE.ordinal());

	public enum State {
		IDLE,
		BUSY;

		@Nullable
		public static State getByOrdinal(int ordinal) {
			for (State s : values()) {
				if (s.ordinal() == ordinal) {
					return s;
				}
			}
			return null;
		}
	}

	public interface BackupExecutorListener {
		void onBackupExecutorStateChanged(@NonNull State state);
	}

	public BackupExecutor(@NonNull OsmandApplication app) {
		super(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
		this.app = app;
	}

	public State getState() {
		return State.getByOrdinal(aState.get());
	}

	public void addListener(@NonNull BackupExecutorListener listener) {
		listeners.add(listener);
	}

	public void removeListener(@NonNull BackupExecutorListener listener) {
		listeners.remove(listener);
	}

	@Nullable
	private State updateState(boolean beforeExecute) {
		State newState = beforeExecute || !getQueue().isEmpty() ? State.BUSY : State.IDLE;
		State oldState = State.getByOrdinal(aState.getAndSet(newState.ordinal()));
		return newState != oldState ? newState : null;
	}

	@Override
	protected void beforeExecute(Thread t, Runnable r) {
		super.beforeExecute(t, r);
		State newState = updateState(true);
		if (newState != null) {
			notifyStateChanged(newState);
		}
	}

	@Override
	protected void afterExecute(Runnable r, Throwable t) {
		super.afterExecute(r, t);
		State newState = updateState(false);
		if (newState != null) {
			notifyStateChanged(newState);
		}
	}

	private void notifyStateChanged(State state) {
		app.runInUIThread(() -> {
			for (BackupExecutorListener listener : listeners) {
				listener.onBackupExecutorStateChanged(state);
			}
		});
	}
}
