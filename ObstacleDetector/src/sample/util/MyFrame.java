package sample.util;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.photo.Photo;
import org.opencv.video.BackgroundSubtractor;
import org.opencv.video.BackgroundSubtractorKNN;
import org.opencv.video.BackgroundSubtractorMOG2;
import org.opencv.video.Video;

import java.util.ArrayList;
import java.util.List;

import static java.lang.StrictMath.PI;
import static org.opencv.imgproc.Imgproc.*;

public class MyFrame {

    private Mat frame;

    private static final int theDistance = 119;

    public MyFrame() {
    }

    public MyFrame(Mat _frame) {
        this.setFrame(_frame);
    }

    public Mat getFrame() {
        return frame;
    }

    public void setFrame(Mat frame) {
        this.frame = frame;
    }

    public static double getTheDistance() {
        return theDistance;
    }

    public MyFrame getROI(Rect roi, boolean raw) {

        Mat mROI = this.getFrame().submat(roi);

        if (!raw) {

            Mat blank = Mat.zeros(this.getFrame().size(), this.getFrame().type());

            mROI.copyTo(blank.submat(roi));

            return new MyFrame(blank);
        }
        else
            return new MyFrame(mROI);
    }

    public MyFrame getAT() {
        Mat gray = new Mat();
        Mat dst = new Mat();

        cvtColor(this.getFrame(), gray, Imgproc.COLOR_BGR2GRAY);

        blur(gray, gray, new Size(9, 9));

        Imgproc.adaptiveThreshold(gray, dst, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 15, -3);

        Imgproc.dilate(dst, dst, new Mat(), new Point(-1, -1), 11);
        Imgproc.erode(dst, dst, new Mat(), new Point(-1, -1), 19);

        return new MyFrame(dst);
    }

    public MyFrame getOtsu() {
        Mat gray = new Mat();
        Mat dst = new Mat();

        cvtColor(this.getFrame(), gray, Imgproc.COLOR_BGR2GRAY);

        blur(gray, gray, new Size(4, 4));

        Imgproc.threshold(gray, dst, 0, 255, THRESH_OTSU | THRESH_BINARY_INV);

        Imgproc.dilate(dst, dst, new Mat(), new Point(-1, -1), 4);
        Imgproc.erode(dst, dst, new Mat(), new Point(-1, -1), 4);

        return new MyFrame(dst);
    }

    public MyFrame getCanny() {
        Mat grayImage = new Mat();
        Mat detectedEdges = new Mat();
        int threshhold = 30;

        Imgproc.cvtColor(this.getFrame(), grayImage, Imgproc.COLOR_BGR2GRAY);

        Imgproc.blur(grayImage, detectedEdges, new Size(5, 5));

        Imgproc.Canny(detectedEdges, detectedEdges, threshhold, threshhold * 3);

        Imgproc.dilate(detectedEdges, detectedEdges, new Mat(), new Point(-1, -1), 1);
        Imgproc.erode(detectedEdges, detectedEdges, new Mat(), new Point(-1, -1), 1);

        Mat dest = new Mat();
        this.getFrame().copyTo(dest, detectedEdges);

        return new MyFrame(detectedEdges);
    }

    public void Stabilize(MyFrame prevFrame) {
        MatOfPoint oldFeatures = new MatOfPoint();

        Mat prev = new Mat(prevFrame.getFrame().size(), prevFrame.getFrame().type());

        cvtColor(prevFrame.getFrame(), prev, COLOR_BGR2GRAY);

        Imgproc.goodFeaturesToTrack(prev, oldFeatures, 100, 0.01, 0.1);

        Mat greyNew = new Mat(), greyOld = new Mat();

        cvtColor(this.getFrame(), greyNew, Imgproc.COLOR_BGR2GRAY);
        cvtColor(prevFrame.getFrame(), greyOld, Imgproc.COLOR_BGR2GRAY);

        MatOfPoint2f currFeatures = new MatOfPoint2f();
        MatOfPoint2f prevFeatures = new MatOfPoint2f(oldFeatures.toArray());

        MatOfByte status = new MatOfByte();
        MatOfFloat err = new MatOfFloat();

        Video.calcOpticalFlowPyrLK(greyOld, greyNew, prevFeatures, currFeatures, status, err);

        Mat correctionMat = Video.estimateRigidTransform(currFeatures, prevFeatures, false);

        try {
            Mat corrected = new Mat();

            Imgproc.warpAffine(this.getFrame(), corrected, correctionMat, this.getFrame().size());
        } catch (Exception ignored) {
            System.err.println("сука проклятая");
        }

    }

