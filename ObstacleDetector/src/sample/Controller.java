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
    private MyFrame prevFrame = new MyFrame();
    private MyFrame currFrame = new MyFrame();
    private boolean foundTransform = false;
    private Mat transformMat;

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
            this.capture.open("/home/eveq154/IdeaProjects/ObstacleDetector/src/sample/resources/2018-02-07_15_51.avi");

            if (this.capture.isOpened()) {
                this.cameraActive = true;

                Runnable frameGrabber = () -> {

                    currFrame = grabFrame();

//                    if (!Objects.equals(prevFrame.getFrame(), null))
//                        currFrame.Stabilize(prevFrame);
//
                    if (!foundTransform) {

                        List<Line> lines = currFrame.findLinesForTransform(new Rect(1000, 1500, 500, 500));

                        Mat pointsForTransform = currFrame.findPointsForTransform(lines.get(5), lines.get(6));

                        transformMat = currFrame.findAndPerformTransform(
                                new Point(currFrame.getFrame().width() / 2, currFrame.getFrame().height() / 2),
                                pointsForTransform
                        );

                        lines = null;
                        foundTransform = true;
                    }

                    MyFrame otsu = new MyFrame(new Mat(currFrame.getFrame().size(), CvType.CV_8U));

                    Imgproc.warpPerspective(currFrame.getOtsu().getFrame(), otsu.getFrame(), transformMat, currFrame.getFrame().size());


                    //find rails
//
                    List<Line> lines = currFrame.findLines(new Rect(1000, 1500, 300, 500));

                    System.out.println("lines - " + lines.size());

                    List<Line> pair = new ArrayList<>();

                    List<Integer> dists = new ArrayList<>();

                    for (int i = 0; i < lines.size(); i++) {

                        if (pair.size() == 2)
                            break;

                        for (int j = 0; j < lines.size(); j++) {

                            Line l1 = lines.get(i);
                            Line l2 = lines.get(j);

                            dists.add(l1.calcDistanceTo(l2));

                            if (l1.calcSlope() == l2.calcSlope()){
                                pair.add(l1);
                                pair.add(l2);
                                break;
                            }
                        }
                    }

                    currFrame.drawLines(lines);

                    dists.stream()
                            .filter(l -> l != -1 && l != 0)
                            .forEach(System.out::println);

                    //



//                    LineSegmentDetector detector = createLineSegmentDetector();
//                    Mat lines = new Mat();
//                    Mat blank = new Mat(currFrame.getFrame().size(), CvType.CV_8U);
//                    detector.detect(currFrame.getOtsu().getROI(new Rect(1000, 1500, 500, 500)).getFrame(), lines);
//
//                    detector.drawSegments(currFrame.getFrame(), lines);



//                    line(currFrame.getFrame(), new Point(1000, 1500), new Point(1500, 1500), new Scalar(255, 0, 0));
                    rectangle(currFrame.getFrame(), new Point(1000, 1500), new Point(1300, 2000), new Scalar(0, 255, 0));
                    Image imageToShow = Utils.mat2Image(otsu
                            //.getROI(new Rect(1000, 1500, 500, 500), true)
                            //.getOtsu()
                            .getFrame()
                    );

                    updateImageView(Frame, imageToShow);

                    prevFrame.setFrame(currFrame.getFrame().clone());
                };

                this.timer = Executors.newSingleThreadScheduledExecutor();
                this.timer.scheduleAtFixedRate(frameGrabber, 0, 1000, TimeUnit.MILLISECONDS);

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
