package net.puffish.mclauncher;

import java.net.URL;
import java.nio.file.Path;

import org.json.JSONObject;

public class AssetObject{
	private String hash;
	private URL url;

	public AssetObject(JSONObject assetJson) throws Exception{
		hash = assetJson.getString("hash");
		url = new URL("http://resources.download.minecraft.net/" + hash.substring(0, 2) + "/" + hash);
	}

	public void download(GameDirectory gd, DownloadHandler dh) throws Exception{
		dh.downloadToFile(url, gd.assetsObjects().resolve(hash.substring(0, 2)).resolve(hash));
	}

	public void copyToVirtual(GameDirectory gd, DownloadHandler dh, String key) throws Exception{
		Path path = gd.assetsVirtualLegacy().resolve(key.replaceFirst("minecraft/", ""));
		if(!path.startsWith(gd.assetsVirtualLegacy())){
			return;
		}
		dh.copyFile(gd.assetsObjects().resolve(hash.substring(0, 2)).resolve(hash), path);
	}

	public void copyToResources(GameDirectory gd, DownloadHandler dh, String key) throws Exception{
		Path path = gd.resources().resolve(key.replaceFirst("minecraft/", ""));
		if(!path.startsWith(gd.resources())){
			return;
		}
		dh.copyFile(gd.assetsObjects().resolve(hash.substring(0, 2)).resolve(hash), path);
	}
}
