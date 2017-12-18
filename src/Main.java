import Model.Agent;
import Model.Board;

public class Main {

    public static void main(String[] args){
        boolean collab = true;
        Board board = new Board(collab);
        for(int i = 0; i < 1000; i++){
            board.addAgent(new Agent(board ,i));
        }
        board.init();
    }
}
