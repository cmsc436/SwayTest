package cmsc436.umd.edu.sway;

/**
 * A Tribute to efficient and simple structure for holding data
 */

public class CoordinateList {
    private float x[];
    private float y[];
    private int size;
    private int maxSize = -1;

    public CoordinateList(int sz){
        maxSize = sz;
        init(maxSize);
    }

    public CoordinateList(CoordinateList ls ){
        maxSize = (int) (ls.getMaxSize() * 1.5);
        init(maxSize);
        System.arraycopy(ls.getXArray(),0,this.x,0,ls.getSize());
        System.arraycopy(ls.getYArray(),0,this.y,0,ls.getSize());
    }

    public void init(int sz){
        x = new float[sz];
        y = new float[sz];
        size = 0;
    }

    public void add(float xVal, float yVal){
        if(size >= maxSize) {
            maxSize *= 1.5;
            float[] temp = x;
            x = new float[maxSize];
            System.arraycopy(temp,0,this.x,0,temp.length);
            temp = y;
            y = new float[maxSize];
            System.arraycopy(temp,0,y,0,temp.length);
        }
        x[size] = xVal;
        y[size] = yVal;
        size++;
    }

    public float getX(int idx){
        return x[idx];
    }

    public float getY(int idx){
        return y[idx];
    }

    public float[] getXArray(){
        return x;
    }
    public float[] getYArray(){
        return y;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public int getSize() {
        return size;
    }


}
