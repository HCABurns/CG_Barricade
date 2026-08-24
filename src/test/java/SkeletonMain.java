import com.codingame.gameengine.runner.MultiplayerGameRunner;

public class SkeletonMain {
    public static void main(String[] args) {

        /* Multiplayer Game */
        MultiplayerGameRunner gameRunner = new MultiplayerGameRunner();

        // Actual simulator of playing
        //gameRunner.addAgent(Agent1.class);
        //gameRunner.addAgent(Agent2.class);
        //gameRunner.addAgent("java config\\AIGeneratedBoss.java");
        //gameRunner.addAgent("java config\\AIBadBoss.java");
        gameRunner.addAgent("python config\\Boss.py");
        gameRunner.addAgent("python config\\Boss.py");

        // Used for visuals for the statement.
        //gameRunner.addAgent(StatementImages1.class);
        //gameRunner.addAgent(StatementImages2.class);

        gameRunner.start();
    }
}


// Another way to add a player
// gameRunner.addAgent("python3 /home/user/player.py");