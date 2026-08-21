import com.codingame.game.Constants;
import com.codingame.game.Coordinate;

public class StatementImages1 {

    public static void basicMovement(){

        // Set position to be central 1,1

        // Remove second player from the grid.
        //private final Coordinate[] startingPosition = {new Coordinate(1, 1), new Coordinate(Constants.GRID_SIZE * 2 - 1 - 1, Constants.GRID_SIZE - 1)};

    }

    public static void basicMovementImage2(){

        // Set position to be central 1,1

        // Remove second player from the grid.
        //private final Coordinate[] startingPosition = {new Coordinate(1, 1), new Coordinate(Constants.GRID_SIZE * 2 - 1 - 1, Constants.GRID_SIZE - 1)};

        // Barriers
        System.out.println("MOVE 1 1");
    }

    public static void jumpMovementImage(){

//        Set player 0 to - (2, 1)
//        viewer.drawPlayer(2, 0, 1);
//        viewer.drawPlayer(1, 1, 1);
//        viewer.drawPlayer(2, 2, 1);
//        viewer.drawPlayer(3, 1, 1);

        System.out.println("BARRIER HOR 0 1");
    }

    public static void barriers(){

        System.out.println("BARRIER HOR 0 0");


    }

    public static void barriers2(){

        System.out.println("BARRIER HOR 0 0");

    }

    public static void invalidInputs(){
        System.out.println("BARRIER HOR 8 6");

        System.out.println("BARRIER HOR 10 10");

    }


    public static void main(String[] args) {
        invalidInputs();
    }

}
