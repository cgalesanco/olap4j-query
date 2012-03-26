package es.cgalesanco.olap4j.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.olap4j.mdx.ParseTreeNode;
import org.olap4j.metadata.Level;
import org.olap4j.metadata.Member;

import es.cgalesanco.olap4j.query.mdx.UnionBuilder;

class CollectionMemberSet implements MemberSet {
	private List<Member> members;
	private Level level;

	public CollectionMemberSet(Member...members) {
		this.members = new ArrayList<Member>();
		Collections.addAll(this.members, members);
		if ( members.length > 0 ) {
			level = members[0].getLevel();
		}
	}
	
	@Override
	public boolean containsAncestorOf(Member m) {
		for(Member member : members) {
			Member p = m;
			while( p != null ) {
				if ( p.equals(member) )
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
	
	public void add(Member m) {
		members.add(m);
	}

	@Override
	public Level getLevel() {
		return level;
	}

	public void addAll(Collection<Member> set) {
		members.addAll(set);
	}
}
