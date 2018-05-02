package sample;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.LineSegmentDetector;
import org.opencv.videoio.VideoCapture;
import sample.util.Line;
import sample.util.MyFrame;
import sample.util.Utils;

import java.awt.geom.Line2D;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.lang.StrictMath.PI;
import static org.opencv.imgproc.Imgproc.*;

public class Controller {
    @FXML
    private Button button;
    @FXML
    private ImageView Frame;

    private ScheduledExecutorService timer;
    private VideoCapture capture;
    private boolean cameraActive;
    private MyFrame currFrame = new MyFrame();
    private boolean foundTransform = false;
    private Mat transformMat;
    private Mat pointsForTransform;

    public void initialize() {
        this.capture = new VideoCapture();
        this.cameraActive = false;
    }

    @FXML
    protected void start() {
        this.Frame.setPreserveRatio(true);
        this.Frame.setFitHeight(1080);
        this.Frame.setFitWidth(1920);

        if (!this.cameraActive) {
            this.capture.open("/home/eveq154/IdeaProjects/ObstacleDetector/src/sample/resources/20171206_115513778.avi");

            if (this.capture.isOpened()) {
                this.cameraActive = true;

                Runnable frameGrabber = () -> {

                    currFrame = grabFrame();

                    if (!foundTransform) {

                        List<Line> lines = currFrame.findLinesForTransform(new Rect(1000, 1500, 500, 200));

//                        currFrame.drawLines(Arrays.asList(lines.get(3), lines.get(1)));

                        pointsForTransform = currFrame.findPointsForTransform(lines.get(1), lines.get(3)); // 5, 6

                        transformMat = currFrame.findAndPerformTransform(
                                new Point(currFrame.getFrame().width() / 2, currFrame.getFrame().height() / 2),
                                pointsForTransform
                        );

                        lines = null;
                        foundTransform = true;
                    }

                    MyFrame otsu = new MyFrame(new Mat(currFrame.getFrame().size(), CvType.CV_8U));

                    Imgproc.warpPerspective(currFrame.getOtsu().getFrame(), otsu.getFrame(), transformMat, currFrame.getFrame().size());
                    Imgproc.warpPerspective(currFrame.getFrame(), currFrame.getFrame(), transformMat, currFrame.getFrame().size());






                    //find rails


                    double[] leftDot = new double[]{1100, 2000};
                    double[] rightDot = new double[]{1200, 2000};

                    List<Line> lines = otsu.findLines(new Rect(1000, 1500, 300, 500));
                    currFrame.drawLines(lines);

                    List<Line> lines1 = otsu.findLines(new Rect(1000, 1000, 300, 500));
                    currFrame.drawLines(lines1);

                    List<Line> lines2 = otsu.findLines(new Rect(1000, 500, 300, 500));
                    currFrame.drawLines(lines2);

                    List<Line> lines3 = otsu.findLines(new Rect(1000, 0, 300, 500));
                    currFrame.drawLines(lines3);




                    // dots
                    line(currFrame.getFrame(), new Point(leftDot[0], leftDot[1]), new Point(leftDot[0], leftDot[1]), new Scalar(0, 255, 0), 3);
                    line(currFrame.getFrame(), new Point(rightDot[0], rightDot[1]), new Point(rightDot[0], rightDot[1]), new Scalar(0, 255, 0), 3);
                    line(currFrame.getFrame(), new Point(1100, 1500), new Point(1100, 1500), new Scalar(0, 255, 0), 3);
                    line(currFrame.getFrame(), new Point(1200, 1500), new Point(1200, 1500), new Scalar(0, 255, 0), 3);






//                    rectangle(currFrame.getFrame(),
//                            new Point(1000, 1500),
//                            new Point(1300, 2000),
//                            new Scalar(0, 255, 0));
//                    rectangle(currFrame.getFrame(),
//                            new Point(1000, 1500),
//                            new Point(1600, 2000),
//                            new Scalar(0, 255, 0),
//                            5);

                    Image imageToShow = Utils.mat2Image(otsu
                            .getFrame()
                    );

                    updateImageView(Frame, imageToShow);
                };

                this.timer = Executors.newSingleThreadScheduledExecutor();
                this.timer.scheduleAtFixedRate(frameGrabber, 0, 2000, TimeUnit.MILLISECONDS);

                this.button.setText("Stop");
            } else {
                System.err.println("Impossible to open the camera connection...");
            }
        } else {
            this.cameraActive = false;
            this.button.setText("Start Camera");

            this.stopAcquisition();
        }
    }

    private MyFrame grabFrame() {
        Mat frame = new Mat();

        if (this.capture.isOpened()) {
            try {
                this.capture.read(frame);


            } catch (Exception e) {
                System.err.println("Exception during the frame elaboration: " + e);
            }
        }

        return new MyFrame(frame);
    }

    private void stopAcquisition() {
        if (this.timer != null && !this.timer.isShutdown()) {
            try {
                // stop the timer
                this.timer.shutdown();
                this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                // log any exception
                System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
            }
        }

        if (this.capture.isOpened()) {
            // release the camera
            this.capture.release();
        }
    }

    private void updateImageView(ImageView view, Image image) {
        Utils.onFXThread(view.imageProperty(), image);
    }

    protected void setClosed() {
        this.stopAcquisition();
    }


}
