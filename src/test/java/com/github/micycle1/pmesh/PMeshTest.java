package com.github.micycle1.pmesh;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.function.ToDoubleFunction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import processing.core.PConstants;
import processing.core.PShape;
import processing.core.PVector;

public class PMeshTest {

	private PShape triangleMesh;
	private PShape quadMesh;
	private PShape gridWithHoleMesh;
	private PShape grid2x2;

	@BeforeEach
	void setUp() {
		triangleMesh = createSingleTriangleMesh(); // single triangle
		quadMesh = createQuadrilateralMesh(); // two triangles forming quadrilateral
		gridWithHoleMesh = create3x3GridSquaresWithHole();
		grid2x2 = create2x2GridSquares();
	}

	@Test
	void testTriangleMeshStructure() {
		PMesh mesh = new PMesh(triangleMesh);
		var t = triangleMesh.getChild(0);

		// Verify basic counts
		assertEquals(3, mesh.getVertices().size());
		assertEquals(3, mesh.getEdges().size());
		assertEquals(1, mesh.getFaces().size());

		// Verify edge connections
		HalfEdge firstEdge = mesh.getEdges().get(0);
		assertEquals(t.getVertex(0), firstEdge.start.getPosition());
		HalfEdge secondEdge = firstEdge.next;
		assertEquals(t.getVertex(1), secondEdge.start.getPosition());
		HalfEdge thirdEdge = secondEdge.next;
		assertEquals(t.getVertex(2), thirdEdge.start.getPosition());

		assertSame(firstEdge, thirdEdge.next);
		assertSame(secondEdge, firstEdge.next);
		assertSame(secondEdge, thirdEdge.prev);
		assertSame(thirdEdge, firstEdge.prev);

		// Verify boundary status
		for (HEVertex vertex : mesh.getVertices()) {
			assertTrue(vertex.onBoundary);
		}

		// Verify vertex neighbors
		for (HEVertex vertex : mesh.getVertices()) {
			assertEquals(2, vertex.getNeighbors().size());
		}
	}

	@Test
	void testQuadrilateralMeshStructure() {
		PMesh mesh = new PMesh(quadMesh);

		// Verify basic counts
		assertEquals(4, mesh.getVertices().size());
		assertEquals(5, mesh.getBaseEdges().size()); // 4 outer edges + 1 diagonal
		assertEquals(6, mesh.getEdges().size()); // 4 outer edges + 1 shared diagonal (=2)
		assertEquals(2, mesh.getFaces().size());

		// Verify twin edges using edges
		int twinCount = 0;
		for (HalfEdge edge : mesh.getEdges()) {
			if (edge.twin != null) {
				twinCount++;
			}
		}
		assertEquals(2, twinCount); // One pair of twin edges for the shared diagonal

		// Verify twin edges using base edges
		twinCount = 0;
		for (HalfEdge edge : mesh.getBaseEdges()) {
			if (edge.twin != null) {
				twinCount++;
			}
		}
		assertEquals(1, twinCount); // One pair of twin edges for the shared diagonal

		// Verify boundary vertices
		int boundaryCount = 0;
		for (HEVertex vertex : mesh.getVertices()) {
			if (vertex.onBoundary) {
				boundaryCount++;
			}
		}
		assertEquals(4, boundaryCount);
	}

	@Test
	void test3x3GridStructure() {
		PMesh mesh = new PMesh(gridWithHoleMesh);
		assertEquals(16, mesh.getVertices().size());
		assertEquals(24, mesh.getBaseEdges().size());
		assertEquals(48 - 12 - 4, mesh.getEdges().size()); // 24 edges*2 - perimeter edges - hole edges
		assertEquals(8, mesh.getFaces().size()); // 8 faces and hole
	}

	@Test
	void testVertexNeighborVertices() {
		PMesh mesh = new PMesh(grid2x2);

		assertEquals(9, mesh.getVertices().size());
		assertEquals(12, mesh.getBaseEdges().size());
		assertEquals(24 - 8, mesh.getEdges().size()); // 12 edges * 2 - perimeter edges
		assertEquals(4, mesh.getFaces().size()); // 8 faces and hole

		// Count the number of vertices with 2, 3, and 4 neighbors
		int count2Neighbors = 0;
		int count3Neighbors = 0;
		int count4Neighbors = 0;

		for (HEVertex v : mesh.getVertices()) {
			int neighborCount = v.getNeighbors().size();
			if (neighborCount == 2) {
				count2Neighbors++;
			} else if (neighborCount == 3) {
				count3Neighbors++;
			} else if (neighborCount == 4) {
				count4Neighbors++;
			} else {
				fail("Vertex has an unexpected number of neighbors");
			}
		}

		assertEquals(1, count4Neighbors); // 1 vertex with 4 neighbors
		assertEquals(4, count2Neighbors); // 4 vertices with 2 neighbors (corners)
		assertEquals(4, count3Neighbors); // 4 vertices with 3 neighbors
	}

	@Test
	@Disabled
	void testVertexNeighborEdges() {
		PMesh mesh = new PMesh(grid2x2);
		for (HEVertex v : mesh.getVertices()) {
			System.out.println(v.getOutgoingEdges().size());
			if (v.getPosition().equals(new PVector(1, 1))) {
//				System.out.println(v.outgoingEdges.size());
				v.getOutgoingEdges().forEach(e -> System.out.println(e));

			}
		}
	}

