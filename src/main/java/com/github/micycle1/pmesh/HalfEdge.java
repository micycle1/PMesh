package com.github.micycle1.pmesh;

/**
 * A half-edge is oriented such that the face it comprises lies to its left.
 * (because shape vertices are orientated CW)
 */
class HalfEdge {
	public HEVertex start;
	public HalfEdge twin;
	public HalfEdge next;
	public HalfEdge prev;
	public HalfEdge baseReference; // may be itself or twin

	public HalfEdge(HEVertex start) {
		this.start = start;
	}

	public HEVertex getEndVertex() {
		return next.start;
	}

	public double length() {
		return start.getPosition().dist(next.start.getPosition());
	}

	@Override
	public String toString() {
		return start.toString() + " -> " + next.start.toString();
	}
}