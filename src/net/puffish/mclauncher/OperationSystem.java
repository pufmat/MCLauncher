package net.puffish.mclauncher;

public enum OperationSystem{

	WINDOWS("windows", ";"),
	MACOS("osx", ":"),
	LINUX("linux", ":");

	public static OperationSystem detect(){
		String os = System.getProperty("os.name").toLowerCase();
		if(os.contains("win")){
			return WINDOWS;
		}else if(os.contains("mac")){
			return MACOS;
		}else if(os.contains("nix") || os.contains("nux")){
			return LINUX;
		}
		throw new IllegalStateException();
	}

	private String name;
	private String classpathSeparator;

	private OperationSystem(String name, String classpathSeparator){
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
