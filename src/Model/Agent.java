package Model;

import java.util.*;

import static java.lang.StrictMath.min;

public class Agent extends Thread{

    public static int TEMPS_MAX_TEMPO = 1000;
    private Board board;
    private int key;
    private int currentStack;
    private boolean pushed;
    private boolean placed;
    private int lowestUnder;

    public Agent(Board board, int key) {
        this.board = board;
        this.key = key;
        this.pushed = false;
        this.placed = false;
        this.lowestUnder = -1;
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

    public int getLowestUnder() {
        return lowestUnder;
    }

    private void pause(int tempsMax){
        try {
            sleep((int) (Math.random() * tempsMax));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run(){
        pause(TEMPS_MAX_TEMPO);
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
            synchronized (board.getLock()) {
                if (pushed) {
                    if (board.getTopNeighbor(this) == null) {
                        board.move(this, (rand.nextInt(Board.NBSTACK - 1) + 1 + currentStack) % Board.NBSTACK);
                        this.pushed = false;
                    } else {
                        board.push(this);
                    }
                } else if (!board.checkWinSolo(this)) {
                    if (board.getTopNeighbor(this) == null) {
                        board.move(this, (rand.nextInt(Board.NBSTACK - 1) + 1 + currentStack) % Board.NBSTACK);
                    } else {
                        board.push(this);
                    }
                }
            }
            pause(TEMPS_MAX_TEMPO);
        }
    }

    // Les agents agissent de manière collaborative et de façon réactive
    // Un agent ne connait que ce qu'il y a en desosus et au dessus de lui
    // Si il est tout en haut d'un stack, il connait le dessus de tous les stacks
    public void runCollabReactif(){
        ArrayList<Agent> choix;
        // Tant que l'agent n'est pas à sa place
        // ( = en bas pour le 0, au dessus du numéro inférieur qui est placé pour les autres)
        while(!placed) {
            synchronized (board.getLock()) {
                // pour éviter d'appeler les fonctions 10 fois
                Agent bottom = board.getBottomNeighbor(this);
                Agent top = board.getTopNeighbor(this);

                // On regarde si celui d'en dessous connait le plus petit en dessous de lui
                // Si c'est le cas le plus petit en dessous de nous = le min entre cette valeur et nous
                updateLowestUnder(bottom);

                // On regarde si on peut se placer
                if (top == null) {
                    // L'agent 0 veut une pile vide
                    if (key == 0){
                        ArrayList<Integer> vides = board.getListStackVide();
                        // Si une case vide on va dessus
                        if (vides.size() > 0) {
                            Collections.shuffle(vides);
                            board.move(this, vides.get(0));
                            pushed = false;
                            placed = true;
                        }
                    }
                    // Les autres veulent l'agent n - 1 si il est bien placé
                    else {
                        choix = board.getTopAgents();
                        choix.remove(this);
                        for (Agent a : choix) {
                            if (a.isPlaced() && a.getKey() == key - 1) {
                                board.move(this, a.getCurrentStack());
                                pushed = false;
                                placed = true;
                            }
                        }
                    }
                }
                // Si on ne vient pas de se placer, on regarde si on a été poussé
                if (!placed) {
                    // Si on est poussé on bouge ou on repousse au dessus
                    if (pushed) {
                        // Si on est en haut du stack
                        if (top == null) {
                            // On se déplace sur le meilleur choix, ou on ne bouge pas si un agent plus important pousse à coté
                            choix = board.getTopAgents();
                            choix.remove(this);
                            int dest = getMeilleurStackCollab(choix);
                            if (dest != currentStack){
                                board.move(this, dest);
                                this.pushed = false;
                                // On regarde si on peut déjà connaitre le plus petit agent en dessous de nous dans la tour
                                this.lowestUnder = -1;
                                updateLowestUnder(board.getBottomNeighbor(this));
                            }
                        }
                    }
                    // Si on est mal placé on pousse
                    if (!placed) {
                        board.push(this);
                    }
                }
            }

            // On fait une petite tempo
            pause(TEMPS_MAX_TEMPO);
        }
        pushed = false;

        // Temps que tous les agents ne sont pas biens placés ou que l'agent du dessus n'est pas bien placé on attend
        Agent topAgentTestFin = board.getTopNeighbor(this);
        while((topAgentTestFin != null && !topAgentTestFin.isPlaced()) || !board.checkWin()){
            pause(TEMPS_MAX_TEMPO);
            topAgentTestFin = board.getTopNeighbor(this);
        }
        // On peut s'éteindre car on ne sert plus à rien
    }

    public void updateLowestUnder(Agent bottom){
        // Si on ne connait toujours pas le plus petit en dessous de nous, on regarde si le voisin du dessous le connait
        if (lowestUnder == -1) {
            if (bottom == null) {
                lowestUnder = key;
            }
            // Le voisin d'en dessous connait la plus petite clé en dessous de lui
            else if (bottom.getLowestUnder() > -1) {
                lowestUnder = min(key, bottom.getLowestUnder());
            }
        }
    }


    //Renvoi le meilleur Stack sur lequel se déplacé en mode Collab
    //un agent qui est au en haut d'un stack peut communiquer avec les autres qui sont aussi en haut d'un stack
    //il demande donc a chacun d'eux quel est l'agent le plus important dans leur stack
    //si un stack possède un agent le plus important moins important que le stack courant, il peut se déplacer dessus
    //sinon si il reste des stack vides, ou un stack vide et que le stack "final" existe ( donc si il existe un agent bien placé)
    // l'agent peut se déplacer dans un stack vide
    //sinon il reste ou il est.
    public int getMeilleurStackCollab(ArrayList<Agent> choix){
        // Si 2 stacks sont vides
        ArrayList<Integer> vides = board.getListStackVide();
        boolean atLeastOnePlaced = false;
        for(Agent a : choix){
            if(a.getLowestUnder() > this.getLowestUnder())
                return a.getCurrentStack();
            if(a.isPlaced())
                atLeastOnePlaced = true;
        }

        if (vides.size() > 1 || (atLeastOnePlaced && vides.size() > 0)) {
            Collections.shuffle(vides);
            return vides.get(0);
        }

        return this.getCurrentStack();
    }



    // Les agents interrogent le plateau pour savoir comment agir, comportement supervisé et non réactif
    public void runCollabSupervise(){
        Random rand = new Random();
        while(!placed) {
            synchronized (board.getLock()) {
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
                } else if (board.getNextToPlace() == this.key) {
                    if (board.getTopNeighbor(this) != null) {
                        board.push(this);
                    } else {
                        board.move(this, board.getTargetStack());
                        this.placed = true;
                        board.notifyPlaced(this);
                    }
                }
            }
            pause(TEMPS_MAX_TEMPO);
        }
    }

    //renvoi l'agent le plus important du stack on communiquant recursivement avec ses voisins inférieurs.
    public int getLowestinStack(){
        Agent a = board.getBottomNeighbor(this);
        if(a == null){
            return this.getKey();
        }
        return min(a.getLowestinStack(), this.getKey());
    }
    @Override
    public String toString() {
        return "" + key ;
    }
}
