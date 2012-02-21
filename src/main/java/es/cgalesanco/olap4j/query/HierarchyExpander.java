package es.cgalesanco.olap4j.query;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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

	public void expand(MemberSet roots, List<Member> drills, AxisExpression expression) {
		if (!isFullyExpanded)
			expandDrills(roots, drills, expression);
		else {
			collapseUndrills(roots, drills, expression);
		}

	}

	private void collapseUndrills(MemberSet roots, List<Member> undrills,
			AxisExpression expression) {
		List<Member> filteredUndrills = new ArrayList<Member>(undrills.size());
		for (Member undrill : undrills) {
			if ( roots.containsAncestorOf(undrill) )
				filteredUndrills.add(undrill);
		}

		for (int i = 0; i < filteredUndrills.size(); ++i) {
			Iterator<Member> itM = filteredUndrills.iterator();
			Member current = filteredUndrills.get(i);
			while (itM.hasNext()) {
				Member cand = itM.next();

				if (isChildOrEqualTo(cand, current)) {
					if ( !cand.equals(current))
						itM.remove();
				}
			}
		}

		if (!filteredUndrills.isEmpty())
			expression.undrill(UnionBuilder.fromMembers(filteredUndrills));
	}

	private boolean isChildOrEqualTo(Member u, Member r) {
		Member m = u;
		while (m != null && !m.equals(r)) {
			m = m.getParentMember();
		}
		return m != null;
	}

	private void expandDrills(MemberSet roots, List<Member> drills,
			AxisExpression expression) {
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

		while (!previousDrill.isEmpty()) {
			itM = drills.iterator();
			nextDrill = new HashSet<Member>();
			while (itM.hasNext()) {
				Member m = itM.next();
				Member parent = m.getParentMember();
				if ( parent != null && previousDrill.contains(parent) ) {
					expression.drill(Mdx.member(m));
					itM.remove();
					nextDrill.add(m);
				}
			}

			previousDrill = nextDrill;
		}
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
}
