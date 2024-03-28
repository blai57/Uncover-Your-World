package byow.Core;

import byow.TileEngine.TERenderer;
import byow.TileEngine.TETile;
import byow.TileEngine.Tileset;
import edu.princeton.cs.algs4.StdDraw;

import java.awt.*;
import java.util.Calendar;
import java.util.Random;

public class BlockFallMiniGame {
    private int WIDTH, HEIGHT, score, playerX, numTreasureChests, playerLives;
    private TETile[][] displayWorld, board;
    private static final Font gameFont = new Font("Monaco", Font.PLAIN, 12);
    private static final Font titleFont = new Font("Monaco", Font.BOLD, 30);
    private static final int BOARD_SIZE = 10;
    private Random random;
    int createEnemiesEvery = 1000;
    int updateEnemyMovementEvery = 250;
    int SCORE_TO_WIN = 5;

    public BlockFallMiniGame(int w, int h, int numTreasureChests, int playerLives) {
        WIDTH = w;
        HEIGHT = h;
        this.createEnemiesEvery = 200 * numTreasureChests;
        this.updateEnemyMovementEvery = 100 * numTreasureChests;
        displayWorld = new TETile[WIDTH][HEIGHT];
        this.random = new Random();
        this.numTreasureChests = numTreasureChests;
        this.playerLives = playerLives;
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                displayWorld[x][y] = Tileset.BLACK;
            }
        }

        //board always 10 x 10
        board = new TETile[BOARD_SIZE][BOARD_SIZE];
        playerX = BOARD_SIZE / 2;
        //need to add outline around board so user knows where area is
        for (int x = 0; x < BOARD_SIZE; x++) {
            for (int y = 0; y < BOARD_SIZE; y++) {
                board[x][y] = Tileset.BLACK;
            }
        }
    }

    private void refreshDisplay(TERenderer r) {

        for (int x = -1; x < BOARD_SIZE + 1; x++) {
            for (int y = -1; y < BOARD_SIZE + 1; y++) {
                int placeX = x + (WIDTH / 2) - (BOARD_SIZE / 2);
                int placeY = y + (HEIGHT / 2) + (BOARD_SIZE / 2);
                if (x == -1 || y == -1 || x == BOARD_SIZE || y == BOARD_SIZE) {
                    displayWorld[placeX][placeY] = Tileset.WHITE;
                } else {
                    displayWorld[placeX][placeY] = board[x][y];
                }
            }
        }


        //put player on screen
        displayWorld[playerX + (WIDTH / 2) - (BOARD_SIZE / 2)][(HEIGHT / 2) + (BOARD_SIZE / 2)] = Tileset.AVATAR2;

        r.renderFrame(displayWorld);
        //print description of block we're on
        //draw hint text
        StdDraw.setFont(gameFont);
        StdDraw.setPenColor(Color.WHITE);
        StdDraw.textLeft(1, HEIGHT - 1, "Avoid enemies by moving left (A) or right (D)");
        StdDraw.setFont(titleFont);
        StdDraw.textLeft(1, HEIGHT - 6, "Score: " + score);
        StdDraw.setFont(gameFont);

        StdDraw.show();
    }

    private long currTime() {
        return Calendar.getInstance().getTimeInMillis();
    }

    public boolean run(TERenderer renderer) {
        directions();
        refreshDisplay(renderer);
        long lastEnemyCreation = 0;
        long lastEnemyUpdate = 0;

        int numRandoms = 0;

        while (true) {
            if (StdDraw.hasNextKeyTyped()) {
                char c = Character.toUpperCase(StdDraw.nextKeyTyped());
                if (c == 'A' && playerX > 0) {
                    playerX--;
                } else if (c == 'D' && playerX < BOARD_SIZE - 1) {
                    playerX++;
                }
                refreshDisplay(renderer);
            }
            //see how much time is passed since we put enemy in
            if (currTime() - lastEnemyCreation > createEnemiesEvery) {
                //make new enemy by placing them somewhere in top row of board
                int randX = RandomUtils.uniform(random, BOARD_SIZE);
                numRandoms++;
                board[randX][BOARD_SIZE - 1] = Tileset.RED;

                lastEnemyCreation = currTime();
            }

            //we need to update all enemies and redraw screen every so often
            if (currTime() - lastEnemyUpdate > updateEnemyMovementEvery) {

                //move enemies down

                for (int x = 0; x < BOARD_SIZE; x++) {
                    for (int y = 0; y < BOARD_SIZE; y++) {
                        if (board[x][y] == Tileset.RED) {
                            //change this tile to black
                            board[x][y] = Tileset.BLACK;
                            //if tile is at bottom of board, don't move it down, but also we successfully
                            //avoid enemy, so give points
                            if (y == 0) {
                                score++;
                                if (score == SCORE_TO_WIN) {
                                    win();
                                    return true;
                                }
                            } else if (x == playerX && y - 1 == 0) {
                                gameOver();
                                return false;
                            } else {
                                board[x][y - 1] = Tileset.RED;
                            }
                        }
                    }
                }
                refreshDisplay(renderer);
                lastEnemyUpdate = currTime();
            }

        }
    }
    private void gameOver() {
        //if one life left, don't show game over
        if (playerLives == 1) {
            return;
        }
        int middleWidth = WIDTH / 2;
        int middleHeight = HEIGHT / 2;

        int countDown = 5000;
        while (countDown > 0) {
            StdDraw.clear(Color.BLACK);
            StdDraw.setFont(titleFont);
            StdDraw.setPenColor(Color.WHITE);
            StdDraw.text(middleWidth, HEIGHT - 7, "GAME OVER");
            StdDraw.setFont(gameFont);
            StdDraw.text(middleWidth, middleHeight, "You lost the minigame!");
            StdDraw.text(middleWidth, middleHeight - 2, "You have " + (playerLives - 1) + " live(s) left.");
            StdDraw.text(middleWidth, middleHeight - 8, "A new treasure has been spawned.");
            StdDraw.text(middleWidth, middleHeight - 10, "Explore the map to find it and play again!");
            StdDraw.text(middleWidth, middleHeight - 14, "You need to win " + numTreasureChests + " mini game(s) to win!");
            StdDraw.text(middleWidth, middleHeight - 16, "Continue in " + countDown / 1000 + " seconds");
            StdDraw.show();
            StdDraw.pause(1000);
            countDown -= 1000;
        }


    }
    private void win() {

        //if there was one chest left when we started the game, don't need to show this screen \
        // because there aren't anymore to find
        if (numTreasureChests == 1) {
            return;
        }
        int middleWidth = WIDTH / 2;
        int middleHeight = HEIGHT / 2;

        int countDown = 5000;
        while (countDown > 0) {
            StdDraw.clear(Color.BLACK);
            StdDraw.setFont(titleFont);
            StdDraw.setPenColor(Color.WHITE);
            StdDraw.text(middleWidth, HEIGHT - 7, "You win!");
            StdDraw.setFont(gameFont);
            StdDraw.text(middleWidth, middleHeight, "You won the mini game!");
            StdDraw.text(middleWidth, middleHeight - 2, "You need to win " + (numTreasureChests - 1) + " mini game(s) to win!");
            StdDraw.text(middleWidth, middleHeight - 16, "Continue in " + countDown / 1000 + " seconds");
            StdDraw.show();
            StdDraw.pause(1000);
            countDown -= 1000;
        }
    }

    public void directions() {
        int middleWidth = WIDTH / 2;
        int middleHeight = HEIGHT / 2;

        StdDraw.clear(Color.BLACK);
        StdDraw.setFont(titleFont);
        StdDraw.setPenColor(Color.WHITE);
        StdDraw.text(middleWidth, HEIGHT - 7, "Block Fall Mini Game");
        StdDraw.setFont(gameFont);
        StdDraw.text(middleWidth, middleHeight, "Use A and D to move left and right.");
        StdDraw.text(middleWidth, middleHeight - 2, "to avoid all red falling blocks.");
        StdDraw.text(middleWidth, middleHeight - 4, "If you dodge 5, you win.");
        StdDraw.text(middleWidth, middleHeight - 6, "Press (C) to continue.");
        StdDraw.show();
        while (true) {
            if (StdDraw.hasNextKeyTyped()) {
                char c = Character.toUpperCase(StdDraw.nextKeyTyped());
                if (c == 'C') {
                    break;
                }
            }
        }
    }
}
