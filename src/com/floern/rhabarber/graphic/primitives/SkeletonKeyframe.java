package com.floern.rhabarber.graphic.primitives;

import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.floern.rhabarber.util.Interpolation;

public class SkeletonKeyframe {
	
	public Map<Bone, Float> data = new HashMap<Bone, Float>();
	public float duration;
	

	public SkeletonKeyframe(Bone root, float _duration) {
		duration = _duration;
		add_data(root);
		
		assert(duration>0);
	}
	
	public SkeletonKeyframe(Skeleton sk, float _duration) {
		duration = _duration;
		
		for(Bone b: sk.rootBone.children) {
			add_data(b);
		}
		
		assert(duration > 0);
	}
	
	public SkeletonKeyframe(float _duration) {
		duration = _duration;
		
		assert(duration>0);
	}

	public void add_data(Bone root) {
		this.data.put(root, root.angle);
		
		for(Bone b: root.children) {
			add_data(b);
		}
	}
		
	
	public boolean has_bone(Bone b) {
		return this.data.get(b) != null;
	}
	
	public void fetch() {
		for(Map.Entry<Bone, Float> d: data.entrySet()) {
			d.setValue(d.getKey().angle);
		}
	}
	
	public void apply() {
		for(Map.Entry<Bone, Float> d: data.entrySet()) {
			d.getKey().angle = d.getValue();
		}
	}
	
	public void apply_interpolated(float percent_next, SkeletonKeyframe next_kf) {
		assert(percent_next >= 0);
		assert(percent_next <= 1);
		
		for(Map.Entry<Bone, Float> d: data.entrySet()) {
			if(next_kf.has_bone(d.getKey()))
				d.getKey().angle = Interpolation.interpolate_angle(d.getValue(), next_kf.data.get(d.getKey()), percent_next);
		}
	}
	
	public static List<SkeletonKeyframe> loadSKAnimation(Skeleton skeleton, InputStream file) {
		java.util.Scanner s = new java.util.Scanner(file).useDelimiter("\\A");
		String data = s.hasNext() ? s.next() : "";
		
		LinkedList<SkeletonKeyframe> anim = new LinkedList<SkeletonKeyframe>();
		SkeletonKeyframe kf = null;
		
		String[] lines = data.split("\n",0);
		for(String l: lines) {
			if(kf == null || l.substring(0, 8).equals("KEYFRAME")) {
				kf = new SkeletonKeyframe(Float.parseFloat(l.substring(9)));
				anim.add(kf);
			}
			else if(!l.trim().equals("")) {
				String[] vals = l.split(",", 2);
				
				String name   = vals[0].trim();
				float angle   = Float.parseFloat(vals[1].trim());
				
				Bone b = skeleton.getBone(name);
				kf.data.put(b, angle);
			}
		}
		
		assert(!anim.isEmpty());
		return anim;
	}
	
	
}
