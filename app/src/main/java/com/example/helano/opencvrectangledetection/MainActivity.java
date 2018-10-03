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
import android.widget.TextView;
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

    TipoAvaliacao tipoAvaliacao;
    Bitmap tempBitmap, currentBitmap, originalBitmap;
    Mat originalMat;
    static Scalar min = new Scalar(50, 50, 50, 0);//BGR-A
    static Scalar max= new Scalar(255, 255, 255, 0);//BGR-A
    //static TextView areaTextView ;

    TextView[] textViews = new TextView[3];


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
       // areaTextView = (TextView)findViewById(R.id.contour_area);


            textViews[0] = (TextView)findViewById(R.id.point0);
            textViews[1] = (TextView)findViewById(R.id.point1);
            textViews[2] = (TextView)findViewById(R.id.point2);






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
           /* case R.id.HoughCircles:
                houghCircles();
                return true;

            case R.id.DoG:
                DifferenceOfGaussian();
                return true;
            case R.id.CannyEdges:
                Canny();
                return true;*/
            case R.id.Frontal_Analysis:

                measure( tipoAvaliacao.ANTERIOR);
                return true;
            case R.id.Posterior_Analysis:
                measure( tipoAvaliacao.POSTERIOR);
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











    void measure(TipoAvaliacao tipoAvaliacao){
        Mat whiteArea = new Mat();
        Mat gray = new Mat();

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        List<Double> areasContours = new ArrayList<Double>();
        List<Point> points = new ArrayList<Point>();
        Double biggerContour = new Double(0);
       // Core.inRange(originalMat, min, max, whiteArea);

        Imgproc.cvtColor(originalMat,gray,Imgproc.COLOR_BGR2GRAY);

        Imgproc.threshold(gray, whiteArea, 200, 255, Imgproc.THRESH_BINARY);

        Imgproc.findContours(whiteArea, contours, new Mat(),     Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++) {


            Mat aux = contours.get(contourIdx);
            areasContours.add( Imgproc.contourArea(aux));
            if (contourIdx==0){
                biggerContour = areasContours.get(contourIdx);
            }else {

                if (biggerContour <= areasContours.get(contourIdx)){
                    biggerContour = areasContours.get(contourIdx);
                }
            }

            System.out.print( "Area contorno:"+contourIdx+"= "+ areasContours.get(contourIdx));
            //areaTextView.setText(Double.toString(biggerContour));
            if (areasContours.get(contourIdx)<200) {
                Imgproc.drawContours(originalMat, contours, contourIdx, new Scalar(0, 0, 255), 1);

            }

        }
        List<Moments> mu = new ArrayList<Moments>(contours.size());
        for (int i = 0; i < contours.size(); i++) {
            mu.add(i, Imgproc.moments(contours.get(i), false));
            Moments p = mu.get(i);
            int x = (int) (p.get_m10() / p.get_m00());
            int y = (int) (p.get_m01() / p.get_m00());
            points.add(new Point(x,y));

            if (areasContours.get(i)<200) {
                Imgproc.circle(originalMat, new Point(x, y), 1, new Scalar(255,49,0,255));
                Imgproc.putText(originalMat, Integer.toString(i), new Point(x, y), 1, 1, new Scalar(255,0,0));
            }



        }




    switch (tipoAvaliacao) {


        case ANTERIOR:
            drawLines(points, tipoAvaliacao.ANTERIOR);
            calcularAngulos(points, tipoAvaliacao.ANTERIOR);
        break;
        case POSTERIOR:
            drawLines(points, tipoAvaliacao.POSTERIOR);
            calcularAngulos(points, tipoAvaliacao.POSTERIOR);
        break;

        //setPoints(points);
    }
        Utils.matToBitmap(originalMat, currentBitmap);
        loadImageToImageView();
    }


    void drawLines(List<Point> listOfPoints, TipoAvaliacao tipoAvaliacao){

      switch (tipoAvaliacao) {

          case ANTERIOR:

             Imgproc.line(originalMat, listOfPoints.get(2), listOfPoints.get(0), new Scalar(0, 255, 0));
             Imgproc.line(originalMat, listOfPoints.get(2), listOfPoints.get(1), new Scalar(0, 255, 0));
             Imgproc.line(originalMat, listOfPoints.get(2), new Point(listOfPoints.get(2).x, 200), new Scalar(255, 0, 0));
          break;
          case POSTERIOR:
              Imgproc.line(originalMat, listOfPoints.get(0), listOfPoints.get(1), new Scalar(0, 255, 0));
              Imgproc.line(originalMat, listOfPoints.get(1), listOfPoints.get(2), new Scalar(0, 255, 0));

           break;
      }




    }
    void setPoints(List<Point> listOfPoints){

        textViews[0].setText(listOfPoints.get(0).toString());
        textViews[1].setText(listOfPoints.get(1).toString());
        textViews[2].setText(listOfPoints.get(2).toString());


    }

    void calcularAngulos(List<Point> listOfPoints, TipoAvaliacao tipoAvaliacao){

        switch (tipoAvaliacao){

            case ANTERIOR:
                calculoFrontal(listOfPoints);
                break;

            case POSTERIOR:
                calculoPosterior(listOfPoints);
                break;
            default:
                break;
        }



    }

    void calculoFrontal(List<Point> listOfPoints){

        double a = listOfPoints.get(0).y- listOfPoints.get(2).y;
        double b  = listOfPoints.get(0).x - listOfPoints.get(2).x;

        double angulo = Math.atan(b/a);
        angulo *= (180/Math.PI);

        textViews[0].setText("angulo 2.0 : "+ angulo);

        a = listOfPoints.get(1).y - listOfPoints.get(2).y;
        b  = listOfPoints.get(2).x - listOfPoints.get(1).x;

        double angulo1 = Math.atan(b/a);
        angulo1 *= (180/Math.PI);

        textViews[1].setText("angulo 2.1 : "+ angulo1);

        double dif = angulo-angulo1;
        if (dif<0){
            dif*=-1;
        }

        textViews[2].setText("diferença angular: " + dif);


    }


    void calculoPosterior(List<Point> listOfPoints){
        double a = listOfPoints.get(2).x - listOfPoints.get(1).x;
        double b = listOfPoints.get(2).y - listOfPoints.get(1).y;



        double hipotenusaAB = Math.sqrt(Math.pow(a, 2)+Math.pow(b, 2));


        double d = listOfPoints.get(0).x - listOfPoints.get(1).x;
        double e  = listOfPoints.get(1).y - listOfPoints.get(0).y;

        double hipotenusaDE =  Math.sqrt(Math.pow(d, 2)+Math.pow(e, 2));


        double f = listOfPoints.get(0).x - listOfPoints.get(2).x;
        double g = listOfPoints.get(2).y - listOfPoints.get(0).y;

        double hipotenusaFG =  Math.sqrt(Math.pow(f, 2)+Math.pow(g, 2));


       double cosseno  = (Math.pow(hipotenusaAB, 2) + Math.pow(hipotenusaDE, 2)-Math.pow(hipotenusaFG, 2) )/(2 * hipotenusaAB * hipotenusaDE);

       double angle  = Math.acos(cosseno);

       angle*=180/Math.PI;

        textViews[0].setText("angulo : "+ angle);
        textViews[1].setText("");
        textViews[2].setText("");


    }


 double verificaSinal(double x){

        if (x <0){

            x*=-1;
        }

        return x;
    }
}
