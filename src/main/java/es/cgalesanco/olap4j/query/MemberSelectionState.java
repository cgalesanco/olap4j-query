package es.cgalesanco.olap4j.query;

import java.util.EnumSet;

import es.cgalesanco.olap4j.query.Selection.Operator;
import es.cgalesanco.olap4j.query.Selection.Sign;


class MemberSelectionState {
	private EnumSet<Operator> includes;
	private EnumSet<Operator> excludes;

	public MemberSelectionState() {
		includes = EnumSet.noneOf(Operator.class);
		excludes = EnumSet.noneOf(Operator.class);
	}
	
	private EnumSet<Operator> getSetFor(Sign s) {
		if ( s == Sign.INCLUDE )
			return includes;
		return excludes;
	}
	
	public void apply(Sign s, Operator op) {

		switch (op) {
		case DESCENDANTS:
			includes.clear();
			excludes.clear();
			getSetFor(s).add(op);
			break;
			
		default:
			if (s != getSelectionSign(Operator.DESCENDANTS)) {
				getSetFor(s).add(op);
				getSetFor(s.opposite()).remove(op);
			} else {
				clear(op);
			}
			break;
		}
	}

	public void include(Operator op) {
		apply(Sign.INCLUDE, op);
	}

	public void exclude(Operator op) {
		apply(Sign.EXCLUDE, op);
	}

	public void clear(Operator op) {
		includes.remove(op);
		excludes.remove(op);
	}

	public Sign getSelectionSign(Operator op) {
		if ( includes.contains(op))
			return Sign.INCLUDE;
		if ( excludes.contains(op))
			return Sign.EXCLUDE;
		return null;
	}

	public boolean isNull() {
		return includes.isEmpty() && excludes.isEmpty();
	}

}
