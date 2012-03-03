package es.cgalesanco.olap4j.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.olap4j.metadata.Level;
import org.olap4j.metadata.Member;

import es.cgalesanco.olap4j.query.mdx.Mdx;
import es.cgalesanco.olap4j.query.mdx.UnionBuilder;

class HierarchyExpander {
	private boolean isFullyExpanded;
	private List<Boolean> overridedDepths;

	public HierarchyExpander() {
		isFullyExpanded = false;
		overridedDepths = new ArrayList<Boolean>();
	}

	public void expandHierarchy() {
		isFullyExpanded = true;
		overridedDepths.clear();
	}

	public void collapseHierarchy() {
		isFullyExpanded = false;
		overridedDepths.clear();
	}

	public boolean isHierarchyExpanded() {
		return isFullyExpanded;
	}

	public boolean isLevelExpanded(int n) {
		if (n >= overridedDepths.size())
			return isFullyExpanded;

		return isFullyExpanded ^ overridedDepths.get(n);
	}

	public void expandLevel(int n) {
		setLevelState(n, true);
	}

	public void collapseLevel(int n) {
		setLevelState(n, false);
	}

	public void expand(MemberSet roots, List<Member> drills,
			List<Level> excludedLeveles, AxisExpression expression) {
		if (!isFullyExpanded)
			expandDrills(roots, drills, excludedLeveles, expression);
		else {
			collapseUndrills(roots, drills, excludedLeveles, expression);
		}

	}

	public void expandLevels(MemberSet roots,
			List<Member> drills, List<Level> includedLevels,
			AxisExpression expression) {
		if (!isFullyExpanded) 
			expandLevelDrills(roots, drills, includedLevels, expression);
		else 
			collapseLevelUndrills(roots, drills, includedLevels, expression);
		
	}

	private void collapseUndrills(MemberSet roots, List<Member> undrills,
			List<Level> excludedLevels, AxisExpression expression) {
		List<Member> rootUndrills = new ArrayList<Member>(undrills);
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
			undrills.remove(m);
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

	private void expandDrills(MemberSet roots, List<Member> drills,
			List<Level> levelExclusions, AxisExpression expression) {
		Set<Member> previousDrill = new HashSet<Member>();
		Set<Member> nextDrill;

		Iterator<Member> itM = drills.iterator();
		nextDrill = new HashSet<Member>();
		while (itM.hasNext()) {
			Member m = itM.next();
			if (roots.contains(m)) {
				previousDrill.add(m);
				expression.drill(Mdx.member(m));
				itM.remove();
			}
		}

		if (previousDrill.isEmpty())
			return;

		Level currentLevel = previousDrill.iterator().next().getLevel();
		List<Level> levels = currentLevel.getHierarchy().getLevels();
		Level nextLevel = findNextLevel(levels, currentLevel.getDepth(),
				levelExclusions);

		List<Member> effectiveDrills = new ArrayList<Member>();
		while (!previousDrill.isEmpty() && nextLevel != null) {
			nextDrill = new HashSet<Member>();
			int levelDistance = nextLevel.getDepth() - currentLevel.getDepth();

			itM = drills.iterator();
			effectiveDrills.clear();
			MemberSet previousSet = new CollectionMemberSet(
					previousDrill.toArray(new Member[previousDrill.size()]));
			while (itM.hasNext()) {
				Member m = itM.next();
				if (m.getLevel().equals(nextLevel)
						&& previousSet.containsAncestorOf(m)) {
					effectiveDrills.add(m);
					itM.remove();
					nextDrill.add(m);
				}
			}

			if (levelDistance == 1) {
				expression.drill(UnionBuilder.fromMembers(effectiveDrills));
			} else {
				expression.include(Mdx.descendants(
						UnionBuilder.fromMembers(effectiveDrills),
						levelDistance));
			}

			previousDrill = nextDrill;
			currentLevel = nextLevel;
			nextLevel = findNextLevel(levels, currentLevel.getDepth(),
					levelExclusions);
		}
	}

	
	private void expandLevelDrills(MemberSet roots,
			List<Member> drills, List<Level> includedLevels,
			AxisExpression expression) {
		List<Member> nextDrills = new ArrayList<Member>();
		Iterator<Member> drillIt = drills.iterator();
		while( drillIt.hasNext() ) {
			Member drill = drillIt.next();
			if ( roots.contains(drill) ) {
				nextDrills.add(drill);
				drillIt.remove();
			}
		}

		if ( nextDrills.isEmpty() )
			return;
		
		Level currentLevel = nextDrills.iterator().next().getLevel();
		List<Level> levels = currentLevel.getHierarchy().getLevels();
		for(int iLevel = currentLevel.getDepth(); iLevel < levels.size(); ++iLevel ) {
			Level nextLevel = levels.get(iLevel);
			if ( includedLevels.contains(nextLevel)) {
				if ( iLevel == currentLevel.getDepth()+1 ) {
					expression.drill(UnionBuilder.fromMembers(nextDrills));
				} else {
					expression.include(Mdx.descendants(UnionBuilder.fromMembers(nextDrills), iLevel));
				}
				
				drillIt = drills.iterator();
				roots = new CollectionMemberSet(nextDrills.toArray(new Member[nextDrills.size()]));
				nextDrills.clear();
				while( drillIt.hasNext() ) {
					Member drill = drillIt.next();
					
					if ( drill.getLevel().getDepth() == iLevel &&
							roots.containsAncestorOf(drill) ) {
						nextDrills.add(drill);
					}
				}
				
				currentLevel = nextLevel;
			}
		}
	}

	
	private void collapseLevelUndrills(MemberSet roots,
			List<Member> undrills, List<Level> includedLevels,
			AxisExpression expression) {
		Level fromLevel = roots.getLevel();
		List<Member> nextUndrills = new ArrayList<Member>();
		Iterator<Member> undrillIt = undrills.iterator();
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
				
				undrillIt = undrills.iterator();
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

	private Level findNextLevel(List<Level> levels, int depth,
			List<Level> levelExclusions) {
		for (int i = depth + 1; i < levels.size(); ++i) {
			Level l = levels.get(i);
			if (!levelExclusions.contains(l))
				return l;
		}
		return null;
	}

	private void setLevelState(int n, boolean expanded) {
		if (n >= overridedDepths.size()) {
			if (isFullyExpanded != expanded) {
				for (int i = overridedDepths.size(); i < n; ++i)
					overridedDepths.add(false);
				overridedDepths.add(true);
			}
		} else {
			if (isFullyExpanded == expanded) {
				overridedDepths.set(n, true);
			} else {
				overridedDepths.set(n, false);
				int lastOverride = overridedDepths.size() - 1;
				while (lastOverride >= 0
						&& overridedDepths.get(lastOverride) == false) {
					overridedDepths.remove(lastOverride--);
				}
			}
		}
	}

	public boolean isDrilled(Member member, List<Member> drillList) {
		if (member == null)
			return true;
		if (isFullyExpanded) {
			if (drillList == null)
				return true;
			Member ancestor = member;
			while (ancestor != null) {
				if (drillList.contains(ancestor))
					return false;

				ancestor = ancestor.getParentMember();
			}
			return true;
		} else {
			if (drillList == null)
				return false;

			Member ancestor = member;
			while (ancestor != null) {
				if (!drillList.contains(ancestor))
					return false;

				ancestor = ancestor.getParentMember();
			}
			return true;
		}
	}
}
