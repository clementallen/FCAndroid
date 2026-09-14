package com.cloudwalk.harness;

/**
 * Where the bundled .task and glider .txt files come from.
 *
 * The JVM reads them off disk, the browser out of a map fetched up front.
 * Both hand back the whole file as text - they are a few hundred lines in
 * total, and keeping it synchronous is what lets the StreamTokenizer parsers
 * stay exactly as they are.
 */
public interface AssetSource {

	/** @param name e.g. "t001.task" or "hangglider.txt" */
	String read(String name);
}
