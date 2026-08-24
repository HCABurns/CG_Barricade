package com.codingame.game;

import com.codingame.gameengine.module.entities.World;

import java.util.Arrays;
import java.util.List;

public class Constants {

    // View dimensions
    public static final int VIEWER_WIDTH = World.DEFAULT_WIDTH;
    public static final int VIEWER_HEIGHT = World.DEFAULT_HEIGHT;

    // Logical board sizes
    public static final int GRID_SIZE = 9;          // Standard 9x9 tiles
    public static final int MATRIX_SIZE = GRID_SIZE*2-1;       // 17x17 interleaved grid

    // Board values
    public static final int EMPTY = -1;
    public static final int WALL = -2;

    // Assets & rules
    public static final String BACKGROUND_SPRITE = "bg.png";
    public static final int TOTAL_BARRIERS = 10; // Standard Quoridor gives 10 per player (or 8 for 4-player)

    public static final int CELL_SIZE = 256;
    public static final int BARRIER_SIZE = 64;
    public static final String TILE_SPRITE = "tile.png";
    public static final String START_RED_TILE_SPRITE = "start_red.png";
    public static final String START_BLUE_TILE_SPRITE = "start_blue.png";
    public static final String BARRIER_HOR_SPRITE = "barrierHor.png";
    public static final String BARRIER_VERT_SPRITE = "barrierVert.png";
    public static final String ACTUAL_BARRIER_VERT_SPRITE = "actualBarrierVert.png";
    public static final String ACTUAL_BARRIER_HOR_SPRITE = "actualBarrierHor.png";
    public static final String ACTUAL_BARRIER_MID_SPRITE = "actualBarrierMid.png";


    // Directions move 2 units in the 17x17 grid
    public enum Direction {
        UP(-2, 0),
        UP_JUMP(-4, 0),
        DOWN(2, 0),
        DOWN_JUMP(4, 0),
        LEFT(0, -2),
        LEFT_JUMP(0, -4),
        RIGHT(0, 2),
        RIGHT_JUMP(0, 4),
        // Diagonal jump moves
        UP_LEFT(-2, -2),
        UP_RIGHT(-2, 2),
        DOWN_LEFT(2, -2),
        DOWN_RIGHT(2, 2);

        private final int rowOffset;
        private final int colOffset;

        private static final java.util.Map<String, Direction> LOOKUP = new java.util.HashMap<>();
        static {
            for (Direction d : values()) {
                LOOKUP.put(key(d.rowOffset, d.colOffset), d);
            }
        }
        private static String key(int r, int c) { return r + "," + c; }
        public static Direction fromOffsets(int rowOffset, int colOffset) { return LOOKUP.get(key(rowOffset, colOffset)); }

        Direction(int rowOffset, int colOffset) {
            this.rowOffset = rowOffset;
            this.colOffset = colOffset;
        }
        public boolean isDiagonal() {return Math.abs(rowOffset) == 2 && Math.abs(colOffset) == 2;}
        public boolean isVertical() {return Math.abs(rowOffset) == 2;}
        public boolean isHorizontal() {return Math.abs(rowOffset) == 0;}
        public boolean isOrthogonal() {return Math.abs(rowOffset) + Math.abs(colOffset) == 2;}
        public boolean isJump() {return Math.abs(rowOffset) == 4 || Math.abs(colOffset) == 4;}
        public int getRowOffset() { return rowOffset; }
        public int getColOffset() { return colOffset; }
        public static List<Direction> getOrthogonal(){return Arrays.asList(UP, DOWN, LEFT, RIGHT);}
        public static List<Direction> getJump(){return Arrays.asList(UP_JUMP, DOWN_JUMP, LEFT_JUMP, RIGHT_JUMP);}
        public static List<Direction> getDiagonal(){return Arrays.asList(UP_LEFT, UP_RIGHT, DOWN_LEFT, DOWN_RIGHT);}
    }

    public enum Orientation {
        HOR,
        VERT
    }

    public enum ActionType {
        MOVE,
        BARRIER
    }

    /**
     * Class for holding information about each player's outputs.
     */
    public static class Action {
        public ActionType type;
        public Direction direction;
        public Orientation orientation;
        public int wallX;
        public int wallY;

        public Action(Direction dir) {
            this.type = ActionType.MOVE;
            this.direction = dir;
        }

        public Action(Orientation orientation, int x, int y) {
            this.type = ActionType.BARRIER;
            this.orientation = orientation;
            this.wallX = x;
            this.wallY = y;
        }

        @Override
        public String toString() {
            if (type == ActionType.MOVE) {
                return "MOVE " + direction.name();
            } else {
                return "BARRIER " + orientation.name() + " " + wallX + " " + wallY;
            }
        }
    }
}