package com.example.ufogame;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Random;

public class GameView extends View {

    float playerX = 500;
    float playerY = 1500;

    int score = 0;
    int hp = 5;
    int highScore = 0;

    Random random = new Random();

    Paint textPaint;
    Paint starPaint;
    Paint gunPaint;

    Bitmap playerShip;
    Bitmap enemyShip;
    Bitmap explosionImg;
    Bitmap bulletImg;

    Bitmap heartFull;
    Bitmap heartEmpty;

    boolean gameOver = false;
    boolean gameStarted = false;
    boolean paused = false;

    boolean rapidFire = false;
    long rapidFireEnd = 0;

    long lastShotTime = 0;

    SharedPreferences prefs;
    ToneGenerator tone;

    class Bullet { float x,y; }
    class Enemy { float x,y; }

    class Explosion{
        float x,y;
        int life=15;
    }

    class Item{
        float x,y;
        int type;
    }
    class Boss{
        float x,y;
        int hp=30;
        boolean active=false;
    }
    ArrayList<Bullet> bullets = new ArrayList<>();
    ArrayList<Enemy> enemies = new ArrayList<>();
    ArrayList<Explosion> explosions = new ArrayList<>();
    ArrayList<Item> items = new ArrayList<>();
    Boss boss = new Boss();
    boolean bossSpawned = false;
    int starCount=100;
    float[] starsX = new float[starCount];
    float[] starsY = new float[starCount];

    public GameView(Context context) {
        super(context);
        init(context);
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context){

        prefs=context.getSharedPreferences("game",Context.MODE_PRIVATE);
        highScore=prefs.getInt("high",0);

        textPaint=new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(70);

        starPaint=new Paint();
        starPaint.setColor(Color.WHITE);

        gunPaint=new Paint();
        gunPaint.setColor(Color.YELLOW);

        tone=new ToneGenerator(AudioManager.STREAM_MUSIC,100);

        setBackgroundColor(Color.BLACK);

        playerShip=BitmapFactory.decodeResource(getResources(),R.drawable.ship);
        enemyShip=BitmapFactory.decodeResource(getResources(),R.drawable.enemy);
        explosionImg=BitmapFactory.decodeResource(getResources(),R.drawable.explosion);
        bulletImg=BitmapFactory.decodeResource(getResources(),R.drawable.bullet);

        heartFull=BitmapFactory.decodeResource(getResources(),android.R.drawable.btn_star_big_on);
        heartEmpty=BitmapFactory.decodeResource(getResources(),android.R.drawable.btn_star_big_off);

        playerShip=Bitmap.createScaledBitmap(playerShip,150,150,false);
        enemyShip=Bitmap.createScaledBitmap(enemyShip,120,120,false);
        explosionImg=Bitmap.createScaledBitmap(explosionImg,150,150,false);
        bulletImg=Bitmap.createScaledBitmap(bulletImg,40,80,false);

        heartFull=Bitmap.createScaledBitmap(heartFull,60,60,false);
        heartEmpty=Bitmap.createScaledBitmap(heartEmpty,60,60,false);

        int width=getResources().getDisplayMetrics().widthPixels;

        for(int i=0;i<starCount;i++){
            starsX[i]=random.nextInt(width);
            starsY[i]=random.nextInt(2000);
        }

        spawnEnemies();
    }

