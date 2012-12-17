package com.floern.rhabarber.logic.elements;

import java.io.InputStream;
import java.util.List;
import java.util.ListIterator;

import javax.microedition.khronos.opengles.GL10;

import com.floern.rhabarber.graphic.primitives.Skeleton;
import com.floern.rhabarber.graphic.primitives.SkeletonKeyframe;
import com.floern.rhabarber.graphic.primitives.Vertexes;
import com.floern.rhabarber.network2.ClientStateAccumulator;
import com.floern.rhabarber.util.FXMath;
import com.floern.rhabarber.util.Vector;

import android.graphics.Color;
import android.util.FloatMath;
import at.emini.physics2D.Shape;
import at.emini.physics2D.util.FXVector;

public class Player extends MovableElement {
	/*
	 * physical properties of players as used by physics engine
	 */
	private static final int hitBoxWidth = 15;
	private static final int hitBoxHeight = 30;
	private static final int mass = 50;
	private static final int elasticity = 30; // "bouncyness", 0% to 100% energy
												// conserved
	private static final int friction = 90; // 0% to 100%

	// defines playernumber
	private int playerIdx;
	public int color;
	
	public int score;

	public final int WINNING_SCORE;
	public FXVector playerGravity;
	
	public boolean is_local_player = false;

	// graphics properties (yeah, this may belong somewhere else...
	public Skeleton skeleton;
	public List<SkeletonKeyframe> anim_standing, anim_running_left,
			anim_running_right;

	private static final float ANIM_SPEED_FACTOR =  2; // lower = faster
	private static final float MOVING_THRESHOLD  = 10; // everything lower is
														// considered standing
														// still
	private static final float MAX_WALKING_SPEED = 30; // limit acceleration due to user input

	private List<SkeletonKeyframe> active_anim;
	private SkeletonKeyframe active_kf, next_kf;
	private ListIterator<SkeletonKeyframe> kfIterator;
	private float frame_age = 0;
	private float aligned_speed = 0;

	public Player(FXVector pos, int playerIdx, InputStream skeleton, int color, int winning_score) {
		this(pos.xAsInt(), pos.yAsInt(), playerIdx, skeleton, color, winning_score);
	}

	public Player(int x, int y, int playerIdx, InputStream skeleton, int color, int winning_score) {
		super(x, y, Shape.createRectangle(hitBoxWidth, hitBoxHeight));
		WINNING_SCORE = winning_score;
		
		if(playerIdx < 0) {
			 // well, not exactly an 'array out of bound' error, but playerIdx is used as an array index and should be positive
			throw new ArrayIndexOutOfBoundsException();
		}
		
		this.score = 0;
		this.color = color;
		this.shape().setElasticity(elasticity);
		this.shape().setMass(mass);
		this.shape().setFriction(friction);
		this.playerIdx = playerIdx;
		this.playerGravity = new FXVector(0, 10);
		this.setGravityAffected(false);
		this.setRotatable(false);
		this.skeleton = new Skeleton(skeleton, 0.04f);
	}

	public int getIdx() {
		return playerIdx;
	}

	public void setActiveAnim(List<SkeletonKeyframe> a) {
		assert (a.size() > 1);

		if (a != active_anim) {
			active_anim = a;
			kfIterator = a.listIterator(0);
			active_kf = kfIterator.next();
			next_kf = kfIterator.next();
			frame_age = 0;
		}
	}
	
	// update internal state, should only be called in server
	public void update() {
		this.aligned_speed = FXMath.FXtoFloat(this.velocityFX().dotFX(this.getAxes()[1]));
	}
	
	public void setAlignedSpeed(float v) {
		this.aligned_speed = v;
	}
	
	public float getAlignedSpeed() {
		return this.aligned_speed;
	}

