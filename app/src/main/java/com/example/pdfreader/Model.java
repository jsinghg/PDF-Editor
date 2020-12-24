package com.example.pdfreader;

import android.graphics.Path;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

public class Model extends Observable {

    // ==== Instance Related ====

    // Current instance of the model
    private static Model model;

    // File names
    final String FILENAME = "shannon1948.pdf";

    // File ID
    final int FILERESID = R.raw.shannon1948;

    // Current Page
    int page_number = 0;

    // Vector of all the paths
     Vector<ArrayList<Path>> annotations = new Vector<>();
     Vector<ArrayList<Path>> highlights = new Vector<>();

     // Vectors of all the Coordinates
    Vector<ArrayList<ArrayList<com.example.pdfreader.Pair>>> annotations_coord =  new Vector<>() ;
    Vector<ArrayList<ArrayList<com.example.pdfreader.Pair>>> highlights_coord =  new Vector<>() ;

    // CTOR
    static Model getInstance(){
        if (model == null){
            model = new Model();
        }
        return model;
    }


    //==== Override methods ====

    @Override
    public synchronized void deleteObserver(Observer o) {
        super.deleteObserver(o);
    }


    @Override
    public synchronized void addObserver(Observer o) {
        super.addObserver(o);
    }


    @Override
    public void notifyObservers() {
        super.notifyObservers();
    }


    // Initialize Observers
    public void initObservers(){
        setChanged();
        notifyObservers();
    }


    // ==== Booleans to specify the state ====

    // Turns true when annotation button is pressed
    private Boolean ANNOTATE = false;

    // Turns true when highlight button is pressed
    private Boolean HIGHLIGHT = false;

    // Turns true when highlight button is pressed
    private Boolean ERASE = false;


    // ==== Getters and Setters for the states

    void onANNOTATE(){
        this.ANNOTATE = true;
        this.HIGHLIGHT = false;
        this.ERASE = false;
        notifyObservers();
    }

    void onHIGHLIGHT(){
        this.ANNOTATE = false;
        this.HIGHLIGHT = true;
        this.ERASE = false;
        notifyObservers();
    }

    void RESET(){
        this.ANNOTATE = false;
        this.HIGHLIGHT = false;
        this.ERASE = false;
    }

    void onERASE(){
        this.ANNOTATE = false;
        this.HIGHLIGHT = false;
        this.ERASE = true;
        notifyObservers();
    }

    // Getter for ANNOTATE
    Boolean getANNOTATE(){
        return  ANNOTATE;
    }

    // Getter for HIGHLIGHT
    Boolean getHIGHLIGHT(){
        return  HIGHLIGHT;
    }

    // Getter for ERASE
    Boolean getERASE(){
        return  ERASE;
    }

    // Recreate all the Paths
    void recreate_paths(){

    }

}
