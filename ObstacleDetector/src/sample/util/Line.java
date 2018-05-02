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

        return Math.abs(Math.atan2(y2 - y1, x2 - x1) * 180 / Math.PI);
    }

    public boolean contains(double X, double Y){
        // (x2 - x1)(y - y1) == (y2 - y1)(x - x1)
        double x1 = this.getDots()[0];
        double y1 = this.getDots()[1];
        double x2 = this.getDots()[2];
        double y2 = this.getDots()[3];

        //double val = Math.abs((x2 - x1) * (Y - y1) - (y2 - y1) * (X - x1));
        return (x2 - x1) * (Y - y1) == (y2 - y1) * (X - x1);
    }

    @Override
    public String  toString() {
        return "Line{" +
                "dots=(" + Arrays.toString(this.getDots()) +
                ", slope=" + slope +
                '}';
    }
}