	@Test
	void testEdgeNavigation() {
		PMesh mesh = new PMesh(triangleMesh);
		HalfEdge edge = mesh.getEdges().get(0);

		// Test 1: Verify loop navigation (3 hops for a triangle)
		HalfEdge current = edge;
		int count = 0;
		do {
			current = current.next;
			count++;
		} while (current != edge);

		assertEquals(3, count); // Should return to the starting edge after 3 hops

		// Test 2: Verify forward-backward navigation
		HalfEdge forwardEdge = edge.next; // Move forward once
		HalfEdge backwardEdge = forwardEdge.prev; // Move backward once

		assertSame(edge, backwardEdge); // Should return to the original edge

		// Test 3: Verify full forward-backward loop
		HalfEdge forward1 = edge.next; // Move forward once
		HalfEdge forward2 = forward1.next; // Move forward twice
		HalfEdge forward3 = forward2.next; // Move forward three times (full loop)

		assertSame(edge, forward3); // Should return to the original edge after 3 forward hops

		HalfEdge backward1 = forward3.prev; // Move backward once
		HalfEdge backward2 = backward1.prev; // Move backward twice
		HalfEdge backward3 = backward2.prev; // Move backward three times (full loop)

		assertSame(edge, backward3); // Should return to the original edge after 3 backward hops
	}

	@Test
	void testVertexPositionAccuracy() {
		PMesh mesh = new PMesh(triangleMesh);
		for (HEVertex vertex : mesh.getVertices()) {
			assertNotNull(vertex.getPosition());
			assertEquals(3, vertex.getPosition().array().length);
		}
	}

	@Test
	void testFaceVertexOrderPreserved() {
		PMesh mesh = new PMesh(quadMesh);
		var f1Vertices = mesh.getFaceVertices(mesh.getFaces().get(0)).stream().map(v -> v.getPosition()).toList();
		var f2Vertices = mesh.getFaceVertices(mesh.getFaces().get(1)).stream().map(v -> v.getPosition()).toList();

		assertEquals(3, f1Vertices.size());
		assertTrue(PShapeUtils.isClockwise(f1Vertices));

		assertEquals(3, f2Vertices.size());
		assertTrue(PShapeUtils.isClockwise(f2Vertices));
	}

	@Test
	void testToUnweightedGraph() {
		var mesh = new PMesh(grid2x2);
		var graph = mesh.toUnweightedGraph();
		assertEquals(9, graph.vertexSet().size());
		assertEquals(12, graph.edgeSet().size());
	}

	@Test
	void testToWeightedGraph() {
		var mesh = new PMesh(grid2x2);
		ToDoubleFunction<HalfEdge> weightFunction = edge -> edge.start.getPosition().dist(edge.getEndVertex().getPosition());

		var graph = mesh.toWeightedGraph(weightFunction);
		assertEquals(9, graph.vertexSet().size());
		assertEquals(12, graph.edgeSet().size());
		graph.edgeSet().forEach(edge -> {
			double expectedWeight = weightFunction.applyAsDouble(findHalfEdge(mesh, graph.getEdgeSource(edge), graph.getEdgeTarget(edge)));
			assertEquals(expectedWeight, graph.getEdgeWeight(edge));
		});
	}

	private HalfEdge findHalfEdge(PMesh mesh, HEVertex u, HEVertex v) {
		for (HalfEdge he : mesh.getBaseEdges()) {
			if (he.start == u && he.getEndVertex() == v || he.start == v && he.getEndVertex() == u) {
				return he;
			}
		}
		return null; // Should not happen in these tests if graph is built correctly
	}

	private PShape createSingleTriangleMesh() {
		PShape mesh = new PShape(PConstants.GROUP);
		PShape face = new PShape(PShape.PATH);
		face.beginShape();
		face.vertex(0, 0);
		face.vertex(1, 0);
		face.vertex(0, 1);
		face.endShape(PConstants.CLOSE);
		mesh.addChild(face);
		return mesh;
	}

	// two triangles forming a quadrilateral
	private PShape createQuadrilateralMesh() {
		PShape mesh = new PShape(PConstants.GROUP);

		// First triangle
		PShape face1 = new PShape(PShape.GEOMETRY);
		face1.beginShape();
		face1.vertex(0, 0);
		face1.vertex(1, 0);
		face1.vertex(1, 1);
		face1.endShape();

		// Second triangle
		PShape face2 = new PShape(PShape.GEOMETRY);
		face2.beginShape();
		face2.vertex(0, 0);
		face2.vertex(1, 1);
		face2.vertex(0, 1);
		face2.endShape();

		mesh.addChild(face1);
		mesh.addChild(face2);
		return mesh;
	}

	private PShape create3x3GridSquaresWithHole() {
		PShape mesh = new PShape(PConstants.GROUP);

		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 3; col++) {
				if (!(row == 1 && col == 1)) { // Skip the central element
					PShape squareMesh = createSquareAt(col, row); // Create square at grid position (col, row)
					mesh.addChild(squareMesh);
				}
			}
		}
		return mesh;
	}

	private PShape create2x2GridSquares() {
		PShape mesh = new PShape(PConstants.GROUP);

		for (int row = 0; row < 2; row++) {
			for (int col = 0; col < 2; col++) {
				PShape squareMesh = createSquareAt(col, row); // Create square at grid position (col, row)
				mesh.addChild(squareMesh);
			}
		}
		return mesh;
	}

	private PShape createSquareAt(float startX, float startY) {
		PShape mesh = new PShape(PConstants.PATH);
		mesh.beginShape(PConstants.QUAD);
		mesh.vertex(startX, startY); // vertex 1
		mesh.vertex(startX + 1, startY); // vertex 2
		mesh.vertex(startX + 1, startY + 1); // vertex 3
		mesh.vertex(startX, startY + 1); // vertex 4
		mesh.endShape(PConstants.CLOSE);
		return mesh;
	}
}