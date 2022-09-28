package net.puffish.mclauncher;

import java.nio.file.Path;

public class GameDirectory{
	private final Path path;

	public GameDirectory(Path path){
		this.path = path;
	}

	public Path root(){
		return path;
	}

	public Path versions(){
		return path.resolve("versions");
	}

	public Path libraries(){
		return path.resolve("libraries");
	}

	public Path assets(){
		return path.resolve("assets");
	}

	public Path resources(){
		return path.resolve("resources");
	}

	public Path assetsObjects(){
		return assets().resolve("objects");
	}

	public Path assetsIndexes(){
		return assets().resolve("indexes");
	}

	public Path assetsVirtualLegacy(){
		return assets().resolve("virtual").resolve("legacy");
	}

	public Path assetsLogConfigs(){
		return path.resolve("assets").resolve("log_configs");
	}
}
