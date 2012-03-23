package es.cgalesanco.olap4j.query;

import java.util.List;

import org.olap4j.Axis;
import org.olap4j.OlapException;
import org.olap4j.mdx.IdentifierSegment;
import org.olap4j.mdx.ParseTreeNode;
import org.olap4j.metadata.Hierarchy;
import org.olap4j.metadata.Level;
import org.olap4j.metadata.Member;

import es.cgalesanco.olap4j.query.Selection.Operator;
import es.cgalesanco.olap4j.query.Selection.Sign;
import es.cgalesanco.olap4j.query.SelectionTree.SelectionNode;

/**
 * <p>
 * Usage of a hierarchy for an OLAP query.
 * </p>
 * 
 * <p>
 * It references a {@link org.olap4j.metadata.Hierarchy} and allows the query
 * creator to manage the member selections for the hierarchy. The state of a
 * {@link QueryHierarchy} does not affect the
 * {@link org.olap4j.metadata.Hierarchy} object in any way so a single hierarchy
 * object can be referenced by many {@link QueryHierarchy} objects.
 * </p>
 * 
 * <p>
 * It differs from the original olap4j {@link org.olap4j.query.QueryDimension}
 * in that:
 * </p>
 * <ul>
 * <li>it references a {@link org.olap4j.metadata.Hierarchy} instead of a
 * {@link org.olap4j.metadata.QueryDimension}; supporting dimensions with
 * several hierarchies.</li>
 * <li>provides support for {@link QueryAxis} drilling</li>
 * <li><em>executes</em> inclusions and exclusions in calling order; the
 * original QueryDimension executes all the inclusions first and then all the
 * exclusions</li>
 * </ul>
 * 
 * @author César García
 * 
 */
public class QueryHierarchy {
	private final SelectionTree selectionTree;
	private final Hierarchy hierarchy;
	private final Query query;
	private QueryAxis axis;

	/**
	 * <p>
	 * Package protected constructor for {@link QueryHierarchy}. Creates a
	 * instance for the given query, using the given hierarchy.
	 * </p>
	 * 
	 * <p>
	 * Used by {@link Query#Query(String, org.olap4j.metadata.Cube)}
	 * </p>
	 * 
	 * @param query
	 *            the query creating this instance
	 * @param baseHierarchy
	 *            the hierarchy used for this QueryDimension
	 */
	QueryHierarchy(QueryAxis axis, Hierarchy baseHierarchy) {
		this.axis = axis;
		this.query = axis.getQuery();
		selectionTree = new SelectionTree(baseHierarchy.getLevels());
		hierarchy = baseHierarchy;
	}

	/**
	 * Returns the axis using this {@link QueryHierarchy}.
	 * 
	 * @return the axis using this {@link QueryHierarchy}, if any.
	 */
	public QueryAxis getAxis() {
		return axis;
	}

	/**
	 * Sets the axis using this {@link QueryHierarchy}.
	 * 
	 * @param axis
	 *            the axis using this {@link QueryHierarchy}.
	 */
	public void setAxis(QueryAxis axis) {
		this.axis = axis;
	}

	/**
	 * Returns the query owning this instance.
	 * 
	 * @return the Query owning this.instance.
	 */
	public Query getQuery() {
		return query;
	}

	/**
	 * Returns the hierarchy used by this instance.
	 * 
	 * @return the hierarchy used by this instance.
	 */
	public Hierarchy getHierarchy() {
		return hierarchy;
	}

	/**
	 * <p>
	 * Selects the members and includes them in the query.
	 * </p>
	 * 
	 * <p>
	 * This method selects and includes a member along with its relatives,
	 * depending on the supplied Selection.Operator operator.
	 * </p>
	 * 
	 * <p>
	 * If this hierarchy is in a {@link org.olap4j.Axis.Standard#FILTER} axis,
	 * the {@code operator} argument is ignored and
	 * {@link Selection.Operator#DESCENDANTS} is used instead; effectively
	 * including that member and its descendants.
	 * </p>
	 * 
	 * @param operator
	 *            Selection operator that defines what relatives of the supplied
	 *            members name to include along.
	 * @param nameParts
	 *            Name of the member to select and include.
	 */
	public Selection include(Operator operator,
			List<IdentifierSegment> nameParts) throws OlapException {
		Member m = getQuery().getCube().lookupMember(nameParts);
		return include(operator, m);
	}

