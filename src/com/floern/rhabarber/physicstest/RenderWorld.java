package com.floern.rhabarber.physicstest;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import at.emini.physics2D.Body;
import at.emini.physics2D.World;
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

			int radius = b.shape().getBoundingRadiusFX();
			c.drawCircle(b.positionFX().xAsInt(), b.positionFX().yAsInt(),
					radius * 2, paint);

			Log.d("bla", "circle at " + b.positionFX().xAsInt() + ", "
					+ b.positionFX().yAsInt() + ", radius " + radius);
		} else {
			// draw the polygon
			Log.d("bla", "polygon at " + b.positionFX().xAsInt() + ", "
					+ b.positionFX().yAsInt() + ", radius ");
			for (int i = 0; i < positions.length - 1; i++) {
				c.drawLine(positions[i].xAsInt(), positions[i].yAsInt(),
						positions[i + 1].xAsInt(), positions[i + 1].yAsInt(),
						paint);
			}
			// draw the final segment
			c.drawLine(positions[positions.length - 1].xAsInt(),
					positions[positions.length - 1].yAsInt(),
					positions[0].xAsInt(), positions[0].yAsInt(), paint);
		}
	}
}
