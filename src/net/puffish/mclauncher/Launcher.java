package net.puffish.mclauncher;

import io.vavr.control.Either;
import io.vavr.control.Option;
import org.json.JSONObject;

import java.net.URL;

public class Launcher{
	private final GameDirectory gd;
	private final OperationSystem os;

	public Launcher(GameDirectory gd, OperationSystem os){
		this.gd = gd;
		this.os = os;
	}

	public VersionManifest getVersionManifest(){
		return new VersionManifest(gd, os, Option.none());
	}

	public Either<Exception, VersionManifest> downloadVersionManifest(DownloadHandler dh){
		try {
			return dh.downloadToString(new URL("https://launchermeta.mojang.com/mc/game/version_manifest.json"))
					.map(x -> new VersionManifest(gd, os, Option.of(new JSONObject(x))));
		} catch (Exception e){
			return Either.left(e);
		}
	}
}
