package net.puffish.mclauncher;

import io.vavr.control.Either;

import java.net.URL;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class ParallelDownloadHandler implements DownloadHandler {
	private final int retries;

	private final DownloadHandler dh;

	protected final List<Task> parallelTasks = new ArrayList<>();
	protected final List<Task> serialTasks = new ArrayList<>();

	public ParallelDownloadHandler(int retries) {
		this(retries, new SerialDownloadHandler(HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(60))
				.executor(Executors.newWorkStealingPool())
				.build()));
	}

	public ParallelDownloadHandler(int retries, DownloadHandler dh) {
		this.retries = retries;
		this.dh = dh;
	}

	public Either<Exception, Void> invokeAll() {
		var parallel = this.new ParallelExecutor(this.parallelTasks);

		return parallel.invokeAll()
				.flatMap(v -> {
					for (var task : serialTasks) {
						var result = task.run();
						if (result.isLeft()) {
							return result;
						}
					}
					return Either.right(null);
				});
	}

	private static class Task {
		private final Supplier<Either<Exception, Void>> supplier;
		private int runCount = 0;

		public Task(Supplier<Either<Exception, Void>> supplier) {
			this.supplier = supplier;
		}

		public int getRunCount() {
			return runCount;
		}

		public Either<Exception, Void> run() {
			runCount++;
			return supplier.get();
		}
	}

	private class ParallelExecutor {
		private final AtomicBoolean stopped = new AtomicBoolean(false);
		private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(
				Math.max(1, Runtime.getRuntime().availableProcessors() - 1)
		);

		private final Collection<Task> tasks;
		private final CountDownLatch latch;

		private Exception exception = null;

		public ParallelExecutor(Collection<Task> tasks) {
			this.tasks = tasks;
			this.latch = new CountDownLatch(tasks.size());
		}

		public Either<Exception, Void> invokeAll() {
			for (var task : tasks) {
				runTask(task, 0);
			}

			try {
				latch.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			executor.shutdownNow();

			if (exception == null) {
				return Either.right(null);
			} else {
				return Either.left(new RuntimeException("Too many retries", exception));
			}
		}

		private void runTask(Task task, long delay) {
			executor.schedule(() -> {
				if (stopped.get()) {
					latch.countDown();
					return;
				}
				var result = task.run();
				if (result.isRight()) {
					latch.countDown();
				} else if (task.getRunCount() > retries) {
					exception = result.getLeft();
					stopped.set(true);
					latch.countDown();
				} else {
					runTask(task, delay + 1);
				}
			}, delay, TimeUnit.SECONDS);
		}
	}

	@Override
	public Either<Exception, String> downloadToString(URL url) {
		return dh.downloadToString(url);
	}

	@Override
	public Either<Exception, String> downloadToFileAndString(URL url, Path path) {
		return dh.downloadToFileAndString(url, path);
	}

	@Override
	public Either<Exception, Void> downloadToFile(URL url, Path path) {
		parallelTasks.add(new Task(() -> dh.downloadToFile(url, path)));
		return Either.right(null);
	}

	@Override
	public Either<Exception, Void> copyFile(Path from, Path to) {
		serialTasks.add(new Task(() -> dh.copyFile(from, to)));
		return Either.right(null);
	}

	@Override
	public Either<Exception, Void> extractJar(Path jarPath, Path directory) {
		serialTasks.add(new Task(() -> dh.extractJar(jarPath, directory)));
		return Either.right(null);
	}
}
