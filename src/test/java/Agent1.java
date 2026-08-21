
import java.util.*;

/**
 * Beatable Quoridor AI for CodinGame.
 *
 * Strategy (intentionally weak, no lookahead / minimax):
 *   - Movement: pure greedy shortest-path (BFS distance-to-goal map), ties broken randomly.
 *   - Walls: myopic single-wall evaluation only (tries every legal wall, picks the one that
 *            hurts the opponent's path the most relative to its own), used only sometimes
 *            (WALL_CHANCE) and only when the opponent isn't clearly behind (RACE_MARGIN).
 *
 * Tune WALL_CHANCE / RACE_MARGIN / MIN_WALL_GAIN below to adjust difficulty.
 *
 * Protocol notes (see referee):
 *   - All coordinates exchanged with the referee are grid coords "x y" (x = column, y = row).
 *   - A straight jump over an adjacent opponent is requested by sending the true landing
 *     square two cells away directly (this relies on the fixed Board.canMoveJump).
 *   - Diagonal jumps are requested by sending the true diagonal landing square directly.
 */
public class Agent1 {

    // ---- Difficulty tuning ----
    static final double WALL_CHANCE = 0.8;   // chance to actually place a "good" wall when found
    static final int RACE_MARGIN = 1;        // only consider walling if opp isn't clearly behind me
    static final int MIN_WALL_GAIN = 1;       // minimum net (oppDelta - myDelta) to bother walling

    static int H, W;
    static boolean[][] horizontalWalls; // horizontalWalls[y][x]: HOR wall anchored at grid (y,x)
    static boolean[][] verticalWalls;   // verticalWalls[y][x]: VERT wall anchored at grid (y,x)

    // current state, all in (row=Y, col=X) grid coordinates
    static int myR, myC, myGoalY;
    static int oppR, oppC, theirGoalY;
    static int myWallsLeft = 10, oppWallsLeft = 10;

