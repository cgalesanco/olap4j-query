package es.cgalesanco.olap4j.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Stack;
import java.util.TreeMap;

import org.olap4j.OlapException;
import org.olap4j.mdx.ParseTreeNode;
import org.olap4j.metadata.Level;
import org.olap4j.metadata.Member;

import es.cgalesanco.olap4j.query.Selection.Operator;
import es.cgalesanco.olap4j.query.Selection.Sign;
import es.cgalesanco.olap4j.query.mdx.Mdx;
import es.cgalesanco.olap4j.query.mdx.UnionBuilder;

class SelectionTree {
	private SelectionNode root;
	private List<Level> levels;
	private NavigableMap<Level, SelectionInfo> levelSelections;
	private int currentSequence;
	
	private static class LevelComparator implements Comparator<Level> {

		@Override
		public int compare(Level o1, Level o2) {
			return o1.getDepth() - o2.getDepth();
		}

	}

	
	public SelectionTree(List<Level> levels) {
		root = new SelectionNode();
		this.levels = levels;
		levelSelections = new TreeMap<Level, SelectionInfo>(new LevelComparator());
		currentSequence = 0;

	}

	/**
	 * The tree of members used to store the selection state of a
	 * QueryHierarchy.
	 * 
	 * @author César García
	 * 
	 */
	class SelectionNode {

		private MemberSelectionState selectionState;
		private Member member;
		private List<SelectionNode> overridingChildren;
		private SelectionNode parent;
		private int sequence;

		/**
		 * Constructs selection tree. The resulting tree has no members
		 * selected.
		 */
		public SelectionNode() {
			selectionState = new MemberSelectionState();
			selectionState.exclude(Operator.DESCENDANTS);
			overridingChildren = new ArrayList<SelectionNode>();
			parent = null;
		}

		protected SelectionNode(SelectionNode parent, Member m) {
			member = m;
			selectionState = new MemberSelectionState();
			overridingChildren = new ArrayList<SelectionNode>();
			this.parent = parent;
		}

		/**
		 * Returns the member of this node.
		 * 
		 * @return the member of this node.
		 */
		public Member getMember() {
			return member;
		}

		public int getSequence() {
			return sequence;
		}

		public void setSequence(int s) {
			sequence = s;
		}

		/**
		 * Returns the selection state for this node.
		 * 
		 * @return the selection state for this node.
		 */
		public MemberSelectionState getStatus() {
			return selectionState;
		}

		/**
		 * Returns a value indicating if this node is superflous. i.e. does not
		 * define any state information for its member and has no overriding
		 * children.
		 * 
		 * @return a value indicating if this node is superflous.
		 */
		public boolean isVoid() {
			return overridingChildren.isEmpty() && getStatus().isNull();
		}

		/**
		 * Returns the child node for a member, if any.
		 * 
		 * @param m
		 *            the member to look for.
		 * @return the child selection node for the given member.
		 */
		public SelectionNode getOverridingChild(Member m) {
			for (SelectionNode child : overridingChildren)
				if (child.member.equals(m))
					return child;
			return null;
		}

		/**
		 * Creates an overriding child for a member
		 * 
		 * @param m
		 *            the member
		 * @return the new node.
		 */
		public SelectionNode createOverridingChild(Member m) {
			SelectionNode n = new SelectionNode(this, m);
			overridingChildren.add(n);
			return n;
		}

		/**
		 * Returns the lis of overriding children of this node.
		 * 
		 * @return the list of overriding children of this node.
		 */
		public List<SelectionNode> getOverridingChildren() {
			return overridingChildren;
		}