    public void drawLines(List<Line> list) {
        for (Line l : list) {
            line(this.getFrame(),
                    new Point(l.getDots()[0], l.getDots()[1]),
                    new Point(l.getDots()[2], l.getDots()[3]),
                    new Scalar(255, 0, 0),
                    3);
        }
    }

    public List<Line> findLinesForTransform(Rect roi) {
        List<Line> lines = new ArrayList<>();
        Mat linesP = new Mat();

        HoughLinesP(this.
                getOtsu().
                getROI(roi, false).
                getFrame(), linesP, 1, 2 * PI /180, 50, 400, 0);

        for (int i = 0; i < linesP.cols(); i++) {
            for (int j = 0; j < linesP.rows(); j++) {
                double[] line = linesP.get(j, i);
                lines.add(new Line(line));
            }
        }

        return lines;
    }

    public List<Line> findLines(Rect roi) {
        List<Line> lines = new ArrayList<>();
        Mat linesP = new Mat();

        Imgproc.HoughLinesP(this.
                getAT().
                getROI(roi, false).
                getFrame(), linesP, 1, PI / 180, 20, 100, 10);


        for (int i = 0; i < linesP.cols(); i++) {
            for (int j = 0; j < linesP.rows(); j++) {
                double[] line = linesP.get(j, i);
                lines.add(new Line(line));
            }
        }

        return lines;
    }

    public Mat findAndPerformTransform(Point center, Mat srcPoints) {
        MyFrame out = new MyFrame();

        float data[] = new float[]{
                1200, 1100,
                1200, 2000,
                1100, 1100,
                1100, 2000
        };

        Mat dstPoints = new Mat(4, 1, CvType.CV_32FC2);
        dstPoints.put(0, 0, data);

        return Imgproc.getPerspectiveTransform(srcPoints, dstPoints);
    }

    private static float[] findIntersection(Line l1, Line l2) {


        double A1 = l1.getDots()[3] - l1.getDots()[1];
        double B1 = l1.getDots()[0] - l1.getDots()[2];
        double C1 = (l1.getDots()[0] * A1) + (l1.getDots()[1] * B1);

        double A2 = l2.getDots()[3] - l2.getDots()[1];
        double B2 = l2.getDots()[0] - l2.getDots()[2];
        double C2 = (l2.getDots()[0] * A2) + (l2.getDots()[1] * B2);

        double det = (A1 * B2) - (A2 * B1);

        return new float[]{(float) (((C1 * B2) - (C2 * B1)) / (det)), (float) (((C2 * A1) - (C1 * A2)) / (det))};
    }

    public Mat findPointsForTransform(Line l1, Line l2) {
        Line upper = new Line(new double[]{0, 1500, this.getFrame().width(), 1500});
        Line lower = new Line(new double[]{0, 2000, this.getFrame().width(), 2000});

        float[] data = new float[8];


        data[0] = findIntersection(l2, upper)[0];
        data[1] = findIntersection(l2, upper)[1];

        data[2] = findIntersection(l2, lower)[0];
        data[3] = findIntersection(l2, lower)[1];

        data[4] = findIntersection(l1, upper)[0];
        data[5] = findIntersection(l1, upper)[1];

        data[6] = findIntersection(l1, lower)[0];
        data[7] = findIntersection(l1, lower)[1];


        Mat legitPoints = new Mat(4, 1, CvType.CV_32FC2);
        legitPoints.put(0, 0, data);

        return legitPoints;
    }

    public MyFrame bgRemoval(){
        Mat res = new Mat();

        Photo.fastNlMeansDenoising(this.getFrame(), res, 3, 7, 21);

        return new MyFrame(res);
    }

    public MyFrame close(){
        Mat res = new Mat();

        Imgproc.dilate(this.getFrame(), res, new Mat(), new Point(-1, -1), 3);
        Imgproc.erode(this.getFrame(), res, new Mat(), new Point(-1, -1), 3);

        return new MyFrame(res);
    }
}
