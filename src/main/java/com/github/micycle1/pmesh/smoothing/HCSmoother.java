package com.github.micycle1.pmesh.smoothing;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.micycle1.pmesh.HEVertex;
import com.github.micycle1.pmesh.PMesh;

import processing.core.PVector;

/**
 * Implements the HC ("Humphrey’s Classes") mesh smoothing algorithm, a
 * Laplacian-like approach, but pushes the vertices of the smoothed mesh back
 * towards their previous locations to avoid the well-known problem of
 * deformation and shrinkage.
 * <p>
 * The HC-algorithm works in two stages:
 * <ol>
 * <li>A Laplacian smoothing step: <br>
 * p<sub>i</sub> = (1/|adj(i)|) &Sigma;<sub>j ∈ adj(i)</sub> q<sub>j</sub>,
 * where q holds the previous positions.</li>
 * <li>An HC–correction step: <br>
 * b<sub>i</sub> = p<sub>i</sub> − (α &middot; o<sub>i</sub> + (1 − α) &middot;
 * q<sub>i</sub>), <br>
 * followed by updating: <br>
 * p<sub>i</sub> = p<sub>i</sub> − [β &middot; b<sub>i</sub> + (1 − β)/|adj(i)|
 * &Sigma;<sub>j∈adj(i)</sub> b<sub>j</sub>].</li>
 * </ol>
 * Here:
 * <ul>
 * <li>o<sub>i</sub> denotes the original position of the vertex (cached during
 * construction),</li>
 * <li>α controls the influence of the original positions relative to the
 * previous positions, and</li>
 * <li>β controls the contribution of the correction term.</li>
 * </ul>
 */
public class HCSmoother extends MeshSmoother {

	private final double alpha;
	private final double beta;
	// Store the original positions (o_i) of each vertex at construction time.
	private final PVector[] originalPositions;

	List<HEVertex> vertices;
	Map<HEVertex, Integer> vertexToIndexMap;

	/**
	 * Constructs a {@code HCSmoother} for the given mesh and parameters.
	 *
	 * @param mesh  the mesh to be smoothed; must not be null
	 * @param alpha the weight for the original positions (o_i) in computing b_i;
	 *              must be between 0 and 1
	 * @param beta  the weight for the difference b_i at the vertex itself in the
	 *              correction; must be between 0 and 1
	 * @throws IllegalArgumentException if the mesh is null or if alpha or beta are
	 *                                  outside [0,1]
	 */
	public HCSmoother(PMesh mesh, double alpha, double beta) {
		super(mesh);
		if (mesh == null) {
			throw new IllegalArgumentException("Mesh cannot be null.");
		}
		if (alpha < 0 || alpha > 1) {
			throw new IllegalArgumentException("Alpha must be between 0 and 1 (inclusive).");
		}
		if (beta < 0 || beta > 1) {
			throw new IllegalArgumentException("Beta must be between 0 and 1 (inclusive).");
		}
		this.alpha = alpha;
		this.beta = beta;

		// Cache the original positions from the mesh vertices.
		this.vertices = mesh.getVertices();
		originalPositions = new PVector[vertices.size()];
		for (int i = 0; i < vertices.size(); i++) {
			// Create a copy of the vertex position.
			originalPositions[i] = vertices.get(i).getPosition().copy();
		}
		int numVertices = this.vertices.size();

		// Create a HashMap to map vertices to their indices
		vertexToIndexMap = new HashMap<>();
		for (int i = 0; i < numVertices; i++) {
			vertexToIndexMap.put(this.vertices.get(i), i);
		}
	}

