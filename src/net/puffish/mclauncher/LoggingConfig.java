package net.puffish.mclauncher;

import io.vavr.control.Either;
import net.puffish.mclauncher.VariablesReplacer.Variable;
import org.json.JSONObject;

import java.net.URL;
import java.util.List;

public class LoggingConfig{
	private final GameDirectory gd;
	private final URL url;
	private final String id;
	private final String argument;

	public static Either<Exception, LoggingConfig> tryMake(GameDirectory gd, JSONObject client){
		try{
			JSONObject file = client.getJSONObject("file");
			return Either.right(new LoggingConfig(
					gd,
					new URL(file.getString("url")),
					file.getString("id"),
					client.getString("argument")
			));
		}catch (Exception e){
			return Either.left(e);
		}
	}

	private LoggingConfig(GameDirectory gd, URL url, String id, String argument) {
		this.gd = gd;
		this.url = url;
		this.id = id;
		this.argument = argument;
	}

	public Either<Exception, Void> download(DownloadHandler dh){
		return dh.downloadToFile(url, gd.assetsLogConfigs().resolve(id));
	}

	public List<String> getArgument(){
		VariablesReplacer vr = new VariablesReplacer(List.of(
			new Variable("${path}", gd.assetsLogConfigs().resolve(id).toAbsolutePath().toString())
		));
		return vr.replace(argument);
	}
}
