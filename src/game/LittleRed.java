package game;
import java.util.Random;
import jgame.*;
import jgame.platform.*;

/*
 * LittleRed is the class that I wrote for CS308 Project 1.
 * In this game, the character that the player plays is Little Red
 * Riding Hood, on her way to Grandmother's house. 
 * 
 * In Level 1, the player must guide Little Red around the flowers
 * that will be randomly generated, and will float down the screen.
 * (Little Red cannot shoot the flowers.)
 * 
 * In Level 2, the player is then confronted by the wolf, armed with
 * a hunter's rifle, possibly coming from the direction of Grandmother's
 * house after a rather satisfying lunch. The object of Level 2 is to 
 * win in a shoot-out with the wolf. It turns out Little Red has been
 * armed with a crossbow pistol this entire time (hence the game title).
 * 
 * Throughout both levels, Little Red has 4 lives (not replenished 
 * at level advancement). Little Red can move up, down, left and right,
 * while the flowers flutter down the screen in spirals, and the wolf
 * moves from side to side at the top of the screen as he shoots bullets
 * at a randomized rate. Little Red's remaining lives are shown at the 
 * top right corner of the screen, represented by baskets.
 *
 * The cheat codes are documented in a file titled "cheatcodes.txt",
 * which are in the general game folder where the README.md, LICENSE,
 * etc. files are located. 
 */

public class LittleRed extends StdGame {
	
	private String[] flowers = {"orange", "pink", "red", "rose", "yellow"};
	private int myMaxLevelTime = 1300;
	private int myFlowerFrequencyInterval = 58;
	private int myBulletFrequency = 50;
	private int myGameLevel;
	

	public static void main(String[]args) {
		new LittleRed(parseSizeArgs(args,0));
	}
	public LittleRed() { 
		initEngineApplet(); 
	}
	public LittleRed(JGPoint size) { 
		initEngine(size.x,size.y); 
	}
	public void initCanvas() { 
		setCanvasSettings(60,45,8,8,null,null,null);
		setTitle();
	}
	public void setTitle() {
		title = "Little Red Crossbow Slinger";
	}
	
	public void initGame() {
		resetGameLevelBossLives();
		defineMedia("littlered.tbl");
		setSpeedRateBGImageHighscores();
		startgame_ingame=true;
	}
	
	public void setSpeedRateBGImageHighscores() {
		setFrameRate(20,1);
		setGameSpeed(2.0);
		setBGImage("bgimg");
		setHighscores(10,new Highscore(0,"nobody"),15);
	}
	
	public void initNewLife() {
		removeObjects(null,0);
		new Red(pfWidth()/2,pfHeight()-55,5);
	}
	public void startGameOver() { 
		resetGameLevelBossLives();
		removeObjects(null,0); 
	}
	public void paintFrameTitle() {
		drawImage(viewHeight(),viewWidth(),"bgimg",false);
		super.paintFrameTitle();
	}
	
	public void doFrameInGame() {
		moveObjects();
		checkCollision(2,1); // flowers/wolf hit player
		checkCollision(5,1); // bullets hit player
		checkCollision(4,2); // arrows hit enemies
		if(myGameLevel == 1) {
			flowerFrequency();
			whenLevelDone();
		} else {
			if(countObjects("enemy",0)==0 & myGameLevel==2) 
				new Wolf();
		}
	}
	
	public void flowerFrequency() {
		Random r = new Random();
		if(checkTime(0, myMaxLevelTime, myFlowerFrequencyInterval)) {
			int index = r.nextInt(flowers.length);
			new Flower(index);
		}
	}
	
	public void whenLevelDone() {
		if(gametime >= myMaxLevelTime || getKey(key_skip)) {
			myGameLevel++;
			initNewLife();
			levelDone();
		}
	}
	
	public void incrementLevel() {
		if(level < 7)
			level++;
		score += 50;
		stage++;
	}
	
	JGFont scoring_font = new JGFont("Cambria",0,6);
	
	public class Flower extends JGObject {
		double timer = 0;
		public Flower(int index) {
			super("enemy",true,random(32,pfWidth()-40),-25,
					2, flowers[index],
					random(-1,1), (1.0+level/2.0), -2 );
		}
		//motion is circular, while the direction of the motion of the entire 
		//flower is determined by the method above
		public void move() {
			timer += gamespeed;
			x += Math.sin(0.1*timer);
			y += Math.cos(0.1*timer);
			if (y>pfHeight()) 
				y = -8;
		}
		public void hit(JGObject o) {
			remove();
			o.remove();
			score += 5;
		}
	}

	public class Wolf extends JGObject {
		double timer = 0;
		//starting position is random, somewhere along the top left of the screen
		public Wolf() {
			super("enemy",true,random(pfWidth()/5,pfWidth()/2),10,
					2, "wolf_shoot",0,0,-2);
		}
		//oscillate from side to side
		public void move() {
			timer += gamespeed;
			x += Math.sin(0.0095*timer);
			Random r = new Random();
			bulletFreq(r);
		}
		public void hit(JGObject o) {
			o.remove();
			bossLives--;
			if(bossLives <= 0) {
				remove();
				gameOver();
				resetGameLevelBossLives();
			}
		}
		//bullets shooting at random intervals
		public void bulletFreq(Random r) {
			int wolfInterval = r.nextInt(myBulletFrequency);
			if(checkTime(0, Integer.MAX_VALUE, wolfInterval)) {
				new JGObject("bullet", true, x+12, y+25, 5, "bullet",
						-0.05*Math.sin(0.02*timer), 28, -2);
				playAudio("sbullet");
				wolfInterval = r.nextInt(25);
			}
		}
	}

	public class Red extends JGObject {
		double spd;
		public Red(double x,double y, double speed) {
			super("player", true, x, y, 1, (myGameLevel==1) ? "red_run":"red_ready", 
					0, 0, speed, speed, -1);
			spd = speed;
		}
		public void move() {
			setDir(0,0);
			if (getKey(key_left)  && x > xspeed) 				xdir=-1;
			if (getKey(key_right) && x < pfWidth()-32-yspeed) 	xdir=1;
			if (getKey(key_up)    && y > yspeed) 				ydir=-1;
			if (getKey(key_down)  && y < pfHeight()-32-yspeed)	ydir=1;
			if (getKey(key_fire) && countObjects("arrow",0) < 4) {
				if(myGameLevel > 1) {
					//if the fire key is pressed, and we are at level 2 of the game:
					new JGObject("arrow",true,x+16,y-21,4,"arrow",0.0,-35.0,-2);
					playAudio("sarrow");
				}
			}
		}
		public void hit(JGObject obj) {
			if((and(obj.colid,2) || and(obj.colid,5)) && invincible == false) {
					lifeLost();
			}
		}
	}
	public void resetGameLevelBossLives() {
		myGameLevel = 1;
		bossLives = 4;
	}
}


