package es.cgalesanco.olap4j.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.olap4j.OlapException;
import org.olap4j.impl.ArrayNamedListImpl;
import org.olap4j.mdx.IdentifierSegment;
import org.olap4j.metadata.Cube;
import org.olap4j.metadata.Dimension;
import org.olap4j.metadata.Hierarchy;
import org.olap4j.metadata.Measure;
import org.olap4j.metadata.Member;
import org.olap4j.metadata.NamedList;
import org.olap4j.metadata.NamedSet;
import org.olap4j.metadata.Schema;
import org.olap4j.metadata.Member.TreeOp;

public class CubeMock implements Cube {
	private NamedList<Dimension> dimensions;
	
	public CubeMock(Dimension...dimensions) {
		this.dimensions = new ArrayNamedListImpl<Dimension>() {
			private static final long serialVersionUID = 1L;

			@Override
			protected String getName(Dimension t) {
				return t.getName();
			}
		};
		Collections.addAll(this.dimensions, dimensions);
	}

	@Override
	public String getName() {
		return "Mock Cube";
	}

	@Override
	public String getUniqueName() {
		return "[Mock Cube]";
	}

	@Override
	public String getCaption() {
		return "Mock cube caption";
	}

	@Override
	public String getDescription() {
		return null;
	}

	@Override
	public boolean isVisible() {
		return true;
	}

	@Override
	public Schema getSchema() {
		return null;
	}

	@Override
	public NamedList<Dimension> getDimensions() {
		return dimensions;
	}

	@Override
	public NamedList<Hierarchy> getHierarchies() {
		NamedList<Hierarchy> hierarchies = new ArrayNamedListImpl<Hierarchy>() {
			private static final long serialVersionUID = -5280763136908515254L;

			@Override
			protected String getName(Hierarchy t) {
				return t.getName();
			}
		};
		
		for(Dimension d : dimensions) {
			hierarchies.addAll(d.getHierarchies());
		}
		return hierarchies;
	}

	@Override
	public List<Measure> getMeasures() {
		return null;
	}

	@Override
	public NamedList<NamedSet> getSets() {
		return null;
	}

	@Override
	public Collection<Locale> getSupportedLocales() {
		ArrayList<Locale> locales = new ArrayList<Locale>();
		locales.add(Locale.ENGLISH);
		return locales;
	}

	@Override
	public Member lookupMember(List<IdentifierSegment> nameParts)
			throws OlapException {
		Hierarchy h = this.getHierarchies().get(nameParts.get(0).getName());
		Member m = h.getRootMembers().get(nameParts.get(1).getName());
		for(int i = 2; i < nameParts.size(); ++i) {
			m = m.getChildMembers().get(nameParts.get(i).getName());
		}
		return m;
	}

	@Override
	public List<Member> lookupMembers(Set<TreeOp> treeOps,
			List<IdentifierSegment> nameParts) throws OlapException {
		return null;
	}

	@Override
	public boolean isDrillThroughEnabled() {
		return false;
	}

}
