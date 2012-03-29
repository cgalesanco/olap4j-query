package es.cgalesanco.olap4j.query;

import java.util.List;

import org.olap4j.mdx.ParseTreeNode;
import org.olap4j.metadata.Level;
import org.olap4j.metadata.Member;

import es.cgalesanco.olap4j.query.SelectionTree.SelectionNode;

interface ExpanderVisitor extends SelectionNodeVisitor {
	ParseTreeNode execute(SelectionNode root, List<Level> levels);

	boolean isDrilled(Member member);

	void setDrills(List<Member> drills);
}
