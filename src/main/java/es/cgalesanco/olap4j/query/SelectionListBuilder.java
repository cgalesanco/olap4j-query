package es.cgalesanco.olap4j.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;

import org.olap4j.metadata.Level;
import org.olap4j.metadata.Member;

import es.cgalesanco.olap4j.query.Selection.Operator;
import es.cgalesanco.olap4j.query.Selection.Sign;
import es.cgalesanco.olap4j.query.SelectionTree.SelectionNode;

class SelectionListBuilder {
	private static class LevelAction {
		private Level level;
		private Sign sign;
		private int sequence;
		private List<SelectionAction> pendingActions;
		
		public LevelAction(Level l, Sign s, int seq) {
			level = l;
			sign = s;
			sequence = seq;
			pendingActions = new ArrayList<SelectionAction>();
		}
		
		public Level getLevel() {
			return level;
		}
		
		public Sign getSign() {
			return sign;
		}
		
		public int getSequence() {
			return sequence;
		}

		public void addActions(List<SelectionAction> selections) {
			pendingActions.addAll(selections);
		}
		
		public List<SelectionAction> getPendingActions() {
			return pendingActions;
		}
	}
	
	private List<SelectionAction> initialActions;
	private List<LevelAction> levelActions;
	
	public SelectionListBuilder(SortedMap<Level,Integer> includes, SortedMap<Level,Integer> excludes) {
		levelActions = new ArrayList<LevelAction>(includes.size()+excludes.size());
		initialActions = new ArrayList<SelectionAction>();
		
		for(Entry<Level,Integer> e : includes.entrySet()) {
			levelActions.add(new LevelAction(e.getKey(), Sign.INCLUDE, e.getValue()));
		}
		for(Entry<Level,Integer> e : excludes.entrySet()) {
			levelActions.add(new LevelAction(e.getKey(), Sign.EXCLUDE, e.getValue()));
		}
		
		Collections.sort(levelActions, new Comparator<LevelAction>(){
			@Override
			public int compare(LevelAction o1, LevelAction o2) {
				return o1.getSequence() - o2.getSequence();
			}});
	}
	
	public void addSelections(SelectionNode node) {
		MemberSelectionState state = node.getStatus();
		Level thisLevel = node.getMember().getLevel();
		Level previousLevel = null;
		if ( thisLevel.getDepth() > 0 )
			previousLevel = thisLevel.getHierarchy().getLevels().get(thisLevel.getDepth()-1);

		
		List<SelectionAction> selections = new ArrayList<SelectionAction>(); 
		
		LevelAction descendantsLevel = null;
		Sign sign = null;
		if ( (sign = state.getSelectionSign(Operator.DESCENDANTS)) != null ) {
			descendantsLevel = findLevelForSequence(node.getSequence());
			selections.add(new SelectionAction(node.getMember(), sign, Operator.DESCENDANTS));
		}
		
		LevelAction childrenLevel = null;
		if ( (sign = state.getSelectionSign(Operator.CHILDREN)) != null ) {
			LevelAction previousAction = findLevel(previousLevel);
			if ( previousAction == null ) { 
				selections.add(new SelectionAction(node.getMember(), sign, Operator.CHILDREN));
			} else if ( previousAction.getSign() != sign ) {
				childrenLevel = previousAction;
				selections.add(new SelectionAction(node.getMember(), sign, Operator.CHILDREN));
			} 
		}
		
		LevelAction memberLevel = null;
		if ( (sign = state.getSelectionSign(Operator.MEMBER)) != null ) {
			LevelAction thisAction = findLevel(thisLevel);
			if ( thisAction == null ) {
				selections.add(new SelectionAction(node.getMember(), sign, Operator.MEMBER));
			} else if ( thisAction.getSign() != sign ) {
				memberLevel = thisAction;
				selections.add(new SelectionAction(node.getMember(), sign, Operator.MEMBER));
			}
		}

		replaceIncludeChildren(selections);
		
		LevelAction targetLevel = descendantsLevel;
		targetLevel = findFartherLevel(childrenLevel, targetLevel);
		targetLevel = findFartherLevel(memberLevel, targetLevel);
		
		if ( targetLevel != null ) {
			targetLevel.addActions(selections);
		} else {
			initialActions.addAll(selections);
		}
	}
	
	public List<Selection> getResult() {
		List<Selection> r = new ArrayList<Selection>();
		r.addAll(initialActions);
		Iterator<LevelAction> it = this.levelActions.iterator();
		while( it.hasNext() ) {
			LevelAction action = it.next();
			
			r.add(new LevelSelection(action.getLevel(), action.getSign()));
			r.addAll(action.getPendingActions());
		}
		return r;
	}

	private void replaceIncludeChildren(List<SelectionAction> selections) {
		int size = selections.size();
		if ( size < 2 )
			return;
		
		SelectionAction lastAction = selections.get(size-1);
		SelectionAction beforeLastAction = selections.get(size-2);
		if ( lastAction.getOperator() == Operator.MEMBER && beforeLastAction.getOperator() == Operator.CHILDREN 
				&& lastAction.getSign() == beforeLastAction.getSign()) {
			Member m = lastAction.getMember();
			Sign  s = lastAction.getSign();
			selections.remove(size-1);
			selections.remove(size-2);
			selections.add(new SelectionAction(m,s,Operator.INCLUDE_CHILDREN));
		}
	}

	private LevelAction findFartherLevel(LevelAction childrenLevel,
			LevelAction targetLevel) {
		if ( childrenLevel != null ) {
			if ( targetLevel != null ) {
				if ( targetLevel.getSequence() < childrenLevel.getSequence() )
					targetLevel = childrenLevel;
			} else {
				targetLevel = childrenLevel;
			}
		}
		return targetLevel;
	}

	private LevelAction findLevel(Level thisLevel) {
		if ( thisLevel == null )
			return null;
		for(LevelAction l : levelActions) {
			if ( l.getLevel().equals(thisLevel) )
				return l;
		}
		return null;
	}

	private LevelAction findLevelForSequence(int sequence) {
		LevelAction current = null;
		for(int i = 0; i < levelActions.size(); ++i ) {
			LevelAction tested = levelActions.get(i); 
			if ( sequence == tested.getSequence() )
				return tested;
			else if ( sequence > tested.getSequence() )
				return current;
			
			current = tested;
		}
		return current;
	}
	
}
