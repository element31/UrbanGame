package com.blstream.urbangame.web;

import com.blstream.urbangame.database.entity.Player;

public interface WebHighLevelInterface {
	
	/**
	 * it downloads games basic info and inserts it into database
	 */
	void downloadGameList();
	
	/**
	 * it downloads all games that user is participating and inserts them into
	 * database
	 */
	void downloadUsersGames();
	
	/**
	 * it downloads game's details and inserts into database
	 * 
	 * @param game's ID to be downloaded if not already in database.
	 */
	void downloadGameDetails(Long selectedGameID);
	
	/**
	 * it joins player to the provided game
	 * 
	 * @param selectedGameID - game's ID to be joined by current user
	 */
	void joinCurrentPlayerToTheGame(Long selectedGameID);
	
	/**
	 * it dis-joins player from the game
	 * 
	 * @param selectedGameID - game's ID to be leaved by current user
	 */
	void leaveCurrentPlayerToTheGame(Long selectedGameID);
	
	/**
	 * it check if user can "log in" (REST is stateless so no session - check if
	 * can get some resource | consult with web)
	 * 
	 * @param email - user's email
	 * @param password - provided password
	 * @return - player data if successful, else null
	 */
	Player checkUsersCredinetials(String email, String password);
	
	/**
	 * if registers user in web server
	 * 
	 * @param email
	 * @param displayName
	 * @param password
	 * @return player if successful, null if not succeeded
	 */
	Player registerPlayer(String email, String displayName, String password);
	
	/**
	 * it downloads all tasks for game with data for current user if they are
	 * not already in database and inserts it into database.
	 * 
	 * @param gameID - game of which tasks should be downloaded
	 */
	void downloadTasksForGame(long gameID);
	
}
