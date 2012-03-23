package es.cgalesanco.olap4j.query;

import es.cgalesanco.olap4j.query.SelectionTree.SelectionNode;

public interface SelectionNodeVisitor {

	void visitLeave(SelectionNode selectionNode);

	boolean visitEnter(SelectionNode selectionNode);

}
