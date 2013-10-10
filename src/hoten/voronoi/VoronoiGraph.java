package hoten.voronoi;

import hoten.geom.Point;
import hoten.geom.Rectangle;
import hoten.voronoi.nodename.as3delaunay.LineSegment;
import hoten.voronoi.nodename.as3delaunay.Voronoi;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

/**
 * VoronoiGraph.java
 *
 * @author Connor
 */
public class VoronoiGraph {

    final public ArrayList<Edge> edges = new ArrayList();
    final public ArrayList<Corner> corners = new ArrayList();
    final public ArrayList<Center> centers = new ArrayList();
    final public Rectangle bounds;
    final private Random r;

    public VoronoiGraph(Voronoi v, int numLloydRelaxations, Random r) {
        this.r = r;
        bumps = r.nextInt(5) + 1;
        startAngle = r.nextDouble() * 2 * Math.PI;
        dipAngle = r.nextDouble() * 2 * Math.PI;
        dipWidth = r.nextDouble() * .5 + .2;
        bounds = v.get_plotBounds();
        for (int i = 0; i < numLloydRelaxations; i++) {
            ArrayList<Point> points = v.siteCoords();
            for (Point p : points) {
                ArrayList<Point> region = v.region(p);
                double x = 0;
                double y = 0;
                for (Point c : region) {
                    x += c.x;
                    y += c.y;
                }
                x /= region.size();
                y /= region.size();
                p.x = x;
                p.y = y;
            }
            v = new Voronoi(points, null, v.get_plotBounds());
        }
        buildGraph(v);
        improveCorners();

        assignCornerElevations();
        assignOceanCoastAndLand();
        redistributeElevations(landCorners());
        assignPolygonElevations();

        calculateDownslopes();
        //calculateWatersheds();
        createRivers();
        assignCornerMoisture();
        redistributeMoisture(landCorners());
        assignPolygonMoisture();
        assignBiomes();
        calculateAreas();
    }
    public BufferedImage img;

    public Center getCenterOf(int x, int y) {
        return centers.get(img.getRGB(x, y) & 0xffffff);
    }

