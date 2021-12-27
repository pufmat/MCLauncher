package net.puffish.mclauncher;

import java.net.URL;

import org.json.JSONObject;

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

	public String getArgument(){
		return argument.replace("${path}", gd.assetsLogConfigs().resolve(id).toAbsolutePath().toString());
	}
}
