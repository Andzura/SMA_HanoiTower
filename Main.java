import Model.Agent;
import Model.Board;

public class Main {

    public static void main(String[] args){
        Board board = new Board();

        for(int i = 0; i < 50; i++){
            board.addAgent(new Agent(board ,i));
        }
        board.init();
    }
}
