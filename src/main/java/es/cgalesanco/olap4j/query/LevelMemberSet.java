package es.cgalesanco.olap4j.query;

import java.util.List;

import org.olap4j.mdx.ParseTreeNode;
import org.olap4j.metadata.Level;
import org.olap4j.metadata.Member;

import es.cgalesanco.olap4j.query.mdx.Mdx;
import es.cgalesanco.olap4j.query.mdx.UnionBuilder;

public class LevelMemberSet implements MemberSet {
	private Level level;
	private List<Member> excluded;
	
	
	public LevelMemberSet(Level l, List<Member> exclusions) {
		level = l;
		excluded = exclusions;
	}

	@Override
	public boolean containsAncestorOf(Member m) {
		Member test = m;
		while( test != null && !level.equals(test.getLevel()) ) {
			test = test.getParentMember();
		}
		return !excluded.contains(test);
	}

	@Override
	public ParseTreeNode getMdx() {
		return Mdx.except(Mdx.allMembers(level), UnionBuilder.fromMembers(excluded));
	}

	@Override
	public boolean contains(Member m) {
		return m.getLevel().equals(level) && !excluded.contains(m);
	}

	@Override
	public Level getLevel() {
		return level;
	}

}
