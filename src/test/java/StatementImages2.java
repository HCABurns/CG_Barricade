public class StatementImages2 {

    public static void basicMovements(){
        // Set grid size to 3

        // Set starting possitions

    }

    public static void basicMovementImage2(){

        // Set position to be central 1,1

        // Remove second player from the grid.
        //private final Coordinate[] startingPosition = {new Coordinate(1, 1), new Coordinate(Constants.GRID_SIZE * 2 - 1 - 1, Constants.GRID_SIZE - 1)};

        // Barriers
        System.out.println("BARRIER VERT 0 1");
        System.out.println("BARRIER HOR 0 0");
    }

    public static void jumpMovementImage(){

//        Set player 0 to - (2, 1)
//        viewer.drawPlayer(2, 0, 1);
//        viewer.drawPlayer(1, 1, 1);
//        viewer.drawPlayer(2, 2, 1);
//        viewer.drawPlayer(3, 1, 1);

        // Barriers
        System.out.println("BARRIER VERT 2 2");
    }

    public static void barriers(){

        System.out.println("BARRIER VERT 0 0");


    }

    public static void barriers2(){
        // Show valid "intersection"

        System.out.println("BARRIER VERT 1 0");
        System.out.println("BARRIER HOR 2 0");


    }

    public static void invalidInputs(){

        System.out.println("BARRIER HOR 7 7");
        System.out.println("MOVE -5 4");
        System.out.println("BARRIER VERT awd 3");
        System.out.println("BARRIER 0 10 10");

    }


    public static void main(String[] args) {
        invalidInputs();
    }


}
