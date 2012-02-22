package es.cgalesanco.olap4j.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.olap4j.metadata.Member;

import es.cgalesanco.olap4j.query.Selection.Operator;
import es.cgalesanco.olap4j.query.Selection.Sign;

/**
 * The tree of members used to store the selection state of a QueryHierarchy.
 * 
 * @author César García
 * 
 */
class SelectionTree {
	/**
	 * Represents the the status of a visitor of a SelectionTree. Accumulates
	 * previous selection information to produce the effective selection at the
	 * current node.
	 * 
	 * @author César García
	 * 
	 */
	static class VisitingInfo {
		private SelectionTree node;
		private Sign defaultSign;
		private SelectionTree parent;

		/**
		 * Constructs an instance
		 * 
		 * @param node
		 *            visited node
		 * @param parent
		 *            parent of the visited node
		 * @param defaultSign
		 *            default selection sign.
		 */
		public VisitingInfo(SelectionTree node, SelectionTree parent,
				Sign defaultSign) {
			this.node = node;
			this.defaultSign = defaultSign;
			this.parent = parent;
		}

		/**
		 * Return the visited node.
		 * 
		 * @return the visited node.
		 */
		public SelectionTree getNode() {
			return node;
		}

		/**
		 * Return the visited member, equivalent to getNode().getMember()
		 * 
		 * @return the visited member.
		 */
		public Member getMember() {
			return node.getMember();
		}

		/**
		 * Return the previously visited node.
		 * 
		 * @return the previously visited node
		 */
		public SelectionTree getParent() {
			return parent;
		}

		/**
		 * Return the default selection sign.
		 * 
		 * @return the default selection sign.
		 */
		public Sign getDefaultSign() {
			return defaultSign;
		}

		/**
		 * Return the effective selection sign for the given operator.
		 * 
		 * @param op
		 *            the operator to check for
		 * @return The effective selection sign for the given operator.
		 */
		public Sign getEffectiveSign(Operator op) {
			if (op == Operator.MEMBER && parent != null) {
				Sign def = parent.getStatus().getSelectionSign(
						Operator.CHILDREN);
				if (def != null)
					return node.getStatus().getEffectiveSign(op, def);
			}

			return node.getStatus().getEffectiveSign(op, defaultSign);
		}
		
		public boolean isMemberIncluded() {
			return getEffectiveSign(Operator.MEMBER) == Sign.INCLUDE;
		}
		
		public boolean areChildrenIncluded() {
			return getEffectiveSign(Operator.CHILDREN) == Sign.INCLUDE;
		}
		
		public boolean areDescendantsIncluded() {
			return getEffectiveSign(Operator.DESCENDANTS) == Sign.INCLUDE;
		}

		/**
		 * Generates the VisitingInfo instance for the given child of the
		 * visited node.
		 * 
		 * @param next
		 *            the next visiting node.
		 * @return the VisitingInfo instance for the given child.
		 */
		public VisitingInfo visitChild(SelectionTree next) {
			Sign nextDefaultSign = next.getStatus().getEffectiveSign(
					Operator.DESCENDANTS, getDefaultSign());
			return new VisitingInfo(next, this.node, nextDefaultSign);
		}
	}

	private MemberSelectionState selectionState;
	private Member member;
	private List<SelectionTree> overridingChildren;

	/**
	 * Constructs selection tree. The resulting tree has no members selected.
	 */
	public SelectionTree() {
		selectionState = new MemberSelectionState();
		selectionState.exclude(Operator.DESCENDANTS);
		overridingChildren = new ArrayList<SelectionTree>();
	}

	private SelectionTree(Member m) {
		selectionState = new MemberSelectionState();
		member = m;
		overridingChildren = new ArrayList<SelectionTree>();
	}

	/**
	 * Returns the member of this node.
	 * 
	 * @return the member of this node.
	 */
	public Member getMember() {
		return member;
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
	public SelectionTree getOverridingChild(Member m) {
		for (SelectionTree child : overridingChildren)
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
	public SelectionTree createOverridingChild(Member m) {
		SelectionTree n = new SelectionTree(m);
		overridingChildren.add(n);
		return n;
	}

	/**
	 * Returns the lis of overriding children of this node.
	 * 
	 * @return the list of overriding children of this node.
	 */
	public List<SelectionTree> getOverridingChildren() {
		return overridingChildren;
	}

	/**
	 * Generates a list of Selections producing the current state of this node.
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
		overridingChildren.clear();
	}
}