package Model;

import java.util.Random;

public class Agent extends Thread{

    private Board board;
    private int key;
    private int currentStack;
    private boolean pushed;
    //private Model.Agent topNeighbor;
    //private Model.Agent bottomNeighbor;

    public Agent(Board board, int key) {
        this.board = board;
        this.key = key;
        this.pushed = false;
    }

    public int getKey() {
        return key;
    }

    public boolean isPushed() {
        return pushed;
    }

    public void setPushed(boolean pushed) {
        this.pushed = pushed;
    }

    public int getCurrentStack() {
        return currentStack;
    }

    public void setCurrentStack(int currentStack) {
        this.currentStack = currentStack;
    }

    @Override
    public void run(){
        Random rand = new Random();
        while(!board.checkWin()){
            if(pushed){
                if(board.getTopNeighbor(this) == null){
                    board.move(this,(rand.nextInt(Board.NBSTACK-1)+1+currentStack)%Board.NBSTACK);
                    this.pushed = false;
                }else{
                    board.push(this);
                }
            }else if(!board.checkWinSolo(this)){
                if(board.getTopNeighbor(this) == null){
                    board.move(this,(rand.nextInt(Board.NBSTACK-1)+1+currentStack)%Board.NBSTACK);
                }else{
                    board.push(this);
                }
            }
            try {
                sleep(0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("STOPPED");
    }

    @Override
    public String toString() {
        return "" + key ;
    }
}
