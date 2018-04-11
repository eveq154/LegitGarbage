package sample.util;

import java.util.Arrays;

public class Line {

    private double[] dots;
    private double slope;

    public Line() {
    }

    public Line(double[] line) {
        this.setDots(line);
        this.setSlope(calcSlope());
    }

    public double getSlope() {
        return slope;
    }

    private void setSlope(double slope) {
        this.slope = slope;
    }

    public double[] getDots() {
        return dots;
    }

    private void setDots(double[] dots) {
        this.dots = dots;
    }

    public double calcSlope() {
        double x1 = this.getDots()[0]
             , y1 = this.getDots()[1]
             , x2 = this.getDots()[2]
             , y2 = this.getDots()[3];

        return Math.atan2(y2 - y1, x2 - x1) * 180 / Math.PI;
    }

    public int calcDistanceTo(Line l) {
        double distance = 0.0;
        try {
            double xDiff = Math.pow(this.getDots()[0], 2) - Math.pow(l.getDots()[0], 2);
            double yDiff = Math.pow(this.getDots()[2], 2) - Math.pow(l.getDots()[2], 2);

            if (xDiff == yDiff)
                distance = Math.sqrt(xDiff + yDiff);
            else
                distance = -1;

        } catch (Exception e) {
            System.err.println("Something went wrong in euclideanDistance function");
        }
        return (int)distance;
    }
}
