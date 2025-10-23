package solver;

import java.util.*;

public class SokoBot {
    private class Point {
        int x, y;
        public Point(int x, int y) { 
            this.x = x; 
            this.y = y; 
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Point)) return false;
            Point p = (Point) o;
            return x == p.x && y == p.y;
        }

        @Override
        public int hashCode() { 
            return Objects.hash(x, y);
        }

        @Override
        public String toString() { 
            return "(" + x + "," + y + ")";
        }
    }

    private class State {
        int playerX, playerY;
        Point[] boxes;
        String moves;
        int h; // heuristic only (GBFS ignores path cost)
        private int hash = -1;

        public State(int px, int py, ArrayList<Point> boxes, String moves, int h) {
            this.playerX = px;
            this.playerY = py;
            this.boxes = boxes.toArray(new Point[0]);
            Arrays.sort(this.boxes, Comparator.comparingInt((Point a) -> a.x * 10000 + a.y));
            this.moves = moves;
            this.h = h;
        }

        int f() { 
            return h; // GBFS uses only heuristic
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof State)) return false;
            State s = (State) o;
            if (playerX != s.playerX || playerY != s.playerY) return false;
            return Arrays.equals(boxes, s.boxes);
        }

        @Override
        public int hashCode() {
            if (hash == -1) {
                int result = Objects.hash(playerX, playerY);
                result = 31 * result + Arrays.hashCode(boxes);
                hash = result;
            }
            return hash;
        }
    }

    /** Checks if every box is on a goal */
    private boolean isGoal(State s, ArrayList<Point> goals) {
        for (Point box : s.boxes)
            if (!goals.contains(box))
                return false;
        return true;
    }

    /** GBFS still uses your advanced heuristic system */
    private int heuristic(State s, ArrayList<Point> goals) {
        if (s.boxes.length >= 8) {
            return optimizedHeuristicFor8Boxes(s, goals);
        } else if (s.boxes.length >= 6) {
            return improvedAdvancedHeuristic(s, goals);
        } else {
            return improvedBasicHeuristic(s, goals);
        }
    }

    private int improvedBasicHeuristic(State s, ArrayList<Point> goals) {
        int total = 0;
        int boxesOnGoals = 0;

        for (Point b : s.boxes) {
            int minDist = Integer.MAX_VALUE;
            boolean onGoal = false;

            for (Point g : goals) {
                int dist = Math.abs(b.x - g.x) + Math.abs(b.y - g.y);
                if (dist < minDist) {
                    minDist = dist;
                }
                if (b.x == g.x && b.y == g.y) {
                    onGoal = true;
                    boxesOnGoals++;
                }
            }

            if (!onGoal) {
                total += minDist * 3;
            }
        }

        total += (s.boxes.length - boxesOnGoals) * 25;
        return total;
    }

    private int improvedAdvancedHeuristic(State s, ArrayList<Point> goals) {
        int total = 0;
        int boxesOnGoals = 0;
        boolean[] goalAssigned = new boolean[goals.size()];

        for (Point box : s.boxes) {
            for (int i = 0; i < goals.size(); i++) {
                if (box.equals(goals.get(i))) {
                    boxesOnGoals++;
                    goalAssigned[i] = true;
                    break;
                }
            }
        }

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

        Collections.sort(unassignedBoxes, (a, b) -> {
            int minDistA = Integer.MAX_VALUE;
            int minDistB = Integer.MAX_VALUE;
            for (int i = 0; i < goals.size(); i++) {
                if (!goalAssigned[i]) {
                    Point g = goals.get(i);
                    minDistA = Math.min(minDistA, Math.abs(a.x - g.x) + Math.abs(a.y - g.y));
                    minDistB = Math.min(minDistB, Math.abs(b.x - g.x) + Math.abs(b.y - g.y));
                }
            }
            return Integer.compare(minDistB, minDistA);
        });

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

        total += (s.boxes.length - boxesOnGoals) * 35;
        return total;
    }

    private int optimizedHeuristicFor8Boxes(State s, ArrayList<Point> goals) {
        int total = 0;
        int boxesOnGoals = 0;
        boolean[] goalAssigned = new boolean[goals.size()];

        for (Point box : s.boxes) {
            for (int i = 0; i < goals.size(); i++) {
                if (box.equals(goals.get(i))) {
                    boxesOnGoals++;
                    goalAssigned[i] = true;
                    break;
                }
            }
        }

        for (Point box : s.boxes) {
            boolean onGoal = false;
            for (Point goal : goals) {
                if (box.equals(goal)) {
                    onGoal = true;
                    break;
                }
            }
            if (!onGoal) {
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
                    for (int i = 0; i < goals.size(); i++) {
                        if (!goalAssigned[i]) {
                            goalAssigned[i] = true;
                            break;
                        }
                    }
                }
            }
        }

        total += (s.boxes.length - boxesOnGoals) * 35;
        return total;
    }

    private boolean hasDeadlock(State s, boolean[][] deadlockMap, ArrayList<Point> goals, int width, int height, char[][] mapData) {
        for (Point box : s.boxes) {
            if (deadlockMap[box.y][box.x]) {
                boolean isGoal = false;
                for (Point g : goals) {
                    if (box.equals(g)) {
                        isGoal = true;
                        break;
                    }
                }
                if (!isGoal) return true;
            }
            if (isBoxFrozen(box, s, width, height, mapData, goals)) return true;
        }
        return false;
    }

    private boolean isBoxFrozen(Point box, State s, int width, int height, char[][] mapData, ArrayList<Point> goals) {
        for (Point g : goals) if (box.equals(g)) return false;

        int[][] directions = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
        for (int[] d : directions) {
            int dx = d[0], dy = d[1];
            int newX = box.x + dx, newY = box.y + dy;
            int behindX = box.x - dx, behindY = box.y - dy;

            if (newX < 0 || newX >= width || newY < 0 || newY >= height) continue;
            if (behindX < 0 || behindX >= width || behindY < 0 || behindY >= height) continue;
            if (mapData[newY][newX] == '#' || mapData[behindY][behindX] == '#') continue;

            boolean blocked = false;
            for (Point other : s.boxes)
                if (other.x == newX && other.y == newY)
                    blocked = true;
            if (!blocked) return false;
        }
        return true;
    }

    private boolean playerCanReach(State s, Point target, int width, int height, char[][] mapData) {
        if (s.playerX == target.x && s.playerY == target.y) return true;

        boolean[][] visited = new boolean[height][width];
        Queue<Point> queue = new LinkedList<>();
        queue.add(new Point(s.playerX, s.playerY));
        visited[s.playerY][s.playerX] = true;

        int[] dx = {0, 0, -1, 1};
        int[] dy = {-1, 1, 0, 0};
        boolean[][] boxPos = new boolean[height][width];
        for (Point b : s.boxes) boxPos[b.y][b.x] = true;

        while (!queue.isEmpty()) {
            Point cur = queue.poll();
            for (int i = 0; i < 4; i++) {
                int nx = cur.x + dx[i];
                int ny = cur.y + dy[i];
                if (nx < 0 || nx >= width || ny < 0 || ny >= height) continue;
                if (visited[ny][nx] || mapData[ny][nx] == '#') continue;
                if (boxPos[ny][nx]) continue;
                if (nx == target.x && ny == target.y) return true;
                visited[ny][nx] = true;
                queue.add(new Point(nx, ny));
            }
        }
        return false;
    }

    public String solveSokobanPuzzle(int width, int height, char[][] mapData, char[][] itemsData) {
        int[] dx = {0, 0, -1, 1};
        int[] dy = {-1, 1, 0, 0};
        char[] dirChar = {'u', 'd', 'l', 'r'};

        int startX = -1, startY = -1;
        ArrayList<Point> goals = new ArrayList<>();
        ArrayList<Point> startBoxes = new ArrayList<>();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (mapData[y][x] == '.') goals.add(new Point(x, y));
                char item = itemsData[y][x];
                if (item == '@' || item == '+') { startX = x; startY = y; }
                if (item == '$' || item == '*') startBoxes.add(new Point(x, y));
            }
        }

        boolean[][] deadlockMap = new boolean[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (mapData[y][x] == '#') continue;
                boolean deadlock = false;
                if (y > 0 && x > 0 && mapData[y-1][x] == '#' && mapData[y][x-1] == '#') deadlock = true;
                else if (y > 0 && x < width-1 && mapData[y-1][x] == '#' && mapData[y][x+1] == '#') deadlock = true;
                else if (y < height-1 && x > 0 && mapData[y+1][x] == '#' && mapData[y][x-1] == '#') deadlock = true;
                else if (y < height-1 && x < width-1 && mapData[y+1][x] == '#' && mapData[y][x+1] == '#') deadlock = true;

                boolean isGoal = goals.stream().anyMatch(g -> g.x == x && g.y == y);
                if (deadlock && !isGoal) deadlockMap[y][x] = true;
            }
        }

        PriorityQueue<State> open = new PriorityQueue<>(Comparator.comparingInt(s -> s.h));
        Set<State> closed = new HashSet<>();

        State start = new State(startX, startY, startBoxes, "", 0);
        start.h = heuristic(start, goals);
        open.add(start);

        int statesExplored = 0;
        int maxStates = 2000000;

        while (!open.isEmpty() && statesExplored < maxStates) {
            State cur = open.poll();
            statesExplored++;

            if (statesExplored % 50000 == 0) {
                System.out.println("Progress: " + statesExplored + " explored, queue=" + open.size());
            }

            if (isGoal(cur, goals)) {
                System.out.println("Solution found in " + cur.moves.length() + " moves!");
                System.out.println("Total states explored: " + statesExplored);
                return cur.moves;
            }

            if (closed.contains(cur)) continue;
            closed.add(cur);
            if (hasDeadlock(cur, deadlockMap, goals, width, height, mapData)) continue;

            for (int i = 0; i < 4; i++) {
                int nx = cur.playerX + dx[i];
                int ny = cur.playerY + dy[i];

                if (nx < 0 || nx >= width || ny < 0 || ny >= height || mapData[ny][nx] == '#') continue;

                boolean pushed = false;
                ArrayList<Point> newBoxes = new ArrayList<>();
                Point pushedBox = null;

                for (Point b : cur.boxes) {
                    if (b.x == nx && b.y == ny) {
                        int bx2 = b.x + dx[i], by2 = b.y + dy[i];
                        if (bx2 < 0 || bx2 >= width || by2 < 0 || by2 >= height) continue;
                        if (mapData[by2][bx2] == '#') continue;
                        boolean blocked = false;
                        for (Point otherB : cur.boxes)
                            if (otherB.x == bx2 && otherB.y == by2)
                                blocked = true;
                        if (blocked) continue;
                        if (deadlockMap[by2][bx2] && goals.stream().noneMatch(g -> g.x == bx2 && g.y == by2)) continue;

                        Point pushPos = new Point(b.x - dx[i], b.y - dy[i]);
                        if (!playerCanReach(cur, pushPos, width, height, mapData)) continue;

                        pushedBox = new Point(bx2, by2);
                        pushed = true;
                        break;
                    }
                }

                if (pushed) {
                    for (Point b : cur.boxes) {
                        if (b.x == nx && b.y == ny) newBoxes.add(pushedBox);
                        else newBoxes.add(new Point(b.x, b.y));
                    }
                } else {
                    boolean intoBox = false;
                    for (Point b : cur.boxes)
                        if (b.x == nx && b.y == ny)
                            intoBox = true;
                    if (intoBox) continue;
                    for (Point b : cur.boxes)
                        newBoxes.add(new Point(b.x, b.y));
                }

                State next = new State(nx, ny, newBoxes, cur.moves + dirChar[i], 0);
                next.h = heuristic(next, goals);
                if (!closed.contains(next)) open.add(next);
            }
        }

        System.out.println("No solution found. States explored: " + statesExplored);
        if (statesExplored >= maxStates) {
            System.out.println("Reached state exploration limit for 8-box map");
        }
        return "";
    }
}
