package com.github.micycle1.pmesh;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.micycle1.pmesh.PMesh.HEVertex;
import com.github.micycle1.pmesh.PMesh.HalfEdge;

import processing.core.PConstants;
import processing.core.PShape;

public class PMeshTest {

	private PShape triangleMesh;
	private PShape quadMesh;
	private PShape gridWithHoleMesh;

	@BeforeEach
	public void setUp() {
		triangleMesh = createSingleTriangleMesh();
		quadMesh = createQuadrilateralMesh();
		gridWithHoleMesh = create3x3GridSquaresWithHole();
	}

	@Test
	public void testTriangleMeshStructure() {
		PMesh mesh = new PMesh(triangleMesh);
		var t = triangleMesh.getChild(0);

		// Verify basic counts
		assertEquals(3, mesh.vertices.size());
		assertEquals(3, mesh.edges.size());
		assertEquals(1, mesh.faces.size());

		// Verify edge connections
		HalfEdge firstEdge = mesh.edges.get(0);
		assertEquals(t.getVertex(0), firstEdge.start.position);
		HalfEdge secondEdge = firstEdge.next;
		assertEquals(t.getVertex(1), secondEdge.start.position);
		HalfEdge thirdEdge = secondEdge.next;
		assertEquals(t.getVertex(2), thirdEdge.start.position);

		assertSame(firstEdge, thirdEdge.next);
		assertSame(secondEdge, firstEdge.next);
		assertSame(secondEdge, thirdEdge.prev);
		assertSame(thirdEdge, firstEdge.prev);

		// Verify boundary status
		for (HEVertex vertex : mesh.vertices) {
			assertTrue(vertex.onBoundary);
		}

		// Verify vertex neighbors
		for (HEVertex vertex : mesh.vertices) {
			assertEquals(2, vertex.neighbors.size());
		}
	}

	@Test
	public void testQuadrilateralMeshStructure() {
		PMesh pMesh = new PMesh(quadMesh);

		// Verify basic counts
		assertEquals(4, pMesh.vertices.size());
		assertEquals(5, pMesh.baseEdges.size()); // 4 outer edges + 1 diagonal
		assertEquals(6, pMesh.edges.size()); // 4 outer edges + 1 shared diagonal (=2)
		assertEquals(2, pMesh.faces.size());

		// Verify twin edges using edges
		int twinCount = 0;
		for (HalfEdge edge : pMesh.edges) {
			if (edge.twin != null) {
				twinCount++;
			}
		}
		assertEquals(2, twinCount); // One pair of twin edges for the shared diagonal

		// Verify twin edges using base edges
		twinCount = 0;
		for (HalfEdge edge : pMesh.baseEdges) {
			if (edge.twin != null) {
				twinCount++;
			}
		}
		assertEquals(1, twinCount); // One pair of twin edges for the shared diagonal

		// Verify boundary vertices
		int boundaryCount = 0;
		for (HEVertex vertex : pMesh.vertices) {
			if (vertex.onBoundary) {
				boundaryCount++;
			}
		}
		assertEquals(4, boundaryCount);
	}

	@Test
	public void test3x3GridStructure() {
		PMesh pMesh = new PMesh(gridWithHoleMesh);
		assertEquals(16, pMesh.vertices.size());
		assertEquals(24, pMesh.baseEdges.size());
		assertEquals(48 - 12 - 4, pMesh.edges.size()); // 24 edges*2 - perimeter edges - hole edges
		assertEquals(8, pMesh.faces.size()); // 8 faces and hole
	}

	@Test
	public void testEdgeNavigation() {
		PMesh pMesh = new PMesh(triangleMesh);
		HalfEdge edge = pMesh.edges.get(0);

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
	public void testVertexPositionAccuracy() {
		PMesh pMesh = new PMesh(triangleMesh);
		for (HEVertex vertex : pMesh.vertices) {
			assertNotNull(vertex.position);
			assertEquals(3, vertex.position.array().length);
		}
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