package es.cgalesanco.olap4j.query;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.olap4j.Axis;
import org.olap4j.CellSet;
import org.olap4j.OlapConnection;
import org.olap4j.OlapException;
import org.olap4j.OlapStatement;
import org.olap4j.mdx.AxisNode;
import org.olap4j.mdx.CubeNode;
import org.olap4j.mdx.IdentifierNode;
import org.olap4j.mdx.ParseTreeNode;
import org.olap4j.mdx.SelectNode;
import org.olap4j.metadata.Cube;
import org.olap4j.metadata.Hierarchy;
import org.olap4j.metadata.NamedList;

/**
 * OLAP Query object. Mimics the original olap4j {@link org.olap4j.query.Query}
 * class.
 * 
 * @author César García
 * 
 */
public class Query {
	private Cube cube;
	private Map<String, QueryHierarchy> hierarchyMap;
	private Map<Axis, QueryAxis> axes;
	private String name;

	/**
	 * Constructs a query against a given cube.
	 * 
	 * @param name
	 *            Any arbitrary name to give to this query.
	 * @param cube
	 *            A Cube object against which to build a query.
	 */
	public Query(String name, Cube cube) {
		this.cube = cube;
		this.name = name;
		axes = new HashMap<Axis, QueryAxis>(4);
		QueryAxis unused = new QueryAxis(this, null);
		axes.put(null, unused);
		axes.put(Axis.ROWS, new QueryAxis(this, Axis.ROWS));
		axes.put(Axis.COLUMNS, new QueryAxis(this, Axis.COLUMNS));
		axes.put(Axis.FILTER, new QueryAxis(this, Axis.FILTER));

		NamedList<Hierarchy> hierarchies = cube.getHierarchies();
		hierarchyMap = new HashMap<String, QueryHierarchy>(hierarchies.size());
		for (Hierarchy h : hierarchies) {
			QueryHierarchy qd = new QueryHierarchy(unused, h);
			hierarchyMap.put(h.getName(), qd);
			unused.addHierarchy(qd);
		}
	}

	/**
	 * Returns the underlying cube object that is used to query against.
	 * 
	 * @return The Olap4j's Cube object.
	 */
	public Cube getCube() {
		return cube;
	}

	/**
	 * Returns this query's name. There is no guarantee that it is unique and is
	 * set at object instanciation.
	 * 
	 * @return This query's name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the query axis for a given axis type. If you pass axis=null,
	 * returns a special axis that is used to hold all unused hierarchies. (We
	 * may change this behavior in future.)
	 * 
	 * @param axis
	 *            Axis Type.
	 * @return Query axis.
	 */
	public QueryAxis getAxis(Axis axis) {
		return axes.get(axis);
	}

	/**
	 * Returns the Olap4j's Dimension object according to the name given as a
	 * parameter. If no dimension of the given name is found, a null value will
	 * be returned.
	 * 
	 * @param name
	 *            The name of the dimension you want the object for.
	 * @return The dimension object, null if no dimension of that name can be
	 *         found.
	 */
	public QueryHierarchy getHierarchy(String name) {
		return hierarchyMap.get(name);
	}

	/**
	 * Returns a map of the current query's axis. Be aware that modifications to
	 * this list might have unpredictable consequences.
	 * 
	 * @return A standard Map object that represents the current query's axis.
	 */
	public Map<Axis, QueryAxis> getAxes() {
		return axes;
	}

	/**
	 * Returns the fictional axis into which all unused dimensions are stored.
	 * All dimensions included in this axis will not be part of the query.
	 * 
	 * @return The QueryAxis representing dimensions that are currently not used
	 *         inside the query.
	 */
	public QueryAxis getUnusedAxis() {
		return axes.get(null);
	}

	/**
	 * Returns the MDX parse tree behind this Query. The returned object is
	 * generated for each call to this function. Altering the returned
	 * SelectNode object won't affect the query itself.
	 * 
	 * @return A SelectNode object representing the current query structure.
	 * @throws OlapException
	 */
	public SelectNode getSelect() throws OlapException {
		List<AxisNode> axisList = new ArrayList<AxisNode>();
		axisList.add(axes.get(Axis.COLUMNS).toOlap4j());

		AxisNode tmpAxis = axes.get(Axis.ROWS).toOlap4j();
		if (tmpAxis != null)
			axisList.add(tmpAxis);

		AxisNode filterAxis = null;

		QueryAxis slicerAxis = axes.get(Axis.FILTER);
		if (slicerAxis.getHierarchies().size() > 0)
			filterAxis = slicerAxis.toOlap4j();

		SelectNode select = new SelectNode(null,
				new ArrayList<ParseTreeNode>(), axisList, new CubeNode(null,
						cube), filterAxis, new ArrayList<IdentifierNode>());
		return select;
	}

	/**
	 * Executes the query against the current OlapConnection and returns a
	 * CellSet object representation of the data.
	 * 
	 * @return A proper CellSet object that represents the query execution
	 *         results.
	 * @throws OlapException
	 *             If something goes sour, an OlapException will be thrown to
	 *             the caller. It could be caused by many things, like a stale
	 *             connection. Look at the root cause for more details.
	 */
	public CellSet execute() throws OlapException {
		OlapConnection connection;
		try {
			connection = cube.getSchema().getCatalog().getMetaData()
					.getConnection();
		} catch (SQLException e) {
			throw new OlapException("Cannot execute query", e);
		}
		OlapStatement stmt = connection.createStatement();
		SelectNode select = getSelect();
		System.out.println(select);
		return stmt.executeOlapQuery(select);
	}

	/**
	 * Interchanges the contents of {@code ROWS} and {@code COLUMNS} axes.
	 */
	public void swapAxes() {
		QueryAxis rows = axes.get(Axis.ROWS);
		QueryAxis columns = axes.get(Axis.COLUMNS);

		rows.setLocation(Axis.COLUMNS);
		columns.setLocation(Axis.ROWS);
		axes.put(Axis.ROWS, columns);
		axes.put(Axis.COLUMNS, rows);
	}

}
