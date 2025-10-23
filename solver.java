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
		Point[] boxes; // Use array instead of ArrayList for efficiency
		String moves;
		int g, h;
		// OPTIMIZATION: Cache hash for 8-box performance
		private int hash = -1;

		public State(int px, int py, ArrayList<Point> boxes, String moves, int g, int h) {
			this.playerX = px;
			this.playerY = py;
			// Convert to sorted array for efficiency
			this.boxes = boxes.toArray(new Point[0]);
			Arrays.sort(this.boxes, Comparator.comparingInt((Point a) -> a.x).thenComparingInt((Point a) -> a.y));
			this.moves = moves;
			this.g = g;
			this.h = h;
		}

		int f() {
			return g + h;
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

	/**
	 * Determines if every box has reached its goal
	 */
	private boolean isGoal(State s, ArrayList<Point> goals) {
		for (Point box : s.boxes)
			if (!goals.contains(box))
				return false;
		return true;
	}

	// OPTIMIZED: Better heuristic using adaptive approach
	private int heuristic(State s, ArrayList<Point> goals) {
		if (s.boxes.length >= 8) {
			// Use the new heuristic tailored for GBFS on complex maps
			return gbfsHeuristicFor8Boxes(s, goals);
		} else if (s.boxes.length >= 6) {
			return improvedAdvancedHeuristic(s, goals);
		} else {
			return improvedBasicHeuristic(s, goals);
		}
	}

	// IMPROVED: Basic heuristic for simpler puzzles
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
				total += minDist * 3; // Increased weight
			}
		}

		// Stronger penalty for boxes not on goals
		total += (s.boxes.length - boxesOnGoals) * 25;
		return total;
	}

	// IMPROVED: Advanced heuristic for complex puzzles (6+ boxes)
	private int improvedAdvancedHeuristic(State s, ArrayList<Point> goals) {
		int total = 0;
		int boxesOnGoals = 0;
		boolean[] goalAssigned = new boolean[goals.size()];
		List<Point> unassignedBoxes = new ArrayList<>();

		// Identify boxes on goals and unassigned boxes
		for (Point box : s.boxes) {
			boolean onGoal = false;
			for (int i = 0; i < goals.size(); i++) {
				if (box.equals(goals.get(i))) {
					boxesOnGoals++;
					goalAssigned[i] = true;
					onGoal = true;
					break;
				}
			}
			if (!onGoal) {
				unassignedBoxes.add(box);
			}
		}

		// Create a list of available goals
		List<Point> availableGoals = new ArrayList<>();
		for (int i = 0; i < goals.size(); i++) {
			if (!goalAssigned[i]) {
				availableGoals.add(goals.get(i));
			}
		}

		// Greedily match each unassigned box to its closest available goal
		for (Point box : unassignedBoxes) {
			int minDist = Integer.MAX_VALUE;
			Point bestGoal = null;
			for (Point goal : availableGoals) {
				int dist = Math.abs(box.x - goal.x) + Math.abs(box.y - goal.y);
				if (dist < minDist) {
					minDist = dist;
					bestGoal = goal;
				}
			}
			if (bestGoal != null) {
				total += minDist;
				availableGoals.remove(bestGoal); // This goal is now "taken"
			}
		}

		// Strong penalty for boxes not on goals
		total += (s.boxes.length - boxesOnGoals) * 35;
		return total;
	}

	// NEW & REFINED: Heuristic for 8-box puzzles, optimized for GBFS.
	// This heuristic calculates the sum of minimum Manhattan distances for each box
	// to its closest available goal. It's a fast and effective way to estimate
	// how close a given state is to the solution.
	private int gbfsHeuristicFor8Boxes(State s, ArrayList<Point> goals) {
		int totalDistance = 0;

		List<Point> unplacedBoxes = new ArrayList<>();
		List<Point> availableGoals = new ArrayList<>(goals);

		// First, remove boxes that are already on goals and the goals they occupy
		for (Point box : s.boxes) {
			if (availableGoals.contains(box)) {
				unplacedBoxes.remove(box);
				availableGoals.remove(box);
			} else {
				unplacedBoxes.add(box);
			}
		}

		// For each unplaced box, find the minimum distance to an available goal
		// and sum these distances up. This is a "minimum matching distance" heuristic.
		for(Point box : unplacedBoxes) {
			int minBoxDistance = Integer.MAX_VALUE;
			for (Point goal : availableGoals) {
				int dist = Math.abs(box.x - goal.x) + Math.abs(box.y - goal.y);
				if (dist < minBoxDistance) {
					minBoxDistance = dist;
				}
			}
			totalDistance += minBoxDistance;
		}

		return totalDistance;
	}

	// OPTIMIZED: More efficient deadlock detection
	private boolean hasDeadlock(State s, boolean[][] deadlockMap, ArrayList<Point> goals, int width, int height, char[][] mapData){
		for (Point box : s.boxes) {
			// Basic corner deadlock check
			if (deadlockMap[box.y][box.x]){
				// A box in a deadlock corner is only okay if that corner IS a goal
				if (!goals.contains(box)) {
					return true;
				}
			}
			// Advanced check for frozen boxes (more computationally expensive)
			if (isBoxFrozen(box, s, width, height, mapData, goals)) {
				return true;
			}
		}
		return false;
	}

	// OPTIMIZED: Check if a box is frozen along a wall
	private boolean isBoxFrozen(Point box, State s, int width, int height, char[][] mapData, ArrayList<Point> goals) {
		if (goals.contains(box)) return false; // Boxes on goals are never frozen

		boolean blockedVert = (mapData[box.y - 1][box.x] == '#' || mapData[box.y + 1][box.x] == '#');
		boolean blockedHoriz = (mapData[box.y][box.x - 1] == '#' || mapData[box.y][box.x + 1] == '#');

		if (blockedVert && blockedHoriz) return true; // Simple corner check handled by deadlockMap, but good to have

		// Wall-based deadlock: if a box is against a wall, check if all goal positions
		// along that wall are already occupied by other boxes.
		if (blockedVert) {
			boolean canMoveHoriz = false;
			for (Point otherBox : s.boxes) {
				if (otherBox.y == box.y) { // check boxes on the same row
					if (mapData[otherBox.y][otherBox.x-1] != '#' && mapData[otherBox.y][otherBox.x+1] != '#') {
						canMoveHoriz = true;
						break;
					}
				}
			}
			if (!canMoveHoriz) {
				// If it can't move horizontally, check if there's an open goal along this wall
				boolean goalAvailableOnWall = false;
				for (Point goal : goals) {
					if (goal.y == box.y && !s.boxes.toString().contains(goal.toString())) {
						goalAvailableOnWall = true;
						break;
					}
				}
				if (!goalAvailableOnWall) return true;
			}
		}
		// Similar check for horizontal blockage
		if (blockedHoriz) {
			boolean canMoveVert = false;
			for (Point otherBox : s.boxes) {
				if(otherBox.x == box.x) { // check boxes on same col
					if(mapData[otherBox.y-1][otherBox.x] != '#' && mapData[otherBox.y+1][otherBox.x] != '#') {
						canMoveVert = true;
						break;
					}
				}
			}
			if (!canMoveVert) {
				boolean goalAvailableOnWall = false;
				for (Point goal : goals) {
					if (goal.x == box.x && !s.boxes.toString().contains(goal.toString())) {
						goalAvailableOnWall = true;
						break;
					}
				}
				if (!goalAvailableOnWall) return true;
			}
		}

		return false;
	}

	// OPTIMIZED: More efficient player reachability with box position caching
	private boolean playerCanReach(State s, Point target, int width, int height, char[][] mapData) {
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

		while (!queue.isEmpty()) {
			Point current = queue.poll();

			for (int i = 0; i < 4; i++) {
				int nx = current.x + dx[i];
				int ny = current.y + dy[i];

				if (nx < 0 || nx >= width || ny < 0 || ny >= height || visited[ny][nx] || mapData[ny][nx] == '#') {
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

	/**
	 * The original A* search algorithm.
	 * Finds an optimal (shortest) solution.
	 */
	public String solveSokobanPuzzle(int width, int height, char[][] mapData, char[][] itemsData) {
		// Initialization code is identical for both algorithms
		Map<String, Object> init = initializeSolver(width, height, mapData, itemsData);
		if (init == null) return "";

		int startX = (int) init.get("startX");
		int startY = (int) init.get("startY");
		ArrayList<Point> goals = (ArrayList<Point>) init.get("goals");
		ArrayList<Point> startBoxes = (ArrayList<Point>) init.get("startBoxes");
		boolean[][] deadlockMap = (boolean[][]) init.get("deadlockMap");

		// A* search with priority queue
		PriorityQueue<State> open = new PriorityQueue<>(Comparator.comparingInt(State::f));
		Set<State> closed = new HashSet<>();

		State start = new State(startX, startY, startBoxes, "", 0, 0);
		start.h = heuristic(start, goals);
		open.add(start);

		return searchLoop(open, closed, goals, deadlockMap, width, height, mapData, 2000000);
	}

	// ####################################################################################
	// ## NEW GREEDY BEST-FIRST SEARCH (GBFS) IMPLEMENTATION
	// ####################################################################################
	/**
	 * Solves the Sokoban puzzle using the Greedy Best-First Search (GBFS) algorithm.
	 * This algorithm is often faster than A* but does not guarantee the shortest solution.
	 * It prioritizes states that appear to be closest to the goal, regardless of the path taken.
	 */
	public String solveWithGBFS(int width, int height, char[][] mapData, char[][] itemsData) {
		// Initialization is the same as A*
		Map<String, Object> init = initializeSolver(width, height, mapData, itemsData);
		if (init == null) return "";

		int startX = (int) init.get("startX");
		int startY = (int) init.get("startY");
		ArrayList<Point> goals = (ArrayList<Point>) init.get("goals");
		ArrayList<Point> startBoxes = (ArrayList<Point>) init.get("startBoxes");
		boolean[][] deadlockMap = (boolean[][]) init.get("deadlockMap");

		// *** KEY DIFFERENCE FOR GBFS ***
		// The PriorityQueue compares states based ONLY on the heuristic (h value).
		PriorityQueue<State> open = new PriorityQueue<>(Comparator.comparingInt(s -> s.h));
		Set<State> closed = new HashSet<>();

		State start = new State(startX, startY, startBoxes, "", 0, 0);
		start.h = heuristic(start, goals); // Calculate initial heuristic
		open.add(start);

		// Use the same search loop logic, but with the GBFS-configured queue
		return searchLoop(open, closed, goals, deadlockMap, width, height, mapData, 2000000);
	}

	// Helper method to initialize puzzle state from map data
	private Map<String, Object> initializeSolver(int width, int height, char[][] mapData, char[][] itemsData) {
		int startX = -1, startY = -1;
		ArrayList<Point> goals = new ArrayList<>();
		ArrayList<Point> startBoxes = new ArrayList<>();

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				if (mapData[y][x] == '.') {
					goals.add(new Point(x, y));
				}
				char item = itemsData[y][x];
				if (item == '@' || item == '+') {
					startX = x;
					startY = y;
				}
				if (item == '$' || item == '*') {
					startBoxes.add(new Point(x, y));
				}
			}
		}

		System.out.println("Start: (" + startX + "," + startY + ")");
		System.out.println("Boxes: " + startBoxes.size() + " - " + startBoxes);
		System.out.println("Goals: " + goals.size() + " - " + goals);

		boolean[][] deadlockMap = new boolean[height][width];
		for (int y = 1; y < height - 1; y++) {
			for (int x = 1; x < width - 1; x++) {
				if (mapData[y][x] != '#') {
					boolean isGoal = goals.contains(new Point(x,y));
					if(!isGoal) {
						if ((mapData[y - 1][x] == '#' && mapData[y][x - 1] == '#') ||
								(mapData[y - 1][x] == '#' && mapData[y][x + 1] == '#') ||
								(mapData[y + 1][x] == '#' && mapData[y][x - 1] == '#') ||
								(mapData[y + 1][x] == '#' && mapData[y][x + 1] == '#')) {
							deadlockMap[y][x] = true;
						}
					}
				}
			}
		}

		Map<String, Object> initData = new HashMap<>();
		initData.put("startX", startX);
		initData.put("startY", startY);
		initData.put("goals", goals);
		initData.put("startBoxes", startBoxes);
		initData.put("deadlockMap", deadlockMap);
		return initData;
	}

	// A generic search loop that works for both A* and GBFS, as the logic
	// is determined by the PriorityQueue's comparator.
	private String searchLoop(PriorityQueue<State> open, Set<State> closed, ArrayList<Point> goals,
							  boolean[][] deadlockMap, int width, int height, char[][] mapData, int maxStates) {

		int[] dx = {0, 0, -1, 1};
		int[] dy = {-1, 1, 0, 0};
		char[] dirChar = {'u', 'd', 'l', 'r'};
		int statesExplored = 0;

		while (!open.isEmpty() && statesExplored < maxStates) {
			State cur = open.poll();
			statesExplored++;

			if (statesExplored % 50000 == 0) {
				System.out.println("Progress: " + statesExplored + " states, " +
						open.size() + " in queue, depth: " + cur.moves.length());
			}

			if (isGoal(cur, goals)) {
				System.out.println("Solution found with " + cur.moves.length() + " moves!");
				System.out.println("Total states explored: " + statesExplored);
				return cur.moves;
			}

			if (closed.contains(cur)) {
				continue;
			}
			closed.add(cur);

			if (hasDeadlock(cur, deadlockMap, goals, width, height, mapData)) {
				continue;
			}

			for (int i = 0; i < 4; i++) {
				int nx = cur.playerX + dx[i];
				int ny = cur.playerY + dy[i];

				if (nx < 0 || nx >= width || ny < 0 || ny >= height || mapData[ny][nx] == '#') {
					continue;
				}

				int boxIndex = -1;
				for (int j = 0; j < cur.boxes.length; j++) {
					if (cur.boxes[j].x == nx && cur.boxes[j].y == ny) {
						boxIndex = j;
						break;
					}
				}

				// Case 1: Player pushes a box
				if (boxIndex != -1) {
					int bx2 = nx + dx[i];
					int by2 = ny + dy[i];

					if (bx2 < 0 || bx2 >= width || by2 < 0 || by2 >= height || mapData[by2][bx2] == '#') continue;

					boolean boxIsBlocked = false;
					for (Point otherBox : cur.boxes) {
						if (otherBox.x == bx2 && otherBox.y == by2) {
							boxIsBlocked = true;
							break;
						}
					}
					if (boxIsBlocked) continue;

					// Simple push-to-deadlock check
					if (deadlockMap[by2][bx2]) continue;

					// If player can't reach the spot to push from, skip
					if (!playerCanReach(cur, new Point(cur.playerX, cur.playerY), width, height, mapData)) continue;

					ArrayList<Point> newBoxes = new ArrayList<>(Arrays.asList(cur.boxes));
					newBoxes.set(boxIndex, new Point(bx2, by2));

					State next = new State(nx, ny, newBoxes, cur.moves + Character.toUpperCase(dirChar[i]), cur.g + 1, 0);
					next.h = heuristic(next, goals);

					if (!closed.contains(next)) {
						open.add(next);
					}
				}
				// Case 2: Player moves to an empty square
				else {
					State next = new State(nx, ny, new ArrayList<>(Arrays.asList(cur.boxes)), cur.moves + dirChar[i], cur.g + 1, 0);
					// No need to recalculate heuristic if no box moved
					next.h = cur.h;

					if (!closed.contains(next)) {
						open.add(next);
					}
				}
			}
		}

		System.out.println("No solution found. States explored: " + statesExplored);
		if (statesExplored >= maxStates) {
			System.out.println("Reached state exploration limit.");
		}
		return "";
	}
}
