package com.github.micycle1.pmesh;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import processing.core.PVector;

public class HEVertex {

	private final PVector position;
	final List<HalfEdge> outgoingEdges = new ArrayList<>();
	final List<HEVertex> neighbors = new ArrayList<>();
	boolean onBoundary = false;

	public HEVertex(PVector position) {
		this.position = position.copy();
	}

	public PVector getPosition() {
		return position;
	}

	public List<HalfEdge> getOutgoingEdges() {
		return Collections.unmodifiableList(outgoingEdges);
	}

	public List<HEVertex> getNeighbors() {
		return Collections.unmodifiableList(neighbors);
	}

	@Override
	public String toString() {
		return String.format("(%s, %s)", position.x, position.y);
	}

	public boolean isOnBoundary() {

		return onBoundary;
	}
}