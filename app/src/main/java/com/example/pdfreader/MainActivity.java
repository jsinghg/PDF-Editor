package com.example.pdfreader;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.*;
import android.graphics.pdf.PdfRenderer;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import org.w3c.dom.Text;

import java.io.*;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

public class MainActivity extends AppCompatActivity implements Observer {

    // Instance of the current model
    Model model;

    final String LOGNAME = "pdf_viewer";

    TextView Pagenumber;

    // manage the pages of the PDF, see below
    PdfRenderer pdfRenderer;
    private ParcelFileDescriptor parcelFileDescriptor;
    private PdfRenderer.Page currentPage;

    // custom ImageView class that captures strokes and draws them over the image
    PDFimage pageImage;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setting the value of the current model
        model = Model.getInstance();
        model.addObserver(this);
        if (savedInstanceState != null) {
            String message = savedInstanceState.getString("message");
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            model.annotations_coord = (Vector<ArrayList<ArrayList<com.example.pdfreader.Pair>>>) savedInstanceState.getSerializable("ObjectA");
            System.out.println("When Saved");
            System.out.println(model.annotations_coord.size());
        }

        LinearLayout layout = findViewById(R.id.PDF);
        pageImage = new PDFimage(this);
        layout.addView(pageImage);
        layout.setEnabled(true);
        pageImage.setMinimumWidth(1000);
        pageImage.setMinimumHeight(2000);
        Pagenumber = (TextView) findViewById(R.id.pages);
        Pagenumber.setTextSize(30);
        Pagenumber.setText("1/55");

