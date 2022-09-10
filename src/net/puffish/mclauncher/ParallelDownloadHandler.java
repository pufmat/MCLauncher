package net.puffish.mclauncher;

import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ParallelDownloadHandler extends DefaultDownloadHandler{
	private List<Callable<Void>> parallelTasks = new ArrayList<>();
	private List<Callable<Void>> serialTasks = new ArrayList<>();

	public void invokeAll() throws Exception{
		ExecutorService parallelExecutor = Executors.newCachedThreadPool();
		ExecutorService serialExecutor = Executors.newSingleThreadExecutor();
		try{
			parallelExecutor.invokeAll(parallelTasks);
			serialExecutor.invokeAll(serialTasks);
		}finally{
			parallelTasks.clear();
			serialTasks.clear();
			parallelExecutor.shutdownNow();
			serialExecutor.shutdownNow();
		}
	}

	@Override
	public void downloadToFile(URL url, Path path){
		parallelTasks.add(() -> {
			super.downloadToFile(url, path);
			return null;
		});
	}

	@Override
	public void copyFile(Path from, Path to){
		serialTasks.add(() -> {
			super.copyFile(from, to);
			return null;
		});
	}

	@Override
	public void extractJar(Path jarPath, Path directory){
		serialTasks.add(() -> {
			super.extractJar(jarPath, directory);
			return null;
		});
	}
}
