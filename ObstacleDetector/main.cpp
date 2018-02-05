#include "util.cpp"
#include <iostream>

using namespace cv;
using namespace std;

#define PI 3.1415926

Mat src, src_warped, src_canny, src_blur, dst, blank, M;

int angle = 34, angle1 = 47;
int initX = 530, initY = 450;
int ROIX = 605, ROIY = 660;

int thresh = 21;

vector<Vec4i> linesP, rails, leftLegitRails, rightLegitRails, legitRails;
vector<Vec2f> lines;
vector<int> labels;
vector<Point2f> inter, huinter;
bool foundInter = false;

int main() {
    VideoCapture capture("6.mp4");

    namedWindow("Result", 1);
    moveWindow("Result", 20, 20);

    createTrackbar( "Thresh:", "Result", &thresh, 100);

    while( true ) {

        capture >> src;

        if (!foundInter)
        {

            Mat blank = getROI(src, initX, initY, 120, 100, true);

            prepare(blank, src_canny);

            HoughLinesP(src_canny, linesP, 1, 2 * CV_PI/angle, 50, 100, 10);


            for( size_t i = 0; i < linesP.size(); i++ )
            {
                Vec4i l1 = linesP[i];
                float slope =calcSlope(l1);
                if(abs(abs(slope) - angle) <= angle1 && abs(abs(slope) - angle) > angle && abs(abs(slope) - angle) != 90)
                    rails.push_back(l1);
            }

            partition(rails, labels, isEqual);

            bool flag1 = true;
            bool flag2 = true;

            for( size_t i = 0; i < rails.size(); i++ )
            {
                Vec4i l1 = rails[i];
                if (legitRails.size() < 2) {
                    if (flag1){
                        if (labels[i] == 0){
                            //line( blank, Point(l1[0], l1[1]), Point(l1[2], l1[3]), Scalar(255,0,0), 1, CV_AA);
                            leftLegitRails.push_back(l1);
                            legitRails.push_back(l1);
                            flag1 = false;
                        }
                    }

                    if (flag2){
                        if (labels[i] == 1){
                            //line( blank, Point(l1[0], l1[1]), Point(l1[2], l1[3]), Scalar(0,0,255), 1, CV_AA);
                            rightLegitRails.push_back(l1);
                            legitRails.push_back(l1);
                            flag2 = false;
                        }
                    }
                }
            }

            Vec4i afookinline1;
            afookinline1[0] = initX;
            afookinline1[1] = initY;
            afookinline1[2] = initX + 150;
            afookinline1[3] = initY;

            Vec4i afookinline2;
            afookinline2[0] = initX;
            afookinline2[1] = initY+300;
            afookinline2[2] = initX + 150;
            afookinline2[3] = initY+300;

            if (legitRails[0][0] < legitRails[1][0]){
                Vec4i temp = legitRails[0];
                legitRails[0] = legitRails[1];
                legitRails[1] = temp;
            }

            get_intersection(legitRails[0], afookinline1, inter);
            line( blank, inter[0], inter[0], Scalar(0,255,0), 2, CV_AA);

            get_intersection(legitRails[0], afookinline2, inter);
            line( blank, inter[1], inter[1], Scalar(0,255,0), 2, CV_AA);

            get_intersection(legitRails[1], afookinline1, inter);
            line( blank, inter[2], inter[2], Scalar(0,255,0), 2, CV_AA);

            get_intersection(legitRails[1], afookinline2, inter);
            line( blank, inter[3], inter[3], Scalar(0,255,0), 2, CV_AA);

            foundInter = true;
        }

        huinter = transformers(src, src_warped, inter);

        prepare(src_warped, src_canny);

        Mat blank = getROI(src_canny, ROIX, ROIY, 70, 50, true);

        Mat skel = spookyScarySkeletons(blank);

        HoughLinesP(skel, linesP, 1, CV_PI/180, thresh, 20, 300);

        vector<Vec4i> realShit = findSomeRealShit(linesP);

        for (int i = 0; i < realShit.size(); i++)
        {
            Vec4i l = realShit[i];
            line(src_warped, Point(l[0], l[1]), Point(l[2], l[3]), Scalar(255, 0, 0), 3);
        }

        rectangle(src_warped, Rect(ROIX, ROIY, 70, 50), Scalar(0, 255, 0), 1);


        imshow("Result", src_warped);

        int k = waitKey(1);
        if (k == 27)
            break;
    }

    capture.release();
    destroyAllWindows();

    return 0;
}