		/**
		 * Generates a list of Selections producing the current state of this
		 * node.
		 * 
		 * @return the list of Selections.
		 */
		public Collection<? extends Selection> listSelections() {
			List<Selection> selections = new ArrayList<Selection>();

			Sign sign = getStatus().getSelectionSign(Operator.DESCENDANTS);
			if (sign != null)
				selections.add(new SelectionAction(getMember(), sign,
						Operator.DESCENDANTS));

			Sign childSign = getStatus().getSelectionSign(Operator.CHILDREN);
			Sign memberSign = getStatus().getSelectionSign(Operator.MEMBER);
			if (childSign != null) {
				if (childSign == memberSign) {
					selections.add(new SelectionAction(getMember(), childSign,
							Operator.INCLUDE_CHILDREN));
					memberSign = null;
				} else
					selections.add(new SelectionAction(getMember(), childSign,
							Operator.CHILDREN));
			}

			if (memberSign != null) {
				selections.add(new SelectionAction(getMember(), memberSign,
						Operator.MEMBER));
			}

			return selections;
		}

		public boolean hasOverridingChildren() {
			return overridingChildren != null && !overridingChildren.isEmpty();
		}

		public void clear() {
			selectionState = new MemberSelectionState();
			selectionState.exclude(Operator.DESCENDANTS);
			for (SelectionNode child : overridingChildren) {
				child.parent = null;
			}
			overridingChildren.clear();
		}

		public Sign getEffectiveSign(Operator op) {
			if (op == Operator.MEMBER && parent != null) {
				Sign def = parent.getStatus().getSelectionSign(
						Operator.CHILDREN);
				if (def != null)
					return selectionState.getEffectiveSign(op, def);
			}

			Sign s = selectionState.getEffectiveSign(op, null);
			if (s == null) {
				return getDefaultSign();
			}
			return s;
		}

		public Sign getDefaultSign() {
			SelectionNode prev = this;
			Sign s = null;
			while (prev != null
					&& (s = prev.selectionState
							.getSelectionSign(Operator.DESCENDANTS)) == null) {
				prev = prev.parent;
			}
			return s;
		}

		public SelectionNode getParent() {
			return parent;
		}

		public void accept(SelectionNodeVisitor visitor) {
			if ( visitor.visitEnter(this) ) {
				for(SelectionNode child : overridingChildren) {
					child.accept(visitor);
				}
				visitor.visitLeave(this);
			}
		}

		public List<Member> getOverridedMembers() {
			List<Member> result = new ArrayList<Member>(overridingChildren.size());
			for(SelectionNode child : overridingChildren) {
				result.add(child.getMember());
			}
			return result;
		}

		public List<Level> getIncludedLevels() {
			List<Level> levels = new ArrayList<Level>();
			for (Entry<Level, SelectionInfo> eLevel : levelSelections.entrySet()) {
				if (eLevel.getValue().getSign() == Sign.INCLUDE && eLevel.getValue().getSequence() >= sequence)
					levels.add(eLevel.getKey());
			}
			return levels;
		}
		
		public List<Level> getExcludedLevels() {
			List<Level> levels = new ArrayList<Level>();
			for (Entry<Level, SelectionInfo> eLevel : levelSelections.entrySet()) {
				if (eLevel.getValue().getSign() == Sign.EXCLUDE && eLevel.getValue().getSequence() >= sequence)
					levels.add(eLevel.getKey());
			}
			return levels;
		}

	}

	public SelectionNode find(Member member) {
		Stack<Member> path = createPathTo(member);
		return find(path);
	}

	public SelectionNode find(Stack<Member> path) {
		SelectionNode current = root;
		while (!path.isEmpty()) {
			Member m = path.peek();
			SelectionNode next = current.getOverridingChild(m);
			if (next == null) {
				break;
			}

			current = next;
			path.pop();
		}
		return current;
	}

	private Stack<Member> createPathTo(Member member) {
		Stack<Member> path = new Stack<Member>();
		while (member != null) {
			path.push(member);
			member = member.getParentMember();
		}
		return path;
	}

