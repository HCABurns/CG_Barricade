package com.codingame.game;

import java.util.List;

import com.codingame.game.ui.Viewer;
import com.codingame.gameengine.core.AbstractPlayer.TimeoutException;
import com.codingame.gameengine.core.AbstractReferee;
import com.codingame.gameengine.core.MultiplayerGameManager;
import com.codingame.gameengine.module.entities.GraphicEntityModule;
import com.codingame.gameengine.module.tooltip.TooltipModule;
import com.codingame.gameengine.module.viewport.ViewportModule;
import com.codingame.gameengine.module.toggle.ToggleModule;
import com.codingame.game.Constants.*;
import com.google.inject.Inject;

public class Referee extends AbstractReferee {
    @Inject
    private MultiplayerGameManager<Player> gameManager;
    @Inject
    private GraphicEntityModule graphicEntityModule;
    @Inject
    private ViewportModule viewportModule;
    @Inject
    private TooltipModule tooltips;
    @Inject private ToggleModule toggleModule;

    private Board board;
    public static String errorMessage;
    private final Coordinate[] startingPosition = {new Coordinate(0, Constants.GRID_SIZE - 1), new Coordinate(Constants.GRID_SIZE * 2 - 1 - 1, Constants.GRID_SIZE - 1)};
    private String lastMove = "NONE";
    private Viewer viewer;

    /**
     * Function to set up the board and visuals.
     */
    @Override
    public void init() {
        // Set frame duration.
        gameManager.setFrameDuration(500);
        gameManager.setFirstTurnMaxTime(1000);
        gameManager.setMaxTurns(200);
        gameManager.setTurnMaxTime(150);

        // Initialize the game board.
        board = new Board(Constants.MATRIX_SIZE, Constants.MATRIX_SIZE);

        // Set up the starting positions.
        int playerId = 0;
        for (Player player : gameManager.getActivePlayers()) {
            player.setPosition(startingPosition[playerId]);
            player.setPlayerId(playerId);
            player.setGoalY(startingPosition[(playerId + 1) % 2].getY());
            board.setPlayer(startingPosition[playerId], player.getIndex());

            //Send each user the required information.
            player.sendInputLine(Constants.GRID_SIZE + " " + Constants.GRID_SIZE); // Grid Size H - W
            player.sendInputLine(startingPosition[playerId].getX()/2 + " " + startingPosition[playerId].getY()/2); // Their position (x y)
            player.sendInputLine(startingPosition[(playerId+1) % 2].getX()/2 + " " + startingPosition[(playerId+1) % 2].getY()/2); // Opponent position (x y)

            playerId += 1;
        }

        // Set up the board UI.
        viewer = new Viewer(graphicEntityModule, tooltips, toggleModule);
        for (int i = 0; i < Constants.GRID_SIZE; i++){
            for (int j = 0; j < Constants.GRID_SIZE; j++){
                viewer.drawTile(i, j);
                if (j != Constants.GRID_SIZE-1) {
                    viewer.drawEmptyBarrier(i, j, Orientation.VERT);
                }
            }

            if (i != 0) {
                for (int j = 0; j < Constants.GRID_SIZE; j++) {
                    viewer.drawEmptyBarrier(i, j, Orientation.HOR);
                }
            }
        }

        // Setup UI for players
        for (Player player : gameManager.getActivePlayers()) {
            viewer.drawPlayer(player.getGridPosition().getY(), player.getGridPosition().getX(), player.getPlayerId());
        }


        // Draw hud and scale the board.
        viewer.drawHud(gameManager.getActivePlayers());
        viewer.scaleGroup(Constants.GRID_SIZE, Constants.GRID_SIZE);
        viewer.getGroup().setZIndex(200);

    }


    /**
     * Parse the action from the user inputs.
     *
     * @param outputs - The data provided by the user.
     * @return Action object parsed from the data from the user.
     * @throws Exception - If an invalid input is provided.
     */
    public Constants.Action parseAction(String[] outputs, Player player) throws Exception {
        String action = outputs[0];
        switch (action) {
            case "MOVE":
                return parseMove(outputs, player);
            case "BARRIER":
                return parseBarrier(outputs);
            default:
                throw new Exception("provided an unknown action type: " + action);
        }
    }

    /**
     * Function to parse a movement action.
     * @param outputs List of outputs from the user.
     * @param player Player that made the move request.
     * @return Action object containing type and the direction of movement.
     * @throws Exception If non-integer
     */
    private Constants.Action parseMove(String[] outputs, Player player) throws Exception {
        int x = parseInt(outputs[1]);
        int y = parseInt(outputs[2]);

        int diffX = (x - player.getGridPosition().getX()) * 2;
        int diffY = (y - player.getGridPosition().getY()) * 2;

        Direction direction = Constants.Direction.fromOffsets(diffY, diffX);
        if (!board.inBoundsPlayer(y*2,x*2) || direction == null) {
            throw new Exception("provided coordinates that the player cannot reach.");
        }
        return new Constants.Action(direction);
    }


    /**
     * Function to parse a barrier action.
     * @param outputs List of outputs from the user.
     * @return Action object containing type, position and orientation of the barrier.
     * @throws Exception If invalid coordinates are provided.
     */
    private Constants.Action parseBarrier(String[] outputs) throws Exception {
        Orientation orientation;
        try {
            orientation = Constants.Orientation.valueOf(outputs[1]);
        } catch (IllegalArgumentException e) {
            throw new Exception("provided an unknown barrier orientation: " + outputs[1]);
        }

        int x = parseInt(outputs[2]);
        int y = parseInt(outputs[3]);

        if (!board.inBoundsBarrier(y,x, orientation)){
            throw new Exception("provided invalid barrier coordinates.");
        }

        return new Action(orientation, x, y);
    }


