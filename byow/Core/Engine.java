package byow.Core;

import byow.InputDemo.InputSource;
import byow.InputDemo.KeyboardInputSource;
import byow.InputDemo.StringInputDevice;
import byow.TileEngine.TERenderer;
import byow.TileEngine.TETile;
import byow.TileEngine.Tileset;
import edu.princeton.cs.algs4.StdDraw;


import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Random;

public class Engine {
    TERenderer ter = new TERenderer();
    /* Feel free to change the width and height. */
    public static final int WIDTH = 40;
    public static final int HEIGHT = 40;
    public static final int HUD_HEIGHT = 4;
    private Random random;
    public static final TETile BACKGROUND = Tileset.GRASS2;
    public static final int SPLIT_ITERATIONS = 5;
    private TreeNode rootOfTree;
    private int avatarX = 0;
    private int avatarY = 0;
    private TETile[][] finalWorldFrame;
    private static final Font titleFont = new Font("Monaco", Font.BOLD, 30);
    private static final Font optionFont = new Font("Monaco", Font.BOLD, 20);
    private static final Font tileDescription = new Font("Monaco", Font.PLAIN, 12);
    private static final InputSource KEYBOARD = new KeyboardInputSource();
    private String curTileDescription = "";
    private String status = "";
    public static final TETile BOUNDARY = Tileset.BRICK;
    public static final int KEY_BACKSPACE = 8;
    private String avatarName = "";
    private ArrayList<Room> allRooms = new ArrayList<>();
    private static int NUM_TREASURE_CHESTS = 3;

    private static int IN_GAME = 0;
    private static int WON_GAME = 1;
    private static int LOST_GAME = 2;
    private int GAME_STATUS = IN_GAME;
    private int PLAYER_LIVES = 3;
    private String KEYPRESS_HISTORY = "";
    private boolean isLoadingGame = false;

    public Engine() {
        random = new Random();
        ter.initialize(WIDTH, HEIGHT + HUD_HEIGHT);
    }

    //make class to represent partitions we will use in BSP algorithm
    private class Partition {
        public int width;
        public int height;
        //x, y is the bottom left corner of rectangle
        public int x;
        public int y;

        public Partition(int x, int y, int width, int height) {
            this.width = width;
            this.height = height;
            this.x = x;
            this.y = y;
        }

        public int getMidPointX() {
            return x + (width / 2);
        }

        public int getMidPointY() {
            return y + (height / 2);
        }

    }

    //make class to represent node in tree for BSP
    private class TreeNode {
        public Partition p;
        public TreeNode left;
        public TreeNode right;

        public TreeNode(Partition p) {
            this.p = p;
            this.left = null;
            this.right = null;
        }

        public boolean isLeaf() {
            return left == null && right == null;
        }
    }

    private class Room {
        public int height;
        public int width;
        public int x;
        public int y;
        public boolean hasTreasure;

        /* @source https://eskerda.com/bsp-dungeon-generation/ */
        public Room(Partition p) {
            int randomStartX = RandomUtils.uniform(random, 0, p.width / 3);
            int randomStartY = RandomUtils.uniform(random, 0, p.height / 3);
            this.x = p.x + randomStartX;
            this.y = p.y + randomStartY;
            this.width = p.width - (x - p.x);
            this.height = p.height - (y - p.y);
            //subtract anywhere from 25% to 60% of room
            this.width -= RandomUtils.uniform(random, p.width * 0.25, p.width * 0.6);
            this.height -= RandomUtils.uniform(random, p.height * 0.25, p.height * 0.6);
            this.hasTreasure = false;

            //x value for room goes through x point of partition

            //shift this room if too far to left to go through center point
            while (x + width <= p.getMidPointX()) {
                x++;
            }
            //shift this room if too far to right to go through center point
            while (x >= p.getMidPointX()) {
                x--;
            }
            //shift this room if too far to up to go through center point
            while (y + height <= p.getMidPointY()) {
                y++;
            }
            //shift this room if too far to right to go through center point
            while (y >= p.getMidPointY()) {
                y--;
            }

            //if we shifted the room, and it is touching the edge of map, move it by 1
            if (x == 0) {
                x++;
            } else if (x == WIDTH - 1) {
                x--;
            }
            if (y == 0) {
                y++;
            } else if (y == HEIGHT - 1) {
                y--;
            }

        }
        public int getMidPointX() {
            return x + (width / 2);
        }

