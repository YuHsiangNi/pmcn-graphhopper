package com.graphhopper.GPXUtil;

import com.graphhopper.util.DistanceCalc;
import com.graphhopper.util.DistanceCalc3D;
import com.graphhopper.util.PointAccess;
import com.graphhopper.util.PointList;
import com.graphhopper.util.shapes.GHPoint;
import com.graphhopper.util.shapes.GHPoint3D;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class PointListCustom implements Iterable<GHPoint3D>, PointAccess {
    public static final com.graphhopper.util.PointList EMPTY = new com.graphhopper.util.PointList(0, true) {
        @Override
        public void set(int index, double lat, double lon, double ele) {
            throw new RuntimeException("cannot change EMPTY PointList");
        }

        @Override
        public void add(double lat, double lon, double ele) {
            throw new RuntimeException("cannot change EMPTY PointList");
        }

        @Override
        public void removeLastPoint() {
            throw new RuntimeException("cannot change EMPTY PointList");
        }

        @Override
        public double getLatitude(int index) {
            throw new RuntimeException("cannot access EMPTY PointList");
        }

        @Override
        public double getLongitude(int index) {
            throw new RuntimeException("cannot access EMPTY PointList");
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public void clear() {
            throw new RuntimeException("cannot change EMPTY PointList");
        }

        @Override
        public void setElevation(int index, double ele) {
            throw new RuntimeException("cannot change EMPTY PointList");
        }

        @Override
        public void trimToSize(int newSize) {
            throw new RuntimeException("cannot change EMPTY PointList");
        }

        @Override
        public void parse2DJSON(String str) {
            throw new RuntimeException("cannot change EMPTY PointList");
        }

        @Override
        public double calcDistance(DistanceCalc calc) {
            throw new UnsupportedOperationException("cannot access EMPTY PointList");
        }

        @Override
        public com.graphhopper.util.PointList copy(int from, int end) {
            throw new RuntimeException("cannot copy EMPTY PointList");
        }

        @Override
        public com.graphhopper.util.PointList clone(boolean reverse) {
            throw new UnsupportedOperationException("cannot access EMPTY PointList");
        }

        @Override
        public double getElevation(int index) {
            throw new UnsupportedOperationException("cannot access EMPTY PointList");
        }

        @Override
        public double getLat(int index) {
            throw new UnsupportedOperationException("cannot access EMPTY PointList");
        }

        @Override
        public double getLon(int index) {
            throw new UnsupportedOperationException("cannot access EMPTY PointList");
        }

        @Override
        public double getEle(int index) {
            throw new UnsupportedOperationException("cannot access EMPTY PointList");
        }

        @Override
        public void reverse() {
            throw new UnsupportedOperationException("cannot change EMPTY PointList");
        }

        @Override
        public int getSize() {
            return 0;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public GHPoint3D toGHPoint(int index) {
            throw new UnsupportedOperationException("cannot access EMPTY PointList");
        }

        @Override
        public boolean is3D() {
            throw new UnsupportedOperationException("cannot access EMPTY PointList");
        }
    };

    private static final DistanceCalc3D distCalc3D = new DistanceCalc3D();
    static final String ERR_MSG = "Tried to access PointList with too big index!";
    protected int size;
    protected boolean is3D;
    private double[] latitudes;
    private double[] longitudes;
    private double[] elevations;
    private double[] accuracy;
    private String[] time;
    private String[] arg1;
    private boolean isImmutable;

    public PointListCustom() {
        this(10, false);
    }

    public PointListCustom(int cap, boolean is3D) {
        this.size = 0;
        this.isImmutable = false;
        this.latitudes = new double[cap];
        this.longitudes = new double[cap];
        this.accuracy = new double[cap];
        this.time = new String[cap];
        this.arg1 = new String[cap];
        this.is3D = is3D;
        if (is3D) {
            this.elevations = new double[cap];
        }
    }

    public boolean is3D() {
        return this.is3D;
    }

    public int getDimension() {
        return this.is3D ? 3 : 2;
    }

    public void ensureNode(int nodeId) {
        this.incCap(nodeId + 1);
    }

    public void setNode(int nodeId, double lat, double lon) {
        this.set(nodeId, lat, lon, 0.0D / 0.0);
    }

    public void setNode(int nodeId, double lat, double lon, double ele) {
        this.set(nodeId, lat, lon, ele);
    }

    public void set(int index, double lat, double lon, double ele) {
        this.ensureMutability();
        if (index >= this.size) {
            throw new ArrayIndexOutOfBoundsException("index has to be smaller than size " + this.size);
        } else {
            this.latitudes[index] = lat;
            this.longitudes[index] = lon;
            if (this.is3D) {
                this.elevations[index] = ele;
            } else if (!Double.isNaN(ele)) {
                throw new IllegalStateException("This is a 2D list we cannot store elevation: " + ele);
            }
        }
    }

    public void setPLC(int index, double lat, double lon, double ele, double accu, String time) {
        this.ensureMutability();
        if (index >= this.size) {
            throw new ArrayIndexOutOfBoundsException("index has to be smaller than size " + this.size);
        } else {
            this.latitudes[index] = lat;
            this.longitudes[index] = lon;
            if (this.is3D) {
                this.elevations[index] = ele;
            } else if (!Double.isNaN(ele)) {
//                throw new IllegalStateException("This is a 2D list we cannot store elevation: " + ele);
            }
            this.accuracy[index] = accu;
            this.time[index] = time;

        }
    }

    private void incCap(int newSize) {
        if (newSize > this.latitudes.length) {
            int cap = newSize * 2;
            if (cap < 15) {
                cap = 15;
            }

            this.latitudes = Arrays.copyOf(this.latitudes, cap);
            this.longitudes = Arrays.copyOf(this.longitudes, cap);
            this.accuracy = Arrays.copyOf(this.accuracy, cap);
            this.time = Arrays.copyOf(this.time, cap);
            this.arg1 = Arrays.copyOf(this.arg1, cap);

            if (this.is3D) {
                this.elevations = Arrays.copyOf(this.elevations, cap);
            }

        }
    }

    public void add(double lat, double lon) {
        if (this.is3D) {
            throw new IllegalStateException("Cannot add point without elevation data in 3D mode");
        } else {
            this.add(lat, lon,Double.NaN);
        }
    }

    public void add(double lat, double lon, double ele) {
        this.add(lat, lon, ele, 0,"");
    }

    public void add(double lat, double lon, double ele, double accu , String time) {
        this.ensureMutability();
        int newSize = this.size + 1;
        this.incCap(newSize);
        this.latitudes[this.size] = lat;
        this.longitudes[this.size] = lon;
        this.accuracy[this.size] = accu;
        this.time[this.size] = time;
        if (this.is3D) {
            this.elevations[this.size] = ele;
        } else if (!Double.isNaN(ele)) {
            throw new IllegalStateException("This is a 2D list we cannot store elevation: " + ele);
        }

        this.size = newSize;
    }

    public void add(PointAccess nodeAccess, int index) {
        if (this.is3D) {
            this.add(nodeAccess.getLatitude(index), nodeAccess.getLongitude(index), nodeAccess.getElevation(index));
        } else {
            this.add(nodeAccess.getLatitude(index), nodeAccess.getLongitude(index));
        }

    }

    public void add(GHPoint point,Double acc, String time) {
        if (this.is3D) {
            this.add(point.lat, point.lon, ((GHPoint3D)point).ele);
        } else {
            this.add(point.lat, point.lon,Double.NaN,acc,time);
        }

    }

    public void add(com.graphhopper.util.PointList points) {
        this.ensureMutability();
        int newSize = this.size + points.getSize();
        this.incCap(newSize);

        for(int i = 0; i < points.getSize(); ++i) {
            int tmp = this.size + i;
            this.latitudes[tmp] = points.getLatitude(i);
            this.longitudes[tmp] = points.getLongitude(i);
            if (this.is3D) {
                this.elevations[tmp] = points.getElevation(i);
            }
        }

        this.size = newSize;
    }

    public void removeLastPoint() {
        if (this.size == 0) {
            throw new IllegalStateException("Cannot remove_copy last point from empty PointList");
        } else {
            --this.size;
        }
    }

    public int size() {
        return this.size;
    }

    public int getSize() {
        return this.size();
    }

    public boolean isEmpty() {
        return this.size() == 0;
    }

    public double getLat(int index) {
        return this.getLatitude(index);
    }

    public double getLatitude(int index) {
        if (index >= this.size) {
            throw new ArrayIndexOutOfBoundsException("Tried to access PointList with too big index! index:" + index + ", size:" + this.size);
        } else {
            return this.latitudes[index];
        }
    }

    public double getLon(int index) {
        return this.getLongitude(index);
    }

    public double getLongitude(int index) {
        if (index >= this.size) {
            throw new ArrayIndexOutOfBoundsException("Tried to access PointList with too big index! index:" + index + ", size:" + this.size);
        } else {
            return this.longitudes[index];
        }
    }

    public double getElevation(int index) {
        if (index >= this.size) {
            throw new ArrayIndexOutOfBoundsException("Tried to access PointList with too big index! index:" + index + ", size:" + this.size);
        } else {
            return !this.is3D ? 0.0D / 0.0 : this.elevations[index];
        }
    }

    public double getEle(int index) {
        return this.getElevation(index);
    }

    public double getAccuracy(int index){
        if (index >= this.size) {
            throw new ArrayIndexOutOfBoundsException("Tried to access PointList with too big index! index:" + index + ", size:" + this.size);
        } else {
            return this.accuracy[index];
        }
    }


    public String getTime(int index){
        if (index >= this.size) {
            throw new ArrayIndexOutOfBoundsException("Tried to access PointList with too big index! index:" + index + ", size:" + this.size);
        } else {
            return this.time[index];
        }
    }

    public void reverse() {
        this.ensureMutability();
        int max = this.size / 2;

        for(int i = 0; i < max; ++i) {
            int swapIndex = this.size - i - 1;
            double tmp = this.latitudes[i];
            this.latitudes[i] = this.latitudes[swapIndex];
            this.latitudes[swapIndex] = tmp;
            tmp = this.longitudes[i];
            this.longitudes[i] = this.longitudes[swapIndex];
            this.longitudes[swapIndex] = tmp;
            if (this.is3D) {
                tmp = this.elevations[i];
                this.elevations[i] = this.elevations[swapIndex];
                this.elevations[swapIndex] = tmp;
            }
            tmp = this.accuracy[i];
            this.accuracy[i] = this.accuracy[swapIndex];
            this.accuracy[swapIndex] = tmp;

            String tmp_str = this.time[i];
            this.time[i] = this.time[swapIndex];
            this.time[swapIndex] = tmp_str;

        }

    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        for(int i = 0; i < this.getSize(); ++i) {
            if (i > 0) {
                sb.append(", ");
            }

            sb.append('(');
            sb.append(this.getLongitude(i));
            sb.append(',');
            sb.append(this.getLatitude(i));
            if (this.is3D()) {
                sb.append(',');
                sb.append(this.getElevation(i));
            }
            sb.append(',');
            sb.append(this.getAccuracy(i));
            sb.append(',');
            sb.append(this.getTime(i));

            sb.append(")  ");
        }

        return sb.toString();
    }

    public void clear() {
        ensureMutability();
        size = 0;
    }

    public void trimToSize(int newSize) {
        ensureMutability();
        if (newSize > size)
            throw new IllegalArgumentException("new size needs be smaller than old size");

        size = newSize;
    }

    public static final double round6(double value) {
        return Math.round(value * 1e6) / 1e6;
    }

    public static final double round2(double value) {
        return Math.round(value * 100) / 100d;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;

        PointListCustom other = (PointListCustom) obj;
        if (this.isEmpty() && other.isEmpty())
            return true;

        if (this.getSize() != other.getSize() || this.is3D() != other.is3D())
            return false;

        for (int i = 0; i < size(); i++) {
            if (!equalsEps(getLatitude(i), other.getLatitude(i)))
                return false;

            if (!equalsEps(getLongitude(i), other.getLongitude(i)))
                return false;

            if (this.is3D() && !equalsEps(getElevation(i), other.getElevation(i)))
                return false;
        }
        return true;
    }

    private final static double DEFAULT_PRECISION = 1e-6;

    public static boolean equalsEps(double d1, double d2) {
        return equalsEps(d1, d2, DEFAULT_PRECISION);
    }

    public static boolean equalsEps(double d1, double d2, double epsilon) {
        return Math.abs(d1 - d2) < epsilon;
    }

    /**
     * Clones this PointList. If this PointList was immutable, the cloned will be mutable. If this PointList was a
     * ShallowImmutablePointList, the cloned PointList will be a regular PointList.
     */
    public PointListCustom clone(boolean reverse) {
        PointListCustom clonePL = new PointListCustom(getSize(), is3D());
        if (is3D())
            for (int i = 0; i < getSize(); i++) {
                clonePL.add(getLatitude(i), getLongitude(i), getElevation(i),getAccuracy(i),getTime(i));
            }
        else
            for (int i = 0; i < getSize(); i++) {
                clonePL.add(getLatitude(i), getLongitude(i), getElevation(i),getAccuracy(i),getTime(i));
            }
        if (reverse)
            clonePL.reverse();
        return clonePL;
    }


    public GHPoint3D toGHPoint(int index) {
        return new GHPoint3D(this.getLatitude(index), this.getLongitude(index), this.getElevation(index));
    }

    public void remove(int RemoveOrder){
        if (RemoveOrder < 0 || RemoveOrder >= this.getSize()) {
            throw new IllegalArgumentException("Error removing, remove_copy order larger or smaller than PLC size.");
        }else if (!this.is3D()){
            for(int inew = RemoveOrder ; inew < this.getSize()-1; inew++){
                setPLC(inew , this.getLatitude(inew+1), this.getLongitude(inew+1), 0, this.getAccuracy(inew+1), this.getTime(inew+1));
            }
            this.size--;
        }else{
            for(int inew = RemoveOrder ; inew < this.getSize()-1; inew++){
                setPLC(inew , this.getLatitude(inew+1), this.getLongitude(inew+1), this.getElevation(inew+1), this.getAccuracy(inew+1), this.getTime(inew+1));
            }
            this.size--;
        }
    }


    @Override
    public Iterator<GHPoint3D> iterator() {
        return new Iterator<GHPoint3D>() {
            int counter = 0;

            @Override
            public boolean hasNext() {
                return counter < getSize();
            }

            @Override
            public GHPoint3D next() {
                if (counter >= getSize())
                    throw new NoSuchElementException();

                GHPoint3D point = PointListCustom.this.toGHPoint(counter);
                counter++;
                return point;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Not supported.");
            }
        };
    }

    public static PointList from(LineString lineString) {
        final PointList pointList = new PointList();
        for (Coordinate coordinate : lineString.getCoordinates()) {
            pointList.add(new GHPoint(coordinate.y, coordinate.x));
        }
        return pointList;
    }


    public boolean isImmutable() {
        return this.isImmutable;
    }

    /**
     * Once immutable, there is no way to make this object mutable again. This is done to ensure the consistency of
     * shallow copies. If you need to modify this object again, you have to create a deep copy of it.
     */
    public void makeImmutable() {
        this.isImmutable = true;
    }

    private void ensureMutability() {
        if (this.isImmutable()) {
            throw new IllegalStateException("You cannot change an immutable PointList");
        }
    }

}