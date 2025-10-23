package solver;

import java.util.*;

public class SokoBot{
    private class Point{
        int x, y;
        public Point(int x, int y){
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object o){
            if (!(o instanceof Point)) return false;
            Point p = (Point) o;
            return x == p.x && y == p.y;
        }

        @Override
        public int hashCode(){
            return Objects.hash(x, y);
        }

        @Override
        public String toString(){
            return "(" + x + "," + y + ")";
        }
    }

    private class State{
        int playerX, playerY;
        Point[] boxes; // Use array instead of ArrayList for efficiency
        String moves;
        int g, h;
        // OPTIMIZATION: Cache hash for 8-box performance
        private int hash = -1;

        public State(int px, int py, ArrayList<Point> boxes, String moves, int g, int h){
            this.playerX = px;
            this.playerY = py;
            // Convert to sorted array for efficiency
            this.boxes = boxes.toArray(new Point[0]);
            Arrays.sort(this.boxes, new Comparator<Point>() {
                @Override
                public int compare(Point a, Point b) {
                    if (a.x == b.x) {
                        return a.y - b.y;
                    }
                    return a.x - b.x;
                }
            });
            this.moves = moves;
            this.g = g;
            this.h = h;
        }

        int f(){
            return g + h;
        }

        @Override
        public boolean equals(Object o){
            if (!(o instanceof State)) return false;
            State s = (State) o;
            if (playerX != s.playerX || playerY != s.playerY) return false;
            return Arrays.equals(boxes, s.boxes);
        }

        @Override
        public int hashCode(){
            if (hash == -1) {
                int result = Objects.hash(playerX, playerY);
                result = 31 * result + Arrays.hashCode(boxes);
                hash = result;
            }
            return hash;
        }
    }

    /**
     * Determines if every box has reached its goal
     */
    private boolean isGoal(State s, ArrayList goals){
        for (Point box : s.boxes)
            if (!goals.contains(box))
                return false;
        return true;
    }

    // OPTIMIZED: Better heuristic using adaptive approach with 8-box specialization
    private int heuristic(State s, ArrayList<Point> goals){
        if (s.boxes.length >= 8) {
            return optimizedHeuristicFor8Boxes(s, goals);
        } else if (s.boxes.length >= 6) {
            return improvedAdvancedHeuristic(s, goals);
        } else {
            return improvedBasicHeuristic(s, goals);
        }
    }

    // IMPROVED: Basic heuristic for simpler puzzles
    private int improvedBasicHeuristic(State s, ArrayList<Point> goals){
        int total = 0;
        int boxesOnGoals = 0;

        for (Point b : s.boxes){
            int minDist = Integer.MAX_VALUE;
            boolean onGoal = false;

            for (Point g : goals){
                int dist = Math.abs(b.x - g.x) + Math.abs(b.y - g.y);
                if (dist < minDist){
                    minDist = dist;
                }
                if (b.x == g.x && b.y == g.y){
                    onGoal = true;
                    boxesOnGoals++;
                }
            }

            if (!onGoal){
                total += minDist * 3; // Increased weight
            }
        }

        // Stronger penalty for boxes not on goals
        total += (s.boxes.length - boxesOnGoals) * 25;
        return total;
    }