	public void animate(float dt) {
		float speed_factor         = FloatMath.sqrt(aligned_speed>=0?aligned_speed:-aligned_speed) / ANIM_SPEED_FACTOR;
		//Log.d("foo", Float.toString(aligned_speed) + "  //  " + Float.toString(speed_factor));
		
		if (aligned_speed > MOVING_THRESHOLD)
			this.setActiveAnim(this.anim_running_right);
		else if (aligned_speed < -MOVING_THRESHOLD)
			this.setActiveAnim(this.anim_running_left);
		else {
			this.setActiveAnim(this.anim_standing);
			speed_factor = 1 / ANIM_SPEED_FACTOR;
		}

		if (this.active_anim != null) {
			assert (active_anim.size() > 1);

			frame_age += dt * speed_factor; // / (touching?1:2); ///< slow
													// in midair
			while (frame_age >= active_kf.duration) {
				frame_age -= active_kf.duration;

				if (!kfIterator.hasNext())
					kfIterator = active_anim.listIterator(0);

				active_kf = next_kf;
				next_kf = kfIterator.next();
			}

			active_kf.apply_interpolated(frame_age / active_kf.duration,
					next_kf);
		}
	}
	
	public void walk(ClientStateAccumulator.UserInputWalk direction) {
		FXVector dir = new FXVector(getAxes()[1]);
		if (   direction == ClientStateAccumulator.UserInputWalk.LEFT
			&& aligned_speed >= -MAX_WALKING_SPEED) {
			dir.mult(-1);
			applyAcceleration(dir, FXMath.floatToFX(2f));
		} else if (direction == ClientStateAccumulator.UserInputWalk.RIGHT
				&& aligned_speed <= MAX_WALKING_SPEED) 
			applyAcceleration(dir, FXMath.floatToFX(2f));
	}

	public void setRotationFromGravity() {
		if (playerGravity.lengthFX() != 0) {
			double angle = ((Math.acos((double) playerGravity.yFX
					/ (double) playerGravity.lengthFX())));
			if (playerGravity.xFX < 0) {
				this.setRotationDeg((int) Math.toDegrees(angle));
			} else {
				this.setRotationDeg(-(int) Math.toDegrees(angle));
			}
		}
	}

	
	public void draw(GL10 gl) {
		
		skeleton.rotation = FXMath.FX2toFloat(rotation2FX());
		
		final float length = 6; // vertical displacement, as player.skt is not perfectly centered
		final Vector pos   = new Vector(positionFX().xAsFloat(), positionFX().yAsFloat());
		final Vector delta = new Vector(FloatMath.sin(skeleton.rotation)*length, -FloatMath.cos(skeleton.rotation)*length);
		skeleton.position = pos;
		skeleton.position.add(delta);
		skeleton.setThickness(is_local_player?3:0.5f);
		
		gl.glColor4f(	Color.red(color)   / 255f,
						Color.green(color) / 255f,
						Color.blue(color)  / 255f,
						1 );
		
		skeleton.draw(gl);
		
		// draw point bar (how much point a player has)
		final float P = Math.min(0.9999f, Math.max(0.0001f, ((float) score) / WINNING_SCORE));
		final float L = (is_local_player?30:20);
		
		// vector pointing from player center in direction of head (absolute)
		final Vector up = (new Vector(0,-1)).rotCCW(skeleton.rotation);
		
		Vector left   = pos.plus( up.times(is_local_player?25:15)).plus(up.rotCCW().times(L/2));
		Vector right  = pos.plus( up.times(is_local_player?25:15)).plus(up.rotCW ().times(L/2));
		Vector middle = left.plus( right.minus(left).normalized().times(P*L) ); 
		
		gl.glColor4f(0, 1, 0, 1);
		Vertexes bar = new Vertexes();
		if (is_local_player)
		bar.setThickness(is_local_player?8:3);
		bar.addPoint(left);
		bar.addPoint(middle);
		bar.draw(gl);
		
		gl.glColor4f(1, 0, 0, 1);
		bar = new Vertexes();
		bar.setThickness(is_local_player?8:3);
		bar.addPoint(middle);
		bar.addPoint(right);
		bar.draw(gl);
		
		if (is_local_player) {
			// draw a yellow arrow over player
			
			gl.glColor4f(1, 1, 0, 1);
			bar = new Vertexes();
			bar.setThickness(3);
			
			bar.addPoint(left.plus (new Vector(0, 0)));
			bar.addPoint(left.plus(right).times(0.5f).plus(up.times(-10)));
			bar.addPoint(right.plus(new Vector(0, 0)));
			
			bar.draw(gl);
		}
	}
}
