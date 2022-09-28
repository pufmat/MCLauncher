package net.puffish.mclauncher;

import io.vavr.control.Either;
import io.vavr.control.Option;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;
import java.nio.file.Path;

public class Library{
	private final GameDirectory gd;

	private final JSONObject libraryJson;
	private JSONObject artifactJson;
	private JSONObject classifiersJson;

	public Library(GameDirectory gd, JSONObject libraryJson){
		this.gd = gd;
		this.libraryJson = libraryJson;

		JSONObject downloadsJson = libraryJson.optJSONObject("downloads");
		if(downloadsJson != null){
			artifactJson = downloadsJson.optJSONObject("artifact");
			classifiersJson = downloadsJson.optJSONObject("classifiers");
		}
	}

	public Either<Exception, Void> downloadJars(DownloadHandler dh){
		try {
			if (artifactJson != null) {
				if (artifactJson.getString("url").isBlank()) {
					return Either.right(null);
				}
				return dh.downloadToFile(
						new URL(artifactJson.getString("url")),
						gd.libraries().resolve(artifactJson.getString("path"))
				);
			}

			if (libraryJson.optJSONObject("downloads") != null) {
				return Either.right(null);
			}

			String name = libraryJson.optString("name", null);
			if (name != null) {
				String url = libraryJson.optString("url", null);
				if (url == null) {
					url = "https://libraries.minecraft.net/";
				}
				if (!url.endsWith("/")) {
					url += "/";
				}
				String[] parts = name.split(":");
				String jarPath = parts[0].replace('.', '/') + "/" + parts[1] + "/" + parts[2] + "/" + parts[1] + "-" + parts[2] + ".jar";
				Path path = gd.libraries()
						.resolve(jarPath);
				if (path.startsWith(gd.libraries())) {
					if (!path.toFile()
							.exists()) {
						if(dh.downloadToFile(new URL(url + jarPath), path) instanceof Either.Left<Exception, Void> left){
							return left;
						}
					}
				}
			}

			return Either.right(null);
		}catch (Exception e){
			return Either.left(e);
		}
	}

	public Either<Exception, Void> downloadNatives(DownloadHandler dh, OperationSystem os, String versionName){
		try {
			if (classifiersJson == null) {
				return Either.right(null);
			}

			JSONObject nativesJson = libraryJson.optJSONObject("natives");
			if (nativesJson == null) {
				return Either.right(null);
			}

			String nativesName = nativesJson.optString(os.getName(), null);
			if (nativesName == null) {
				return Either.right(null);
			}

			JSONObject nameJson = classifiersJson.getJSONObject(nativesName);

			Path jarPath = gd.libraries().resolve(nameJson.getString("path"));

			if(dh.downloadToFile(new URL(nameJson.getString("url")), jarPath) instanceof Either.Left<Exception, Void> left){
				return left;
			}

			Path natives = gd.versions()
					.resolve(versionName)
					.resolve("natives");

			return dh.extractJar(jarPath, natives);
		}catch (Exception e){
			return Either.left(e);
		}
	}

	public boolean matches(OperationSystem os){
		JSONArray rulesJson = libraryJson.optJSONArray("rules");
		if(rulesJson == null){
			return true;
		}

		for(Object obj : rulesJson){
			if(obj instanceof JSONObject ruleJson){
				JSONObject osJson = ruleJson.optJSONObject("os");
				if(osJson != null){
					if(osJson.getString("name").equals(os.getName())){
						return ruleJson.getString("action").equals("allow");
					}
				}
			}
		}

		for(Object obj : rulesJson){
			if(obj instanceof JSONObject ruleJson){
				JSONObject osJson = ruleJson.optJSONObject("os");
				if(osJson == null){
					return ruleJson.getString("action").equals("allow");
				}
			}
		}

		return false;
	}

	public Option<Path> getJarPath(){
		if(artifactJson != null){
			return Option.of(
					gd.libraries().resolve(artifactJson.getString("path"))
			);
		}

		String name = libraryJson.getString("name");
		String[] parts = name.split(":");
		String jarPath = parts[0].replace('.', '/') + "/" + parts[1] + "/" + parts[2] + "/" + parts[1] + "-" + parts[2] + ".jar";
		Path path = gd.libraries().resolve(jarPath);
		if(!path.startsWith(gd.libraries())){
			return Option.none();
		}
		return Option.of(path);
	}
}
