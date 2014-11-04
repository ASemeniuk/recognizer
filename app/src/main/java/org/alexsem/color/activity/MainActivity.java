package org.alexsem.color.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import org.alexsem.color.R;
import org.alexsem.color.widget.CloseColorView;
import org.alexsem.color.widget.TargetedImageView;

import java.io.File;
import java.io.IOException;
import java.util.Date;

public class MainActivity extends Activity {

    private final int REQUEST_PICK_IMAGE = 10;
    private int SCREEN_WIDTH;

    private ViewFlipper mFlipper;

    private TargetedImageView mImage;
    private Bitmap mImageBitmap;
    private String mImagePath;
    private View mPanelLeft;
    private View mPanelCenter;
    private View mPanelRight;
    private View mLoad;

    private String mCurrentPhotoPath;

    private TextView mPanelHex;
    private View mPanelPreview;
    private TextView mPanelRed;
    private TextView mPanelGreen;
    private TextView mPanelBlue;
    private SeekBar mPanelHueSeek;
    private TextView mPanelHueText;
    private SeekBar mPanelSaturationSeek;
    private TextView mPanelSaturationText;
    private SeekBar mPanelValueSeek;
    private TextView mPanelValueText;

    private int mPanelPreviewColor;

    private CloseColorView mCloseColor;
    private SeekBar mCloseSaturationSeek;
    private TextView mCloseSaturationText;

    private float[] mCloseColorHsv = new float[3];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        //--- Get screen width ---
        Point size = new Point();
        getWindowManager().getDefaultDisplay().getSize(size);
        SCREEN_WIDTH = size.x;

        //--- Setup flipper ---
        mFlipper = (ViewFlipper) findViewById(R.id.main_flipper);

        //--- First screen ---
        mImage = (TargetedImageView) findViewById(R.id.main_image);
        mImage.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_MOVE:
                        mImage.setTarget(event.getX(), event.getY());
                        if (event.getX() < SCREEN_WIDTH / 2) {
                            mPanelLeft.setVisibility(View.VISIBLE);
                            mPanelRight.setVisibility(View.GONE);
                        } else {
                            mPanelLeft.setVisibility(View.GONE);
                            mPanelRight.setVisibility(View.VISIBLE);
                        }
                        mPanelCenter.setVisibility(View.VISIBLE);
                        mLoad.setVisibility(View.GONE);

