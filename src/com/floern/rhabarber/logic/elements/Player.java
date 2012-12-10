package com.floern.rhabarber.logic.elements;

import java.io.InputStream;
import java.util.List;
import java.util.ListIterator;

import javax.microedition.khronos.opengles.GL10;

import com.floern.rhabarber.graphic.primitives.Skeleton;
import com.floern.rhabarber.graphic.primitives.SkeletonKeyframe;
import com.floern.rhabarber.graphic.primitives.Vertexes;
import com.floern.rhabarber.util.FXMath;
import com.floern.rhabarber.util.Vector;

import android.graphics.Color;
import android.util.FloatMath;
import android.util.Log;
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
	private int color;
	
	public int score;

	private final int WINNING_SCORE;
	public FXVector playerGravity;

	// graphics properties (yeah, this may belong somewhere else...
	public Skeleton skeleton;
	public List<SkeletonKeyframe> anim_standing, anim_running_left,
			anim_running_right;

	private static final float ANIM_SPEED_FACTOR =  2; // lower = faster
	private static final float MOVING_THRESHOLD  = 10; // everything lower is
														// considered standing
														// still

	private List<SkeletonKeyframe> active_anim;
	private SkeletonKeyframe active_kf, next_kf;
	private ListIterator<SkeletonKeyframe> kfIterator;
	private float frame_age = 0;

	public Player(FXVector pos, int playerIdx, InputStream skeleton, int color, int winning_score) {
		this(pos.xAsInt(), pos.yAsInt(), playerIdx, skeleton, color, winning_score);
	}

	public Player(int x, int y, int playerIdx, InputStream skeleton, int color, int winning_score) {
		super(x, y, Shape.createRectangle(hitBoxWidth, hitBoxHeight));
		WINNING_SCORE = winning_score;
		
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

	public void animate(float dt) {
		final float aligned_speed = FXMath.FXtoFloat(this.velocityFX().dotFX(this.getAxes()[1]));
		float speed_factor        = FloatMath.sqrt(aligned_speed>=0?aligned_speed:-aligned_speed) / ANIM_SPEED_FACTOR;
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
		gl.glColor4f(Color.red(color), Color.green(color), Color.blue(color), 1);
		
		
		skeleton.rotation = FXMath.FX2toFloat(rotation2FX());
		
		final float length = 6; // vertical displacement, as player.skt is not perfectly centered
		final Vector pos   = new Vector(positionFX().xAsFloat(), positionFX().yAsFloat());
		final Vector delta = new Vector(FloatMath.sin(skeleton.rotation)*length, -FloatMath.cos(skeleton.rotation)*length);
		skeleton.position = pos;
		skeleton.position.add(delta);
		
		skeleton.draw(gl);
		
		// draw point bar (how much point a player has)
		final float P = Math.min(1, Math.max(0, ((float) score) / WINNING_SCORE));
		Log.d("foo", "score: "+Integer.toString(score));
		Log.d("foo", "winning: "+Integer.toString(WINNING_SCORE));
		Log.d("foo", "P = "+Float.toString(P));
		final float L = 20;
		
		Vector left   = pos.plus( (new Vector(-L/2, -15)).rotCCW(skeleton.rotation) );
		Vector right  = pos.plus( (new Vector( L/2, -15)).rotCCW(skeleton.rotation) );
		Vector middle = left.plus( right.minus(left).normalized().times(P*16) ); 
		
		gl.glColor4f(0, 1, 0, 1);
		Vertexes bar = new Vertexes();
		bar.setThickness(10);
		bar.addPoint(left);
		bar.addPoint(middle);
		bar.draw(gl);
		
		gl.glColor4f(1, 0, 0, 1);
		bar = new Vertexes();
		bar.setThickness(10);
		bar.addPoint(middle);
		bar.addPoint(right);
		bar.draw(gl);
	}
}
