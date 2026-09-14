package com.cloudwalk.client;

import java.util.StringTokenizer;

import com.cloudwalk.platform.Log;
import com.cloudwalk.platform.NetLink;

import com.cloudwalk.framework3d.Tools3d;

/**
 * Speaks the game server's protocol.
 *
 * This class is all protocol and no transport: it turns received lines into
 * changes to the model, and model events into lines to send. Carrying those
 * lines is a {@link NetLink}'s job - a TCP socket on Android, a WebSocket in a
 * browser - which is what lets the same protocol code run on both.
 */
public class XCNet implements NetLink.Listener {

	private NetLink link;
	XCModelViewer xcModelViewer;
	/** Server host, kept only for the status line. */
	public String host = "";

	public XCNet(XCModelViewer xcModelViewer) {
		this.xcModelViewer = xcModelViewer;
		String hp = xcModelViewer.modelEnv.getHostPort();
		if (hp != null) {
			int i = hp.indexOf(":");
			host = i < 0 ? hp : hp.substring(0, i);
		}
	}

	/** Attaches the transport. Until this is called, sends are dropped. */
	public void attach(NetLink link) {
		this.link = link;
	}

	public void send(String s) {
		if (link != null) {
			link.send(s);
		}
		// Log.i("FC XCNET", s);//debug
	}

