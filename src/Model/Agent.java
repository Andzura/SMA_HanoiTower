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
            // Si on est l'agent 0 on a un traitement particulier (pour initialiser la pile)
            if (key == 0) {
                synchronized (board.getLock()) {
                    // Si on est en bas (et personne au dessus), on est bien placé
                    if (board.getBottomNeighbor(this) == null && board.getTopNeighbor(this) == null)
                        placed = true;
                    // Si on est en haut, on se met dans une colonne vide (et si il n'y en a pas on attend...)
                    else if (board.getTopNeighbor(this) == null) {
                        ArrayList<Integer> stacksVides = board.getListStackVide();
                        if (stacksVides.size() > 0) {
                            Collections.shuffle(stacksVides);
                            board.move(this, stacksVides.get(0));
                            placed = true;
                            pushed = false;
                        }
                    }
                    // Sinon on pousse
                    else
                        board.push(this);
                }
                pause(TEMPS_MAX_TEMPO);
            }

            // Pour tous les autres agents
            else {
                synchronized (board.getLock()) {
                    // pour éviter d'appeler les fonctions 10 fois
                    Agent bottom = board.getBottomNeighbor(this);
                    Agent top = board.getTopNeighbor(this);

                    // On regarde si celui d'en dessous connait le plus petit en dessous de lui
                    // Si c'est le cas le plus petit en dessous de nous = le min entre cette valeur et nous
                    updateLowestUnder(bottom);

                    // On regarde si on peut se placer
                    boolean aBouge = false;
                    if (top == null) {
                        choix = board.getTopAgents();
                        choix.remove(this);
                        for (Agent a : choix) {
                            if (a.isPlaced() && a.getKey() == key - 1) {
                                board.move(this, a.getCurrentStack());
                                pushed = false;
                                aBouge = true;
                                placed = true;
                            }
                        }
                    }
                    // Si on ne vient pas de se placer, on regarde si on a été poussé
                    if (!aBouge) {
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
                                    this.lowestUnder = -1;
                                }
                            }
                        }

                        // On vérifie si on est bien placé
                        if (bottom != null && bottom.isPlaced() && bottom.getKey() == key - 1) {
                            placed = true;
                        }
                        // Si on est mal placé on pousse
                        else {
                            board.push(this);
                        }
                    }
                }

                // On fait une petite tempo
                pause(TEMPS_MAX_TEMPO);
            }
            pushed = false;
        }

        // Temps que tous les agents ne sont pas biens placés ou que l'agent du desuss n'est pas bien placé on attend
        Agent topAgentTestFin = board.getTopNeighbor(this);
        while(!board.checkWin() && (topAgentTestFin == null || topAgentTestFin.isPlaced())){
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