                        float[] eventXY = new float[]{event.getX(), event.getY()};
                        Matrix invertMatrix = new Matrix();
                        mImage.getImageMatrix().invert(invertMatrix);
                        invertMatrix.mapPoints(eventXY);
                        int x = Math.round(eventXY[0]);
                        int y = Math.round(eventXY[1]);
                        Bitmap bitmap = ((BitmapDrawable) mImage.getDrawable()).getBitmap();
                        if (x < 0) {
                            x = 0;
                        } else if (x > bitmap.getWidth() - 1) {
                            x = bitmap.getWidth() - 1;
                        }
                        if (y < 0) {
                            y = 0;
                        } else if (y > bitmap.getHeight() - 1) {
                            y = bitmap.getHeight() - 1;
                        }
                        setPreviewColor(bitmap.getPixel(x, y));
                        return true;
                }
                return false;
            }
        });
        mPanelLeft = findViewById(R.id.main_panel_left);
        mPanelRight = findViewById(R.id.main_panel_right);
        mPanelCenter = findViewById(R.id.main_panel_center);
        mPanelCenter.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return true;
            }
        });
        mLoad = findViewById(R.id.main_load);
        mLoad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                Intent chooserIntent = Intent.createChooser(pickIntent, getString(R.string.pick_chooser));
                if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) { //Camera available
                    try {
                        Intent takeIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        String name = String.format("IMG_%1$tY%1$tm%1$td%1$tH%1$tM%1$tS", new Date());
                        File image = File.createTempFile(name, ".jpg", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES));
                        mCurrentPhotoPath = image.getAbsolutePath();
                        takeIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(image));
                        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{takeIntent});
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                startActivityForResult(chooserIntent, REQUEST_PICK_IMAGE);
            }
        });

        //--- Color panel ---
        mPanelHex = (TextView) findViewById(R.id.main_panel_hex);
        findViewById(R.id.main_panel_x).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        mPanelPreview = findViewById(R.id.main_panel_preview);
        mPanelRed = (TextView) findViewById(R.id.main_panel_red);
        mPanelGreen = (TextView) findViewById(R.id.main_panel_green);
        mPanelBlue = (TextView) findViewById(R.id.main_panel_blue);
        mPanelHueSeek = (SeekBar) findViewById(R.id.main_panel_hue_seek);
        mPanelHueSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mPanelHueText.setText(String.valueOf(progress));
                if (fromUser) {
                    float[] hsv = new float[3];
                    hsv[0] = mPanelHueSeek.getProgress();
                    hsv[1] = (float) mPanelSaturationSeek.getProgress() / 100f;
                    hsv[2] = (float) mPanelValueSeek.getProgress() / 100f;
                    setPreviewColor(Color.HSVToColor(hsv));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        mPanelHueText = (TextView) findViewById(R.id.main_panel_hue_text);
        mPanelSaturationSeek = (SeekBar) findViewById(R.id.main_panel_saturation_seek);
        mPanelSaturationSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mPanelSaturationText.setText(String.valueOf(progress));
                if (fromUser) {
                    float[] hsv = new float[3];
                    hsv[0] = mPanelHueSeek.getProgress();
                    hsv[1] = (float) mPanelSaturationSeek.getProgress() / 100f;
                    hsv[2] = (float) mPanelValueSeek.getProgress() / 100f;
                    setPreviewColor(Color.HSVToColor(hsv));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        mPanelSaturationText = (TextView) findViewById(R.id.main_panel_saturation_text);
        mPanelValueSeek = (SeekBar) findViewById(R.id.main_panel_value_seek);
        mPanelValueSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mPanelValueText.setText(String.valueOf(progress));
                if (fromUser) {
                    float[] hsv = new float[3];
                    hsv[0] = mPanelHueSeek.getProgress();
                    hsv[1] = (float) mPanelSaturationSeek.getProgress() / 100f;
                    hsv[2] = (float) mPanelValueSeek.getProgress() / 100f;
                    setPreviewColor(Color.HSVToColor(hsv));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        mPanelValueText = (TextView) findViewById(R.id.main_panel_value_text);
        findViewById(R.id.main_panel_process).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setCloseColor(mPanelPreviewColor);
                mFlipper.showNext();
            }
        });

        //--- Second screen ---
        mCloseColor = (CloseColorView) findViewById(R.id.main_close_color);
        mCloseSaturationSeek = (SeekBar) findViewById(R.id.main_close_saturation_seek);
        mCloseSaturationSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mCloseSaturationText.setText(String.valueOf(progress));
                if (fromUser) {
                    mCloseColorHsv[1] = (float) progress / 100f;
                    mCloseColor.setColor(Color.HSVToColor(mCloseColorHsv));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        mCloseSaturationText = (TextView) findViewById(R.id.main_close_saturation_text);
        findViewById(R.id.main_close_invert).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setCloseColor((0xFFFFFFFF - Color.HSVToColor(mCloseColorHsv)) | 0xFF000000);
            }
        });
        findViewById(R.id.main_close_reset).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setCloseColor(mPanelPreviewColor);
            }
        });
        findViewById(R.id.main_close_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        //--- Initialization ---
        mImagePath = getSharedPreferences(getPackageName(), MODE_PRIVATE).getString("path", null);
        if (mImagePath != null) {
            immerse();
            loadImage(mImagePath);
        } else {
            mLoad.performClick();
        }

    }

    /**
     * Start Immersive mode
     */
    private void immerse() {
        mImage.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);
    }

    /**
     * Set new color to be previewed in panel
     * @param color Color to set
     */
    private void setPreviewColor(int color) {
        mPanelPreviewColor = color;
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        mPanelHex.setText(String.format("#%06X", 0xFFFFFF & color));
        mPanelPreview.setBackgroundColor(color);
        mPanelRed.setText(String.valueOf(Color.red(color)));
        mPanelGreen.setText(String.valueOf(Color.green(color)));
        mPanelBlue.setText(String.valueOf(Color.blue(color)));
        mPanelHueSeek.setProgress(Math.round(hsv[0]));
        mPanelSaturationSeek.setProgress(Math.round(hsv[1] * 100));
        mPanelValueSeek.setProgress(Math.round(hsv[2] * 100));
    }

    /**
     * Setup color for close view display
     * @param color Color to set
     */
    private void setCloseColor(int color) {
        Color.colorToHSV(color, mCloseColorHsv);
        mCloseSaturationSeek.setProgress(Math.round(mCloseColorHsv[1] * 100));
        mCloseColor.setColor(color);
    }

    /**
     * Load image file for processing
     * @param path Path to the file
     */
    private void loadImage(String path) {
        if (path != null) {
            mImagePath = path;
            if (mImageBitmap != null) {
                mImageBitmap.recycle();
            }
            try {
                mImageBitmap = BitmapFactory.decodeFile(path);
                mImage.setImageBitmap(mImageBitmap);
                getSharedPreferences(getPackageName(), MODE_PRIVATE).edit().putString("path", path).commit();
                Toast.makeText(this, R.string.pick_success, Toast.LENGTH_SHORT).show();
            } catch (Throwable ex) {
                Toast.makeText(this, R.string.pick_failure, Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, R.string.pick_invalid, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        immerse();
        if (requestCode == REQUEST_PICK_IMAGE && resultCode == RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                Cursor cursor = getContentResolver().query(uri, new String[]{MediaStore.Images.ImageColumns.DATA}, null, null, null);
                try {
                    cursor.moveToFirst();
                    loadImage(cursor.getString(0));
                } finally {
                    cursor.close();
                }
            } else {
                loadImage(mCurrentPhotoPath);
            }
            mCurrentPhotoPath = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (mFlipper.getDisplayedChild() > 0) {
            mFlipper.showPrevious();
            return;
        }
        if (mPanelCenter.getVisibility() == View.VISIBLE) {
            mPanelCenter.setVisibility(View.GONE);
            mLoad.setVisibility(View.VISIBLE);
            mImage.resetTarget();
            return;
        }
        super.onBackPressed();
    }

}
