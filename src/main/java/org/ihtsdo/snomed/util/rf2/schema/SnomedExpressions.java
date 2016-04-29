package org.ihtsdo.snomed.util.rf2.schema;

public interface SnomedExpressions {
	
	public enum CONSTRAINT {
		DESCENDENT ("< "),
		DESCENDENT_OR_SELF ("<< ");
		
		private final String symbol;
		private CONSTRAINT (String symbol) {
			this.symbol = symbol;
		}
		
		public String toString() {
			return symbol;
		}
	}
}
