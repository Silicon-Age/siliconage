package com.siliconage.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import javax.servlet.http.HttpServletRequest;

public abstract class NetworkUtils {
	private NetworkUtils() {
		throw new UnsupportedOperationException();
	}
	
	public static boolean isRealPublicIpAddress(String addressStr) {
		try {
			InetAddress address = InetAddress.getByName(addressStr);
			return address != null &&
				!address.isSiteLocalAddress() &&
				!address.isAnyLocalAddress() &&
				!address.isLinkLocalAddress() &&
				!address.isLoopbackAddress() &&
				!address.isMulticastAddress();
		} catch (UnknownHostException e) {
			return false;
		}
	}

	public static String getClientIpAddress(HttpServletRequest request) {
		// Cloudflare proxies requests and provides the original IP address as a special header
		String cloudflareSource = request.getHeader("cf-connecting-ip");
		if (isRealPublicIpAddress(cloudflareSource)) {
			return cloudflareSource;
		}

		String forwardedFor = request.getHeader("X-Forwarded-For");
		if (isRealPublicIpAddress(forwardedFor)) {
			return forwardedFor;
		}
		
		String remoteAddress = request.getRemoteAddr();
		if (isRealPublicIpAddress(remoteAddress)) {
			return remoteAddress;
		}
		
		return null;
	}
}