    private void spawnEnemies(){
        enemies.clear();

        int width=getResources().getDisplayMetrics().widthPixels;

        for(int i=0;i<5;i++){
            Enemy e=new Enemy();
            e.x=random.nextInt(width);
            e.y=random.nextInt(400);
            enemies.add(e);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(!gameStarted){
            canvas.drawText("SPACE SHOOTER",200,600,textPaint);
            canvas.drawText("TAP TO START",260,800,textPaint);
            invalidate();
            return;
        }

        if(gameOver){
            canvas.drawText("GAME OVER",300,650,textPaint);
            canvas.drawText("Score: "+score,320,780,textPaint);
            canvas.drawText("Tap to Restart",260,900,textPaint);
            return;
        }

        if(paused){
            canvas.drawText("PAUSED",400,700,textPaint);
            invalidate();
            return;
        }

        for(int i=0;i<starCount;i++){

            canvas.drawCircle(starsX[i],starsY[i],3,starPaint);

            starsY[i]+=4;

            if(starsY[i]>getHeight()){
                starsY[i]=0;
                starsX[i]=random.nextInt(getWidth());
            }
        }

        canvas.drawText("Score: "+score,50,100,textPaint);
// spawn boss khi 100 điểm
        if(score>=100 && !bossSpawned){
            boss.x=getWidth()/2;
            boss.y=200;
            boss.hp=30;
            boss.active=true;
            bossSpawned=true;
        }
        for(int i=0;i<5;i++){

            if(i<hp)
                canvas.drawBitmap(heartFull,50+i*70,140,null);
            else
                canvas.drawBitmap(heartEmpty,50+i*70,140,null);
        }

        canvas.drawBitmap(playerShip,playerX-75,playerY-75,null);
        if(boss.active){

            Bitmap bigEnemy = Bitmap.createScaledBitmap(enemyShip,300,300,false);

            canvas.drawBitmap(bigEnemy,boss.x-150,boss.y-150,null);

            boss.x += 5;

            if(boss.x > getWidth()-150 || boss.x < 150){
                boss.x -= 5;
            }
        }
        int fireRate = rapidFire ? 120 : 300;

        if(System.currentTimeMillis()-lastShotTime>fireRate){

            Bullet b=new Bullet();
            b.x=playerX;
            b.y=playerY;
            bullets.add(b);

            tone.startTone(ToneGenerator.TONE_PROP_BEEP,80);

            lastShotTime=System.currentTimeMillis();
        }

        for(int i=0;i<bullets.size();i++){

            Bullet b=bullets.get(i);
// trúng boss
            if(boss.active){

                if(b.x>boss.x-150 && b.x<boss.x+150 &&
                        b.y>boss.y-150 && b.y<boss.y+150){

                    boss.hp--;

                    bullets.remove(i);
                    i--;

                    if(boss.hp<=0){

                        boss.active=false;
                        score+=20;

                        Explosion ex=new Explosion();
                        ex.x=boss.x;
                        ex.y=boss.y;
                        explosions.add(ex);
                    }

                    continue;
                }
            }
            canvas.drawBitmap(bulletImg,b.x-20,b.y-40,null);

            b.y-=25;

            if(b.y<0){
                bullets.remove(i);
                i--;
            }
        }

        for(int i=0;i<enemies.size();i++){

            Enemy e=enemies.get(i);

            canvas.drawBitmap(enemyShip,e.x-60,e.y-60,null);

            e.y+=6;

            if(e.y>getHeight()){
                e.y=0;
                e.x=random.nextInt(getWidth());
            }

            if(e.y>playerY-80 && e.x>playerX-80 && e.x<playerX+80){
                hp--;
                e.y=0;
            }

            for(int j=0;j<bullets.size();j++){

                Bullet b=bullets.get(j);

                if(b.x>e.x-60 && b.x<e.x+60 && b.y>e.y-60 && b.y<e.y+60){

                    score++;

                    Explosion ex=new Explosion();
                    ex.x=e.x;
                    ex.y=e.y;
                    explosions.add(ex);

                    tone.startTone(ToneGenerator.TONE_PROP_NACK,120);

                    if(random.nextInt(15)==1){

                        Item it=new Item();
                        it.x=e.x;
                        it.y=e.y;
                        it.type=random.nextBoolean()?1:2;

                        items.add(it);
                    }

                    e.y=0;
                    bullets.remove(j);
                    j--;
                }
            }
        }

        for(int i=0;i<items.size();i++){

            Item it=items.get(i);

            if(it.type==1)
                canvas.drawBitmap(heartFull,it.x-30,it.y-30,null);
            else
                canvas.drawRect(it.x-20,it.y-20,it.x+20,it.y+20,gunPaint);

            it.y+=8;

            if(it.y>playerY-80 && it.x>playerX-80 && it.x<playerX+80){

                if(it.type==1)
                    hp=Math.min(5,hp+1);

                if(it.type==2){
                    rapidFire=true;
                    rapidFireEnd=System.currentTimeMillis()+6000;
                }

                items.remove(i);
                i--;
            }
        }

        if(rapidFire && System.currentTimeMillis()>rapidFireEnd){
            rapidFire=false;
        }

        for(int i=0;i<explosions.size();i++){

            Explosion ex=explosions.get(i);

            canvas.drawBitmap(explosionImg,ex.x-75,ex.y-75,null);

            ex.life--;

            if(ex.life<=0){
                explosions.remove(i);
                i--;
            }
        }

        if(hp<=0){

            gameOver=true;

            if(score>highScore){
                highScore=score;
                prefs.edit().putInt("high",score).apply();
            }
        }

        invalidate();
    }

    public void restartGame(){

        score=0;
        hp=5;

        bullets.clear();
        explosions.clear();
        items.clear();

        spawnEnemies();

        gameOver=false;
        gameStarted=true;
    }

    public void startGame(){
        gameStarted=true;
    }

    public void pauseGame(){
        paused=!paused;
        boss.active=false;
        bossSpawned=false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){

        if(event.getAction()==MotionEvent.ACTION_DOWN){

            if(!gameStarted){
                gameStarted=true;
                return true;
            }

            if(gameOver){
                restartGame();
                return true;
            }
        }

        if(!gameOver && (event.getAction()==MotionEvent.ACTION_MOVE ||
                event.getAction()==MotionEvent.ACTION_DOWN)){

            playerX=event.getX();
            playerY=event.getY();
        }

        return true;
    }
}