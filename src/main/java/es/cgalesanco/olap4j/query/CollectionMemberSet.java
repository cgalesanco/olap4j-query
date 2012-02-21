package es.cgalesanco.olap4j.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.olap4j.mdx.ParseTreeNode;
import org.olap4j.metadata.Member;

import es.cgalesanco.olap4j.query.mdx.UnionBuilder;

class CollectionMemberSet implements MemberSet {
	private List<Member> members;

	public CollectionMemberSet(Member...members) {
		this.members = new ArrayList<Member>();
		Collections.addAll(this.members, members);
	}
	
	@Override
	public boolean containsAncestorOf(Member m) {
		for(Member member : members) {
			Member p = member;
			while( p != null ) {
				if ( p.equals(m) )
					return true;
				
				p = p.getParentMember();
			}
		}
		return false;
	}

	@Override
	public ParseTreeNode getMdx() {
		return UnionBuilder.fromMembers(members);
	}

	@Override
	public boolean contains(Member m) {
		return members.contains(m);
	}

	@Override
	public boolean remove(Member m) {
		return members.remove(m);
	}
}
