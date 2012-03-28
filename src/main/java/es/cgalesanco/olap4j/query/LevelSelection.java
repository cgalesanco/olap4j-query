package es.cgalesanco.olap4j.query;

import org.olap4j.metadata.Level;
import org.olap4j.metadata.MetadataElement;

import es.cgalesanco.olap4j.query.Selection.Sign;

public class LevelSelection implements Selection {
	private Sign sign;
	private Level level;
	
	public LevelSelection(Level l, Sign s) {
		level = l;
		sign = s;
	}

	@Override
	public MetadataElement getRoot() {
		return getLevel();
	}
	
	public Level getLevel() {
		return level;
	}

	@Override
	public Sign getSign() {
		return sign;
	}

	@Override
	public Operator getOperator() {
		return null;
	}

	@Override
	public String toString() {
		return String.format("%1$s %2$s", sign, level.getCaption());
	}
	
	@Override
	public int hashCode() {
		return (sign == Sign.INCLUDE ? 1 : -1) * this.level.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (!(obj instanceof LevelSelection)) {
			return false;
		}
		final LevelSelection action = (LevelSelection) obj;
		return sign == action.sign && level.equals(action.level);
	}

	
}
