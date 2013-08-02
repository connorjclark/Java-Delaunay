/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hoten.voronoi;

import hoten.geom.Point;
import java.util.ArrayList;

/**
 * Corner.java Function Date Jun 6, 2013
 *
 * @author Connor
 */
public class Corner {

    public ArrayList<Center> touches = new ArrayList(); //good
    public ArrayList<Corner> adjacent = new ArrayList(); //good
    public ArrayList<Edge> protrudes = new ArrayList();
    public Point loc;
    public int index;
    public boolean border;
    
    public double elevation;
    public boolean water, ocean, coast;
    public Corner downslope;
    public int river;
    public double moisture;
    
}
