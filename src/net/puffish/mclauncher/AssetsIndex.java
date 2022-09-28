package net.puffish.mclauncher;

import io.vavr.control.Either;
import io.vavr.control.Option;
import org.json.JSONObject;

import java.net.URL;
import java.nio.file.Path;

public class AssetsIndex{
	private final GameDirectory gd;
	private final String id;
	private final URL url;

	public static Either<Exception, AssetsIndex> tryMake(GameDirectory gd, JSONObject json){
		try{
			return Either.right(new AssetsIndex(
					gd,
					json.getString("id"),
					new URL(json.getString("url"))
			));
		}catch (Exception e){
			return Either.left(e);
		}
	}

	private AssetsIndex(GameDirectory gd, String id, URL url) {
		this.gd = gd;
		this.id = id;
		this.url = url;
	}

	public Either<Exception, Void> download(DownloadHandler dh){
		Path path = gd.assetsIndexes().resolve(id + ".json");
		return dh.downloadToFileAndString(url, path).flatMap(x -> {
			try {
				JSONObject json = new JSONObject(x);

				JSONObject objectsJson = json.getJSONObject("objects");
				for (String key : objectsJson.keySet()) {
					Option<AssetObject> optAssetObject = AssetObject.tryMake(objectsJson.getJSONObject(key));
					if (optAssetObject.isEmpty()) {
						continue;
					}
					AssetObject assetObject = optAssetObject.get();

					if (assetObject.download(gd, dh) instanceof Either.Left<Exception, Void> left) {
						return left;
					}
					if (assetObject.copyToVirtual(gd, dh, key) instanceof Either.Left<Exception, Void> left) {
						return left;
					}
					if (id.equals("pre-1.6")) {
						if (assetObject.copyToResources(gd, dh, key) instanceof Either.Left<Exception, Void> left) {
							return left;
						}
					}
				}

				return Either.right(null);
			}catch (Exception e){
				return Either.left(e);
			}
		});
	}

	public String getName(){
		return id;
	}
}
