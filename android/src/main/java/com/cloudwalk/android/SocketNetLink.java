package com.cloudwalk.android;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

import com.cloudwalk.platform.Log;
import com.cloudwalk.platform.NetLink;

/**
 * {@link NetLink} over a TCP socket, with a thread blocking on readLine.
 *
 * This is the transport half of what used to be XCNet. The protocol half
 * stayed behind in the engine, so a browser can put a WebSocket here instead
 * and talk to the same server.
 */
public final class SocketNetLink implements NetLink, Runnable {

	private final Socket socket;
	private final NetLink.Listener listener;
	private PrintWriter out;
	private Thread thread;
	private volatile boolean closed = false;

	public SocketNetLink(String hostPort, NetLink.Listener listener) throws IOException {
		int i = hostPort.indexOf(':');
		String host = hostPort.substring(0, i);
		int port = Integer.parseInt(hostPort.substring(i + 1).trim());
		this.listener = listener;
		this.socket = new Socket(host, port);
		this.socket.setSoTimeout(0);
	}

	public void start() {
		if (thread == null) {
			thread = new Thread(this, "XCNet");
			thread.start();
		}
	}

	public void send(String line) {
		if (out != null) {
			out.println(line);
			out.flush();
		}
	}

	public void run() {
		BufferedReader in = null;
		try {
			out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			String line;
			while (!closed && (line = in.readLine()) != null) {
				listener.onLine(line);
			}
		} catch (IOException e) {
			if (!closed) {
				Log.i("FC XCNET", "Failed I/O: " + e);
			}
		} finally {
			closeQuietly(in);
		}
	}

	public void close() {
		if (closed) {
			return;
		}
		closed = true;
		try {
			if (out != null) {
				out.println("QUIT");
				out.flush();
				out.close();
			}
		} catch (Exception ignored) {
		}
		try {
			socket.close();
		} catch (Exception ignored) {
		}
		if (thread != null) {
			thread.interrupt();
			thread = null;
		}
	}

	private static void closeQuietly(BufferedReader r) {
		try {
			if (r != null) {
				r.close();
			}
		} catch (IOException ignored) {
		}
	}
}
