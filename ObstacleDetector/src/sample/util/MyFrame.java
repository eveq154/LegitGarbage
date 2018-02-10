package sample.util;

import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

public class MyFrame {

    private Mat frame;

    public MyFrame(Mat _frame){
        this.setFrame(_frame);
    }

    public Mat getFrame() {
        return frame;
    }

    public void setFrame(Mat frame) {
        this.frame = frame;
    }

    public MyFrame getROI(Rect roi){

        Mat mROI = this.getFrame().submat(roi);

        Mat blank = Mat.zeros(this.getFrame().size(), this.getFrame().type());

        mROI.copyTo(blank.submat(roi));

        return new MyFrame(blank);
    }

    public MyFrame getBinary(){
        Mat gray = new Mat();
        Mat dst = new Mat();

        Imgproc.cvtColor(this.getFrame(), gray, Imgproc.COLOR_BGR2GRAY);

        Imgproc.adaptiveThreshold(gray, dst, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 15, -3);

        return new MyFrame(dst);
    }



}
