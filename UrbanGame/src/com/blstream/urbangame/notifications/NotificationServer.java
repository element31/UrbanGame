package com.blstream.urbangame.notifications;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;

import com.blstream.urbangame.R;
import com.blstream.urbangame.UrbanGameApplication;
import com.blstream.urbangame.database.Database;
import com.blstream.urbangame.database.DatabaseInterface;
import com.blstream.urbangame.database.entity.ABCDTask;
import com.blstream.urbangame.database.entity.LocationTask;
import com.blstream.urbangame.database.entity.PlayerGameSpecific;
import com.blstream.urbangame.database.entity.Task;
import com.blstream.urbangame.database.entity.UrbanGame;
import com.blstream.urbangame.database.entity.UrbanGameShortInfo;
import com.blstream.urbangame.database.helper.Base64ImageCoder;
import com.blstream.urbangame.webserver.ServerResponseHandler;
import com.blstream.urbangame.webserver.WebResponse;
import com.blstream.urbangame.webserver.WebServer;
import com.blstream.urbangame.webserver.WebServerNotificationListener;

/**
 * This is a singleton notifier class. You can implement an observer interface
 * (NotificationListener) when there will appear any change in game or task the
 * observator's interface method will be called. If you don't want to see
 * notifications then comment 3 lines of code in MenuActivity (where
 * NotificationServer is used).
 * */

public class NotificationServer implements WebServerNotificationListener {
	private final static String TAG = NotificationServer.class.getSimpleName();
	
	private static NotificationServer instance;
	private final List<NotificationListener> observers;
	private final DatabaseInterface database;
	private final String playerEmail;
	
	private Context context;
	private long timeToNextQuery;
	private CountDownTimer queryCountDownTimer;
	private boolean noWebServerQuery;
	private AsyncNotificationQuery asyncNotificationQuery;
	private NotificationsManager notificationManager;
	private UrbanGameApplication urbanGameApplication;
	private final WebServer webServer;
	private final Handler mHandler;
	
	public static NotificationServer getInstance(Context context) {
		if (instance == null) {
			instance = new NotificationServer(context);
		}
		return instance;
	}
	
	private NotificationServer(Context context) {
		this.observers = new ArrayList<NotificationListener>();
		this.context = context;
		this.mHandler = new Handler();
		this.database = new Database(context);
		this.playerEmail = database.getLoggedPlayerID();
		this.webServer = new WebServer(context, new ServerResponseHandler(this));
		this.notificationManager = new NotificationsManager(context);
		
		// If you want to run a query to web server use
		// "setWebServerQuery()" instead of "setNoWebServerQuery()";
		// setWebServerQuery();
		setNoWebServerQuery();
		
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		if (sharedPrefs.getBoolean(context.getString(R.string.key_notifications_on), true)) {
			turnOnNotifications();
			Log.i(TAG, "notifications started");
		}
	}
	
	// FIXME this method is only used to prevent exception:
	// android.view.WindowManager$BadTokenException: Unable to add window 
	public void updateContext(Context context) {
		unregisterNotificationListener(notificationManager);
		this.context = context;
		notificationManager = new NotificationsManager(context);
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		if (sharedPrefs.getBoolean(context.getString(R.string.key_notifications_on), true)) {
			turnOnNotifications();
		}
	}
	
	public void turnOnNotifications() {
		registerNotificationListener(notificationManager);
	}
	
	public void turnOffNotifications() {
		for (NotificationListener notificationListener : observers) {
			unregisterNotificationListener(notificationListener);
		}
	}
	
	public synchronized void registerNotificationListener(NotificationListener notificationListener) {
		if (observers.isEmpty()) {
			startCallback();
		}
		
		if (!observers.contains(notificationListener)) {
			observers.add(notificationListener);
		}
	}
	
	public synchronized void unregisterNotificationListener(NotificationListener notificationListener) {
		observers.remove(notificationListener);
		if (observers.isEmpty()) {
			cancellCallback();
		}
	}
	
	private void startCallback() {
		queryCountDownTimer.start();
	}
	
	private void cancellCallback() {
		queryCountDownTimer.cancel();
	}
	