	/**
	 * Performs a single iteration of the HC smoothing algorithm.
	 * <p>
	 * The algorithm first computes an intermediate Laplacian-based smoothing (q ->
	 * p) and then applies a correction step using the original positions.
	 * <p>
	 * If preserveBoundary is true, boundary vertices are not moved.
	 *
	 * @param preserveBoundary if true, do not move boundary vertices
	 * @return the maximum displacement of any vertex during this iteration
	 */
	@Override
	public double smoothOnce(boolean preserveBoundary) {
		List<HEVertex> vertices = mesh.getVertices();
		int numVertices = vertices.size();

		// Arrays to store the positions at different stages.
		// q: positions from the previous iteration.
		PVector[] qPositions = new PVector[numVertices];
		// p: positions after the Laplacian (smoothing) operation.
		PVector[] pPositions = new PVector[numVertices];
		// b: the differences computed in the first step.
		PVector[] bVectors = new PVector[numVertices];
		// newPositions: the final updated positions.
		PVector[] newPositions = new PVector[numVertices];

		double maxDisplacement = 0;

		// --- First, copy the current positions (q) from the vertices. ---
		for (int i = 0; i < numVertices; i++) {
			// Make a copy of the current position.
			qPositions[i] = vertices.get(i).getPosition().copy();
		}

		// --- First Pass: Laplacian Smoothing Step ---
		for (int i = 0; i < numVertices; i++) {
			HEVertex vertex = vertices.get(i);
			// For boundary vertices, if preserveBoundary is true, skip smoothing.
			if (preserveBoundary && vertex.isOnBoundary()) {
				// Keep the vertex position unchanged.
				pPositions[i] = qPositions[i].copy();
			} else {
				List<HEVertex> neighbors = vertex.getNeighbors();
				int n = neighbors.size();
				if (n > 0) {
					PVector avg = new PVector();
					for (HEVertex neighbor : neighbors) {
						avg.add(neighbor.getPosition());
					}
					avg.div(n);
					pPositions[i] = avg;
				} else {
					// If there are no neighbors, simply keep current position.
					pPositions[i] = qPositions[i].copy();
				}
			}

			// Compute b_i = p_i - (α * o_i + (1-α)*q_i)
			// o_i is the original position (cached in originalPositions).
			PVector weightedOriginal = originalPositions[i].copy();
			weightedOriginal.mult((float) alpha);
			PVector weightedQ = qPositions[i].copy();
			weightedQ.mult((float) (1.0 - alpha));
			PVector blend = PVector.add(weightedOriginal, weightedQ);
			bVectors[i] = PVector.sub(pPositions[i], blend);
		}

		// --- Second Pass: HC Correction Step ---
		for (int i = 0; i < numVertices; i++) {
			HEVertex vertex = vertices.get(i);
			if (preserveBoundary && vertex.isOnBoundary()) {
				// Do not change boundary vertices.
				newPositions[i] = qPositions[i].copy();
			} else {
				List<HEVertex> neighbors = vertex.getNeighbors();
				int n = neighbors.size();
				if (n > 0) {
					// Compute the average of b for the neighbors
					PVector sumB = new PVector();
					for (HEVertex neighbor : neighbors) {
						// Look up the index of the neighbor in the mesh’s vertex list.
						int j = vertexToIndexMap.get(neighbor);
						sumB.add(bVectors[j]);
					}
					sumB.div(n);

					// Compute correction: β*b_i + (1-β)*average(b_neighbors)
					PVector correction = new PVector();
					PVector term1 = bVectors[i].copy();
					term1.mult((float) beta);

					PVector term2 = sumB.copy();
					term2.mult((float) (1.0 - beta));

					correction = PVector.add(term1, term2);

					// Finally, update: newPosition = p_i - correction
					newPositions[i] = PVector.sub(pPositions[i], correction);
				} else {
					// If there are no neighbors, do not change the point.
					newPositions[i] = pPositions[i].copy();
				}
			}

			// Compute displacement from the old position.
			float displacement = PVector.dist(qPositions[i], newPositions[i]);
			if (displacement > maxDisplacement) {
				maxDisplacement = displacement;
			}
		}

		// --- Update all vertex positions simultaneously ---
		for (int i = 0; i < numVertices; i++) {
			vertices.get(i).getPosition().set(newPositions[i]);
		}

		return maxDisplacement;
	}
}
