package Display;


import Model.Board;

import java.util.Date;

public class ConsolDisplay extends Thread{
    private Board board;
    private int tempo;
    private Date dateDebut;

    public ConsolDisplay(Board board, int tempo){
        this.board = board;
        this.tempo = tempo;
        this.dateDebut = new Date();
    }

    @Override
    public void run(){
        while(!board.checkWin()){
            board.print();
            try {
                sleep(tempo);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Fini en : "+getTime(new Date().getTime() - dateDebut.getTime()));
        board.print();
    }

    // Renvoi le temps au format h min s ms
    public String getTime(long ms){
        long diff = ms%1000;
        ms=ms/1000;
        String ret = diff+"ms";
        diff=ms%60;
        ms=ms/60;
        if (diff>0)
            ret = diff+"s "+ret;
        diff=ms%60;
        ms=ms/60;
        if (diff>0)
            ret = diff+"min "+ret;
        diff=ms;
        if (diff>0)
            ret = diff+"h "+ret;
        return ret;
    }


}
