package com.example.android.bitview;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;


public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int SELECT_IMAGE = 1;
    private static int RESULT_LOAD_IMAGE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button cameraButton = (Button) findViewById(R.id.cameraButton);
        cameraButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
            }
        });

        final Button galleryButton = (Button) findViewById(R.id.galleryButton);
        galleryButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
//                Intent galleryIntent = new Intent();
//                galleryIntent.setType("image/*");
//                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
//                startActivityForResult(Intent.createChooser(galleryIntent, "Select Picture"), SELECT_IMAGE);
                Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, RESULT_LOAD_IMAGE);
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

            Bitmap pic = processBitmap(picturePath);

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

    private Bitmap processBitmap(String picturePath) {
        /**
         * Process the selected bitmap to become a 3-bit, dithered image!
         */

        // The Bitmap from decoding file is non-mutable :( Realistically that's good because I
        // don't want to derp up my images. So let's make a mutable copy to work with! :D
        Bitmap nonMutablePic = BitmapFactory.decodeFile(picturePath);
        Bitmap pic = nonMutablePic.copy(Bitmap.Config.ARGB_8888, true);

        // Get dimensions to base the following work off of.
        int picHeight = pic.getHeight();
        int picWidth = pic.getWidth();

        // In this block we're go to change every pixel to the closest 3-bit value. Then we're
        // going to "dither", or randomize, the further pixels as to make the image look more
        // cleaner... for a 3-bit looking image that is... Ha!
        int x, y, oldPixel, newPixel;
        int quantError, rightPixel, rightBottomPixel, bottomPixel, leftBottomPixel;
        for(y=0; y<picHeight; y++) {
            for(x=0; x<picWidth; x++) {
                oldPixel = pic.getPixel(x, y);
                newPixel = colorPicker(oldPixel);
                pic.setPixel(x, y, newPixel);

            }
        }

        return pic;
    }

    private int colorPicker(int pixel) {
        /**
         * Figure out what the closest 3 bit color is to this pixel and change it to that!
         */
        int red, green, blue, alpha;

        red   = (Color.red(pixel)   >= 127) ? 255 : 0;
        green = (Color.green(pixel) >= 127) ? 255 : 0;
        blue  = (Color.blue(pixel)  >= 127) ? 255 : 0;

        // Keeping image alpha in case future work might need it.
        alpha =  Color.alpha(pixel);

        return Color.argb(alpha, red, green, blue);
    }
}
