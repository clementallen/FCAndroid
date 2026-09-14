package com.cloudwalk.web;

import com.cloudwalk.harness.Trace;

/**
 * Phase 0 spike: run the simulation, compiled to JavaScript, and print the
 * same trace the JVM prints.
 *
 * The question this answers is whether TeaVM's class library covers what the
 * engine needs - StreamTokenizer for the asset parsers, the java.nio buffers
 * the geometry uses, Vector and Random and the rest - and whether the physics
 * comes out the same on both sides. If the two traces match, everything except
 * rendering and input has already made it to the browser.
 */
public final class WebMain {

	public static void main(String[] args) {
		String task = args.length > 0 ? args[0] : "t001";
		int pilotType = args.length > 1 ? Integer.parseInt(args[1]) : 0;
		int frames = args.length > 2 ? Integer.parseInt(args[2]) : 2000;
		int every = args.length > 3 ? Integer.parseInt(args[3]) : 25;

		System.out.print(Trace.run(new BakedAssets(), task, pilotType, frames, every));
	}

	private WebMain() {
	}
}