        public int getMidPointY() {
            return y + (height / 2);
        }

        public boolean isAvatarInside(int x, int y) {
            return this.x <= x && x <= this.x + width && this.y <= y && y <= this.y + height;
        }

        public void draw(TETile[][] world, TETile type, boolean isLight) {
            for (int x = this.x; x <= this.x + width; x++) {
                for (int y = this.y; y <= this.y + height; y++) {
                    if (!isLight) {
                        world[x][y] = Tileset.BLACK;
                    } else {
                        world[x][y] = type;
                    }
                }
            }
            if (isLight && hasTreasure) {
                world[getMidPointX()][getMidPointY()] = Tileset.TREASURE_CHEST;
            }
        }
    }

    //Use BSP algorithm to set up the world
    /* @source https://eskerda.com/bsp-dungeon-generation/ */
    private TreeNode makeBSPTree(Partition p, int numIterations, TETile[][] world) {
        TreeNode newNode = new TreeNode(p);
        if (numIterations != 0) {
            //split the node
            Partition[] splitPartitions = splitPartition(p);
            newNode.left = makeBSPTree(splitPartitions[0], numIterations - 1, world);
            newNode.right = makeBSPTree(splitPartitions[1], numIterations - 1, world);

        }

        return newNode;

    }

    /* @source https://eskerda.com/bsp-dungeon-generation/ */
    public Partition[] splitPartition(Partition p) {
        Partition p1 = null;
        Partition p2 = null;
        double discardRatio = 0.45;
        int randDir = RandomUtils.uniform(random, 2);
        Partition[] values = new Partition[2];
        if (randDir == 0) {
            //vertical
            int newWidth = RandomUtils.uniform(random, 1, p.width);
            p1 = new Partition(p.x, p.y, newWidth, p.height);
            p2 = new Partition(p.x + p1.width, p.y, p.width - p1.width, p.height);

            //recalculate partition split if one of the partitions is too small
            if (p1.width / (double) p1.height < discardRatio || p2.width / (double) p2.height < discardRatio) {
                return splitPartition(p);
            }

        } else if (randDir == 1) {
            //horizontal
            int newHeight = RandomUtils.uniform(random, 1, p.height);
            p1 = new Partition(p.x, p.y, p.width, newHeight);
            p2 = new Partition(p.x, p.y + newHeight, p.width, p.height - newHeight);
            //recalculate partition split if one of the partitions is too small
            if (p1.height / (double) p1.width < discardRatio || p2.height / (double) p2.width < discardRatio) {
                return splitPartition(p);
            }
        }
        values[0] = p1;
        values[1] = p2;
        return values;
    }

    public void getLeafNodes(TreeNode node, ArrayList<TreeNode> leafNodes) {
        if (node == null) {
            return;
        }

        getLeafNodes(node.left, leafNodes);
        if (node.isLeaf()) {
            leafNodes.add(node);
        }
        getLeafNodes(node.right, leafNodes);
    }

    public void drawPathFromCenterOfPartitions(TETile[][] world, Partition p1, Partition p2) {
        int p1x = p1.getMidPointX();
        int p1y = p1.getMidPointY();

        int p2x = p2.getMidPointX();
        int p2y = p2.getMidPointY();

        if (p1x == p2x) {
            int yStart = p1y;
            if (p2y < yStart) {
                yStart = p2y;
            }
            int yStop = p2y;
            if (p1y > yStop) {
                yStop = p1y;
            }
            for (int i = yStart; i <= yStop; i++) {
                world[p1x][i] = Tileset.DIRT2;
            }
        } else if (p1y == p2y) {
            int xStart = p1x;
            if (p2x < xStart) {
                xStart = p2x;
            }
            int xStop = p2x;
            if (p1x > xStop) {
                xStop = p1x;
            }
            for (int i = xStart; i <= xStop; i++) {
                world[i][p1y] = Tileset.DIRT2;
            }
        }
    }

    public void drawAllPaths(TETile[][] world, TreeNode node) {
        if (node == null) {
            return;
        }
        //if not leaf node draw path from center of partitions
        if (!node.isLeaf()) {
            drawPathFromCenterOfPartitions(world, node.left.p, node.right.p);
        }
        drawAllPaths(world, node.left);
        drawAllPaths(world, node.right);
    }

