package com.example.pdfreader;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.*;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener;
import android.widget.ImageView;
import androidx.core.graphics.ColorUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

@SuppressLint("AppCompatCustomView")
public class PDFimage extends ImageView implements Observer{

    // A Model pointing to the current instance of the model
    Model model;

    // Listener for dragging and zooming
    private ScaleGestureDetector mScaleDetector;

    // Current scale for zooming
    private float curr_scale = 1.f;

    final String LOGNAME = "pdf_image";


    // Undo Stack
    Vector<Pair <String, Path>> UndoStack = new Vector<>();

    // Redo Stack
    Vector<Pair <String, Path>> RedoStack = new Vector<>();

    // drawing path
    Path path = null;

    // Container for Points
    ArrayList<com.example.pdfreader.Pair> points = new ArrayList<>();

    // An arraylist of paths of annotations
    ArrayList<Path> annotations = new ArrayList();

    // An arraylist of paths of Highlights
    ArrayList<Path> highlights = new ArrayList();

    // An Array list of paths and the their points
    ArrayList<Pair<Path, ArrayList<com.example.pdfreader.Pair>>> coordinates = new ArrayList<>();

    // Paint for Highlights
    Paint highlight_paint = new Paint(Color.RED);

    // Paint for Annotations
    Paint annotation_paint = new Paint(Color.RED);

    // image to display
    Bitmap bitmap;

    // Max zoom factor
    private float maxScale = 5.f;

    // Min zoom factor
    private float minScale = 0.5f;

    private Path path_del;

    private float mPositionX;
    private float mPositionY;
    private float mLastTouchX;
    private float mLastTouchY;

    // constructor
    public PDFimage(Context context) {
        super(context);
        model = Model.getInstance();
        model.addObserver(this);
        paint_annotation(annotation_paint);
        paint_highlight(highlight_paint);
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        annotations = new ArrayList<>();

    }

    // capture touch events (down/move/up) to create a path
    // and use that to create a stroke that we can draw
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mScaleDetector.onTouchEvent(event);
        // Will only be executed if either state is ANNOTATION or HIGHLIGHT
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    Log.d(LOGNAME, "Action down");
                    path = new Path();
                    points = new ArrayList<>();
                    if (model.getANNOTATE()){
                        annotations.add(path);

                        // Tracking the points of thes of the path
                        com.example.pdfreader.Pair curr = new com.example.pdfreader.Pair(event.getX(),event.getY());
                        points.add(curr);

                        // Moving the path to the proper coordinates
                        path.moveTo(event.getX() , event.getY());

                        // Adding in the undo stack
                        Pair<String,Path> temp = new Pair<>("Annotation", path);
                        UndoStack.add(temp);

                    } else if (model.getHIGHLIGHT()){
                        highlights.add(path);

                        // Tracking the points of the current Highlight
                        com.example.pdfreader.Pair curr = new com.example.pdfreader.Pair(event.getX(),event.getY());
                        points.add(curr);

                        // Moving the path to proper coordinates
                        path.moveTo(event.getX(), event.getY());

                        // Adding in the undo stack
                        Pair<String,Path> temp = new Pair<>("Highlight", path);
                        UndoStack.add(temp);

                    } else if (model.getERASE()){
                        // Checking if any path is clicked
                        path_del = new Path();
                        if(hit_test(event.getX(), event.getY())){
                            if(annotations.contains((path_del))) {
                                // Adding in the undo stack
                                Pair<String,Path> temp = new Pair<>("Erase-Annotation", path_del);
                                UndoStack.add(temp);
                                annotations.remove(path_del);
                            } else {
                                // Adding in the undo stack
                                Pair<String,Path> temp = new Pair<>("Erase-Highlight", path_del);
                                UndoStack.add(temp);
                                highlights.remove(path_del);
                            }
                            invalidate();
                        }
                    } else {
                        //get x and y cords of where we touch the screen
                        final float x = event.getX();
                        final float y = event.getY();

                        //remember where touch event started
                        mLastTouchX = x;
                        mLastTouchY = y;

                        break;
                    }

