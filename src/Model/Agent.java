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
    private int pushedBy;

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

    public int getPushedBy() {
        return pushedBy;
    }

    public void setPushedBy(int pushedBy) {
        this.pushedBy = pushedBy;
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
        // Temps que l'agent n'est pas à sa place
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
                        board.pushWithId(this);
                }
                pause(TEMPS_MAX_TEMPO);
            }

            // Pour tous les autres agents
            else {
                synchronized (board.getLock()) {
                    // pour éviter d'appeler les fonctions 10 fois
                    Agent bottom = board.getBottomNeighbor(this);
                    Agent top = board.getTopNeighbor(this);

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
                                if (dest != currentStack) {
                                    board.move(this, dest);
                                    this.pushed = false;
                                }
                            }
                        }

                        // On vérifie si on est bien placé
                        if (bottom != null && bottom.isPlaced() && bottom.getKey() == key - 1) {
                            placed = true;
                        }
                        // Si on est mal placé on pousse
                        else {
                            board.pushWithId(this);
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


    // Parcours la liste des agents en haut de stacks et renvoi le stack du plus petit agent plus gros nous
    // ou de l'agent sur lequel on doit être si celui-ci est bien placé
    // Si aucun agent plus petit que nous, on se met sur le plus gros
    // Si un stack vide, on va dedans
    // On ne va pas sur un agent poussé, sauf si on est poussé par un plus gros
    public int getMeilleurStackCollab(ArrayList<Agent> choix){
        ArrayList<Agent> nonPoussesNonPlaces = new ArrayList<Agent>();
        Agent maxPoussePar = null;
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
            // On va d'abord essayer d'aller sur un agent non poussé
            else if (!a.isPushed() && !a.isPlaced()) {
                Collections.shuffle(nonPoussesNonPlaces);
                nonPoussesNonPlaces.add(a);
            }
            // Sinon si l'agent a été poussé, on peut quand même allé sur lui si un agent plus gros nous a poussé
            else if (a.isPushed()) {
                int pousseur = min(a.getPushedBy(), a.getKey());
                if (maxPoussePar == null || pousseur > min(maxPoussePar.getKey(), maxPoussePar.getPushedBy())){
                    maxPoussePar = a;
                }
            }
        }
        // Si 2 stacks sont vides, ou un stack vide + un agent bien placé, on va dans le vide
        ArrayList<Integer> vides = board.getListStackVide();
        if (vides.size() > 1 || (agentBienPlace && vides.size() ==1)) {
            Collections.shuffle(vides);
            return vides.get(0);
        }
        // Si un agent n'est pas poussé on va dessus
        if (nonPoussesNonPlaces.size() > 0){
            return nonPoussesNonPlaces.get(0).getCurrentStack();
        }
        // Si on est poussé par un gros et qu'un autre est poussé par un moins gros, on se met sur lui
        else if (pushed && maxPoussePar != null && min(maxPoussePar.getKey(), maxPoussePar.getPushedBy()) > pushedBy)
            return maxPoussePar.getCurrentStack();
        // Sinon on reste où on est
        else
            return currentStack;
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

    @Override
    public String toString() {
        return "" + key ;
    }
}
