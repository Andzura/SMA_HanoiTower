import Display.ConsolDisplay;
import Model.Agent;
import Model.Board;

public class Main {

    public static void main(String[] args){
        Board board = new Board();
        ConsolDisplay affichage = new ConsolDisplay(board, 500);

        for(int i = 0; i < 150; i++){
            board.addAgent(new Agent(board ,i));
        }
        board.init();
        affichage.run();
    }
}
