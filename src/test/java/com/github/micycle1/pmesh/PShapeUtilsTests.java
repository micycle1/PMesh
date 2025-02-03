package com.github.micycle1.pmesh;

import static com.github.micycle1.pmesh.PShapeUtils.isClockwise;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import processing.core.PVector;

class PShapeUtilsTests {

	@Test
	void testOrientation() {
		// @formatter:off
        //		(0,0) --------> (1,0)  (x-axis increasing to the right)
        //		   |              |
        //		   |              |
        //		   V              V
        //		(0,1) <--------- (1,1)  (y-axis increasing downwards)
		// @formatter:on
		List<PVector> clockwisePoints = List.of(new PVector(0, 0), new PVector(1, 0), new PVector(1, 1), new PVector(0, 1));
		assertTrue(isClockwise(clockwisePoints));

		List<PVector> counterClockwisePoints = List.of(new PVector(0, 0), new PVector(0, 1), new PVector(1, 1), new PVector(1, 0));
		assertFalse(isClockwise(counterClockwisePoints));

		List<PVector> clockwisePointsClosed = new ArrayList<>(
				List.of(new PVector(0, 0), new PVector(1, 0), new PVector(1, 1), new PVector(0, 1), new PVector(0, 0)));
		assertTrue(isClockwise(clockwisePointsClosed));

		List<PVector> counterClockwisePointsClosed = new ArrayList<>(
				List.of(new PVector(0, 0), new PVector(0, 1), new PVector(1, 1), new PVector(1, 0), new PVector(0, 0)));
		assertFalse(isClockwise(counterClockwisePointsClosed));

	}

}