    /**
     * Parse an integer.
     * @param string String to be parsed.
     * @return Integer version of the provided string.
     * @throws Exception if a non-integer string is provided.
     */
    private int parseInt(String string) throws Exception {
        try {
            return Integer.parseInt(string);
        } catch (NumberFormatException e) {
            throw new Exception("provided non-integer coordinates.");
        }
    }


    /**
     * Function to perform the moving of a player.
     *
     * @param player Player performing the move
     * @param action The intended moves for the player.
     * @return boolean - True if the player moved, false if the move is invalid.
     */
    public boolean movePlayer(Player player, Action action) {
        // check if the move is valid
        Board.CanMove canMove;
        if (action.direction.isOrthogonal()){
            canMove = board.canMoveOrthogonal(player, action.direction);
        }else if (action.direction.isJump()){
            canMove = board.canMoveJump(player, action.direction);
        }
        else{
            canMove = board.canMoveDiagonal(player, action.direction);
        }

        // If invalid move, return false.
        if (!canMove.isCanMove()) {
            return false;
        }

        // Move player, update UI.
        board.move(player, canMove.getNextPosition());
        viewer.movePlayer(player.getGridPosition().getY(), player.getGridPosition().getX(), player.getPlayerId());
        return true;
    }


    /**
     * Function to create a barrier on the board given the users inputs. Ensuring the barrier isn't out of the grid,
     * doesn't overlap, intersect or block either player from reaching their goal zone.
     * @param player - Player that is creating the barrier, used to determine if they have barriers remaining.
     * @param action - [Action] object containing the information to place the barrier.
     * @return boolean - True if the barrier was created, otherwise false.
     */
    public boolean createBarrier(Player player, Action action) throws Exception{
        // Check the user has barriers to place.
        if (player.getBarriersRemaining() == 0){
            throw new Exception("tried to place a barrier without any remaining.");
        }

        // Check that the barrier is valid to be placed
        if (board.isValidBarrier(action.wallY, action.wallX, action.orientation.name())) {
            // Add the barrier to the board.
            board.addBarrier(action.wallY, action.wallX, action.orientation.name());
            // Check that it is still possible to reach the end for all players.
            if (!board.canReachEnd(gameManager.getActivePlayers())) {
                throw new Exception("created a barrier blocking a player from reaching their goal zone.");
            }
            viewer.drawBarrier(action.wallY, action.wallX, action.orientation, player.getPlayerId());
            player.removeBarrier();
            return true;
        }
        throw new Exception("tried to place an invalid barrier.");
    }

    /**
     * Function to check if a player has reached the opposite end and thus won the game.
     *
     * @param player Player to check for the win.
     * @return boolean - True if the player has won, otherwise false.
     */
    public boolean checkWin(Player player) {
        return player.position.getY() == player.goalY;
    }


    /**
     * Function to run the game. Taking in user inputs, executing valid requests otherwise ending the game.
     * @param turn - Integer indicating which turn it is.
     */
    @Override
    public void gameTurn(int turn) {
        // Determine the active player and send them the relevant information.
        Player player = gameManager.getPlayer(turn % 2);
        player.sendInputLine(lastMove);
        player.execute();
        try {
            List<String> outputs = player.getOutputs();
            lastMove = outputs.get(0);
            // Check validity of the player output.
            Action action = parseAction(lastMove.split(" "), player);

            // Complete the player's intended move.
            if (action.type == ActionType.MOVE) {
                boolean moved = movePlayer(player, action);
                if (!moved) {throw new Exception("attempted an invalid move.");}
                lastMove = "MOVE " + player.getGridPosition().getX() + " " + player.getGridPosition().getY();
            } else if (action.type == ActionType.BARRIER) {
                boolean created = createBarrier(player, action);
                if (!created) {throw new Exception("created an invalid barrier.");}
                // Update players remaining barriers UI.
                viewer.reducePlayerBarrier(player);
            }

            // Check if the player has won via moving into their goal row.
            if (checkWin(player)) {
                player.setScore(1);
                gameManager.endGame();
                gameManager.addToGameSummary("Winner - " + player.getNicknameToken());
            }

            // Check if the game has reached max turns and terminate if it has.
            if (turn == gameManager.getMaxTurns()){
                gameManager.addToGameSummary("Turn limit exceeded, result is a draw.");
                gameManager.endGame();
            }

        } catch (TimeoutException e) {
            player.deactivate(String.format("%d timeout!", player.getIndex()));
            player.setScore(-1);
            gameManager.addToGameSummary(String.format("Error - %s timed out", player.getNicknameToken()));
            gameManager.endGame();
        } catch (IndexOutOfBoundsException e){
            player.deactivate(String.format("%d invalid action!", player.getIndex()));
            player.setScore(-1);
            gameManager.addToGameSummary(String.format("Error - %s provided out of bounds action: %s", player.getNicknameToken(), lastMove));
            gameManager.endGame();
        }
        catch (Exception e) {
            player.deactivate(String.format("%d %s!", player.getIndex(), e));
            player.setScore(-1);
            gameManager.addToGameSummary(String.format("Error - %s %s",player.getNicknameToken(), e.getMessage()));
            gameManager.endGame();
        }
    }
}
