package es.cgalesanco.olap4j.query;

import java.util.List;

import org.olap4j.mdx.ParseTreeNode;
import org.olap4j.metadata.Member;

import es.cgalesanco.olap4j.query.mdx.Mdx;
import es.cgalesanco.olap4j.query.mdx.UnionBuilder;

class InverseMemberSet implements MemberSet {
	private Member parent;
	private List<Member> excludedMembers;

	public InverseMemberSet(Member parent, List<Member> excludedChildren) {
		this.parent = parent;
		this.excludedMembers = excludedChildren;
	}

	@Override
	public boolean containsAncestorOf(Member descendant) {
		int parentDepth = parent.getDepth();
		
		Member child;
		do {
			child = descendant;
			descendant = descendant.getParentMember();
		} while( descendant != null && descendant.getDepth() > parentDepth );
			
		
		if ( parent.equals(descendant) )
			return !excludedMembers.contains(child);
		return false;
	}

	@Override
	public ParseTreeNode getMdx() {
		return Mdx.except(Mdx.children(parent), UnionBuilder.fromMembers(excludedMembers));
	}

	@Override
	public boolean contains(Member member) {
		if ( !parent.equals(member.getParentMember()))
			return false;
		
		return !excludedMembers.contains(member);
	}

	@Override
	public boolean remove(Member m) {
		if ( !parent.equals(m.getParentMember()))
			return false;
		
		if ( excludedMembers.contains(m) )
			return false;
		
		excludedMembers.add(m);
		return true;
	}

}
