package net.puffish.mclauncher;

public class Arguments{
	private final String username;
	private final String userArguments;

	public Arguments(String username){
		this(username, "");
	}

	public Arguments(String username, String userArguments){
		this.username = username;
		this.userArguments = userArguments;
	}

	public String getUsername(){
		return username;
	}

	public String getUserArguments(){
		return userArguments;
	}
}
