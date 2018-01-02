package Model;

import java.util.*;

public class Agent extends Thread{

    private Board board;
    private int key;
    private int currentStack;
    private boolean pushed;
    private boolean placed;

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
        if(board.isRandom()){
            runRandom();
        }else if(board.isCollabSupervise()){
            runCollabSupervise();
        }else if(board.isCollabReactif()){
            runCollabReactif();
        }
    }

    // Les agents se déplacent de manières aléatoires si ils ont été poussés
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
                sleep(0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("STOPPED");
    }

    // Les agents agissent de manière collaborative et de façon réactive
    // Un agent ne connait que ce qu'il y a en desosus et au dessus de lui
    // Si il est tout en haut d'un stack, il connait le dessus de tous les stacks
    public void runCollabReactif(){
        ArrayList<Agent> choix;
        // Temps que l'agent n'est pas à sa place
        // ( = en bas pour le 0, au dessus du numéro inférieur qui est placé pour les autres)
        while(!placed) {
            // Si on est l'agent 0 on a un traitement particulier (pour initialiser la pile)
            if (key == 0)
                actionAgent0();

            else {
                // pour éviter d'appeler la fonction 10 fois
                Agent bottom = board.getBottomNeighbor(this);
                // Si on est poussé on bouge ou on repousse au dessus
                if (pushed) {
                    // Si on est en haut du stack
                    if (board.getTopNeighbor(this) == null) {
                        // On se déplace sur le meilleur choix
                        choix = board.getTopAgents();
                        choix.remove(this);
                        board.move(this, getMeilleurStackCollab(choix));
                        this.pushed = false;
                        // Sinon on n'est pas en haut donc on pousse l'agent du dessus
                    } else {
                        board.push(this);
                    }
                }
                // Sinon si on est en haut d'un stack, on regarde si on peut se placer
                else if (board.getTopNeighbor(this) == null) {
                    choix = board.getTopAgents();
                    choix.remove(this);
                    for (Agent a : choix) {
                        if (a.isPlaced() && a.getKey() == key - 1) {
                            board.move(this, a.getCurrentStack());
                        }
                    }
                }
                // Sinon si on n'est pas sur l'agent directement plus petit que nous (pour former un stack à l'envers) on pousse
                else if (bottom != null && bottom.getKey() != key + 1) {
                    board.push(this);
                }
                // On vérifie si on est bien placé
                if (bottom != null && bottom.isPlaced() && bottom.getKey() == key + 1)
                    placed = true;
            }

            // On fait une petite tempo
            try {
                Thread.sleep(0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void actionAgent0() {
        // Si on est en bas, on est bien placé
        if (board.getBottomNeighbor(this) == null)
            placed = true;
        // Si on est en haut, on se met dans une colonne vide (et si il n'y en a pas on attend...)
        else if (board.getTopNeighbor(this) == null){
            ArrayList<Integer> stacksVides = board.getListStackVide();
            if(stacksVides.size() > 0){
                Collections.shuffle(stacksVides);
                board.move(this, stacksVides.get(0));
                placed = true;
            }
        }
        // Sinon on pousse
        else
            board.push(this);
    }

    // Parcours la liste des agents en haut de stacks et renvoi le stack du plus petit agent plus gros nous
    // ou de l'agent sur lequel on doit être si celui-ci est bien placé
    // Si aucun agent plus petit que nous, on se met sur le plus gros
    // Si un stack vide, on va dedans
    public int getMeilleurStackCollab(ArrayList<Agent> choix){
        Agent maxTousAgents = null;
        Agent minPlusGrosQueNous = null;
        boolean agentBienPlace = false;

        for (Agent a : choix){
            // Si un agent est bien placé
            if (a.isPlaced()){
                agentBienPlace = true;
                // et si l'agent est bien placé et celui sur lequel on doit être, on se met dessus
                if ( a.getKey() == key - 1)
                    return a.getCurrentStack();
            }
            // Sinon on met à jour le meilleur choix si on trouve mieux
            // Plus gros des agents en haut d'une pile
            if (maxTousAgents == null || a.getKey() > maxTousAgents.getKey())
                maxTousAgents = a;
            // Plus petit agent plus gros que nous
            if (a.getKey() > key && (minPlusGrosQueNous == null || a.getKey() < minPlusGrosQueNous.getKey())){
                minPlusGrosQueNous = a;
            }
        }
        // Si 2 stacks sont vides, ou un stack vide + un agent bien placé, on va dans le vide
        ArrayList<Integer> vides = board.getListStackVide();
        if (vides.size() > 1 || (agentBienPlace && vides.size() ==1)) {
            Collections.shuffle(vides);
            return vides.get(0);
        }
        // On renvoie le stack du plus petit agent plus gros nous
        if (minPlusGrosQueNous != null)
            return minPlusGrosQueNous.getCurrentStack();
        // ou sinon du plus gros agent
        return maxTousAgents.getCurrentStack();
    }



    // Les agents interrogent le plateau pour savoir comment agir, comportement supervisé et non réactif
    public void runCollabSupervise(){
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
