package com.floern.rhabarber.network;

public class GameDescription {
	private String gameName;
	private String mapName;
	private int playerCount;

	private static final String DELIMITER = "$";

	public GameDescription(String gameName, String mapName, int playerCount) {
		super();
		this.gameName = gameName;
		this.mapName = mapName;
		this.playerCount = playerCount;
	}

	/**
	 * Creates a default description.
	 */
	public GameDescription() {
		this("", "Default Map", 0);
	}

	public GameDescription(String name, String mapName) {
		this(name, mapName, 0);
	}

	public GameDescription(String stringRepresentation) {
		int index = stringRepresentation.lastIndexOf(DELIMITER);
		if (index == -1) {
			this.gameName = stringRepresentation;
			this.mapName = "";
			this.playerCount = 0;
		} else {
			this.gameName = stringRepresentation.substring(0, index);
			this.mapName = stringRepresentation.substring(index
					+ DELIMITER.length());
			this.playerCount = 0;
		}
	}

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

	/**
	 * Since the channel is advertised via a single string, this method builds a
	 * String with all the information encoded that is needed to advertise game.
	 * Note that whitespaces lead to problems, therefore they are removed.
	 * 
	 * @return
	 */
	public String getStringRepresentation() {
//		return gameName.replace(' ', '_') + DELIMITER
//				+ mapName.replace(' ', '_');
		return this.gameName;
	}
}
