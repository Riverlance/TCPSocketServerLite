package com.tcpsocketserverlite;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

// Found at:
// https://stackoverflow.com/questions/6064510/how-to-get-ip-address-of-the-device-from-code

public class Utils {
    /**
     * Returns MAC address of the given interface name.
     *
     * @param interfaceName eth0, wlan0 or NULL=use first interface
     * @return mac address or empty string
     */
    public static String getMACAddress(String interfaceName) {
        try {
            List<NetworkInterface> networkInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface networkInterface : networkInterfaces) {
                if (interfaceName != null) {
                    if (!networkInterface.getName().equalsIgnoreCase(interfaceName)) {
                        continue;
                    }
                }

                byte[] hardwareAddress = networkInterface.getHardwareAddress();
                if (hardwareAddress == null)
                    return "";

                StringBuilder stringBuilder = new StringBuilder();
                for (byte byteHardwareAddress : hardwareAddress) {
                    stringBuilder.append(String.format("%02X:", byteHardwareAddress));
                }
                if (stringBuilder.length() > 0) {
                    stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                }
                return stringBuilder.toString();
            }
        } catch (Exception ignored) {
            // Do nothing
        }
        return "";
        /*try {
            // This is so Linux hack
            return loadFileAsString("/sys/class/net/" +interfaceName + "/address").toUpperCase().trim();
        } catch (IOException ex) {
            return null;
        }*/
    }

    /**
     * Get IP address from first non-localhost interface
     *
     * @param useIPv4 true=return ipv4, false=return ipv6
     * @return address or empty string
     */
    public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> networkInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface networkInterface : networkInterfaces) {
                List<InetAddress> inetAddresses = Collections.list(networkInterface.getInetAddresses());
                for (InetAddress inetAddress : inetAddresses) {
                    if (!inetAddress.isLoopbackAddress()) {
                        String hostAddress = inetAddress.getHostAddress();
                        // boolean isIPv4 = InetAddressUtils.isIPv4Address(hostAddress);
                        boolean isIPv4 = hostAddress.indexOf(':') < 0;

                        if (useIPv4) {
                            if (isIPv4) {
                                return hostAddress;
                            }
                        } else {
                            if (!isIPv4) {
                                int delimiter = hostAddress.indexOf('%'); // Drop ip6 zone suffix
                                return delimiter < 0 ? hostAddress.toUpperCase() : hostAddress.substring(0, delimiter).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // Do nothing
        }
        return "";
    }

    public static String getIPAddress() {
        return getIPAddress(true);
    }
}
