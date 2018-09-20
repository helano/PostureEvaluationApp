package com.example.helano.opencvrectangledetection;

import android.content.Intent;
import android.database.Cursor;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.opencv.core.Core.*;

public class MainActivity extends AppCompatActivity {


    Bitmap tempBitmap, currentBitmap, originalBitmap;
    Mat originalMat;
    static Scalar min = new Scalar(50, 50, 50, 0);//BGR-A
    static Scalar max= new Scalar(255, 255, 255, 0);//BGR-A


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);




            }

    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();

        inflater.inflate(R.menu.filename, menu);

        return true;

    }

    public boolean onOptionsItemSelected(MenuItem item) {
// Handle action bar item clicks here. The action bar will
// automatically handle clicks on the Home/Up
        // button, so long
// as you specify a parent activity in
        //AndroidManifest.xml.

//noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }


        switch (item.getItemId()) {
            case R.id.OpenGallery:

                Intent intent = new Intent(Intent.ACTION_PICK,
                        Uri.parse("content://media/internal/images/media"));
                startActivityForResult(intent, 0);

                return true;
            case R.id.HoughCircles:
                houghCircles();
                return true;

            case R.id.DoG:
                DifferenceOfGaussian();
                return true;
            case R.id.CannyEdges:
                Canny();
                return true;
            case R.id.White_Area:
                getWhiteArea();
                return true;

            default:

                return super.onOptionsItemSelected(item);
        }
    }
    private void loadImageToImageView()
    {
        ImageView imgView = (ImageView) findViewById(R.id.image_view);
        imgView.setImageBitmap(currentBitmap);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0 && resultCode == RESULT_OK &&
                null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            Cursor cursor = getContentResolver().query(selectedImage,
                            filePathColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();
// String picturePath contains the path
           // of selected Image
//To speed up loading of image
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 1;
            Bitmap temp = BitmapFactory.decodeFile(picturePath, options);
//Get orientation information
            int orientation = 0;
            try {
                ExifInterface imgParams = new ExifInterface(picturePath);
                orientation = imgParams.getAttributeInt(
                                ExifInterface.TAG_ORIENTATION,
                                ExifInterface.ORIENTATION_UNDEFINED);
            } catch (IOException e) {
                e.printStackTrace();
            }
//Rotating the image to get the correct orientation
            Matrix rotate90 = new Matrix();
          rotate90.postRotate(orientation);
            originalBitmap = rotateBitmap(temp,orientation);
            originalBitmap = temp;
//Convert Bitmap to Mat
            Bitmap tempBitmap = originalBitmap;
             tempBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888,true);
            originalMat = new Mat(tempBitmap.getHeight(), tempBitmap.getWidth(), CvType.CV_8U);
            Utils.bitmapToMat(tempBitmap, originalMat);
            currentBitmap =
                    originalBitmap.copy(Bitmap.Config.ARGB_8888,false);
            loadImageToImageView();
        }
    }

    public static Bitmap rotateBitmap(Bitmap source, float angle)
    {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }


    private BaseLoaderCallback mOpenCVCallBack = new
            BaseLoaderCallback(this) {
                @Override
                public void onManagerConnected(int status) {
                    switch (status) {
                        case LoaderCallbackInterface.SUCCESS:
//DO YOUR WORK/STUFF HERE
                            break;
                        default:
                            super.onManagerConnected(status);
                            break;
                    }
                }
            };
    @Override
    protected void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0,
                this,
                mOpenCVCallBack);
    }

    void houghCircles()
    {
        Mat grayMat = new Mat();
        Mat cannyEdges = new Mat();
        Mat circles = new Mat();
        Mat white = new Mat();
        Mat blur = new Mat();
//Converting the image to grayscale


        Imgproc.cvtColor(blur,grayMat,Imgproc.COLOR_BGR2GRAY);
        Imgproc.threshold(grayMat,white, 200, 255, Imgproc.THRESH_BINARY);
      Imgproc.Canny(white, cannyEdges,10, 100);
        Imgproc.blur(white, blur,new Size(3,3) );
        Imgproc.HoughCircles(blur, circles, Imgproc.CV_HOUGH_GRADIENT,1,1, 100.0, 30.0, 0, 40);
        Mat houghCircles = new Mat();
        houghCircles.create(cannyEdges.rows(),cannyEdges.cols(),CvType.CV_8UC1);
//Drawing lines on the image
        for(int i = 0 ; i < circles.cols() ; i++)
        {
            double[] parameters = circles.get(0,i);
            double x, y;
            int r;
            x = parameters[0];
            y = parameters[1];
            r = (int)parameters[2];
            Point center = new Point(x, y);

            System.out.println("X:"+x +"y:"+ y);
//Drawing circles on an image
         //Core.circle(houghCircles,center,r, new Scalar(255,0,0),1);
            Imgproc.circle(originalMat, center, 1, new Scalar(0,0,0), 3, 8, 0 );
//
            Imgproc.circle(originalMat,center,r, new Scalar(0,0,255),1);

        }
//Converting Mat back to Bitmap
        Utils.matToBitmap(originalMat, currentBitmap);
        loadImageToImageView();
    }





    public void  DifferenceOfGaussian()
    {


       Mat  grayMat = new Mat();
        Mat blur1 = new Mat();
        Mat blur2 = new Mat();
//Converting the image to grayscale
        Imgproc.cvtColor(originalMat
                ,grayMat,Imgproc.COLOR_BGR2GRAY);
//Bluring the images using two different blurring radius
        Imgproc.GaussianBlur(grayMat,blur1,new Size(15,15),5);
        Imgproc.GaussianBlur(grayMat,blur2,new Size(21,21),5);
//Subtracting the two blurred images
        Mat DoG = new Mat();
        Core.absdiff(blur1, blur2,DoG);
//Inverse Binary Thresholding
        Core.multiply(DoG,new Scalar(100), DoG);
        Imgproc.threshold(DoG,DoG,50,255
                ,Imgproc.THRESH_BINARY_INV);
//Converting Mat back to Bitmap
        Utils.matToBitmap(DoG, currentBitmap);
        loadImageToImageView();
    }

    public void Canny()
    {
        Mat grayMat = new Mat();
        Mat cannyEdges = new Mat();
//Converting the image to grayscale
        Imgproc.cvtColor(originalMat,grayMat,Imgproc.COLOR_BGR2GRAY);
        Imgproc.Canny(grayMat, cannyEdges,10, 100);
//Converting Mat back to Bitmap
        Utils.matToBitmap(cannyEdges, currentBitmap);
        loadImageToImageView();
    }




    void getWhiteArea(){
        Mat whiteArea = new Mat();
        Mat gray = new Mat();

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        List<Double> areasContours = new ArrayList<Double>();
       // Core.inRange(originalMat, min, max, whiteArea);

        Imgproc.cvtColor(originalMat,gray,Imgproc.COLOR_BGR2GRAY);

        Imgproc.threshold(gray, whiteArea, 200, 255, Imgproc.THRESH_BINARY);

        Imgproc.findContours(whiteArea, contours, new Mat(),     Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++) {
            Imgproc.drawContours(originalMat, contours, contourIdx, new Scalar(0, 0, 255), 1);
        }
        List<Moments> mu = new ArrayList<Moments>(contours.size());
        for (int i = 0; i < contours.size(); i++) {
            mu.add(i, Imgproc.moments(contours.get(i), false));
            Moments p = mu.get(i);
            int x = (int) (p.get_m10() / p.get_m00());
            int y = (int) (p.get_m01() / p.get_m00());
            Imgproc.circle(originalMat, new Point(x, y), 1, new Scalar(255,49,0,255));

            Mat aux = contours.get(i);



            System.out.print( "Area contorno:"+i+"= "+ Imgproc.contourArea(aux));

        }







        Utils.matToBitmap(originalMat, currentBitmap);
        loadImageToImageView();
    }
}
