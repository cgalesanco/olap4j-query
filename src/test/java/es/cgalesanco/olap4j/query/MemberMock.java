package es.cgalesanco.olap4j.query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.olap4j.OlapException;
import org.olap4j.impl.ArrayNamedListImpl;
import org.olap4j.mdx.ParseTreeNode;
import org.olap4j.metadata.Dimension;
import org.olap4j.metadata.Hierarchy;
import org.olap4j.metadata.Level;
import org.olap4j.metadata.Member;
import org.olap4j.metadata.NamedList;
import org.olap4j.metadata.Property;

public class MemberMock extends MetadataMock implements Member {

	private NamedList<Member> children;
	private Member parent;
	private Hierarchy hierarchy;
	private Type type;
	private boolean isAll;
	private Level level;
	private int ordinal;
	private boolean hidden;
	private NamedList<Property> properties;
	private Map<String,Object> propertyValues;

	public MemberMock(Hierarchy h, Level level, String name) {
		super(name, h.getUniqueName()+".["+name+"]");
		this.hierarchy = h;
		this.parent = null;
		this.level = level;
		this.ordinal = 0;
		this.hidden = false;
		this.properties = new ArrayNamedListImpl<Property>(){
			private static final long serialVersionUID = 2900139010442712667L;

			@Override
			protected String getName(Property t) {
				return t.getUniqueName();
			}
		};
		this.propertyValues = new HashMap<String,Object>();
		this.children = new ArrayNamedListImpl<Member>() {
			private static final long serialVersionUID = 5530859817468323033L;

			@Override
			protected String getName(Member t) {
				return t.getName();
			} 
		};
	}

	public MemberMock(Member parent, String name) {
		super(name, getUniqueName(parent,name));
		this.hierarchy = parent.getHierarchy();
		this.parent = parent; 
		this.ordinal = 0;
		this.hidden = false; 
		this.properties = new ArrayNamedListImpl<Property>(){
			private static final long serialVersionUID = 2900139010442712667L;
			@Override
			protected String getName(Property t) {
				return t.getUniqueName();
			}
		};
		this.propertyValues = new HashMap<String,Object>();
		this.children = new ArrayNamedListImpl<Member>() {
			private static final long serialVersionUID = 5530859817468323033L;

			@Override
			protected String getName(Member t) {
				return t.getName();
			} 
		};
		if ( this.parent != null ) {
			ordinal = ((MemberMock)this.parent).children.size();
			((MemberMock)this.parent).children.add(this);
		}
	}
	
	
	private static String getUniqueName(Member parent, String name) {
		StringBuilder sb = new StringBuilder();
		sb.append('[');
		sb.append(name);
		sb.append(']');
		Hierarchy h = parent.getHierarchy();
		while(parent != null && !parent.isAll() ) {
			sb.insert(0, "].");
			sb.insert(0, parent.getName());
			sb.insert(0,"[");
			parent = parent.getParentMember();
		}
		sb.insert(0,".");
		sb.insert(0, h.getUniqueName());
		
		return sb.toString();
	}

	@Override
	public NamedList<? extends Member> getChildMembers() throws OlapException {
		return children;
	}

	@Override
	public int getChildMemberCount() throws OlapException {
		return children.size();
	}

	@Override
	public Member getParentMember() {
		return parent;
	}

	@Override
	public Level getLevel() {
		return level;
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
	public Type getMemberType() {
		return type;
	}

	@Override
	public boolean isAll() {
		return isAll;
	}

	@Override
	public boolean isChildOrEqualTo(Member member) {
		if ( member.getHierarchy().equals(this.getHierarchy()))
			throw new UnsupportedOperationException();
		if ( this.equals(member) ) 
			return true;
		
		for(Member child : children) {
			if ( child.isChildOrEqualTo(member))
				return true;
		}
		return false;
	}

	@Override
	public boolean isCalculated() {
		return false;
	}

	@Override
	public int getSolveOrder() {
		return 0;
	}

	@Override
	public ParseTreeNode getExpression() {
		return null;
	}

	@Override
	public List<Member> getAncestorMembers() {
		List<Member> ancestors = new ArrayList<Member>();
		Member m = parent;
		while( m != null ) {
			ancestors.add(m);
			m = m.getParentMember();
		}
			
		return ancestors;
	}

	@Override
	public boolean isCalculatedInQuery() {
		return false;
	}

	@Override
	public Object getPropertyValue(Property property) throws OlapException {
		return propertyValues.get(property.getUniqueName());
	}

	@Override
	public String getPropertyFormattedValue(Property property)
			throws OlapException {
		return propertyValues.get(property.getUniqueName()).toString();
	}

	@Override
	public void setProperty(Property property, Object value)
			throws OlapException {
		if ( properties.contains(property) )
			propertyValues.put(property.getUniqueName(), value);
	}

	@Override
	public NamedList<Property> getProperties() {
		return properties;
	}

	@Override
	public int getOrdinal() {
		return ordinal;
	}

	@Override
	public boolean isHidden() {
		return hidden;
	}

	@Override
	public int getDepth() {
		return getAncestorMembers().size();
	}

	@Override
	public Member getDataMember() {
		throw new RuntimeException("unimplemented");
	}

	public void setLevel(Level level) {
		this.level = level;
	}

	public void setAll(boolean b) {
		this.isAll = true;
	}

}
