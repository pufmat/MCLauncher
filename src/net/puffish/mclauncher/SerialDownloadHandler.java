package net.puffish.mclauncher;

import io.vavr.control.Either;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class SerialDownloadHandler implements DownloadHandler {
	private final HttpClient client;

	public SerialDownloadHandler(HttpClient client) {
		this.client = client;
	}

	public SerialDownloadHandler() {
		this(HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(60))
				.build()
		);
	}

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
			Files.createDirectories(path.getParent());
			try (var stream = new BufferedOutputStream(new FileOutputStream(path.toFile()))) {
				var request = HttpRequest.newBuilder(url.toURI()).GET().build();
				var response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
				var length = response.headers().firstValueAsLong("Content-Length").orElseThrow();
				var body = response.body();

				stream.write(body);
				var written = body.length;

				while (written < length) {
					request = HttpRequest.newBuilder(url.toURI()).GET().header("Range", "bytes=" + written + "-" + length).build();
					response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
					body = response.body();

					if (body.length == 0) {
						throw new RuntimeException("Received 0 bytes");
					}

					stream.write(body);
					written += body.length;
				}
			}
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
					Files.createDirectories(path.getParent());
					Files.copy(jar.getInputStream(entry), path, StandardCopyOption.REPLACE_EXISTING);
				}
			}
			return Either.right(null);
		} catch (Exception e) {
			return Either.left(
					new RuntimeException("An error occurred while extracting jar " + jarPath.toString(), e)
			);
		}
	}
}
