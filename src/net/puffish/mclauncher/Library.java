package net.puffish.mclauncher;

import java.net.URL;
import java.nio.file.Path;

import org.json.JSONArray;
import org.json.JSONObject;

public class Library{
	private GameDirectory gd;

	private JSONObject libraryJson;
	private JSONObject artifactJson;
	private JSONObject classifiersJson;

	public Library(GameDirectory gd, JSONObject libraryJson) throws Exception{
		this.gd = gd;
		this.libraryJson = libraryJson;

		JSONObject downloadsJson = libraryJson.optJSONObject("downloads");
		if(downloadsJson != null){
			artifactJson = downloadsJson.optJSONObject("artifact");
			classifiersJson = downloadsJson.optJSONObject("classifiers");
		}
	}

	public void downloadJars(DownloadHandler dh) throws Exception{
		if(artifactJson != null){
			if(artifactJson.getString("url").isBlank()) {
				return;
			}
			dh.downloadToFile(new URL(artifactJson.getString("url")), gd.libraries().resolve(artifactJson.getString("path")));
			return;
		}

		if(libraryJson.optJSONObject("downloads") != null){
			return;
		}

		String name = libraryJson.optString("name", null);
		if(name != null){
			String url = libraryJson.optString("url", null);
			if(url == null){
				url = "https://libraries.minecraft.net/";
			}
			if(!url.endsWith("/")){
				url += "/";
			}
			String[] parts = name.split(":");
			String jarPath = parts[0].replace('.', '/') + "/" + parts[1] + "/" + parts[2] + "/" + parts[1] + "-" + parts[2] + ".jar";
			Path path = gd.libraries().resolve(jarPath);
			if(path.startsWith(gd.libraries())){
				if(!path.toFile().exists()){
					dh.downloadToFile(new URL(url + jarPath), path);
				}
			}
		}
	}

	public void downloadNatives(DownloadHandler dh, OperationSystem os, String versionName) throws Exception{
		if(classifiersJson == null){
			return;
		}

		JSONObject nativesJson = libraryJson.optJSONObject("natives");
		if(nativesJson == null){
			return;
		}

		String nativesName = nativesJson.optString(os.getName(), null);
		if(nativesName == null){
			return;
		}

		JSONObject nameJson = classifiersJson.getJSONObject(nativesName);

		Path jarPath = gd.libraries().resolve(nameJson.getString("path"));
		dh.downloadToFile(new URL(nameJson.getString("url")), jarPath);

		Path natives = gd.versions().resolve(versionName).resolve("natives");

		dh.extractJar(jarPath, natives);
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

	public Path getJarPath(){
		if(artifactJson != null){
			return gd.libraries().resolve(artifactJson.getString("path"));
		}

		String name = libraryJson.getString("name");
		String[] parts = name.split(":");
		String jarPath = parts[0].replace('.', '/') + "/" + parts[1] + "/" + parts[2] + "/" + parts[1] + "-" + parts[2] + ".jar";
		Path path = gd.libraries().resolve(jarPath);
		if(!path.startsWith(gd.libraries())){
			return null;
		}
		return path;
	}
}
