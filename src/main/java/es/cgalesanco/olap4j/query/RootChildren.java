package es.cgalesanco.olap4j.query;

import java.util.Collection;

import org.olap4j.mdx.ParseTreeNode;
import org.olap4j.metadata.Level;
import org.olap4j.metadata.Member;

import es.cgalesanco.olap4j.query.mdx.Mdx;
import es.cgalesanco.olap4j.query.mdx.UnionBuilder;

class RootChildren implements MemberSet {
	private Collection<Member> excludedRoots;
	private Level level;

	public RootChildren(Level l, Collection<Member> overrided) {
		excludedRoots = overrided;
		level = l.getHierarchy().getLevels().get(0);
	}

	@Override
	public boolean containsAncestorOf(Member m) {
		Member parent = m;
		while( parent.getParentMember() != null ) {
			parent = m.getParentMember();
		}
		return !excludedRoots.contains(parent);
	}

	@Override
	public ParseTreeNode getMdx() {
		return Mdx.descendants(UnionBuilder.fromMembers(excludedRoots),level.getDepth());
	}

	@Override
	public boolean contains(Member m) {
		Member parent = m.getParentMember();
		return parent == null && !excludedRoots.contains(parent);
	}

	@Override
	public Level getLevel() {
		return level;
	}

}
