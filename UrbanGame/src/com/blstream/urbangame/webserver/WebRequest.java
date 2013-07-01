package com.blstream.urbangame.webserver;

import android.net.Uri;
import android.os.AsyncTask;

import com.blstream.urbangame.webserver.WebServer.QueryType;

/**
 * This class represents simple asynchronous background HTTP request for
 * specific resource on web server and passes response back to server
 */
//FIXME implementation needed
public class WebRequest extends AsyncTask<Void, Integer, WebResponse> {
	private Uri requestUri;
	private final WebResponse webResponse;
	private WebDownloader webDownloader;
	private final QueryType queryType;
	private final WebServer webServer;
	private final int gameID;
	private final int taskID;
	private String userName;
	private String password;
	
	public WebRequest(WebServer webServer, QueryType queryType) {
		this(webServer, queryType, 0, 0);
	}
	
	public WebRequest(WebServer webServer, QueryType queryType, int gameID) {
		this(webServer, queryType, gameID, 0);
	}
	
	public WebRequest(WebServer webServer, QueryType queryType, int gameID, int taskID) {
		this.gameID = gameID;
		this.taskID = taskID;
		this.queryType = queryType;
		this.webServer = webServer;
		this.webResponse = new WebResponse(queryType);
	}
	
	public WebRequest(WebServer webServer, QueryType queryType, int gameID, String userName, String password) {
		this(webServer, queryType, gameID, 0);
		this.userName = userName;
		this.password = password;
	}
	
	/**
	 * Specific {@link WebDownloader} and request URI is configured here
	 * regarding to query type.
	 */
	@Override
	protected void onPreExecute() {
		switch (queryType) {
			case GetGamesList:
				this.webDownloader = new WebDownloaderGET();
				WebAPI webAPI = new WebAPI(webDownloader);
				this.requestUri = webAPI.getAllGamesUri();
				break;
			/*
			 *  and so on ...
			 *  Combination of requestUri and webDownloader will be used
			 */
			
			default:
				break;
		}
	}
	
	@Override
	protected WebResponse doInBackground(Void... args) {
		String responseString = webDownloader.executeRequest(requestUri);
		webResponse.setResponse(responseString);
		return webResponse;
	}
	
	@Override
	protected void onPostExecute(WebResponse result) {
		webServer.onWebServerResponse(result);
	}
}