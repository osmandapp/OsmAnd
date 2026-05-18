package net.osmand.plus.backup;

import android.annotation.SuppressLint;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPoolTaskExecutor<T extends ThreadPoolTaskExecutor.Task> extends ThreadPoolExecutor {

	private static final int DEFAULT_THREAD_POOL_SIZE = 4;

	private boolean interruptOnError;
	private final OnThreadPoolTaskExecutorListener<T> listener;

	private boolean cancelled;
	private final Map<Future<?>, T> taskMap = new ConcurrentHashMap<>();
	private final Map<Future<?>, Throwable> exceptions = new ConcurrentHashMap<>();
	private AsyncTaskExecutor asyncTask;

	public interface OnThreadPoolTaskExecutorListener<T> {
		void onTaskStarted(@NonNull T task);

		void onTaskFinished(@NonNull T task);

		void onTasksFinished(@NonNull List<T> results);
	}

	public interface ProgressPublisher<T> {
		void publishTaskProgress(@NonNull T task);
	}

	public abstract static class Task implements Callable<Void> {

		boolean cancelled;
		boolean finished;

		public boolean isFinished() {
			return finished;
		}

		public boolean isCancelled() {
			return cancelled;
		}

		public void cancel() {
			this.cancelled = true;
		}

		public void setFinished(boolean finished) {
			this.finished = finished;
		}
	}

	public ThreadPoolTaskExecutor(@Nullable OnThreadPoolTaskExecutorListener<T> listener) {
		super(DEFAULT_THREAD_POOL_SIZE, DEFAULT_THREAD_POOL_SIZE, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
		this.listener = listener;
	}

	public ThreadPoolTaskExecutor(int poolSize, @Nullable OnThreadPoolTaskExecutorListener<T> listener) {
		super(poolSize, poolSize, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
		this.listener = listener;
	}

	public Map<Future<?>, Throwable> getExceptions() {
		return exceptions;
	}

	public boolean isCancelled() {
		return cancelled;
	}

	public void cancel() {
		this.cancelled = true;
	}

	private boolean isInterrupted() {
		return !exceptions.isEmpty();
	}

	public boolean isInterruptOnError() {
		return interruptOnError;
	}

	public void setInterruptOnError(boolean interruptOnError) {
		this.interruptOnError = interruptOnError;
	}

	public void run(@NonNull List<T> tasks) {
		runImpl(tasks);
		if (listener != null) {
			listener.onTasksFinished(new ArrayList<>(taskMap.values()));
		}
	}

	private void runImpl(@NonNull List<T> tasks) {
		initTasks(tasks);
		shutdown();
		waitUntilFinished();
	}

	public void runAsync(@NonNull List<T> tasks, @Nullable Executor executor) {
		asyncTask = new AsyncTaskExecutor(tasks);
		asyncTask.executeOnExecutor(executor != null ? executor : AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private void initTasks(List<T> tasks) {
		for (T task : tasks) {
			Future<?> future = submit(task);
			taskMap.put(future, task);
		}
	}

	private void waitUntilFinished() {
		boolean finished = false;
		while (!finished) {
			try {
				finished = awaitTermination(100, TimeUnit.MILLISECONDS);
				if (isCancelled() || (interruptOnError && isInterrupted())) {
					for (Entry<Future<?>, T> futureTask : taskMap.entrySet()) {
						futureTask.getKey().cancel(false);
						futureTask.getValue().cancel();
					}
				}
			} catch (InterruptedException e) {
				// ignore
			}
		}
	}

	@Override
	protected void beforeExecute(Thread t, Runnable r) {
		super.beforeExecute(t, r);
		if (r instanceof Future<?>) {
			Future<?> future = (Future<?>) r;
			T task = taskMap.get(future);
			if (task != null) {
				if (asyncTask != null) {
					asyncTask.publishTaskProgress(task);
				} else {
					publishTaskProgress(task);
				}
			}
		}
	}

	protected void afterExecute(Runnable r, Throwable t) {
		super.afterExecute(r, t);
		if (r instanceof Future<?>) {
			Future<?> future = (Future<?>) r;
			if (t == null && future.isDone()) {
				T task = taskMap.get(future);
				if (task != null) {
					task.finished = true;
					if (asyncTask != null) {
						asyncTask.publishTaskProgress(task);
					} else {
						publishTaskProgress(task);
					}
				}
				try {
					future.get();
				} catch (CancellationException | InterruptedException e) {
					// ignore
				} catch (ExecutionException ee) {
					exceptions.put(future, ee.getCause());
				}
			} else {
				exceptions.put(future, t);
			}
		}
	}

	@SafeVarargs
	private final void publishTaskProgress(T... values) {
		if (listener != null) {
			T task = values[0];
			if (task.isFinished()) {
				listener.onTaskFinished(task);
			} else {
				listener.onTaskStarted(task);
			}
		}
	}

	@SuppressLint("StaticFieldLeak")
	private class AsyncTaskExecutor extends AsyncTask<Void, T, Void> {
		private final List<T> tasks;

		public AsyncTaskExecutor(List<T> tasks) {
			this.tasks = tasks;
		}

		@SafeVarargs
		@Override
		protected final void onProgressUpdate(T... values) {
			ThreadPoolTaskExecutor.this.publishTaskProgress(values);
		}

		@Override
		protected Void doInBackground(Void... voids) {
			runImpl(tasks);
			return null;
		}

		@Override
		protected void onPostExecute(Void unused) {
			if (listener != null) {
				listener.onTasksFinished(new ArrayList<>(taskMap.values()));
			}
		}

		private void publishTaskProgress(@NonNull T task) {
			publishProgress(task);
		}
	}
}
