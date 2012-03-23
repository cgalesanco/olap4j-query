package es.cgalesanco.olap4j.query;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.olap4j.mdx.ParseTreeNode;
import org.olap4j.metadata.Level;
import org.olap4j.metadata.Member;

import es.cgalesanco.olap4j.query.Selection.Operator;
import es.cgalesanco.olap4j.query.Selection.Sign;
import es.cgalesanco.olap4j.query.SelectionTree.SelectionNode;
import es.cgalesanco.olap4j.query.mdx.Mdx;
import es.cgalesanco.olap4j.query.mdx.UnionBuilder;

class HierarchyDrillerVisitor implements SelectionNodeVisitor {
	private AxisExpression expression;
	private List<Member> drillList;
	private HierarchyExpander expander;
	private List<Level> levels;

	public HierarchyDrillerVisitor(List<Member> drills, HierarchyExpander expander, List<Level> levels) {
		expression = new AxisExpression();
		this.drillList = drills; 
		this.expander = expander;
		this.levels = levels;
	}

	@Override
	public boolean visitEnter(SelectionNode current) {
		if ( current.getMember() == null )
			return true;
		
		Member currentMember = current.getMember();
		Sign currentSign = current.getEffectiveSign(Operator.MEMBER);

		boolean alreadyIncluded = false;
		if ( current.getParent().getMember() != null ) {
			alreadyIncluded = current.getParent().getEffectiveSign(Operator.CHILDREN) == Sign.INCLUDE;
		}
		
		// If this member is included, and is not drilled (nor expanded),
		// include it in the axis and ends the visit.
		if (currentSign == Sign.INCLUDE) {
			if (!drillList.remove(currentMember)) {
				if (!alreadyIncluded)
					expression.include(Mdx.member(currentMember));
				return false;
			}
		}

		// Include or exclude current member if necessary
		if (currentSign == Sign.INCLUDE) {
			if (!alreadyIncluded)
				expression.include(Mdx.member(currentMember));
		} else {
			if (alreadyIncluded)
				expression.exclude(Mdx.member(currentMember));
		}
		return true;
	}

	@Override
	public void visitLeave(SelectionNode current) {
		if ( current.getMember() == null ) {
			drillLevels(current);
			return;
		}
		
		Member currentMember = current.getMember();
		Sign currentSign = current.getEffectiveSign(Operator.MEMBER);
		Sign childrenSign = current.getEffectiveSign(Operator.CHILDREN);
		Sign descendantsSign = current.getEffectiveSign(Operator.DESCENDANTS);
		
		List<Member> overridedMembers = current.getOverridedMembers();

		// If descendants are included, expand non-overriding nodes in the
		// inclusion/exclusion tree.
		if (descendantsSign == Sign.INCLUDE) {
			// Childrens are excluded, so we have to add grandchildren to the
			// axis expression
			if (childrenSign == Sign.EXCLUDE) {
				GrandchildrenSet expansionRoots = new GrandchildrenSet(
						currentMember, overridedMembers);
				if (overridedMembers.isEmpty()) {
						expression.include(Mdx.descendants(
								Mdx.member(currentMember), 2));
				} else {
					expression.include(expansionRoots.getMdx());
				}
				expander.expand(expansionRoots, drillList,
						current.getExcludedLevels(),
						expression);
			} else {
				ChildrenMemberSet expansionRoots = new ChildrenMemberSet(
				currentMember, overridedMembers);
				if (currentSign == Sign.INCLUDE) {
					expression.drill(Mdx.member(currentMember));
				} else {
					expression.include(Mdx.children(currentMember));
				}
				expander.expand(expansionRoots, drillList,
						current.getExcludedLevels(),
						expression);
			}
		} else {
			if (childrenSign == Sign.INCLUDE) {
				// Include children members
				if (currentSign == Sign.INCLUDE) {
					expression.drill(Mdx.member(currentMember));
				} else {
					expression.include(Mdx.children(currentMember));
				}
				
				List<Level> includedLevels = current.getIncludedLevels();
				Level fromLevel = currentMember.getLevel();
				if ( fromLevel.getDepth()+2 < levels.size() ) {
					expander.expandLevels(
							new GrandchildrenSet(currentMember, overridedMembers),
							drillList, 
							includedLevels, 
							expression);
				}
			} else {
				Level fromLevel = currentMember.getLevel();
				int iLevel = fromLevel.getDepth()+2;
				List<Level> actuallyIncludedLevels = current.getIncludedLevels();
				while( iLevel < levels.size() ) {
					fromLevel = levels.get(iLevel);
					if ( actuallyIncludedLevels.contains(fromLevel) )
						break;
					
					++iLevel;
				}
				
				if ( iLevel > levels.size() )
					return;
				
				expander.expandLevels(new DescendantsSet(currentMember, fromLevel), drillList, actuallyIncludedLevels, expression);
			}
		}


	}

	public ParseTreeNode getExpression() {
		return expression.getExpression();
	}

	private void drillLevels(SelectionNode root) {
		Iterator<Level> itLevels = root.getIncludedLevels().iterator();
		if (!itLevels.hasNext())
			return;

		// Include members from first expanded level
		Level firstLevel = itLevels.next();
		MemberSet rootMemberSet = new LevelMemberSet(firstLevel,
				root.getOverridedMembers());
		if (firstLevel.getDepth() == 0) {
			expression.include(Mdx.descendants(rootMemberSet.getMdx(),
					firstLevel.getDepth()));
		}

		MemberSet previousUndrills = rootMemberSet;
		while (itLevels.hasNext()) {
			// Computes the list of drilled members in this level
			Level nextLevel = itLevels.next();
			List<Member> drilledRoots = new ArrayList<Member>();
			Iterator<Member> itDrills = drillList.iterator();
			while (itDrills.hasNext()) {
				Member drill = itDrills.next();
				if (drill.getLevel().equals(firstLevel)) {
					if (previousUndrills.containsAncestorOf(drill)) {
						itDrills.remove();
						drilledRoots.add(drill);
					}
				}
			}

			// Executes drill for those members
			int levelDistance = nextLevel.getDepth() - firstLevel.getDepth();
			if (levelDistance == 1)
				expression.drill(UnionBuilder.fromMembers(drilledRoots));
			else
				expression.include(Mdx.descendants(
						UnionBuilder.fromMembers(drilledRoots), levelDistance));

			// Prepares for next iteration
			firstLevel = nextLevel;
			previousUndrills = new CollectionMemberSet(
					drilledRoots.toArray(new Member[drilledRoots.size()]));
		}
	}
}
