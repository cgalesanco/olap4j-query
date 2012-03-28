package es.cgalesanco.olap4j.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.olap4j.metadata.Level;

import es.cgalesanco.olap4j.query.SelectionTree.SelectionNode;

class SelectionListBuilder implements SelectionNodeVisitor {
	private NavigableMap<Integer,List<SelectionAction>> selectionsList;
	private NavigableMap<Integer, LevelSelection> levelActions;
	
	
	public SelectionListBuilder(NavigableMap<Level,SelectionInfo> levelSelections) {
		selectionsList = new TreeMap<Integer,List<SelectionAction>>();
		this.levelActions = new TreeMap<Integer, LevelSelection>();
		for(Entry<Level,SelectionInfo> eSels : levelSelections.entrySet()) {
			levelActions.put(
					eSels.getValue().getSequence(),
					new LevelSelection(eSels.getKey(), eSels.getValue().getSign())
					);
		}
	}
	
	public List<Selection> getResult() {
		List<Selection> result = new ArrayList<Selection>(selectionsList.size()*2);

		for(Entry<Integer, List<SelectionAction>> eSel : selectionsList.entrySet()) {
			Integer seq = eSel.getKey();
			
			while(!levelActions.isEmpty() && levelActions.firstKey() <= seq) {
				result.add(levelActions.firstEntry().getValue());
				levelActions.remove(levelActions.firstKey());
			}
			
			result.addAll(eSel.getValue());
		}
		
		if ( !selectionsList.isEmpty() ) {
			result.addAll(levelActions.tailMap(selectionsList.lastKey()).values());
		} else {
			result.addAll(levelActions.values());
		}
		
		return result;
	}

	@Override
	public void visitLeave(SelectionNode selectionNode) {
	}

	@Override
	public boolean visitEnter(SelectionNode node) {
		if ( node.getMember() == null )
			return true;
		
		for(Entry<Integer,List<SelectionAction>> eSel : node.listSelections().entrySet()) {
			List<SelectionAction> nodeSelections = selectionsList.get(eSel.getKey());
			if ( nodeSelections == null )
				selectionsList.put(eSel.getKey(), eSel.getValue());
			else
				nodeSelections.addAll(eSel.getValue());
		}
		return true;
	}

}
