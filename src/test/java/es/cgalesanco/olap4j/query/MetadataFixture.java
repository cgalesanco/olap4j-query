package es.cgalesanco.olap4j.query;

import java.util.Calendar;

import org.olap4j.metadata.Member;

public class MetadataFixture {

	static public CubeMock createCube() throws Exception {
		DimensionMock timeDimension = createTimeDimension();
		DimensionMock measuresDimension = createMeasuresDimension();
		DimensionMock genderDimension = createGenderDimension();
		
		return new CubeMock(timeDimension, measuresDimension, genderDimension);
		
	}
	
	private static DimensionMock createGenderDimension() {
		DimensionMock genderDimension = new DimensionMock("Gender");
		HierarchyMock genderHierarchy = genderDimension.createHierarchy(null);
		genderHierarchy.createLevel("All");
		genderHierarchy.createLevel("Gender");
		MemberMock root = genderHierarchy.createRoot("All Gender");
		genderHierarchy.createMember(1, root, "Male");
		genderHierarchy.createMember(1, root, "Female");
		return genderDimension;
	}

	private static DimensionMock createMeasuresDimension() {
		DimensionMock measuresDimension = new DimensionMock("Measures");
		
		HierarchyMock measuresHierarchy = measuresDimension.createHierarchy(null);
		measuresHierarchy.createLevel("Measures");
		measuresHierarchy.createRoot("Unit Sales");
		measuresHierarchy.createRoot("Store Cost");
		measuresHierarchy.createRoot("Store Sales");
		
		return measuresDimension;
	}

	private static DimensionMock createTimeDimension() {
		DimensionMock timeDimension = new DimensionMock("Time");
		
		HierarchyMock timeByDayHierarchy = timeDimension
				.createHierarchy(null);
		timeByDayHierarchy.createLevel("Year");
		timeByDayHierarchy.createLevel("Quarter");
		timeByDayHierarchy.createLevel("Month");
		timeByDayHierarchy.createLevel("Day");
		for (int year = 1997; year <= 1998; ++year)
			createYear(timeByDayHierarchy, year);
		
		HierarchyMock timeWeekly = timeDimension.createHierarchy("Time.Weekly");
		timeWeekly.createLevel("Year");
		timeWeekly.createLevel("Week");
		timeWeekly.createLevel("Day");
		
		for(int year = 1997; year <= 1998; ++year) 
			createYearWeekly(timeWeekly, year);
		return timeDimension;
	}
	
	private static void createYearWeekly(HierarchyMock h, int y) {
		Member year = h.createRoot(Integer.toString(y));
		int lastWeek = 0;
		Calendar cal = Calendar.getInstance();
		cal.clear();
		cal.set(y, 0, 1);
		Member weekMember = null;
		for( ; cal.get(Calendar.YEAR) == y; cal.add(Calendar.DAY_OF_MONTH, 1)) {
			int week = cal.get(Calendar.WEEK_OF_YEAR);
			if ( week != lastWeek ) {
				weekMember = h.createMember(1, year, Integer.toString(week));
				lastWeek = week; 
			}
			
			h.createMember(2, weekMember, Integer.toString(cal.get(Calendar.DAY_OF_MONTH)));
		}
	}

	static private void createYear(HierarchyMock h, int y) {
		Member year = h.createRoot(Integer.toString(y));
		for (int q = 0; q < 4; ++q) {
			Member quarter = h.createMember(1, year, "Q" + (q + 1));
			for (int m = q * 3; m < (q + 1) * 3; ++m) {
				MemberMock month = h.createMember(2, quarter, Integer.toString(m + 1));
				Calendar cal = Calendar.getInstance();
				cal.clear();
				cal.set(y, m, 1);
				int maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
				for(int d = 0; d < maxDay; ++d) {
					h.createMember(3, month, Integer.toString(d+1));
				}
			}
		}
	}

	
}
