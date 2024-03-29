package net.puffish.mclauncher;

import io.vavr.control.Either;
import io.vavr.control.Option;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

public class VersionManifest {
	private final GameDirectory gd;
	private final OperationSystem os;

	private final Option<JSONObject> json;

	public VersionManifest(GameDirectory gd, OperationSystem os, Option<JSONObject> json) {
		this.gd = gd;
		this.os = os;
		this.json = json;
	}

	private Path getJsonPath(String versionName) {
		return gd.versions()
				.resolve(versionName)
				.resolve(versionName + ".json");
	}

	private Either<Exception, URL> findDownloadUrl(String versionName) {
		return json.<Either<Exception, URL>>map(x -> {
			try {
				JSONArray versionsJson = x.getJSONArray("versions");
				for (Object obj : versionsJson) {
					if (obj instanceof JSONObject versionJson) {
						if (versionJson.getString("id").equals(versionName)) {
							return Either.right(new URL(versionJson.getString("url")));
						}
					}
				}
			} catch (Exception e) {
				return Either.left(e);
			}
			return Either.left(new IllegalStateException("Version `" + versionName + "` does not exists!"));
		}).getOrElse(() -> Either.left(new IllegalStateException("Missing version manifest!")));
	}

	private Either<Exception, Version> getVersionInfo(Path path, Function<String, Either<Exception, Version>> supplier) {
		try {
			return Version.tryMake(gd, os, new JSONObject(Files.readString(path)), supplier);
		} catch (Exception e) {
			return Either.left(e);
		}
	}

	private Either<Exception, Version> getVersionInfo(String versionName, Function<String, Either<Exception, Version>> supplier) {
		return getVersionInfo(getJsonPath(versionName), supplier);
	}

	public Either<Exception, Version> getVersionInfo(String versionName) {
		return getVersionInfo(versionName, this::getVersionInfo);
	}

	public Either<Exception, Version> downloadVersionInfo(String versionName, URL versionUrl, DownloadHandler dh) {
		try {
			return dh.downloadToFileAndString(versionUrl, getJsonPath(versionName))
					.flatMap(x -> Version.tryMake(gd, os, new JSONObject(x), y -> this.downloadVersionInfo(y, dh)));
		} catch (Exception e) {
			return Either.left(e);
		}
	}

	public Either<Exception, Version> downloadVersionInfo(String versionName, DownloadHandler dh) {
		return findDownloadUrl(versionName)
				.fold(
						e -> getVersionInfo(versionName, x -> downloadVersionInfo(x, dh)),
						versionUrl -> downloadVersionInfo(versionName, versionUrl, dh)
				);
	}
}
