package net.puffish.mclauncher;

import java.nio.file.Path;

public class Launcher{
	private VersionManifest vm;

	public Launcher(GameDirectory gd) throws Exception{
		this(gd, OperationSystem.detect());
	}

	public Launcher(GameDirectory gd, OperationSystem os) throws Exception{
		this.vm = new VersionManifest(gd, os);
	}

	public void download(String versionName) throws Exception{
		ParallelDownloadHandler pdh = new ParallelDownloadHandler();
		download(versionName, pdh);
		pdh.invokeAll();
	}

	public void download(String versionName, DownloadHandler dh) throws Exception{
		vm.download(versionName, dh).download(dh);
	}

	public Command launch(String versionName, Arguments arguments, Path javaPath) throws Exception{
		return vm.get(versionName).getCommand(arguments, javaPath);
	}
}
