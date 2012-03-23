package es.cgalesanco.olap4j.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
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

		private EnumMap<Operator, Sign> selections;
		private Member member;
		private List<SelectionNode> overridingChildren;
		private SelectionNode parent;
		private int sequence;

		/**
		 * Constructs selection tree. The resulting tree has no members
		 * selected.
		 */
		public SelectionNode() {
			selections = new EnumMap<Operator,Sign>(Operator.class);
			selections.put(Operator.DESCENDANTS, Sign.EXCLUDE);
			overridingChildren = new ArrayList<SelectionNode>();
			parent = null;
		}

		protected SelectionNode(SelectionNode parent, Member m) {
			member = m;
			selections = new EnumMap<Operator,Sign>(Operator.class);
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
		
		private void applyMember(Sign s) {
			selections.remove(Operator.MEMBER);
			if ( getMemberSign() == s )
				return;
			
			selections.put(Operator.MEMBER, s);
		}
		
		private void applyChildren(Sign s) {
			selections.remove(Operator.CHILDREN);
			if ( getChildrenSign() != s )
				selections.put(Operator.CHILDREN, s);
			 
			Iterator<SelectionNode> itChilds = overridingChildren.iterator();
			while(itChilds.hasNext()) {
				SelectionNode child = itChilds.next();
				child.selections.remove(Operator.MEMBER);
				if ( child.isVoid() ) {
					child.parent = null;
					itChilds.remove();
				}
			}
		}
		
		private void applyDescendants(Sign s) {
			selections.clear();
			
			if ( getDefaultSign() != s || getMemberSign() != s || getChildrenSign() != s) {
				selections.put(Operator.DESCENDANTS, s);
				sequence = currentSequence;
			}
			
			Iterator<SelectionNode> itChilds = overridingChildren.iterator();
			while(itChilds.hasNext()) {
				SelectionNode child = itChilds.next();
				child.parent = null;
				itChilds.remove();
			}
		}
		
		public Sign getMemberSign() {
			Sign s;
			if ( (s = selections.get(Operator.MEMBER)) != null )
				return s;
			
			SelectionInfo levelInfo = levelSelections.get(getMemberLevel());
			Sign descendantsSign = selections.get(Operator.DESCENDANTS);
			if ( descendantsSign != null ) {
				if ( levelInfo == null || levelInfo.getSequence() <= getSequence())
					return descendantsSign;
			}
			
			if ( parent != null && (s = parent.selections.get(Operator.CHILDREN)) != null ) {
				return s;
			}
			
			if ( levelInfo != null && levelInfo.getSequence() > getDefaultSequence() ) {
				return levelInfo.getSign();
			}
			
			return getDefaultSign();
		}
		
		private Level getMemberLevel() {
			if ( getMember() != null )
				return getMember().getLevel();
			return null;
		}

		public Sign getChildrenSign() {
			Sign s = selections.get(Operator.CHILDREN);
			if ( s != null )
				return s;
			
			SelectionInfo levelInfo = levelSelections.get(getChildrenLevel());
			Sign descendantsSign = selections.get(Operator.DESCENDANTS);
			if ( descendantsSign != null ) {
				if ( levelInfo == null || levelInfo.getSequence() <= getSequence())
					return descendantsSign;
			}

			if ( levelInfo != null && levelInfo.getSequence() > getDefaultSequence() ) {
				return levelInfo.getSign();
			}
			
			return getDefaultSign();
		}

		private Level getChildrenLevel() {
			if ( getMember() == null )
				return levels.get(0);
			int pos = getMember().getLevel().getDepth()+1;
			if ( pos < levels.size() )
				return levels.get(pos);
			return null;
		}

		private int getDefaultSequence() {
			SelectionNode n = this;
			while( n != null ) {
				if ( n.selections.get(Operator.DESCENDANTS) != null )
					return n.getSequence();
				
				n = n.getParent();
			}
			return 0;
		}

		/**
		 * Returns a value indicating if this node is superflous. i.e. does not
		 * define any state information for its member and has no overriding
		 * children.
		 * 
		 * @return a value indicating if this node is superflous.
		 */
		public boolean isVoid() {
			return overridingChildren.isEmpty() && selections.isEmpty();
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
			List<Selection> list = new ArrayList<Selection>();

			Sign sign = selections.get(Operator.DESCENDANTS);
			if (sign != null)
				list.add(new SelectionAction(getMember(), sign,
						Operator.DESCENDANTS));

			Sign childSign = selections.get(Operator.CHILDREN);
			Sign memberSign = selections.get(Operator.MEMBER);
			if (childSign != null) {
				if (childSign == memberSign) {
					list.add(new SelectionAction(getMember(), childSign,
							Operator.INCLUDE_CHILDREN));
					memberSign = null;
				} else
					list.add(new SelectionAction(getMember(), childSign,
							Operator.CHILDREN));
			}

			if (memberSign != null) {
				list.add(new SelectionAction(getMember(), memberSign,
						Operator.MEMBER));
			}

			return list;
		}

		public boolean hasOverridingChildren() {
			return overridingChildren != null && !overridingChildren.isEmpty();
		}

		public void clear() {
			selections.clear();
			selections.put(Operator.DESCENDANTS, Sign.EXCLUDE);
			for (SelectionNode child : overridingChildren) {
				child.parent = null;
			}
			overridingChildren.clear();
		}

		public Sign getDefaultSign() {
			SelectionNode prev = this;
			Sign s = null;
			while (prev != null
					&& (s = prev.selections.get(Operator.DESCENDANTS)) == null) {
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

		public void apply(Operator operator, Sign sign) {
			switch(operator) {
			case MEMBER:
				applyMember(sign);
				break;
				
			case CHILDREN:
				applyChildren(sign);
				break;
				
			case DESCENDANTS:
				applyDescendants(sign);
				break;
			}
			
			SelectionNode n = this;
			while( n != null && n.isVoid() ) {
				SelectionNode p = n.parent;
				if ( p != null ) {
					n.parent = null;
					p.overridingChildren.remove(n);
				}

				n = p;
			}
		}

		public Sign getSelectionSign(Operator op) {
			return selections.get(op);
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
		return toOlap4jFilter(root);
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
	private ParseTreeNode toOlap4jFilter(SelectionNode selectionNode) {
		Sign selectionSign = selectionNode.getDefaultSign();

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
					finalExpression.add(toOlap4jFilter(overriding));
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
					finalExpression.add(toOlap4jFilter(overriding));
				}
			}
			return finalExpression.getUnionNode();
		}
	}

	/**
	 * Implementation of {@link #toOlap4j()} for query axes.
	 */
	ParseTreeNode toOlap4jQuery(HierarchyExpander expander) {
		return expander.expand(root, levels);
	}

	public boolean isIncluded(Member member) {
		SelectionNode info = find(member);
		if (member.equals(info.getMember()))
			return info.getMemberSign() == Sign.INCLUDE;
		Member parent = member.getParentMember();
		if (parent != null && parent.equals(info.getMember())) {
			return info.getChildrenSign() == Sign.INCLUDE;
		}
		if ( info.getDefaultSign() == Sign.INCLUDE) {
			return !info.getExcludedLevels().contains(member.getLevel());
		} else {
			return info.getIncludedLevels().contains(member.getLevel());
		}
	}

	void applyLevel(Level level, Sign s) {
		int seq = ++currentSequence;
		levelSelections.put(level, new SelectionInfo(s, seq));

		applyLevelAction(this.root, level.getDepth()+1, s);
	}

	private void applyLevelAction(SelectionNode selection, int depth, Sign s) {
		if (depth == 0) {
			selection.selections.get(Operator.MEMBER);
			return;
		}

		if (depth == 1) {
			selection.selections.get(Operator.CHILDREN);
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
  
		// Creates necesarry tree nodes including the one corresponding to this
		// action
		while (!path.isEmpty()) {
			Member m = path.pop();
			memberInfo = memberInfo.createOverridingChild(m);
		}

		// TODO detect overriding every children of a member and replace the
		// inclusion.
		// e.g.: excluding every children MEMBER is equivalent to exclude
		// CHILDREN

		memberInfo.apply(action.getOperator(), action.getSign());
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

			Sign childrenDefaultSign = node.getChildrenSign();

			// Counts the number of children nodes excluded overriding this node
			// CHILDREN
			// exclusion. Used to detect the case of a CHILDREN inclusion
			// overrided by the
			// exclusion of every child MEMBER.
			int overridingExcludedCount = 0;
			for (SelectionNode override : node.getOverridingChildren()) {
				Sign memberSign = override.getMemberSign();
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
					Sign descendantsSign = node.getDefaultSign();
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