        // it will be displayed as an image in the pageImage (above)
        try {
            openRenderer(this);
            if(savedInstanceState == null)
                showPage(model.page_number);
            else showPage(3);
            closeRenderer();
        } catch (IOException exception) {
            Log.d(LOGNAME, "Error ope" +
                    "ning PDF");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onStop() {
        super.onStop();
        Log.d(LOGNAME, "Activity Stopped");
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void openRenderer(Context context) throws IOException {
        // In this sample, we read a PDF from the assets directory.
        File file = new File(context.getCacheDir(), model.FILENAME);
        if (!file.exists()) {
            // pdfRenderer cannot handle the resource directly,
            // so extract it into the local cache directory.
            InputStream asset = this.getResources().openRawResource(model.FILERESID);
            FileOutputStream output = new FileOutputStream(file);
            final byte[] buffer = new byte[1024];
            int size;
            while ((size = asset.read(buffer)) != -1) {
                output.write(buffer, 0, size);
            }
            asset.close();
            output.close();
        }
        parcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);

        // capture PDF data
        // all this just to get a handle to the actual PDF representation
        if (parcelFileDescriptor != null) {
            pdfRenderer = new PdfRenderer(parcelFileDescriptor);
        }
    }


    // Runs before quitting
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void closeRenderer() throws IOException {
        if (null != currentPage) {
            currentPage.close();
        }
        pdfRenderer.close();
        parcelFileDescriptor.close();
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void showPage(int index) {
        if (pdfRenderer.getPageCount() <= index) {
            return;
        }


        // Use `openPage` to open a specific page in PDF.
        currentPage = pdfRenderer.openPage(index);

        // Important: the destination bitmap must be ARGB (not RGB).
        Bitmap bitmap = Bitmap.createBitmap(currentPage.getWidth(), currentPage.getHeight(), Bitmap.Config.ARGB_8888);

       // Current Page is rendered
        currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

        // Display the page
        pageImage.setImage(bitmap);
    }


    @Override
    public void update(Observable o, Object arg) {

    }

    public void onannotate(View view) {
        if(model.getANNOTATE()){
            model.RESET();
            return;
        }
        model.onANNOTATE();
    }

    public void onhighlight(View view) {
        if(model.getHIGHLIGHT()){
            model.RESET();
            return;
        }
        model.onHIGHLIGHT();
    }

    public void onerase(View view) {
        if(model.getERASE()){
            model.RESET();
            return;
        }
        model.onERASE();

    }


    // PageUp action
    public void pageup(View view) {

        // Adding the paths of the current page to the stack of paths
        // Saves the state if the current page
        if(model.page_number == model.annotations.size()){
            model.annotations.add((ArrayList<Path>)pageImage.annotations.clone());
            model.highlights.add((ArrayList<Path>)pageImage.highlights.clone());
            Log.d(LOGNAME,"saved" + String.valueOf(model.annotations.size()) + String.valueOf(model.page_number) );
            add_coordinates();
        } else if (model.page_number < model.annotations.size()){
            model.annotations.removeElementAt(model.page_number);
            model.annotations.add(model.page_number,(ArrayList<Path>)pageImage.annotations.clone());
            model.highlights.removeElementAt(model.page_number);
            model.highlights.add(model.page_number,(ArrayList<Path>)pageImage.highlights.clone());
            Log.d(LOGNAME,"saved Again" + String.valueOf(model.annotations.size()) + String.valueOf(model.page_number) );
            // Removing corresponding coordinates too
            model.highlights_coord.removeElementAt(model.page_number);
            model.annotations_coord.removeElementAt(model.page_number);
            add_coordinates(model.page_number);
        }

        // Tasks for the current page
        model.page_number--;

        // Check so that page number doesnt go out of bounds
        if(model.page_number <= 0){
            model.page_number = 0;
        }
        Pagenumber.setText((model.page_number+1) + "/55");

        pageImage.annotations.clear();
        pageImage.highlights.clear();

        if(model.page_number < model.annotations.size()){
            pageImage.annotations = (ArrayList<Path>) model.annotations.elementAt(model.page_number).clone();
            pageImage.highlights = (ArrayList<Path>) model.highlights.elementAt(model.page_number).clone();
            pageImage.invalidate();
            Log.d(LOGNAME,String.valueOf(model.annotations.elementAt(model.page_number).size()) );
        }
        try {
            openRenderer(this);
            showPage(model.page_number);
            closeRenderer();
        } catch (IOException exception) {
            Log.d(LOGNAME, "Error ope" +
                    "ning PDF");
        }
    }

    public void pagedown(View view) {
        // Saves the state if the current page
        if(model.page_number == model.annotations.size()){
            model.annotations.add((ArrayList<Path>)pageImage.annotations.clone());
            model.highlights.add((ArrayList<Path>)pageImage.highlights.clone());
            Log.d(LOGNAME,"saved" + String.valueOf(model.annotations.size()) + String.valueOf(model.page_number) );
            // Adding coordinates for Saving the state
            add_coordinates();

        } else if (model.page_number < model.annotations.size()){
            model.annotations.removeElementAt(model.page_number);
            model.annotations.add(model.page_number,(ArrayList<Path>)pageImage.annotations.clone());
            model.highlights.removeElementAt(model.page_number);
            model.highlights.add(model.page_number,(ArrayList<Path>)pageImage.highlights.clone());
            Log.d(LOGNAME,"saved Again" + String.valueOf(model.annotations.size()) + String.valueOf(model.page_number) );
            // Removing corresponding coordinates too
            model.highlights_coord.removeElementAt(model.page_number);
            model.annotations_coord.removeElementAt(model.page_number);
            add_coordinates(model.page_number);
        }

        // Tasks for the new page
        model.page_number++;
        Pagenumber.setText((model.page_number+1) + "/55");
        pageImage.annotations.clear();
        pageImage.highlights.clear();

        // Checking if the new paths for the current page already exist
        if(model.page_number < model.annotations.size()){
            // Adding Paths for drawing
            pageImage.annotations = (ArrayList<Path>) model.annotations.elementAt(model.page_number).clone();
            pageImage.highlights = (ArrayList<Path>) model.highlights.elementAt(model.page_number).clone();
            pageImage.invalidate();
            Log.d(LOGNAME,String.valueOf(model.annotations.elementAt(model.page_number).size()) );
        }

        // Rendering the current page
        try {
            openRenderer(this);
            showPage(model.page_number);
            closeRenderer();
        } catch (IOException exception) {
            Log.d(LOGNAME, "Error ope" +
                    "ning PDF");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("ObjectA", model.annotations_coord);
        Log.d(LOGNAME,"State Saved");

    }


    // Undo Action
    public void Undo(View view) {
        pageImage.undo();
    }

    // Redo Action
    public void Redo(View view) {
        pageImage.redo();
    }

    // Adding coordinates
    void add_coordinates(){
        ArrayList<ArrayList<com.example.pdfreader.Pair>> htemp = new ArrayList<>();
        ArrayList<ArrayList<com.example.pdfreader.Pair>> atemp = new ArrayList<>();
        for (int i = 0; i < pageImage.coordinates.size(); ++i){
            if(pageImage.annotations.contains(pageImage.coordinates.get(i).first)){
                atemp.add(pageImage.coordinates.get(i).second);
            } else if (pageImage.highlights.contains(pageImage.coordinates.get(i).first)){
                htemp.add(pageImage.coordinates.get(i).second);
            }
        }
        model.annotations_coord.add(atemp);
        model.highlights_coord.add(htemp);
    }

    // Adding coordinates
    void add_coordinates(int index){
        ArrayList<ArrayList<com.example.pdfreader.Pair>> htemp = new ArrayList<>();
        ArrayList<ArrayList<com.example.pdfreader.Pair>> atemp = new ArrayList<>();
        for (int i = 0; i < pageImage.coordinates.size(); ++i){
            if(pageImage.annotations.contains(pageImage.coordinates.get(i).first)){
                atemp.add(pageImage.coordinates.get(i).second);
            } else if (pageImage.highlights.contains(pageImage.coordinates.get(i).first)){
                htemp.add(pageImage.coordinates.get(i).second);
            }
        }
        model.annotations_coord.add(index,atemp);
        model.highlights_coord.add(index,htemp);
    }
}



