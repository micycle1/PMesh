package com.github.micycle1.pmesh;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToDoubleFunction;

import org.jgrapht.Graph;
import org.jgrapht.graph.SimpleGraph;
import org.jgrapht.graph.SimpleWeightedGraph;

import processing.core.PShape;
import processing.core.PVector;

/**
 * Half-edge representation for mesh-like PShapes.
 */
public class PMesh {

	// TODO vertex deletion
	// TODO edge deletion (non-boundary edges only?)
	// TODO good way to get vertex --> all outgoing edges (directionally based) (or
	// adjacent vertices)
	// TODO add steiner vertex (connect to vertices on the face it's inserted into)
	
	// https://geometry-central.net/surface/surface_mesh/mutation/

	private final List<HEVertex> vertices;
	private final List<HEFace> faces;
	private final List<HalfEdge> edges;
	private final List<HalfEdge> baseEdges;

	/**
	 *
	 * @param mesh a <code>GROUP</code> PShape whose child shapes comprise a
	 *             conforming mesh. The mesh may form holes but each face must be a
	 *             simple polygon.
	 */
	public PMesh(PShape mesh) {
		this.vertices = new ArrayList<>();
		this.faces = new ArrayList<>();
		this.edges = new ArrayList<>();
		this.baseEdges = new ArrayList<>();
		buildHalfEdgeStructure(mesh);
	}

	private void buildHalfEdgeStructure(PShape mesh) {
		List<PShape> faceShapes = PShapeUtils.getChildren(mesh);

		Map<PVector, HEVertex> pvectorToVertex = new HashMap<>(4 * faceShapes.size());
		Map<EdgeKey, HalfEdge> edgeMap = new HashMap<>(4 * faceShapes.size());

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
			getFaces().add(face);

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
					getEdges().add(he);
					edgeMap.put(edgeKey, he);
					a.outgoingEdges.add(he);

					if (hasTwin) {
						he.twin = twin;
						twin.twin = he;
					} else {
						he.baseReference = he;
						getBaseEdges().add(he);
					}
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
		getEdges().forEach(he -> {
			if (he.twin == null) {
				// TODO
				he.start.onBoundary = true;
				he.getEndVertex().onBoundary = true;
			}
		});

		// Build neighbors list
		vertices.forEach(v -> {
			Set<HEVertex> uniqueNeighbors = new LinkedHashSet<>();
			v.outgoingEdges.forEach(he -> {
				// TODO
				uniqueNeighbors.add(he.getEndVertex());
				uniqueNeighbors.add(he.prev.start);
			});
			v.neighbors.addAll(uniqueNeighbors);
		});
	}

	public List<HEVertex> getVertices() {
		return vertices;
	}

	public List<HEFace> getFaces() {
		return faces;
	}

	public List<HalfEdge> getEdges() {
		return edges;
	}

	public List<HalfEdge> getBaseEdges() {
		return baseEdges;
	}

	public List<HalfEdge> getFaceHalfEdges(HEFace face) {
		List<HalfEdge> faceHalfEdges = new ArrayList<>();
		var start = face.edge;
		if (start != null) {
			HalfEdge currentHE = start;
			do {
				faceHalfEdges.add(currentHE);
				currentHE = currentHE.next;
			} while (currentHE != start);
		}
		return faceHalfEdges;
	}

	public List<HEVertex> getFaceVertices(HEFace face) {
		List<HEVertex> faceVertices = new ArrayList<>();
		List<HalfEdge> faceHalfEdges = getFaceHalfEdges(face);
		for (HalfEdge he : faceHalfEdges) {
			faceVertices.add(he.start);
		}
		return faceVertices;
	}

	public PShape toPShape() {
		var faceShapes = faces.stream().map(f -> {
			var vertices = getFaceVertices(f).stream().map(v -> v.getPosition()).toList();
			return PShapeUtils.fromPVector(vertices);
		}).toList();

		return PShapeUtils.flatten(faceShapes);
	}

	/**
	 * Returns a simple undirected unweighted graph representing the mesh, with
	 * HalfEdge as edges.
	 * 
	 * @return a simple undirected unweighted graph
	 */
	public Graph<HEVertex, HalfEdge> toUnweightedGraph() {
		SimpleGraph<HEVertex, HalfEdge> graph = new SimpleGraph<>(HalfEdge.class);
		vertices.forEach(graph::addVertex);
		getBaseEdges().forEach(baseEdge -> graph.addEdge(baseEdge.start, baseEdge.getEndVertex(), baseEdge));
		return graph;
	}

	/**
	 * Returns a simple undirected weighted graph representing the mesh, with
	 * HalfEdge as edges and weights derived from the supplied edge weight function.
	 * 
	 * @param edgeWeightFunction function to determine the weight of an edge. Takes
	 *                           a HalfEdge and returns a double weight.
	 * @return a simple undirected weighted graph
	 */
	public Graph<HEVertex, HalfEdge> toWeightedGraph(ToDoubleFunction<HalfEdge> edgeWeightFunction) {
		SimpleWeightedGraph<HEVertex, HalfEdge> graph = new SimpleWeightedGraph<>(HalfEdge.class);
		vertices.forEach(graph::addVertex);
		getBaseEdges().forEach(edge -> {
			graph.addEdge(edge.start, edge.getEndVertex(), edge);
			double weight = edgeWeightFunction.applyAsDouble(edge);
			graph.setEdgeWeight(edge, weight);
		});
		return graph;
	}

	static class HEFace {
		public HalfEdge edge;
	}

	private static class EdgeKey {
		private final HEVertex a;
		private final HEVertex b;

		private EdgeKey(HEVertex a, HEVertex b) {
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
			int hash = (1 + a.hashCode()) * 31;
			hash = 31 * hash + b.hashCode();
			return hash;
		}
	}
}