package com.example.dodgegamefinal;

import android.graphics.Color;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.os.Bundle;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.dodgegamefinal.R;

public class MainActivity extends AppCompatActivity {
    // Declare GameSurface Instance Defined in Inner Class
    GameSurface gameSurface;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        // Initialize GameSurface in Context (In this case, the MainActivity)
        gameSurface = new GameSurface(this);

        // Instead of using the standard XML, we are employ the "Canvas" created in the GameSurface instance
        setContentView(gameSurface);
    }

    // LifeCycle Methods employed to call the GameSurface pause/resume and therefore
    // ensure our game does not crash if/when the application is paused/resumed
    @Override
    protected void onResume() {
        super.onResume();
        gameSurface.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        gameSurface.pause();
    }
    // Define GameSurface
    public class GameSurface extends SurfaceView implements Runnable, SensorEventListener{
        // Almost all of these variables are required anytime you are implementing a SurfaceView
        Thread gameThread;  // required for functionality
        SurfaceHolder holder; // required for functionality
        volatile boolean running = false; // variable shared amongst threads; required for functionality
        Bitmap car, background, tire;
        float movementX;
        float previouslocation;
        MediaPlayer mediaPlayer;
        float tireheight;
        Paint paintProperty; // required for functionality
        int screenWidth, screenHeight; // required for functionality
        private float roll = 0;
        int velocity=1;
        int count=0;
        int score;
        boolean speed;
        int lives=3;
        boolean displaySpeedUpMessage;
        float tireloc=(float) (Math.random()*screenWidth+1);
        private float x,y;
        private long speedUpMessageStartTime = 0;
        private static final long SPEED_UP_MESSAGE_DISPLAY_DURATION = 3000;
        boolean collision;
        boolean livesshow=true;
        private long speedUpTextDisplayStartTime = 0;
        private static final long SPEED_UP_TEXT_DISPLAY_DURATION = 3000;


        public GameSurface(Context context) {
            super(context);
            // Initialize holder
            holder = getHolder();

            //plays background audio
            mediaPlayer = MediaPlayer.create(MainActivity.this,R.raw.drivingnoise);
            mediaPlayer.setLooping(true);
            mediaPlayer.start();

            setOnTouchListener(new OnTouchListener() {
                @Override

                //screen touch response
                public boolean onTouch(View v, MotionEvent event) {
                    speed=true;
                    return true;
                }
            });

            // Initialize resources
            background = BitmapFactory.decodeResource(getResources(), R.drawable.road);
            car = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.car),
                    500, 500, false);
            tire = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.tire),
                    400, 300, false);

            // Retrieve screensize
            Display screenDisplay = getWindowManager().getDefaultDisplay();
            Point sizeOfScreen = new Point();
            screenDisplay.getSize(sizeOfScreen);
            screenWidth = sizeOfScreen.x;
            screenHeight = sizeOfScreen.y;

            // Initialize paintProperty for "drawing" on the Canvas
            paintProperty = new Paint();

            // Needed if using MotionSensors to create the image movement
            SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            //using x rotation of phone to move
            movementX = (float)(event.values[0]*50);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }

        // Create run method for thread
        @Override
        public void run() {
            // Create Canvas to "draw" on
            Canvas canvas = null;
            // Put resource into a referencable Drawable
            Drawable d = getResources().getDrawable(R.drawable.road, null);

            // loop should run as long as running == true
            while(running) {
                // if holder is null or invalid, exit loop
                if (!holder.getSurface().isValid())
                    continue;

                // lock canvas to make necessary changes
                canvas = holder.lockCanvas(null);

                // resize background drawable to the root View left/top/right/bottom
                d.setBounds(getLeft(), getTop(), getRight(), getBottom());

                // draw the Drawable onto the canvas
                d.draw(canvas);

                // Define the spacing required to accommodate the image and screen size so images don't exceed bounds
                float ballImageHorizontalSpacing = (screenWidth / 2.0f) - (car.getWidth() / 2.0f);
                float ballImageVerticalSpacing = (screenHeight / 2.0f) - (car.getHeight() / 2.0f);

                //limits to borders
                float totalmovement = 0;

                //sets boundaries for car
                float newloc = ballImageHorizontalSpacing-(float)(velocity*movementX);

                if(newloc<screenWidth-400&&newloc>0)
                    totalmovement = newloc;

                else if(newloc>=screenWidth-400)
                    totalmovement = screenWidth-400;

                else if(newloc<=0)
                    totalmovement = 0;

                Paint textPaint = new Paint();
                textPaint.setColor(Color.WHITE);
                textPaint.setTextSize(100);
                canvas.drawText("Score: " + score, ((screenWidth / 2.0f) - (car.getWidth() / 2.0f))+75, 200, textPaint);

                if(lives==0)
                    livesshow = false;

                if(livesshow)
                    canvas.drawText("Lives: " + lives, ((screenWidth / 2.0f) - (car.getWidth() / 2.0f))+75, 400, textPaint);

                if (speed) {
                    speedUpTextDisplayStartTime = System.currentTimeMillis();
                    velocity = 2;
                    displaySpeedUpMessage = true;
                }


                // Display "SPEED UP!!!" message
                if (displaySpeedUpMessage) {
                    // Draw the "SPEED UP!!!" message
                    canvas.drawText("SPEED UP!!!", ((screenWidth / 2.0f) - (car.getWidth() / 2.0f)) + 75, ballImageVerticalSpacing, textPaint);

                    // Check if the display duration has elapsed
                    if (System.currentTimeMillis() - speedUpMessageStartTime >= SPEED_UP_MESSAGE_DISPLAY_DURATION) {
                        displaySpeedUpMessage = false; // Stop displaying the message
                    }
                }

                if(!speed) {
                    velocity = 1;
                }

                canvas.drawBitmap(car, totalmovement, ballImageVerticalSpacing+600, null);

                Rect carRect = new Rect();
                Rect tireRect = new Rect();

                int carLeft = (int) totalmovement;
                int carTop = (int) (ballImageVerticalSpacing + 600);
                int carRight = carLeft + car.getWidth();
                int carBottom = carTop + car.getHeight();
                carRect.set(carLeft+50, carTop+50, carRight-50, carBottom-50);

                int tireLeft = (int) tireloc;
                int tireTop = (int) tireheight;
                int tireRight = tireLeft + tire.getWidth();
                int tireBottom = tireTop + tire.getHeight();
                tireRect.set(tireLeft+50, tireTop+50, tireRight-50, tireBottom-50);

                if(tireheight<screenHeight) {
                    canvas.drawBitmap(tire,tireloc , tireheight += 20, null);
                }

                if (Rect.intersects(carRect, tireRect)) {
                    // Collision detected
                    tireheight = 0;
                    tireloc = (float) (Math.random()*(screenWidth-400)+200);
                    collision = true;
                    lives--;
                    MediaPlayer mediaPlayer1 = MediaPlayer.create(MainActivity.this,R.raw.stopnoise);
                    mediaPlayer1.start();

                    // Check if lives reach 0
                    if (lives == 0) {
                        MediaPlayer mediaPlayer2 = MediaPlayer.create(MainActivity.this,R.raw.crash);
                        mediaPlayer2.start();
                        displaySpeedUpMessage =false;
                        canvas.drawText("Lives: " + lives, ((screenWidth / 2.0f) - (car.getWidth() / 2.0f))+75, 400, textPaint);
                        canvas.drawText("GAME OVER", ballImageHorizontalSpacing, screenHeight-200, textPaint);
                        mediaPlayer.stop();
                        running = false;

                    } else {
                        // Reset car position and update carRect
                        totalmovement = ballImageHorizontalSpacing; // Reset car position
                        carLeft = (int) totalmovement;
                        carRight = carLeft + car.getWidth();
                        carRect.set(carLeft, carTop, carRight, carBottom);
                    }

                } else if (tireheight >= screenHeight) {
                    // Tire reached the bottom without collision
                    tireheight = 0;
                    tireloc = (float) (Math.random()*(screenWidth-400)+200);
                    score++;
                }



                holder.unlockCanvasAndPost(canvas);
            }

        }public void resume(){
            running = true;
            gameThread = new Thread(this);
            gameThread.start();
        }
        public void pause(){
            running = false;
            while(true){
                try{
                    gameThread.join();
                }catch (InterruptedException e){
                    e.printStackTrace();}
            }
        }


    }
}
