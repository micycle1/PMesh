package com.github.micycle1.pmesh;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import processing.core.PShape;
import processing.core.PVector;

public class PMesh {

	final PShape mesh;
	final List<HEVertex> vertices;
	final List<HEFace> faces;
	final List<HalfEdge> edges;
	final List<HalfEdge> baseEdges;

	/**
	 *
	 * @param mesh a <code>GROUP</code> PShape whose child shapes comprise a
	 *             conforming mesh. The mesh may form holes but each face must be a
	 *             simple polygon.
	 */
	public PMesh(PShape mesh) {
		this.mesh = mesh;
		this.vertices = new ArrayList<>();
		this.faces = new ArrayList<>();
		this.edges = new ArrayList<>();
		this.baseEdges = new ArrayList<>();
		buildHalfEdgeStructure();
	}

	private void buildHalfEdgeStructure() {
		Map<PVector, HEVertex> pvectorToVertex = new HashMap<>();
		Map<EdgeKey, HalfEdge> edgeMap = new HashMap<>();

		List<PShape> faceShapes = PShapeUtils.getChildren(mesh);
		for (PShape faceShape : faceShapes) {
			int vertexCount = faceShape.getVertexCount();
			List<HEVertex> faceVertices = new ArrayList<>(vertexCount);

			for (int i = 0; i < vertexCount; i++) {
				PVector p = faceShape.getVertex(i);
				HEVertex v = pvectorToVertex.computeIfAbsent(p, k -> {
					HEVertex newV = new HEVertex(p);
					vertices.add(newV);
					return newV;
				});
				faceVertices.add(v);
			}

			List<HalfEdge> faceEdges = new ArrayList<>(vertexCount);
			HEFace face = new HEFace();
			faces.add(face);

			for (int i = 0; i < vertexCount; i++) {
				HEVertex a = faceVertices.get(i);
				HEVertex b = faceVertices.get((i + 1) % vertexCount);

				EdgeKey edgeKey = new EdgeKey(a, b);
				EdgeKey twinKey = new EdgeKey(b, a);

				HalfEdge he = edgeMap.get(edgeKey);
				HalfEdge twin = edgeMap.get(twinKey);
				boolean hasTwin = twin != null;
				if (he == null) {
					he = new HalfEdge(a);
					edges.add(he);
					edgeMap.put(edgeKey, he);
					a.outgoingEdges.add(he);

					if (hasTwin) {
						he.twin = twin;
						twin.twin = he;
					} else {
						baseEdges.add(he);
					}
				} else {
					System.out.println("wtf");
				}
				faceEdges.add(he);
			}

			for (int i = 0; i < vertexCount; i++) {
				HalfEdge current = faceEdges.get(i);
				HalfEdge next = faceEdges.get((i + 1) % vertexCount);
				current.next = next;
				next.prev = current;
			}
			face.edge = faceEdges.get(0);
		}

		// Mark boundary vertices
		edges.forEach(he -> {
			if (he.twin == null) {
				he.start.onBoundary = true;
				he.getEndVertex().onBoundary = true;
			}
		});

		// Build neighbors list
		vertices.forEach(v -> {
			Set<HEVertex> uniqueNeighbors = new LinkedHashSet<>();
			v.outgoingEdges.forEach(he -> {
				uniqueNeighbors.add(he.getEndVertex());

				uniqueNeighbors.add(he.prev.start);
			});
			v.neighbors.addAll(uniqueNeighbors);
		});
	}

	static class HEVertex {
		public final PVector position;
		public final List<HalfEdge> outgoingEdges = new ArrayList<>();
		public final List<HEVertex> neighbors = new ArrayList<>();
		public boolean onBoundary = false;

		public HEVertex(PVector position) {
			this.position = position.copy();
		}

		public PVector getPosition() {
			return position;
		}

		@Override
		public String toString() {
			return position.toString();
		}
	}

	static class HalfEdge {
		public HEVertex start;
		public HalfEdge twin;
		public HalfEdge next;
		public HalfEdge prev;

		public HalfEdge(HEVertex start) {
			this.start = start;
		}

		public HEVertex getEndVertex() {
			return next.start;
		}

		@Override
		public String toString() {
			return start.toString();
		}
	}

	static class HEFace {
		public HalfEdge edge;
	}

	private static class EdgeKey {
		private final HEVertex a;
		private final HEVertex b;

		public EdgeKey(HEVertex a, HEVertex b) {
			this.a = a;
			this.b = b;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof EdgeKey edgeKey)) {
				return false;
			}
			return a == edgeKey.a && b == edgeKey.b;
		}

		@Override
		public int hashCode() {
			return Objects.hash(System.identityHashCode(a), System.identityHashCode(b));
		}
	}

	// Example of how to get all half-edges of a face
	public List<HalfEdge> getFaceHalfEdges(HEFace face) {
		List<HalfEdge> faceHalfEdges = new ArrayList<>();
		if (face.edge != null) {
			HalfEdge currentHE = face.edge;
			do {
				faceHalfEdges.add(currentHE);
				currentHE = currentHE.next;
			} while (currentHE != face.edge);
		}
		return faceHalfEdges;
	}

	// Example of how to get all vertices of a face
	public List<HEVertex> getFaceVertices(HEFace face) {
		List<HEVertex> faceVertices = new ArrayList<>();
		List<HalfEdge> faceHalfEdges = getFaceHalfEdges(face);
		for (HalfEdge he : faceHalfEdges) {
			faceVertices.add(he.start);
		}
		return faceVertices;
	}
}