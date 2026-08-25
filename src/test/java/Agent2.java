import java.util.*;

/**
 * Stronger AI GENERATED AI.
 */
public class Agent2 {

    // ---- Search tuning ----
    static final int MAX_DEPTH = 2;
    static final int TIME_BUDGET_MS = 140; // stay safely under the referee's 150ms turn limit
    static final int WALL_CANDIDATE_LIMIT = 16;
    static final int MIN_REMAINING_DEPTH_FOR_WALLS = 2;
    static final int WALL_WEIGHT = 1; // eval bonus per spare wall relative to opponent
    static final int WIN_SCORE = 100000;

    static int H, W;
    static boolean[][] horizontalWalls;
    static boolean[][] verticalWalls;

    // "A" = this bot (root perspective), "B" = opponent. Mutated in place during search, undone after.
    static int myR, myC, myGoalY;
    static int oppR, oppC, theirGoalY;
    static int myWallsLeft = 10, oppWallsLeft = 10;

    static long deadline;
    static class TimeUp extends RuntimeException {}

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);

        if (!in.hasNextLine()) return;
        String[] var = in.nextLine().split(" ");
        H = Integer.parseInt(var[0]);
        W = Integer.parseInt(var[1]);

        horizontalWalls = new boolean[H][W];
        verticalWalls = new boolean[H][W];

        var = in.nextLine().split(" ");
        myC = Integer.parseInt(var[0]);
        myR = Integer.parseInt(var[1]);
        myGoalY = (myR == 0) ? H - 1 : 0;

        var = in.nextLine().split(" ");
        oppC = Integer.parseInt(var[0]);
        oppR = Integer.parseInt(var[1]);
        theirGoalY = (oppR == 0) ? H - 1 : 0;

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

    static boolean wallBlocksRight(int r, int c) {
        if (c < 0 || c >= W - 1) return true;
        boolean blocked = verticalWalls[r][c];
        if (!blocked && r > 0) blocked = verticalWalls[r - 1][c];
        return blocked;
    }

    static boolean wallBlocksDown(int r, int c) {
        if (r < 0 || r >= H - 1) return true;
        boolean blocked = horizontalWalls[r][c];
        if (!blocked && c > 0) blocked = horizontalWalls[r][c - 1];
        return blocked;
    }

    static boolean blockedBetween(int r1, int c1, int r2, int c2) {
        if (r1 == r2) return wallBlocksRight(r1, Math.min(c1, c2));
        return wallBlocksDown(Math.min(r1, r2), c1);
    }

    static boolean canPlaceWall(int y, int x, boolean vertical) {
        if (y < 0 || y > H - 2 || x < 0 || x > W - 2) return false;
        if (verticalWalls[y][x] || horizontalWalls[y][x]) return false;
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
        if (vertical) verticalWalls[y][x] = true; else horizontalWalls[y][x] = true;
    }

    static void removeWall(int y, int x, boolean vertical) {
        if (vertical) verticalWalls[y][x] = false; else horizontalWalls[y][x] = false;
    }

    // ===================== Pathfinding =====================

    static int[][] distanceMap(int goalRow) {
        int[][] dist = new int[H][W];
        for (int[] row : dist) Arrays.fill(row, -1);
        Deque<int[]> queue = new ArrayDeque<>();
        for (int c = 0; c < W; c++) {
            dist[goalRow][c] = 0;
            queue.add(new int[]{goalRow, c});
        }
        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            int r = cur[0], c = cur[1];
            for (int[] d : dirs) {
                int nr = r + d[0], nc = c + d[1];
                if (!inBounds(nr, nc) || dist[nr][nc] != -1) continue;
                if (blockedBetween(r, c, nr, nc)) continue;
                dist[nr][nc] = dist[r][c] + 1;
                queue.add(new int[]{nr, nc});
            }
        }
        return dist;
    }

    // ===================== Pawn move generation (shared shape with Agent1) =====================

    static class PawnDest { int r, c; PawnDest(int r, int c) { this.r = r; this.c = c; } }

    static List<PawnDest> legalPawnMoves(int r, int c, int oR, int oC) {
        List<PawnDest> moves = new ArrayList<>();
        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] d : dirs) {
            int tr = r + d[0], tc = c + d[1];
            if (!inBounds(tr, tc) || blockedBetween(r, c, tr, tc)) continue;

            if (tr == oR && tc == oC) {
                int jr = tr + d[0], jc = tc + d[1];
                if (inBounds(jr, jc) && !blockedBetween(tr, tc, jr, jc)) {
                    moves.add(new PawnDest(jr, jc));
                } else {
                    int[][] perp = (d[0] != 0) ? new int[][]{{0, -1}, {0, 1}} : new int[][]{{-1, 0}, {1, 0}};
                    for (int[] p : perp) {
                        int dr = tr + p[0], dc = tc + p[1];
                        if (!inBounds(dr, dc) || blockedBetween(tr, tc, dr, dc)) continue;
                        moves.add(new PawnDest(dr, dc));
                    }
                }
            } else {
                moves.add(new PawnDest(tr, tc));
            }
        }
        return moves;
    }

    // ===================== Search move representation =====================

    static class SearchMove {
        boolean isWall;
        int r, c;           // pawn: destination
        int wy, wx; boolean vertical; // wall: anchor + orientation
        int quickScore;      // used for move ordering only

        static SearchMove pawn(int r, int c, int quickScore) {
            SearchMove m = new SearchMove(); m.isWall = false; m.r = r; m.c = c; m.quickScore = quickScore;
            return m;
        }
        static SearchMove wall(int y, int x, boolean vertical, int quickScore) {
            SearchMove m = new SearchMove(); m.isWall = true; m.wy = y; m.wx = x; m.vertical = vertical; m.quickScore = quickScore;
            return m;
        }
    }

    static void checkTime() {
        if (System.currentTimeMillis() > deadline) throw new TimeUp();
    }

    static List<SearchMove> generateMoves(boolean isA, int remainingDepth) {
        int selfR = isA ? myR : oppR, selfC = isA ? myC : oppC;
        int otherR = isA ? oppR : myR, otherC = isA ? oppC : myC;
        int selfGoal = isA ? myGoalY : theirGoalY;
        int otherGoal = isA ? theirGoalY : myGoalY;
        int wallsLeft = isA ? myWallsLeft : oppWallsLeft;

        int[][] selfDistMap = distanceMap(selfGoal);
        List<SearchMove> result = new ArrayList<>();
        for (PawnDest pd : legalPawnMoves(selfR, selfC, otherR, otherC)) {
            int d = selfDistMap[pd.r][pd.c];
            result.add(SearchMove.pawn(pd.r, pd.c, -d));
        }

        if (wallsLeft > 0 && remainingDepth >= MIN_REMAINING_DEPTH_FOR_WALLS) {
            int baseSelfDist = selfDistMap[selfR][selfC];
            int baseOtherDist = distanceMap(otherGoal)[otherR][otherC];
            List<SearchMove> wallCandidates = new ArrayList<>();
            for (int y = 0; y < H - 1; y++) {
                for (int x = 0; x < W - 1; x++) {
                    for (int v = 0; v < 2; v++) {
                        boolean vertical = (v == 0);
                        if (!canPlaceWall(y, x, vertical)) continue;
                        placeWall(y, x, vertical);
                        int newSelfDist = distanceMap(selfGoal)[selfR][selfC];
                        int newOtherDist = distanceMap(otherGoal)[otherR][otherC];
                        removeWall(y, x, vertical);
                        if (newSelfDist == -1 || newOtherDist == -1) continue;
                        int gain = (newOtherDist - baseOtherDist) - (newSelfDist - baseSelfDist);
                        if (gain > 0) wallCandidates.add(SearchMove.wall(y, x, vertical, gain));
                    }
                }
            }
            wallCandidates.sort((a, b) -> b.quickScore - a.quickScore);
            for (int i = 0; i < Math.min(WALL_CANDIDATE_LIMIT, wallCandidates.size()); i++) {
                result.add(wallCandidates.get(i));
            }
        }

        result.sort((a, b) -> b.quickScore - a.quickScore);
        return result;
    }

    static void applyMove(SearchMove m, boolean isA) {
        if (m.isWall) {
            placeWall(m.wy, m.wx, m.vertical);
            if (isA) myWallsLeft--; else oppWallsLeft--;
        } else {
            if (isA) { myR = m.r; myC = m.c; } else { oppR = m.r; oppC = m.c; }
        }
    }

    static void undoMove(SearchMove m, boolean isA, int savedR, int savedC) {
        if (m.isWall) {
            removeWall(m.wy, m.wx, m.vertical);
            if (isA) myWallsLeft++; else oppWallsLeft++;
        } else {
            if (isA) { myR = savedR; myC = savedC; } else { oppR = savedR; oppC = savedC; }
        }
    }

    static int evaluate(boolean aToMove) {
        int myDist = distanceMap(myGoalY)[myR][myC];
        int oppDist = distanceMap(theirGoalY)[oppR][oppC];
        int score = (oppDist - myDist) + WALL_WEIGHT * (myWallsLeft - oppWallsLeft);
        return aToMove ? score : -score;
    }

    static int negamax(int depth, boolean aToMove, int alpha, int beta) {
        checkTime();
        if (myR == myGoalY) return aToMove ? WIN_SCORE + depth : -(WIN_SCORE + depth);
        if (oppR == theirGoalY) return aToMove ? -(WIN_SCORE + depth) : WIN_SCORE + depth;
        if (depth == 0) return evaluate(aToMove);

        List<SearchMove> moves = generateMoves(aToMove, depth);
        if (moves.isEmpty()) return evaluate(aToMove);

        int best = Integer.MIN_VALUE + 1;
        for (SearchMove m : moves) {
            int savedR = aToMove ? myR : oppR, savedC = aToMove ? myC : oppC;
            applyMove(m, aToMove);
            int val;
            try {
                val = -negamax(depth - 1, !aToMove, -beta, -alpha);
            } finally {
                // Must run even if TimeUp propagates through, or the tentative move
                // (position change or wall) leaks into the real board state.
                undoMove(m, aToMove, savedR, savedC);
            }
            if (val > best) best = val;
            if (best > alpha) alpha = best;
            if (alpha >= beta) break;
        }
        return best;
    }

    static SearchMove rootSearch(int depth) {
        List<SearchMove> moves = generateMoves(true, depth);
        int alpha = Integer.MIN_VALUE + 1, beta = Integer.MAX_VALUE - 1;
        SearchMove best = null;
        for (SearchMove m : moves) {
            int savedR = myR, savedC = myC;
            applyMove(m, true);
            int val;
            try {
                val = -negamax(depth - 1, false, -beta, -alpha);
            } finally {
                undoMove(m, true, savedR, savedC);
            }
            if (best == null || val > alpha) { alpha = val; best = m; }
        }
        return best;
    }

    // ===================== Top-level decision =====================

    static String decideMove() {
        deadline = System.currentTimeMillis() + TIME_BUDGET_MS;
        SearchMove chosen = null;
        try {
            for (int depth = 1; depth <= MAX_DEPTH; depth++) {
                SearchMove candidate = rootSearch(depth);
                if (candidate != null) chosen = candidate; // depth finished cleanly, safe to commit
            }
        } catch (TimeUp e) {
            // Discard the in-progress depth entirely; 'chosen' still holds the last fully completed depth.
        }

        if (chosen == null) {
            // Fallback (shouldn't normally trigger): plain shortest-path move.
            int[][] map = distanceMap(myGoalY);
            List<PawnDest> moves = legalPawnMoves(myR, myC, oppR, oppC);
            PawnDest best = null; int bestDist = Integer.MAX_VALUE;
            for (PawnDest pd : moves) {
                int d = map[pd.r][pd.c];
                if (d != -1 && d < bestDist) { bestDist = d; best = pd; }
            }
            if (best == null) return "MOVE " + myC + " " + myR;
            myR = best.r; myC = best.c;
            return "MOVE " + myC + " " + myR;
        }

        if (chosen.isWall) {
            placeWall(chosen.wy, chosen.wx, chosen.vertical);
            myWallsLeft--;
            String orientation = chosen.vertical ? "VERT" : "HOR";
            return "BARRIER " + orientation + " " + chosen.wx + " " + chosen.wy;
        } else {
            myR = chosen.r; myC = chosen.c;
            return "MOVE " + myC + " " + myR;
        }
    }
}
    /*
    public static void straightUp(){
        System.out.println("MOVE UP");
        System.out.println("MOVE UP");
        System.out.println("MOVE UP");
        System.out.println("MOVE UP");
        System.out.println("MOVE UP");
        System.out.println("MOVE UP");
        System.out.println("MOVE UP");
        System.out.println("MOVE UP");
        System.out.println("MOVE UP");
        System.out.println("MOVE UP");
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
        System.out.println("MOVE UP");            // Turn 1: Step to (0,7)
        System.out.println("MOVE UP");            // Turn 2: Step to (0,6)
        System.out.println("MOVE UP");            // Turn 3: Step to (0,5)
        System.out.println("MOVE UP");            // Turn 4: Face-to-face with Bot 1 at (0,4)
        System.out.println("BARRIER VERT 0 3");   // Turn 5: TRY TO CROSS WALL AT (0,3) -> SHOULD FAIL / BE ILLEGAL!
        System.out.println("MOVE RIGHT");
    }

    public static void x5x5(){
        // Testing moving UP_RIGHT AND UP_LEFT when there is a edge blocking the upwards
        System.out.println("MOVE RIGHT");
        System.out.println("MOVE LEFT");
        System.out.println("MOVE UP");
        System.out.println("MOVE UP");
        System.out.println("MOVE UP");
        //System.out.println("MOVE UP_LEFT");
        System.out.println("MOVE UP_RIGHT");
    }


    public static void x5x5x2(){
        //System.out.println("MOVE UP");
        System.out.println("BARRIER HOR 1 2");
        System.out.println("BARRIER VERT 1 1");
        System.out.println("BARRIER VERT 2 1");

        System.out.println("BARRIER HOR 3 0");
        System.out.println("MOVE 3 4");
        System.out.println("MOVE 4 4");
        System.out.println("MOVE 4 3");
        System.out.println("MOVE 4 2");

    }

    public static void x5x5x3(){
        // Testing horizontal dia rule
        System.out.println("MOVE 3 4");
        System.out.println("MOVE 4 4");
        System.out.println("MOVE 4 3");
        System.out.println("MOVE 4 2");
        System.out.println("MOVE 3 2");

        System.out.println("BARRIER HOR 2 1");

        System.out.println("MOVE 2 2");
        System.out.println("MOVE 1 1");
        System.out.println("MOVE 0 1");
        System.out.println("MOVE LEFT");


    }



    public static void main(String[] args) {

        //x5x5();
        x5x5x2();
        //x5x5x3();
        //t1();
        //straightUp();
        //impossibleBarriers1();

    }
}


     */