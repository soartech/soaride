package com.soartech.soar.ide.core.sql.datamap;

import java.util.ArrayList;

import com.soartech.soar.ide.core.sql.Triple;

/**
 * Represents a path from the root node &lt;s&gt; that cannot extend any farther.
 * Either the last node has no child nodes, or the path loops into itself, or it
 * loops into another terminal path, or something like that.
 * @author miller
 *
 */
public class TerminalPath {
	public ArrayList<Triple> path;
	public ArrayList<Triple> links = new ArrayList<Triple>();
	public TerminalPath(ArrayList<Triple> path, ArrayList<Triple> links) {
		this.path = path;
		this.links = links;
	}
	@Override
	public String toString() {
		StringBuffer buff = new StringBuffer();
		buff.append("" + path);
		if (links != null) {
			buff.append(", loops to: " + links);
		}
		return buff.toString();
	}
}
