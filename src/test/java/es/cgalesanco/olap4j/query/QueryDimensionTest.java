package es.cgalesanco.olap4j.query;

import java.util.EnumSet;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.olap4j.metadata.Hierarchy;
import org.olap4j.metadata.Member;

import es.cgalesanco.olap4j.query.Selection.Operator;
import es.cgalesanco.olap4j.query.Selection.Sign;

public class QueryDimensionTest {
	private static Hierarchy hierarchy;
	private static Member childMember;
	private static Member descendantMember;
	private static Member rootMember;
	private static CubeMock cube;
	
	public Query query;
	EnumSet<Operator> testOperators = EnumSet.of(Operator.MEMBER, Operator.CHILDREN, Operator.INCLUDE_CHILDREN, Operator.DESCENDANTS);

	@BeforeClass
	public static void setupFixture() throws Exception {
		cube = MetadataFixture.createCube();
		hierarchy = cube.getHierarchies().get("Time");
		rootMember = hierarchy.getRootMembers().get("1997");
		childMember = rootMember.getChildMembers().get("Q2");
		descendantMember = childMember.getChildMembers().get("5");
	}
	
	@Before
	public void setUp() {
		query = new Query("Test QueryHierarchy", cube);
	}
	
	public void testSelection(String msg, SelectionAction[] expectedSelections,
			SelectionAction[] userSelections) {
		QueryHierarchy dimension = query.getHierarchy(hierarchy.getName());
		for (SelectionAction op : userSelections) {
			dimension.apply(op);
		}
		List<Selection> resultSelections = dimension.listSelections();
		Assert.assertArrayEquals(msg, expectedSelections, resultSelections
				.toArray(new SelectionAction[resultSelections.size()]));
	}

	@Test
	public void testInitialState() {
		testSelection("", new SelectionAction[0], new SelectionAction[0]);
	}

