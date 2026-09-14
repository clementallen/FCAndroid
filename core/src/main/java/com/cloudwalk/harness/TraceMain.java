package com.cloudwalk.harness;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Prints a trace, reading assets off disk. The browser build has its own entry
 * point that feeds {@link Trace} from a prefetched map instead.
 *
 * Usage: TraceMain &lt;assetDir&gt; [task] [pilotType] [frames] [sampleEvery]
 */
public final class TraceMain {

	public static void main(String[] args) throws IOException {
		final File dir = new File(args.length > 0 ? args[0] : "assets");
		String task = args.length > 1 ? args[1] : "t001";
		int pilotType = args.length > 2 ? Integer.parseInt(args[2]) : 0;
		int frames = args.length > 3 ? Integer.parseInt(args[3]) : 600;
		int every = args.length > 4 ? Integer.parseInt(args[4]) : 50;

		AssetSource assets = new AssetSource() {
			public String read(String name) {
				File f = new File(dir, name);
				if (!f.exists()) {
					return null;
				}
				try {
					InputStream in = new FileInputStream(f);
					try {
						byte[] buf = new byte[(int) f.length()];
						int off = 0;
						while (off < buf.length) {
							int n = in.read(buf, off, buf.length - off);
							if (n < 0) {
								break;
							}
							off += n;
						}
						return new String(buf, 0, off, "UTF-8");
					} finally {
						in.close();
					}
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		};

		System.out.print(Trace.run(assets, task, pilotType, frames, every));
	}

	private TraceMain() {
	}
}
