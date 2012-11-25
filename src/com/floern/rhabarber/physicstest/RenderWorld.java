package com.floern.rhabarber.physicstest;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import at.emini.physics2D.Body;
import at.emini.physics2D.World;
import at.emini.physics2D.util.FXUtil;
import at.emini.physics2D.util.FXVector;

public class RenderWorld extends World {

	public RenderWorld() {
		super();
	}

	public void draw(Canvas c) {
		int bodyCount = getBodyCount();
		Body[] bodies = getBodies();
		c.drawColor(Color.WHITE);
		// draw bodies
		for (int i = 0; i < bodyCount; i++) {
			drawBody(c, bodies[i]);
		}
	}

	private void drawBody(Canvas c, Body b) {
		// Paint used to draw stuff
		Paint paint = new Paint();
		paint.setColor(Color.BLACK);

		FXVector[] positions = b.getVertices();
		if (positions.length == 1) {
			// draw a circle

			int radius = FXUtil.fromFX(b.shape().getBoundingRadiusFX());
			c.drawCircle(b.positionFX().xAsInt(), b.positionFX().yAsInt(),
					radius, paint);

			Log.d("bla", "circle at " + b.positionFX().xAsInt() + ", "
					+ b.positionFX().yAsInt() + ", radius " + radius);
		} else {
			// draw the polygon
			int L = positions.length;
			for (int i = 0; i < L; i++) {
				c.drawLine(positions[i].xAsInt(),
						   positions[i].yAsInt(),
						   positions[(i + 1) % L].xAsInt(),
						   positions[(i + 1) % L].yAsInt(),
						   paint);
			}
		}
	}
}
