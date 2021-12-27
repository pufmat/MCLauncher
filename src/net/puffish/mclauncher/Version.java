package net.puffish.mclauncher;

import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.json.JSONArray;
import org.json.JSONObject;

public class Version{
	private GameDirectory gd;
	private OperationSystem os;

	private List<Library> libraries = new ArrayList<Library>();
	private List<String> gameArguments = new ArrayList<String>();
	private List<String> jvmArguments = new ArrayList<String>();

	private LoggingConfig loggingConfig;

	private String versionName;

	private String mainClass;
	private Version inheritsFrom;

	private AssetsIndex assets;

	private URL clientUrl;

	public Version(GameDirectory gd, DownloadHandler dh, OperationSystem os, VersionManifest vm, JSONObject rootJson) throws Exception{
		this.gd = gd;
		this.os = os;

		JSONArray librariesJson = rootJson.getJSONArray("libraries");
		for(Object obj : librariesJson){
			if(obj instanceof JSONObject libraryJson){
				Library library = new Library(gd, libraryJson);
				if(library.matches(os)){
					libraries.add(library);
				}
			}
		}

		JSONObject argumentsJson = rootJson.optJSONObject("arguments");
		if(argumentsJson != null){
			JSONArray gameJson = argumentsJson.optJSONArray("game");
			if(gameJson != null){
				for(Object obj : gameJson){
					if(obj instanceof String str){
						gameArguments.add(str.replace(" ", ""));
					}
				}
			}
			JSONArray jvmJson = argumentsJson.optJSONArray("jvm");
			if(jvmJson != null){
				for(Object obj : jvmJson){
					if(obj instanceof String str){
						jvmArguments.add(str.replace(" ", ""));
					}
				}
			}
		}

		String minecraftArgumentsJson = rootJson.optString("minecraftArguments");
		if(minecraftArgumentsJson != null){
			gameArguments.add(minecraftArgumentsJson);
		}

		versionName = rootJson.getString("id");
		mainClass = rootJson.getString("mainClass");

		String inheritsFromName = rootJson.optString("inheritsFrom", null);
		if(inheritsFromName != null){
			inheritsFrom = (dh == null ? vm.get(inheritsFromName) : vm.download(inheritsFromName, dh));
		}

		JSONObject downloadsJson = rootJson.optJSONObject("downloads");
		if(downloadsJson != null){
			JSONObject clientJson = downloadsJson.getJSONObject("client");
			clientUrl = new URL(clientJson.getString("url"));
		}

		JSONObject assetIndexJson = rootJson.optJSONObject("assetIndex");
		if(assetIndexJson != null){
			assets = new AssetsIndex(gd, assetIndexJson);
		}

		JSONObject loggingJson = rootJson.optJSONObject("logging");
		if(loggingJson != null){
			JSONObject clientJson = loggingJson.optJSONObject("client");
			if(clientJson != null){
				loggingConfig = new LoggingConfig(gd, clientJson);
			}
		}
	}

	public void download(DownloadHandler dh) throws Exception{
		if(assets != null){
			assets.download(dh);
		}

		for(Library library : libraries){
			library.downloadJars(dh);
			library.downloadNatives(dh, os, versionName);
		}

		if(loggingConfig != null){
			loggingConfig.download(dh);
		}

		if(clientUrl != null){
			dh.downloadToFile(clientUrl, gd.versions().resolve(versionName).resolve(versionName + ".jar"));
		}

		if(inheritsFrom != null){
			inheritsFrom.download(dh);

			dh.copyFile(getJarPathRecursive(), getJarPath());
		}
	}

	private void collectJvmArguments(List<String> parts){
		parts.addAll(jvmArguments);

		if(inheritsFrom != null){
			inheritsFrom.collectJvmArguments(parts);
		}
	}

	private void collectGameArguments(List<String> parts){
		parts.addAll(gameArguments);

		if(inheritsFrom != null){
			inheritsFrom.collectGameArguments(parts);
		}
	}