    // IMPROVED: Advanced heuristic for complex puzzles (6+ boxes)
    private int improvedAdvancedHeuristic(State s, ArrayList<Point> goals){
        int total = 0;
        int boxesOnGoals = 0;

        // First count boxes already on goals and properly assign goals
        boolean[] goalAssigned = new boolean[goals.size()];

        // Count boxes on goals
        for (Point box : s.boxes) {
            for (int i = 0; i < goals.size(); i++) {
                if (box.equals(goals.get(i))) {
                    boxesOnGoals++;
                    goalAssigned[i] = true;
                    break;
                }
            }
        }

        // For boxes not on goals, use improved assignment
        List<Point> unassignedBoxes = new ArrayList<>();
        for (Point box : s.boxes) {
            boolean onGoal = false;
            for (Point goal : goals) {
                if (box.equals(goal)) {
                    onGoal = true;
                    break;
                }
            }
            if (!onGoal) {
                unassignedBoxes.add(box);
            }
        }

        // Sort boxes by difficulty (furthest from any unassigned goal first)
        Collections.sort(unassignedBoxes, new Comparator<Point>() {
            @Override
            public int compare(Point a, Point b) {
                int minDistA = Integer.MAX_VALUE;
                int minDistB = Integer.MAX_VALUE;
                for (int i = 0; i < goals.size(); i++) {
                    if (!goalAssigned[i]) {
                        Point g = goals.get(i);
                        minDistA = Math.min(minDistA, Math.abs(a.x - g.x) + Math.abs(a.y - g.y));
                        minDistB = Math.min(minDistB, Math.abs(b.x - g.x) + Math.abs(b.y - g.y));
                    }
                }
                // Sort in descending order (hardest boxes first)
                return Integer.compare(minDistB, minDistA);
            }
        });

        // Assign boxes to closest available goals
        boolean[] tempGoalUsed = goalAssigned.clone();
        for (Point box : unassignedBoxes) {
            int minDist = Integer.MAX_VALUE;
            for (int i = 0; i < goals.size(); i++) {
                if (!tempGoalUsed[i]) {
                    Point g = goals.get(i);
                    int dist = Math.abs(box.x - g.x) + Math.abs(box.y - g.y);
                    if (dist < minDist) {
                        minDist = dist;
                    }
                }
            }
            if (minDist != Integer.MAX_VALUE) {
                total += minDist;
                // Mark the closest goal as used
                for (int i = 0; i < goals.size(); i++) {
                    if (!tempGoalUsed[i]) {
                        Point g = goals.get(i);
                        if (Math.abs(box.x - g.x) + Math.abs(box.y - g.y) == minDist) {
                            tempGoalUsed[i] = true;
                            break;
                        }
                    }
                }
            }
        }

        // Much stronger penalty for boxes not on goals for complex puzzles
        total += (s.boxes.length - boxesOnGoals) * 35;

        return total;
    }

    // NEW: Optimized heuristic specifically for 8-box maps
    private int optimizedHeuristicFor8Boxes(State s, ArrayList<Point> goals) {
        int total = 0;
        int boxesOnGoals = 0;

        // Use your original advanced heuristic logic but with optimizations
        boolean[] goalAssigned = new boolean[goals.size()];

        // Count boxes on goals
        for (Point box : s.boxes) {
            for (int i = 0; i < goals.size(); i++) {
                if (box.equals(goals.get(i))) {
                    boxesOnGoals++;
                    goalAssigned[i] = true;
                    break;
                }
            }
        }

        // For boxes not on goals, use simplified assignment (faster for 8 boxes)
        for (Point box : s.boxes) {
            boolean onGoal = false;
            for (Point goal : goals) {
                if (box.equals(goal)) {
                    onGoal = true;
                    break;
                }
            }
            if (!onGoal) {
                // Fast Manhattan distance with simple goal assignment
                int minDist = Integer.MAX_VALUE;
                for (int i = 0; i < goals.size(); i++) {
                    if (!goalAssigned[i]) {
                        Point g = goals.get(i);
                        int dist = Math.abs(box.x - g.x) + Math.abs(box.y - g.y);
                        if (dist < minDist) {
                            minDist = dist;
                        }
                    }
                }
                if (minDist != Integer.MAX_VALUE) {
                    total += minDist;
                    // Simple goal assignment - use first available
                    for (int i = 0; i < goals.size(); i++) {
                        if (!goalAssigned[i]) {
                            goalAssigned[i] = true;
                            break;
                        }
                    }
                }
            }
        }

        // Use the same strong penalty as your advanced heuristic
        total += (s.boxes.length - boxesOnGoals) * 35;
        return total;
    }

    // OPTIMIZED: More efficient deadlock detection
    private boolean hasDeadlock(State s, boolean[][] deadlockMap, ArrayList<Point> goals, int width, int height, char[][] mapData){
        for (Point box : s.boxes) {
            // Basic corner deadlock
            if (deadlockMap[box.y][box.x]){
                boolean isGoal = false;
                for (Point g : goals) {
                    if (box.equals(g)){
                        isGoal = true;
                        break;
                    }
                }
                if (!isGoal) {
                    return true;
                }
            }

            // Check for frozen boxes (boxes that can't be moved)
            if (isBoxFrozen(box, s, width, height, mapData, goals)) {
                return true;
            }
        }
        return false;
    }

    // OPTIMIZED: Check if a box is frozen with early termination
    private boolean isBoxFrozen(Point box, State s, int width, int height, char[][] mapData, ArrayList<Point> goals){
        // If box is on goal, it's not frozen
        for (Point g : goals){
            if (box.equals(g)) return false;
        }

        // Check all four directions with early termination
        int[][] directions = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};

