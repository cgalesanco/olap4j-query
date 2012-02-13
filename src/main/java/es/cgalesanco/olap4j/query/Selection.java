package es.cgalesanco.olap4j.query;

import org.olap4j.metadata.Member;

/**
 * <p>
 * A selection of members from an OLAP dimension hierarchy. The selection is a
 * conceptual list of members from a given hierarchy.
 * </p>
 * 
 * <p>
 * It differs from the original olap4j version in that its used just to save and
 * restore a list of selections.
 * </p>
 * 
 * @author César García
 * 
 */
public interface Selection {
	/**
	 * Defines the selected members when including/excluding a member.
	 * 
	 * @author César García
	 * 
	 */
	public static enum Operator {
		/**
		 * Only the root member will be selected.
		 */
		MEMBER,
		/**
		 * Only the children of the root member will be selected.
		 */
		CHILDREN,
		/**
		 * The root member will be selected along with all it's children.
		 */
		INCLUDE_CHILDREN,
		/**
		 * Selects the set of the descendants of a specified member, including
		 * the member itself.
		 */
		DESCENDANTS,
		/**
		 * Selects the set of the ascendants of a specified member, including
		 * the member itself.
		 */
		ANCESTORS,
		/**
		 * Will select the root member along with all it's siblings.
		 */
		SIBLINGS
	}

	/**
	 * Defines the "sign" of a selection: inclusion or exclusion
	 * 
	 * @author César García
	 * 
	 */
	public static enum Sign {
		/**
		 * Members are included in the selection
		 */
		INCLUDE,
		/**
		 * Members are excluded from the selection
		 */
		EXCLUDE;

		/**
		 * Returns the opposite sign of this.
		 * 
		 * @return the opposite sign of this.
		 */
		public Sign opposite() {
			return this == INCLUDE ? EXCLUDE : INCLUDE;
		}
	}

	/**
	 * Returns the member used as reference for this selection.
	 * 
	 * @return the member used as reference for this selection.
	 */
	public Member getMember();

	/**
	 * Returns the sign of this selection.
	 * 
	 * @return the sign of this selection.
	 */
	public Sign getSign();

	/**
	 * Returns the operator used for this selection.
	 * 
	 * @see Operator
	 * @return the operator used for this selection.
	 */
	public Operator getOperator();
}
