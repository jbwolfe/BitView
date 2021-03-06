package com.example.android.bitview;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";
    private static int REQUEST_IMAGE_CAPTURE = 2;
    private static int RESULT_LOAD_IMAGE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Open camera, process taken picture, view the processed captured picture.
        final Button cameraButton = (Button) findViewById(R.id.cameraButton);
        cameraButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }

            }
        });

        // Open gallery, process chosen picture, view the processed chosen picture.
        final Button galleryButton = (Button) findViewById(R.id.galleryButton);
        galleryButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, RESULT_LOAD_IMAGE);
            }
        });

        // Long long click, save processed picture if there is one there.
        final ImageView imageViewer = (ImageView) findViewById(R.id.imageViewer);
        imageViewer.setLongClickable(true);
        imageViewer.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                // Perform action on long click
                if (imageViewer.getDrawable() != null) {
                    File storageDir = new File(Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_PICTURES) + File.separator + "BitView");
                    if (!storageDir.isDirectory()) {
                        storageDir.mkdir();
                    }

                    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                    String imageFileName = "BitView_" + timeStamp + ".png";

                    ImageView imageView = (ImageView) findViewById(R.id.imageViewer);
                    imageView.buildDrawingCache();
                    Bitmap bitmap = imageView.getDrawingCache();

                    FileOutputStream fp;
                    try {
                        fp = new FileOutputStream(new File(storageDir + File.separator + imageFileName));
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fp);
                        fp.flush();
                        fp.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    Toast.makeText(getApplicationContext(),
                            "FILE PATH: " + storageDir + "/" + imageFileName,
                            Toast.LENGTH_LONG).show();
                }

                return true;
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };

            Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();

            Bitmap nonMutablePic = BitmapFactory.decodeFile(picturePath);
            Bitmap pic = processBitmap(nonMutablePic);

            ImageView imageView = (ImageView) findViewById(R.id.imageViewer);
            imageView.setImageBitmap(pic);
        }
        else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();

            Bitmap imageBitmap = (Bitmap) extras.get("data");
            Bitmap pic = processBitmap(imageBitmap);

            ImageView imageView = (ImageView) findViewById(R.id.imageViewer);
            imageView.setImageBitmap(pic);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.saveFile) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private Bitmap processBitmap(Bitmap bitmap) {
        /**
         * Process the selected bitmap to become a 3-bit, dithered image!
         */
        // The Bitmap from decoded file path is non-mutable :( Realistically that's good because I
        // don't want to derp up my images. So let's make a mutable copy to work with! :D
        Bitmap pic = bitmap.copy(Bitmap.Config.ARGB_8888, true);

        // Get dimensions to base the following work off of.
        int picHeight = pic.getHeight();
        int picWidth = pic.getWidth();

        // In this block we're go to change every pixel to the closest 3-bit value. Then we're
        // going to "dither", or randomize, the adjacent, further pixels as to make the image
        // look more cleaner... for a 3-bit looking image that is... Ha!
        int x, y, oldPixel, newPixel;
        int quantError, rightPixel, bottomRightPixel, bottomPixel, bottomLeftPixel;
        for(y=0; y<picHeight; y++) {
            for(x=0; x<picWidth; x++) {
                oldPixel = pic.getPixel(x, y);
                newPixel = colorPicker(oldPixel);
                pic.setPixel(x, y, newPixel);

                quantError = computeQuantizationError(oldPixel, newPixel);

                if(x+1 < picWidth) { // Right pixel
                    rightPixel = pic.getPixel(x+1, y);
                    pic.setPixel(x+1, y, ditherPixel(rightPixel, quantError, 7.0/16.0));

                    if (y+1 < picHeight) { // Right bottom pixel
                        bottomRightPixel = pic.getPixel(x+1, y+1);
                        pic.setPixel(x + 1, y+1, ditherPixel(bottomRightPixel, quantError, 1.0/16.0));
                    }
                }
                if(y+1 < picHeight) { // Bottom pixel
                    bottomPixel = pic.getPixel(x, y+1);
                    pic.setPixel(x, y+1, ditherPixel(bottomPixel, quantError, 5.0/16.0));

                    if (x-1 >= 0) { // Left bottom pixel
                        bottomLeftPixel = pic.getPixel(x-1, y+1);
                        pic.setPixel(x-1, y+1, ditherPixel(bottomLeftPixel, quantError, 3.0/16.0));
                    }
                }
            }
        }

        return pic;
    }

    private int colorPicker(int pixel) {
        /**
         * Figure out what the closest 3 bit color is to this pixel and change it to that!
         */
        int red, green, blue, alpha;

        red   = (Color.red(pixel)   > 127) ? 255 : 0;
        green = (Color.green(pixel) > 127) ? 255 : 0;
        blue  = (Color.blue(pixel)  > 127) ? 255 : 0;
        alpha = Color.alpha(pixel);

        return Color.argb(alpha, red, green, blue);
    }

    private int computeQuantizationError(int oldPixel, int newPixel) {
        /**
         * Computer the quantization error between the original pixel and the newly colored pixel.
         */
        int oldRed, oldGreen, oldBlue, oldAlpha,
            newRed, newGreen, newBlue, newAlpha;

        oldRed   = Color.red(oldPixel);
        oldGreen = Color.green(oldPixel);
        oldBlue  = Color.blue(oldPixel);
        oldAlpha = Color.alpha(oldPixel);

        newRed   = Color.red(newPixel);
        newGreen = Color.green(newPixel);
        newBlue  = Color.blue(newPixel);
        newAlpha = Color.alpha(newPixel);

        return Color.argb(newAlpha, oldRed - newRed, oldGreen - newGreen, oldBlue - newBlue);
    }

    private int ditherPixel(int futurePixel, int quantError, double factor) {
        /**
         * Dither pixel that has yet to be reached.
         */
        int futureRed, futureGreen, futureBlue, futureAlpha,
            quantRed,  quantGreen,  quantBlue, quantAlpha;

        quantRed   = (int)(Color.red(quantError)   * factor);
        quantGreen = (int)(Color.green(quantError) * factor);
        quantBlue  = (int)(Color.blue(quantError)  * factor);
        quantAlpha = (int)(Color.alpha(quantError));

        futureRed   = Color.red(futurePixel);
        futureGreen = Color.green(futurePixel);
        futureBlue  = Color.blue(futurePixel);
        futureAlpha = Color.alpha(futurePixel);

//        return Color.argb(futureAlpha, futureRed + quantRed, futureGreen + quantGreen, futureBlue + quantBlue);
        return Color.argb(futureAlpha, futureRed, futureGreen, futureBlue);
    }
}