    static final Random rng = new Random();
    static int[][] DIRS = {{1, 0}, {-1, 0}, {0, -1}, {0, 1}};

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);

        if (!in.hasNextLine()) return;
        String[] var = in.nextLine().split(" ");
        H = Integer.parseInt(var[0]);
        W = Integer.parseInt(var[1]);

        horizontalWalls = new boolean[H][W];
        verticalWalls = new boolean[H][W];

        var = in.nextLine().split(" ");
        int myX = Integer.parseInt(var[0]);
        myR = Integer.parseInt(var[1]);
        myC = myX;
        myGoalY = (myR == 0) ? H - 1 : 0;
        
        if (myGoalY == 0){
            DIRS = new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        }

        var = in.nextLine().split(" ");
        int oppX = Integer.parseInt(var[0]);
        oppR = Integer.parseInt(var[1]);
        oppC = oppX;
        theirGoalY = (oppR == 0) ? H - 1 : 0;

        // Game loop
        while (in.hasNextLine()) {
            String line = in.nextLine();
            applyOpponentLine(line);

            String output = decideMove();
            System.out.println(output);
        }
    }

    // ===================== Opponent move parsing =====================

    static void applyOpponentLine(String line) {
        if (line == null) return;
        line = line.trim();
        if (line.isEmpty()) return;
        String[] parts = line.split(" ");
        if (parts.length == 0) return;

        if (parts[0].equals("MOVE") && parts.length >= 3) {
            oppC = Integer.parseInt(parts[1]);
            oppR = Integer.parseInt(parts[2]);
        } else if (parts[0].equals("BARRIER") && parts.length >= 4) {
            String orientation = parts[1];
            int x = Integer.parseInt(parts[2]);
            int y = Integer.parseInt(parts[3]);
            placeWall(y, x, orientation.equals("VERT"));
            oppWallsLeft = Math.max(0, oppWallsLeft - 1);
        }
    }

    // ===================== Wall geometry =====================

    static boolean inBounds(int r, int c) {
        return r >= 0 && r < H && c >= 0 && c < W;
    }

    // Blocks horizontal movement between (r,c) and (r,c+1)
    static boolean wallBlocksRight(int r, int c) {
        if (c < 0 || c >= W - 1) return true; // off the right edge counts as blocked
        boolean blocked = verticalWalls[r][c];
        if (!blocked && r > 0) blocked = verticalWalls[r - 1][c];
        return blocked;
    }

    // Blocks vertical movement between (r,c) and (r+1,c)
    static boolean wallBlocksDown(int r, int c) {
        if (r < 0 || r >= H - 1) return true; // off the bottom edge counts as blocked
        boolean blocked = horizontalWalls[r][c];
        if (!blocked && c > 0) blocked = horizontalWalls[r][c - 1];
        return blocked;
    }

    static boolean blockedBetween(int r1, int c1, int r2, int c2) {
        if (r1 == r2) {
            int c = Math.min(c1, c2);
            return wallBlocksRight(r1, c);
        } else {
            int r = Math.min(r1, r2);
            return wallBlocksDown(r, c1);
        }
    }

    static boolean canPlaceWall(int y, int x, boolean vertical) {
        if (y < 0 || y > H - 2 || x < 0 || x > W - 2) return false;
        if (verticalWalls[y][x] || horizontalWalls[y][x]) return false; // same post used
        if (vertical) {
            if (y > 0 && verticalWalls[y - 1][x]) return false;
            if (y < H - 2 && verticalWalls[y + 1][x]) return false;
        } else {
            if (x > 0 && horizontalWalls[y][x - 1]) return false;
            if (x < W - 2 && horizontalWalls[y][x + 1]) return false;
        }
        return true;
    }

    static void placeWall(int y, int x, boolean vertical) {
        if (vertical) verticalWalls[y][x] = true;
        else horizontalWalls[y][x] = true;
    }

    static void removeWall(int y, int x, boolean vertical) {
        if (vertical) verticalWalls[y][x] = false;
        else horizontalWalls[y][x] = false;
    }

    // ===================== Pathfinding =====================

    /** Multi-source BFS distance (in steps, ignoring jumps) from every cell to goalRow. -1 = unreachable. */
    static int[][] distanceMap(int goalRow) {
        int[][] dist = new int[H][W];
        for (int[] row : dist) Arrays.fill(row, -1);
        Deque<int[]> queue = new ArrayDeque<>();
        for (int c = 0; c < W; c++) {
            dist[goalRow][c] = 0;
            queue.add(new int[]{goalRow, c});
        }

        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            int r = cur[0], c = cur[1];
            for (int[] d : DIRS) {
                int nr = r + d[0], nc = c + d[1];
                if (!inBounds(nr, nc)) continue;
                if (dist[nr][nc] != -1) continue;
                if (blockedBetween(r, c, nr, nc)) continue;
                dist[nr][nc] = dist[r][c] + 1;
                queue.add(new int[]{nr, nc});
            }
        }
        return dist;
    }

    // ===================== Move generation =====================

    static class Move {
        int destR, destC; // where I will actually end up
        int sendR, sendC; // what grid coords to print in "MOVE x y"
        boolean isBarrier;
        int wallY, wallX;
        boolean vertical;

        static Move move(int destR, int destC, int sendR, int sendC) {
            Move m = new Move();
            m.destR = destR; m.destC = destC; m.sendR = sendR; m.sendC = sendC;
            m.isBarrier = false;
            return m;
        }

        static Move barrier(int y, int x, boolean vertical) {
            Move m = new Move();
            m.isBarrier = true;
            m.wallY = y; m.wallX = x; m.vertical = vertical;
            return m;
        }
    }

    /** All legal moves (pawn moves only) for the pawn at (r,c), given the opponent at (oR,oC). */
    static List<Move> legalMoves(int r, int c, int oR, int oC) {
        List<Move> moves = new ArrayList<>();
        for (int[] d : DIRS) {
            int tr = r + d[0], tc = c + d[1];
            if (!inBounds(tr, tc)) continue;
            if (blockedBetween(r, c, tr, tc)) continue;

            if (tr == oR && tc == oC) {
                // Opponent is adjacent - try straight jump first.
                int jr = tr + d[0], jc = tc + d[1];
                if (inBounds(jr, jc) && !blockedBetween(tr, tc, jr, jc)) {
                    // Straight jump legal - send the landing square directly.
                    moves.add(Move.move(jr, jc, jr, jc));
                } else {
                    // Straight jump blocked -> diagonal jumps to either side become legal.
                    int[][] perp = (d[0] != 0) ? new int[][]{{0, -1}, {0, 1}} : new int[][]{{-1, 0}, {1, 0}};
                    for (int[] p : perp) {
                        int dr = tr + p[0], dc = tc + p[1];
                        if (!inBounds(dr, dc)) continue;
                        if (blockedBetween(tr, tc, dr, dc)) continue;
                        moves.add(Move.move(dr, dc, dr, dc)); // diagonal: send the landing square directly
                    }
                }
            } else {
                moves.add(Move.move(tr, tc, tr, tc));
            }
        }
        return moves;
    }

    // ===================== Decision making =====================

    static String decideMove() {
        int[][] myDistMap = distanceMap(myGoalY);
        int[][] oppDistMap = distanceMap(theirGoalY);
        int oldMyDist = myDistMap[myR][myC];
        int oldOppDist = oppDistMap[oppR][oppC];

        Move bestWall = null;
        int bestGain = MIN_WALL_GAIN - 1;

        if (myWallsLeft > 0 && oldOppDist <= oldMyDist + RACE_MARGIN) {
            for (int y = 0; y < H - 1; y++) {
                for (int x = 0; x < W - 1; x++) {
                    for (int v = 0; v < 2; v++) {
                        boolean vertical = (v == 0);
                        if (!canPlaceWall(y, x, vertical)) continue;

                        placeWall(y, x, vertical);
                        int[][] newMyMap = distanceMap(myGoalY);
                        int[][] newOppMap = distanceMap(theirGoalY);
                        removeWall(y, x, vertical);

                        int newMyDist = newMyMap[myR][myC];
                        int newOppDist = newOppMap[oppR][oppC];
                        if (newMyDist == -1 || newOppDist == -1) continue; // would trap someone

                        int gain = (newOppDist - oldOppDist) - (newMyDist - oldMyDist);
                        if (gain > bestGain) {
                            bestGain = gain;
                            bestWall = Move.barrier(y, x, vertical);
                        }
                    }
                }
            }
        }

        if (bestWall != null && rng.nextDouble() < WALL_CHANCE) {
            placeWall(bestWall.wallY, bestWall.wallX, bestWall.vertical);
            myWallsLeft--;
            String orientation = bestWall.vertical ? "VERT" : "HOR";
            return "BARRIER " + orientation + " " + bestWall.wallX + " " + bestWall.wallY;
        }

        // Otherwise: greedy shortest-path movement.
        List<Move> moves = legalMoves(myR, myC, oppR, oppC);
        Move chosen = null;
        int bestDist = Integer.MAX_VALUE;
        List<Move> tied = new ArrayList<>();
        for (Move m : moves) {
            int d = myDistMap[m.destR][m.destC];
            if (d == -1) continue;
            if (d < bestDist) {
                bestDist = d;
                tied.clear();
                tied.add(m);
            } else if (d == bestDist) {
                tied.add(m);
            }
        }
        if (!tied.isEmpty()) {
            chosen = tied.get(rng.nextInt(tied.size()));
        } else if (!moves.isEmpty()) {
            chosen = moves.get(rng.nextInt(moves.size())); // fallback, shouldn't normally happen
        }

        if (chosen == null) {
            // No legal move found (shouldn't happen) - forfeit-safe fallback: stay put attempt.
            return "MOVE " + myC + " " + myR;
        }

        myR = chosen.destR;
        myC = chosen.destC;
        return "MOVE " + chosen.sendC + " " + chosen.sendR;
    }
}




