package com.panacea.sdk.db;

import java.util.Hashtable;

import android.util.Log;

public class PMPerformance
{
	static Hashtable<String, Long> inProgress = new Hashtable<String,Long>();
	
	static Hashtable<String, Long> results = new Hashtable<String,Long>();
	
	
	public static void start(String name)
	{
		inProgress.put(name,System.currentTimeMillis());
	}
	
	public static void stop(String name)
	{
		long start = inProgress.get(name);
		long result = System.currentTimeMillis() - start;
		
		results.put(name, result);
		
		Log.d("PMPerformance", "results:\n" + results.toString());
	}
	
}
