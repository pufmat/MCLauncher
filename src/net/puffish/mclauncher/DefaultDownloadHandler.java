package net.puffish.mclauncher;

import io.vavr.control.Either;

import java.io.InputStream;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.concurrent.Executors;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class DefaultDownloadHandler implements DownloadHandler {
	private final HttpClient client = HttpClient.newBuilder()
			.executor(Executors.newFixedThreadPool(
					Math.max(1, Runtime.getRuntime().availableProcessors() - 1)
			))
			.build();

	@Override
	public Either<Exception, String> downloadToString(URL url) {
		try {
			var request = HttpRequest.newBuilder(url.toURI()).GET().build();
			var response = client.send(request, HttpResponse.BodyHandlers.ofString());
			return Either.right(response.body());
		} catch (Exception e) {
			return Either.left(
					new RuntimeException("An error occurred while downloading " + url.toString(), e)
			);
		}
	}

	@Override
	public Either<Exception, String> downloadToFileAndString(URL url, Path path) {
		try {
			var request = HttpRequest.newBuilder(url.toURI()).GET().build();
			var response = client.send(request, HttpResponse.BodyHandlers.ofString());
			String content = response.body();
			Files.createDirectories(path.getParent());
			Files.writeString(path, content);
			return Either.right(content);
		} catch (Exception e) {
			return Either.left(
					new RuntimeException("An error occurred while downloading " + url.toString(), e)
			);
		}
	}

	@Override
	public Either<Exception, Void> downloadToFile(URL url, Path path) {
		try {
			var request = HttpRequest.newBuilder(url.toURI()).GET().build();
			var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
			streamToFile(response.body(), path);
			return Either.right(null);
		} catch (Exception e) {
			return Either.left(
					new RuntimeException("An error occurred while downloading " + url.toString(), e)
			);
		}
	}

	@Override
	public Either<Exception, Void> copyFile(Path from, Path to) {
		try {
			Files.createDirectories(to.getParent());
			Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
			return Either.right(null);
		} catch (Exception e) {
			return Either.left(
					new RuntimeException("An error occurred while copying file from " + from.toString() + " to " + to.toString(), e)
			);
		}
	}

	@Override
	public Either<Exception, Void> extractJar(Path jarPath, Path directory) {
		try (JarFile jar = new JarFile(jarPath.toFile())) {
			Enumeration<JarEntry> enumeration = jar.entries();
			while (enumeration.hasMoreElements()) {
				JarEntry entry = enumeration.nextElement();
				Path path = directory.resolve(entry.getName());
				if (entry.isDirectory()) {
					Files.createDirectories(path);
				} else {
					streamToFile(jar.getInputStream(entry), path);
				}
			}
			return Either.right(null);
		} catch (Exception e) {
			return Either.left(
					new RuntimeException("An error occurred while extracting jar " + jarPath.toString(), e)
			);
		}
	}

	private void streamToFile(InputStream stream, Path to) throws Exception {
		Files.createDirectories(to.getParent());
		Files.copy(stream, to, StandardCopyOption.REPLACE_EXISTING);
	}
}
