package me.bridgar;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.bukkit.command.CommandSender;

public class ProxyServer implements Runnable {
	CommandSender sender;
	ConcurrentLinkedQueue<String> in;
	HashMap<String, Socket> socks;
	Socket server;
	
	public ProxyServer(CommandSender sender, ConcurrentLinkedQueue<String> in) {
		this.sender = sender;
		this.in = in;
		socks = new HashMap<String, Socket>();
	}
	
	public void run() {
		while (true) {
			if(in.isEmpty()) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
				continue;
			}
			
			
			String data;
			String dest;
			int port;
			try {
				data = in.poll();
				
				String line = data.split("\n")[0];
				String url = line.split(" ")[1];
				int httpPos = url.indexOf("://");
				String temp;
				
				if(httpPos == -1) temp = url;
				else temp = url.substring(httpPos+3, url.length());
				
				int portPos = temp.indexOf(":");
				int destPos = temp.indexOf("/");
				if(destPos == -1) destPos = temp.length();
				dest = "";
				port = -1;
				if(portPos == -1 || destPos < portPos) {
					port = 80;
					dest = temp.substring(0, destPos);
				} else {
					port = Integer.parseInt(temp.substring(portPos+1, temp.length()).substring(0,  destPos-portPos-1));
					dest = temp.substring(0, portPos);
				}
			} catch (Exception e) {
				e.printStackTrace();
				continue;
			}
			
			sendToServer(dest, port, data);
		}
	}
	
	private void sendToServer(String dest, int port, String data) {
		String search = dest + ":" + port;
		Socket s;
		if(socks.containsKey(search)) {
			s = socks.get(search);
		} else {
			try {
				s = new Socket((InetAddress) dnsQuery(data)[0], Integer.parseInt((String) dnsQuery(data)[1]));
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
			socks.put(search, s);
		}
		
		InputStream inFromS = null;
		
		try {
			inFromS = s.getInputStream();
			DataOutputStream outToS = new DataOutputStream(s.getOutputStream());
			
			System.out.println("writing bytes: " + data);
			outToS.writeBytes(data);
			outToS.flush();
		} catch (IOException e) {
			socks.remove(search);
			System.out.println("socket missing. Retrying with new socket." );
			sendToServer(dest, port, data);
		}
		
		int bytesRead;
		byte[] reply = new byte[4096];
		try {
			while((bytesRead = inFromS.read(reply)) != -1) {
				System.out.println(reply.toString());
				sender.sendMessage(reply.toString());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public Object[] dnsQuery(String request) throws UnknownHostException {

	    Object[] addressPort = new Object[2];
	    String hostname = request.substring(request.indexOf("host") + 6);
	    hostname = hostname.substring(0, hostname.indexOf("\r"));

	    if (hostname.contains(":")) {

	        hostname = hostname.substring(0, hostname.indexOf(":"));
	        addressPort[1] = hostname.substring(hostname.indexOf(":"));
	    }
	    else
	        addressPort[1] = "80";

	    addressPort[0] = InetAddress.getByName(hostname);

	    return addressPort;
	}

}
