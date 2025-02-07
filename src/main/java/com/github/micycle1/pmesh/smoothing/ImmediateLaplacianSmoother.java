package com.github.micycle1.pmesh.smoothing;

import java.util.List;
import com.github.micycle1.pmesh.HEVertex;
import com.github.micycle1.pmesh.PMesh;
import processing.core.PVector;

/**
 * Applies Laplacian smoothing to a {@link PMesh} by immediately updating
 * vertex positions. Laplacian smoothing moves each vertex toward the centroid
 * of its neighboring vertices, weighted by a smoothing factor.
 */
public class ImmediateLaplacianSmoother extends SimultaneousLaplacianSmoother {
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
	public ImmediateLaplacianSmoother(PMesh mesh, double smoothingFactor) {
		super(mesh, smoothingFactor);
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
		float maxDisplacement = 0;

		for (int j = 0; j < vertices.size(); j++) {
			final HEVertex vertex = vertices.get(j);
			if (!preserveBoundary || !vertex.isOnBoundary()) {
				PVector centroid = computeCentroid(vertex);
				PVector newPosition = computeNewPosition(vertex.getPosition(), centroid);
				vertex.getPosition().set(newPosition);
				// Track maximum displacement
				float displacement = PVector.dist(vertex.getPosition(), newPosition);
				maxDisplacement = Math.max(maxDisplacement, displacement);
			}
		}

		return maxDisplacement;
	}
}