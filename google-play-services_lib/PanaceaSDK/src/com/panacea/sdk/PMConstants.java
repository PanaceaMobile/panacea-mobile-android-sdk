package com.panacea.sdk;

import com.panacea.sdk.web.PMWebServiceController;

/**
 * Various constants, mostly pertaining to WebService calls made by
 * {@link PMWebServiceController}
 * 
 * @author Cobi Interactive
 */
public class PMConstants
{
//	public static final String PANACEA_API_URL = "http://console-qa.panaceamobile.com:29011/";
	public static final String PANACEA_API_URL = "https://android.panaceamobile.com/";
	
	/* This flag enables certain debug and developer options */
	public static final boolean DEBUG = false;

	public static class Status
	{
		public static final int OK = 0;
		public static final int NO_ACTION_SPECIFIED = -1;
		public static final int NO_SUCH_ACTION = -2;
		public static final int MISSING_PARAMETER = -4;
		public static final int AUTHENTICAITON_FAILED = -8;
		public static final int INTERNAL_ERROR = -16;
		public static final int INVALID_RECORD = -128;
		public static final int COULD_NOT_SAVE = -256;
		public static final int DUPLICATE_RECORD = -512;
		public static final int GENERIC_ERROR = -1024;
	}

	public static class PushChannel
	{
		public static final String APPLE = "1";
		public static final String ANDROID = "2";
		public static final String BLACKBERRY = "4";
		public static final String NOKIA = "8";
	}

	public static class Exceptions
	{
		public static final String KEY_NOT_FOUND = "PMDictionary does not contain a value for key";
	}
}
