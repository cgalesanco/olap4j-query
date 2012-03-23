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

class HierarchyExpanderVisitor implements SelectionNodeVisitor {
	private AxisExpression expression;
	private List<Member> undrillList;
	private HierarchyExpander expander;
	private List<Level> levels;

	public HierarchyExpanderVisitor(List<Member> drills, HierarchyExpander expander, List<Level> levels) {
		expression = new AxisExpression();
		this.undrillList = drills; 
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
			SelectionNode parent = current.getParent();
			alreadyIncluded = parent.getEffectiveSign(Operator.CHILDREN) == Sign.INCLUDE && parent.getEffectiveSign(Operator.DESCENDANTS) == Sign.EXCLUDE;
		}

		// If this member is included, and is not drilled (nor expanded),
		// include it in the axis and ends the visit.
		if (currentSign == Sign.INCLUDE) {
			if (undrillList.contains(currentMember)) {
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
			expandLevels(current);
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
					expression
							.include(Mdx.descendants(
									Mdx.member(currentMember), 2,
									"SELF_AND_AFTER"));
				} else {
					expression.include(Mdx.descendants(Mdx.except(
							Mdx.children(currentMember),
							UnionBuilder.fromMembers(overridedMembers)), 1,
							"SELF_AND_AFTER"));
				}
				expander.expand(expansionRoots, undrillList,
						current.getExcludedLevels(),
						expression);
			} else {
				ChildrenMemberSet expansionRoots = new ChildrenMemberSet(
						currentMember, overridedMembers);
				if (overridedMembers.isEmpty()) {
					expression.include(Mdx.descendants(
							Mdx.member(currentMember), 1, "SELF_AND_AFTER"));
				} else {
					expression.include(Mdx.descendants(Mdx.except(
							Mdx.children(currentMember),
							UnionBuilder.fromMembers(overridedMembers)), 0,
							"SELF_AND_AFTER"));
				} 
				expander.expand(expansionRoots, undrillList,
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
							undrillList, 
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
				
				expander.expandLevels(new DescendantsSet(currentMember, fromLevel), undrillList, actuallyIncludedLevels, expression);
			}
		}
	}
	
	private void expandLevels(SelectionNode root) {
		Iterator<Level> itLevels = root.getIncludedLevels().iterator();
		if (!itLevels.hasNext())
			return;

		// Include members from first expanded level

		Level firstLevel = itLevels.next();
		List<Member> processedMembers = getProcessedMembers(root, firstLevel);
		MemberSet rootMembers = new LevelMemberSet(levels.get(0), processedMembers);
		expression.include(Mdx.descendants(rootMembers.getMdx(),
				firstLevel.getDepth()));

		while (itLevels.hasNext()) {
			// Computes the list of drilled members in this level
			Level nextLevel = itLevels.next();
			List<Member> undrilledRoots = new ArrayList<Member>();
			Iterator<Member> itUndrills = undrillList.iterator();
			while (itUndrills.hasNext()) {
				Member undrill = itUndrills.next();
				if (undrill.getLevel().equals(firstLevel)) {
					if (rootMembers.containsAncestorOf(undrill)) {
						itUndrills.remove();
						undrilledRoots.add(undrill);
					}
				}
			}

			// Executes drill for not-undrilled members
			int levelDepth = nextLevel.getDepth();
			expression.include(Mdx.except(Mdx.allMembers(nextLevel), Mdx
					.descendants(UnionBuilder.fromMembers(processedMembers),
							levelDepth)));
			expression.exclude(Mdx.descendants(
					UnionBuilder.fromMembers(undrilledRoots), levelDepth,
					"SELF_AND_AFTER"));

			// Prepares for next iteration
			firstLevel = nextLevel;
		}
	}

	private List<Member> getProcessedMembers(SelectionNode root, Level l) {
		List<Member> treeRoots = new ArrayList<Member>();
		for (SelectionNode r : root.getOverridingChildren()) {
			treeRoots.add(r.getMember());
		}
		return treeRoots;
	}

	public ParseTreeNode getExpression() {
		return expression.getExpression();
	}
	
}
