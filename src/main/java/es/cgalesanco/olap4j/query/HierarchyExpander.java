package es.cgalesanco.olap4j.query;

import java.util.ArrayList;
import java.util.List;

import org.olap4j.mdx.ParseTreeNode;
import org.olap4j.metadata.Level;
import org.olap4j.metadata.Member;

import es.cgalesanco.olap4j.query.SelectionTree.SelectionNode;

class HierarchyExpander {
	private boolean isFullyExpanded;
	private ExpanderVisitor expander;

	public HierarchyExpander() {
		collapseHierarchy();
	}

	public void expandHierarchy() {
		isFullyExpanded = true; 
		expander = new HierarchyExpanderVisitor();
		expander.setDrills(new ArrayList<Member>());
	}

	public void collapseHierarchy() {
		isFullyExpanded = false;
		expander = new HierarchyDrillerVisitor();
		expander.setDrills(new ArrayList<Member>());
	}

	public boolean isHierarchyExpanded() {
		return isFullyExpanded;
	}

	public boolean isDrilled(Member member) {
		if (member == null)
			return true;
		
		return expander.isDrilled(member);
	}

	public ParseTreeNode expand(SelectionNode root, List<Level> levels) {
		return expander.execute(root, levels);
	}

	public void setDrills(List<Member> drills) {
		if ( drills == null )
			expander.setDrills(new ArrayList<Member>());
		else
			expander.setDrills(new ArrayList<Member>(drills));
	}
}
