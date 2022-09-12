package net.puffish.mclauncher;

import net.puffish.mclauncher.VariablesReplacer.Variable;
import org.json.JSONObject;

import java.net.URL;
import java.util.List;

public class LoggingConfig{
	private GameDirectory gd;
	private URL url;
	private String id;
	private String argument;

	public LoggingConfig(GameDirectory gd, JSONObject client) throws Exception{
		this.gd = gd;
		argument = client.getString("argument");
		JSONObject file = client.getJSONObject("file");
		url = new URL(file.getString("url"));
		id = file.getString("id");
	}

	public void download(DownloadHandler dh) throws Exception{
		dh.downloadToFile(url, gd.assetsLogConfigs().resolve(id));
	}

	public List<String> getArgument(){
		VariablesReplacer vr = new VariablesReplacer(
			new Variable("${path}", gd.assetsLogConfigs().resolve(id).toAbsolutePath().toString())
		);
		return vr.replace(argument);
	}
}
