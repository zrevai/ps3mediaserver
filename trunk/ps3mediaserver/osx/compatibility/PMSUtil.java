package net.pms.util;

import java.lang.reflect.Array;
import java.net.NetworkInterface;
import java.net.SocketException;

import net.pms.newgui.LooksFrame;

public class PMSUtil {
	
	@SuppressWarnings("unchecked")
	public static <T> T[] copyOf(T[] original, int newLength) {
		Class newType = original.getClass();
		 T[] copy = ((Object)newType == (Object)Object[].class)
         ? (T[]) new Object[newLength]
         : (T[]) Array.newInstance(newType.getComponentType(), newLength);
     System.arraycopy(original, 0, copy, 0,
                      Math.min(original.length, newLength));
     return copy;
	}
	
	public static boolean isNetworkInterfaceLoopback(NetworkInterface ni) throws SocketException {
		return false;
	}
	
	public static void browseURI(String uri) {
		
	}
	
	public static void addSystemTray(final LooksFrame frame) {
		
	}
	
	public static byte [] getHardwareAddress(NetworkInterface ni) throws SocketException {
		return null;
	}

}
