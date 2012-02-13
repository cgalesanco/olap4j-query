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
	
	public MemberSelectionState clone() {
		MemberSelectionState clone = new MemberSelectionState();
		clone.includes.addAll(this.includes);
		clone.excludes.addAll(this.excludes);
		return clone;
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

	public Sign getEffectiveSign(Operator op, Sign defaultSign) {
		Sign result = null;
		switch (op) {
		case MEMBER:
			result = getSelectionSign(Operator.MEMBER);
			if (result == null)
				result = getSelectionSign(Operator.DESCENDANTS);
			break;

		case CHILDREN:
			result = getSelectionSign(Operator.CHILDREN);
			if (result == null)
				result = getSelectionSign(Operator.DESCENDANTS);
			break;

		case DESCENDANTS:
			result = getSelectionSign(Operator.DESCENDANTS);
		}
		return result == null ? defaultSign : result;
	}

}
