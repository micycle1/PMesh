package com.github.micycle1.pmesh;

import static processing.core.PConstants.GROUP;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import processing.core.PConstants;
import processing.core.PShape;
import processing.core.PVector;

class PShapeUtils {

	private static final int green = color(173, 245, 66, 255);
	private static final int dark = color(39, 41, 37, 255);

	/**
	 * Extracts the children of a given PShape.
	 * <p>
	 * This method assumes that the children of the input shape are not GROUP shapes
	 * themselves, but rather simple polygons (Jordan curves) composed of vertices.
	 * It further assumes these child shapes are not Processing's primitive or
	 * bezier curve PShapes, but simple vertex-based paths.
	 * <p>
	 * For each child shape, this method:
	 * <ul>
	 * <li>Converts the PShape into a list of {@link PVector} vertices.</li>
	 * <li>Ensures the vertices are oriented clockwise. If they are
	 * counter-clockwise, the vertex order is reversed to enforce clockwise
	 * orientation.</li>
	 * <li>Converts the (potentially reversed) list of {@link PVector} vertices back
	 * into a <b>new PShape object</b>.</li>
	 * </ul>
	 *
	 * @param shape The parent PShape whose children are to be processed.
	 * @return A list of new PShapes, each representing a processed child of the
	 *         input shape, or an empty list if the input shape has no children or
	 *         if the input shape is null.
	 */
	static List<PShape> getChildren(PShape shape) {
		return Stream.of(shape.getChildren()).map(s -> {
			var vertices = toPVector(s); // structurally unclosed
			if (!isClockwise(vertices)) {
				Collections.reverse(vertices); // enforce CW orientation
			}
			return fromPVector(vertices); // closed with endShape(PConstants.CLOSE)
		}).toList();
	}

	/**
	 * Converts a PShape into a list of PVector objects representing its vertices.
	 * Ensures that duplicate consecutive vertices are not included in the list, and
	 * also removes the closing vertex if it matches the starting vertex.
	 */
	static List<PVector> toPVector(PShape shape) {
		final List<PVector> vertices = new ArrayList<>();
		PVector lastVertex = null;
		for (int i = 0; i < shape.getVertexCount(); i++) {
			PVector currentVertex = shape.getVertex(i);
			if (lastVertex == null || !lastVertex.equals(currentVertex)) {
				vertices.add(currentVertex);
				lastVertex = currentVertex;
			}
		}
		if (!vertices.isEmpty() && vertices.get(0).equals(vertices.get(vertices.size() - 1))) {
			vertices.remove(vertices.size() - 1); // removing closing vertex (if present)
		}
		if (vertices.size() < 3) {
			throw new IllegalArgumentException("Mesh faces must have at least 3 unique vertices.");
		}
		return vertices;
	}

	/**
	 * Creates a PShape object from a list of PVector vertices. The resulting PShape
	 * is a closed path.
	 *
	 * @param vertices The list of PVector objects representing the vertices of the
	 *                 shape.
	 * @return A PShape object representing the closed path defined by the given
	 *         vertices.
	 */
	static PShape fromPVector(Collection<PVector> vertices) {
		PShape shape = new PShape();
		shape.setFamily(PShape.PATH);
		shape.setFill(true);
		shape.setFill(green);
		shape.setStroke(true);
		shape.setStroke(dark);
		shape.setStrokeWeight(2);
		
		shape.beginShape();
		for (var v : vertices) {
			shape.vertex(v.x, v.y);
		}
		shape.endShape(PConstants.CLOSE);

		return shape;
	}

	/**
	 * Flattens a collection of PShapes into a single GROUP PShape which has the
	 * shapes as its children. If the collection contains only one shape, it is
	 * returned directly as a non-GROUP shape.
	 */
	static PShape flatten(Collection<PShape> shapes) {
		PShape group = new PShape(GROUP);
		shapes.forEach(group::addChild);
		if (group.getChildCount() == 1) {
			return group.getChild(0);
		}
		return group;
	}

	/**
	 * Determines whether a polygon defined by a list of points is oriented
	 * clockwise in Processing's coordinate system (y-axis points downward). This
	 * method aligns with what is visually clockwise on the screen, where the y-axis
	 * is inverted.
	 *
	 * @param points A list of {@link PVector} points defining a polygon. The
	 *               polygon must have at least 3 points.
	 * @return {@code true} if the polygon is oriented clockwise (visually clockwise
	 *         in Processing), {@code false} if counter-clockwise.
	 */
	static boolean isClockwise(final List<PVector> points) {
		if (points == null || points.size() < 3) {
			throw new IllegalArgumentException("Polygon must have at least 3 points.");
		}

		double area = 0;
		int n = points.size();

		for (int i = 0; i < n; i++) {
			PVector p1 = points.get(i);
			PVector p2 = points.get((i + 1) % n);
			area += (p1.x * p2.y - p2.x * p1.y);
		}

		// In Processing's y-down system, a positive area corresponds to CW
		return area > 0;
	}

	private static int color(final int red, final int green, final int blue, final int alpha) {
		return alpha << 24 | red << 16 | green << 8 | blue;
	}

}
