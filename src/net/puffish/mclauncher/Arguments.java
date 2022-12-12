package net.puffish.mclauncher;

public class Arguments{
	private final Account account;

	private final String userArguments;

	public Arguments(Account account){
		this(account, "");
	}

	public Arguments(Account account, String userArguments){
		this.account = account;
		this.userArguments = userArguments;
	}

	public String getUserArguments(){
		return userArguments;
	}

	public String getName(){
		return account.getName();
	}

	public String getAccessToken(){
		return account.getAccessToken();
	}
	public String getSession(){
		return "token:" + getAccessToken() + ":" + getUUID();
	}

	public String getUUID(){
		return account.getUUID();
	}
}
