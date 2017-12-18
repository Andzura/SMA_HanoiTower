package Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Board{

    public static final int NBSTACK = 3;

    private List<Agent> agents;
    private List<Agent>[] stacks;
    private int nbMove = 0;
    private final Object lock = new Object();
    private int nextToPlace = 0;
    private int targetStack = 2;
    private boolean collab;

    public Board(boolean collab) {
        this.collab = collab;
        stacks = (ArrayList<Agent>[])new ArrayList[NBSTACK];
        for(int i = 0; i < NBSTACK; i++){
            stacks[i] = new ArrayList<>();
        }
        agents = new ArrayList<>();
    }

    public void addAgent(Agent a){
        agents.add(a);
        stacks[0].add(a);
        a.setCurrentStack(0);
    }

    public Agent getTopNeighbor(Agent a){
        int currentStack = a.getCurrentStack();
        int i = stacks[currentStack].indexOf(a);
        synchronized (lock) {
            if (i + 1 == stacks[currentStack].size()) {
                return null;
            }
            return stacks[currentStack].get(i + 1);
        }
    }

    public Agent getBottomNeighbor(Agent a){
        int currentStack = a.getCurrentStack();
        int i = stacks[currentStack].indexOf(a);
        if(i == 0){
            return null;
        }
        return stacks[currentStack].get(i-1);
    }

    public boolean checkWin(){
        int i = agents.get(0).getCurrentStack();
        if(stacks[i].size()  != agents.size())
            return false;
        for(int j = 0; j < stacks[i].size(); j++){
            if(stacks[i].get(j).getKey() != j)
                return false;
        }
        return true;
    }

    public boolean checkWinSolo(Agent a){
        if(stacks[a.getCurrentStack()].indexOf(a) != a.getKey())
            return false;
        return true;
    }

    public synchronized void move(Agent a, int dest){
        int currStack = a.getCurrentStack();
        if(this.getTopNeighbor(a) != null)
            return;
        synchronized (lock) {
            stacks[currStack].remove(a);
            stacks[dest].add(a);
            a.setCurrentStack(dest);
            nbMove++;
        }
    }

    public void push(Agent a){
        Agent a2Push = this.getTopNeighbor(a);
        if(a2Push != null)
            a2Push.setPushed(true);
    }

    public void init(){
        Collections.shuffle(stacks[0]);
        for(Agent a : agents){
            a.start();
        }
    }

    public void print() {
        synchronized (lock) {
            System.out.println("move : " + nbMove);
            for (int i = 0; i < NBSTACK; i++) {
                System.out.println("Stack " + i + " : " + stacks[i].toString());
            }
            System.out.println("=============");
        }
    }

    public int getNextToPlace() {
        return nextToPlace;
    }

    public void setNextToPlace(int nextToPlace) {
        this.nextToPlace = nextToPlace;
    }

    public int getTargetStack() {
        return targetStack;
    }

    public void setTargetStack(int targetStack) {
        this.targetStack = targetStack;
    }

    public boolean isCollab() {
        return collab;
    }

    public void notifyPlaced(Agent agent) {
        if(agent.getKey() == nextToPlace)
            nextToPlace++;
    }
}
