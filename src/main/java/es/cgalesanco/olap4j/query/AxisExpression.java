package es.cgalesanco.olap4j.query;

import org.olap4j.mdx.CallNode;
import org.olap4j.mdx.ParseTreeNode;
import org.olap4j.mdx.Syntax;

import es.cgalesanco.olap4j.query.mdx.Mdx;
import es.cgalesanco.olap4j.query.mdx.UnionBuilder;

class AxisExpression {
	final private UnionBuilder roots;
	final private UnionBuilder drills;
	final private UnionBuilder undrills;
	final private UnionBuilder exclude;
	
	public AxisExpression() {
		roots = new UnionBuilder();
		drills = new UnionBuilder();
		undrills = new UnionBuilder();
		exclude = new UnionBuilder();
	}
	
	public void include(ParseTreeNode e) {
		roots.add(e);
	}
	
	public void exclude(ParseTreeNode e) {
		exclude.add(e);
	}
	
	public void drill(ParseTreeNode e) {
		drills.add(e);
	}

	public ParseTreeNode getExpression() {
		if (roots.getUnionNode() == null)
			return new CallNode(null, "{}", Syntax.Braces);
		ParseTreeNode ex = null;
		if ( !undrills.isEmpty() ) {
			ex = Mdx.descendants(undrills.getUnionNode(), 0, "AFTER"); 
			if ( !exclude.isEmpty() ) {
				UnionBuilder ub = new UnionBuilder();
				ub.add(ex);
				ub.add(exclude.getUnionNode());
				ex = ub.getUnionNode();
			}
		} else {
			if ( !exclude.isEmpty() )
				ex = exclude.getUnionNode();
		}
		return Mdx.except(Mdx.drillDown(roots, drills), ex);
	}
	
	ParseTreeNode getDrillExpression() {
		return drills.getUnionNode();
	}

	public void undrill(ParseTreeNode fromMembers) {
		undrills.add(fromMembers);
		
	}
}