	/**
	 * Handles one line from the server.
	 *
	 * This was the body of a blocking readLine() loop on its own thread. The
	 * transport calls it now, which is the only change a browser needed: a
	 * WebSocket delivers messages to a callback rather than to a thread that
	 * asks for them.
	 */
	public void onLine(String nextLine) {
		GliderManager gliderManager = xcModelViewer.xcModel.gliderManager;

			String nextLineUpper = nextLine.toUpperCase();

			// nextLine = nextLine.toUpperCase();
			Log.w("FC XCNET", "IN: " + nextLine); // debug

			if (nextLineUpper.indexOf("TIME:") == 0) { // what's the model time
				String tmp = nextLine.substring(nextLine.indexOf(":") + 2, nextLine.length());
				float t = Tools3d.parseFloat(tmp);
				xcModelViewer.clock.synchTime(t);
				// was `continue` in the old readLine loop: this line is done
				return;
			}

			if (nextLine.indexOf("+") == 0) { // server.sendWelcomeMessage
				// String cmdLine = nextLine.substring(4,nextLine.length());

				if (nextLineUpper.indexOf("+HELLO:") == 0) { // what player id am
														// i ?
					String tmp = nextLine.substring(nextLine.indexOf(":") + 2, nextLine.length());
					String[] parts = tmp.split("#");
					int myID = Tools3d.parseInt(parts[0]);
					if (parts.length == 2) {
						String[] taskGlider = parts[1].split(":");
						String task = taskGlider[0];
						int pilotType = Integer.parseInt(taskGlider[1]);
						xcModelViewer.modelEnv.setPilotType(pilotType);
						xcModelViewer.modelEnv.setTask(task);
						xcModelViewer.xcModel.loadTask(xcModelViewer.modelEnv.getTask(), xcModelViewer.modelEnv.getPilotType(),
								xcModelViewer.modelEnv.getTypeNums());
						gliderManager.createUser(pilotType);
						gliderManager.setMyID(myID);
						xcModelViewer.clock.start();
						xcModelViewer.xcModel.startPlay();
					} else {
						xcModelViewer.modelEnv.showDialog("Error",
								"Game server is not compatible with this client.\nPlease update Flight Club on phone that acts as Game Server");
					}

				}

				// first time we have model time
				if (nextLineUpper.indexOf("+TIME:") == 0) { // what's the model
														// time
					String tmp = nextLine.substring(nextLine.indexOf(":") + 2, nextLine.length());
					float t = Tools3d.parseFloat(tmp);
					xcModelViewer.clock.synchTime(t);
					NodeManager x = xcModelViewer.xcModel.task.nodeManager;
					x.loadNodes(0, t);
					xcModelViewer.netTimeFlag = true;
				}

				if (nextLineUpper.indexOf("CONNECTED") > 0) {
					String tmp = nextLine.substring(nextLine.indexOf(":") + 2, nextLine.length());
					int wingType = Tools3d.parseInt(tmp);
					gliderManager.addUser(parseId(nextLine), wingType);
				}

				/**
				 * if (cmdLine.indexOf("LAUNCHED")==0) { int from =
				 * parseId(nextLine); gliderManager.addUser(from);
				 * gliderManager.launchUser(from); }
				 * 
				 * if (cmdLine.indexOf("LANDED")==0) {
				 * gliderManager.addUser(parseId(nextLine)); }
				 * 
				 * if (cmdLine.indexOf("#")==0) { int from =
				 * parseId(nextLine); gliderManager.addUser(from);
				 * gliderManager.launchUser(from);
				 * gliderManager.changeUser(from
				 * ,cmdLine.substring(1,cmdLine.length())); }
				 * 
				 * if (cmdLine.indexOf("UNCONNECTED")==0) {
				 * gliderManager.removeUser(parseId(nextLine)); }
				 */

			} else { // server.sendToAll
				String cmdLine = nextLine.substring(nextLine.indexOf(" ") + 1, nextLine.length()); // todo:
				String cmdLineUpper = cmdLine.toUpperCase();
				int id = parseId2(nextLine);
				if (cmdLineUpper.indexOf("UNCONNECTED") == 0) {
					gliderManager.removeUser(id);
				} else {
					gliderManager.addUserIfNecessary(id, xcModelViewer.modelEnv.getPilotType());
				}

				if (cmdLineUpper.indexOf("CONNECTED") == 0) {
					String tmp = nextLine.substring(nextLine.indexOf(":") + 2, nextLine.length());
					StringTokenizer st = new StringTokenizer(tmp, ",");
					String s = st.nextToken();
					int wingType = Tools3d.parseInt(s);
					gliderManager.addUserIfNecessary(id, wingType);
					try {
						String playerName = st.nextToken();
						int playerColor = Integer.parseInt(st.nextToken());
						gliderManager.changeNetGlider(id, wingType, playerColor);
						gliderManager.nameNetUser(id, playerName);
					} catch (Exception e) {
						Log.e("FC XCNET", e.getMessage(), e);
					}
				} else if (cmdLineUpper.indexOf("LAUNCHED") == 0) {
					StringTokenizer st = new StringTokenizer(cmdLine, ":");
					st.nextToken();
					String wingTypeString = st.nextToken();
					int wingType = Tools3d.parseInt(wingTypeString.trim());
					String colorString = st.nextToken();
					int color = Tools3d.parseInt(colorString);
					gliderManager.changeNetGlider(id, wingType, color);
					try { //for older clients
						String playerName=st.nextToken();
						gliderManager.nameNetUser(id, playerName);
					} catch (Exception e) {
					}
					gliderManager.launchNetUser(id);
				} else if (cmdLineUpper.indexOf("LANDED") == 0) {
					gliderManager.landNetUser(id);
				} else if (cmdLine.indexOf("#") == 0) {
					gliderManager.changeUser(id, cmdLine.substring(1, cmdLine.length()));
				} 

			}
	}

	/**
	 * Returns the glider id which appears before '>' in messages from the
	 * server. We ignore the leading '+' (which appears in in welcome messages).
	 * 
	 * +10> CONNECTED
	 */
	private int parseId(String msg) {
		return Tools3d.parseInt(msg.substring(1, msg.indexOf(">")));
	}

	/**
	 * Returns the glider id which appears before '>' in messages from the
	 * server.
	 * 
	 * 10> CONNECTED
	 */
	private int parseId2(String msg) {
		return Tools3d.parseInt(msg.substring(0, msg.indexOf(">")));
	}

	void destroyMe() {
		if (link != null) {
			Log.i("FC XCNET", "Closing link !");
			link.close();
			link = null;
		}
	}
}
