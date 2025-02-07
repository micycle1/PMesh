# PMesh: Half-Edge Representation for Mesh-like PShapes

**PMesh** is a Java library that provides a robust half-edge data structure for representing and manipulating mesh-like PShape objects in Processing.

The core idea behind PMesh is to **separate the topology** (how vertices, edges, and faces are connected) from the **geometry** (the actual coordinates of the vertices). This separation of concerns allows you to perform operations like smoothing, subdivision, or deformation without worrying about accidentally altering the mesh's connectivity. For example, when smoothing a mesh, you can move vertices based on their connected neighbors, knowing that the underlying structure of the mesh remains intact.

In PMesh, the topology is represented using a **half-edge data structure**, where each edge is split into two directed half-edges. These half-edges form counter-clockwise cycles (via their `.next` pointers) that **implicitly** define the faces of the mesh. This means that faces are emergent from the connectivity of the half-edges, rather than being explicitly stored.

By keeping topology and geometry distinct, PMesh ensures that changes to vertex positions don't disrupt the mesh's structure, making it a powerful tool for mesh processing tasks.

## Features

- **Half-Edge Data Structure**: Efficiently represents the topology of a mesh using half-edges, which are useful for navigating and manipulating mesh connectivity.
- **Topological Operations**: 🚧Supports operations like vertex and edge deletion, face traversal, and boundary detection🚧.
- **Smoothing Algorithms**: Includes an abstract base class (`MeshSmoother`) for implementing various mesh smoothing algorithms.
- **Graph Representation**: Converts the mesh into an undirected graph (weighted or unweighted) for further analysis or processing.

## Usage

### Creating a PMesh

To create a `PMesh` from a `PShape`, you can use the following code:

```
PMesh mesh = new PMesh(meshShape);
```

### Accessing Mesh Elements

You can access the vertices, edges, and faces of the mesh using the provided getter methods:

```java
List<HEVertex> vertices = mesh.getVertices(); // HEVertices have an underlying `position` variable.
List<HalfEdge> edges = mesh.getEdges();
List<HEFace> faces = mesh.getFaces();
```

### Smoothing the Mesh

To smooth the mesh use one of the built-in smoothers, or implement a custom `MeshSmoother` and apply it to the mesh.

```java
// Usage
MeshSmoother smoother = new SimultaneousLaplacianSmoother(mesh);
smoother.smooth(10, true); // Smooth for 10 iterations, preserving the boundary
```

### Converting to PShape

You can convert the `PMesh` back to a `PShape` for rendering:

```java
PShape smoothedShape = mesh.toPShape();
shape(smoothedShape);
```

This method extracts the updated vertex positions from the PMesh and reconstructs the mesh as a PShape, also preserving the existing topology.

### Graph Representation

The mesh can also be represented as a [jGraphT](https://jgrapht.org/) graph for further analysis:

```java
Graph<HEVertex, HalfEdge> graph = mesh.toUnweightedGraph();
// Or, for a weighted graph:
Graph<HEVertex, HalfEdge> weightedGraph = mesh.toWeightedGraph(he -> he.length()); // with a user-supplied edge weight function
```
