package magic.firemind;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ScoringSet {
	
	public int fitness;
	private String[] mutatableKeys = {"scorePermanents", "scoreHand", "scoreLife", "scorePower"};
	
	protected Map<String, Integer> scoringHash;
	public ScoringSet(){
		scoringHash =  new HashMap<>();
		scoringHash.put("scorePermanents", 13);
		scoringHash.put("scorePower", 14);
		scoringHash.put("scoreToughness", 10);
		scoringHash.put("scoreHand", 3);
		scoringHash.put("scoreGraveyard", 0);
		scoringHash.put("scoreExiled", 0);
		scoringHash.put("scoreLife", 3);
		scoringHash.put("scorePoison", -5);
	}
	public ScoringSet(ScoringSet origScoringSet) {
		scoringHash =  new HashMap<>();
		for(String key: origScoringSet.scoringHash.keySet()){
			scoringHash.put(key, origScoringSet.scoringHash.get(key));
		}
	}
	
	public int get(String key){
		return scoringHash.get(key);
	}	
	
	public int put(String key, Integer value){
		return scoringHash.put(key, value);
	}
	
	public Set<String> keys(){
		return scoringHash.keySet();
	}
	
	
	public String[] mutatableKeys(){
		return mutatableKeys;
	}
	
	public void print() {
		System.out.println("Fitness: "+fitness);
		for(String key: scoringHash.keySet()){
			System.out.println(key+": "+scoringHash.get(key));
		}
	}
	
	@Override
	public boolean equals(Object other){
		if (!(other instanceof ScoringSet))
            return false;
        if (other == this)
            return true;

        return scoringHash.equals(((ScoringSet)other).scoringHash);
	}
}
