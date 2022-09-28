package net.puffish.mclauncher;

import io.vavr.control.Option;

public enum OperationSystem{

	WINDOWS("windows", ";"),
	MACOS("osx", ":"),
	LINUX("linux", ":");

	public static Option<OperationSystem> detect(){
		String os = System.getProperty("os.name").toLowerCase();
		if(os.contains("win")){
			return Option.of(WINDOWS);
		}else if(os.contains("mac")){
			return Option.of(MACOS);
		}else if(os.contains("nix") || os.contains("nux")){
			return Option.of(LINUX);
		}
		return Option.none();
	}

	private final String name;
	private final String classpathSeparator;

	OperationSystem(String name, String classpathSeparator){
		this.name = name;
		this.classpathSeparator = classpathSeparator;
	}

	public String getName(){
		return name;
	}

	public String getClasspathSeparator(){
		return classpathSeparator;
	}
}
