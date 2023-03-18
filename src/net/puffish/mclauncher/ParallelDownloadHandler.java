package net.puffish.mclauncher;

import io.vavr.control.Either;
import io.vavr.control.Option;

import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class ParallelDownloadHandler implements DownloadHandler {
	private final DownloadHandler dh;

	protected final HashMap<Path, Supplier<Either<Exception, Void>>> parallelTasks = new HashMap<>();
	protected final List<Supplier<Either<Exception, Void>>> serialTasks = new ArrayList<>();

	public ParallelDownloadHandler() {
		this(new DefaultDownloadHandler());
	}

	public ParallelDownloadHandler(DownloadHandler dh) {
		this.dh = dh;
	}

	public Either<Exception, Void> invokeAll() {
		ExecutorService parallelExecutor = Executors.newFixedThreadPool(
				Math.max(1, Runtime.getRuntime().availableProcessors() - 1)
		);

		var latch = new CountDownLatch(1);
		var countDown = new AtomicInteger(parallelTasks.size());

		var parallelFutures = parallelTasks.values()
				.stream()
				.map(task -> parallelExecutor.submit(() -> {
					var tmp = task.get();
					if (tmp.isRight()) {
						if (countDown.decrementAndGet() == 0) {
							latch.countDown();
						}
					} else {
						latch.countDown();
					}
					return tmp;
				}))
				.toList();

		try {
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		parallelExecutor.shutdownNow();

		return Option.ofOptional(
						parallelFutures.stream()
								.filter(Predicate.not(Future::isCancelled))
								.map(future -> {
									try {
										return future.get();
									} catch (Exception e) {
										return Either.<Exception, Void>left(e);
									}
								})
								.filter(Either::isLeft)
								.findAny()
				)
				.getOrElse(() -> {
					for (var task : serialTasks) {
						var result = task.get();
						if (result.isLeft()) {
							return result;
						}
					}

					return Either.right(null);
				});
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
		parallelTasks.put(path, () -> dh.downloadToFile(url, path));
		return Either.right(null);
	}

	@Override
	public Either<Exception, Void> copyFile(Path from, Path to) {
		serialTasks.add(() -> dh.copyFile(from, to));
		return Either.right(null);
	}

	@Override
	public Either<Exception, Void> extractJar(Path jarPath, Path directory) {
		serialTasks.add(() -> dh.extractJar(jarPath, directory));
		return Either.right(null);
	}
}
