package es.cgalesanco.olap4j.query;

import java.util.List;

import org.olap4j.mdx.ParseTreeNode;
import org.olap4j.metadata.Level;
import org.olap4j.metadata.Member;
import org.olap4j.metadata.NamedList;

import es.cgalesanco.olap4j.query.mdx.Mdx;
import es.cgalesanco.olap4j.query.mdx.UnionBuilder;

class GrandchildrenSet implements MemberSet {
	private Member parent;
	private List<Member> excludedChildren;
	private Level level;

	public GrandchildrenSet(Member parent, List<Member> overridedChildren) {
		this.parent = parent;
		this.excludedChildren = overridedChildren;
		NamedList<Level> levels = parent.getHierarchy().getLevels();
		int levelDepth = parent.getLevel().getDepth() + 2;
		level = parent.getLevel();
		if (levelDepth < levels.size())
			level = levels.get(levelDepth);
	}

	@Override
	public boolean containsAncestorOf(Member m) {
		Member child = m;
		Member parent = child.getParentMember();
		if (parent == null)
			return false;
		Member grandparent = parent.getParentMember();
		while (grandparent != null) {
			if (this.parent.equals(grandparent)) {
				return !excludedChildren.contains(parent);
			}

			child = parent;
			parent = grandparent;
			grandparent = grandparent.getParentMember();
		}
		return false;
	}

	@Override
	public ParseTreeNode getMdx() {
		return Mdx.descendants(
				Mdx.except(Mdx.children(parent),
						UnionBuilder.fromMembers(excludedChildren)), 1);
	}

	@Override
	public boolean contains(Member m) {
		Member memberParent = m.getParentMember();
		if (memberParent == null)
			return false;
		if (!parent.equals(memberParent.getParentMember()))
			return false;
		return !excludedChildren.contains(memberParent);
	}

	@Override
	public Level getLevel() {
		return level;
	}

}
