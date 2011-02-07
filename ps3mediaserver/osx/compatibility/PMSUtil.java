package net.pms.util;

import java.lang.reflect.Array;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import net.pms.newgui.LooksFrame;
import net.pms.PMS;
import net.pms.io.OutputTextConsumer;

import com.apple.eawt.*;

public class PMSUtil {

    private static LooksFrame frameRef;
	
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


    public static void addSystemTray(final LooksFrame frame) {
        frameRef = frame;
	Application.getApplication().addApplicationListener(new com.apple.eawt.ApplicationAdapter() {
														
  	    public void handleReOpenApplication(ApplicationEvent e) {
	        if(!frameRef.isVisible()) frameRef.setVisible(true);
	    }
															
	    public void handleQuit(ApplicationEvent e) {
	        System.exit(0);
	    }
															
	});
    }

    /**
     * On Mac OS, open the given URI with the "open" command.  This will open HTTP URLs in the default browser.
     *
     * @param uri URI string to open externally.
     */
    public static void browseURI(String uri) {
        try {
            Runtime.getRuntime().exec(new String[]{"open", uri});
        } catch (IOException e) {
            PMS.error("Unable to open the given URI: " + uri, e);
        }
    }

    /**
     * On Mac OS, fetch the hardware address from the command line tool "ifconfig".
     *
     * @param ni Interface to fetch the mac address for
     * @return the mac address as bytes, or null if it couldn't be fetched.
     * @throws SocketException This won't happen on Mac OS, since the NetworkInterface is only used to get a name.
     */
    public static byte[] getHardwareAddress(NetworkInterface ni) throws SocketException {
        byte[] aHardwareAddress = null;
        try {
            Process aProc = Runtime.getRuntime().exec(new String[]{"ifconfig", ni.getName(), "ether"});
            aProc.waitFor();
            OutputTextConsumer aConsumer = new OutputTextConsumer(aProc.getInputStream(), false);
            aConsumer.run();
            List<String> aLines = aConsumer.getResults();
            String aMacStr = null;
            Pattern aMacPattern = Pattern.compile("\\s*ether\\s*([a-d0-9]{2}:[a-d0-9]{2}:[a-d0-9]{2}:[a-d0-9]{2}:[a-d0-9]{2}:[a-d0-9]{2})");
            for (String aLine : aLines) {
                Matcher aMacMatcher = aMacPattern.matcher(aLine);
                if (aMacMatcher.find()) {
                    aMacStr = aMacMatcher.group(1);
                    break;
                }
            }
            if (aMacStr != null) {
                String[] aComps = aMacStr.split(":");
                aHardwareAddress = new byte[aComps.length];
                for (int i = 0; i < aComps.length; i++) {
                    String aComp = aComps[i];
                    aHardwareAddress[i] = (byte) Short.valueOf(aComp, 16).shortValue();
                }
            }

        } catch (IOException e) {
            PMS.error("Failed to execute ifconfig", e);
        } catch (InterruptedException e) {
            PMS.error("Interrupted while waiting for ifconfig", e);
            Thread.interrupted(); // XXX work around a Java bug - see ProcessUtil.waitFor()
        }
        return aHardwareAddress;
     }

}
