package net.puffish.mclauncher;

import io.vavr.control.Either;
import io.vavr.control.Option;

import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public class ParallelDownloadHandler extends DefaultDownloadHandler{
	protected final List<Supplier<Either<Exception, Void>>> parallelTasks = new ArrayList<>();
	protected final List<Supplier<Either<Exception, Void>>> serialTasks = new ArrayList<>();

	public Either<Exception, Void> invokeAll() {
		ExecutorService parallelExecutor = Executors.newCachedThreadPool();

		var parallelFutures = parallelTasks.stream()
				.map(task -> CompletableFuture.supplyAsync(task, parallelExecutor))
				.toList();

		for (var future : parallelFutures) {
			future.whenComplete((result, throwable) -> {
				if (result.isLeft()) {
					parallelFutures.forEach(f -> f.cancel(true));
				}
			});
		}

		return Option.ofOptional(
				parallelFutures.stream()
						.map(CompletableFuture::join)
						.filter(Either::isLeft)
						.findAny()
		).getOrElse(() -> {
			for(var task : serialTasks){
				var result = task.get();
				if(result.isLeft()){
					return result;
				}
			}

			return Either.right(null);
		});
	}

	@Override
	public Either<Exception, Void> downloadToFile(URL url, Path path){
		parallelTasks.add(() -> super.downloadToFile(url, path));
		return Either.right(null);
	}

	@Override
	public Either<Exception, Void> copyFile(Path from, Path to){
		serialTasks.add(() -> super.copyFile(from, to));
		return Either.right(null);
	}

	@Override
	public Either<Exception, Void> extractJar(Path jarPath, Path directory){
		serialTasks.add(() -> super.extractJar(jarPath, directory));
		return Either.right(null);
	}
}
