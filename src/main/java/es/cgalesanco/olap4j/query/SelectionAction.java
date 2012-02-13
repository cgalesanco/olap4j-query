package es.cgalesanco.olap4j.query;

import org.olap4j.metadata.Member;

/**
 * Implementation for the Selection interface.
 * 
 * @author César García
 * 
 */
public class SelectionAction implements Selection {
	private Operator operator;
	private Member member;
	private Sign sign;

	public SelectionAction(Member m, Sign s, Operator o) {
		this.member = m;
		this.sign = s;
		operator = o;
	}

	@Override
	public Operator getOperator() {
		return this.operator;
	}

	@Override
	public Sign getSign() {
		return this.sign;
	}

	@Override
	public Member getMember() {
		return this.member;
	}

	@Override
	public int hashCode() {
		return (sign == Sign.INCLUDE ? 1 : -1) * this.member.hashCode() * 13
				+ operator.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (!(obj instanceof SelectionAction)) {
			return false;
		}
		final SelectionAction action = (SelectionAction) obj;
		return sign == action.sign && operator.equals(action.operator)
				&& member.equals(action.member);
	}

	@Override
	public String toString() {
		return sign + " " + operator + " on " + member.getUniqueName();
	}
}
