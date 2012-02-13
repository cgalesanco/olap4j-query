package es.cgalesanco.olap4j.query;

import org.olap4j.OlapException;
import org.olap4j.impl.ArrayNamedListImpl;
import org.olap4j.metadata.Dimension;
import org.olap4j.metadata.Hierarchy;
import org.olap4j.metadata.NamedList;

public class DimensionMock extends MetadataMock implements Dimension {

	private Type type;
	private NamedList<Hierarchy> hierarchies;
	
	public DimensionMock(String name) {
		super(name, name);
		this.type = Type.UNKNOWN;
		this.hierarchies = new ArrayNamedListImpl<Hierarchy>() {
			private static final long serialVersionUID = 1L;

			@Override
			protected String getName(Hierarchy t) {
				return t.getName();
			}
		};
	}

	@Override
	public NamedList<Hierarchy> getHierarchies() {
		return hierarchies;
	}

	@Override
	public Type getDimensionType() throws OlapException {
		return type;
	}

	@Override
	public Hierarchy getDefaultHierarchy() {
		return hierarchies.get(0);
	}
	
	public HierarchyMock createHierarchy(String uniqueName) {
		if ( uniqueName == null )
			uniqueName = name;
		HierarchyMock h = new HierarchyMock(this, uniqueName);
		hierarchies.add(h);
		return h;
	}

}