    private void calculateAreas() {
        img = new BufferedImage((int) bounds.width, (int) bounds.height, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g = img.createGraphics();
        for (Center c : centers) {
            g.setColor(new Color(c.index));

            //only used if Center c is on the edge of the graph. allows for completely filling in the outer polygons
            Corner edgeCorner1 = null;
            Corner edgeCorner2 = null;
            for (Center n : c.neighbors) {
                Edge e = edgeWithCenters(c, n);

                if (e.v0 == null) {
                    //outermost voronoi edges aren't stored in the graph
                    continue;
                }

                //find a corner on the exterior of the graph
                //if this Edge e has one, then it must have two,
                //finding these two corners will give us the missing
                //triangle to render. this special triangle is handled
                //outside this for loop
                Corner cornerWithOneAdjacent = e.v0.border ? e.v0 : e.v1;
                if (cornerWithOneAdjacent.border) {
                    if (edgeCorner1 == null) {
                        edgeCorner1 = cornerWithOneAdjacent;
                    } else {
                        edgeCorner2 = cornerWithOneAdjacent;
                    }
                }

                drawTriangle(g, e.v0, e.v1, c);
                c.area += (c.loc.x * (e.v0.loc.y - e.v1.loc.y) + e.v0.loc.x * (e.v1.loc.y - c.loc.y) + e.v1.loc.x * (c.loc.y - e.v0.loc.y)) / 2;
            }

            //handle the missing triangle
            if (edgeCorner2 != null) {
                //if these two outer corners are NOT on the same exterior edge of the graph,
                //then we actually must render a polygon (w/ 4 points) and take into consideration
                //one of the four corners (either 0,0 or 0,height or width,0 or width,height)
                //note: the 'missing polygon' may have more than just 4 points. this
                //is common when the number of sites are quite low (less than 5), but not a problem
                //with a more useful number of sites. 
                //TODO: find a way to fix this

                if (closeEnough(edgeCorner1.loc.x, edgeCorner2.loc.x, 1)) {
                    drawTriangle(g, edgeCorner1, edgeCorner2, c);
                } else {
                    int[] x = new int[4];
                    int[] y = new int[4];
                    x[0] = (int) c.loc.x;
                    y[0] = (int) c.loc.y;
                    x[1] = (int) edgeCorner1.loc.x;
                    y[1] = (int) edgeCorner1.loc.y;

                    //determine which corner this is
                    x[2] = (int) ((closeEnough(edgeCorner1.loc.x, bounds.x, 1) || closeEnough(edgeCorner2.loc.x, bounds.x, .5)) ? bounds.x : bounds.right);
                    y[2] = (int) ((closeEnough(edgeCorner1.loc.y, bounds.y, 1) || closeEnough(edgeCorner2.loc.y, bounds.y, .5)) ? bounds.y : bounds.bottom);

                    x[3] = (int) edgeCorner2.loc.x;
                    y[3] = (int) edgeCorner2.loc.y;

                    g.fillPolygon(x, y, 4);
                }
            }
        }
    }

    private void improveCorners() {
        Point[] newP = new Point[corners.size()];
        for (Corner c : corners) {
            if (c.border) {
                newP[c.index] = c.loc;
            } else {
                double x = 0;
                double y = 0;
                for (Center center : c.touches) {
                    x += center.loc.x;
                    y += center.loc.y;
                }
                newP[c.index] = new Point(x / c.touches.size(), y / c.touches.size());
            }
        }
        for (Corner c : corners) {
            c.loc = newP[c.index];
        }
        for (Edge e : edges) {
            if (e.v0 != null && e.v1 != null) {
                e.setVornoi(e.v0, e.v1);
            }
        }
    }

    private Edge edgeWithCenters(Center c1, Center c2) {
        for (Edge e : c1.borders) {
            if (e.d0 == c2 || e.d1 == c2) {
                return e;
            }
        }
        return null;
    }

    private void drawTriangle(Graphics2D g, Corner c1, Corner c2, Center center) {
        int[] x = new int[3];
        int[] y = new int[3];
        x[0] = (int) center.loc.x;
        y[0] = (int) center.loc.y;
        x[1] = (int) c1.loc.x;
        y[1] = (int) c1.loc.y;
        x[2] = (int) c2.loc.x;
        y[2] = (int) c2.loc.y;
        g.fillPolygon(x, y, 3);
    }

    private boolean closeEnough(double d1, double d2, double diff) {
        return Math.abs(d1 - d2) <= diff;
    }

    public void paint(Graphics2D g) {
        paint(g, true, false, false, false, false);
    }

    public void paint(Graphics2D g, boolean drawBg, boolean drawSites, boolean drawCorners, boolean drawDelaunay, boolean drawVoronoi) {
        final int numSites = centers.size();

        Color[] key = new Color[numSites];
        for (int i = 0; i < key.length; i++) {
            Random r = new Random(i);
            key[i] = new Color(r.nextInt(255), r.nextInt(255), r.nextInt(255));
        }

        //draw via triangles
        if (drawBg) {
            for (Center c : centers) {
                g.setColor(key[c.index]);
                if (c.ocean) {
                    g.setColor(new Color(OCEAN));
                } else if (c.water) {
                    g.setColor(new Color(LAKE));
                } else if (c.coast) {
                    g.setColor(new Color(BEACH));
                } else {
                    Color color;
                    switch (c.biome) {
                        case TUNDRA:
                            color = new Color(TUNDRA);
                            break;
                        case TROPICAL_SEASONAL_FOREST:
                            color = new Color(TROPICAL_SEASONAL_FOREST);
                            break;
                        case TROPICAL_RAIN_FOREST:
                            color = new Color(TROPICAL_RAIN_FOREST);
                            break;
                        case SUBTROPICAL_DESERT:
                            color = new Color(SUBTROPICAL_DESERT);
                            break;
                        case SNOW:
                            color = new Color(SNOW);
                            break;
                        case SCORCHED:
                            color = new Color(SCORCHED);
                            break;
                        case LAKESHORE:
                            color = new Color(LAKESHORE);
                            break;
                        case ICE:
                            color = new Color(ICE);
                            break;
                        case BEACH:
                            color = new Color(BEACH);
                            break;
                        case COAST:
                            color = new Color(COAST);
                            break;
                        case BARE:
                            color = new Color(BARE);
                            break;
                        case SHRUBLAND:
                            color = new Color(SHRUBLAND);
                            break;
                        case TAIGA:
                            color = new Color(TAIGA);
                            break;
                        case MARSH:
                            color = new Color(MARSH);
                            break;
                        default:
                            color = new Color(GRASSLAND);
                    }
                    g.setColor(color);
                }

                //only used if Center c is on the edge of the graph. allows for completely filling in the outer polygons
                Corner edgeCorner1 = null;
                Corner edgeCorner2 = null;
                for (Center n : c.neighbors) {
                    Edge e = edgeWithCenters(c, n);

                    if (e.v0 == null) {
                        //outermost voronoi edges aren't stored in the graph
                        continue;
                    }

                    //find a corner on the exterior of the graph
                    //if this Edge e has one, then it must have two,
                    //finding these two corners will give us the missing
                    //triangle to render. this special triangle is handled
                    //outside this for loop
                    Corner cornerWithOneAdjacent = e.v0.border ? e.v0 : e.v1;
                    if (cornerWithOneAdjacent.border) {
                        if (edgeCorner1 == null) {
                            edgeCorner1 = cornerWithOneAdjacent;
                        } else {
                            edgeCorner2 = cornerWithOneAdjacent;
                        }
                    }

                    drawTriangle(g, e.v0, e.v1, c);
                }

                //handle the missing triangle
                if (edgeCorner2 != null) {
                    //if these two outer corners are NOT on the same exterior edge of the graph,
                    //then we actually must render a polygon (w/ 4 points) and take into consideration
                    //one of the four corners (either 0,0 or 0,height or width,0 or width,height)
                    //note: the 'missing polygon' may have more than just 4 points. this
                    //is common when the number of sites are quite low (less than 5), but not a problem
                    //with a more useful number of sites. 
                    //TODO: find a way to fix this

                    if (closeEnough(edgeCorner1.loc.x, edgeCorner2.loc.x, 1)) {
                        drawTriangle(g, edgeCorner1, edgeCorner2, c);
                    } else {
                        int[] x = new int[4];
                        int[] y = new int[4];
                        x[0] = (int) c.loc.x;
                        y[0] = (int) c.loc.y;
                        x[1] = (int) edgeCorner1.loc.x;
                        y[1] = (int) edgeCorner1.loc.y;

                        //determine which corner this is
                        x[2] = (int) ((closeEnough(edgeCorner1.loc.x, bounds.x, 1) || closeEnough(edgeCorner2.loc.x, bounds.x, .5)) ? bounds.x : bounds.right);
                        y[2] = (int) ((closeEnough(edgeCorner1.loc.y, bounds.y, 1) || closeEnough(edgeCorner2.loc.y, bounds.y, .5)) ? bounds.y : bounds.bottom);

                        x[3] = (int) edgeCorner2.loc.x;
                        y[3] = (int) edgeCorner2.loc.y;

                        g.fillPolygon(x, y, 4);
                    }
                }
            }
        }


        for (Edge e : edges) {
            if (drawDelaunay) {
                g.setStroke(new BasicStroke(1));
                g.setColor(Color.YELLOW);
                g.drawLine((int) e.d0.loc.x, (int) e.d0.loc.y, (int) e.d1.loc.x, (int) e.d1.loc.y);
            }
            if (e.river > 0) {
                g.setStroke(new BasicStroke(1 + (int) Math.sqrt(e.river * 2)));
                g.setColor(new Color(RIVER));
                g.drawLine((int) e.v0.loc.x, (int) e.v0.loc.y, (int) e.v1.loc.x, (int) e.v1.loc.y);
            }
        }

        if (drawSites) {
            g.setColor(Color.BLACK);
            for (Center s : centers) {
                g.fillOval((int) (s.loc.x - 2), (int) (s.loc.y - 2), 4, 4);
            }
        }

        if (drawCorners) {
            g.setColor(Color.WHITE);
            for (Corner c : corners) {
                g.fillOval((int) (c.loc.x - 2), (int) (c.loc.y - 2), 4, 4);
            }
        }
        g.setColor(Color.WHITE);
        g.drawRect((int) bounds.x, (int) bounds.y, (int) bounds.width, (int) bounds.height);
    }

    private void buildGraph(Voronoi v) {
        final HashMap<Point, Center> pointCenterMap = new HashMap();
        final ArrayList<Point> points = v.siteCoords();
        for (Point p : points) {
            Center c = new Center();
            c.loc = p;
            c.index = centers.size();
            centers.add(c);
            pointCenterMap.put(p, c);
        }

        //bug fix
        for (Center c : centers) {
            v.region(c.loc);
        }

        final ArrayList<hoten.voronoi.nodename.as3delaunay.Edge> libedges = v.edges();
        final HashMap<Integer, Corner> pointCornerMap = new HashMap();

        for (hoten.voronoi.nodename.as3delaunay.Edge libedge : libedges) {
            final LineSegment vEdge = libedge.voronoiEdge();
            final LineSegment dEdge = libedge.delaunayLine();

            final Edge edge = new Edge();
            edge.index = edges.size();
            edges.add(edge);

            edge.v0 = makeCorner(pointCornerMap, vEdge.p0);
            edge.v1 = makeCorner(pointCornerMap, vEdge.p1);
            edge.d0 = pointCenterMap.get(dEdge.p0);
            edge.d1 = pointCenterMap.get(dEdge.p1);

            // Centers point to edges. Corners point to edges.
            if (edge.d0 != null) {
                edge.d0.borders.add(edge);
            }
            if (edge.d1 != null) {
                edge.d1.borders.add(edge);
            }
            if (edge.v0 != null) {
                edge.v0.protrudes.add(edge);
            }
            if (edge.v1 != null) {
                edge.v1.protrudes.add(edge);
            }

            // Centers point to centers.
            if (edge.d0 != null && edge.d1 != null) {
                addToCenterList(edge.d0.neighbors, edge.d1);
                addToCenterList(edge.d1.neighbors, edge.d0);
            }

            // Corners point to corners
            if (edge.v0 != null && edge.v1 != null) {
                addToCornerList(edge.v0.adjacent, edge.v1);
                addToCornerList(edge.v1.adjacent, edge.v0);
            }

            // Centers point to corners
            if (edge.d0 != null) {
                addToCornerList(edge.d0.corners, edge.v0);
                addToCornerList(edge.d0.corners, edge.v1);
            }
            if (edge.d1 != null) {
                addToCornerList(edge.d1.corners, edge.v0);
                addToCornerList(edge.d1.corners, edge.v1);
            }

            // Corners point to centers
            if (edge.v0 != null) {
                addToCenterList(edge.v0.touches, edge.d0);
                addToCenterList(edge.v0.touches, edge.d1);
            }
            if (edge.v1 != null) {
                addToCenterList(edge.v1.touches, edge.d0);
                addToCenterList(edge.v1.touches, edge.d1);
            }
        }
    }

    // Helper functions for the following for loop; ideally these
    // would be inlined
    private void addToCornerList(ArrayList<Corner> list, Corner c) {
        if (c != null && !list.contains(c)) {
            list.add(c);
        }
    }

    private void addToCenterList(ArrayList<Center> list, Center c) {
        if (c != null && !list.contains(c)) {
            list.add(c);
        }
    }

    //ensures that each corner is represented by only one corner object
    private Corner makeCorner(HashMap<Integer, Corner> pointCornerMap, Point p) {
        if (p == null) {
            return null;
        }
        int index = (int) ((int) p.x + (int) (p.y) * bounds.width * 2);
        Corner c = pointCornerMap.get(index);
        if (c == null) {
            c = new Corner();
            c.loc = p;
            c.border = bounds.liesOnAxes(p);
            c.index = corners.size();
            corners.add(c);
            pointCornerMap.put(index, c);
        }
        return c;
    }

    private void assignCornerElevations() {
        LinkedList<Corner> queue = new LinkedList();
        for (Corner c : corners) {
            c.water = isWater(c.loc);
            if (c.border) {
                c.elevation = 0;
                queue.add(c);
            } else {
                c.elevation = Double.MAX_VALUE;
            }
        }

        while (!queue.isEmpty()) {
            Corner c = queue.pop();
            for (Corner a : c.adjacent) {
                double newElevation = 0.01 + c.elevation;
                if (!c.water && !a.water) {
                    newElevation += 1;
                }
                if (newElevation < a.elevation) {
                    a.elevation = newElevation;
                    queue.add(a);
                }
            }
        }
    }
    double[][] noise;
    double ISLAND_FACTOR = 1.07;  // 1.0 means no small islands; 2.0 leads to a lot
    final int bumps;
    final double startAngle;
    final double dipAngle;
    final double dipWidth;

    //only the radial implementation of amitp's map generation
    //TODO implement more island shapes
    private boolean isWater(Point p) {
        p = new Point(2 * (p.x / bounds.width - 0.5), 2 * (p.y / bounds.height - 0.5));

        double angle = Math.atan2(p.y, p.x);
        double length = 0.5 * (Math.max(Math.abs(p.x), Math.abs(p.y)) + p.length());

        double r1 = 0.5 + 0.40 * Math.sin(startAngle + bumps * angle + Math.cos((bumps + 3) * angle));
        double r2 = 0.7 - 0.20 * Math.sin(startAngle + bumps * angle - Math.sin((bumps + 2) * angle));
        if (Math.abs(angle - dipAngle) < dipWidth
                || Math.abs(angle - dipAngle + 2 * Math.PI) < dipWidth
                || Math.abs(angle - dipAngle - 2 * Math.PI) < dipWidth) {
            r1 = r2 = 0.2;
        }
        return !(length < r1 || (length > r1 * ISLAND_FACTOR && length < r2));

        //return false;

        /*if (noise == null) {
         noise = new Perlin2d(.125, 8, MyRandom.seed).createArray(257, 257);
         }
         int x = (int) ((p.x + 1) * 128);
         int y = (int) ((p.y + 1) * 128);
         return noise[x][y] < .3 + .3 * p.l2();*/

        /*boolean eye1 = new Point(p.x - 0.2, p.y / 2 + 0.2).length() < 0.05;
         boolean eye2 = new Point(p.x + 0.2, p.y / 2 + 0.2).length() < 0.05;
         boolean body = p.length() < 0.8 - 0.18 * Math.sin(5 * Math.atan2(p.y, p.x));
         return !(body && !eye1 && !eye2);*/
    }

    private void assignOceanCoastAndLand() {
        LinkedList<Center> queue = new LinkedList();
        final double waterThreshold = .3;
        for (final Center center : centers) {
            int numWater = 0;
            for (final Corner c : center.corners) {
                if (c.border) {
                    center.border = center.water = center.ocean = true;
                    queue.add(center);
                }
                if (c.water) {
                    numWater++;
                }
            }
            center.water = center.ocean || ((double) numWater / center.corners.size() >= waterThreshold);
        }
        while (!queue.isEmpty()) {
            final Center center = queue.pop();
            for (final Center n : center.neighbors) {
                if (n.water && !n.ocean) {
                    n.ocean = true;
                    queue.add(n);
                }
            }
        }
        for (Center center : centers) {
            boolean oceanNeighbor = false;
            boolean landNeighbor = false;
            for (Center n : center.neighbors) {
                oceanNeighbor |= n.ocean;
                landNeighbor |= !n.water;
            }
            center.coast = oceanNeighbor && landNeighbor;
        }

        for (Corner c : corners) {
            int numOcean = 0;
            int numLand = 0;
            for (Center center : c.touches) {
                numOcean += center.ocean ? 1 : 0;
                numLand += !center.water ? 1 : 0;
            }
            c.ocean = numOcean == c.touches.size();
            c.coast = numOcean > 0 && numLand > 0;
            c.water = c.border || ((numLand != c.touches.size()) && !c.coast);
        }
    }

    private ArrayList<Corner> landCorners() {
        final ArrayList<Corner> list = new ArrayList();
        for (Corner c : corners) {
            if (!c.ocean && !c.coast) {
                list.add(c);
            }
        }
        return list;
    }

    private void redistributeElevations(ArrayList<Corner> landCorners) {
        Collections.sort(landCorners, new Comparator<Corner>() {
            @Override
            public int compare(Corner o1, Corner o2) {
                if (o1.elevation > o2.elevation) {
                    return 1;
                } else if (o1.elevation < o2.elevation) {
                    return -1;
                }
                return 0;
            }
        });

        final double SCALE_FACTOR = 1.1;
        for (int i = 0; i < landCorners.size(); i++) {
            double y = (double) i / landCorners.size();
            double x = Math.sqrt(SCALE_FACTOR) - Math.sqrt(SCALE_FACTOR * (1 - y));
            x = Math.min(x, 1);
            landCorners.get(i).elevation = x;
        }

        for (Corner c : corners) {
            if (c.ocean || c.coast) {
                c.elevation = 0.0;
            }
        }
    }

    private void assignPolygonElevations() {
        for (Center center : centers) {
            double total = 0;
            for (Corner c : center.corners) {
                total += c.elevation;
            }
            center.elevation = total / center.corners.size();
        }
    }

    private void calculateDownslopes() {
        for (Corner c : corners) {
            Corner down = c;
            //System.out.println("ME: " + c.elevation);
            for (Corner a : c.adjacent) {
                //System.out.println(a.elevation);
                if (a.elevation <= down.elevation) {
                    down = a;
                }
            }
            c.downslope = down;
        }
    }

    private void createRivers() {
        for (int i = 0; i < bounds.width / 2; i++) {
            Corner c = corners.get(r.nextInt(corners.size()));
            if (c.ocean || c.elevation < 0.3 || c.elevation > 0.9) {
                continue;
            }
            // Bias rivers to go west: if (q.downslope.x > q.x) continue;
            while (!c.coast) {
                if (c == c.downslope) {
                    break;
                }
                Edge edge = lookupEdgeFromCorner(c, c.downslope);
                if (!edge.v0.water || !edge.v1.water) {
                    edge.river++;
                    c.river++;
                    c.downslope.river++;  // TODO: fix double count
                }
                c = c.downslope;
            }
        }
    }

    private Edge lookupEdgeFromCorner(Corner c, Corner downslope) {
        for (Edge e : c.protrudes) {
            if (e.v0 == downslope || e.v1 == downslope) {
                return e;
            }
        }
        return null;
    }

    private void assignCornerMoisture() {
        LinkedList<Corner> queue = new LinkedList();
        for (Corner c : corners) {
            if ((c.water || c.river > 0) && !c.ocean) {
                c.moisture = c.river > 0 ? Math.min(3.0, (0.2 * c.river)) : 1.0;
                queue.push(c);
            } else {
                c.moisture = 0.0;
            }
        }

        while (!queue.isEmpty()) {
            Corner c = queue.pop();
            for (Corner a : c.adjacent) {
                double newM = .9 * c.moisture;
                if (newM > a.moisture) {
                    a.moisture = newM;
                    queue.add(a);
                }
            }
        }

        // Salt water
        for (Corner c : corners) {
            if (c.ocean || c.coast) {
                c.moisture = 1.0;
            }
        }
    }

    private void redistributeMoisture(ArrayList<Corner> landCorners) {
        Collections.sort(landCorners, new Comparator<Corner>() {
            @Override
            public int compare(Corner o1, Corner o2) {
                if (o1.moisture > o2.moisture) {
                    return 1;
                } else if (o1.moisture < o2.moisture) {
                    return -1;
                }
                return 0;
            }
        });
        for (int i = 0; i < landCorners.size(); i++) {
            landCorners.get(i).moisture = (double) i / landCorners.size();
        }
    }

    private void assignPolygonMoisture() {
        for (Center center : centers) {
            double total = 0;
            for (Corner c : center.corners) {
                total += c.moisture;
            }
            center.moisture = total / center.corners.size();
        }
    }

    private Biomes getBiome(Center p) {
        if (p.ocean) {
            return Biomes.OCEAN;
        } else if (p.water) {
            if (p.elevation < 0.1) {
                return Biomes.MARSH;
            }
            if (p.elevation > 0.8) {
                return Biomes.ICE;
            }
            return Biomes.LAKE;
        } else if (p.coast) {
            return Biomes.BEACH;
        } else if (p.elevation > 0.8) {
            if (p.moisture > 0.50) {
                return Biomes.SNOW;
            } else if (p.moisture > 0.33) {
                return Biomes.TUNDRA;
            } else if (p.moisture > 0.16) {
                return Biomes.BARE;
            } else {
                return Biomes.SCORCHED;
            }
        } else if (p.elevation > 0.6) {
            if (p.moisture > 0.66) {
                return Biomes.TAIGA;
            } else if (p.moisture > 0.33) {
                return Biomes.SHRUBLAND;
            } else {
                return Biomes.TEMPERATE_DESERT;
            }
        } else if (p.elevation > 0.3) {
            if (p.moisture > 0.83) {
                return Biomes.TEMPERATE_RAIN_FOREST;
            } else if (p.moisture > 0.50) {
                return Biomes.TEMPERATE_DECIDUOUS_FOREST;
            } else if (p.moisture > 0.16) {
                return Biomes.GRASSLAND;
            } else {
                return Biomes.TEMPERATE_DESERT;
            }
        } else {
            if (p.moisture > 0.66) {
                return Biomes.TROPICAL_RAIN_FOREST;
            } else if (p.moisture > 0.33) {
                return Biomes.TROPICAL_SEASONAL_FOREST;
            } else if (p.moisture > 0.16) {
                return Biomes.GRASSLAND;
            } else {
                return Biomes.SUBTROPICAL_DESERT;
            }
        }
    }

    private void assignBiomes() {
        for (Center center : centers) {
            center.biome = getBiome(center);
        }
    }
    //
    //
    public int OCEAN = 0x44447a;
    public int COAST = 0x33335a;
    public int LAKESHORE = 0x225588;
    public int LAKE = 0x336699;
    public int RIVER = 0x225588;
    public int MARSH = 0x2f6666;
    public int ICE = 0x99ffff;
    public int BEACH = 0xa09077;
    public int ROAD1 = 0x442211;
    public int ROAD2 = 0x553322;
    public int ROAD3 = 0x664433;
    public int vBRIDGE = 0x686860;
    public int LAVA = 0xcc3333;
    // Terrain
    public int SNOW = 0xffffff;
    public int TUNDRA = 0xbbbbaa;
    public int BARE = 0x888888;
    public int SCORCHED = 0x555555;
    public int TAIGA = 0x99aa77;
    public int SHRUBLAND = 0x889977;
    public int TEMPERATE_DESERT = 0xc9d29b;
    public int TEMPERATE_RAIN_FOREST = 0x448855;
    public int TEMPERATE_DECIDUOUS_FOREST = 0x679459;
    public int GRASSLAND = 0x88aa55;
    public int SUBTROPICAL_DESERT = 0xd2b98b;
    public int TROPICAL_RAIN_FOREST = 0x337755;
    public int TROPICAL_SEASONAL_FOREST = 0x559944;
    public static HashMap<Integer, String> colorBiomeMap = new HashMap();
    public static HashMap<String, Integer> biomeColorMap = new HashMap();

    static {
        addColor("OCEAN", 0x44447a);
        addColor("COAST", 0x33335a);
        addColor("LAKESHORE", 0x225588);
        addColor("LAKE", 0x336699);
        addColor("RIVER", 0x225588);
        addColor("MARSH", 0x2f6666);
        addColor("MARSH", 0x2f6666);
        addColor("ICE", 0x99ffff);
        addColor("BEACH", 0xa09077);
        addColor("SNOW", 0xffffff);
        addColor("TUNDRA", 0xbbbbaa);
        addColor("BARE", 0x888888);
        addColor("SCORCHED", 0x555555);
        addColor("TAIGA", 0x99aa77);
        addColor("SHRUBLAND", 0x889977);
        addColor("TEMPERATE_DESERT", 0xc9d29b);
        addColor("TEMPERATE_RAIN_FOREST", 0x448855);
        addColor("TEMPERATE_DECIDUOUS_FOREST", 0x679459);
        addColor("GRASSLAND", 0x88aa55);
        addColor("SUBTROPICAL_DESERT", 0xd2b98b);
        addColor("TROPICAL_RAIN_FOREST", 0x337755);
        addColor("TROPICAL_SEASONAL_FOREST", 0x559944);
    }

    private static void addColor(String name, int color) {
        colorBiomeMap.put(color, name);
        biomeColorMap.put(name, color);
    }
}
