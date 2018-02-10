package sample;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import sample.util.MyFrame;
import sample.util.Utils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class Controller
{
    @FXML
    private Button button;
    @FXML
    private CheckBox grayscale;
    @FXML
    private ImageView currentFrame;

    private ScheduledExecutorService timer;
    private VideoCapture capture;
    private boolean cameraActive;

    public void initialize()
    {
        this.capture = new VideoCapture();
        this.cameraActive = false;
    }

    @FXML
    protected void startCamera()
    {
        this.currentFrame.setFitWidth(600);
        this.currentFrame.setPreserveRatio(true);

        if (!this.cameraActive)
        {
            this.capture.open("/home/eveq154/Downloads/6.mp4");

            if (this.capture.isOpened())
            {
                this.cameraActive = true;

                Runnable frameGrabber = () -> {
                    MyFrame originalFrame = grabFrame();

                    int centerX = originalFrame.getFrame().width()/2, centerY = originalFrame.getFrame().height()/2;

                    Rect roiForTransform = new Rect(centerX - 100, centerY + 50, 100, 100);

                    Mat linesP = new Mat();

                    Imgproc.HoughLinesP(originalFrame.
                            getBinary().
                            getROI(roiForTransform).
                            getFrame(), linesP, 1.0, Math.PI/90, 50);

                    for(int i = 0; i < linesP.cols(); i++) {
                        double[] val = linesP.get(0, i);
                        Imgproc.line(originalFrame.getFrame(), new Point(val[0], val[1]), new Point(val[2], val[3]), new Scalar(255, 0, 0), 3);
                    }


                    Image imageToShow = Utils.mat2Image(originalFrame.getFrame());
                    updateImageView(currentFrame, imageToShow);
                };

                this.timer = Executors.newSingleThreadScheduledExecutor();
                this.timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);

                this.button.setText("Stop Camera");
            }
            else
            {
                System.err.println("Impossible to open the camera connection...");
            }
        }
        else
        {
            this.cameraActive = false;
            this.button.setText("Start Camera");

            this.stopAcquisition();
        }
    }

    private MyFrame grabFrame()
    {
        Mat frame = new Mat();

        if (this.capture.isOpened())
        {
            try
            {
                this.capture.read(frame);


            }
            catch (Exception e)
            {
                System.err.println("Exception during the frame elaboration: " + e);
            }
        }

        return new MyFrame(frame);
    }


    private void stopAcquisition()
    {
        if (this.timer != null && !this.timer.isShutdown())
        {
            try
            {
                // stop the timer
                this.timer.shutdown();
                this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);
            }
            catch (InterruptedException e)
            {
                // log any exception
                System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
            }
        }

        if (this.capture.isOpened())
        {
            // release the camera
            this.capture.release();
        }
    }

    private void updateImageView(ImageView view, Image image)
    {
        Utils.onFXThread(view.imageProperty(), image);
    }

    protected void setClosed()
    {
        this.stopAcquisition();
    }



}
