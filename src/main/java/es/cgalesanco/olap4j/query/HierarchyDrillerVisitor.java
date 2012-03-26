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

class HierarchyDrillerVisitor implements SelectionNodeVisitor, ExpanderVisitor {
	private Set<Member> drillList;
	private AxisExpression expression;
	private List<Level> levels;
	private Level firstIncludedLevel;
	
	public boolean expand(SelectionNode node) {
		Sign memberAction = getMemberAction(node);
		if ( isCollapsed(node) ) {
			processMember(node, memberAction);
			return false;
		} else {
			if ( node.getMemberSign() == Sign.INCLUDE )
				drillList.remove(node.getMember());
		}
		
		Sign childrenAction = getChildrenAction(node);
		SelectionInfo descendantsSelection = node.getDefaultSelection();
		
		if ( processCommonCases(memberAction, childrenAction, descendantsSelection, node) )
			return true;
		
		processMember(node, memberAction);
		
		MemberSet drillRoots = processChildren(node, childrenAction);
		
		if ( descendantsSelection.getSign() == Sign.INCLUDE ) {
			List<Level> excludedLevels = node.getOverridingLevels(Sign.INCLUDE);
			boolean lastLevelIncluded = node.getChildrenSign() == Sign.INCLUDE;
			for(int iLevel = node.getMember().getDepth()+2; iLevel < levels.size(); ++iLevel) {
				Level l = levels.get(iLevel);
				
				if ( excludedLevels.contains(l) ) {
					lastLevelIncluded = false;
					continue;
				} 
					
			
				if ( lastLevelIncluded )
					expression.drill(drillRoots.getMdx());
				else
					expression.include(Mdx.descendants(drillRoots.getMdx(), l.getDepth() - drillRoots.getLevel().getDepth()));
				lastLevelIncluded = true;
				
				CollectionMemberSet nextRoots = new CollectionMemberSet();
				Iterator<Member> itDrill = drillList.iterator();
				while(itDrill.hasNext()) {
					Member drill = itDrill.next();
					if ( drill.getLevel().equals(l) && drillRoots.containsAncestorOf(drill) ) {
						nextRoots.add(drill);
						itDrill.remove();
					}
				}
				
				drillRoots = nextRoots;
			}
		} else {
			List<Level> includedLevels = node.getOverridingLevels(Sign.EXCLUDE);
			int lastLevelDepth = node.getChildrenSign() == Sign.INCLUDE ? node.getMember().getLevel().getDepth()+1 : 0;
			for(Level l : includedLevels) {
				if ( l.getDepth() - lastLevelDepth == 1 ) {
					expression.drill(drillRoots.getMdx());
				} else {
					expression.include(Mdx.descendants(drillRoots.getMdx(), includedLevels.get(0)));
				}
				
				CollectionMemberSet nextRoots = new CollectionMemberSet();
				Iterator<Member> itDrill = drillList.iterator();
				while(itDrill.hasNext()) {
					Member drill = itDrill.next();
					if ( drill.getLevel().equals(l) && drillRoots.containsAncestorOf(drill) ) {
						nextRoots.add(drill);
						itDrill.remove();
					}
				}
				
				lastLevelDepth = l.getDepth();
				drillRoots = nextRoots;
			}
		}
		
		return true;
	}

	private void expandRoot(SelectionNode node) {
		List<Level> includedLevels = node.getIncludedLevels();
		if ( includedLevels.size() < 2 )
			return;
		MemberSet drillRoots = new RootChildren(levels.get(0), node.getOverridedMembers());

		Level currentLevel = includedLevels.get(0);
		for(int iLevel = 1; iLevel < includedLevels.size(); ++iLevel) {
			Level nextLevel = includedLevels.get(iLevel);
			
			Iterator<Member> itDrills = drillList.iterator();
			CollectionMemberSet drills = new CollectionMemberSet();
			while( itDrills.hasNext() ) {
				Member drill = itDrills.next();
				
				if ( drill.getLevel().equals(currentLevel) && drillRoots.containsAncestorOf(drill)) {
					itDrills.remove();
					drills.add(drill);
				}
			}
			int levelDistance = nextLevel.getDepth() - currentLevel.getDepth();
			if ( levelDistance == 1 )
				expression.drill(drills.getMdx());
			else
				expression.include(Mdx.descendants(drills.getMdx(), levelDistance));
			

			currentLevel = nextLevel;
			drillRoots = drills;
		}
	}

	private Sign getMemberAction(SelectionNode node) {
		if ( node.getMemberSign() == Sign.INCLUDE ) {
			if ( node.isMemberAlreadyIncluded() || (node.getMember().getLevel().equals(firstIncludedLevel)) )
				return null;
			return Sign.INCLUDE;
		} else {
			if ( node.isMemberAlreadyIncluded() || (node.getMember().getLevel().equals(firstIncludedLevel)) )
				return Sign.EXCLUDE;
			return null;
		}
	}

	private MemberSet processChildren(SelectionNode node,
			Sign childrenAction) {
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

		List<Member> overridedChildren = node.getOverridedMembers();
		if ( node.getChildrenSign() == Sign.INCLUDE ) {
			CollectionMemberSet drilled = new CollectionMemberSet();
			Iterator<Member> itDrill = drillList.iterator();
			while(itDrill.hasNext()) {
				Member drill = itDrill.next();
				if ( node.getMember().equals(drill.getParentMember()) && 
						!overridedChildren.contains(drill) ) {
					drilled.add(drill);
					itDrill.remove();
				}
			}
			//expression.drill(drilled.getMdx());
			return drilled;
		} else {
			if ( overridedChildren.isEmpty() )
				return new GrandchildrenSet(node.getMember(), overridedChildren);
			else
				return new ChildrenMemberSet(node.getMember(), overridedChildren);
		}
			
	}

	private boolean processCommonCases(Sign memberAction, Sign childrenAction,
			SelectionInfo descendantsSelection, SelectionNode node) {
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
		return node.getMemberSign() == Sign.INCLUDE && !drillList.contains(node.getMember());
	}

	@Override
	public void visitLeave(SelectionNode node) {
	}

	@Override
	public boolean visitEnter(SelectionNode node) {
		if ( node.getMember() == null ) {
			expandRoot(node);
			return true;
		}
		return expand(node);
	}

	@Override
	public ParseTreeNode execute(SelectionNode root, List<Level> levels) {
		expression = new AxisExpression();
		this.levels = levels;
		
		List<Level> includedLevels = root.getIncludedLevels();
		this.firstIncludedLevel = null;
		if ( !includedLevels.isEmpty() ) {
			this.firstIncludedLevel = includedLevels.get(0);
			expression.include(Mdx.allMembers(this.firstIncludedLevel));
		}
		
		root.accept(this);
		return expression.getExpression();
	}

	@Override
	public boolean isDrilled(Member member) {
		if ( drillList == null )
			return false;
		Member parent = member;
		while( parent != null ) {
			if ( !drillList.contains(parent))
				return false;
			
			parent = parent.getParentMember();
		}
			
		return true;
	}

	@Override
	public void setDrills(List<Member> drills) {
		this.drillList = new HashSet<Member>(drills);
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


}