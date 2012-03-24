package es.cgalesanco.olap4j.query;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.olap4j.mdx.ParseTreeNode;
import org.olap4j.metadata.Level;
import org.olap4j.metadata.Member;

import es.cgalesanco.olap4j.query.Selection.Sign;
import es.cgalesanco.olap4j.query.SelectionTree.SelectionNode;
import es.cgalesanco.olap4j.query.mdx.Mdx;
import es.cgalesanco.olap4j.query.mdx.UnionBuilder;

class HierarchyExpanderVisitor implements SelectionNodeVisitor, ExpanderVisitor {
	
	private Set<Member> undrillList;
	private AxisExpression expression;
	private List<Level> levels;
	
	public boolean expand(SelectionNode node) {
		Sign memberAction = getMemberAction(node);
		if ( isCollapsed(node) ) {
			undrillList.remove(node.getMember());
			processMember(node, memberAction);
			return false;
		}
		
		Sign childrenAction = getChildrenAction(node);
		SelectionInfo descendantsSelection = node.getDefaultSelection();
		
		if ( processCommonCases(memberAction, childrenAction, descendantsSelection, node) )
			return true;
		
		processMember(node, memberAction);
		
		GrandchildrenSet nonOverridedDescendants = processChildren(node, childrenAction);
		
		if ( descendantsSelection.getSign() == Sign.INCLUDE ) {
			expandDescendants(node, descendantsSelection,
					nonOverridedDescendants);
		} else {
			expandIncludedLevels(node, descendantsSelection,
					nonOverridedDescendants);
		}
		
		return true;
	}

	private Sign getMemberAction(SelectionNode node) {
		Sign s = node.getMemberSign();
		if ( s == Sign.INCLUDE ) {
			if ( node.isMemberAlreadyIncluded() || node.isMemberLevelIncluded() )
				s = null;
		} else {
			if ( !node.isMemberLevelIncluded() && !node.isMemberAlreadyIncluded() )
				s = null;
		}
		return s;
	}

	private Sign getChildrenAction(SelectionNode node) {
		Sign s = node.getChildrenSign();
		if ( s == Sign.INCLUDE ) {
			if ( node.isChildrenLevelIncluded() )
				s = null;
		} else {
			if ( !node.isChildrenLevelIncluded() )
				s = null;
		}
		return s;
	}

	private void expandIncludedLevels(SelectionNode node,
			SelectionInfo descendantsSelection,
			MemberSet nonOverridedDescendants) {
		CollectionMemberSet undrilled = new CollectionMemberSet();
		UnionBuilder undrilledUnion = new UnionBuilder();
		undrilledUnion.add(nonOverridedDescendants.getMdx());
		for(Level included : node.getOverridingLevels(Sign.EXCLUDE)) {
			int nodeLevel = node.getMember().getLevel().getDepth();
			int levelDistance = included.getDepth() - (nodeLevel+2);
			
			if ( levelDistance == 0 ) {
				expression.exclude(undrilledUnion.getUnionNode());
			} else {
				expression.exclude(Mdx.descendants(undrilledUnion.getUnionNode(), included));
			}
			
			Iterator<Member> itUndrill = undrillList.iterator();
			while(itUndrill.hasNext()) {
				Member undrill = itUndrill.next();
				
				if ( !included.equals(undrill.getLevel()))
					continue;
				
				if ( nonOverridedDescendants.containsAncestorOf(undrill) && !undrilled.containsAncestorOf(undrill) ) {
					undrilled.add(undrill);
					undrilledUnion.add(Mdx.member(undrill));
					itUndrill.remove();
				}
			}
		}
	}

