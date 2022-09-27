import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.omg.CORBA.NameValuePair;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class GPSTracker {

	private int localPortNo;
	private ServerSocket ss;
	private String domain;

	private static ArrayList<GPSDevice> devices = new ArrayList();
	
	public GPSTracker(String localPort) throws Exception {
		this.localPortNo = Integer.parseInt(localPort);
		ss = new ServerSocket(localPortNo);

		System.out.println("TCP Server Created");
		logMessage("TCP Server Created", "Info");

	}
	
	public GPSTracker(String localPort,String domain) throws Exception {
		this.localPortNo = Integer.parseInt(localPort);
		ss = new ServerSocket(localPortNo);
		this.domain=domain;

		System.out.println("TCP Server Created");
		logMessage("TCP Server Created", "Info");

	}


	public static void listenAndSend(int portNo) {
		HttpServer server;
		try {
			// server = HttpServer.create(new InetSocketAddress(portNo),
			// 0);192.168.1.10
			server = HttpServer.create(new InetSocketAddress(portNo), 0);
			server.createContext("/comando", new MyHandler());
			server.setExecutor(null); // creates a default executor
			server.start();

			System.out.println("HTTP Server Created");
			logMessage("HTTP Server Created", "Info");
		} catch (IOException e) {
			System.out.println("listenAndSend(int portNo) " + e.getMessage());
			logMessage("listenAndSend(int portNo) " + e.getMessage(), "Error");
		}

	}

	static class MyHandler implements HttpHandler {
		public void handle(HttpExchange t) throws IOException {
			try {

				if (t.getRequestMethod().equalsIgnoreCase("Get")) {

					String pair[] = t.getRequestURI().getQuery().split("=");
					if (pair.length == 2 && pair[0].equals("cmd")) {
						String command = "Send Command: " + pair[1];
						int i = 0;
						System.out.println("No of sockets we have "
								+ devices.size());
						logMessage("No of sockets we have " + devices.size(),
								"Info");

						while (i < devices.size()) {
							try {
								System.out.println("Device Id of Socket No "
										+ i
										+ " is "
										+ devices.get(i).deviceID
										+ " Socket "
										+ devices.get(i).socket
												.getRemoteSocketAddress());
								logMessage(
										"Device Id of Socket No "
												+ i
												+ " is "
												+ devices.get(i).deviceID
												+ " Socket "
												+ devices.get(i).socket
														.getRemoteSocketAddress(),
										"Info");
								if (pair[1].indexOf(devices.get(i).deviceID) != -1) {
									// Device is Found
									try {
										System.out
												.println("Writing to the device : "
														+ devices.get(i).deviceID);

										logMessage("Writing to the device : "
												+ devices.get(i).deviceID,
												"Info1");

										devices.get(i).socket.getOutputStream()
												.write(pair[1].getBytes());
										System.out
												.println("Written successfully!");
										logMessage("Written successfully!",
												"Info1");
										
										break;

									} catch (Exception e) {
										System.out
												.println("handle(HttpExchange t) "
														+ e.getMessage());
										logMessage("handle(HttpExchange t) "
												+ e.getMessage(), "Error");
										// Removes because it is failed
										devices.remove(i);
									}
									
								} else if (devices.get(i).deviceID
										.equals("Error")) {
									devices.get(i).socket.close();
									devices.remove(i);
									--i;
								}

							} catch (Exception e) {
								System.out.println("handle(HttpExchange t) "
										+ e.getMessage());
								logMessage(
										"handle(HttpExchange t) "
												+ e.getMessage(), "Error");
							}
							++i;
						}
						System.out.println(command);
						t.sendResponseHeaders(200, command.length());
						OutputStream os = t.getResponseBody();
						os.write(command.getBytes());
						os.close();
					}

				}
			} catch (Exception e) {
				System.out.println("handle(HttpExchange t) " + e.getMessage());
				logMessage("handle(HttpExchange t) " + e.getMessage(), "Error");

			}

		}
	}

	public static String sendGET(String targetURL) {

		StringBuffer response = new StringBuffer();
		try {

			// URL url = new URL(URLDecoder.decode(targetURL,
			// "UTF-8").replaceAll(" ", "%20"));
			URL url = new URL(targetURL);

			// URL url = new URL(URLEncoder.encode(targetURL, "UTF-8"));

			HttpURLConnection con = (HttpURLConnection) url.openConnection();

			// optional default is GET
			con.setRequestMethod("GET");

			// add request header
			// con.setRequestProperty("User-Agent", "");

			// int responseCode = con.getResponseCode();

			// System.out.println("\nSending 'GET' request to URL : "
			// + URLDecoder.decode(targetURL, "UTF-8"));
			// System.out.println("Response Code : " + responseCode);

			BufferedReader in = new BufferedReader(new InputStreamReader(
					con.getInputStream(), Charset.forName("UTF-8")));

			String inputLine;

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();

			// print result
			System.out.println("Response value" + response.toString());
			logMessage("Response value" + response.toString(), "Info1");

		} catch (Exception e) {
			System.out.println("Error " + e.getMessage());
			logMessage(e.getMessage(), "Error");

		}

		return response.toString();
	}

	public void lookForIncomingData() throws Exception {

		while (true) {
			Socket s = ss.accept();
			s.setSoTimeout(60000 * 15);
			System.out.println("Client Connected");
			logMessage("Client Connected", "Info");
			GPSDevice gDev = new GPSDevice();
			IncomingData id = new IncomingData(s, gDev,domain);
			id.start();
			System.out.println("Waiting for client Data");
			logMessage("Waiting for client Data", "Info");

			// while (gDev.deviceID.equals(null) || gDev.socket == null) {
			// Waiting till the socket and device id is retrieved
			// }
			// System.out
			// .println("Socket and device id :"
			// + gDev.socket.getLocalSocketAddress() + " "
			// + gDev.deviceID);
			devices.add(gDev);

		}

	}

	public static void logMessage(String message, String type) {

		// Current Date and Time
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Calendar cal = Calendar.getInstance();

		try {
			String pathName = GPSTracker.class.getProtectionDomain()
					.getCodeSource().getLocation().getPath();
			pathName = pathName.substring(0, pathName.lastIndexOf("/"));

			File file = new File(pathName + "/Log");

			if (Files.notExists(file.toPath())) {
				try {
					Files.createDirectory(file.toPath());
				} catch (IOException e) {
					System.out.println(e.getLocalizedMessage());
				}
			}

			if (type.equalsIgnoreCase("Error")) {
				// System.out.println("inside error");

				PrintWriter writer = null;
				file = new File(pathName + "/Log/Error.txt");
				try {
					if (!file.exists())
						file.createNewFile();
					writer = new PrintWriter(new FileOutputStream(pathName
							+ "/Log/Error.txt", true));
					writer.println(dateFormat.format(cal.getTime()) + " : "
							+ message);
					// writer.println(message);
				} catch (IOException ex) {
					System.out
							.println("Error logMessage(String message,String type) : "
									+ ex.getMessage());
				} finally {
					if (writer != null) {
						writer.flush();
						writer.close();
					}
				}
			} else if (type.equalsIgnoreCase("Info1")) { //			

				PrintWriter writer = null;
				file = new File(pathName + "/Log/Info.txt");
				try {
					if (!file.exists())
						file.createNewFile();
					writer = new PrintWriter(new FileOutputStream(pathName
							+ "/Log/Info.txt", true)); // /var/www/gps.lokaliza.mx/socket
					writer.println(dateFormat.format(cal.getTime()) + " : "
							+ message);
					// writer.println(message);
				} catch (IOException ex) {
					System.out
							.println("Error logMessage(String message,String type) : "
									+ ex.getMessage());
				} finally {
					if (writer != null) {
						writer.flush();
						writer.close();
					}
				}

			}

		} catch (Exception e) {
			System.out
					.println("Error logMessage(String message,String type) : "
							+ e.getMessage());
		}
	}

	public static void main(String[] arg) {

		try {
			System.out.print("Please Enter Port Number for TCP Listening : ");
			String tcpPort = new BufferedReader(
					new InputStreamReader(System.in)).readLine();

			System.out.print("Please Enter Port Number for Http listening : ");
			int httpPort = Integer.parseInt(new BufferedReader(
					new InputStreamReader(System.in)).readLine());
			
			System.out.print("Please Domain Name : ");
			String domName = new BufferedReader(
					new InputStreamReader(System.in)).readLine();
			
			GPSTracker c = new GPSTracker(tcpPort,domName); // http listening
			GPSTracker.listenAndSend(httpPort); // tcp listening
			c.lookForIncomingData();

			// http://gps.lokaliza.mx?hdr=SA200STT&cell=1ef136&swver=299&lat=%2032.652169&lon=-115.465926&spd=000.031&devid=841296&crs=000.00&satt=9&fix=1&dist=67&pwrvolt=11.03&io=010010&mode=1&msgnum=0257&date=20150514&time=22:29:51&altId=0&emgId=0
			// http://gps.lokaliza.mx/?hdr=SA200STT&cell=1ef136&swver=299&lat=%2032.652169&lon=-115.465926&spd=000.031&devid=841296&crs=000.00&satt=9&fix=1&dist=67&pwrvolt=11.03&io=010010&mode=1&msgnum=0257&date=20150514&time=22:29:51&altId=0&emgId=0
			// GPSTracker.sendGET("http://gps.lokaliza.mx/?hdr=SA200STT&cell=1a801&swver=299&lat=19.455256&lon=-99.18862&spd=0.072&devid=841279&crs=0.0&satt=12&fix=1&dist=7604033&pwrvolt=12.86&io=001000&mode=1&msgnum=2005&date=20150526&time=18:37:17&altId=0&emgId=0&evt=0");

		} catch (Exception e) {
			System.out.println("main() " + e);
			logMessage("main() " + e.getMessage(), "Error");
		}

	}

}

