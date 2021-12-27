package net.puffish.mclauncher;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class DefaultDownloadHandler implements DownloadHandler{
	@Override
	public String downloadToString(URL url) throws Exception{
		return new String(url.openStream().readAllBytes());
	}

	@Override
	public String downloadToFileAndString(URL url, Path path) throws Exception{
		String content = new String(url.openStream().readAllBytes());
		Files.createDirectories(path.getParent());
		Files.writeString(path, content);
		return content;
	}

	@Override
	public void downloadToFile(URL url, Path path) throws Exception{
		streamToFile(url.openStream(), path);
	}

	@Override
	public void copyFile(Path from, Path to) throws Exception{
		Files.createDirectories(to.getParent());
		Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
	}

	@Override
	public void extractJar(Path jarPath, Path directory) throws Exception{
		JarFile jar = new JarFile(jarPath.toFile());
		Enumeration<JarEntry> enumeration = jar.entries();
		while(enumeration.hasMoreElements()){
			JarEntry entry = enumeration.nextElement();
			Path path = directory.resolve(entry.getName());
			if(entry.isDirectory()){
				path.toFile().mkdirs();
			}else{
				streamToFile(jar.getInputStream(entry), path);
			}
		}
	}

	private void streamToFile(InputStream stream, Path to) throws Exception{
		Files.createDirectories(to.getParent());
		Files.copy(stream, to, StandardCopyOption.REPLACE_EXISTING);
	}
}
