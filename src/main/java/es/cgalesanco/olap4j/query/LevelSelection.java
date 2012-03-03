package es.cgalesanco.olap4j.query;

import org.olap4j.metadata.Level;
import org.olap4j.metadata.MetadataElement;

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

}
