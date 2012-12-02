package com.floern.rhabarber.graphic.primitives;

import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.microedition.khronos.opengles.GL10;

import android.util.Log;

import com.floern.rhabarber.util.Vector;



public class Skeleton extends GLPrimitive {
	
	Bone rootBone = new Bone(0, 0, "root");
	Map<String, Bone> allBones = new HashMap<String, Bone>();
	
	public Vector position = new Vector();
	public float  rotation = 0;
	
	public Skeleton() {
		
	}
	
	public Skeleton(InputStream file, float scale) {
		java.util.Scanner s = new java.util.Scanner(file).useDelimiter("\\A");
		String data = s.hasNext() ? s.next() : "";
		
		String[] lines = data.split("\n",0);
		for(String l: lines) {
			String[] vals = l.split(",", 4);
			
			String name   = vals[0].trim();
			String parent = vals[1].trim();
			float length  = Float.parseFloat(vals[2].trim());
			float angle   = Float.parseFloat(vals[3].trim());
			
			Bone b = new Bone(angle, length*scale, name);
			Log.d("foo", "created new Bone:");
			Log.d("foo", "name:   "+name);
			Log.d("foo", "parent: "+parent);
			Log.d("foo", "angle:  "+Float.toString(angle));
			Log.d("foo", "length: "+Float.toString(length));
			
			if (!parent.equals("NULL")) {
				this.getBone(parent).addBone(b);
			}
			
			this.addBone(b);
		}
	}
	
	public void addBone(Bone b) {
		if(!b.hasParent()) {
			rootBone.addBone(b);
		}
		
		allBones.put(b.getName(), b);
	}
	
	public Bone getBone(String name) {
		return allBones.get(name);
	}

	@Override
	public void draw(GL10 gl) {
		LinkedList<Vector> vects = new LinkedList<Vector>();
		rootBone.angle = this.rotation;
		rootBone.getAllVectors(vects);
		
		Vertexes verts = new Vertexes(vects, this.position);
		verts.setMode(GL10.GL_LINES);
		verts.setThickness(2);
		verts.draw(gl);
		
		// make nice corners
		//verts.setMode(GL10.GL_POINTS);
		//verts.draw(gl);
	}

}