	private void expandDescendants(SelectionNode node,
			SelectionInfo descendantsSelection,
			GrandchildrenSet nonOverridedDescendants) {
		AxisExpression descendantsExpression = new AxisExpression();
		
		// Include non overrided descendants
		descendantsExpression.include(nonOverridedDescendants.getMdxDescendants());

		// Process grandchildren undrills
		Iterator<Member> itUndrill = undrillList.iterator();
		while(itUndrill.hasNext()) {
			Member undrill = itUndrill.next();
			if ( nonOverridedDescendants.contains(undrill) ) {
				expression.undrill(Mdx.member(undrill));
				itUndrill.remove();
			}
		}
		
		// Remove descendants from excluded leveles
		CollectionMemberSet descendantsUndrills = new CollectionMemberSet();
		for(Level excluded : node.getOverridingLevels(Sign.EXCLUDE)) {
			int levelDistance = excluded.getDepth()-node.getMember().getDepth()+2;
			descendantsExpression.exclude(Mdx.descendants(nonOverridedDescendants.getMdx(), levelDistance));

			// Remove undrilled descendants
			itUndrill = undrillList.iterator();
			while(itUndrill.hasNext()) {
				Member undrill = itUndrill.next();
				if ( !excluded.equals(undrill.getLevel()))
					continue;
				
				if ( nonOverridedDescendants.containsAncestorOf(undrill) && !descendantsUndrills.containsAncestorOf(undrill) ) {
					descendantsUndrills.add(undrill);
					itUndrill.remove();
				}
			}
		}
		
		descendantsExpression.undrill(descendantsUndrills.getMdx());
		expression.include(descendantsExpression.getExpression());
	}

	private GrandchildrenSet processChildren(SelectionNode node, Sign childrenAction) {
		if ( childrenAction == Sign.INCLUDE ) {
			if ( node.getMemberSign() == Sign.INCLUDE )
				expression.drill(Mdx.member(node.getMember()));
			else
				expression.include(Mdx.children(node.getMember()));
			
		}
		else {
			if ( childrenAction == Sign.EXCLUDE )  
				expression.exclude(Mdx.children(node.getMember()));
		}
		
		Set<Member> excludedChildren = new HashSet<Member>(node.getOverridedMembers());
		if ( node.getChildrenSign() == Sign.INCLUDE ) {
			Iterator<Member> itUndrill = undrillList.iterator();
			while(itUndrill.hasNext()) {
				Member undrill = itUndrill.next();
				if ( node.getMember().equals(undrill.getParentMember()) && 
						!excludedChildren.contains(undrill) ) {
					excludedChildren.add(undrill);
					itUndrill.remove();
				}
			}
		}
		
		return new GrandchildrenSet(node.getMember(), excludedChildren);
	}

	private boolean processCommonCases(Sign memberAction, Sign childrenAction,
			SelectionInfo descendantsSelection, SelectionNode node) {
		if ( node.hasOverridingChildren() )
			return false;

		if ( !node.getOverridingLevels(descendantsSelection.getSign()).isEmpty() )
			return false;

		if ( node.getChildrenSign() == Sign.INCLUDE ) {
			for(Member m : undrillList) {
				if ( node.getMember().equals(m.getParentMember()) )
					return false;
			}
		}
		
		if ( childrenAction == null && descendantsSelection.getSign() == Sign.INCLUDE ) {
			expression.include(Mdx.descendants(Mdx.member(node.getMember()), 2, "SELF_AND_AFTER"));
			
			if ( memberAction == Sign.INCLUDE )
				expression.include(node.getMember());
			else if (memberAction == Sign.EXCLUDE )
				expression.exclude(node.getMember());
			
			
			// Process grandchildren undrills
			GrandchildrenSet grandchildren = new GrandchildrenSet(node.getMember(), node.getOverridedMembers());
			Iterator<Member> itUndrill = undrillList.iterator();
			while(itUndrill.hasNext()) {
				Member undrill = itUndrill.next();
				if ( grandchildren.contains(undrill) ) {
					expression.undrill(Mdx.member(undrill));
					itUndrill.remove();
				}
			}
			
			return true;
		} 
		
		if ( childrenAction == Sign.INCLUDE && descendantsSelection.getSign() == Sign.INCLUDE ) {
			if ( memberAction == Sign.INCLUDE ) {
				expression.include(Mdx.descendants(Mdx.member(node.getMember()), 0, "SELF_AND_AFTER"));
			} else {
				expression.include(Mdx.descendants(Mdx.member(node.getMember()), 1, "SELF_AND_AFTER"));
			}
			
			if ( memberAction == Sign.EXCLUDE )
				expression.exclude(node.getMember());
			
			// Process grandchildren undrills
			GrandchildrenSet grandchildren = new GrandchildrenSet(node.getMember(), node.getOverridedMembers());
			Iterator<Member> itUndrill = undrillList.iterator();
			while(itUndrill.hasNext()) {
				Member undrill = itUndrill.next();
				if ( grandchildren.contains(undrill) ) {
					expression.undrill(Mdx.member(undrill));
					itUndrill.remove();
				}
			}
			
			return true;
		}
		return false;
	}

