package com.github.micycle1.pmesh.smoothing;

import java.util.List;
import com.github.micycle1.pmesh.HEVertex;
import com.github.micycle1.pmesh.PMesh;
import processing.core.PVector;

/**
 * Applies Laplacian smoothing to a {@link PMesh} by simultaneously updating
 * vertex positions. Laplacian smoothing moves each vertex toward the centroid
 * of its neighboring vertices, weighted by a smoothing factor.
 */
public class SimultaneousLaplacianSmoother extends MeshSmoother {

	protected final double smoothingFactor;

	/**
	 * Constructs a {@code SimultaneousLaplacianSmoother} for the given mesh and
	 * smoothing factor.
	 *
	 * @param mesh            the mesh to be smoothed; must not be null
	 * @param smoothingFactor the factor controlling the degree of smoothing; must
	 *                        be between 0 and 1 (inclusive)
	 * @throws IllegalArgumentException if the mesh is null or the smoothing factor
	 *                                  is outside the valid range
	 */
	public SimultaneousLaplacianSmoother(PMesh mesh, double smoothingFactor) {
		super(mesh);
		if (smoothingFactor < 0 || smoothingFactor > 1) {
			throw new IllegalArgumentException("Smoothing factor must be between 0 and 1 (inclusive).");
		}
		this.smoothingFactor = smoothingFactor;
	}

	/**
	 * Performs a single iteration of Laplacian smoothing. Vertex positions are
	 * updated simultaneously after computing new positions for all vertices.
	 *
	 * @param preserveBoundary if true, boundary vertices will not be moved
	 * @return the maximum displacement of any vertex during this iteration
	 */
	@Override
	public double smoothOnce(boolean preserveBoundary) {
		List<HEVertex> vertices = mesh.getVertices();
		PVector[] newPositions = new PVector[vertices.size()];
		float maxDisplacement = 0;

		// Compute new positions for all vertices
		for (int j = 0; j < vertices.size(); j++) {
			HEVertex vertex = vertices.get(j);
			if (!preserveBoundary || !vertex.isOnBoundary()) {
				PVector centroid = computeCentroid(vertex);
				PVector newPosition = computeNewPosition(vertex.getPosition(), centroid);
				newPositions[j] = newPosition;

				// Track maximum displacement
				float displacement = PVector.dist(vertex.getPosition(), newPosition);
				maxDisplacement = Math.max(maxDisplacement, displacement);
			} else {
				newPositions[j] = vertex.getPosition(); // Preserve boundary vertex position
			}
		}

		// Update vertex positions simultaneously
		for (int j = 0; j < vertices.size(); j++) {
			vertices.get(j).getPosition().set(newPositions[j]);
		}

		return maxDisplacement;
	}

	/**
	 * Computes the centroid of a vertex's neighboring vertices.
	 *
	 * @param vertex the vertex whose neighbors are used to compute the centroid
	 * @return the centroid of the vertex's neighbors
	 */
	protected PVector computeCentroid(HEVertex vertex) {
		PVector centroid = new PVector();
		List<HEVertex> neighbors = vertex.getNeighbors();
		for (HEVertex neighbor : neighbors) {
			centroid.add(neighbor.getPosition());
		}
		if (!neighbors.isEmpty()) {
			centroid.div(neighbors.size());
		}
		return centroid;
	}

	/**
	 * Computes the new position of a vertex based on its current position and the
	 * centroid.
	 *
	 * @param currentPosition the current position of the vertex
	 * @param centroid        the centroid of the vertex's neighbors
	 * @return the new position of the vertex
	 */
	protected PVector computeNewPosition(PVector currentPosition, PVector centroid) {
		PVector diff = PVector.sub(centroid, currentPosition);
		diff.mult((float) smoothingFactor);
		return PVector.add(currentPosition, diff);
	}
}