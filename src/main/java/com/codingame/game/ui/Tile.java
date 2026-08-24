package com.codingame.game.ui;

import com.codingame.game.Coordinate;
import com.codingame.gameengine.module.entities.Sprite;

public class Tile {

    private Sprite sprite;
    private String tileType;
    private int playerId;

    Tile(Sprite sprite, String tileType){
        this.tileType = tileType;
        this.sprite = sprite;
    }

    Tile(Sprite sprite, int playerId){
        this.sprite = sprite;
        this.playerId = playerId;
    }

    public Sprite getSprite() {
        return sprite;
    }

    public void setSprite(Sprite sprite) {
        this.sprite = sprite;
    }

    public String getTileType() {
        return tileType;
    }

    public void setTileType(String tileType) {
        this.tileType = tileType;
    }
}