        for (int i = 0; i < 4; i++){
            int dx = directions[i][0];
            int dy = directions[i][1];

            // Check if box can be pushed in this direction
            int newX = box.x + dx;
            int newY = box.y + dy;
            int behindX = box.x - dx;
            int behindY = box.y - dy;

            // Check bounds and walls
            if (newX < 0 || newX >= width || newY < 0 || newY >= height) continue;
            if (behindX < 0 || behindX >= width || behindY < 0 || behindY >= height) continue;
            if (mapData[newY][newX] == '#' || mapData[behindY][behindX] == '#') continue;

            // Check for other boxes
            boolean boxBlocking = false;
            for (Point other : s.boxes){
                if (other.x == newX && other.y == newY){
                    boxBlocking = true;
                    break;
                }
            }
            if (!boxBlocking) {
                return false; // Found at least one movable direction
            }
        }

        return true; // No movable directions found
    }

    // OPTIMIZED: More efficient player reachability with box position caching
    private boolean playerCanReach(State s, Point target, int width, int height, char[][] mapData){
        if (s.playerX == target.x && s.playerY == target.y) {
            return true;
        }

        boolean[][] visited = new boolean[height][width];
        Queue<Point> queue = new LinkedList<>();
        queue.add(new Point(s.playerX, s.playerY));
        visited[s.playerY][s.playerX] = true;

        int[] dx = {0, 0, -1, 1};
        int[] dy = {-1, 1, 0, 0};

        // OPTIMIZATION: Precompute box positions for faster checking
        boolean[][] boxPositions = new boolean[height][width];
        for (Point box : s.boxes) {
            boxPositions[box.y][box.x] = true;
        }

        while (!queue.isEmpty()){
            Point current = queue.poll();

            for (int i = 0; i < 4; i++){
                int nx = current.x + dx[i];
                int ny = current.y + dy[i];

                if (nx < 0 || nx >= width || ny < 0 || ny >= height){
                    continue;
                }
                if (visited[ny][nx]){
                    continue;
                }
                if (mapData[ny][nx] == '#'){
                    continue;
                }

                // Check for boxes using precomputed array (much faster)
                if (boxPositions[ny][nx]) continue;

                if (nx == target.x && ny == target.y) return true;

                visited[ny][nx] = true;
                queue.add(new Point(nx, ny));
            }
        }

        return false;
    }

    public String solveSokobanPuzzle(int width, int height, char[][] mapData, char[][] itemsData){
        // Directions (u, d, l, r)
        int[] dx = {0, 0, -1, 1};
        int[] dy = {-1, 1, 0, 0};
        char[] dirChar = {'u', 'd', 'l', 'r'};

        // Store the coordinates of the initial position of the player, boxes, and goals
        int startX = -1, startY = -1;
        ArrayList<Point> goals = new ArrayList<>();
        ArrayList<Point> startBoxes = new ArrayList<>();

        // Read the map and items data
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++){
                if (mapData[y][x] == '.') {
                    goals.add(new Point(x, y));
                }

                char item = itemsData[y][x];
                if (item == '@' || item == '+'){
                    startX = x;
                    startY = y;
                }
                if (item == '$' || item == '*'){
                    startBoxes.add(new Point(x, y));
                }
            }
        }

        // Print positions for debugging
        System.out.println("Start: (" + startX + "," + startY + ")");
        System.out.println("Boxes: " + startBoxes.size() + " - " + startBoxes);
        System.out.println("Goals: " + goals.size() + " - " + goals);

        // Deadlock detection map
        boolean[][] deadlockMap = new boolean[height][width];

        // Deadlock detection algorithm with bounds checking
        for (int y = 0; y < height; y++){
            for (int x = 0; x < width; x++){
                if (mapData[y][x] == '#') continue;

                // Check if this position is a corner deadlock
                boolean deadlock = false;

                // Check all four possible corner configurations with bounds checking
                if (y > 0 && x > 0 && mapData[y-1][x] == '#' && mapData[y][x-1] == '#'){
                    deadlock = true;
                }
                else if (y > 0 && x < width-1 && mapData[y-1][x] == '#' && mapData[y][x+1] == '#'){
                    deadlock = true;
                }
                else if (y < height-1 && x > 0 && mapData[y+1][x] == '#' && mapData[y][x-1] == '#'){
                    deadlock = true;
                }
                else if (y < height-1 && x < width-1 && mapData[y+1][x] == '#' && mapData[y][x+1] == '#'){
                    deadlock = true;
                }

                if (deadlock) {
                    // Check if this position is a goal (boxes on goals are never deadlocks)
                    boolean isGoal = false;
                    for (Point g : goals) {
                        if (g.x == x && g.y == y) {
                            isGoal = true;
                            break;
                        }
                    }
                    if (!isGoal) {
                        deadlockMap[y][x] = true;
                    }
                }
            }
        }

        // A* search with priority queue
        PriorityQueue<State> open = new PriorityQueue<>(new Comparator<State>(){
            @Override
            public int compare(State s1, State s2){
                return Integer.compare(s1.f(), s2.f());
            }
        });
        Set<State> closed = new HashSet<>();

        State start = new State(startX, startY, startBoxes, "", 0, 0);
        start.h = heuristic(start, goals);
        open.add(start);

        int statesExplored = 0;
        // OPTIMIZATION: Increase state limit for 8-box maps
        int maxStates = 2000000;

        while (!open.isEmpty() && statesExplored < maxStates){
            State cur = open.poll();
            statesExplored++;

            // Progress reporting
            if (statesExplored % 50000 == 0){
                System.out.println("Progress: " + statesExplored + " states explored, " +
                        open.size() + " in queue, current depth: " + cur.moves.length());
            }

            if (isGoal(cur, goals)){
                System.out.println("Solution found with " + cur.moves.length() + " moves!");
                System.out.println("Total states explored: " + statesExplored);
                return cur.moves;
            }

            if (closed.contains(cur)){
                continue;
            }

            closed.add(cur);

            // More aggressive deadlock detection
            if (hasDeadlock(cur, deadlockMap, goals, width, height, mapData)){
                continue;
            }

            for (int i = 0; i < 4; i++){
                int px = cur.playerX;
                int py = cur.playerY;
                int nx = px + dx[i];
                int ny = py + dy[i];

                if (nx < 0 || nx >= width || ny < 0 || ny >= height){
                    continue;
                }
                if (mapData[ny][nx] == '#'){
                    continue;
                }

                boolean pushed = false;
                ArrayList<Point> newBoxes = new ArrayList<>();
                Point pushedBox = null;

                for (Point b : cur.boxes){
                    if (b.x == nx && b.y == ny){
                        int bx2 = b.x + dx[i];
                        int by2 = b.y + dy[i];

                        if (bx2 < 0 || bx2 >= width || by2 < 0 || by2 >= height){
                            continue;
                        }
                        if (mapData[by2][bx2] == '#'){
                            continue;
                        }

                        boolean blocked = false;
                        for (Point otherB : cur.boxes){
                            if (otherB.x == bx2 && otherB.y == by2){
                                blocked = true;
                                break;
                            }
                        }

                        if (blocked){
                            continue;
                        }

                        if (deadlockMap[by2][bx2]){
                            boolean isGoalPos = false;
                            for (Point g : goals){
                                if (g.x == bx2 && g.y == by2){
                                    isGoalPos = true;
                                    break;
                                }
                            }
                            if (!isGoalPos){
                                continue;
                            }
                        }

                        int behindX = b.x - dx[i];
                        int behindY = b.y - dy[i];

                        if (behindX < 0 || behindX >= width || behindY < 0 || behindY >= height){
                            continue;
                        }

                        if (mapData[behindY][behindX] == '#'){
                            continue;
                        }

                        blocked = false;
                        for (Point otherB : cur.boxes){
                            if (otherB.x == behindX && otherB.y == behindY){
                                blocked = true;
                                break;
                            }
                        }

                        if (blocked){
                            continue;
                        }

                        Point pushPos = new Point(behindX, behindY);
                        if (!playerCanReach(cur, pushPos, width, height, mapData)){
                            continue;
                        }

                        pushedBox = new Point(bx2, by2);
                        pushed = true;
                        break;
                    }
                }

                if (pushed){
                    for (Point b : cur.boxes){
                        if (b.x == nx && b.y == ny){
                            newBoxes.add(pushedBox);
                        }
                        else{
                            newBoxes.add(new Point(b.x, b.y));
                        }
                    }
                }
                else{
                    boolean movingIntoBox = false;
                    for (Point b : cur.boxes){
                        if (b.x == nx && b.y == ny){
                            movingIntoBox = true;
                            break;
                        }
                    }
                    if (movingIntoBox){
                        continue;
                    }

                    for (Point b : cur.boxes){
                        newBoxes.add(new Point(b.x, b.y));
                    }
                }

                State next = new State(nx, ny, newBoxes, cur.moves + dirChar[i], cur.g + 1, 0);
                next.h = heuristic(next, goals);

                if (!closed.contains(next)){
                    open.add(next);
                }
            }
        }

        System.out.println("No solution found. States explored: " + statesExplored);
        if (statesExplored >= maxStates) {
            System.out.println("Reached state exploration limit for 8-box map");
        }
        return "";
    }
}