	private synchronized void notifyGameWon(final UrbanGame game) {
		setGameStatusHasChanged(game);
		
		if (urbanGameApplication != null && urbanGameApplication.isApplicationRunning()) {
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					for (NotificationListener notificationListener : observers) {
						notificationListener.onGameWon(game);
					}
				};
			});
		}
	}
	
	private synchronized void notifyGameLost(final UrbanGame game) {
		setGameStatusHasChanged(game);
		
		if (urbanGameApplication != null && urbanGameApplication.isApplicationRunning()) {
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					for (NotificationListener notificationListener : observers) {
						notificationListener.onGameLost(game);
					}
				};
			});
		}
	}
	
	private void setGameStatusHasChanged(UrbanGame game) {
		if (playerEmail != null) {
			boolean success = false;
			PlayerGameSpecific pgs = new PlayerGameSpecific(playerEmail, game.getID(), null, true);
			pgs.setState(PlayerGameSpecific.GAME_ENDED);
			database.updateUserGameSpecific(pgs);
			Log.i(TAG, "game status changed" + success);
		}
		else {
			Log.i(TAG, "game status changed player not logged");
		}
	}
	
	private synchronized void notifyGameChanged(final UrbanGame oldGame, final UrbanGame newGame) {
		setGameHasChanges(newGame);
		
		if (urbanGameApplication != null && urbanGameApplication.isApplicationRunning()) {
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					for (NotificationListener notificationListener : observers) {
						notificationListener.onGameChanged(oldGame, newGame);
					}
				};
			});
		}
	}
	
	private void setGameHasChanges(UrbanGame newGame) {
		boolean success;
		if (database.getGameInfo(newGame.getID()) == null) {
			success = database.insertGameInfo(newGame);
			Log.i(TAG, "game inserted into database " + success);
		}
		else {
			success = database.updateGame(newGame);
			Log.i(TAG, "game updated in database " + success);
		}
	}
	
	private synchronized void notifyTaskNew(final UrbanGame game, final Task task) {
		setTaskHasChanges(game, task);
		
		if (urbanGameApplication != null && urbanGameApplication.isApplicationRunning()) {
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					for (NotificationListener notificationListener : observers) {
						notificationListener.onTaskNew(game, task);
					}
				};
			});
		}
	}
	
	private synchronized void notifyTaskChanged(final UrbanGame game, final Task oldTask, final Task newTask) {
		setTaskHasChanges(game, newTask);
		
		if (urbanGameApplication != null && urbanGameApplication.isApplicationRunning()) {
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					for (NotificationListener notificationListener : observers) {
						notificationListener.onTaskChanged(game, oldTask, newTask);
					}
				};
			});
		}
	}
	
	private void setTaskHasChanges(UrbanGame game, Task task) {
		boolean success;
		if (database.getTask(task.getId()) == null) {
			success = database.insertTaskForGame(game.getID(), task);
			Log.i(TAG, "task inserted into database " + success);
		}
		else {
			success = database.updateTask(task);
			Log.i(TAG, "task updated in database " + success);
		}
	}
	
	public void setApplication(UrbanGameApplication urbanGameApplication) {
		this.urbanGameApplication = urbanGameApplication;
	}
	
	private void initQueryCountDownTimer(long time) {
		queryCountDownTimer = new CountDownTimer(time, time) {
			@Override
			public void onTick(long millisUntilFinished) {}
			
			@Override
			public void onFinish() {
				asyncNotificationQuery = new AsyncNotificationQuery();
				asyncNotificationQuery.execute();
			}
		};
	}
	
	private class AsyncNotificationQuery extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			if (noWebServerQuery) {
				runNoWebServerQuery();
				Log.i(TAG, "no web server query started");
			}
			else {
				webServer.getAllGames();
				Log.i(TAG, "web server query started");
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			asyncNotificationQuery = new AsyncNotificationQuery();
			if (urbanGameApplication != null && urbanGameApplication.isApplicationRunning()) {
				queryCountDownTimer.start();
			}
			else {
				Log.i(TAG, "asyncNotificationQuery quit");
			}
		}
	}
	
	//**********************//
	//						//
	//		M O C K	  		//
	//						//
	//**********************//	
	
	private void runNoWebServerQuery() {
		UrbanGameShortInfo game = null;
		Task task = null;
		Random r = new Random();
		
		List<UrbanGameShortInfo> gameList = database.getAllGamesShortInfo();
		if (gameList != null) {
			game = gameList.get(r.nextInt(gameList.size()));
			
			List<Task> taskList = database.getTasksForGame(game.getID());
			if (taskList != null) {
				task = taskList.get(r.nextInt(taskList.size()));
			}
		}
		
		if (r.nextBoolean() || game == null) { //game
		
			if (r.nextBoolean()) { //simulate that new game is available
				Log.i(TAG, "Mock simulate new game available");
				UrbanGame newGame = mockSimulateNewGameAvailable();
				notifyGameChanged(newGame, newGame);
			}
			else { //update game
				if (game != null) {
					if (r.nextBoolean()) { // simulate that game is over
					
						Log.i(TAG, "Mock simulate game is over");
						if (r.nextBoolean()) {
							notifyGameWon(database.getGameInfo(game.getID()));
						}
						else {
							notifyGameLost(database.getGameInfo(game.getID()));
						}
						
					}
					else // simulate that game content changed
					{
						Log.i(TAG, "Mock simulate game changed");
						UrbanGame urbanGame = database.getGameInfo(game.getID());
						UrbanGame updatedGame = updateGameDataContent(r, urbanGame);
						notifyGameChanged(urbanGame, updatedGame);
					}
				}
			}
		}
		else //task
		{
			if (r.nextBoolean() || task == null) { //simulate that new task is available
				Log.i(TAG, "Mock simulate new task available");
				Task newTask = null;
				
				if (r.nextBoolean()) {
					newTask = mockSimulateNewTaskAvailable(Task.TASK_TYPE_ABCD);
				}
				else {
					newTask = mockSimulateNewTaskAvailable(Task.TASK_TYPE_LOCATION);
				}
				
				notifyTaskNew(database.getGameInfo(game.getID()), newTask);
			}
			else // simulate that task content changed	
			{
				Log.i(TAG, "Mock simulate task changed");
				Task newTask = updateTaskDataContent(r, task);
				notifyTaskChanged(database.getGameInfo(game.getID()), task, newTask);
			}
			
		}
		
	}
	
	protected Task updateTaskDataContent(Random r, Task task) {
		Task newTask;
		
		if (task.getType() == Task.TASK_TYPE_ABCD) {
			newTask = new ABCDTask();
		}
		else {
			newTask = new LocationTask();
		}
		newTask.setId(task.getId());
		newTask.setType(task.getType());
		
		switch (task.getType()) {
			case Task.TASK_TYPE_ABCD:
				ABCDTask abcdTask = (ABCDTask) newTask;
				if (r.nextBoolean()) {
					abcdTask.setQuestion(((ABCDTask) task).getQuestion() + " NOTIFICATION");
				}
				if (r.nextBoolean()) {
					abcdTask.setAnswers(new String[] { "MockA", "MockB", "MockC", "MockD" });
				}
			default:
				if (r.nextBoolean()) {
					newTask.setTitle(task.getTitle() + " NOTIFICATION");
				}
				if (r.nextBoolean()) {
					newTask.setDescription(task.getDescription() + " NOTIFICATION");
				}
				if (r.nextBoolean()) {
					newTask.setIsRepetable(task.isRepetable() ? false : true);
				}
				if (r.nextBoolean()) {
					newTask.setIsHidden(task.isHidden() ? false : true);
				}
				if (r.nextBoolean()) {
					newTask.setNumberOfHidden(r.nextInt(15));
				}
				break;
		}
		
		return newTask;
	}
	
	protected UrbanGame updateGameDataContent(Random r, UrbanGame oldGame) {
		UrbanGame game = new UrbanGame(oldGame.getID(), oldGame.getGameVersion(), oldGame.getTitle(),
			oldGame.getOperatorName(), oldGame.getWinningStrategy(), oldGame.getPlayers(), oldGame.getMaxPlayers(),
			oldGame.getStartDate(), oldGame.getEndDate(), oldGame.getDifficulty(), oldGame.getReward(),
			oldGame.getPrizesInfo(), oldGame.getDescription(), oldGame.getGameLogoBase64(),
			oldGame.getOperatorLogoBase64(), oldGame.getComments(), oldGame.getLocation(), oldGame.getDetailsLink());
		
		game.setMaxPlayers(game.getPlayers() + r.nextInt(20));
		
		if (r.nextBoolean()) {
			game.setGameVersion(r.nextDouble());
		}
		if (r.nextBoolean()) {
			game.setTitle(game.getTitle() + " NOTIFICATION");
		}
		if (r.nextBoolean()) {
			game.setOperatorName(game.getOperatorName() + " NOTIFICATION");
		}
		if (r.nextBoolean()) {
			game.setWinningStrategy(game.getWinningStrategy() + " NOTIFICATION");
		}
		if (r.nextBoolean()) {
			game.setDifficulty(r.nextInt(5));
		}
		if (r.nextBoolean()) {
			game.setReward(game.getReward() ? false : true);
		}
		if (r.nextBoolean()) {
			game.setPrizesInfo(game.getPrizesInfo() + " NOTIFICATION");
		}
		if (r.nextBoolean()) {
			game.setDescription(game.getDescription() + " NOTIFICATION");
		}
		if (r.nextBoolean()) {
			game.setComments(game.getComments() + " NOTIFICATION");
		}
		if (r.nextBoolean()) {
			game.setLocation(game.getLocation() + " NOTIFICATION");
		}
		
		return game;
	}
	
	public Task mockSimulateNewTaskAvailable(int taskType) {
		// Method returns new object of class "Task" that is not stored in Database. 
		// The first parameter is the type of Task that you want to get as return. 
		// If it wasn't possible to create a new Task null is returned.
		
		long taskId = 0;
		long i;
		
		// Look for task ID that is not stored in Database
		for (i = 0; i < Long.MAX_VALUE; ++i) {
			if (database.getTask(i) == null) {
				taskId = i;
				break;
			}
		}
		
		// If all TIDs are used null is returned
		if (i == Long.MAX_VALUE) {
			Log.e(TAG, "mockSimulateNewTaskAvailable couldn't create new task");
			return null;
		}
		
		Task task = null;
		if (taskType == Task.TASK_TYPE_ABCD) {
			task = new ABCDTask(Long.valueOf(taskId), "ABCDTaskTitle" + taskId, "ABCDTaskImage" + taskId,
				"ABCDTaskDescription" + taskId, true, true, 1, new Date(), 1, "ABCDTaskQuestion" + taskId,
				new String[] { "A" + taskId, "B" + taskId, "C" + taskId, "D" + taskId });
		}
		else {
			task = new LocationTask(Long.valueOf(taskId), "LocationTaskTitle" + taskId, "LocationTaskImage" + taskId,
				"LocationTaskDescription" + taskId, true, true, 1, new Date(), 1);
		}
		
		return task;
	}
	
	public UrbanGame mockSimulateNewGameAvailable() {
		// Method returns new object of class "UrbanGame" that is not stored in Database. 
		// If it wasn't possible to create a new UrbanGame null is returned.
		
		long gameId = 0;
		long i;
		
		// Look for game ID that is not stored in Database
		for (i = 0; i < Long.MAX_VALUE; ++i) {
			if (database.getGameInfo(i) == null) {
				gameId = i;
				break;
			}
		}
		
		// If all GIDs are used null is returned
		if (i == Long.MAX_VALUE) {
			Log.e(TAG, "mockSimulateNewTaskAvailable couldn't create new task");
			return null;
		}
		
		SimpleDateFormat curFormater = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ENGLISH);
		Date startDate = null;
		Date endDate = null;
		try {
			startDate = curFormater.parse("04/05/2013 08:40");
			endDate = curFormater.parse("04/06/2013 13:40");
		}
		catch (ParseException e) {
			Log.e(TAG, "ParseException " + e.toString());
		}
		
		return new UrbanGame(Long.valueOf(gameId), Double.valueOf(i), "MyGameTitle" + i, "MyOperatorName" + i,
			"MyWinningStrategy" + i, 10, 15, startDate, endDate, 5, true, "MyPrizesInfo" + i, "MyDescription" + i,
			Base64ImageCoder.convertImage(context.getResources().getDrawable(R.drawable.ic_launcher_big)),
			Base64ImageCoder.convertImage(context.getResources().getDrawable(R.drawable.mock_logo_operator)),
			"MyComments" + i, "MyLocation" + i, "MyDetaisLink" + i);
	}
	
	protected void setNoWebServerQuery() {
		noWebServerQuery = true;
		timeToNextQuery = 10 * 1000; // 10 seconds
		initQueryCountDownTimer(timeToNextQuery);
	}
	
	//**********************//
	//						//
	//		M O C K	  END	//
	//						//
	//**********************//
	
	protected void setWebServerQuery() {
		noWebServerQuery = false;
		timeToNextQuery = 3 * 60 * 1000; // 3 minutes
		initQueryCountDownTimer(timeToNextQuery);
	}
	
	public void executeTestWebServerQuery() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				
				Looper.prepare();
				new Handler().post(new Runnable() {
					@Override
					public void run() {
						webServer.getAllGames();
					}
				});
				Looper.loop();
			}
		}).start();
	}
	
	@Override
	public void onWebServerResponse(Message message) {
		mockSimulateNewGameAvailable();
		WebResponse webResponse = (WebResponse) message.obj;
		
		switch (webResponse.queryType) {
			case DownloadGamesList:
				break;
			default:
				break;
		}
	}
}