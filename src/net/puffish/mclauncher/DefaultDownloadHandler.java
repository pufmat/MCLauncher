package net.puffish.mclauncher;

import io.vavr.control.Either;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class DefaultDownloadHandler implements DownloadHandler {
	@Override
	public Either<Exception, String> downloadToString(URL url) {
		try {
			return Either.right(new String(url.openStream().readAllBytes()));
		} catch (Exception e) {
			return Either.left(
					new RuntimeException("An error occurred while downloading " + url.toString(), e)
			);
		}
	}

	@Override
	public Either<Exception, String> downloadToFileAndString(URL url, Path path) {
		try {
			String content = new String(url.openStream().readAllBytes());
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
			streamToFile(url.openStream(), path);
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
		try {
			JarFile jar = new JarFile(jarPath.toFile());
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