	private void processMember(SelectionNode node, Sign memberAction) {
		if ( memberAction == Sign.INCLUDE ) {
			expression.include(node.getMember());
		} else if ( memberAction == Sign.EXCLUDE ) {
			expression.exclude(node.getMember());
		}
	}

	private boolean isCollapsed(SelectionNode node) {
		return node.getMemberSign() == Sign.INCLUDE && undrillList.contains(node.getMember());
	}

	@Override
	public boolean visitEnter(SelectionNode node) {
		if ( node.getMember() == null ) {
			expandRoot(node);
			return true;
		} else 
		return expand(node);
	}

	private void expandRoot(SelectionNode node) {
		RootChildren nonOverridedRoots = new RootChildren(levels.get(0), node.getOverridedMembers());
		Set<Member> undrilled = new HashSet<Member>();
		if ( node.getChildrenSign() == Sign.INCLUDE ) {
			Iterator<Member> itUndrill = undrillList.iterator();
			while(itUndrill.hasNext()) {
				Member undrill = itUndrill.next();
				if ( nonOverridedRoots.contains(undrill) ) {
					undrilled.add(undrill);
					itUndrill.remove();
				}
			}
		}
		
		UnionBuilder undrillsUnion = new UnionBuilder();
		undrillsUnion.add(undrilled);
		for(Level l : node.getIncludedLevels()) {
			if ( l.getDepth() == 0 )
				continue;
			
			expression.exclude(Mdx.descendants(undrillsUnion.getUnionNode(), l));
			
			Iterator<Member> itUndrill = undrillList.iterator();
			while(itUndrill.hasNext()) {
				Member undrill = itUndrill.next();
				if ( !undrill.getLevel().equals(l) )
					continue;

				if ( nonOverridedRoots.containsAncestorOf(undrill) ) {
					// TODO avoid undrilling descendants of a previously undrilled member
					undrilled.add(undrill);
					undrillsUnion.add(Mdx.member(undrill));
					itUndrill.remove();
				}
			}
		}
	}

	public ParseTreeNode getExpression() {
		return expression.getExpression();
	}

	@Override
	public ParseTreeNode execute(SelectionNode root, List<Level> levels) {
		expression = new AxisExpression();
		this.levels = levels;

		for(Level l : root.getIncludedLevels()) {
			expression.include(Mdx.allMembers(l));
		}
		
		root.accept(this);
		return expression.getExpression();
	}

	@Override
	public boolean isDrilled(Member member) {
		if (undrillList == null)
			return true;
		Member ancestor = member;
		while (ancestor != null) {
			if (undrillList.contains(ancestor))
				return false;

			ancestor = ancestor.getParentMember();
		}
		return true;
	}

	@Override
	public void setDrills(List<Member> drills) {
		this.undrillList = new HashSet<Member>(drills);
		
	}

	@Override
	public void visitLeave(SelectionNode selectionNode) {
	}
}