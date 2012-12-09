package com.floern.rhabarber.util;

import java.util.HashMap;
import java.util.Scanner;

import android.util.Log;

/* parses a string in the following format:
 *
 *  foo=1
 *  bar=2
 *  baz=hello
 *  asdf = zxcvxcv
 *
 * This parser also converts everything to lower case for case-insensitive comparisons.
 *
 */
public class KeyValueParser {
	
	public static HashMap<String, String> parse(String text) {
		
		HashMap<String, String> result = new HashMap<String, String>();
		
		Scanner linescanner = new Scanner(text);
		linescanner.useDelimiter("\n");
		
		//Log.d("foo", "parsing:");
		//Log.d("foo", text);
		//Log.d("foo", "////////////////////////////////// result:");
		
		while(linescanner.hasNext()) {
			String line = linescanner.next();
			Scanner entryscanner = new Scanner(line);
			entryscanner.useDelimiter("=");
			
			//Log.d("foo", "line ["+line+"]");
			
			if(entryscanner.hasNext()) {
				String key   = entryscanner.next();
				
				if(entryscanner.hasNext()) {
					String value = entryscanner.next();
					
					//Log.d("foo", "["+key+"]=["+value+"]");
					
					result.put(key.trim().toLowerCase(), value.trim().toLowerCase());
				}
			}
		}
		
		return result;
	}

}
