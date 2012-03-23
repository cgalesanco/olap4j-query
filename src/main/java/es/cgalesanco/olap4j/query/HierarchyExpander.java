package es.cgalesanco.olap4j.query;

import java.util.List;

import org.olap4j.metadata.Member;

class HierarchyExpander {
	private boolean isFullyExpanded;

	public HierarchyExpander() {
		isFullyExpanded = false;
	}

	public void expandHierarchy() {
		isFullyExpanded = true;
	}

	public void collapseHierarchy() {
		isFullyExpanded = false;
	}

	public boolean isHierarchyExpanded() {
		return isFullyExpanded;
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
