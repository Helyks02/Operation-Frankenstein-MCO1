package solver;

public class SokoBot {

    private class Point{

        int x;
        int y ;

        public Point(int x, int y){
          this.x = x;
          this.y = y;
        }


    }
    private class State{

        int userX;
        int userY;
        Point[] boxes;

    }
    public String solveSokobanPuzzle(int width, int height, char[][] mapData, char[][] itemsData) {
      /*
       * YOU NEED TO REWRITE THE IMPLEMENTATION OF THIS METHOD TO MAKE THE BOT SMARTER
       */
      /*
       * Default stupid behavior: Think (sleep) for 3 seconds, and then return a
       * sequence
       * that just moves left and right repeatedly.
       */
      try {
        Thread.sleep(3000);
      } catch (Exception ex) {
        ex.printStackTrace();
      }
      return "lrlrlrlrlrlrlrlrlrlrlrlrlrlrlrlrlrlrlrlrlrlrlrlrlrlrlrlrlrlrlrlrlrlrlrlrlrlr";
    }

}
