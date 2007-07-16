/**
 * This file is a part of Angry IP Scanner source code,
 * see http://www.azib.net/ for more information.
 * Licensed under GPLv2.
 */

package net.azib.ipscan.fetchers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.azib.ipscan.config.GlobalConfig;
import net.azib.ipscan.config.LoggerFactory;
import net.azib.ipscan.core.ScanningSubject;

/**
 * PortTextFetcher - generic configurable fetcher to read some particular information from a port.
 *
 * @author Anton Keks
 */
public abstract class PortTextFetcher implements Fetcher {
	private static final Logger LOG = LoggerFactory.getLogger();
	
	private GlobalConfig globalConfig;

	private int port;
	private String textToSend;
	private Pattern matchingRegexp;
	
	public PortTextFetcher(GlobalConfig globalConfig, int port, String textToSend, String matchingRegexp) {
		this.globalConfig = globalConfig;
		this.port = port;
		this.textToSend = textToSend;
		this.matchingRegexp = Pattern.compile(matchingRegexp);
	}

	public void cleanup() {
	}

	public void init() {
	}

	public Object scan(ScanningSubject subject) {
		Socket socket = new Socket();
		try {
			// TODO: support multiple ports and check them sequentially
			// TODO: use adapted port timeout if it is configured to do so			
			socket.connect(new InetSocketAddress(subject.getIPAddress(), port), globalConfig.portTimeout);
			socket.setTcpNoDelay(true);
			socket.setSoTimeout(globalConfig.portTimeout);
			socket.setSoLinger(true, 0);
			
			socket.getOutputStream().write(textToSend.getBytes());
			
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			String line;
			while ((line = in.readLine()) != null) {
				Matcher matcher = matchingRegexp.matcher(line);
				if (matcher.matches()) {
					// mark that additional info is available
					subject.setResultType(ScanningSubject.RESULT_TYPE_ADDITIONAL_INFO);
					// return the required contents
					return matcher.group(1);
				}
			}
		}
		catch (ConnectException e) {
			// no connection
		}
		catch (SocketTimeoutException e) {
			// no information
		}
		catch (IOException e) {
			LOG.log(Level.FINE, subject.getIPAddress().toString(), e);
		}
		finally {
			try {
				socket.close();
			}
			catch (IOException e) {}
		}
		return null;
	}

}