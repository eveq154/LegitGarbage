#include "util.h"

#include "opencv2/imgcodecs.hpp"
#include "opencv2/highgui.hpp"
#include "opencv2/imgproc.hpp"
#include "opencv2/opencv.hpp"
#include "opencv2/videoio.hpp"
#include "cmath"

using namespace cv;
using namespace std;

int theDISTANCE;

Mat spookyScarySkeletons(Mat &src)
{
    Mat skel(src.size(), CV_8UC1, cv::Scalar(0));
    cv::Mat temp;
    cv::Mat eroded;

    Mat element = getStructuringElement(MORPH_CROSS, Size(3, 3));

    bool done;
    do
    {
      erode(src, eroded, element);
      dilate(eroded, temp, element); // temp = open(src)
      subtract(src, temp, temp);
      bitwise_or(skel, temp, skel);
      eroded.copyTo(src);

      done = (countNonZero(src) == 0);
    } while (!done);

    return skel;
}

Mat getROI(Mat &m, int x, int y, int width, int height, bool onBlank)
{
    Rect roi(x, y, width, height);
    Mat m_roi = m(roi);

    if (onBlank){
        Mat blank = Mat::zeros(m.size(), m.type());
        m_roi.copyTo(blank(roi));
        return blank;
    }
    else
        return m_roi;
}

void prepare(Mat src, Mat &dst)
{
    Mat src_blur;

    cvtColor( src, src_blur, CV_BGR2GRAY );

    adaptiveThreshold(~src_blur, dst, 255, CV_ADAPTIVE_THRESH_MEAN_C, THRESH_BINARY, 15, -3);
}

bool isEqual(const Vec4i& _l1, const Vec4i& _l2)
{
    Vec4i l1(_l1), l2(_l2);

        float length1 = sqrtf((l1[2] - l1[0])*(l1[2] - l1[0]) + (l1[3] - l1[1])*(l1[3] - l1[1]));
        float length2 = sqrtf((l2[2] - l2[0])*(l2[2] - l2[0]) + (l2[3] - l2[1])*(l2[3] - l2[1]));

        float product = (l1[2] - l1[0])*(l2[2] - l2[0]) + (l1[3] - l1[1])*(l2[3] - l2[1]);

        if (fabs(product / (length1 * length2)) < cos(CV_PI / 30))
            return false;

        float mx1 = (l1[0] + l1[2]) * 0.5f;
        float mx2 = (l2[0] + l2[2]) * 0.5f;

        float my1 = (l1[1] + l1[3]) * 0.5f;
        float my2 = (l2[1] + l2[3]) * 0.5f;
        float dist = sqrtf((mx1 - mx2)*(mx1 - mx2) + (my1 - my2)*(my1 - my2));

        if (dist > std::max(length1, length2) * 0.5f)
            return false;

        return true;
}

float calcSlope( Vec4i lines)
{
    float x1 = lines[0], y1= lines[1], x2 = lines[2], y2= lines[3];
    float slope = (y2-y1)/(x2-x1);
    return tan(slope) * 180 / CV_PI;
}

std::array<int, 3> cross(const std::array<int, 3> &a, const std::array<int, 3> &b)
{
    std::array<int, 3> result;
    result[0] = a[1] * b[2] - a[2] * b[1];
    result[1] = a[2] * b[0] - a[0] * b[2];
    result[2] = a[0] * b[1] - a[1] * b[0];
    return result;
}

bool get_intersection(const cv::Vec4i &line_a, const cv::Vec4i &line_b, vector<cv::Point2f> &intersection)
{
    cv::Point intersec;
    std::array<int, 3> pa{ { line_a[0], line_a[1], 1 } };
    std::array<int, 3> pb{ { line_a[2], line_a[3], 1 } };
    std::array<int, 3> la = cross(pa, pb);
    pa[0] = line_b[0], pa[1] = line_b[1], pa[2] = 1;
    pb[0] = line_b[2], pb[1] = line_b[3], pb[2] = 1;
    std::array<int, 3> lb = cross(pa, pb);
    std::array<int, 3> inter = cross(la, lb);
    if (inter[2] == 0){
        intersec.x = 1;
        intersec.y = 1;
        intersection.push_back(intersec);
        return false;
    }
    else {
        intersec.x = inter[0] / inter[2];
        intersec.y = inter[1] / inter[2];
        intersection.push_back(intersec);
        return true;
    }
}

vector<Point2f> transformers(Mat &input, Mat &output, vector<Point2f> objPts)
{
    Mat transMat;

    Point2f centerOfFrame(input.size().width / 2, input.size().height / 2);

    vector<Point2f> tempPoints;

    tempPoints.push_back(Point2f(centerOfFrame.x + 10, centerOfFrame.y + 50));
    tempPoints.push_back(Point2f(centerOfFrame.x + 10, centerOfFrame.y + 360));
    tempPoints.push_back(Point2f(centerOfFrame.x - 10, centerOfFrame.y + 50));
    tempPoints.push_back(Point2f(centerOfFrame.x - 10, centerOfFrame.y + 360));

    transMat = getPerspectiveTransform(objPts, tempPoints);

    warpPerspective( input, output, transMat, input.size() );

    theDISTANCE = tempPoints[0].x - tempPoints[2].x;

    for (int i = 0; i < tempPoints.size(); i++)
    {
        circle(input, tempPoints[i], 1, Scalar(255, 0, 0));
    }

    return tempPoints;
}

bool oneTime(const Vec4i& _l1, const Vec4i& _l2)
{
    Vec4i l1(_l1), l2(_l2);

    int upperDistance = l1[0] - l2[0];
    int lowerDistance = l1[2] - l2[2];

    if ( abs(upperDistance - theDISTANCE) < 1 && abs(lowerDistance - theDISTANCE) < 1 && abs(upperDistance - lowerDistance) < 1 )
        return true;

    return false;
}

vector<Vec4i> findSomeRealShit(vector<Vec4i> &lines){
    vector<Vec4i> result;

    for (int i = 0; i < lines.size(); i++){
        for (int j = 0; j < lines.size(); j++){
            if ( oneTime(lines[i], lines[j]) ){
                result.push_back(lines[i]);
                result.push_back(lines[j]);
                i++;
            }
        }
    }

    return result;
}
/*
void findROIs(Mat &src, int initX, int initY)
{
    Mat currentROI = getROI(src, initX, initY, )
}
*/
util::util()
{

}