	/**
	 * <p>
	 * Selects the members and includes them in the query.
	 * </p>
	 * 
	 * <p>
	 * This method selects and includes a member along with its relatives,
	 * depending on the supplied Selection.Operator operator.
	 * </p>
	 * 
	 * <p>
	 * If this hierarchy is in a {@link org.olap4j.Axis.Standard#FILTER} axis,
	 * the {@code operator} argument is ignored and
	 * {@link Selection.Operator#DESCENDANTS} is used instead; effectively
	 * including that member and its descendants.
	 * </p>
	 * 
	 * @param operator
	 *            Selection operator that defines what relatives of the supplied
	 *            members name to include along.
	 * @param member
	 *            The member to select and include in the query.
	 */
	public Selection include(Operator operator, Member member) {
		SelectionAction include = new SelectionAction(member, Sign.INCLUDE,
				operator);
		apply(include);
		return include;
	}

	public void include(Level level) {
		selectionTree.applyLevel(level, Sign.INCLUDE);
	}

	public void exclude(Level level) {
		selectionTree.applyLevel(level, Sign.EXCLUDE);
	}

	/**
	 * <p>
	 * Tests if a given member is included in the {@link QueryHierarchy}.
	 * </p>
	 * 
	 * <p>
	 * For hierarchies in an {@link org.olap4j.Axis.Standard#FILTER} axis, a
	 * member is included only if all of its descendants are also include.
	 * </p>
	 * 
	 * @param member
	 *            member to test for inclusion.
	 * @return {@code true} if this member is included in the hierarchy,
	 *         {@code false} otherwise.
	 */
	public boolean isIncluded(Member member) {
		if (getAxis().getLocation() == Axis.FILTER) {
			return isIncludedFilter(member);
		} else {
			return selectionTree.isIncluded(member);
		}
	}

	/**
	 * <p>
	 * Selects members and excludes them from the query.
	 * </p>
	 * <p>
	 * This method selects and excludes a member along with its relatives,
	 * depending on the supplied {@link Selection.Operator} operator.
	 * </p>
	 * <p>
	 * If this hierarchy is in a {@link org.olap4j.Axis.Standard#FILTER} axis,
	 * the {@code operator} argument is ignored and
	 * {@link Selection.Operator#DESCENDANTS} is used instead; effectively
	 * excluding that member and its descendants.
	 * </p>
	 * 
	 * @param operator
	 *            Selection operator that defines what relatives of the supplied
	 *            member name to exclude along.
	 * @param nameParts
	 * @return Name of the root member to select and exclude.
	 * @throws OlapException
	 *             If no member corresponding to the supplied name parts could
	 *             be resolved in the cube.
	 */
	public Selection exclude(Operator operator,
			List<IdentifierSegment> nameParts) throws OlapException {
		Member m = query.getCube().lookupMember(nameParts);
		return exclude(operator, m);
	}

	/**
	 * <p>
	 * Selects members and excludes them from the query.
	 * </p>
	 * <p>
	 * This method selects and excludes a member along with its relatives,
	 * depending on the supplied {@link Selection.Operator} operator.
	 * </p>
	 * <p>
	 * If this hierarchy is in a {@link org.olap4j.Axis.Standard#FILTER} axis,
	 * the {@code operator} argument is ignored and
	 * {@link Selection.Operator#DESCENDANTS} is used instead; effectively
	 * excluding that member and its descendants.
	 * </p>
	 * 
	 * @param operator
	 *            Selection operator that defines what relatives of the supplied
	 *            members name to exclude along.
	 * @param member
	 *            The member to select and exclude in the query.
	 */
	public Selection exclude(Operator operator, Member member)
			throws OlapException {
		SelectionAction exclude = new SelectionAction(member, Sign.EXCLUDE,
				operator);
		apply(exclude);
		return exclude;
	}

	/**
	 * <p>
	 * Tests if a member is excluded from the hierarchy.
	 * </p>
	 * <p>
	 * If this hierarchy is NOT in a {@link org.olap4j.Axis.Standard#FILTER}
	 * axis, its equivalento to {@code !isIncluded(member)}.
	 * </p>
	 * <p>
	 * For hierarchies in a {@link org.olap4j.Axis.Standard#FILTER} axis, this
	 * methods return {@code true} if this member and all of its descendants are
	 * exclude; {@code false} otherwise.
	 * </p>
	 * 
	 * @param member
	 *            The member to select and exclude from the query
	 * @return {@code true} if the member is exclude, {@code false}
	 *         otherwise.</p>
	 */
	public boolean isExcluded(Member member) {
		if (getAxis().getLocation() != Axis.FILTER)
			return !isIncluded(member);

		SelectionNode info = selectionTree.find(member);
		if (member.equals(info.getMember()))
			return info.getMemberSign() == Sign.EXCLUDE;
		Member parent = member.getParentMember();
		if (parent != null && parent.equals(info.getMember())) {
			return info.getChildrenSign() == Sign.EXCLUDE;
		}
		return info.getDefaultSign() == Sign.EXCLUDE;
	}

