package hoten.voronoi;

import hoten.geom.Point;
import java.util.ArrayList;

/**
 * Center.java
 *
 * @author Connor
 */
public class Center {

    public int index;
    public Point loc;
    public ArrayList<Corner> corners = new ArrayList();//good
    public ArrayList<Center> neighbors = new ArrayList();//good
    public ArrayList<Edge> borders = new ArrayList();
    public boolean border, ocean, water, coast;
    public double elevation;
    public double moisture;
    public Biomes biome;
    public double area;

    public Center() {
    }

    public Center(Point loc) {
        this.loc = loc;
    }
}
