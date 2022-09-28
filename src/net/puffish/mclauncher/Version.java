package net.puffish.mclauncher;

import io.vavr.control.Either;
import io.vavr.control.Option;
import net.puffish.mclauncher.VariablesReplacer.Variable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Version{
	private final GameDirectory gd;
	private final OperationSystem os;

	private final List<Library> libraries;
	private final List<String> gameArguments;
	private final List<String> jvmArguments;

	private final Option<LoggingConfig> loggingConfig;

	private final String versionName;

	private final String mainClass;
	private final Option<Version> inheritsFrom;

	private final Option<AssetsIndex> assets;

	private final Option<URL> clientUrl;

	public static Either<Exception, Version> tryMake(GameDirectory gd, OperationSystem os, JSONObject rootJson, Function<String, Either<Exception, Version>> versionFunction){
		List<Library> libraries = new ArrayList<>();
		List<String> gameArguments = new ArrayList<>();
		List<String> jvmArguments = new ArrayList<>();

		JSONArray librariesJson = rootJson.getJSONArray("libraries");
		for (Object obj : librariesJson) {
			if (obj instanceof JSONObject libraryJson) {
				Library library = new Library(gd, libraryJson);
				if (library.matches(os)) {
					libraries.add(library);
				}
			}
		}

		JSONObject argumentsJson = rootJson.optJSONObject("arguments");
		if (argumentsJson != null) {
			JSONArray gameJson = argumentsJson.optJSONArray("game");
			if (gameJson != null) {
				for (Object obj : gameJson) {
					if (obj instanceof String str) {
						gameArguments.add(str.replace(" ", ""));
					}
				}
			}
			JSONArray jvmJson = argumentsJson.optJSONArray("jvm");
			if (jvmJson != null) {
				for (Object obj : jvmJson) {
					if (obj instanceof String str) {
						jvmArguments.add(str.replace(" ", ""));
					}
				}
			}
		}

		String minecraftArgumentsJson = rootJson.optString("minecraftArguments");
		if (minecraftArgumentsJson != null) {
			gameArguments.add(minecraftArgumentsJson);
		}

		Either<Exception, Version> inheritsFrom = Either.left(null);
		String inheritsFromName = rootJson.optString("inheritsFrom", null);
		if (inheritsFromName != null) {
			inheritsFrom = versionFunction.apply(inheritsFromName);
			if(inheritsFrom instanceof Either.Left<Exception, Version> left){
				return left;
			}
		}

		Option<URL> clientUrl = Option.none();
		JSONObject downloadsJson = rootJson.optJSONObject("downloads");
		if (downloadsJson != null) {
			JSONObject clientJson = downloadsJson.getJSONObject("client");
			try{
				clientUrl = Option.of(new URL(clientJson.getString("url")));
			}catch (Exception e){
				return Either.left(e);
			}
		}

		Either<Exception, AssetsIndex> assets = Either.left(null);
		JSONObject assetIndexJson = rootJson.optJSONObject("assetIndex");
		if (assetIndexJson != null) {
			assets = AssetsIndex.tryMake(gd, assetIndexJson);
			if(assets instanceof Either.Left<Exception, AssetsIndex> left){
				return Either.left(left.getLeft());
			}
		}

		Either<Exception, LoggingConfig> loggingConfig = Either.left(null);
		JSONObject loggingJson = rootJson.optJSONObject("logging");
		if (loggingJson != null) {
			JSONObject clientJson = loggingJson.optJSONObject("client");
			if (clientJson != null) {
				loggingConfig = LoggingConfig.tryMake(gd, clientJson);
				if(loggingConfig instanceof Either.Left<Exception, LoggingConfig> left){
					return Either.left(left.getLeft());
				}
			}
		}

		return Either.right(new Version(
				gd,
				os,
				libraries,
				gameArguments,
				jvmArguments,
				loggingConfig.toOption(),
				rootJson.getString("id"),
				rootJson.getString("mainClass"),
				inheritsFrom.toOption(),
				assets.toOption(),
				clientUrl
		));
	}

	private Version(GameDirectory gd, OperationSystem os, List<Library> libraries, List<String> gameArguments, List<String> jvmArguments, Option<LoggingConfig> loggingConfig, String versionName, String mainClass, Option<Version> inheritsFrom, Option<AssetsIndex> assets, Option<URL> clientUrl) {
		this.gd = gd;
		this.os = os;
		this.libraries = libraries;
		this.gameArguments = gameArguments;
		this.jvmArguments = jvmArguments;
		this.loggingConfig = loggingConfig;
		this.versionName = versionName;
		this.mainClass = mainClass;
		this.inheritsFrom = inheritsFrom;
		this.assets = assets;
		this.clientUrl = clientUrl;
	}

	public Either<Exception, Void> downloadFiles(DownloadHandler dh){
		if(assets.map(x -> x.download(dh)).getOrElse(() -> Either.right(null)) instanceof Either.Left<Exception, Void> left){
			return left;
		}

		for(Library library : libraries){
			if(library.downloadJars(dh) instanceof Either.Left<Exception, Void> left){
				return left;
			}
			if(library.downloadNatives(dh, os, versionName) instanceof Either.Left<Exception, Void> left){
				return left;
			}
		}

		if(loggingConfig.map(x -> x.download(dh)).getOrElse(() -> Either.right(null)) instanceof Either.Left<Exception, Void> left){
			return left;
		}

		if(clientUrl.map(x -> dh.downloadToFile(x, gd.versions().resolve(versionName).resolve(versionName + ".jar"))).getOrElse(() -> Either.right(null)) instanceof Either.Left<Exception, Void> left){
			return left;
		}
		if(inheritsFrom.map(x -> x.downloadFiles(dh).flatMap(y -> dh.copyFile(getJarPathRecursive(), getJarPath()))).getOrElse(() -> Either.right(null)) instanceof Either.Left<Exception, Void> left){
			return left;
		}

		return Either.right(null);
	}

	private void collectJvmArguments(List<String> parts){
		parts.addAll(jvmArguments);

		inheritsFrom.peek(x -> x.collectJvmArguments(parts));
	}

	private void collectGameArguments(List<String> parts){
		parts.addAll(gameArguments);

		inheritsFrom.peek(x -> x.collectGameArguments(parts));
	}

	private void collectLibraries(List<Library> libraries){
		libraries.addAll(this.libraries);

		inheritsFrom.peek(x -> x.collectLibraries(libraries));
	}

	private Option<String> getAssetsIndexNameRecursive(){
		return inheritsFrom.map(Version::getAssetsIndexNameRecursive).getOrElse(() -> assets.map(AssetsIndex::getName));
	}

	private Path getNativesPathRecursive(){
		return inheritsFrom.map(Version::getNativesPathRecursive).getOrElse(this::getNativesPath);
	}

	private Path getNativesPath(){
		return gd.versions().resolve(versionName).resolve("natives");
	}

	private Option<LoggingConfig> getLoggingConfigRecursive(){
		return inheritsFrom.map(Version::getLoggingConfigRecursive).getOrElse(loggingConfig);
	}

	private Path getJarPathRecursive(){
		return inheritsFrom.map(Version::getJarPathRecursive).getOrElse(this::getJarPath);
	}

	private Path getJarPath(){
		return gd.versions().resolve(versionName).resolve(versionName + ".jar");
	}

	public Command getLaunchCommand(Arguments arguments, Path javaPath){
		List<Library> libraries = new ArrayList<>();
		collectLibraries(libraries);

		String librariesString =
				Stream.concat(
								libraries.stream().map(Library::getJarPath).filter(Option::isDefined).map(Option::get),
								Stream.of(getJarPath())
						)
						.map(Path::toAbsolutePath)
						.map(Path::toString)
						.collect(Collectors.joining(os.getClasspathSeparator()));

		List<String> command = new ArrayList<>();
		command.add(javaPath.toAbsolutePath().toString());
		if(!arguments.getUserArguments().isEmpty()){
			command.add(arguments.getUserArguments());
		}

		getLoggingConfigRecursive().peek(x -> command.addAll(x.getArgument()));

		collectJvmArguments(command);

		if(command.stream().noneMatch(str -> str.contains("-Djava.library.path="))){
			command.add("-Djava.library.path=${natives_directory}");
		}

		if(command.stream().noneMatch(str -> str.contains("-cp"))){
			command.add("-cp");
			command.add("${classpath}");
		}

		List<String> gameArguments = new ArrayList<>();
		collectGameArguments(gameArguments);
		command.addAll(gameArguments.stream().flatMap(str -> Arrays.stream(str.split(" +"))).distinct().toList());

		List<Variable> variables = new ArrayList<>(List.of(
				new Variable("${auth_player_name}", arguments.getUsername()),
				new Variable("${version_name}", versionName),
				new Variable("${game_directory}", gd.root().toAbsolutePath().toString()),
				new Variable("${assets_root}", gd.assets().toAbsolutePath().toString()),
				new Variable("${auth_uuid}", UUID.nameUUIDFromBytes(("OfflinePlayer:" + arguments.getUsername()).getBytes(StandardCharsets.UTF_8)).toString()),
				new Variable("${auth_access_token}", "00000000000000000000000000000000"),
				new Variable("${user_properties}", "{}"),
				new Variable("${user_type}", "mojang"),
				new Variable("${version_type}", "release"),
				new Variable("${auth_session}", "00000000000000000000000000000000"),
				new Variable("${game_assets}", "resources"),
				new Variable("${classpath}", librariesString, mainClass),
				new Variable("${library_directory}", gd.libraries().toAbsolutePath().toString()),
				new Variable("${classpath_separator}", os.getClasspathSeparator()),
				new Variable("${natives_directory}", getNativesPathRecursive().toAbsolutePath().toString()),
				new Variable("${clientid}", "0000"),
				new Variable("${auth_xuid}", "0000")
		));
		getAssetsIndexNameRecursive().peek(x -> variables.add(new Variable("${assets_index_name}", x)));

		VariablesReplacer vr = new VariablesReplacer(variables);

		return new Command(
				command.stream()
						.flatMap(str -> vr.replace(str).stream())
						.toList(),
				gd.root()
		);
	}
}
