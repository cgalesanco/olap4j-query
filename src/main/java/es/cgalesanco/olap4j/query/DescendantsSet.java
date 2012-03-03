package es.cgalesanco.olap4j.query;

import org.olap4j.mdx.ParseTreeNode;
import org.olap4j.metadata.Level;
import org.olap4j.metadata.Member;

import es.cgalesanco.olap4j.query.mdx.Mdx;

public class DescendantsSet implements MemberSet {
	private Member root;
	private Level level;

	public DescendantsSet(Member r, Level l) {
		this.root = r;
		this.level = l;
	}

	@Override
	public boolean containsAncestorOf(Member m) {
		if ( m.getLevel().getDepth() < level.getDepth() )
			return false;
		
		Member p = m;
		while( p != null ) {
			if ( p.equals(root) )
				return true;
			
			p = p.getParentMember();
		}
		return false;
	}

	@Override
	public ParseTreeNode getMdx() {
		return Mdx.descendants(Mdx.member(root), level.getDepth()-root.getLevel().getDepth());
	}

	@Override
	public boolean contains(Member m) {
		if ( !m.getLevel().equals(level) )
			return false;
		Member p = m;
		while( p != null ) {
			if ( p.equals(root) )
				return true;
			
			p = p.getParentMember();
		}
		return false;
	}

	@Override
	public Level getLevel() {
		return level;
	}

}
