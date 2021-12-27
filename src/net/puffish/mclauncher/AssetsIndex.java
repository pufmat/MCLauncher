package net.puffish.mclauncher;

import java.net.URL;
import java.nio.file.Path;

import org.json.JSONObject;

public class AssetsIndex{
	private GameDirectory gd;
	private String id;
	private URL url;

	public AssetsIndex(GameDirectory gd, JSONObject json) throws Exception{
		this.gd = gd;
		this.url = new URL(json.getString("url"));
		this.id = json.getString("id");
	}

	public void download(DownloadHandler dh) throws Exception{
		Path path = gd.assetsIndexes().resolve(id + ".json");
		JSONObject json = new JSONObject(dh.downloadToFileAndString(url, path));

		JSONObject objectsJson = json.getJSONObject("objects");
		for(String key : objectsJson.keySet()){
			AssetObject assetObject = new AssetObject(objectsJson.getJSONObject(key));
			assetObject.download(gd, dh);
			assetObject.copyToVirtual(gd, dh, key);
			if(id.equals("pre-1.6")){
				assetObject.copyToResources(gd, dh, key);
			}
		}
	}

	public String getName(){
		return id;
	}
}
