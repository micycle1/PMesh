package com.github.micycle1.pmesh.smoothing;

import com.github.micycle1.pmesh.PMesh;

/**
 * An abstract base class for smoothing operations on a {@link PMesh}.
 * Implementations of this class may modify the topology and geometry of the
 * underlying mesh.
 */
public abstract class MeshSmoother {

	/** The maximum number of iterations allowed for smoothing operations. */
	public static final int MAX_ITERATIONS = 10_000;

	/** The mesh on which the smoothing operations are performed. */
	protected final PMesh mesh;

	/**
	 * Constructs a {@code MeshSmoother} for the given mesh.
	 *
	 * @param mesh the mesh to be smoothed; must not be null
	 * @throws IllegalArgumentException if the mesh is null
	 */
	public MeshSmoother(PMesh mesh) {
		if (mesh == null) {
			throw new IllegalArgumentException("Mesh must not be null.");
		}
		this.mesh = mesh;
	}

	/**
	 * Performs a single iteration of the smoothing operation.
	 *
	 * @param preserveBoundary if true, the boundary of the mesh will be preserved
	 *                         during smoothing
	 * @return the maximum displacement of any vertex during this iteration
	 */
	public abstract double smoothOnce(boolean preserveBoundary);

	/**
	 * Performs the smoothing operation for a specified number of iterations.
	 *
	 * @param iterations       the number of iterations to perform; must be
	 *                         non-negative
	 * @param preserveBoundary if true, the boundary of the mesh will be preserved
	 *                         during smoothing
	 * @return the maximum displacement of any vertex during the last iteration
	 */
	public double smooth(int iterations, boolean preserveBoundary) {
		if (iterations < 0) {
			throw new IllegalArgumentException("Number of iterations must be non-negative.");
		}
		double displacement = 0;
		for (int i = 0; i < iterations; i++) {
			displacement = smoothOnce(preserveBoundary);
		}

		return displacement;
	}

	/**
	 * Performs the smoothing operation until the maximum vertex displacement falls
	 * below the specified cutoff or the maximum number of iterations is reached.
	 *
	 * @param displacementCutoff the threshold for stopping the smoothing operation
	 * @param preserveBoundary   if true, the boundary of the mesh will be preserved
	 *                           during smoothing
	 */
	public void smoothUntilConvergence(double displacementCutoff, boolean preserveBoundary) {
		if (displacementCutoff < 0) {
			throw new IllegalArgumentException("Displacement cutoff must be non-negative.");
		}
		int iteration = 0;
		double displacement;
		do {
			displacement = smoothOnce(preserveBoundary);
		} while (displacement > displacementCutoff && iteration++ < MAX_ITERATIONS);
	}
}