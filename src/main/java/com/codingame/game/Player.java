package com.codingame.game;
import com.codingame.gameengine.core.AbstractMultiplayerPlayer;

public class Player extends AbstractMultiplayerPlayer {

    Coordinate position;
    private int barriersRemaining = Constants.TOTAL_BARRIERS;
    int playerId;
    int goalY = 0;

    public void setPosition(Coordinate coords){
        position = coords;
    }

    public void setPlayerId(int player_id) {
        this.playerId = player_id;
    }

    public Coordinate getPosition() {
        return position;
    }

    public Coordinate getGridPosition(){
        return new Coordinate((1+position.getY())/2, (1+position.getX())/2);
    }

    public int getBarriersRemaining() {
        return barriersRemaining;
    }

    public int getPlayerId() {
        return playerId;
    }

    public void setBarriersRemaining(int barriersRemaining) {
        this.barriersRemaining = barriersRemaining;
    }
    public void removeBarrier(){barriersRemaining-=1;}
    public void setGoalY(int goalY) {
        this.goalY = goalY;
    }

    @Override
    public int getExpectedOutputLines() {
        // Returns the number of expected lines of outputs for a player
        return 1;
    }
}