	@Test
	public void testIncludeMember_IncludeChildren() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.MEMBER),
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.CHILDREN)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.INCLUDE_CHILDREN)
				},
				user
				);
		
	}

	@Test
	public void testIncludeMember_IncludeIncludeChildren() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.MEMBER),
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.INCLUDE_CHILDREN)}; 
		testSelection("Same Member: IM,IiCh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.INCLUDE_CHILDREN)
				},
				user
				);
		
	}
	
	@Test
	public void testIncludeMember_IncludeDescendants() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.MEMBER),
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS)}; 
		testSelection("Same Member: IM,ID", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS)
				},
				user
				);
	}
	
	@Test
	public void testIncludeMember_ExcludeMember() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.MEMBER),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.MEMBER)}; 
		testSelection("Same Member: IM,ID", 
				new SelectionAction[]{
				},
				user
				);
	}
	
	@Test
	public void testIncludeMember_ExcludeChildren() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.MEMBER),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.CHILDREN)}; 
		testSelection("Same Member: IM,ID", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.MEMBER),
				},
				user
				);
	}
	
	@Test
	public void testIncludeMember_ExcludeIncludeChildren() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.MEMBER),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.INCLUDE_CHILDREN)}; 
		testSelection("Same Member: IM,ID", 
				new SelectionAction[]{
				},
				user
				);
	}
	
	@Test
	public void testIncludeMember_ExcludeDescendants() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.MEMBER),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.DESCENDANTS)}; 
		testSelection("Same Member: IM,ID", 
				new SelectionAction[]{
				},
				user
				);
	}
	
	// Starts with Include Children
	@Test
	public void testIncludeChildren_IncludeMember() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.CHILDREN),
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.MEMBER)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.INCLUDE_CHILDREN)
				},
				user
				);
		
	}

	@Test
	public void testIncludeChildren_IncludeIncludeChildren() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.CHILDREN),
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.INCLUDE_CHILDREN)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.INCLUDE_CHILDREN)
				},
				user
				);
		
	}

	@Test
	public void testIncludeChildren_IncludeDescendants() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.CHILDREN),
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS)
				},
				user
				);
		
	}

	@Test
	public void testIncludeChildren_ExcludeMember() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.CHILDREN),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.MEMBER)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.CHILDREN)
				},
				user
				);
	}

	@Test
	public void testIncludeChildren_ExcludeChildren() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.CHILDREN),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.CHILDREN)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
				},
				user
				);
		
	}
	
	@Test
	public void testIncludeChildren_ExcludeIncludeChildren() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.CHILDREN),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.INCLUDE_CHILDREN)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
				},
				user
				);
		
	}
	
	@Test
	public void testIncludeChildren_ExcludeDescendants() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.CHILDREN),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.DESCENDANTS)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
				},
				user
				);
		
	}

	// Start include INCLUDE_CHILDREN
	@Test
	public void testIncludeIncludeChildren_IncludeMember() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.INCLUDE_CHILDREN),
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.MEMBER)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.INCLUDE_CHILDREN)
				},
				user
				);
	}
	
	@Test
	public void testIncludeIncludeChildren_IncludeChildren() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.INCLUDE_CHILDREN),
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.CHILDREN)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.INCLUDE_CHILDREN)
				},
				user
				);
	}
	
	@Test
	public void testIncludeIncludeChildren_IncludeDescendants() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.INCLUDE_CHILDREN),
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS)
				},
				user
				);
	}
	
	@Test
	public void testIncludeIncludeChildren_ExcludeMember() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.INCLUDE_CHILDREN),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.MEMBER)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.CHILDREN),
				},
				user
				);
	}
	
	@Test
	public void testIncludeChildren_IncludeChildren() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.CHILDREN),
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.INCLUDE_CHILDREN)}; 
		testSelection("Same Member: IM,ID", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.INCLUDE_CHILDREN)
				},
				user
				);
	}
	
	@Test
	public void testIncludeChildren_Descendants() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.CHILDREN),
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS)}; 
		testSelection("Same Member: IM,ID", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS)
				},
				user
				);
	}
	
	@Test
	public void testIncludeIncludeChildren_ExcludeChildren() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.INCLUDE_CHILDREN),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.CHILDREN)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.MEMBER),
				},
				user
				);
	}
	
	@Test
	public void testIncludeIncludeChildren_ExcludeDescendants() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.INCLUDE_CHILDREN),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.DESCENDANTS)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
				},
				user
				);
	}
	
	
	// Start with include Descendants

	@Test
	public void testIncludeDescendants_ExcludeMember() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.MEMBER)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
					new SelectionAction(rootMember, Sign.EXCLUDE, Operator.MEMBER)
				},
				user
				);
	}
	
	@Test
	public void testIncludeDescendants_ExcludeChildren() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.CHILDREN)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
					new SelectionAction(rootMember, Sign.EXCLUDE, Operator.CHILDREN)
				},
				user
				);
	}
	
	@Test
	public void testIncludeDescendants_ExcludeIncludeChildren() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.INCLUDE_CHILDREN)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
					new SelectionAction(rootMember, Sign.EXCLUDE, Operator.INCLUDE_CHILDREN)
				},
				user
				);
	}

	// Start with exclude Member
	@Test
	public void testExcludeMember_IncludeChildren() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.MEMBER),
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.CHILDREN)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.CHILDREN)
				},
				user
				);
	}
	
	@Test
	public void testExcludeMember_IncludeIncludeChildren() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.MEMBER),
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.INCLUDE_CHILDREN)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.INCLUDE_CHILDREN),
				},
				user
				);
	}
	
	@Test
	public void testExcludeMember_IncludeDescendants() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.MEMBER),
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
				},
				user
				);
	}
	
	@Test
	public void testExcludeMember_excludeChildren() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
				new SelectionAction(childMember, Sign.EXCLUDE, Operator.MEMBER),
				new SelectionAction(childMember, Sign.EXCLUDE, Operator.CHILDREN)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
					new SelectionAction(childMember, Sign.EXCLUDE, Operator.INCLUDE_CHILDREN),
				},
				user
				);
	}
	
	@Test
	public void testExcludeMember_excludeIncludeChildren() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
				new SelectionAction(childMember, Sign.EXCLUDE, Operator.MEMBER),
				new SelectionAction(childMember, Sign.EXCLUDE, Operator.INCLUDE_CHILDREN)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
					new SelectionAction(childMember, Sign.EXCLUDE, Operator.INCLUDE_CHILDREN),
				},
				user
				);
	}
	
	@Test
	public void testExcludeMember_excludeDescendants() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.MEMBER),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.DESCENDANTS)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
				},
				user
				);
	}
	
	// Start exclude Children
	
	@Test
	public void testExcludeChildren_includeMember() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.CHILDREN),
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.MEMBER)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.MEMBER),
				},
				user
				);
	}
	
	@Test
	public void testExcludeChildren_includeIncludeChildren() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.CHILDREN),
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.INCLUDE_CHILDREN)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.INCLUDE_CHILDREN),
				},
				user
				);
	}
	
	@Test
	public void testExcludeChildren_includeDescendant() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.CHILDREN),
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
				},
				user
				);
	}
	
	@Test
	public void testExcludeChildren_excludeMember() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
				new SelectionAction(childMember, Sign.EXCLUDE, Operator.CHILDREN),
				new SelectionAction(childMember, Sign.EXCLUDE, Operator.MEMBER)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
					new SelectionAction(childMember, Sign.EXCLUDE, Operator.INCLUDE_CHILDREN),
				},
				user
				);
	}
	
	@Test
	public void testExcludeChildren_excludeIncludeChildren() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
				new SelectionAction(childMember, Sign.EXCLUDE, Operator.CHILDREN),
				new SelectionAction(childMember, Sign.EXCLUDE, Operator.INCLUDE_CHILDREN)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
					new SelectionAction(childMember, Sign.EXCLUDE, Operator.INCLUDE_CHILDREN),
				},
				user
				);
	}
	
	@Test
	public void testExcludeChildren_excludeDescendants() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.CHILDREN),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.DESCENDANTS)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
				},
				user
				);
	}
	
	// Start with exclude INCLUDE_CHILDREN
	
	@Test
	public void testExcludeIncludeChildren_includeMember() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.INCLUDE_CHILDREN),
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.MEMBER)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.MEMBER),
				},
				user
				);
	}
	
	@Test
	public void testExcludeIncludeChildren_includeChildren() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
				new SelectionAction(childMember, Sign.EXCLUDE, Operator.INCLUDE_CHILDREN),
				new SelectionAction(childMember, Sign.INCLUDE, Operator.CHILDREN)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
					new SelectionAction(childMember, Sign.EXCLUDE, Operator.MEMBER)
				},
				user
				);
	}
	
	@Test
	public void testExcludeIncludeChildren_includeDescendants() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.INCLUDE_CHILDREN),
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS)
				},
				user
				);
	}
	
	@Test
	public void testExcludeIncludeChildren_excludeMember() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
				new SelectionAction(childMember, Sign.EXCLUDE, Operator.INCLUDE_CHILDREN),
				new SelectionAction(childMember, Sign.EXCLUDE, Operator.MEMBER)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
					new SelectionAction(childMember, Sign.EXCLUDE, Operator.INCLUDE_CHILDREN)
				},
				user
				);
	}
	
	@Test
	public void testExcludeIncludeChildren_excludeDescendants() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.INCLUDE_CHILDREN),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.DESCENDANTS)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
				},
				user
				);
	}
	
	@Test
	public void testExcludeIncludeChildren_excludeChildren() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.INCLUDE_CHILDREN),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.CHILDREN)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
					new SelectionAction(rootMember, Sign.EXCLUDE, Operator.INCLUDE_CHILDREN)
				},
				user
				);
	}
	
	// Starts exclude DESCENDANTS
	
	@Test
	public void testExcludeDescendants_includeMember() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.DESCENDANTS),
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.MEMBER)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.MEMBER)
				},
				user
				);
	}

	@Test
	public void testExcludeDescendants_includeChildren() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.DESCENDANTS),
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.CHILDREN)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.CHILDREN)
				},
				user
				);
	}

	@Test
	public void testExcludeDescendants_includeIncludeChildren() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.DESCENDANTS),
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.INCLUDE_CHILDREN)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.INCLUDE_CHILDREN)
				},
				user
				);
	}

	@Test
	public void testExcludeDescendants_excludeMember() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.DESCENDANTS),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.MEMBER)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
				},
				user
				);
	}

	@Test
	public void testExcludeDescendants_excludeChildren() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.DESCENDANTS),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.CHILDREN)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
				},
				user
				);
	}

	@Test
	public void testExcludeDescendants_excludeIncludeChildren() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.DESCENDANTS),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.INCLUDE_CHILDREN)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
				},
				user
				);
	}
	
	@Test
	public void testIncludeMemberExcludeChildren_includeChildren() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.MEMBER),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.CHILDREN),
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.CHILDREN)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.INCLUDE_CHILDREN)
				},
				user
				);
	}

	@Test
	public void testIncludeMemberExcludeChildren_includeIncludeChildren() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.MEMBER),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.CHILDREN),
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.INCLUDE_CHILDREN)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.INCLUDE_CHILDREN)
				},
				user
				);
	}
	
	@Test
	public void testIncludeMemberExcludeChildren_includeDescendants() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.MEMBER),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.CHILDREN),
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS)
				},
				user
				);
	}
	
	@Test
	public void testIncludeMemberExcludeChildren_excludeMember() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.MEMBER),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.CHILDREN),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.MEMBER)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
				},
				user
				);
	}
	
	@Test
	public void testIncludeMemberExcludeChildren_excludeIncludeChildren() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.MEMBER),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.CHILDREN),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.INCLUDE_CHILDREN)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
				},
				user
				);
	}
	
	@Test
	public void testIncludeMemberExcludeChildren_excludeDescendants() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.MEMBER),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.CHILDREN),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.DESCENDANTS)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
				},
				user
				);
	}
	
	@Test
	public void testIncludeDescendantsExcludeMember_includeMember() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.MEMBER),
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.MEMBER)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS)
				},
				user
				);
	}
	
	@Test
	public void testIncludeDescendantsExcludeMember_includeChildren() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.MEMBER),
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.CHILDREN)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
					new SelectionAction(rootMember, Sign.EXCLUDE, Operator.MEMBER)
				},
				user
				);
	}
	
	@Test
	public void testIncludeDescendantsExcludeMember_includeIncludeChildren() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.MEMBER),
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.INCLUDE_CHILDREN)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS)
				},
				user
				);
	}
	
	@Test
	public void testIncludeDescendantsExcludeMember_includeDescendants() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.MEMBER),
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS)
				},
				user
				);
	}

	@Test
	public void testIncludeDescendantsExcludeMember_excludeChildren() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.MEMBER),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.CHILDREN)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
					new SelectionAction(rootMember, Sign.EXCLUDE, Operator.INCLUDE_CHILDREN)
				},
				user
				);
	}
	
	@Test
	public void testIncludeDescendantsExcludeMember_excludeIncludeChildren() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.MEMBER),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.INCLUDE_CHILDREN)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
					new SelectionAction(rootMember, Sign.EXCLUDE, Operator.INCLUDE_CHILDREN)
				},
				user
				);
	}
	
	@Test
	public void testIncludeDescendantsExcludeMember_excludeDescendants() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.MEMBER),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.DESCENDANTS)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
				},
				user
				);
	}

	@Test
	public void testIncludeDescendantsExcludeChildren_includeMember() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.CHILDREN),
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.MEMBER)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
					new SelectionAction(rootMember, Sign.EXCLUDE, Operator.CHILDREN)
				},
				user
				);
	}
	
	@Test
	public void testIncludeDescendantsExcludeChildren_includeChildren() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.CHILDREN),
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.CHILDREN)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS)
				},
				user
				);
	}
	
	@Test
	public void testIncludeDescendantsExcludeChildren_includeIncludeChildren() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.CHILDREN),
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.INCLUDE_CHILDREN)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS)
				},
				user
				);
	}
	
	@Test
	public void testIncludeDescendantsExcludeChildren_includeDescendants() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.CHILDREN),
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS)
				},
				user
				);
	}

	@Test
	public void testIncludeDescendantsExcludeChildren_excludeMember() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.CHILDREN),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.MEMBER)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
					new SelectionAction(rootMember, Sign.EXCLUDE, Operator.INCLUDE_CHILDREN)
				},
				user
				);
	}

	@Test
	public void testIncludeDescendantsExcludeChildren_excludeChildren() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.CHILDREN),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.CHILDREN)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
					new SelectionAction(rootMember, Sign.EXCLUDE, Operator.CHILDREN)
				},
				user
				);
	}
	
	@Test
	public void testIncludeDescendantsExcludeChildren_excludeIncludeChildren() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.CHILDREN),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.INCLUDE_CHILDREN)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
					new SelectionAction(rootMember, Sign.EXCLUDE, Operator.INCLUDE_CHILDREN)
				},
				user
				);
	}

	@Test
	public void testIncludeDescendantsExcludeChildren_excludeDescendants() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.CHILDREN),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.DESCENDANTS)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
				},
				user
				);
	}
	
	@Test
	public void testIncludeDescendantsExcludeIncludeChildren_includeMember() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.INCLUDE_CHILDREN),
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.MEMBER)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
					new SelectionAction(rootMember, Sign.EXCLUDE, Operator.CHILDREN)
				},
				user
				);
		
	}
	
	@Test
	public void testIncludeDescendantsExcludeIncludeChildren_includeChildren() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.INCLUDE_CHILDREN),
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.CHILDREN)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
					new SelectionAction(rootMember, Sign.EXCLUDE, Operator.MEMBER)
				},
				user
				);
		
	}
	
	@Test
	public void testIncludeDescendantsExcludeIncludeChildren_includeIncludeChildren() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.INCLUDE_CHILDREN),
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.INCLUDE_CHILDREN)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS)
				},
				user
				);
		
	}
	
	@Test
	public void testIncludeDescendantsExcludeIncludeChildren_includeDescendants() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.INCLUDE_CHILDREN),
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS)
				},
				user
				);
		
	}
	
	@Test
	public void testIncludeDescendantsExcludeIncludeChildren_excludeMember() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.INCLUDE_CHILDREN),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.MEMBER)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
					new SelectionAction(rootMember, Sign.EXCLUDE, Operator.INCLUDE_CHILDREN),
				},
				user
				);
		
	}
	
	@Test
	public void testIncludeDescendantsExcludeIncludeChildren_excludeChildren() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.INCLUDE_CHILDREN),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.CHILDREN)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
					new SelectionAction(rootMember, Sign.EXCLUDE, Operator.INCLUDE_CHILDREN),
				},
				user
				);
		
	}
	
	@Test
	public void testIncludeDescendantsExcludeIncludeChildren_excludeIncludeChildren() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.INCLUDE_CHILDREN),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.INCLUDE_CHILDREN)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
					new SelectionAction(rootMember, Sign.EXCLUDE, Operator.INCLUDE_CHILDREN),
				},
				user
				);
		
	}
	
	@Test
	public void testIncludeDescendantsExcludeIncludeChildren_excludeDescendants() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.INCLUDE_CHILDREN),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.DESCENDANTS)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
				},
				user
				);
		
	}
	
	@Test
	public void testIncludeMember_childIncludeMember() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.MEMBER),
				new SelectionAction(childMember, Sign.INCLUDE, Operator.MEMBER)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.MEMBER),
					new SelectionAction(childMember, Sign.INCLUDE, Operator.MEMBER),
				},
				user
				);
		
	}
	
	@Test
	public void testIncludeChildren_childIncludeMember() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.CHILDREN),
				new SelectionAction(childMember, Sign.INCLUDE, Operator.MEMBER)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.CHILDREN)
				},
				user
				);
		
	}
	
	@Test
	public void testIncludeChildren_childIncludeChildren() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.CHILDREN),
				new SelectionAction(childMember, Sign.INCLUDE, Operator.CHILDREN)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.CHILDREN),
					new SelectionAction(childMember, Sign.INCLUDE, Operator.CHILDREN)
				},
				user
				);
		
	}
	
	@Test
	public void testIncludeChildren_childIncludeIncludeChildren() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.CHILDREN),
				new SelectionAction(childMember, Sign.INCLUDE, Operator.INCLUDE_CHILDREN)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.CHILDREN),
					new SelectionAction(childMember, Sign.INCLUDE, Operator.CHILDREN)
				},
				user
				);
		
	}
	
	@Test
	public void testIncludeChildren_childExcludeMember() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.CHILDREN),
				new SelectionAction(childMember, Sign.EXCLUDE, Operator.MEMBER)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.CHILDREN),
				new SelectionAction(childMember, Sign.EXCLUDE, Operator.MEMBER),
				},
				user
				);
		
	}
	
	@Test
	public void testIncludeChildren_childExcludeChildren() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.CHILDREN),
				new SelectionAction(childMember, Sign.EXCLUDE, Operator.CHILDREN)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.CHILDREN),
				},
				user
				);
	}
	
	@Test
	public void testIncludeIncludeChildren_childIncludeIncludeChildren() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.INCLUDE_CHILDREN),
				new SelectionAction(childMember, Sign.INCLUDE, Operator.INCLUDE_CHILDREN)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.INCLUDE_CHILDREN),
					new SelectionAction(childMember, Sign.INCLUDE, Operator.CHILDREN)
				},
				user
				);
		
	}
	
	@Test
	public void testIncludeDescendants_childIncludeMember() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
				new SelectionAction(childMember, Sign.INCLUDE, Operator.MEMBER)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS)
				},
				user
				);
		
	}
	
	@Test
	public void testIncludeDescendants_childIncludeChildren() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
				new SelectionAction(childMember, Sign.INCLUDE, Operator.CHILDREN)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS)
				},
				user
				);
		
	}
	
	@Test
	public void testIncludeDescendants_childExcludeMember() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
				new SelectionAction(childMember, Sign.EXCLUDE, Operator.MEMBER)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
					new SelectionAction(childMember, Sign.EXCLUDE, Operator.MEMBER)
				},
				user
				);
		
	}
	
	@Test
	public void testIncludeDescendantsExcludeMember_childIncludeMember() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.MEMBER),
				new SelectionAction(childMember, Sign.EXCLUDE, Operator.MEMBER)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
					new SelectionAction(rootMember, Sign.EXCLUDE, Operator.MEMBER),
					new SelectionAction(childMember, Sign.EXCLUDE, Operator.MEMBER)					
				},
				user
				);
		
	}
	
	@Test
	public void testIncludeDescendantsExcludeChildren_childExcludeMember() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.CHILDREN),
				new SelectionAction(childMember, Sign.EXCLUDE, Operator.MEMBER)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
					new SelectionAction(rootMember, Sign.EXCLUDE, Operator.CHILDREN)				
				},
				user
				);
	}
	
	@Test
	public void testIncludeMember_descendantIncludeMember() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.MEMBER),
				new SelectionAction(descendantMember, Sign.INCLUDE, Operator.MEMBER)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.MEMBER),
					new SelectionAction(descendantMember, Sign.INCLUDE, Operator.MEMBER)				
				},
				user
				);
	}
	
	@Test
	public void testIncludeMember_descendantExcludeMember() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.MEMBER),
				new SelectionAction(descendantMember, Sign.EXCLUDE, Operator.MEMBER)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.MEMBER)
				},
				user
				);
	}
	
	@Test
	public void testIncludeChildren_descendantIncludeMember() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.CHILDREN),
				new SelectionAction(descendantMember, Sign.INCLUDE, Operator.MEMBER)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.CHILDREN),
					new SelectionAction(descendantMember, Sign.INCLUDE, Operator.MEMBER)				
				},
				user
				);
	}
	
	@Test
	public void testIncludeDescendants_descendantIncludeChildren() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
				new SelectionAction(descendantMember, Sign.INCLUDE, Operator.CHILDREN)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
				},
				user
				);
	}
	
	@Test
	public void testIncludeMember_parentIncludeMember() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(childMember, Sign.INCLUDE, Operator.MEMBER),
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.MEMBER)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.MEMBER),
					new SelectionAction(childMember, Sign.INCLUDE, Operator.MEMBER),
				},
				user
				);
	}
	
	@Test
	public void testIncludeMember_parentIncludeChildren() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(childMember, Sign.INCLUDE, Operator.MEMBER),
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.CHILDREN)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.CHILDREN),
				},
				user
				);
	}
	
	@Test
	public void testIncludeMember_parentIncludeIncludeChildren() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(childMember, Sign.INCLUDE, Operator.MEMBER),
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.INCLUDE_CHILDREN)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.INCLUDE_CHILDREN),
				},
				user
				);
	}
	
	@Test
	public void testIncludeMember_parentIncludeDescendants() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(childMember, Sign.INCLUDE, Operator.MEMBER),
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS),
				},
				user
				);
	}
	
	@Test
	public void testIncludeMember_parentExcludeMember() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(childMember, Sign.INCLUDE, Operator.MEMBER),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.MEMBER)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(childMember, Sign.INCLUDE, Operator.MEMBER),
				},
				user
				);
	}
	
	@Test
	public void testIncludeMember_parentExcludeChildren() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(childMember, Sign.INCLUDE, Operator.MEMBER),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.CHILDREN)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
				},
				user
				);
	}

	@Test
	public void testIncludeMember_parentExcludeDescendants() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(childMember, Sign.INCLUDE, Operator.MEMBER),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.DESCENDANTS)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
				},
				user
				);
	}
	
	@Test
	public void testIncludeChildren_parentIncludeMember() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(childMember, Sign.INCLUDE, Operator.CHILDREN),
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.MEMBER)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.MEMBER),
					new SelectionAction(childMember, Sign.INCLUDE, Operator.CHILDREN),
				},
				user
				);
	}
	
	@Test
	public void testIncludeChildren_parentIncludeChildren() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(childMember, Sign.INCLUDE, Operator.CHILDREN),
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.CHILDREN)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.CHILDREN),
					new SelectionAction(childMember, Sign.INCLUDE, Operator.CHILDREN),
				},
				user
				);
	}
	
	@Test
	public void testIncludeChildren_parentIncludeDescendants() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(childMember, Sign.INCLUDE, Operator.CHILDREN),
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS)
				},
				user
				);
	}
	
	@Test
	public void testIncludeChildren_parentExcludeChildren() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(childMember, Sign.INCLUDE, Operator.CHILDREN),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.CHILDREN)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(childMember, Sign.INCLUDE, Operator.CHILDREN),
				},
				user
				);
	}
	
	@Test
	public void testIncludeChildren_parentExcludeDescendants() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(childMember, Sign.INCLUDE, Operator.CHILDREN),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.DESCENDANTS)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
				},
				user
				);
	}
	
	@Test
	public void testIncludeDescendants_parentIncludeChildren() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(childMember, Sign.INCLUDE, Operator.DESCENDANTS),
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.CHILDREN)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.CHILDREN), 
					new SelectionAction(childMember, Sign.INCLUDE, Operator.DESCENDANTS),
				},
				user
				);
	}
	
	@Test
	public void testIncludeDescendants_parentIncludeDescendants() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(childMember, Sign.INCLUDE, Operator.DESCENDANTS),
				new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
					new SelectionAction(rootMember, Sign.INCLUDE, Operator.DESCENDANTS), 
				},
				user
				);
	}
	
	@Test
	public void testIncludeDescendants_parentExcludeDescendants() {
		SelectionAction[] user = new SelectionAction[]{
				new SelectionAction(childMember, Sign.INCLUDE, Operator.DESCENDANTS),
				new SelectionAction(rootMember, Sign.EXCLUDE, Operator.DESCENDANTS)}; 
		testSelection("Same Member: IM,ICh", 
				new SelectionAction[]{
				},
				user
				);
	}
}
