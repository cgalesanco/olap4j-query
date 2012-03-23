package es.cgalesanco.olap4j.query;

import es.cgalesanco.olap4j.query.Selection.Sign;

class SelectionInfo {
	private Sign sign;
	private int sequence;
	
	public SelectionInfo(Sign s, int seq) {
		sign = s;
		sequence = seq;
	}
	
	public Sign getSign() {
		return sign;
	}
	
	public int getSequence() {
		return sequence;
	}
}
