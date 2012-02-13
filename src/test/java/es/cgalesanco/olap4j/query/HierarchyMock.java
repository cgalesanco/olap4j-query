package es.cgalesanco.olap4j.query;

import org.olap4j.OlapException;
import org.olap4j.impl.ArrayNamedListImpl;
import org.olap4j.metadata.Dimension;
import org.olap4j.metadata.Hierarchy;
import org.olap4j.metadata.Level;
import org.olap4j.metadata.Member;
import org.olap4j.metadata.NamedList;

public class HierarchyMock extends MetadataMock implements Hierarchy {

	private Dimension dimension;
	
	private Member defaultMember;
	private NamedList<Member> roots;
	private NamedList<Level> levels;
	
	
	public HierarchyMock(Dimension dimension, String name) {
		super(name, "["+name+"]");
		this.dimension = dimension; 
		roots = new ArrayNamedListImpl<Member>() {
			private static final long serialVersionUID = -8579714402488349298L;

			@Override
			protected String getName(Member t) {
				return t.getName();
			}
		};
		levels = new ArrayNamedListImpl<Level>() {
			private static final long serialVersionUID = -750247538736118709L;

			@Override
			protected String getName(Level t) {
				return t.getUniqueName();
			}
		};
	}

	@Override
	public Dimension getDimension() {
		return dimension;
	}

	@Override
	public NamedList<Level> getLevels() {
		return levels;
	}

	@Override
	public boolean hasAll() {
		return roots.size() == 1 && roots.get(0).isAll();
	}

	@Override
	public Member getDefaultMember() throws OlapException {
		return defaultMember;
	}

	@Override
	public NamedList<Member> getRootMembers() throws OlapException {
		return roots;
	}
	
	public LevelMock createLevel(String name) { 
		LevelMock l = new LevelMock(this, name, levels.size());
		levels.add(l);
		return l;
	}

	public MemberMock createRoot(String name) {
		Level l = levels.get(0);
		MemberMock m = new MemberMock(this, l, name);
		roots.add(m);
		((LevelMock)l).addMember(m);
		return m;
	}
	
	public MemberMock createMember(int levelOrdinal, Member parent, String name) {
		Level l = levels.get(levelOrdinal);
		MemberMock m = new MemberMock(parent, name);
		((LevelMock)l).addMember(m);
		return m;
	}
}
