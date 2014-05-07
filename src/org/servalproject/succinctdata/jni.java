package org.servalproject.succinctdata;

/*
 * To use these in other classes, you need:
 * 
 *      import org.servalproject.succinctdata.jni;
 * 
 * and then to call, use something like:
 * 
 * 		// XXX Testing JNI
 *      org.servalproject.succinctdata.jni.xml2succinctdata("foo","bar","quux");
 *
 */

public class jni {

	static {
		System.loadLibrary("smac");
	}
	
	public static native byte[] xml2succinct(String xmlforminstance, String recipename, String succinctpath);	
	
}
