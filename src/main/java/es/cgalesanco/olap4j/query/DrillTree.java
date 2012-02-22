package es.cgalesanco.olap4j.query;

import java.util.ArrayList;
import java.util.List;

import org.olap4j.OlapException;
import org.olap4j.mdx.ParseTreeNode;
import org.olap4j.metadata.Member;

import es.cgalesanco.olap4j.query.mdx.CrossJoinBuilder;
import es.cgalesanco.olap4j.query.mdx.Mdx;
import es.cgalesanco.olap4j.query.mdx.UnionBuilder;

/**
 * This class provides a set of positions (sequences of
 * {@link org.olap4j.metadata.Member}). Its used by {@link QueryAxis} to store
 * the set of drilled positions within the axis.
 * 
 * @author César García
 * 
 */
class DrillTree {
	public interface Visitor {
		void visit(List<Member> parents, List<Member> drills)
				throws OlapException;

		boolean shouldVisitChild(int hierarchyPos, Member child);

	}

	private static class Node {
		private final Member member;
		private List<Node> children;
		private List<Member> drills;

		public Node(Member m) {
			member = m;
		}

		public Member getMember() {
			return member;
		}

		public void addDrill(Member m) {
			if (drills == null)
				drills = new ArrayList<Member>();
			if (!drills.contains(m))
				drills.add(m);
		}

		public Node getChild(Member m) {
			if (children == null)
				return null;

			for (Node n : children) {
				if (n.getMember().equals(m))
					return n;
			}
			return null;
		}

		public Node addChild(Member m) {
			if (children == null)
				children = new ArrayList<Node>();
			Node n = new Node(m);
			children.add(n);
			return n;
		}

		public boolean hasChildren() {
			return children != null && !children.isEmpty();
		}

		public List<Node> getChildren() {
			return children;
		}

		public List<Member> getDrills() {
			return drills;
		}

		public void removeDrill(Member m) {
			if (drills != null)
				drills.remove(m);
		}

		public boolean isDrilled(Member member) {
			return drills == null ? false : drills.contains(member);
		}

		public void removeChildren() {
			children = null;
		}

		public void removeDrills() {
			drills = null;
		}
	}

	private final Node root;

	public DrillTree() {
		root = new Node(null);
	}

	/**
	 * Adds a position to the set.
	 * 
	 * @param position
	 *            the position to be added.
	 */
	public void add(Member[] position) {
		Node current = root;
		int drillDepth = position.length - 1;
		for (int i = 0; i < drillDepth; ++i) {
			Member m = position[i];
			Node next = current.getChild(m);
			if (next == null)
				next = current.addChild(m);

			current = next;
		}

		current.addDrill(position[drillDepth]);
	}

	/**
	 * Removes a position from the set.
	 * 
	 * @param memberList
	 *            the position to remove.
	 */
	public void remove(Member[] memberList) {
		Node current = root;
		int drillDepth = memberList.length - 1;
		for (int i = 0; i < drillDepth; ++i) {
			Member m = memberList[i];
			Node next = current.getChild(m);
			if (next == null)
				return;

			current = next;
		}

		current.removeDrill(memberList[drillDepth]);
	}

	/**
	 * Removes from the set every position with a length greater of equal to
	 * {@code level} members long.
	 * 
	 * @param level
	 *            member length limit.
	 */
	public void prune(int level) {
		prune(root, level);
	}

	/**
	 * Tests if this set contains a position.
	 * 
	 * @param position
	 *            the position to search for.
	 * @return true if this set contains the position, false otherwise.
	 */
	public boolean isDrilled(Member... position) {
		Node current = root;
		int drillDepth = position.length - 1;
		for (int i = 0; i < drillDepth; ++i) {
			Member m = position[i];
			Node next = current.getChild(m);
			if (next == null)
				return false;

			current = next;
		}

		return current.isDrilled(position[drillDepth]);
	}

	public void clearLevel(final int level) {
		if (root != null)
			clearLevel(0, root, level);
	}

	private void clearLevel(int currentLevel, Node current, int targetLevel) {
		if (currentLevel > targetLevel)
			return;
		if (currentLevel == targetLevel) {
			if (current.drills != null)
				current.drills.clear();
		} else if (current.hasChildren()) {
			for (Node child : current.getChildren()) {
				clearLevel(currentLevel + 1, child, targetLevel);
			}
		}
	}

	public void visit(Visitor visitor) throws OlapException {
		List<Member> parents = new ArrayList<Member>();
		visit(parents, root, visitor);
	}

	public ParseTreeNode toOlap4j(List<QueryHierarchy> dimensions,
			List<HierarchyExpander> expanders) {
		UnionBuilder expression = new UnionBuilder();
		toOlap4j(expression, root, null, 0, dimensions, expanders);
		return expression.getUnionNode();
	}

	private void toOlap4j(UnionBuilder expression, Node current,
			ParseTreeNode partialExpression, int level,
			List<QueryHierarchy> dimensions, List<HierarchyExpander> expanders) {
		
		HierarchyExpander expander = expanders.get(level);
		QueryHierarchy h = dimensions.get(level);

		// Creates a list of children to be processed
		List<Member> childrenMembers = new ArrayList<Member>();
		if (current.hasChildren()) {
			for (Node child : current.children) {
				if (!h.isIncluded(child.getMember()) || 
					!expander.isDrilled(child.getMember().getParentMember(), current.drills))
					continue;
			
				childrenMembers.add(child.getMember());
			}
		}

		// Generates the join of this query hierarchy, appling the drills and removing
		// any member involved in a larger drill position.
		CrossJoinBuilder xJoin = new CrossJoinBuilder();
		xJoin.join(partialExpression);
		xJoin.join(Mdx.except(h.toOlap4j(expander, current.drills),
				UnionBuilder.fromMembers(childrenMembers)));
		for (int n = level + 1; n < dimensions.size(); ++n) {
			HierarchyExpander nextExp = expanders.get(n);
			QueryHierarchy nh = dimensions.get(n);
			xJoin.join(nh.toOlap4j(nextExp, null));
		}
		expression.add(xJoin.getJoinNode());

		// Recursively generates expression for larger drill positions.
		for (Member child : childrenMembers) {
			CrossJoinBuilder x = new CrossJoinBuilder();
			x.join(partialExpression);
			x.join(Mdx.member(child));
			toOlap4j(expression, current.getChild(child), x.getJoinNode(),
					level + 1, dimensions, expanders);
		}
	}

	private void prune(Node n, int level) {
		if (level > 1) {
			List<Node> children = n.getChildren();
			if (children != null) {
				for (Node child : children) {
					prune(child, level - 1);
				}
			}
		} else {
			n.removeChildren();
			n.removeDrills();
		}
	}

	private void visit(List<Member> parents, Node node, Visitor visitor)
			throws OlapException {
		List<Member> drilledMembers = node.getDrills();
		visitor.visit(parents, drilledMembers);
		if (!node.hasChildren())
			return;

		for (Node child : node.getChildren()) {
			if (visitor.shouldVisitChild(parents.size(), child.getMember())) {
				parents.add(child.getMember());
				visit(parents, child, visitor);
				parents.remove(parents.size() - 1);
			}
		}
	}

}
