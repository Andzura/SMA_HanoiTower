import Display.ConsolDisplay;
import Model.Agent;
import Model.Board;

public class Main {

    public static void main(String[] args){
        // Nombre d'agents
        int nbAgents = 25;
        // 0 = random, 1 = collaboratif SUPERVISE
        int strategie = 1;
        // Fréquence d'actualisation de l'affichage en ms, 0 ou négatif = affiche tous les mouvements
        int freqAffichage = 1000;

        boolean threadAff = freqAffichage > 0;

        Board board = new Board(strategie, !threadAff);
        // Création d'un thread d'affichage pour éviter le spam dans la console
        ConsolDisplay affichage = null;
        if (threadAff) affichage = new ConsolDisplay(board, freqAffichage);

        // Création des agents
        for(int i = 0; i < nbAgents; i++){
            board.addAgent(new Agent(board ,i));
        }
        // Mélange des agents + démarrage
        board.init();
        // Démarrage de l'affichage
        if (threadAff) affichage.run();
    }
}
