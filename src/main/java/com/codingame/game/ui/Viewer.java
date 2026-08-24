package com.codingame.game.ui;

import com.codingame.game.Constants;
import com.codingame.game.Coordinate;
import com.codingame.game.Player;
import com.codingame.gameengine.core.MultiplayerGameManager;
import com.codingame.gameengine.module.entities.GraphicEntityModule;
import com.codingame.gameengine.module.entities.Group;
import com.codingame.gameengine.module.entities.Sprite;
import com.codingame.gameengine.module.entities.Text;
import com.codingame.gameengine.module.toggle.ToggleModule;
import com.codingame.gameengine.module.tooltip.TooltipModule;
import com.google.inject.Inject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Viewer {

    @Inject
    private MultiplayerGameManager<Player> gameManager;
    @Inject
    private GraphicEntityModule graphicEntityModule;
    @Inject
    private TooltipModule tooltips;
    @Inject
    private ToggleModule toggleModule;

    private final Group group;
    private final HashMap<Integer, Sprite> players = new HashMap<>();
    private final HashMap<Coordinate, Sprite> movesBoard = new HashMap<>();
    private final HashMap<EmptyBarrier, Sprite> emptyBarriers = new HashMap<>();
    private ArrayList<Text> playerBarriersUI = new ArrayList<>();

    public Viewer(GraphicEntityModule graphicEntityModule, TooltipModule tooltips, ToggleModule toggleModule) {
        this.graphicEntityModule = graphicEntityModule;
        this.tooltips = tooltips;
        this.toggleModule = toggleModule;
        //graphicEntityModule.createSprite().setImage(Constants.BACKGROUND_SPRITE).setZIndex(0);
        group = graphicEntityModule.createGroup();
    }

    /**
     * Function to draw the main board.
     */
    public void drawBoard() {
        String tile;
        for (int i = 0; i < Constants.GRID_SIZE; i++){
            for (int j = 0; j < Constants.GRID_SIZE; j++) {
                if (i == 0){
                    tile = Constants.START_BLUE_TILE_SPRITE;
                }else if (i == Constants.GRID_SIZE-1){
                    tile = Constants.START_RED_TILE_SPRITE;
                }else{
                    tile = Constants.TILE_SPRITE;
                }
                drawTile(i, j, tile);

                // Setup the "moves" tiles.
                drawMoveTile(i,j,0);
                drawMoveTile(i,j,1);

                if (j != Constants.GRID_SIZE - 1) {
                    drawEmptyBarrier(i, j, Constants.Orientation.VERT);
                }
            }

            if (i!=0) {
                for (int j = 0; j < Constants.GRID_SIZE; j++) {
                    drawEmptyBarrier(i, j, Constants.Orientation.HOR);
                }
            }
        }
    }


    /**
     * Function to draw dummy move tiles and link them to the "moves" option.
     * @param i row of the tile.
     * @param j index of the tile
     * @param playerId player id to determine the correct image.
     */
    public void drawMoveTile(int i, int j, int playerId){
        String tile = playerId == 0 ? "tile_0.png" : "tile_1.png";
        Sprite sprite = createSprite(tile, j * Constants.CELL_SIZE + (j * Constants.BARRIER_SIZE), i * Constants.CELL_SIZE + i * Constants.BARRIER_SIZE, 100, 0).setVisible(false);
        group.add(sprite);
        movesBoard.put(new Coordinate(i*2,j*2, playerId), sprite);
        toggleModule.displayOnToggleState(sprite, "moves", true);
    }


    /**
     * Function to change the visibility of the move tiles. (Only changes if the move setting is set.)
     * @param player Player whose current turn it is.
     * @param tiles List of the tiles that the player can reach.
     */
    public void updateTiles(Player player, List<Coordinate> tiles){
        Sprite tile;
        for (Coordinate coord : tiles){
            tile = movesBoard.get(new Coordinate(coord.getY(), coord.getX(), player.getPlayerId()));
            graphicEntityModule.commitEntityState(0, tile.setVisible(true));
            graphicEntityModule.commitEntityState(1, tile.setVisible(false));
        }
    }


    /**
     * Draw the player HUD boxes. Contain player profile, name and remaining barriers.
     *
     * @param players - List of the players in the game.
     */
    public void drawHud(List<Player> players) {
        for (Player player : players) {
            int x = player.getIndex() == 0 ? 280 : 1920 - 280;
            int y = 220;

            graphicEntityModule
                    .createRectangle()
                    .setWidth(140)
                    .setHeight(140)
                    .setX(x - 70)
                    .setY(y - 70)
                    .setLineWidth(0)
                    .setFillColor(player.getColorToken());

            graphicEntityModule
                    .createRectangle()
                    .setWidth(120)
                    .setHeight(120)
                    .setX(x - 60)
                    .setY(y - 60)
                    .setLineWidth(0)
                    .setFillColor(0xffffff);

            Text playerNameText = graphicEntityModule.createText(player.getNicknameToken())
                    .setX(x)
                    .setY(y + 120)
                    .setZIndex(20)
                    .setFontSize(40)
                    .setFillColor(0xffffff)
                    .setAnchor(0.5);

            Text barriers = graphicEntityModule.createText(String.valueOf(player.getBarriersRemaining()))
                    .setX(x)
                    .setY(y + 240)
                    .setZIndex(20)
                    .setFontSize(40)
                    .setFillColor(0xffffff)
                    .setAnchor(0.5);

            Sprite avatar = graphicEntityModule.createSprite()
                    .setX(x)
                    .setY(y)
                    .setZIndex(20)
                    .setImage(player.getAvatarToken())
                    .setAnchor(0.5)
                    .setBaseHeight(116)
                    .setBaseWidth(116);
            playerBarriersUI.add(barriers);
        }
    }

    /**
     * Function to reduce the number of barriers for a player.
     *
     * @param player - Player that added the barrier.
     */
    public void reducePlayerBarrier(Player player) {
        playerBarriersUI.get(player.getPlayerId()).setText(String.valueOf(player.getBarriersRemaining()));
        graphicEntityModule.commitEntityState(0.5, playerBarriersUI.get(player.getPlayerId()));
    }


    /**
     * Draw the tile and add to the group.
     *
     * @param i - Vertical position.
     * @param j - Horizontal position.
     */
    public Sprite drawTile(int i, int j, String tileName) {
        //String tileName = Constants.TILE_SPRITE;
        int z_TILES = 5;
        Sprite tile = createSprite(tileName, j * Constants.CELL_SIZE + (j * Constants.BARRIER_SIZE), i * Constants.CELL_SIZE + i * Constants.BARRIER_SIZE, z_TILES, 0);
        group.add(tile);

        // Setup another tile that contains the tooltip, allowing it to be toggled to display or hide tooltips.
        Sprite tooltipTile = createSprite(tileName, j * Constants.CELL_SIZE + (j * Constants.BARRIER_SIZE), i * Constants.CELL_SIZE + i * Constants.BARRIER_SIZE, z_TILES, 0);
        tooltips.setTooltipText(tooltipTile, String.format("X: %d\nY: %d", j, i));
        toggleModule.displayOnToggleState(tooltipTile, "debug", true);
        group.add(tooltipTile);
        return tile;
    }


    /**
     * Draw the empty space for a barrier and add to the group.
     *
     * @param i   Vertical position.
     * @param j   Horizontal position.
     * @param dir Direction of the barrier. VERT or HOR.
     */
    public void drawEmptyBarrier(int i, int j, Constants.Orientation dir) {
        String tileName;
        String orientation;
        int offsetX = j;
        int offsetY = i;
        if (dir == Constants.Orientation.VERT) {
            orientation = "VERT";
            tileName = Constants.BARRIER_VERT_SPRITE;
            offsetX += 1;
        } else {
            orientation = "HOR";
            tileName = Constants.BARRIER_HOR_SPRITE;
            offsetY -= 1;
        }
        int z_TILES = 5;
        Sprite barrier = createSprite(tileName, ((offsetX) * Constants.CELL_SIZE) + j * Constants.BARRIER_SIZE, i * Constants.CELL_SIZE + offsetY * Constants.BARRIER_SIZE, z_TILES, 0);
        group.add(barrier);

        // Tooltips + with toggle.
        Sprite tooltipsBarrier = createSprite(tileName, ((offsetX) * Constants.CELL_SIZE) + j * Constants.BARRIER_SIZE, i * Constants.CELL_SIZE + offsetY * Constants.BARRIER_SIZE, z_TILES, 0);
        int barrierI = i;
        if (dir.equals(Constants.Orientation.HOR)) {
            barrierI -= 1;
        }
        ;
        emptyBarriers.put(new EmptyBarrier(barrierI, j, dir), tooltipsBarrier);
        tooltips.setTooltipText(tooltipsBarrier, String.format("Barrier\nX: %d\nY: %d\nOrientation: %s", j, barrierI, orientation));
        group.add(tooltipsBarrier);
        toggleModule.displayOnToggleState(tooltipsBarrier, "debug", true);
    }


    /**
     * Draw actual barrier into the location specified. Draw two barriers and also the midpoint.
     *
     * @param i        Vertical position.
     * @param j        Horizontal position.
     * @param dir      Direction of the barrier. VERT or HOR.
     * @param playerId ID of the player building the barrier - Used for aiding the tooltip for who placed the barrier.
     */
    public void drawBarrier(int i, int j, Constants.Orientation dir, int playerId) {

        boolean isVert = (dir == Constants.Orientation.VERT);
        String spriteName = isVert ? Constants.ACTUAL_BARRIER_VERT_SPRITE
                : Constants.ACTUAL_BARRIER_HOR_SPRITE;

        // Calculate base pixel top-left for grid cell (i, j)
        int baseX = j * (Constants.CELL_SIZE + Constants.BARRIER_SIZE);
        int baseY = i * (Constants.CELL_SIZE + Constants.BARRIER_SIZE);

        // Offset by CELL_SIZE on the perpendicular axis where the barrier line sits
        int x1 = isVert ? baseX + Constants.CELL_SIZE : baseX;
        int y1 = isVert ? baseY : baseY + Constants.CELL_SIZE;

        // Second segment continues 1 step down (vert) or 1 step right (horiz)
        int step = Constants.CELL_SIZE + Constants.BARRIER_SIZE;
        int x2 = isVert ? x1 : x1 + step;
        int y2 = isVert ? y1 + step : y1;

        int zTiles = 10;

        int middleX = isVert ? x1 : x1 + Constants.CELL_SIZE;
        int middleY = isVert ? y1 + Constants.CELL_SIZE : y1;

        Sprite barrier1 = createSprite(spriteName, x1, y1, zTiles, 0);
        Sprite barrierMiddle = createSprite(Constants.ACTUAL_BARRIER_MID_SPRITE, middleX, middleY, zTiles, 0);
        Sprite barrier2 = createSprite(spriteName, x2, y2, zTiles, 0);

        group.add(barrier1);
        group.add(barrier2);
        group.add(barrierMiddle);

        // Find the grid position for the barriers.
        int i2 = i;
        int j2 = j;
        if (isVert) {
            i2 += 1;
        } else {
            j2 += 1;
        }

        // Tooltips.
        String playerColour = playerId == 0 ? "RED" : "BLUE";
        Sprite b1 = emptyBarriers.get(new EmptyBarrier(i, j, dir));
        Sprite b2 = emptyBarriers.get(new EmptyBarrier(i2, j2, dir));
        tooltips.setTooltipText(b1, String.format("Barrier\nX: %d\nY: %d\nOrientation: %s\nPlaced By: %s", j, i, dir.name(), playerColour));
        tooltips.setTooltipText(b2, String.format("Barrier\nX: %d\nY: %d\nOrientation: %s\nPlaced By: %s", j2, i2, dir.name(), playerColour));
        graphicEntityModule.commitEntityState(0.5, group);
    }


    /**
     * Add the player UI to the board.
     *
     * @param i        Row index of where to place the player.
     * @param j        Column index of where to place the player.
     * @param playerId Integer of the playerId of the player currently being added.
     */
    public void drawPlayer(int i, int j, int playerId) {
        String imageName = "p" + playerId + ".png";
        Sprite player = createSprite(imageName, j * Constants.CELL_SIZE + (j * Constants.BARRIER_SIZE), i * Constants.CELL_SIZE + i * Constants.BARRIER_SIZE, 500, 0);
        players.put(playerId, player);
        group.add(player);
    }


    /**
     * Function to move a players UI.
     *
     * @param i        Row index of where to move the player.
     * @param j        Column index of where to move the player.
     * @param playerId Integer of the playerId of the player currently being moved.
     */
    public void movePlayer(int i, int j, int playerId) {
        Sprite player = players.get(playerId);
        player.setX(j * Constants.CELL_SIZE + (j * Constants.BARRIER_SIZE));
        player.setY(i * Constants.CELL_SIZE + i * Constants.BARRIER_SIZE);
    }


    /**
     * This function will create a sprite with the given parameters.
     *
     * @param texture - String of the texture.
     * @param x       - X-Position to place the sprite.
     * @param y       - Y-Position to place the sprite.
     * @param z       - Z-index of the sprite.
     * @param anchor  - Double value of the anchor.
     * @return Generated Sprite object.
     */
    public Sprite createSprite(String texture, int x, int y, int z, int anchor) {
        return graphicEntityModule.createSprite()
                .setImage(texture)
                .setX(x)
                .setY(y)
                .setAnchor(anchor)
                .setZIndex(z);
    }


    /**
     * Scale the group to fit the screen.
     *
     * @param w - Width of the board.
     * @param h - Height of the board.
     */
    public void scaleGroup(int w, int h) {

        // Calculate total grid size in pixels
        int gridWidth = w * Constants.CELL_SIZE + (w - 1) * Constants.BARRIER_SIZE;
        int gridHeight = h * Constants.CELL_SIZE + (w - 1) * Constants.BARRIER_SIZE;

        // Calculate scale to fit in viewer
        double scaleX = (double) Constants.VIEWER_WIDTH / gridWidth;
        double scaleY = (double) (Constants.VIEWER_HEIGHT - 100) / gridHeight;
        double scale = Math.min(1.0, Math.min(scaleX, scaleY));

        // Recompute size after scale for centering
        int scaledWidth = (int) (gridWidth * scale);
        int scaledHeight = (int) (gridHeight * scale);

        // Center the group in the viewer
        int centerX = Constants.VIEWER_WIDTH / 2;
        int centerY = Constants.VIEWER_HEIGHT / 2;

        // Scale and position both groups.
        group.setScale(scale);
        group.setX(centerX - scaledWidth / 2);
        group.setY(centerY - scaledHeight / 2);
    }

    public Group getGroup() {
        return group;
    }
}
