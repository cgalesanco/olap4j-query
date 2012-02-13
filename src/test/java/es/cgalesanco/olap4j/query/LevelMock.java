package es.cgalesanco.olap4j.query;

import java.util.ArrayList;
import java.util.List;

import org.olap4j.OlapException;
import org.olap4j.impl.ArrayNamedListImpl;
import org.olap4j.metadata.Dimension;
import org.olap4j.metadata.Hierarchy;
import org.olap4j.metadata.Level;
import org.olap4j.metadata.Member;
import org.olap4j.metadata.NamedList;
import org.olap4j.metadata.Property;

public class LevelMock extends MetadataMock implements Level {

	private Hierarchy hierarchy;
	private Type type;
	private NamedList<Property> properties;
	private int depth;
	private List<Member> members;

	public LevelMock(Hierarchy hierarchyMock, String name, int depth) {
		super(name,name);
		this.hierarchy = hierarchyMock;
		this.depth = depth;
		this.members = new ArrayList<Member>();
		this.properties = new ArrayNamedListImpl<Property>() {
			private static final long serialVersionUID = 1L;

			@Override
			protected String getName(Property t) {
				return t.getUniqueName();
			}
		};
	}
	
	@Override
	public int getDepth() {
		return depth;
	}

	@Override
	public Hierarchy getHierarchy() {
		return hierarchy;
	}

	@Override
	public Dimension getDimension() {
		return hierarchy.getDimension();
	}

	@Override
	public Type getLevelType() {
		return type;
	}

	@Override
	public boolean isCalculated() {
		return false;
	}

	@Override
	public NamedList<Property> getProperties() {
		return properties;
	}

	@Override
	public List<Member> getMembers() throws OlapException {
		return members;
	}

	@Override
	public int getCardinality() {
		return members.size();
	}

	public void addMember(Member m) {
		members.add(m);
		((MemberMock)m).setLevel(this);
		
	}

}