class IncomingData extends Thread {
	private Socket soc = null;
	GPSDevice gDev;
	
	private String domain;

	public IncomingData(Socket soc, GPSDevice gDev,String domain) throws Exception {
		this.soc = soc;
		this.gDev = gDev;
		this.domain=domain;
		gDev.deviceID="";
		if(domain.equals("")){
			this.domain="http://gps.lokaliza.mx";
		}
	}

	public IncomingData(Socket s, GPSDevice gDev2) {
		this.soc = soc;
		this.gDev = gDev;
		gDev.deviceID="";
		this.domain="http://gps.lokaliza.mx";
	}

	@Override
	public void run() {
		readIncomingData();
	}

	private void readIncomingData() {
		try {

			gDev.socket = soc;
			while (true) {

				String s = null;

				s = new BufferedReader(new InputStreamReader(
						soc.getInputStream())).readLine();

				System.out.println("String from Socket:"+s);
				GPSTracker.logMessage(s, "Info");
				try {

					StringTokenizer st = new StringTokenizer(s, ";");
					if (st.hasMoreTokens()) {
						String param = "";
						int i = 1;

						while (st.hasMoreTokens()) {
							String p=st.nextToken().trim();
							p=p.charAt(0)=='+'?p.substring(1,p.length()):p;
							param = param + "&param" + i + "=" + p;

							i++;
						}

						param = "param0=" + (i - 1) + param;
						
						//"http://gps.lokaliza.mx/?"
						String sUrl = domain+"/?" + param;
						System.out.println(sUrl);

						if(gDev.deviceID.length()==0){
							gDev.deviceID = GPSTracker.sendGET(sUrl).trim();
						}else{
							GPSTracker.sendGET(sUrl);
						}

					}

				} catch (Exception e) {
					System.out.println("readIncomingData()" + e.getMessage());
					 GPSTracker.logMessage(
					 "readIncomingData() " + e.getMessage(), "Error");
					gDev.deviceID = "Error";
					try {
						soc.close();
					} catch (IOException e1) {

					}

					break;
				}
			}
		} catch (Exception e) {
			System.out.println("readIncomingData()" + e.getMessage());
			 GPSTracker.logMessage("readIncomingData() " + e.getMessage(),
			 "Error");
		} finally {
			try {
				soc.close();
			} catch (IOException e) {
				System.out.println(e.getMessage());
				 GPSTracker.logMessage(
				 "readIncomingData() " + e.getMessage(), "Error");
				GPSTracker.logMessage("readIncomingData() Socket Closing",
						"Info");
				gDev.deviceID = "Error";
				try {
					soc.close();
				} catch (IOException e1) {
					 GPSTracker.logMessage(
					 "readIncomingData() " + e1.getMessage(), "Error");
				}
			}
		}
	}

}
