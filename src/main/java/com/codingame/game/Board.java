package com.codingame.game;

import java.util.*;

import static com.codingame.game.Constants.*;

/**
 * Class for storing all information about the board. Including barriers and movement rules.
 */
public class Board {
    private final int h;
    private final int w;
    private final int[][] board;

    /**
     * Create an empty board. Height and width are of the matrix sizes. This is h*2-1 and w*2-1, which allows for rows
     * and columns to be used for vertical and horizontal barriers. Odd index columns are used solely for vertical
     * barriers and odd index rows are used for horizontal barriers.
     *
     * @param h Height of the board.
     * @param w Width of the board.
     */
    public Board(int h, int w) {
        board = new int[h][w];
        this.h = h;
        this.w = w;
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                board[i][j] = EMPTY;
            }
        }
    }

    /**
     * Function to determine if a provided coordinate for a player tile is inBounds of the matrix.
     * @param i Row index of the coordinate.
     * @param j Column index of the coordinate.
     * @return boolean - True if within the board, otherwise false.
     */
    public boolean inBoundsPlayer(int i, int j){
        return 0 <= i && i < h && 0 <= j && j < w;
    }


    /**
     * Function to determine if a barrier can be placed on the grid or not.
     * @param i Row index of the first barrier.
     * @param j Column index of the first barrier.
     * @return boolean - true if the barrier is within the board, otherwise false.
     */
    public boolean inBoundsBarrier(int i, int j, Orientation orientation){
        if (orientation.name().equals("VERT")) {
            i = i*2;
            j = j*2+1;
            return (0 <= i && i < h && 0 <= j && j < w) && (0 <= i+2 && i+2 < h);
        } else {
            i = 2*i + 1;
            j = 2*j;
            return (0 <= i && i < h && 0 <= j && j < w) && (0 <= j+2 && j+2 < w);
        }
    }


    /**
     * This function will place a player on the board at the given coordinates. Note: These are matrix coordinates.
     *
     * @param coordinate Coordinate of the starting player.
     * @param playerId The id of the player to be added.
     */
    public void setPlayer(Coordinate coordinate, int playerId) {
        board[coordinate.getY()][coordinate.getX()] = playerId;
    }


    /**
     * Function to check if a barrier is allowed to be placed in a given location by checking it's empty.
     * A barrier coordinates are provided in grid coordinates, not matrix.
     *
     * @param y Y-coordinate of where the barrier starts.
     * @param x X-coordinate of where the barrier starts.
     * @param direction String containing whether the barrier is a vertical or horizontal barrier.
     * @return boolean - true if the barrier is valid otherwise false.
     */
    public boolean isValidBarrier(int y, int x, String direction){
        if (Objects.equals(direction, "VERT")) {
            int wy = 2 * y;
            int wx = 2 * x + 1;
            return board[wy][wx] == EMPTY && board[wy + 1][wx] == EMPTY && board[wy + 2][wx] == EMPTY;
        } else {
            int wr = 2 * y + 1;
            int wc = 2 * x;
            return board[wr][wc] == EMPTY && board[wr][wc + 1] == EMPTY && board[wr][wc + 2] == EMPTY;
        }
    }


    /**
     * Function to add a barrier to the board.
     *
     * @param y - Y-coordinate of the top left of the barrier.
     * @param x - X-coordinate of the top left of the barrier.
     * @param direction - String of the direction, either VERT or HOR.
     */
    public void addBarrier(int y, int x, String direction) {
        if (direction.equals("VERT")) {
            int wy = 2 * y;
            int wx = 2 * x + 1;
            board[wy][wx] = WALL;
            board[wy + 1][wx] = WALL;
            board[wy + 2][wx] = WALL;
        } else {
            int wy = 2 * y + 1;
            int wx = 2 * x;
            board[wy][wx] = WALL;
            board[wy][wx + 1] = WALL;
            board[wy][wx + 2] = WALL;
        }
    }


    /**
     * Validates whether ALL players can reach their respective goal zone.
     *
     * @param players List of active players in the game.
     * @return true if every player has at least one valid path to their goal row, otherwise false.
     */
    public boolean canReachEnd(List<Player> players) {
        for (Player player : players) {
            if (!hasPathToGoal(player)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Validates if a single player can reach their goal zone. Perform BFS from starting location until goal zone.
     * Simple orthogonal directions movement only as it simulates, jumps and horizontal movements.
     *
     * @param player [Player] to check if there
     */
    private boolean hasPathToGoal(Player player) {
        int startY = player.getPosition().getY();
        int startX = player.getPosition().getX();
        int goalY = player.goalY;

        if (startY == goalY) {
            return true;
        }

        boolean[][] visited = new boolean[board.length][board[0].length];
        Queue<int[]> queue = new ArrayDeque<>();

        queue.add(new int[]{startY, startX});
        visited[startY][startX] = true;

        Constants.Direction[] directions = {
                Constants.Direction.UP,
                Constants.Direction.DOWN,
                Constants.Direction.LEFT,
                Constants.Direction.RIGHT
        };

        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int curY = current[0];
            int curX = current[1];

            for (Constants.Direction dir : directions) {
                int rowStep = dir.getRowOffset();
                int colStep = dir.getColOffset();

                int wallY = curY + (rowStep / 2);
                int wallX = curX + (colStep / 2);

                int nextY = curY + rowStep;
                int nextX = curX + colStep;

                if (nextY >= 0 && nextY < board.length && nextX >= 0 && nextX < board[0].length) {
                    if (board[wallY][wallX] == EMPTY && !visited[nextY][nextX]) {
                        if (nextY == goalY) {
                            return true;
                        }
                        visited[nextY][nextX] = true;
                        queue.add(new int[]{nextY, nextX});
                    }
                }
            }
        }
        return false;
    }


    /**
     * Determine if a player can legally make an orthogonal movement (U, D, L, R).
     *
     * @param player Player intending to make the move.
     * @param direction Direction that the player wishes to move.
     * @return boolean - True if the move is valid, otherwise false.
     */
    public CanMove canMoveOrthogonal(Player player, Constants.Direction direction) {
        CanMove invalidMoveResponse = new CanMove(false, null);
        if (!direction.isOrthogonal()){
            return invalidMoveResponse;
        }

        // Get current position and the direction of travel.
        int curY = player.getPosition().getY();
        int curX = player.getPosition().getX();
        int rowStep = direction.getRowOffset();
        int colStep = direction.getColOffset();

        // Get the position of the wall between current tile and destination tile.
        int wallY = curY + (rowStep / 2);
        int wallX = curX + (colStep / 2);

        // Destination tile space position
        int targetY = curY + rowStep;
        int targetX = curX + colStep;

        // Check that the destination tile is within the board.
        if (targetY < 0 || targetY >= board.length || targetX < 0 || targetX >= board[0].length) {
            return invalidMoveResponse;
        }

        // Check if a wall is blocking the direct path to the destination tile.
        if (board[wallY][wallX] != EMPTY) {
            return invalidMoveResponse;
        }

        // Ensure the destination tile is empty. If so, allow the player to move into it.
        if (board[targetY][targetX] == EMPTY) {
            return new CanMove(true, new Coordinate(targetY, targetX));
        }

        return invalidMoveResponse;
    }


    /**
     * Determine if a player can move via "jumping" over the opponent. This is allowed if the opponent is adjacent
     * to the player and there is no barrier: between the player and opponent, not a barrier or edge of board BEHIND
     * the opponent in the intended direction.
     * @param player Player that is making the move.
     * @param direction Direction of intended movement.
     * @return [CanMove] object containing boolean if the move is possible. If it is possible, also contains the
     * coordinates of the destination.
     */
    public CanMove canMoveJump(Player player, Direction direction){
        CanMove invalidMoveResponse = new CanMove(false, null);
        if (!direction.isJump()){
            return invalidMoveResponse;
        }

        // Get current position and the direction of travel.
        int curY = player.getPosition().getY();
        int curX = player.getPosition().getX();
        int rowStep = direction.getRowOffset()/2;
        int colStep = direction.getColOffset()/2;

        // Get the position of the wall between current tile and adjacent tile.
        int wallY = curY + (rowStep / 2);
        int wallX = curX + (colStep / 2);

        // Adjacent tile space position
        int targetY = curY + rowStep;
        int targetX = curX + colStep;

        // Check that the adjacent tile is within the board.
        if (targetY < 0 || targetY >= board.length || targetX < 0 || targetX >= board[0].length) {
            return invalidMoveResponse;
        }

        // Check if a wall is blocking the direct path to the adjacent tile.
        if (board[wallY][wallX] != EMPTY) {
            return invalidMoveResponse;
        }

        // Ensure the adjacent tile is not empty. If so, don't allow the player to move as it violates jump rule.
        if (board[targetY][targetX] == EMPTY) {
            return invalidMoveResponse;
        }

        // Destination tile location.
        int destinationTileY = targetY + rowStep;
        int destinationTileX = targetX + colStep;

        // Check jump landing tile boundaries are within the grid.
        if (destinationTileY < 0 || destinationTileY >= board.length || destinationTileX < 0 || destinationTileX >= board[0].length) {
            return invalidMoveResponse;
        }

        // Get the position of the wall behind the opponent.
        int wallBehindOppY = targetY + (rowStep / 2);
        int wallBehindOppX = targetX + (colStep / 2);

        // If there wall behind the opponent, don't allow a jump.
        if (board[wallBehindOppY][wallBehindOppX] != EMPTY) {
            return invalidMoveResponse;
        }

        // Ensure that the destination tile doesn't contain an enemy.
        if (board[destinationTileY][destinationTileX] == EMPTY) {
            return new CanMove(true, new Coordinate(destinationTileY, destinationTileX));
        }
        return invalidMoveResponse;

    }

    /**
     * Function to determine whether a player can perform a diagonal jump in the specified diagonal direction.
     *
     * @param player Player that is making the move.
     * @param direction Direction of intended movement.
     * @return [CanMove] object containing boolean if the move is possible. If it is possible, also contains the
     * coordinates of the destination.
     */
    public CanMove canMoveDiagonal(Player player, Direction direction) {
        CanMove invalidMoveResponse = new CanMove(false, null);

        if (!direction.isDiagonal()) {
            return invalidMoveResponse;
        }

        // Get current position and the direction of travel.
        int curY = player.getPosition().getY();
        int curX = player.getPosition().getX();

        // Destination tile location (final diagonal landing tile).
        int targetY = curY + direction.getRowOffset();
        int targetX = curX + direction.getColOffset();

        // Check that the destination tile is within the board.
        if (targetY < 0 || targetY >= board.length || targetX < 0 || targetX >= board[0].length) {
            return invalidMoveResponse;
        }

        // Break diagonal direction into vector components (primary = toward opponent, secondary = sideways).
        Direction primary = (direction.getRowOffset() < 0) ? Direction.UP : Direction.DOWN;
        Direction secondary = (direction.getColOffset() < 0) ? Direction.LEFT : Direction.RIGHT;
        int primaryY = curY + primary.getRowOffset();
        int primaryX = curX;
        if (primaryY < 0 || primaryY >= board.length || board[primaryY][primaryX] == EMPTY) {
            Direction tmp = secondary;
            secondary = primary;
            primary = tmp;
        }

        // Ensure the adjacent tile in the primary direction is not empty. If so, don't allow the player to move as it violates diagonal rule.
        if (primary.isVertical() && board[curY+primary.getRowOffset()][curX] == EMPTY ||
                primary.isHorizontal() && board[curY][curX+primary.getColOffset()] == EMPTY){
            return invalidMoveResponse;
        }

        // Check if a wall is blocking the direct path to the adjacent tile or if there is no wall or edge of grid behind the opponent (Which is critical rule for diagonal move)
        if (primary.isVertical()){
            int wallBehindOppY = curY + primary.getRowOffset() + primary.getRowOffset()/2;
            if (board[curY + primary.getRowOffset()/2][curX] != EMPTY || ((wallBehindOppY>=0 && wallBehindOppY<board.length) && board[wallBehindOppY][curX] == EMPTY)) {
                return invalidMoveResponse;
            }
        } else {
            int wallBehindOppX = curX + primary.getColOffset() + primary.getColOffset()/2;
            if (board[curY][curX + primary.getColOffset()/2] != EMPTY || ((wallBehindOppX>=0 && wallBehindOppX<board[0].length) && board[curY][wallBehindOppX] == EMPTY)){
                return invalidMoveResponse;
            }
        }

        // Move in the primary direction.
        curY += primary.getRowOffset();
        curX += primary.getColOffset();

        // Check that no barrier is blocking movement in the secondary direction.
        if (secondary.isVertical()){
            if (board[curY + secondary.getRowOffset()/2][curX] != EMPTY) {
                return invalidMoveResponse;
            }
        } else {
            if (board[curY][curX + secondary.getColOffset()/2] != EMPTY) {
                return invalidMoveResponse;
            }
        }

        // Move to the destination tile.
        curY += secondary.getRowOffset();
        curX += secondary.getColOffset();

        return new CanMove(true, new Coordinate(curY, curX));
    }


    /**
     * Move a player to a new position on the board.
     *
     * @param player The player that is moving.
     * @param nextPosition Coordinates of the players next position.
     */
    public void move(Player player, Coordinate nextPosition) {
        board[player.position.getY()][player.position.getX()] = EMPTY;
        player.setPosition(nextPosition);
        board[nextPosition.getY()][nextPosition.getX()] = player.getPlayerId();
    }


    /**
     * Class to store information regarding a movement.
     *
     * canMove - Boolean if the move is possible or not.
     * nextPosition - Coordinate of the next position, if the move is valid.
     */
    public class CanMove {

        private boolean canMove;
        private Coordinate nextPosition;

        CanMove(boolean canMove, Coordinate nextPosition) {
            this.canMove = canMove;
            this.nextPosition = nextPosition;
        }

        public boolean isCanMove() {
            return canMove;
        }

        public Coordinate getNextPosition() {
            return nextPosition;
        }
    }
}