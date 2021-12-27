package net.puffish.mclauncher;

public class Arguments{
	private String username;
	private String userArguments;

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