/*
public class Agent1 {

    public static void straightDown(){
        System.out.println("MOVE DOWN");
        System.out.println("MOVE DOWN");
        System.out.println("MOVE DOWN");
        System.out.println("MOVE DOWN");
        System.out.println("MOVE DOWN");
        System.out.println("MOVE DOWN");
        System.out.println("MOVE DOWN");
        System.out.println("MOVE DOWN");
        System.out.println("MOVE DOWN");
        System.out.println("MOVE DOWN");
    }

    public static void impossibleBarriers1(){
        System.out.println("BARRIER HOR 0 0");
        System.out.println("BARRIER HOR 2 0");
        System.out.println("BARRIER HOR 4 0");
        System.out.println("BARRIER HOR 6 0");
        System.out.println("BARRIER HOR 7 1");
        System.out.println("BARRIER VERT 7 0");
    }

    public static void t1(){
        System.out.println("MOVE DOWN");          // Turn 1: Step to (0,1)
        System.out.println("BARRIER HOR 1 3");
        System.out.println("BARRIER VERT 7 2");
        System.out.println("MOVE DOWN");          // Turn 1: Step to (0,1)
        System.out.println("MOVE DOWN");          // Turn 2: Step to (0,2)
        System.out.println("MOVE DOWN");          // Turn 3: Step to (0,3)
        System.out.println("BARRIER HOR 1 3");     // Turn 4: Place wall behind Bot 2 at (0,3)
        System.out.println("MOVE DOWN");          // Turn 5: Straight jump over Bot 2 to (0,5)!
        System.out.println("MOVE DOWN");
    }

    public static void x5x5(){
        System.out.println("BARRIER VERT 1 1");
        System.out.println("BARRIER HOR 0 1");
        System.out.println("BARRIER VERT 2 0");
        System.out.println("BARRIER HOR 0 3");
        System.out.println("BARRIER HOR 3 3");

        System.out.println("MOVE DOWN");
        System.out.println("MOVE DOWN");
        System.out.println("MOVE DOWN");
        System.out.println("MOVE DOWN");
        System.out.println("MOVE DOWN");
    }

    public static void x5x5x2(){
        // Test the bottom left and bottom right rule
        System.out.println("MOVE LEFT");
        System.out.println("MOVE RIGHT");
        System.out.println("MOVE DOWN");
        System.out.println("MOVE DOWN");

        System.out.println("MOVE DOWN_LEFT");

        System.out.println("MOVE DOWN");
        //System.out.println("MOVE DOWN_LEFT");
        System.out.println("MOVE DOWN_RIGHT");
        System.out.println("MOVE DOWN");
    }


    public static void x5x5x3(){
        //test hor dia rule
        System.out.println("MOVE 1 0");
        System.out.println("MOVE 0 0");
        System.out.println("MOVE 0 1");
        System.out.println("MOVE 0 2");
        System.out.println("MOVE 1 2");

        System.out.println("BARRIER HOR 1 2");
        System.out.println("BARRIER VERT 0 2");
        System.out.println("BARRIER HOR 0 1");

        System.out.println("MOVE 2 2");

    }

    public static void main(String[] args) {

        //x5x5();
        //x5x5x2();
        x5x5x3();

        //t1();
        //straightDown();
        //impossibleBarriers1();

    }
}
 */