package com.floern.rhabarber.logic.elements;

import java.io.InputStream;
import java.util.List;
import java.util.ListIterator;

import com.floern.rhabarber.graphic.primitives.Skeleton;
import com.floern.rhabarber.graphic.primitives.SkeletonKeyframe;
import com.floern.rhabarber.util.FXMath;

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
	private static final int friction = 10; // 0% to 100%

	// defines playernumber
	private int playerIdx;

	public FXVector playerGravity;

	// graphics properties (yeah, this may belong somewhere else...
	public Skeleton skeleton;
	public List<SkeletonKeyframe> anim_standing, anim_running_left,
			anim_running_right;

	private static float ANIM_SPEED_FACTOR = 5; // higher = faster
	private static final float MOVING_THRESHOLD = 5f; // everything lower is
														// considered standing
														// still

	private List<SkeletonKeyframe> active_anim;
	private SkeletonKeyframe active_kf, next_kf;
	private ListIterator<SkeletonKeyframe> kfIterator;
	private float frame_age = 0;

	public Player(FXVector pos, int playerIdx, InputStream skeleton) {
		this(pos.xFX, pos.yFX, playerIdx, skeleton);
	}

	public Player(int x, int y, int playerIdx, InputStream skeleton) {
		super(x, y, Shape.createRectangle(hitBoxWidth, hitBoxHeight));
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
		ANIM_SPEED_FACTOR = FXMath.FXtoFloat(this.velocityFX().lengthFX()) / 10;

		if (this.velocityFX().xAsFloat() > MOVING_THRESHOLD)
			this.setActiveAnim(this.anim_running_right);
		else if (this.velocityFX().xAsFloat() < -MOVING_THRESHOLD)
			this.setActiveAnim(this.anim_running_left);
		else
			this.setActiveAnim(this.anim_standing);

		if (this.active_anim != null) {
			assert (active_anim.size() > 1);

			frame_age += dt * ANIM_SPEED_FACTOR; // / (touching?1:2); ///< slow
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
}