	/**
	 * <p>
	 * Tests if a given member is a leaf in the current query hierarchy, e.g. it
	 * has no descendant included in the query hierarchy.
	 * </p>
	 * 
	 * @param member
	 *            member to test.
	 * @return {@code true} if this member has no descendant included in the
	 *         query hierarchy. {@code false} otherwise.
	 * @throws OlapException
	 *             If testing descendants triggers an exception while looking up
	 *             members in the underlying cube.
	 */
	public boolean isLeaf(Member member) throws OlapException {
		return selectionTree.isLeaf(member);
	}

	/**
	 * Clears any previous include/exclude operation, returning the instance to
	 * its initial state.
	 */
	public void clear() {
		selectionTree.clear();
	}

	public Sign getEffectiveSignAt(Member m, Operator op) {
		// TODO: check if this method is really needed.
		SelectionNode info = selectionTree.find(m);
		switch (op) {
		case DESCENDANTS:
			return info.getDefaultSign();
			
		case CHILDREN:
			if ( m.equals(info.getMember()))
				return info.getChildrenSign();
			return info.getDefaultSign();
			
		case MEMBER:
			if ( m.equals(info.getMember()))
				return info.getMemberSign();
			
			if (m.getParentMember() != null
					&& m.getParentMember().equals(info.getMember())) {
				return info.getChildrenSign();
			}
			return info.getDefaultSign();
		}
		return null;
	}

	/**
	 * <p>
	 * For query hierarchies returns the parse tree expressing the set of
	 * visible members of this hierarchy, given a list of drilled members.
	 * <p>
	 * <p>
	 * For hierarchies in a {@link org.olap4j.Axis.Standard#FILTER} axis,
	 * generates the filter expression, ignoring any parameter.
	 * </p>
	 * 
	 * @param expander
	 *            helper object to execute drills.
	 * @param drills
	 *            list of drilled members.
	 * @return the parse tree expressing this hierarchy set of members for the
	 *         given drill list.
	 */
	ParseTreeNode toOlap4j(HierarchyExpander expander) {
		if (getAxis().getLocation() == Axis.FILTER)
			return selectionTree.toOlap4jFilter();

		return selectionTree.toOlap4jQuery(expander);
	}

	/**
	 * <p>
	 * For query hierarchies, the onvenience method to get the parse tree
	 * expression for the root members of this query hierarchy.
	 * </p>
	 * <p>
	 * For filter hierarchies returns the filter expression for this hierarchy
	 * </p>
	 * 
	 * @see #toOlap4j(HierarchyExpander, List)
	 * @return the parse tree expression for the root members of this query
	 *         hierarchy.
	 */
	public ParseTreeNode toOlap4j() {
		return toOlap4j(new HierarchyExpander());
	}

	/**
	 * <p>
	 * Implements include/exclude given the corresponding
	 * {@link SelectionAction}. Generates the equivalte basic selection action
	 * and delgates onto {@link #applyBasic(SelectionAction)}.
	 * </p>
	 * 
	 * @param action
	 *            selection action.
	 */
	void apply(SelectionAction action) {

		if (getAxis().getLocation() == Axis.FILTER) {
			selectionTree.applyBasic(new SelectionAction(action.getMember(),
					action.getSign(), Operator.DESCENDANTS));
		} else {

			switch (action.getOperator()) {
			case INCLUDE_CHILDREN:
				selectionTree.applyBasic(new SelectionAction(action.getMember(),
						action.getSign(), Operator.CHILDREN));
				selectionTree.applyBasic(new SelectionAction(action.getMember(),
						action.getSign(), Operator.MEMBER));
				break;

			case SIBLINGS:
				break;
			case ANCESTORS:
				break;

			default:
				selectionTree.applyBasic(action);
			}
		}
	}

	/**
	 * Implementation of {@link #isIncluded(Member)} for filter axes.
	 */
	private boolean isIncludedFilter(Member m) {
		SelectionNode info = selectionTree.find(m);
		if (m.equals(info.getMember())) {
			if (info.hasOverridingChildren())
				return false;
			return info.getDefaultSign() == Sign.INCLUDE;
		}

		return info.getDefaultSign() == Sign.INCLUDE;
	}

	public List<Selection> listSelections() {
		return selectionTree.listSelections();
	}

}