                    break;
                case MotionEvent.ACTION_MOVE:
                    if(model.getHIGHLIGHT() || model.getANNOTATE()) {
                        Log.d(LOGNAME, "Action move");
                        path.lineTo(event.getX(), event.getY());
                        com.example.pdfreader.Pair curr2 = new com.example.pdfreader.Pair(event.getX(), event.getY());
                        points.add(curr2);
                        invalidate();

                    } else {
                        final float x = event.getX();
                        final float y = event.getY();

                        //calculate the distance in x and y directions
                        final float distanceX = x - mLastTouchX;
                        final float distanceY = y - mLastTouchY;

                        mPositionX += distanceX;
                        mPositionY += distanceY;

                        //remember this touch position for next move event
                        mLastTouchX = x;
                        mLastTouchY = y;

                        //redraw canvas call onDraw method
                        invalidate();

                    }
                    break;
                case MotionEvent.ACTION_UP:
                    if(model.getHIGHLIGHT() || model.getANNOTATE()) {
                        Log.d(LOGNAME, "Action up");
                        Pair<Path, ArrayList<com.example.pdfreader.Pair>> temp = new Pair<>(path, points);
                        coordinates.add(temp);
                        break;
                    }
            }

        return true;
    }

    // set image as background
    public void setImage(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    @Override
    protected void onDraw(Canvas canvas) {
//        canvas.save();
        canvas.translate(mPositionX, mPositionY);
        Log.d(LOGNAME, String.valueOf(mPositionX));
        canvas.scale(curr_scale, curr_scale);

        // draw background
        if (bitmap != null) {
            this.setImageBitmap(bitmap);
        }
        // Drawing all the annotations
        for (Path path : annotations) {
            canvas.drawPath(path, annotation_paint);
        }

        // Drawing all the Highlights
        for (Path path : highlights) {
            canvas.drawPath(path, highlight_paint);
        }
        canvas.save();
        super.onDraw(canvas);
    }


    // Set Paint for annotation
    void paint_annotation(Paint paint){
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
        paint.setColor(Color.BLACK);
    }


    // Set Paint for annotation
    void paint_highlight(Paint paint){
        paint.setStrokeWidth(15);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(ColorUtils.setAlphaComponent(Color.YELLOW,255));
    }


    // Hit test for all the paths
    boolean hit_test(float x, float y){
        for (int i = 0; i < coordinates.size(); ++i){
            for (int j = 0; j < coordinates.get(i).second.size(); ++j){
                if( (x < (coordinates.get(i).second.get(j).first + 20)) && (x > coordinates.get(i).second.get(j).first - 20 ) &&
                        (y < (coordinates.get(i).second.get(j).second + 20)) && (y > coordinates.get(i).second.get(j).second - 20 )){
                    Log.d(LOGNAME, String.valueOf(x) + ", " + String.valueOf(y));
                    path_del = coordinates.get(i).first;
                    coordinates.remove(coordinates.get(i));
                    return true;
                }
            }
        }
        return false;
    }

    // Undo the task
    void undo(){
        // Checking if there isnt any move to undo
        if(UndoStack.isEmpty()){
            return;
        }
        // Retrieving the last thing done
        Pair<String, Path> last = UndoStack.elementAt(UndoStack.size() - 1);
        if (last.first.equals("Annotation")){
            annotations.remove(last.second);
            RedoStack.add(last);
            UndoStack.remove(last);
            invalidate();
        } else if (last.first.equals("Highlight")){
            highlights.remove(last.second);
            RedoStack.add(last);
            UndoStack.remove(last);
            invalidate();
        } else if (last.first.equals(("Erase-Annotation"))){
            annotations.add(last.second);
            RedoStack.add(last);
            UndoStack.remove(last);
            invalidate();
        } else if (last.first.equals("Erase-Highlight")){
            highlights.add(last.second);
            RedoStack.add(last);
            UndoStack.remove(last);
            invalidate();
        }
    }


    // Implement Redo actions
    void redo(){
        // Checking if there isnt any move to undo
        if(RedoStack.isEmpty()){
            return;
        }
        // Retrieving the last thing done
        Pair<String, Path> last = RedoStack.elementAt(RedoStack.size() - 1);

        if (last.first.equals(("Annotation"))){
            annotations.add(last.second);
            RedoStack.remove(last);
            UndoStack.add(last);
            invalidate();
        } else if (last.first.equals("Highlight")){
            highlights.add(last.second);
            RedoStack.remove(last);
            UndoStack.add(last);
            invalidate();
        } else if (last.first.equals("Erase-Annotation")){
            annotations.remove(last.second);
            UndoStack.add(last);
            RedoStack.remove(last);
            invalidate();
        } else if (last.first.equals("Erase-Highlight")){
            highlights.remove(last.second);
            UndoStack.add(last);
            RedoStack.remove(last);
            invalidate();
        }
    }


    @Override
    public void update(Observable o, Object arg) {
    }

    // ==== ScaleListener to implement zooming ====

    int width = 1000;

    int height = 2000;

    float translate_X = 0;

    float translate_Y = 0 ;

    private class ScaleListener
            extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            curr_scale *= detector.getScaleFactor();

            // Don't let the object get too small or too large.
            curr_scale = Math.max(0.1f, Math.min(curr_scale, 5.0f));
            Log.d(LOGNAME, "Action down");
            translate_X = detector.getCurrentSpanX();
            translate_Y = detector.getCurrentSpanY();
            invalidate();
            return true;
        }

    }
}


