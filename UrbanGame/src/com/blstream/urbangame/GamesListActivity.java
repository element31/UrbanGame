package com.blstream.urbangame;

import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnActionExpandListener;
import com.actionbarsherlock.widget.SearchView;
import com.actionbarsherlock.widget.SearchView.OnQueryTextListener;
import com.blstream.urbangame.adapters.GamesListAdapter;
import com.blstream.urbangame.database.Database;
import com.blstream.urbangame.database.entity.UrbanGame;
import com.blstream.urbangame.database.entity.UrbanGameShortInfo;
import com.blstream.urbangame.helpers.ExpandableListViewPropertiesSetter;
import com.blstream.urbangame.web.WebHighLevel;
import com.blstream.urbangame.web.WebHighLevelInterface;
import com.blstream.urbangame.webserver.ServerResponseHandler;
import com.blstream.urbangame.webserver.WebResponse;
import com.blstream.urbangame.webserver.WebServerNotificationListener;

public class GamesListActivity extends AbstractGamesListActivity implements OnChildClickListener,
	WebServerNotificationListener {
	private GamesListAdapter adapter;
	private ExpandableListView list;
	private ServerResponseHandler handler;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_games_list);
		setSupportProgressBarVisibility(true);
		
		this.handler = new ServerResponseHandler(this);
		WebHighLevelInterface web = new WebHighLevel(handler, this);
		web.downloadGamesList();
		
		list = (ExpandableListView) findViewById(R.id.listViewAllGamesList);
		
		adapter = new GamesListAdapter(this, R.layout.list_item_game);
		list.setAdapter(adapter);
		list.setOnChildClickListener(this);
		
		ExpandableListViewPropertiesSetter.setPropertiesOfExpandableListView(adapter, list);
	}
	
	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
		Bundle bundle = new Bundle();
		
		UrbanGameShortInfo game = (UrbanGameShortInfo) adapter.getChild(groupPosition, childPosition);
		Long selectedGameId = (game == null ? -1 : game.getID());
		bundle.putLong(GameDetailsActivity.GAME_KEY, selectedGameId);
		
		Intent intent = new Intent(GamesListActivity.this, GameDetailsActivity.class);
		intent.putExtras(bundle);
		startActivity(intent);
		return true;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		MenuInflater menuInflater = getSupportMenuInflater();
		menuInflater.inflate(R.menu.top_bar_search, menu);
		configureSearchAction(menu);
		
		return true;
	}
	
	private void configureSearchAction(Menu menu) {
		final MenuItem moreItem = menu.findItem(R.id.menu_more);;
		
		MenuItem searchItem = menu.findItem(R.id.menu_list_search);
		searchItem.setOnActionExpandListener(new OnActionExpandListener() {
			@Override
			public boolean onMenuItemActionExpand(MenuItem item) {
				moreItem.setVisible(false);
				return true;
			}
			
			@Override
			public boolean onMenuItemActionCollapse(MenuItem item) {
				moreItem.setVisible(true);
				return true;
			}
		});
		
		SearchView searchView = (SearchView) searchItem.getActionView();
		searchView.setOnQueryTextListener(new OnQueryTextListener() {
			@Override
			public boolean onQueryTextSubmit(String query) {
				return true;
			}
			
			@Override
			public boolean onQueryTextChange(String newText) {
				return true;
			}
		});
	}
	
	@Override
	public void onWebServerResponse(Message message) {
		// TODO implement on response behavior
		// FIXME refreshing adapters should be moved here
		Log.i("DOWNLOAD GAMES", ((WebResponse) message.obj).getResponse());
		tempJSonParse(((WebResponse) message.obj).getResponse());
	}
	
	private void tempJSonParse(String jsonString) {
		JSONObject jsonObject;
		UrbanGame urbanGame, urbanGame2;
		Database database = new Database(this);
		try {
			jsonObject = new JSONObject(jsonString);
			JSONArray jsonArray = jsonObject.getJSONArray("_embedded");
			for (int i = 0; i < jsonArray.length(); i++) {
				jsonObject = jsonArray.getJSONObject(i);
				urbanGame = new UrbanGame();
				urbanGame.setID(jsonObject.getLong("gid"));
				urbanGame.setGameVersion(jsonObject.getDouble("version"));
				urbanGame.setTitle(jsonObject.getString("name"));
				urbanGame.setStartDate(new Date(jsonObject.getLong("startTime")));
				urbanGame.setEndDate(new Date(jsonObject.getLong("endTime")));
				urbanGame.setOperatorName(jsonObject.getString("operatorName"));
				// dummy
				urbanGame.setPlayers(0);
				urbanGame.setDescription("");
				urbanGame.setWinningStrategy("");
				urbanGame.setLocation("");
				urbanGame.setDetailsLink("");
				urbanGame.setReward(false);
				urbanGame2 = database.getGameInfo(jsonObject.getLong("gid"));
				if (urbanGame2 == null) {
					if (database.insertGameInfo(urbanGame)) {
						Log.i("PARSING", "added a game");
					}
					else {
						Log.i("PARSING", "did not add a game");
					}
				}
				//TODO check if different
				
			}
		}
		catch (JSONException e) {
			Log.e("JSONparse games list", e.getMessage());
		}
		database.closeDatabase();
		adapter.updateData();
		Log.i("PARSING", "finished");
	}
}