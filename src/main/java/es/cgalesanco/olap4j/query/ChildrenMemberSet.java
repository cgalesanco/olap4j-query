package es.cgalesanco.olap4j.query;

import java.util.Collection;

import org.olap4j.mdx.ParseTreeNode;
import org.olap4j.metadata.Level;
import org.olap4j.metadata.Member;
import org.olap4j.metadata.NamedList;

import es.cgalesanco.olap4j.query.mdx.Mdx;
import es.cgalesanco.olap4j.query.mdx.UnionBuilder;

class ChildrenMemberSet implements MemberSet {
	private Member parent;
	private Collection<Member> excludedMembers;
	private Level level;

	public ChildrenMemberSet(Member parent, Collection<Member> excludedChildren) {
		this.parent = parent;
		this.excludedMembers = excludedChildren;
		this.level = parent.getLevel();
		NamedList<Level> levels = parent.getHierarchy().getLevels();
		if ( parent.getLevel().getDepth()+1 < levels.size() )
			this.level = levels.get(parent.getLevel().getDepth()+1);
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
	public Level getLevel() {
		return level;
	}

}
