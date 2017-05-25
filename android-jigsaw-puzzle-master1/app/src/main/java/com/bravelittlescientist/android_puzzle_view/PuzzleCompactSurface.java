package com.bravelittlescientist.android_puzzle_view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.Random;

public class PuzzleCompactSurface extends SurfaceView implements SurfaceHolder.Callback {
                                                       //SurfaceHolder.Callback是監聽surface改變的一個接口
    /** Surface Components **/
    private PuzzleThread gameThread;
    private volatile boolean running = false;
    private int found = -1;

    /** Puzzle and Canvas **/
    private int MAX_PUZZLE_PIECE_SIZE = 100;
    private int LOCK_ZONE_LEFT = 20;
    private int LOCK_ZONE_TOP = 20;

    private JigsawPuzzle puzzle;
    private BitmapDrawable[] scaledSurfacePuzzlePieces;
    private Rect[] scaledSurfaceTargetBounds;

    private BitmapDrawable backgroundImage;
    private Paint framePaint;

    public PuzzleCompactSurface(Context context) {
        super(context);

        getHolder().addCallback(this); //利用getHolder()取得SurfaceHolder的引用對象

        gameThread = new PuzzleThread(getHolder(), context, this);

        setFocusable(true);
        Toast.makeText(context, "hi", Toast.LENGTH_LONG).show();
        Log.d("PuzzleSurface", "construct");
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) { //這個Activity得到或者失去焦點的時候就會call
        if (!hasWindowFocus) gameThread.pause();

        Log.d("PuzzleSurface", "onWindowsFocusChanged");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,int height) { //在surface的大小發生改變時

        gameThread.setSurfaceSize(width, height);  // 1184,720


    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) { //在創建時，一般在這裡調用畫圖的線程。
        gameThread.setRunning(true);
        gameThread.start();

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {//銷毀時，一般在這裡將畫圖的線程停止、釋放。
       boolean retry = true;
        gameThread.setRunning(false);
        while (retry) {
            try {
                gameThread.join();
                retry = false;
            } catch (InterruptedException e) {
            }
        }
    }


    public void setPuzzle(JigsawPuzzle jigsawPuzzle) {
        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE); //獲得窗口管理對象
        Display display = wm.getDefaultDisplay(); //獲得螢幕資訊
        Point outSize = new Point(); //負責接收size
        display.getSize(outSize);

        puzzle = jigsawPuzzle;
        Random r = new Random();

        if (puzzle.isBackgroundTextureOn()) {
            backgroundImage = new BitmapDrawable(puzzle.getBackgroundTexture());
            backgroundImage.setBounds(0, 0, outSize.x, outSize.y);

        }
        framePaint = new Paint();
        framePaint.setColor(Color.BLACK);
        framePaint.setStyle(Paint.Style.STROKE);
        framePaint.setTextSize(20);
       // Log.d("outsizeX: ",  String.valueOf( outSize.x));
       // Log.d("outsizeY: ",  String.valueOf( outSize.y));
        /** Initialize drawables from puzzle pieces **/
        Bitmap[] originalPieces = puzzle.getPuzzlePiecesArray();
        int[][] positions = puzzle.getPuzzlePieceTargetPositions();
        int[] dimensions = puzzle.getPuzzleDimensions();

        scaledSurfacePuzzlePieces = new BitmapDrawable[originalPieces.length];
        scaledSurfaceTargetBounds = new Rect[originalPieces.length];
       //  Log.d("originalPieces.length: ", String.valueOf(originalPieces.length));
        for (int i = 0; i < originalPieces.length; i++) { //originalPieces.length = 12

           // scaledSurfacePuzzlePieces[i] = new BitmapDrawable(originalPieces[i]);
            scaledSurfacePuzzlePieces[i] = new BitmapDrawable(getResources(),originalPieces[i]);
            // Top left is (0,0) in Android canvas
            int topLeftX = r.nextInt(outSize.x - MAX_PUZZLE_PIECE_SIZE);
            int topLeftY = r.nextInt(outSize.y - 2*MAX_PUZZLE_PIECE_SIZE);
           // Log.d("topLeftX: ", i + " : " + String.valueOf(topLeftX));
           // Log.d("topLeftY: ", i + " : " + String.valueOf(topLeftY));

            scaledSurfacePuzzlePieces[i].setBounds(topLeftX, topLeftY,
                    topLeftX + MAX_PUZZLE_PIECE_SIZE, topLeftY + MAX_PUZZLE_PIECE_SIZE);  //
        }
       // Log.d("dimensions[2]:  ",String.valueOf(dimensions[2]));
      //  Log.d("dimensions[3]: ", String.valueOf(dimensions[3]));
        for (int w = 0; w < dimensions[2]; w++) {
            for (int h = 0; h < dimensions[3]; h++) {
                int targetPiece = positions[w][h];
               // Log.d("targetPiece ", String.valueOf( positions[w][h]));
                scaledSurfaceTargetBounds[targetPiece] = new Rect(
                        LOCK_ZONE_LEFT + w*MAX_PUZZLE_PIECE_SIZE,
                        LOCK_ZONE_TOP + h*MAX_PUZZLE_PIECE_SIZE,
                        LOCK_ZONE_LEFT + w*MAX_PUZZLE_PIECE_SIZE + MAX_PUZZLE_PIECE_SIZE,
                        LOCK_ZONE_TOP + h*MAX_PUZZLE_PIECE_SIZE + MAX_PUZZLE_PIECE_SIZE);
               // Log.d("TARGETBOUND" , targetPiece + " : " +   (String.valueOf(LOCK_ZONE_LEFT + w*MAX_PUZZLE_PIECE_SIZE)+ " , " +String.valueOf(LOCK_ZONE_TOP + h*MAX_PUZZLE_PIECE_SIZE)+ " , " +String.valueOf( LOCK_ZONE_LEFT + w*MAX_PUZZLE_PIECE_SIZE + MAX_PUZZLE_PIECE_SIZE) + " , "+ String.valueOf( LOCK_ZONE_TOP + h*MAX_PUZZLE_PIECE_SIZE + MAX_PUZZLE_PIECE_SIZE)));
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawColor(Color.RED); //

        if (puzzle.isBackgroundTextureOn()) {
            backgroundImage.draw(canvas);    //畫背景
        }
        canvas.drawRect(20, 20, 420, 320, framePaint);

        for (int bmd = 0; bmd < scaledSurfacePuzzlePieces.length; bmd++) {
            if (puzzle.isPieceLocked(bmd)) {
                scaledSurfacePuzzlePieces[bmd].draw(canvas);
            }
        }

        for (int bmd = 0; bmd < scaledSurfacePuzzlePieces.length; bmd++) {
            if (!puzzle.isPieceLocked(bmd)) {
                scaledSurfacePuzzlePieces[bmd].draw(canvas);
            }


        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int xPos =(int) event.getX();
        int yPos =(int) event.getY();

        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:   //手指按下去
                for (int i = 0; i < scaledSurfacePuzzlePieces.length; i++) {
                    Rect place = scaledSurfacePuzzlePieces[i].copyBounds();

                    if (place.contains(xPos, yPos) && !puzzle.isPieceLocked(i)) {
                        found = i;
                           //按下拼圖且未被鎖定，即觸發的事件

                        // Trigger puzzle piece picked up
                        puzzle.onJigsawEventPieceGrabbed(found, place.left, place.top);
                    }
                }
                break;


            case MotionEvent.ACTION_MOVE: //手指放在屏幕上
                if (found >= 0 && found < scaledSurfacePuzzlePieces.length && !puzzle.isPieceLocked(found)) {
                    // Lock into position...
                    if (scaledSurfaceTargetBounds[found].contains(xPos, yPos) ) {
                        scaledSurfacePuzzlePieces[found].setBounds(scaledSurfaceTargetBounds[found]);
                        puzzle.setPieceLocked(found, true);

                        // Trigger jigsaw piece events
                        puzzle.onJigsawEventPieceMoved(found,
                                scaledSurfacePuzzlePieces[found].copyBounds().left,
                                scaledSurfacePuzzlePieces[found].copyBounds().top);
                        puzzle.onJigsawEventPieceDropped(found,
                                scaledSurfacePuzzlePieces[found].copyBounds().left,
                                scaledSurfacePuzzlePieces[found].copyBounds().top);
                    } else {
                        Rect rect = scaledSurfacePuzzlePieces[found].copyBounds();

                        rect.left = xPos - MAX_PUZZLE_PIECE_SIZE/2;
                        rect.top = yPos - MAX_PUZZLE_PIECE_SIZE/2;
                        rect.right = xPos + MAX_PUZZLE_PIECE_SIZE/2;
                        rect.bottom = yPos + MAX_PUZZLE_PIECE_SIZE/2;
                        scaledSurfacePuzzlePieces[found].setBounds(rect);

                        // Trigger jigsaw piece event
                        puzzle.onJigsawEventPieceMoved(found, rect.left, rect.top);
                    }
                }
                break;

            case MotionEvent.ACTION_UP: //手指離開屏幕
                // Trigger jigsaw piece event
                if (found >= 0 && found < scaledSurfacePuzzlePieces.length) {
                    puzzle.onJigsawEventPieceDropped(found, xPos, yPos);
                }
                found = -1;
                break;

        }


        return true;
    }

    public PuzzleThread getThread () {
        return gameThread;
    }
}
