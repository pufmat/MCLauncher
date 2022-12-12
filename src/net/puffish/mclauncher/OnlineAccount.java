package net.puffish.mclauncher;

import io.vavr.control.Either;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class OnlineAccount implements Account{
	private final String name;
	private final String uuid;
	private final String accessToken;

	public OnlineAccount(String name, String uuid, String accessToken) {
		this.name = name;
		this.uuid = uuid;
		this.accessToken = accessToken;
	}

	public static Either<Exception, OnlineAccount> login(String accessToken){
		try{

			HttpClient client = HttpClient.newHttpClient();
			HttpRequest request = HttpRequest.newBuilder(new URI("https://api.minecraftservices.com/minecraft/profile"))
					.GET()
					.header("Authorization", "Bearer " + accessToken)
					.header("Content-Type", "application/json")
					.header("Accept", "application/json")
					.build();
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

			JSONObject responseJson = new JSONObject(response.body());

			String error = responseJson.optString("errorMessage", null);
			if(error != null){
				return Either.left(new RuntimeException(error));
			}

			String name = responseJson.getString("name");
			String uuid = responseJson.getString("id");

			return Either.right(new OnlineAccount(name, uuid, accessToken));
		}catch (Exception e){
			return Either.left(e);
		}
	}

	public String getName() {
		return name;
	}

	public String getUUID() {
		return uuid;
	}

	public String getAccessToken() {
		return accessToken;
	}
}
