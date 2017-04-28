package cmsc436.umd.edu.sway;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.util.Log;

import java.util.List;

/**
 * Created by csalaman on 4/27/2017.
 */

public class DisplayImages {

    // List of XY Coordinates and Center point
    List<MeasurementService.DataPoint> list;
    MeasurementService.DataPoint center;

    // Initialize the presets
    final int BITMAP_SIZE = 900;
    final float ACCELERATION_LIMIT = 4.5f; //max accle before someone falls
    final float CONSTANT = (BITMAP_SIZE/2) / ACCELERATION_LIMIT;

    // Constructor, params: XY Coordinate list, XY center
    public DisplayImages(List<MeasurementService.DataPoint> list, MeasurementService.DataPoint center){
        this.list = list;
        this.center = center;
    }

    public Bitmap getQuadrantAnalysis(){
        // Create bitmap to save image
        Bitmap bitmap = Bitmap.createBitmap(BITMAP_SIZE,BITMAP_SIZE, Bitmap.Config.ARGB_8888);
        // Define the paint
        Paint paint = new Paint();
        // Define shape and style for grids
        paint.setStyle(Paint.Style.STROKE); //CHANGE TO FILL LATER
        //Set stroke with
        paint.setStrokeWidth(2);
        // Create canvas, drawing block
        Canvas canvas = new Canvas(bitmap);
        // Set Background color
       // canvas.drawColor(Color.BLACK);
        // Set Paint Color
        paint.setColor(Color.BLACK);
        // Calculate the length of each Quadrant
        int H = (int)Math.sqrt(2*(BITMAP_SIZE*BITMAP_SIZE)/list.size());

       // int[][] quadrantCount = countQuadrant(list,H);
        //Draw heat map
        for(int i = 0; i < (int)BITMAP_SIZE/H; i++){
            for(int j = 0; j < (int)BITMAP_SIZE/H; j++){
                canvas.drawRect(i*H,j*H,(i+1)*H,(j+1)*H,paint);
//                double count = quadrantCount[j][i]/list.size();
//                // Setting up the heat
//                if( count <= .05){
//                    paint.setColor(Color.parseColor("#ffffb2"));
//                    canvas.drawRect(i*H,j*H,(i+1)*H,(j+1)*H,paint);
//                }else if (count > .05 && count <= .1 ){
//                    paint.setColor(Color.parseColor("#fecc5c"));
//                    canvas.drawRect(i*H,j*H,(i+1)*H,(j+1)*H,paint);
//                }else if (count > .1 && count <= .15){
//                    paint.setColor(Color.parseColor("#fd8d3c"));
//                    canvas.drawRect(i*H,j*H,(i+1)*H,(j+1)*H,paint);
//                }else if(count > .15 && count <= .2){
//                    paint.setColor(Color.parseColor("#f03b20"));
//                    canvas.drawRect(i*H,j*H,(i+1)*H,(j+1)*H,paint);
//                }else{
//                    paint.setColor(Color.parseColor("#bd0026"));
//                    canvas.drawRect(i*H,j*H,(i+1)*H,(j+1)*H,paint);
//                }
            }
        }
        float[] trans = getTranslationVector(center.getX(),center.getY(),BITMAP_SIZE,BITMAP_SIZE,CONSTANT);
        // Plot points
        for (MeasurementService.DataPoint p: list) {
            paint.setColor(Color.RED);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle((p.getX()*CONSTANT)+trans[0],(p.getY()*CONSTANT)+trans[1],5.5f,paint);
        }


        return bitmap;
    }


    // Creates a drawing of the path with boundaries
    public Bitmap getPath(){

        // Path defines line, Paint defines the color
        Path path = new Path();
        Paint paint = new Paint();

        paint.setAntiAlias(true);

        // Defines shape and size
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.SQUARE);

        // Creates a drawing block
        Bitmap bitmap = Bitmap.createBitmap(
                BITMAP_SIZE,
                BITMAP_SIZE,
                Bitmap.Config.ARGB_8888);

        // What draws on the bitmap
        Canvas canvas = new Canvas(bitmap);

        // Background color
        canvas.drawColor(Color.LTGRAY);

        // Draws 4 circles: green, yellow, red, gray
        paint.setStrokeWidth(15);
        paint.setColor(Color.GREEN);
        canvas.drawCircle(BITMAP_SIZE/2,BITMAP_SIZE/2,BITMAP_SIZE *.125f,paint);
        paint.setColor(Color.YELLOW);
        canvas.drawCircle(BITMAP_SIZE/2,BITMAP_SIZE/2,BITMAP_SIZE *.25f,paint);
        paint.setColor(Color.RED);
        canvas.drawCircle(BITMAP_SIZE/2,BITMAP_SIZE/2,BITMAP_SIZE *.375f,paint);
        paint.setColor(Color.DKGRAY);
        canvas.drawCircle(BITMAP_SIZE/2,BITMAP_SIZE/2,BITMAP_SIZE *.5f,paint);

        // Defines a line being drawn
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(5);
        path.moveTo(BITMAP_SIZE/2,BITMAP_SIZE/2);

        // Translate the XY coordinates to our coordinate system
        float[] translationVector = getTranslationVector(
                center.getX(),
                center.getY(),
                BITMAP_SIZE,
                BITMAP_SIZE,
                CONSTANT);
        //Draws the path
        for(MeasurementService.DataPoint p: list){
            path.lineTo(
                    (p.getX() * CONSTANT) + translationVector[0],
                    (p.getY() * CONSTANT) + translationVector[1]
            );
        }
        // Finalize the drawings in to the bitmap; ("Push the drawings to the bitmap")
        canvas.drawPath(path,paint);

        // Return the bitmap
        return bitmap;
    }
    // Returns the length of the paths
    public float getMetric(){
        float distance = 0.0f;
        MeasurementService.DataPoint prv = center;

        for(MeasurementService.DataPoint d : list){
            distance += Math.sqrt(
                    Math.pow(prv.getX() - d.getX(),2) +
                            Math.pow(prv.getY() - d.getY(),2)
            );
            prv = d;
        }

        return distance;
    }
    // Iterate the list of XY points and determine its quadrant
    private int[][] countQuadrant(List<MeasurementService.DataPoint> list, int sizeOfQuadrants){
        int[][] quadrantsCounts = new int[BITMAP_SIZE/sizeOfQuadrants][BITMAP_SIZE/sizeOfQuadrants];
        float[] transXY;
        transXY = getTranslationVector(center.getX(),center.getY(),BITMAP_SIZE, BITMAP_SIZE, CONSTANT);
        for(int i = 0; i< list.size();i++) {
            float x = (list.get(i).getX());
            float y = (list.get(i).getY());
            quadrantsCounts[(int)(((x*CONSTANT) + transXY[0])/sizeOfQuadrants)][(int)(( (y*CONSTANT) + transXY[1])/sizeOfQuadrants)]++;
        }
        return quadrantsCounts;
    }


    //Translation of XY to X'Y' of our coordinate system
    private float[] getTranslationVector(float centerX, float centerY,
                                         int bitmapXLength, int bitmapYLength,
                                         float constant){
        return new float[]{
                -(centerX * constant) + (bitmapXLength/2),
                -(centerY * constant) + (bitmapYLength/2)
        };
    }






}
