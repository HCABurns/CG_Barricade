package com.codingame.game;

import java.util.Objects;

/**
 * Class for storing information about a coordinate.
 */
public class Coordinate {

    private int y;
    private int x;
    private final int playerId; // Used for the moves UI.

    public Coordinate(int y,int x){
        this.y = y;
        this.x = x;
        this.playerId = -1;
    }

    public Coordinate(int y,int x, int playerId){
        this.y = y;
        this.x = x;
        this.playerId = playerId;
    }

    public int getY() {
        return y;
    }

    public int getX() {
        return x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setX(int x) {
        this.x = x;
    }

    /**
     * Custom checker for comparing coordinates based on position ONLY.
     * @param o - Coordinate to compare to.
     * @return Boolean - True if the same position otherwise False.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Coordinate that = (Coordinate) o;
        return y == that.y &&
                x == that.x &&
                playerId == that.playerId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(y, x, playerId);
    }

    @Override
    public String toString() {
        return "Coordinate{" +
                "y=" + y +
                ", x=" + x +
                '}';
    }
}
