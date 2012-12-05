package com.floern.rhabarber.network;

public class GameDescription {
	private String gameName;
	private String mapName;
	public GameDescription(String gameName, String mapName, int playerCount) {
		super();
		this.gameName = gameName;
		this.mapName = mapName;
		this.playerCount = playerCount;
	}

	private int playerCount;

	public String getGameName() {
		return gameName;
	}

	public void setGameName(String gameName) {
		this.gameName = gameName;
	}

	public String getMapName() {
		return mapName;
	}

	public void setMapName(String mapName) {
		this.mapName = mapName;
	}

	public int getPlayerCount() {
		return playerCount;
	}

	public void setPlayerCount(int playerCount) {
		this.playerCount = playerCount;
	}

}
