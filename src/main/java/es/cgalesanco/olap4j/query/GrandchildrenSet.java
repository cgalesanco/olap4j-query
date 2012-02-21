package es.cgalesanco.olap4j.query;

import java.util.List;

import org.olap4j.mdx.ParseTreeNode;
import org.olap4j.metadata.Member;

import es.cgalesanco.olap4j.query.mdx.Mdx;
import es.cgalesanco.olap4j.query.mdx.UnionBuilder;

public class GrandchildrenSet implements MemberSet {
	private Member parent;
	private List<Member> excludedChildren;
	
	public GrandchildrenSet(Member parent, List<Member> overridedChildren) {
		this.parent = parent;
		this.excludedChildren = overridedChildren;
	}

	@Override
	public boolean containsAncestorOf(Member m) {
		Member child = m;
		Member parent = child.getParentMember();
		if ( parent == null )
			return false;
		Member grandparent = parent.getParentMember();
		while( grandparent != null ) {
			if ( parent.equals(grandparent) ) {
				return !excludedChildren.contains(child);
			}
			
			child = parent;
			parent = grandparent;
			grandparent = grandparent.getParentMember();
		}
		return false;
	}

	@Override
	public ParseTreeNode getMdx() {
		return Mdx.descendants(Mdx.except(Mdx.children(parent), UnionBuilder.fromMembers(excludedChildren)), 1);
	}

	@Override
	public boolean contains(Member m) {
		Member memberParent = m.getParentMember();
		if ( memberParent == null )
			return false;
		return parent.equals(memberParent.getParentMember());
	}

	@Override
	public boolean remove(Member m) {
		throw new RuntimeException();
	}

}
