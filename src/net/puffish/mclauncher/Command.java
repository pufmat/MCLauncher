package net.puffish.mclauncher;

import java.io.IOException;
import java.nio.file.Path;

public class Command{
	private String command;
	private Path directory;

	public Command(String command, Path directory){
		this.command = command;
		this.directory = directory;
	}

	public Process createProcess() throws IOException{
		return Runtime.getRuntime().exec(command, null, directory.toFile());
	}

	@Override
	public String toString(){
		return command;
	}
}
