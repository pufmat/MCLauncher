package net.puffish.mclauncher;

import io.vavr.control.Either;

import java.net.URL;
import java.nio.file.Path;

public interface DownloadHandler{
	Either<Exception, String> downloadToString(URL url);

	Either<Exception, String> downloadToFileAndString(URL url, Path path);

	Either<Exception, Void> downloadToFile(URL url, Path path);

	Either<Exception, Void> copyFile(Path from, Path to);

	Either<Exception, Void> extractJar(Path jarPath, Path directory);
}
