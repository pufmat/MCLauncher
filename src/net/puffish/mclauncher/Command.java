package net.puffish.mclauncher;

import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class Command{
	private final List<String> command;
	private final Path directory;

	public Command(List<String> command, Path directory){
		this.command = command;
		this.directory = directory;
	}

	public Process createProcess() throws IOException{
		return Runtime.getRuntime().exec(command.toArray(String[]::new), null, directory.toFile());
	}

	@Override
	public String toString(){
		return command.stream()
				.map(str -> str.contains(" ") ? JSONObject.quote(str) : str)
				.collect(Collectors.joining(" "));
	}
}
