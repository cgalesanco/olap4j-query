package es.cgalesanco.olap4j.query;

import java.util.EnumMap;
import java.util.EnumSet;

import es.cgalesanco.olap4j.query.Selection.Operator;
import es.cgalesanco.olap4j.query.Selection.Sign;


class MemberSelectionState {
	private EnumMap<Operator,Sign> selections;

	public MemberSelectionState() {
		selections = new EnumMap<Operator,Sign>(Operator.class);
	}
	
	public void apply(Sign s, Operator op) {
		selections.put(op, s);
	}

	public void clear(Operator op) {
		selections.remove(op);
	}

	public Sign getSelectionSign(Operator op) {
		return selections.get(op);
	}

	public boolean isNull() {
		return selections.isEmpty();
	}

}
