package net.puffish.mclauncher;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import org.json.JSONArray;
import org.json.JSONObject;

public class VersionManifest{
	private GameDirectory gd;
	private OperationSystem os;

	private JSONObject json;

	public VersionManifest(GameDirectory gd, OperationSystem os){
		this.gd = gd;
		this.os = os;
	}

	private Path getJsonPath(String versionName){
		return gd.versions().resolve(versionName).resolve(versionName + ".json");
	}

	private URL findDownloadUrl(String versionName, DownloadHandler dh) throws Exception{
		if(json == null){
			json = new JSONObject(dh.downloadToString(new URL("https://launchermeta.mojang.com/mc/game/version_manifest.json")));
		}

		JSONArray versionsJson = json.getJSONArray("versions");
		for(Object obj : versionsJson){
			if(obj instanceof JSONObject versionJson){
				if(versionJson.getString("id").equals(versionName)){
					return new URL(versionJson.getString("url"));
				}
			}
		}

		return null;
	}

	private Version get(Path path, DownloadHandler dh) throws Exception{
		return get(path, dh, Files.readString(path));
	}

	private Version get(Path path, DownloadHandler dh, String content) throws Exception{
		return new Version(gd, dh, os, this, new JSONObject(content));
	}

	public Version get(String versionName) throws Exception{
		return get(getJsonPath(versionName), null);
	}

	public Version download(String versionName, DownloadHandler dh) throws Exception{
		Path path = getJsonPath(versionName);
		URL url = findDownloadUrl(versionName, dh);
		if(url == null){
			return get(path, dh);
		}else{
			return get(path, dh, dh.downloadToFileAndString(findDownloadUrl(versionName, dh), path));
		}
	}
}
