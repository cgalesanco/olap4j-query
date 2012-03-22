package es.cgalesanco.olap4j.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Stack;

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

	private MemberSelectionState selectionState;
	private Member member;
	private List<SelectionTree> overridingChildren;
	private SelectionTree parent;
	private int sequence;

	/**
	 * Constructs selection tree. The resulting tree has no members selected.
	 */
	public SelectionTree() {
		selectionState = new MemberSelectionState();
		selectionState.exclude(Operator.DESCENDANTS);
		overridingChildren = new ArrayList<SelectionTree>();
		parent = null;
	}

	protected SelectionTree(SelectionTree parent, Member m) {
		member = m;
		selectionState = new MemberSelectionState();
		overridingChildren = new ArrayList<SelectionTree>();
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
		SelectionTree n = new SelectionTree(this, m);
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
		for(SelectionTree child : overridingChildren) {
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
		if ( s == null ) {
			return getDefaultSign();
		}
		return s;
	}

	public SelectionTree find(Member member) {
		Stack<Member> path = createPathTo(member);
		return find(path);
	}

	public SelectionTree find(Stack<Member> path) {
		SelectionTree current = this;
		while(!path.isEmpty()) {
			Member m = path.peek();
			SelectionTree next = current.getOverridingChild(m);
			if ( next == null ) {
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

	public Sign getDefaultSign() {
		SelectionTree prev = this;
		Sign s = null;
		while( prev != null && (s = prev.selectionState.getSelectionSign(Operator.DESCENDANTS)) == null) {
			prev = prev.parent;
		}
		return s;
	}

	public SelectionTree getParent() {
		return parent;
	}

}