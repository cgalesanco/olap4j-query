package es.cgalesanco.olap4j.query;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import org.olap4j.Axis;
import org.olap4j.OlapException;
import org.olap4j.mdx.CallNode;
import org.olap4j.mdx.IdentifierSegment;
import org.olap4j.mdx.ParseTreeNode;
import org.olap4j.mdx.Syntax;
import org.olap4j.metadata.Hierarchy;
import org.olap4j.metadata.Member;

import es.cgalesanco.olap4j.query.Selection.Operator;
import es.cgalesanco.olap4j.query.Selection.Sign;
import es.cgalesanco.olap4j.query.SelectionTree.VisitingInfo;
import es.cgalesanco.olap4j.query.mdx.Mdx;
import es.cgalesanco.olap4j.query.mdx.UnionBuilder;

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
		selectionTree = new SelectionTree();
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
			VisitingInfo info = createVisitInfo(member);
			if (member.equals(info.getMember()))
				return info.getEffectiveSign(Operator.MEMBER) == Sign.INCLUDE;
			Member parent = member.getParentMember();
			if (parent != null && parent.equals(info.getMember())) {
				Sign s = info.getNode().getStatus()
						.getSelectionSign(Operator.CHILDREN);
				if (s != null)
					return s == Sign.INCLUDE;
			}
			return info.getEffectiveSign(Operator.DESCENDANTS) == Sign.INCLUDE;
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

		VisitingInfo info = createVisitInfo(member);
		if (member.equals(info.getMember()))
			return info.getEffectiveSign(Operator.MEMBER) == Sign.EXCLUDE;
		Member parent = member.getParentMember();
		if (parent != null && parent.equals(info.getMember())) {
			Sign s = info.getNode().getStatus()
					.getSelectionSign(Operator.CHILDREN);
			if (s != null)
				return s == Sign.EXCLUDE;
		}
		return info.getEffectiveSign(Operator.DESCENDANTS) == Sign.EXCLUDE;
	}

	/**
	 * <p>
	 * Lists the effective selections for this QueryHierarchy.The returned list
	 * is the result of optimizing the sequence of include/exclude operations
	 * </p>
	 * 
	 * @return the effective selections for this Query.
	 */
	public List<Selection> listSelections() {
		List<Selection> result = new ArrayList<Selection>();
		for (SelectionTree root : selectionTree.getOverridingChildren())
			listSelections(result, root);
		return result;
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
		int childMemberCount = member.getChildMemberCount();
		if (childMemberCount == 0)
			return true;

		VisitingInfo visitInfo = createVisitInfo(member);
		Member visitMember = visitInfo.getMember();
		if (visitMember == null) {
			return false;
		}
		if (!visitMember.equals(member)) {
			return visitInfo.getDefaultSign() != Sign.INCLUDE;
		}

		return !hasChildren(visitInfo);
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
		VisitingInfo info = createVisitInfo(m);
		if (m.equals(info.getMember())) {
			return info.getEffectiveSign(op);
		}

		switch (op) {
		case DESCENDANTS:
		case CHILDREN:
			return info.getEffectiveSign(Operator.DESCENDANTS);
		case MEMBER:
			if (m.getParentMember() != null
					&& m.getParentMember().equals(info.getMember())) {
				return info.getEffectiveSign(Operator.CHILDREN);
			}
			return info.getEffectiveSign(Operator.DESCENDANTS);
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
	 * generates the filter expression, ignoring the {@code drill} parameter.
	 * </p>
	 * 
	 * @param drills
	 *            list of drilled members.
	 * @return the parse tree expressing this hierarchy set of members for the
	 *         given drill list.
	 */
	public ParseTreeNode toOlap4j(List<Member> drills) {
		if (getAxis().getLocation() == Axis.FILTER)
			return toOlap4jFilter();

		List<Member> drillList = new LinkedList<Member>();
		if (drills != null)
			drillList.addAll(drills);

		return toOlap4jQuery(drillList);
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
	 * @see #toOlap4j(List)
	 * @return the parse tree expression for the root members of this query
	 *         hierarchy.
	 */
	public ParseTreeNode toOlap4j() {
		return toOlap4j(null);
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
			applyBasic(new SelectionAction(action.getMember(),
					action.getSign(), Operator.DESCENDANTS));
		} else {

			switch (action.getOperator()) {
			case INCLUDE_CHILDREN:
				applyBasic(new SelectionAction(action.getMember(),
						action.getSign(), Operator.CHILDREN));
				applyBasic(new SelectionAction(action.getMember(),
						action.getSign(), Operator.MEMBER));
				break;

			case SIBLINGS:
				break;
			case ANCESTORS:
				break;

			default:
				applyBasic(action);
			}
		}
	}

	/**
	 * <p>
	 * Implements include/exclude for basic selection actions
	 * </p>
	 * <p>
	 * Basic selections actions use only {@link Selection.Operator.MEMBER},
	 * {@link Selection.Operator.CHILDREN} and
	 * {@link Selection.Operator.DESCENDANTS} operators.
	 * </p>
	 * 
	 * @param action
	 *            selection action.
	 */
	void applyBasic(SelectionAction action) {
		Stack<Member> path = createPathTo(action.getMember());
		SelectionTree.VisitingInfo memberInfo = createVisitInfo(path);

		// Compute the parent node, if it's found in the selection tree
		SelectionTree parent = memberInfo.getParent();
		if (path.size() > 0)
			parent = path.size() == 1 ? memberInfo.getNode() : null;

		// Avoids creating tree nodes when this action is a no-op
		if (parent != selectionTree && path.size() > 0) {
			switch (action.getOperator()) {
			case MEMBER:
				if (parent != null) {
					Sign parentSign = parent.getStatus().getSelectionSign(
							Operator.CHILDREN);
					if (parentSign == action.getSign())
						return;
					if (parentSign == null
							&& memberInfo
									.getEffectiveSign(Operator.DESCENDANTS) == action
									.getSign())
						return;
				} else {
					if (memberInfo.getEffectiveSign(Operator.DESCENDANTS) == action
							.getSign()) {
						return;
					}
				}
				break;

			case CHILDREN:
				if (memberInfo.getEffectiveSign(Operator.DESCENDANTS) == action
						.getSign()) {
					return;
				}
				break;

			case DESCENDANTS:
				if (parent != null) {
					Sign parentSign = parent.getStatus().getSelectionSign(
							Operator.CHILDREN);
					if (parentSign == null) {
						if (memberInfo.getEffectiveSign(Operator.DESCENDANTS) == action
								.getSign())
							return;
					} else {
						if (parentSign == action.getSign()
								&& memberInfo
										.getEffectiveSign(Operator.DESCENDANTS) == action
										.getSign())
							return;
					}
				} else {
					if (memberInfo.getEffectiveSign(Operator.DESCENDANTS) == action
							.getSign())
						return;
				}
				break;

			}
		}

		// Creates necesarry tree nodes including the one corresponding to this
		// action
		while (!path.isEmpty()) {
			memberInfo = memberInfo.visitChild(memberInfo.getNode()
					.createOverridingChild(path.pop()));
		}
		if (action.getOperator() == Operator.MEMBER
				&& action.getSign() == memberInfo
						.getEffectiveSign(Operator.MEMBER))
			return;

		// TODO detect overriding every children of a member and replace the
		// inclusion.
		// e.g.: excluding every children MEMBER is equivalent to exclude
		// CHILDREN

		SelectionTree current = memberInfo.getNode();
		MemberSelectionState currentSt = current.getStatus();
		currentSt.apply(action.getSign(), action.getOperator());

		if (action.getOperator() == Operator.DESCENDANTS) {
			current.getOverridingChildren().clear();
		} else {
			if (action.getOperator() == Operator.CHILDREN) {
				Iterator<SelectionTree> itChild = current
						.getOverridingChildren().iterator();
				while (itChild.hasNext()) {
					SelectionTree child = itChild.next();
					MemberSelectionState childSt = child.getStatus();
					childSt.clear(Operator.MEMBER);
					if (child.isVoid())
						itChild.remove();
				}
			}
		}
	}

	/**
	 * Method used to implement {@link #isLeaf(Member)}.
	 * 
	 * @param visit
	 *            selection tree visit information.
	 * @return true if the member associated to {@code visit} has at least one
	 *         included descendant in this hierarchy.
	 * @throws OlapException
	 *             If testing descendants triggers an exception while looking up
	 *             members in the underlying cube.
	 */
	private boolean hasChildren(VisitingInfo visit) throws OlapException {
		// Stack of selection nodes pending to be processed.
		Stack<SelectionTree.VisitingInfo> pendingNodes = new Stack<SelectionTree.VisitingInfo>();
		pendingNodes.add(visit);

		while (!pendingNodes.isEmpty()) {
			SelectionTree.VisitingInfo node = pendingNodes.pop();

			Sign childrenDefaultSign = node.getEffectiveSign(Operator.CHILDREN);

			// Counts the number of children nodes excluded overriding this node
			// CHILDREN
			// exclusion. Used to detect the case of a CHILDREN inclusion
			// overrided by the
			// exclusion of every child MEMBER.
			int overridingExcludedCount = 0;
			for (SelectionTree override : node.getNode()
					.getOverridingChildren()) {
				SelectionTree.VisitingInfo childArgs = node
						.visitChild(override);
				Sign memberSign = childArgs.getEffectiveSign(Operator.MEMBER);
				if (memberSign == Sign.INCLUDE) {
					// We've found an included children, so this member has at
					// least a child.
					return true;
				} else {
					if (childrenDefaultSign == Sign.INCLUDE)
						overridingExcludedCount++;

					// This member is excluded, we should keep looking for
					// included descendants.
					pendingNodes.push(childArgs);
				}
			}

			if (childrenDefaultSign == Sign.INCLUDE) {
				// If children are included by default (and the exclussions does
				// not sum up the total number
				// of member children), this node do has children, so return
				// true.
				// In other case keep processing pending nodes.
				if (overridingExcludedCount < node.getMember()
						.getChildMemberCount())
					return true;
			} else {
				if (node.getNode().getOverridingChildren().size() == 0) {
					// Children are EXCLUDED and there is no overrding
					// descendant
					Sign descendantsSign = node
							.getEffectiveSign(Operator.DESCENDANTS);
					if (descendantsSign == Sign.EXCLUDE) {
						// If DESCENDANTS are excluded, there is no child,
						// return false.
						return false;
					}

					// DESCENDANTS are included, check to see if there is any
					// grandson
					Member m = node.getMember();
					if (m != null) {
						if (m.getChildMemberCount() == 0)
							return true;

						// TODO: avoid checking every child for non parent/child
						// hierarchies
						for (Member child : m.getChildMembers()) {
							if (child.getChildMemberCount() > 0)
								return true;
						}
					} else {
						// parent of root members, always return true
						return true;
					}
				}

			}

		}
		// None of the processed nodes had any children, so return false.
		return false;
	}

	/**
	 * Helper method to create visit information for a member.
	 * 
	 * @param member
	 *            member to visit.
	 * @return visit information for that member.
	 */
	private SelectionTree.VisitingInfo createVisitInfo(Member member) {
		return createVisitInfo(createPathTo(member));
	}

	/**
	 * Helper method to create visit information for a member, given its path
	 * from the root of the {@link org.olap4j.metadata.Hierarchy}.
	 * 
	 * @param path
	 *            path to the member.
	 * @return visit information for that member.
	 */
	private VisitingInfo createVisitInfo(Stack<Member> path) {
		SelectionTree.VisitingInfo visitInfo = new SelectionTree.VisitingInfo(
				selectionTree, null, Sign.EXCLUDE);
		while (!path.isEmpty()) {
			SelectionTree next = visitInfo.getNode().getOverridingChild(
					path.peek());
			if (next == null)
				break;

			visitInfo = visitInfo.visitChild(next);
			path.pop();
		}
		return visitInfo;
	}

	/**
	 * Immersion method to recursively generate the list of optimized
	 * inclusion/exclusion.
	 * 
	 * @param result
	 *            current list of selections.
	 * @param from
	 *            current selection node.
	 */
	private void listSelections(List<Selection> result, SelectionTree from) {
		result.addAll(from.listSelections());

		for (SelectionTree child : from.getOverridingChildren()) {
			listSelections(result, child);
		}
	}

	/**
	 * Helper method to create a stack of ancestor to this member.
	 * 
	 * @param member
	 *            the member.
	 * @return a stack with its ancestors.
	 */
	private Stack<Member> createPathTo(Member member) {
		Stack<Member> path = new Stack<Member>();
		while (member != null) {
			path.push(member);
			member = member.getParentMember();
		}
		return path;
	}

	/**
	 * Implementation of {@link #isIncluded(Member)} for filter axes.
	 */
	private boolean isIncludedFilter(Member m) {
		VisitingInfo info = createVisitInfo(m);
		if (m.equals(info.getMember())) {
			if (info.getNode().hasOverridingChildren())
				return false;
			return info.getEffectiveSign(Operator.DESCENDANTS) == Sign.INCLUDE;
		}

		return info.getEffectiveSign(Operator.DESCENDANTS) == Sign.INCLUDE;
	}

	/**
	 * Implementation of {@link #toOlap4j()} for filter axes.
	 */
	private ParseTreeNode toOlap4jFilter() {
		Sign selectionSign = selectionTree.getStatus().getEffectiveSign(
				Operator.DESCENDANTS, null);
		return toOlap4jFilter(selectionTree, selectionSign);
	}

	/**
	 * Immersion method to recursively generate the parse tree node for filter
	 * axes.
	 * 
	 * @param selectionNode
	 *            current node
	 * @param defaultSign
	 *            default descendants sign at the current node.
	 * @return the parse tree.
	 */
	private ParseTreeNode toOlap4jFilter(SelectionTree selectionNode,
			Sign defaultSign) {
		Sign selectionSign = selectionNode.getStatus().getEffectiveSign(
				Operator.DESCENDANTS, defaultSign);

		if (!selectionNode.hasOverridingChildren()) {
			// Current node has no overriding children, its filter expression is
			// the corresponding MemberNode if the member is included, void in
			// other case.
			if (selectionSign == Sign.INCLUDE)
				return Mdx.member(selectionNode.getMember());
			else
				return null;
		} else {
			// Current node has overriding children

			UnionBuilder finalExpression = new UnionBuilder();
			if (selectionSign == Sign.INCLUDE) {
				// Current node is included, so overriding children are excluded
				// or have excluded descendants.

				UnionBuilder overridingChildren = new UnionBuilder();
				for (SelectionTree overriding : selectionNode
						.getOverridingChildren()) {
					overridingChildren.add(Mdx.member(overriding.getMember()));
					finalExpression.add(toOlap4jFilter(overriding,
							selectionSign));
				}

				// Return the set of non overriding children plus recursive
				// expression evaluations
				finalExpression.add(Mdx.except(
						Mdx.children(selectionNode.getMember()),
						overridingChildren.getUnionNode()));
			} else {
				// Current node is excluded, returns the union of recursive
				// evaluation for overriding children.
				for (SelectionTree overriding : selectionNode
						.getOverridingChildren()) {
					finalExpression.add(toOlap4jFilter(overriding,
							selectionSign));
				}
			}
			return finalExpression.getUnionNode();
		}
	}

	/**
	 * Implementation of {@code #toOlap4j()} for query axes.
	 */
	private ParseTreeNode toOlap4jQuery(List<Member> drillList) {
		SelectionTree.VisitingInfo visitInfo = new SelectionTree.VisitingInfo(
				selectionTree, null, Sign.EXCLUDE);
		UnionBuilder roots = new UnionBuilder();
		UnionBuilder drillExpansion = new UnionBuilder();
		UnionBuilder exceptList = new UnionBuilder();
		for (SelectionTree root : selectionTree.getOverridingChildren()) {
			VisitingInfo rootVisit = visitInfo.visitChild(root);
			toOlap4jQuery(true, rootVisit, drillList, roots, drillExpansion,
					exceptList);
		}

		if (roots.getUnionNode() == null)
			return new CallNode(null, "{}", Syntax.Braces);
		return Mdx.except(Mdx.drillDown(roots, drillExpansion),
				exceptList.getUnionNode());
	}

	/**
	 * Immersion method to recursively implement {@link #toOlap4j()} for query
	 * axes.
	 * 
	 * @param dangling
	 *            indicates that this member has not been included because of a
	 *            <code>DrillDownMember</code>
	 * @param current
	 *            visit info for the current selection node
	 * @param drillList
	 *            list of drills pending to be executed
	 * @param roots
	 *            union expression for the root set.
	 * @param drillExpansion
	 *            union expression for the members to be drilled.
	 * @param exceptList
	 *            union expression for the members to be removed after the
	 *            drill.
	 */
	private void toOlap4jQuery(boolean dangling, VisitingInfo current,
			List<Member> drillList, UnionBuilder roots,
			UnionBuilder drillExpansion, UnionBuilder exceptList) {
		Member currentMember = current.getMember();
		SelectionTree currentNode = current.getNode();
		Sign currentSign = current.getEffectiveSign(Operator.MEMBER);

		if (currentSign == Sign.INCLUDE) {
			if (dangling)
				roots.add(Mdx.member(currentMember));

			// This member is included if it's not drilled, ends this visit
			// branch
			// else remove it from the drillList
			if (!drillList.remove(currentMember))
				return;
		}

		Sign childrenSign = current.getEffectiveSign(Operator.CHILDREN);
		Sign descendantsSign = current.getEffectiveSign(Operator.DESCENDANTS);
		if (childrenSign == Sign.INCLUDE) {
			// As children are included by default, drill on this member
			if (currentSign == Sign.INCLUDE)
				drillExpansion.add(Mdx.member(currentMember));
			else {
				roots.add(Mdx.children(currentMember));
			}

			for (SelectionTree childNode : currentNode.getOverridingChildren()) {
				VisitingInfo childVisit = current.visitChild(childNode);
				Sign childMemberSign = childVisit
						.getEffectiveSign(Operator.MEMBER);
				if (childMemberSign == Sign.EXCLUDE) {
					exceptList.add(Mdx.member(childNode.getMember()));
					drillList.remove(childNode.getMember());
				}
				toOlap4jQuery(childMemberSign == Sign.EXCLUDE, childVisit,
						drillList, roots, drillExpansion, exceptList);
			}

			// Children out of the overridingChildren set ar included,
			// so we should apply any drill for children (or descendants if
			// descendants are included) of this node.
			if (descendantsSign == Sign.INCLUDE)
				applyDrills(currentMember, drillList, drillExpansion);
		} else {
			List<Member> overrided = new ArrayList<Member>();
			for (SelectionTree childNode : currentNode.getOverridingChildren()) {
				VisitingInfo childVisit = current.visitChild(childNode);
				Sign childMemberSign = childVisit
						.getEffectiveSign(Operator.MEMBER);
				if (childMemberSign == Sign.EXCLUDE)
					drillList.remove(childVisit.getMember());
				overrided.add(childVisit.getMember());
				toOlap4jQuery(true, childVisit, drillList, roots,
						drillExpansion, exceptList);
			}

			if (descendantsSign == Sign.INCLUDE) {
				if (overrided.isEmpty())
					roots.add(Mdx.descendants(Mdx.member(currentMember), 2));
				else {
					roots.add(Mdx.descendants(Mdx.except(
							Mdx.children(currentMember),
							UnionBuilder.fromMembers(overrided)), 1));
				}

				List<Member> grandsonDrillCandidates = new ArrayList<Member>();
				for (Member m : drillList) {
					Member drillParent = m.getParentMember();
					if (drillParent != null
							&& currentMember.equals(drillParent
									.getParentMember()))
						grandsonDrillCandidates.add(drillParent);
				}

				for (Member m : grandsonDrillCandidates) {
					applyDrills(m, drillList, drillExpansion);
				}
			}
		}
	}

	/**
	 * <p>
	 * Helper function to recursively apply drills for descendants of a member.
	 * </p>
	 * <p>
	 * Recursively search for descendants of {@code member} included in the list
	 * of drills. Adds them to {@code drillExpansion} and removes them from
	 * {@code drillList},
	 * </p>
	 * 
	 * @param member
	 *            initial member
	 * @param drillList
	 *            list of drills to be applied.
	 * @param drillExpansion
	 *            union expression with the member to be drilled.
	 */
	private void applyDrills(Member member, List<Member> drillList,
			UnionBuilder drillExpansion) {
		Stack<Member> pending = new Stack<Member>();
		pending.push(member);
		while (!pending.isEmpty()) {
			Member currentMember = pending.pop();
			Iterator<Member> itMember = drillList.iterator();
			while (itMember.hasNext()) {
				Member m = itMember.next();
				if (currentMember.equals(m.getParentMember())) {
					drillExpansion.add(Mdx.member(m));
					itMember.remove();
					pending.push(m);
				}
			}
		}
	}

}