	private void collectLibraries(List<Library> libraries){
		libraries.addAll(this.libraries);

		if(inheritsFrom != null){
			inheritsFrom.collectLibraries(libraries);
		}
	}

	private String getAssetsIndexNameRecursive(){
		if(inheritsFrom != null){
			return inheritsFrom.getAssetsIndexNameRecursive();
		}
		return assets.getName();
	}

	private Path getNativesPathRecursive(){
		if(inheritsFrom != null){
			return inheritsFrom.getNativesPathRecursive();
		}
		return gd.versions().resolve(versionName).resolve("natives");
	}

	private Path getJarPathRecursive(){
		if(inheritsFrom != null){
			return inheritsFrom.getJarPathRecursive();
		}
		return getJarPath();
	}

	private LoggingConfig getLoggingConfigRecursive(){
		if(inheritsFrom != null){
			return inheritsFrom.getLoggingConfigRecursive();
		}
		return loggingConfig;
	}

	private Path getJarPath(){
		return gd.versions().resolve(versionName).resolve(versionName + ".jar");
	}

	public Command getCommand(Arguments arguments, Path javaPath) throws Exception{
		List<Library> libraries = new ArrayList<Library>();
		collectLibraries(libraries);

		String librariesString = Stream.concat(libraries.stream().map(Library::getJarPath).filter(Objects::nonNull), Stream.of(getJarPath())).map(Path::toAbsolutePath).map(Path::toString).collect(Collectors.joining(os.getClasspathSeparator()));

		List<String> parts = new ArrayList<String>();
		parts.add(javaPath.toAbsolutePath().toString());
		if(!arguments.getUserArguments().isEmpty()){
			parts.add(arguments.getUserArguments());
		}

		LoggingConfig loggingConfig = getLoggingConfigRecursive();
		if(loggingConfig != null){
			parts.add(getLoggingConfigRecursive().getArgument());
		}

		collectJvmArguments(parts);

		if(!parts.stream().anyMatch(str -> str.contains("-Djava.library.path="))){
			parts.add("-Djava.library.path=${natives_directory}");
		}

		if(!parts.stream().anyMatch(str -> str.contains("-cp"))){
			parts.add("-cp ${classpath}");
		}

		List<String> tmp = new ArrayList<String>();
		collectGameArguments(tmp);
		parts.addAll(tmp.stream().flatMap(str -> Arrays.stream(str.split(" "))).distinct().toList());

		parts.replaceAll(str -> {
			str = str.replace("${auth_player_name}", arguments.getUsername());
			str = str.replace("${version_name}", versionName);
			str = str.replace("${game_directory}", gd.root().toAbsolutePath().toString());
			str = str.replace("${assets_root}", gd.assets().toAbsolutePath().toString());
			str = str.replace("${auth_uuid}", "00000000000000000000000000000000");
			str = str.replace("${auth_access_token}", "00000000000000000000000000000000");
			str = str.replace("${user_properties}", "{}");
			str = str.replace("${user_type}", "mojang");
			str = str.replace("${version_type}", "release");
			str = str.replace("${assets_index_name}", getAssetsIndexNameRecursive());
			str = str.replace("${auth_session}", "00000000000000000000000000000000");
			str = str.replace("${game_assets}", "resources");
			str = str.replace("${classpath}", librariesString + " " + mainClass);
			str = str.replace("${library_directory}", gd.libraries().toAbsolutePath().toString());
			str = str.replace("${classpath_separator}", os.getClasspathSeparator());
			str = str.replace("${natives_directory}", getNativesPathRecursive().toAbsolutePath().toString());
			str = str.replace("${clientid}", "0000");
			str = str.replace("${auth_xuid}", "0000");
			return str;
		});

		String command = parts.stream().collect(Collectors.joining(" "));

		return new Command(command, gd.root());
	}
}