	public void clear() {
		root.clear();
		levelSelections.clear();
		currentSequence = 0;
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
		SelectionListBuilder builder = new SelectionListBuilder(levelSelections);
		for (SelectionNode r : root.getOverridingChildren())
			listSelections(builder, r); 
		return builder.getResult();
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
	private void listSelections(SelectionListBuilder builder, SelectionNode from) {
		builder.addSelections(from);

		for (SelectionNode child : from.getOverridingChildren()) {
			listSelections(builder, child);
		}
	}

	/**
	 * Implementation of {@link #toOlap4j()} for filter axes.
	 */
	ParseTreeNode toOlap4jFilter() {
		Sign selectionSign = root.getStatus().getEffectiveSign(
				Operator.DESCENDANTS, null);
		return toOlap4jFilter(root, selectionSign);
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
	private ParseTreeNode toOlap4jFilter(SelectionNode selectionNode,
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
				for (SelectionNode overriding : selectionNode
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
				for (SelectionNode overriding : selectionNode
						.getOverridingChildren()) {
					finalExpression.add(toOlap4jFilter(overriding,
							selectionSign));
				}
			}
			return finalExpression.getUnionNode();
		}
	}

	/**
	 * Implementation of {@link #toOlap4j()} for query axes.
	 */
	ParseTreeNode toOlap4jQuery(HierarchyExpander expander,
			List<Member> drillList) {
		if ( expander.isHierarchyExpanded() ) {
			HierarchyExpanderVisitor visitor = new HierarchyExpanderVisitor(drillList, levels);
			root.accept(visitor);
			return visitor.getExpression();
		} else {
			HierarchyDrillerVisitor visitor = new HierarchyDrillerVisitor(drillList, levels);
			root.accept(visitor);
			return visitor.getExpression();
		}
	}

	private List<Level> getIncludedLevels(int sequence) {
		List<Level> levels = new ArrayList<Level>();
		for (Entry<Level, SelectionInfo> eLevel : levelSelections.entrySet()) {
			if (eLevel.getValue().getSign() == Sign.INCLUDE && eLevel.getValue().getSequence() >= sequence)
				levels.add(eLevel.getKey());
		}
		return levels;
	}

	private List<Level> getExcludedLevels(int sequence) {
		List<Level> levels = new ArrayList<Level>();
		for (Entry<Level, SelectionInfo> eLevel : levelSelections.entrySet()) {
			if (eLevel.getValue().getSign() == Sign.EXCLUDE && eLevel.getValue().getSequence() >= sequence)
				levels.add(eLevel.getKey());
		}
		return levels;
	}

	public boolean isIncluded(Member member) {
		SelectionNode info = find(member);
		if (member.equals(info.getMember()))
			return info.getEffectiveSign(Operator.MEMBER) == Sign.INCLUDE;
		Member parent = member.getParentMember();
		if (parent != null && parent.equals(info.getMember())) {
			return info.getEffectiveSign(Operator.CHILDREN) == Sign.INCLUDE;
		}
		if ( info.getEffectiveSign(Operator.DESCENDANTS) == Sign.INCLUDE) {
			return !getExcludedLevels(info.getSequence()).contains(member.getLevel());
		} else {
			return getIncludedLevels(info.getSequence()).contains(member.getLevel());
		}
	}

	void applyLevel(Level level, Sign s) {
		int seq = ++currentSequence;
		levelSelections.put(level, new SelectionInfo(s, seq));

		applyLevelAction(this.root, level.getDepth()+1, s);
	}

	private void applyLevelAction(SelectionNode selection, int depth, Sign s) {
		if (depth == 0) {
			selection.getStatus().apply(s, Operator.MEMBER);
			return;
		}

		if (depth == 1) {
			selection.getStatus().apply(s, Operator.CHILDREN);
		}

		for (SelectionNode child : selection.getOverridingChildren()) {
			applyLevelAction(child, depth - 1, s);
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
		SelectionNode memberInfo = find(path);
  
		// Compute the parent node, if it's found in the selection tree
		SelectionNode parent = memberInfo.getParent();
		if (path.size() > 0)
			parent = path.size() == 1 ? memberInfo : null;

		// Avoids creating tree nodes when this action is a no-op
		if (parent != root && path.size() > 0) {
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
			Member m = path.pop();
			memberInfo = memberInfo.createOverridingChild(m);
			
			Level memberLevel = m.getLevel();
			SelectionInfo affectedLevel = levelSelections.get(memberLevel);
			if ( affectedLevel != null )
				memberInfo.getStatus().apply(affectedLevel.getSign(), Operator.MEMBER);
			
			if ( memberLevel.getDepth() + 1 < levels.size()) {
				Level childrenLevel = levels.get(memberLevel.getDepth()+1);
				affectedLevel = levelSelections.get(childrenLevel);
				if ( affectedLevel != null )
					memberInfo.getStatus().apply(affectedLevel.getSign(), Operator.CHILDREN);
			}
		}
		if (action.getOperator() == Operator.MEMBER
				&& action.getSign() == memberInfo
						.getEffectiveSign(Operator.MEMBER))
			return;

		// TODO detect overriding every children of a member and replace the
		// inclusion.
		// e.g.: excluding every children MEMBER is equivalent to exclude
		// CHILDREN

		MemberSelectionState currentSt = memberInfo.getStatus();
		currentSt.apply(action.getSign(), action.getOperator());

		if (action.getOperator() == Operator.DESCENDANTS) {
			memberInfo.setSequence(currentSequence);
			memberInfo.getOverridingChildren().clear();
		} else {
			if (action.getOperator() == Operator.CHILDREN) {
				Iterator<SelectionNode> itChild = memberInfo
						.getOverridingChildren().iterator();
				while (itChild.hasNext()) {
					SelectionNode child = itChild.next();
					MemberSelectionState childSt = child.getStatus();
					childSt.clear(Operator.MEMBER);
					if (child.isVoid()) {
						// TODO remove back-reference from removed child
						itChild.remove();
					}
				}
			}
		}
	}

	public boolean isLeaf(Member member) throws OlapException {
		int childMemberCount = member.getChildMemberCount();
		if (childMemberCount == 0)
			return true;

		SelectionNode visitInfo = find(member);
		Member visitMember = visitInfo.getMember();
		if (!member.equals(visitMember)) {
			if ( visitInfo.getDefaultSign() == Sign.EXCLUDE ) {
				for(Entry<Level,SelectionInfo> iLevel : levelSelections.entrySet() ) {
					if ( iLevel.getValue().getSign() != Sign.INCLUDE )
						continue;
					if ( iLevel.getValue().getSequence() > visitInfo.getSequence() &&
							iLevel.getKey().getDepth() > member.getLevel().getDepth() )
						return false;
				}
				return true;
			} else {
				for(int i = member.getLevel().getDepth()+1; i < levels.size(); ++i) {
					Level l = levels.get(i);
					SelectionInfo levelSelection = levelSelections.get(l);
					if ( levelSelection == null  || levelSelection.getSequence() <= visitInfo.getSequence() )
						return false;
				}
				return true;
			}
		}

		return !hasChildren(visitInfo);
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
	private boolean hasChildren(SelectionNode visit) throws OlapException {
		// Stack of selection nodes pending to be processed.
		Stack<SelectionNode> pendingNodes = new Stack<SelectionNode>();
		pendingNodes.add(visit);

		while (!pendingNodes.isEmpty()) {
			SelectionNode node = pendingNodes.pop();

			Sign childrenDefaultSign = node.getEffectiveSign(Operator.CHILDREN);

			// Counts the number of children nodes excluded overriding this node
			// CHILDREN
			// exclusion. Used to detect the case of a CHILDREN inclusion
			// overrided by the
			// exclusion of every child MEMBER.
			int overridingExcludedCount = 0;
			for (SelectionNode override : node.getOverridingChildren()) {
				Sign memberSign = override.getEffectiveSign(Operator.MEMBER);
				if (memberSign == Sign.INCLUDE) {
					// We've found an included children, so this member has at
					// least a child.
					return true;
				} else {
					if (childrenDefaultSign == Sign.INCLUDE)
						overridingExcludedCount++;

					// This member is excluded, we should keep looking for
					// included descendants.
					pendingNodes.push(override);
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
				if (node.getOverridingChildren().size() == 0) {
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

	
}