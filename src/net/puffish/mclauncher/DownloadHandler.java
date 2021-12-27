package net.puffish.mclauncher;

import java.net.URL;
import java.nio.file.Path;

public interface DownloadHandler{
	public String downloadToString(URL url) throws Exception;

	public String downloadToFileAndString(URL url, Path path) throws Exception;

	public void downloadToFile(URL url, Path path) throws Exception;

	public void copyFile(Path from, Path to) throws Exception;

	public void extractJar(Path jarPath, Path directory) throws Exception;
}
