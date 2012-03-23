package es.cgalesanco.olap4j.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

class HierarchyExpanderVisitor implements SelectionNodeVisitor, ExpanderVisitor {
	private AxisExpression expression;
	private List<Member> undrillList;
	private List<Level> levels;

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
		List<Member> overridedMembers = current.getOverridedMembers();
		
		if ( current.getMember() == null ) {
			expandLevels(current);
			return;
		}
		
		Member currentMember = current.getMember();
		Sign currentSign = current.getEffectiveSign(Operator.MEMBER);
		Sign childrenSign = current.getEffectiveSign(Operator.CHILDREN);
		Sign descendantsSign = current.getEffectiveSign(Operator.DESCENDANTS);
		
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
				collapseUndrills(expansionRoots,
						current.getExcludedLevels());
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
				collapseUndrills(expansionRoots,
						current.getExcludedLevels());
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
					collapseLevelUndrills(new GrandchildrenSet(currentMember, overridedMembers),
							includedLevels); 
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

				collapseLevelUndrills(new DescendantsSet(currentMember, fromLevel), actuallyIncludedLevels);
			}
		}
	}
	
	private void expandLevels(SelectionNode root) {
		Iterator<Level> itLevels = root.getIncludedLevels().iterator();
		if (!itLevels.hasNext())
			return;

		// Include members from first expanded level

		Level firstLevel = itLevels.next();
		List<Member> overridedMembers = root.getOverridedMembers();
		MemberSet rootMembers = new LevelMemberSet(levels.get(0), overridedMembers);
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
					.descendants(UnionBuilder.fromMembers(overridedMembers),
							levelDepth)));
			expression.exclude(Mdx.descendants(
					UnionBuilder.fromMembers(undrilledRoots), levelDepth,
					"SELF_AND_AFTER"));

			// Prepares for next iteration
			firstLevel = nextLevel;
		}
	}

	public ParseTreeNode getExpression() {
		return expression.getExpression();
	}

	private void collapseUndrills(MemberSet roots, List<Level> excludedLevels) {
		List<Member> rootUndrills = new ArrayList<Member>(undrillList);
		Collections.sort(rootUndrills, new Comparator<Member>() {
			@Override
			public int compare(Member arg0, Member arg1) {
				return arg0.getDepth() - arg1.getDepth();
			}
		});
		 
		CollectionMemberSet processed = new CollectionMemberSet();
		Iterator<Member> itFiltered = rootUndrills.iterator();
		while( itFiltered.hasNext() ) {
			Member m = itFiltered.next();
			if ( !roots.containsAncestorOf(m) )
				continue;
			if ( processed.containsAncestorOf(m)) 
				continue;
			if ( excludedLevels.contains(m.getLevel()) )
				continue;
					
			expression.undrill(Mdx.member(m));
			undrillList.remove(m);
			processed.add(m);
		}
		
		Level currentLevel = roots.getLevel();
		List<Level> levels = currentLevel.getHierarchy().getLevels();
		for (int iLevel = currentLevel.getDepth() + 1; iLevel < levels.size(); ++iLevel) {
			if (excludedLevels.contains(levels.get(iLevel))) {
				expression.exclude(Mdx.descendants(roots.getMdx(), iLevel));
			} 
		}
	}

	private void collapseLevelUndrills(MemberSet roots,
			List<Level> includedLevels) {
		Level fromLevel = roots.getLevel();
		List<Member> nextUndrills = new ArrayList<Member>();
		Iterator<Member> undrillIt = undrillList.iterator();
		while( undrillIt.hasNext() ) {
			Member drill = undrillIt.next();
			if ( roots.containsAncestorOf(drill) ) {
				nextUndrills.add(drill);
				undrillIt.remove();
			}
		}

		Level currentLevel = fromLevel;
		List<Level> levels = currentLevel.getHierarchy().getLevels();
		for(int iLevel = currentLevel.getDepth(); iLevel < levels.size(); ++iLevel ) {
			Level nextLevel = levels.get(iLevel);
			if ( includedLevels.contains(nextLevel)) {
				expression.include(
						Mdx.descendants(
								Mdx.except(
										roots.getMdx(), 
										UnionBuilder.fromMembers(nextUndrills)
								), 
								iLevel-fromLevel.getDepth()
						)
				);
				
				undrillIt = undrillList.iterator();
				nextUndrills.clear();
				while( undrillIt.hasNext() ) {
					Member undrill = undrillIt.next();
					
					if ( undrill.getLevel().getDepth() == iLevel &&
							roots.containsAncestorOf(undrill) ) {
						nextUndrills.add(undrill);
					}
				}
				
				currentLevel = nextLevel;
			}
		}
	}

	@Override
	public ParseTreeNode execute(SelectionNode root, List<Level> levels) {
		expression = new AxisExpression();
		this.levels = levels;
		
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
	public void setDrills(List<Member> undrills) {
		this.undrillList = undrills;
	}
}
