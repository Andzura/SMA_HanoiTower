package Model;

import java.util.Random;

public class Agent extends Thread{

    private Board board;
    private int key;
    private int currentStack;
    private boolean pushed;
    private boolean placed;
    //private Model.Agent topNeighbor;
    //private Model.Agent bottomNeighbor;

    public Agent(Board board, int key) {
        this.board = board;
        this.key = key;
        this.pushed = false;
        this.placed = false;
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

    public boolean isPlaced() {
        return placed;
    }

    public void setPlaced(boolean placed) {
        this.placed = placed;
    }

    @Override
    public void run(){
        if(board.isCollab()){
            runCollab();
        }else{
            runRandom();
        }
    }

    public void runRandom(){
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
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("STOPPED");
    }

    public void runCollab(){
        Random rand = new Random();
        while(!placed) {
            if (pushed) {
                if (board.getTopNeighbor(this) == null) {
                    int stack2Move = 0;
                    do {
                        stack2Move = (rand.nextInt(Board.NBSTACK - 1) + 1 + currentStack) % Board.NBSTACK;
                    } while (stack2Move == board.getTargetStack());
                    board.move(this, stack2Move);
                    this.pushed = false;
                } else {
                    board.push(this);
                }
            }
            else if(board.getNextToPlace() == this.key){
                if (board.getTopNeighbor(this) != null) {
                    board.push(this);
                } else {
                    board.move(this, board.getTargetStack());
                    this.placed = true;
                    board.notifyPlaced(this);
                }
            }
            try {
                Thread.sleep(0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String toString() {
        return "" + key ;
    }
}
