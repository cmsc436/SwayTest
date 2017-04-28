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
 * Created by Glorious csalaman on 4/27/2017.
 */



public class DisplayImages {

    private class Region{
        int leftX;
        int topY;
        int rightX;
        int bottomY;

        public Region(int leftX, int topY, int rightX, int bottomY){
            this.leftX = leftX;
            this.topY = topY;
            this.rightX = rightX;
            this.bottomY = bottomY;
        }

    }



    // List of XY Coordinates and Center point
    List<MeasurementService.DataPoint> list;
    MeasurementService.DataPoint center;

    // Initialize the presets
    final int BITMAP_SIZE = 900;
    final float ACCELERATION_LIMIT = 4.5f; //max accle before someone falls
    final float CONSTANT = (BITMAP_SIZE/2) / ACCELERATION_LIMIT;

    //Regions of points
    Region[][] regions;

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
        paint.setStyle(Paint.Style.FILL); //CHANGE TO FILL LATER
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

        // Determine the regions of points
        regions = new Region[BITMAP_SIZE/H][BITMAP_SIZE/H]; // MAke H*H later?
        for(int y = 0; y < (int)BITMAP_SIZE/H; y++){
            for(int x = 0; x < (int)BITMAP_SIZE/H; x++){
                regions[x][y] = new Region(x*H,y*H,(x+1)*H,(y+1)*H);
            }
        }


        int[][] quadrantCount = countQuadrant(list,H);
        /*Draw heat map
         * Note: The params:
         *  - drawRect(X_on_Left,Y_on_Top,X_on_Right,Y_on_Bottom,paint)
        */
        for(int y = 0; y < (int)BITMAP_SIZE/H; y++){
            for(int x = 0; x < (int)BITMAP_SIZE/H; x++){
                Log.e("Dimensions of GRid",BITMAP_SIZE/H+" x "+BITMAP_SIZE/H);
                //canvas.drawRect(x*H,y*H,(x+1)*H,(y+1)*H,paint); // TRY WITH STROKE, CHANGING IT
                // Get the proportion of the count with size
                double count = (quadrantCount[x][y]);
                // Setting up the heat
                if( count <= .05){
                    Log.e("LESS THAN 5%",""+count);
                    paint.setColor(Color.parseColor("#ffffb2"));
                    canvas.drawRect(x*H,y*H,(x+1)*H,(y+1)*H,paint);
                }else if (count > .05 && count <= .1 ){
                    Log.e("Between 5% and 10%",""+count);
                    paint.setColor(Color.parseColor("#fecc5c"));
                    canvas.drawRect(x*H,y*H,(x+1)*H,(y+1)*H,paint);
                }else if (count > .1 && count <= .15){
                    Log.e("Betwwen 10% and 15%",""+count);
                    paint.setColor(Color.parseColor("#fd8d3c"));
                    canvas.drawRect(x*H,y*H,(x+1)*H,(y+1)*H,paint);
                }else if(count > .15 && count <= .2){
                    Log.e("Between 15% and 20%",""+count);
                    paint.setColor(Color.parseColor("#f03b20"));
                    canvas.drawRect(x*H,y*H,(x+1)*H,(y+1)*H,paint);
                }else{
                    Log.e("MORE THAN 20%",""+count);
                    paint.setColor(Color.parseColor("#bd0026"));
                    canvas.drawRect(x*H,y*H,(x+1)*H,(y+1)*H,paint);
                }
            }
        }
        float[] trans = getTranslationVector(center.getX(),center.getY(),BITMAP_SIZE,BITMAP_SIZE,CONSTANT);
        // Plot points
        for (MeasurementService.DataPoint p: list) {
            paint.setColor(Color.GREEN);
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
    private int[][] countQuadrant(List<MeasurementService.DataPoint> list, int H){
        int[][] quadrantsCounts = new int[(int)BITMAP_SIZE/H][(int)BITMAP_SIZE/H];
        float[] transXY;
        transXY = getTranslationVector(center.getX(),center.getY(),BITMAP_SIZE, BITMAP_SIZE, CONSTANT);

        //Testing max,min
        int min = 999999999;
        int max = -999999999;
        // TESTING

        for (MeasurementService.DataPoint p : list) {
          // quadrantsCounts[(int)((p.getX()*CONSTANT)+transXY[0])/sizeOfQuadrants][(int)((p.getY()*CONSTANT)+transXY[1])/sizeOfQuadrants]+=1;
            int transX = (int)((p.getX()*CONSTANT)+ transXY[0]);
            int transY = (int)((p.getY()*CONSTANT)+ transXY[1]);

            int[] detQuad = determineRegion(transX,transY,H);
            if(detQuad != null){
                quadrantsCounts[detQuad[0]][detQuad[1]]++;

                //TESTING
                if(quadrantsCounts[detQuad[0]][detQuad[1]] > max){
                    max = quadrantsCounts[detQuad[0]][detQuad[1]];
                }else if(quadrantsCounts[detQuad[0]][detQuad[1]] < min){
                    min = quadrantsCounts[detQuad[0]][detQuad[1]];
                }

                //TESTING
            }
        }
        Log.e("Minimum count",""+min);
        Log.e("Maximum count",""+max);
        Log.e("List size",""+list.size());

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

    // Function to determine to region of a specific point, return null if none
    private int[] determineRegion(int X, int Y ,int H){
        int[] quadrantPoint = new int[2];
        for(int y = 0; y < (int)BITMAP_SIZE/H; y++){
            for(int x = 0; x < (int)BITMAP_SIZE/H; x++){
                Region reg = regions[x][y];
                if (X >= reg.leftX && X <= reg.rightX && Y >= reg.topY && Y <= reg.bottomY){
                    quadrantPoint[0] = x;
                    quadrantPoint[1] = y;
                    return quadrantPoint;
                }
            }
        }

        return null;
    }




}
