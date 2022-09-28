package net.puffish.mclauncher;

import io.vavr.control.Either;
import io.vavr.control.Option;
import org.json.JSONObject;

import java.net.URL;
import java.nio.file.Path;

public class AssetObject{
	private final String hash;
	private final URL url;

	public static Option<AssetObject> tryMake(JSONObject assetJson){
		try {
			String hash = assetJson.getString("hash");
			return Option.of(new AssetObject(
					hash,
					new URL("http://resources.download.minecraft.net/" + hash.substring(0, 2) + "/" + hash)
			));
		}catch (Exception e){
			return Option.none();
		}
	}

	private AssetObject(String hash, URL url) {
		this.hash = hash;
		this.url = url;
	}

	public Either<Exception, Void> download(GameDirectory gd, DownloadHandler dh){
		return dh.downloadToFile(url, gd.assetsObjects().resolve(hash.substring(0, 2)).resolve(hash));
	}

	public Either<Exception, Void> copyToVirtual(GameDirectory gd, DownloadHandler dh, String key){
		Path path = gd.assetsVirtualLegacy().resolve(key.replaceFirst("minecraft/", ""));
		if(!path.startsWith(gd.assetsVirtualLegacy())){
			return Either.right(null);
		}
		return dh.copyFile(gd.assetsObjects().resolve(hash.substring(0, 2)).resolve(hash), path);
	}

	public Either<Exception, Void> copyToResources(GameDirectory gd, DownloadHandler dh, String key){
		Path path = gd.resources().resolve(key.replaceFirst("minecraft/", ""));
		if(!path.startsWith(gd.resources())){
			return Either.right(null);
		}
		return dh.copyFile(gd.assetsObjects().resolve(hash.substring(0, 2)).resolve(hash), path);
	}
}