    /**
     * Method used for exploring a fresh world. This method should handle all inputs,
     * including inputs from the main menu.
     */
    public void interactWithKeyboard() {
        mainMenu();
        //the start menu has 3 options: N, L, and Q
        //new game = change screen to prompt for seed
        //load game = loads the game from storage
        //quit = exits game
        while (KEYBOARD.possibleNextInput()) {
            char c = KEYBOARD.getNextKey();
            if (c == 'Q') {
                System.exit(0);
            } else if (c == 'N') {
                KEYPRESS_HISTORY += c;
                showEnterSeedScreen();
                directions();
                createWorld();
                setupNewGame();
                playGame();
            } else if (c == 'A') {
                showEnterAvatarName();
            } else if (c == 'L') {
                isLoadingGame = true;
                showLoadGameScreen();
                playGame();
            }
        }
    }

    public void refreshDisplay() {

        //the world represent playable area of screen but we want to add 2 empty lines at bottom
        TETile[][] displayWorld = new TETile[WIDTH][HEIGHT + HUD_HEIGHT];
        //copy over original world but don't start at 0, 0
        //y should start at 2
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                displayWorld[x][y] = finalWorldFrame[x][y];
            }
        }

        //bottom 2 lines are null, so let's fill it in to avoid crashes
        for (int x = 0; x < WIDTH; x++) {
            for (int y = HEIGHT; y < HEIGHT + HUD_HEIGHT; y++) {
                displayWorld[x][y] = Tileset.BLACK;
            }
        }


        for (int i = 0; i < allRooms.size(); i++) {
            if (allRooms.get(i).isAvatarInside(avatarX, avatarY)) {
                allRooms.get(i).draw(displayWorld, Tileset.DIRT2, true);
            } else {
                allRooms.get(i).draw(displayWorld, Tileset.DIRT2, false);
            }
        }

        displayWorld[avatarX][avatarY] = Tileset.AVATAR2;
        //draw current lives left
        //out display world has 2 rows above playing board, put hearts on right of first row above our board
        int x = WIDTH - 2;
        for (int i = 0; i < PLAYER_LIVES; i++) {
            displayWorld[x][HEIGHT + 1] = Tileset.HEART;
            x--;
        }

        x = WIDTH - 8;
        for (int i = 0; i < NUM_TREASURE_CHESTS; i++) {
            displayWorld[x][HEIGHT + 1] = Tileset.TREASURE_CHEST;
            x--;
        }
        ter.renderFrame(displayWorld);
        //print description of block we're on
        //draw hint text
        StdDraw.setFont(tileDescription);
        StdDraw.setPenColor(Color.WHITE);
        StdDraw.textLeft(1, HEIGHT + HUD_HEIGHT - 1, curTileDescription);
        StdDraw.textLeft(1, HEIGHT + HUD_HEIGHT - 2, status);

        //draw avatar name
        StdDraw.textRight(WIDTH - 1, HEIGHT + HUD_HEIGHT - 1, avatarName);


        int midScreen = WIDTH / 2;
        StdDraw.textLeft(midScreen, HEIGHT + HUD_HEIGHT - 1, "Up: W");
        StdDraw.textLeft(midScreen, HEIGHT + HUD_HEIGHT - 2, "Down: S");
        StdDraw.textLeft(midScreen + 6, HEIGHT + HUD_HEIGHT - 1, "Left: A");
        StdDraw.textLeft(midScreen + 6, HEIGHT + HUD_HEIGHT - 2, "Right: D");
        StdDraw.show();
    }

    //create mini game (the 540 points primary feature)
    //1 treasure chest in random room at the start it is not same room as player
    //opening chest spawns mini game, if you win mini game, you get point
    //if you lose game, a new chest is randomly generated and you can go find it to play again
    //need 3 mini games points to win

    public void setupNewGame() {
        GAME_STATUS = IN_GAME;
        generateTreasureChest();
        refreshDisplay();
        //reset all game elements necessary
    }
    public void playGame() {
        isLoadingGame = false;

        int prevX = (int) StdDraw.mouseX();
        int prevY = (int) StdDraw.mouseY();

        while (true) {
            int x = (int) StdDraw.mouseX();
            int y = (int) StdDraw.mouseY();
            //check to see we are within bounds
            if (x >= 0 && x <= WIDTH - 1 && y >= 0 && y <= HEIGHT - 1 && (prevX != x || prevY != y)) {
                prevX = x;
                prevY = y;
                curTileDescription = finalWorldFrame[x][y].description();
                refreshDisplay();
            }

            if (StdDraw.hasNextKeyTyped()) {
                char c = Character.toUpperCase(StdDraw.nextKeyTyped());
                KEYPRESS_HISTORY += c;
                boolean didMove = moveAvatar(c);

                if (GAME_STATUS == WON_GAME) {
                    showWinScreen();
                } else if (GAME_STATUS == LOST_GAME) {
                    showLostScreen();
                }

                if (c == 'Q') {
                    //pressing q does not quit the game, so if we don't find the colon, it does nothing.
                    //check to see if the 2nd to last button was a colon.
                    char isColon = KEYPRESS_HISTORY.charAt(KEYPRESS_HISTORY.length() - 2);
                    if (isColon == ':') {
                        saveGameProgress();
                        System.exit(0);
                    }
                }
                refreshDisplay();
            }

        }
    }

    private void showWinScreen() {

        int middleWidth = WIDTH / 2;
        int middleHeight = HEIGHT / 2;

        StdDraw.clear(Color.BLACK);
        StdDraw.setFont(titleFont);
        StdDraw.setPenColor(Color.WHITE);
        StdDraw.text(middleWidth, HEIGHT - 7, "You win!");
        StdDraw.setFont(optionFont);
        StdDraw.text(middleWidth, middleHeight, "Press (Q) to quit.");

        StdDraw.show();
        while (true) {
            if (StdDraw.hasNextKeyTyped()) {
                char c = Character.toUpperCase(StdDraw.nextKeyTyped());
                if (c == 'Q') {
                    System.exit(0);
                }
            }
        }
    }

    private void showLostScreen() {
        int middleWidth = WIDTH / 2;
        int middleHeight = HEIGHT / 2;

        StdDraw.clear(Color.BLACK);
        StdDraw.setFont(titleFont);
        StdDraw.setPenColor(Color.WHITE);
        StdDraw.text(middleWidth, HEIGHT - 7, "You lost!");
        StdDraw.setFont(optionFont);
        StdDraw.text(middleWidth, middleHeight, "Press (Q) to quit.");
        StdDraw.show();
        while (true) {
            if (StdDraw.hasNextKeyTyped()) {
                char c = Character.toUpperCase(StdDraw.nextKeyTyped());
                if (c == 'Q') {
                    System.exit(0);
                }
            }
        }
    }

    public void generateTreasureChest() {

        //we should never spawn a treasure chest same room it was in, so put it down.
        int lastRoom = -1;

        //get rid of any treasure if they exist
        for (int i = 0; i < allRooms.size(); i++) {
            if (allRooms.get(i).hasTreasure) {
                allRooms.get(i).hasTreasure = false;
                lastRoom = i;
                break;
            }
        }
        int randRoom = -1;
        do {
            randRoom = RandomUtils.uniform(random, allRooms.size());
            if (allRooms.get(randRoom).isAvatarInside(avatarX, avatarY)) {
                randRoom = -1;
            }
        } while (randRoom == -1 || randRoom == lastRoom);

        allRooms.get(randRoom).hasTreasure = true;
    }

    public boolean moveAvatar(char c) {
        if (c == 'W') {
            if (avatarY == HEIGHT - 1 || finalWorldFrame[avatarX][avatarY + 1] == BOUNDARY) {
                status = "Can't move up";
                return false;
            }
            avatarY++;
        } else if (c == 'S') {
            if (avatarY == 0 || finalWorldFrame[avatarX][avatarY - 1] == BOUNDARY) {
                status = "Can't move down";
                return false;
            }
            avatarY--;
        } else if (c == 'A') {
            if (avatarX == 0 || finalWorldFrame[avatarX - 1][avatarY] == BOUNDARY) {
                status = "Can't move left";
                return false;
            }
            avatarX--;
        } else if (c == 'D') {
            if (avatarX == WIDTH || finalWorldFrame[avatarX + 1][avatarY] == BOUNDARY) {
                status = "Can't move right";
                return false;
            }
            avatarX++;
        }
        //after they moved, they may have landed on a treasure
        //go through all rooms, if user is in room, then check if the room has treasure
        for (int i = 0; i < allRooms.size(); i++) {
            if (allRooms.get(i).isAvatarInside(avatarX, avatarY)) {
                //see if this room has treasure
                if (allRooms.get(i).hasTreasure) {
                    //if avatar is in center of the room, they ran over the trasure
                    if (avatarX == allRooms.get(i).getMidPointX() && avatarY == allRooms.get(i).getMidPointY()) {
                        //add in enemies from top, move towards our player who is only on bottom row
                        //player has to avoid 5 enemies to win
                        if (!isLoadingGame) {
                            BlockFallMiniGame bf = new BlockFallMiniGame(WIDTH, HEIGHT + HUD_HEIGHT,
                                    NUM_TREASURE_CHESTS, PLAYER_LIVES);
                            boolean wonGame = bf.run(ter);
                            KEYPRESS_HISTORY += "T";
                            if (wonGame) {
                                NUM_TREASURE_CHESTS--;
                                KEYPRESS_HISTORY += "1";
                            } else {
                                PLAYER_LIVES--;
                                KEYPRESS_HISTORY += "0";
                            }
                            if (NUM_TREASURE_CHESTS == 0) {
                                //we win the game
                                GAME_STATUS = WON_GAME;
                                showWinScreen();
                                break;
                            } else if (PLAYER_LIVES == 0) {
                                GAME_STATUS = LOST_GAME;
                                showLostScreen();
                                break;
                            }
                        }
                        generateTreasureChest();
                        break;
                    }
                }
            }
        }
        status = "";
        return true;
    }

    public void directions() {
        int middleWidth = WIDTH / 2;
        int middleHeight = HEIGHT / 2;

        StdDraw.clear(Color.BLACK);
        StdDraw.setFont(titleFont);
        StdDraw.setPenColor(Color.WHITE);
        StdDraw.text(middleWidth, HEIGHT - 7, "Directions");
        StdDraw.setFont(optionFont);
        StdDraw.text(middleWidth, middleHeight, "Find a treasure chest in the dark to unlock ");
        StdDraw.text(middleWidth, middleHeight - 2, "a minigame.");
        StdDraw.text(middleWidth, middleHeight - 5, "Rooms will be illuminated when ");
        StdDraw.text(middleWidth, middleHeight - 7, "the avatar enters.");
        StdDraw.text(middleWidth, middleHeight - 10, "Win 3 minigames to complete the map.");
        StdDraw.text(middleWidth, middleHeight - 13, "Press (C) to continue.");

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

    public void mainMenu() {

        int middleWidth = WIDTH / 2;
        int middleHeight = HEIGHT / 2;

        StdDraw.clear(Color.BLACK);
        StdDraw.setFont(titleFont);
        StdDraw.setPenColor(Color.WHITE);
        StdDraw.text(middleWidth, HEIGHT - 7, "CS61B: THE GAME");
        StdDraw.setFont(optionFont);
        StdDraw.text(middleWidth, middleHeight, "New Game (N)");
        StdDraw.text(middleWidth, middleHeight - 2, "Load Game (L)");
        StdDraw.text(middleWidth, middleHeight - 4, "Choose Avatar Name (A)   " + avatarName);
        StdDraw.text(middleWidth, middleHeight - 6, "Quit (Q)");
        StdDraw.show();
    }

    public void showEnterSeedScreen() {
        int middleWidth = WIDTH / 2;
        int middleHeight = HEIGHT / 2;

        String seedNumber = "";
        boolean enterSeed = true;
        while (enterSeed) {
            StdDraw.clear(Color.BLACK);
            StdDraw.setFont(titleFont);
            StdDraw.setPenColor(Color.WHITE);
            StdDraw.text(middleWidth, HEIGHT - 7, "Enter a seed number:");
            StdDraw.text(middleWidth, HEIGHT - 9, "(press S when finished)");
            Font optionFont = new Font("Monaco", Font.BOLD, 20);
            StdDraw.setFont(optionFont);
            StdDraw.text(middleWidth, middleHeight, seedNumber);
            StdDraw.show();
            char c = KEYBOARD.getNextKey();
            if (Character.isDigit(c)) {
                seedNumber += c;
                KEYPRESS_HISTORY += c;
            } else if ((int) c == KEY_BACKSPACE) {
                //when the player press backspace
                seedNumber = seedNumber.substring(0, seedNumber.length() - 1);
                KEYPRESS_HISTORY = KEYPRESS_HISTORY.substring(0, KEYPRESS_HISTORY.length() - 1);
            }
            if (c  == 'S') {
                KEYPRESS_HISTORY += c;
                enterSeed = false;
            }
        }
        //process seed they typed in
        long seedNum = Long.parseLong(seedNumber);
        random.setSeed(seedNum);
    }

    //180 points secondary feature
    public void showEnterAvatarName() {
        int middleWidth = WIDTH / 2;
        int middleHeight = HEIGHT / 2;

        avatarName = "";
        boolean enterName = true;
        while (enterName) {
            StdDraw.clear(Color.BLACK);
            StdDraw.setFont(titleFont);
            StdDraw.setPenColor(Color.WHITE);
            StdDraw.text(middleWidth, HEIGHT - 7, "Enter a name:");
            StdDraw.text(middleWidth, HEIGHT - 9, "(press [ENTER] when finished)");
            Font optionFont = new Font("Monaco", Font.BOLD, 20);
            StdDraw.setFont(optionFont);
            StdDraw.text(middleWidth, middleHeight, avatarName);
            StdDraw.show();
            char c = KEYBOARD.getNextKey();
            if ((int) c == KEY_BACKSPACE) {
                //when the player press backspace
                avatarName = avatarName.substring(0, avatarName.length() - 1);
            } else if (c  == '\n') {
                enterName = false;
            } else {
                avatarName += c;
            }

        }
        mainMenu();
    }

    public void showLoadGameScreen() {
        File f = new File("./savedata.txt");
        String input = null;
        if (f.exists()) {
            try {
                FileInputStream fs = new FileInputStream(f);
                ObjectInputStream os = new ObjectInputStream(fs);
                input = os.readObject().toString();
                String[] inputString = input.split(",");
                avatarName = inputString[0];
                String world = inputString[1];
                interactWithInputString(world);
            } catch (FileNotFoundException e) {
                System.out.println("File not found");
                System.exit(0);
            } catch (IOException e) {
                System.out.println(e);
                System.exit(0);
            } catch (ClassNotFoundException e) {
                System.out.println("Class not found");
                System.exit(0);
            }
        }

    }
    public TETile[][] createWorld() {
        finalWorldFrame = new TETile[WIDTH][HEIGHT];
        //make all tiles
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                finalWorldFrame[x][y] = BACKGROUND;
            }
        }

        Partition p = new Partition(0, 0, WIDTH - 1, HEIGHT - 1);

        //use ran num of iterations so num of rooms changes
        int numIterations = RandomUtils.uniform(random, 2, SPLIT_ITERATIONS);
        rootOfTree = makeBSPTree(p, numIterations, finalWorldFrame);
        drawAllPaths(finalWorldFrame, rootOfTree);
        ArrayList<TreeNode> leafNodes = new ArrayList<>();
        getLeafNodes(rootOfTree, leafNodes);

        //pick random room to put avatar in
        //let's pick a random leaf node
        int randRoom = RandomUtils.uniform(random, leafNodes.size());
        //go through all leaf nodes and draw room in partition
        for (int i = 0; i < leafNodes.size(); i++) {
            Room newRoom = new Room(leafNodes.get(i).p);
            newRoom.draw(finalWorldFrame, Tileset.DIRT2, true);
            allRooms.add(newRoom);
            //if this room is the random room to put avatar in, calculate avatar coordinates
            if (i == randRoom) {
                avatarX = newRoom.getMidPointX();
                avatarY = newRoom.getMidPointY();
            }
        }
        //draw walls around all paths and rooms
        drawBoundaries(finalWorldFrame);

        ter.renderFrame(finalWorldFrame);
        return finalWorldFrame;
    }

    public void saveGameProgress() {
        File f = new File("./savedata.txt");
        try {
            if (!f.exists()) {
                f.createNewFile();
            }
            FileOutputStream fs = new FileOutputStream(f);
            ObjectOutputStream os = new ObjectOutputStream(fs);
            String saveString = avatarName + "," + KEYPRESS_HISTORY.substring(0, KEYPRESS_HISTORY.length() - 2);
            os.writeObject(saveString);
        } catch (FileNotFoundException e) {
            System.out.println("File not found");
            System.exit(0);
        } catch (IOException e) {
            System.out.println(e);
            System.exit(0);
        }
    }

    /**
     * Method used for autograding and testing your code. The input string will be a series
     * of characters (for example, "n123sswwdasdassadwas", "n123sss:q", "lwww". The engine should
     * behave exactly as if the user typed these characters into the engine using
     * interactWithKeyboard.
     *
     * Recall that strings ending in ":q" should cause the game to quite save. For example,
     * if we do interactWithInputString("n123sss:q"), we expect the game to run the first
     * 7 commands (n123sss) and then quit and save. If we then do
     * interactWithInputString("l"), we should be back in the exact same state.
     *
     * In other words, running both of these:
     *   - interactWithInputString("n123sss:q")
     *   - interactWithInputString("lww")
     *
     * should yield the exact same world state as:
     *   - interactWithInputString("n123sssww")
     *
     * @param input the input string to feed to your program
     * @return the 2D TETile[][] representing the state of the world
     */
    public TETile[][] interactWithInputString(String input) {
        // Fill out this method so that it run the engine using the input
        // passed in as an argument, and return a 2D tile representation of the
        // world that would have been drawn if the same inputs had been given
        // to interactWithKeyboard().
        //
        // See proj3.byow.InputDemo for a demo of how you can make a nice clean interface
        // that works for many different input types.

        input = input.toUpperCase();
        InputSource inputSource = new StringInputDevice(input);
        //the input string should be starting keypress history;
        KEYPRESS_HISTORY = input;
        // "n3412s"
        boolean createNewWorld = false;
        boolean isParsingNumber = false;
        String parseNum = "";
        long seedNumber = 0;
        //if no N, don't start the world
        while (inputSource.possibleNextInput()) {
            char current = inputSource.getNextKey();
            //if in the middle of parsing number, concatenate number
            if (isParsingNumber) {
                if (Character.isDigit(current)) {
                    parseNum += current;
                } else {
                    isParsingNumber = false;
                    seedNumber = Long.parseLong(parseNum);
                }
            } else if (Character.isDigit(current)) {
                if (!isParsingNumber) {
                    isParsingNumber = true;
                }
                parseNum += current;
            }
            if (current == 'N') {
                isParsingNumber = true;
            } else if (current == 'S') {
                //use the number that came before this to seed random number generator
                random.setSeed(seedNumber);
                break;
            }
        }

        createWorld();
        GAME_STATUS = IN_GAME;
        generateTreasureChest();
        refreshDisplay();

        while (inputSource.possibleNextInput()) {
            char current = inputSource.getNextKey();
            if (current == 'T') {
                //assume we save file correct and there is always number after T
                char nextChar = inputSource.getNextKey();
                if (nextChar == '0') {
                    //lost
                    PLAYER_LIVES--;
                } else {
                    //0 or 1
                    NUM_TREASURE_CHESTS--;
                }
            } else {
                moveAvatar(current);
            }
        }
        refreshDisplay();

        return finalWorldFrame;
    }

    public void drawBoundaries(TETile[][] world) {
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                //if space not background, every neighbouring box that is background should get turned into boundary
                if (world[x][y] != BACKGROUND && world[x][y] != BOUNDARY) {
                    if (y < HEIGHT - 1) {
                        //check left
                        if (x > 0 && world[x - 1][y + 1] == BACKGROUND) {
                            world[x - 1][y + 1] = BOUNDARY;
                        }
                        //check above
                        if (world[x][y + 1] == BACKGROUND) {
                            world[x][y + 1] = BOUNDARY;
                        }
                        //check right
                        if (x < WIDTH - 1 && world[x + 1][y + 1] == BACKGROUND) {
                            world[x + 1][y + 1] = BOUNDARY;
                        }
                    }
                    //check left
                    if (x > 0 && world[x - 1][y + 1] == BACKGROUND) {
                        world[x - 1][y + 1] = BOUNDARY;
                    }

                    //check right
                    if (x < WIDTH - 1 && world[x + 1][y + 1] == BACKGROUND) {
                        world[x + 1][y + 1] = BOUNDARY;
                    }

                    if (y > 0) {
                        //check left
                        if (x > 0 && world[x - 1][y - 1] == BACKGROUND) {
                            world[x - 1][y - 1] = BOUNDARY;
                        }
                        //check above
                        if (world[x][y - 1] == BACKGROUND) {
                            world[x][y - 1] = BOUNDARY;
                        }
                        //check right
                        if (x < WIDTH - 1 && world[x + 1][y - 1] == BACKGROUND) {
                            world[x + 1][y - 1] = BOUNDARY;
                        }
                    }
                }
            }
        }
    }

}
