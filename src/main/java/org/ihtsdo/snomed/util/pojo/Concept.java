package org.ihtsdo.snomed.util.pojo;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.ihtsdo.snomed.util.rf2.schema.RF2SchemaConstants;

public class Concept implements Comparable<Concept>, RF2SchemaConstants {

	private static Map<Long, Concept> allStatedConcepts = new HashMap<Long, Concept>();
	private static Map<Long, Concept> allInferredConcepts = new HashMap<Long, Concept>();
	private static Map<Long, Boolean> fullyDefinedMap = new HashMap<Long, Boolean>();

	private Long sctId;
	private boolean isFullyDefined = false;
	Set<Concept> parents = new TreeSet<Concept>();
	Set<Concept> children = new TreeSet<Concept>();
	List<RelationshipGroup> groups = new ArrayList<RelationshipGroup>();
	private GroupsHash groupsHash = null;
	public static final int DEPTH_NOT_SET = -1;
	public static final int IMMEDIATE_CHILDREN_ONLY = 1;
	private int depth = DEPTH_NOT_SET;

	public Concept(Long id) {
		this.sctId = id;
	}

	public static Concept getConcept(long sctId, CHARACTERISTIC characteristic) {
		Map<Long, Concept> allConcepts = characteristic.equals(Relationship.CHARACTERISTIC.STATED) ? allStatedConcepts
				: allInferredConcepts;
		return allConcepts.get(sctId);
	}

	public static Concept registerConcept(String sctIdStr, CHARACTERISTIC characteristic) {
		Map<Long, Concept> allConcepts = characteristic.equals(Relationship.CHARACTERISTIC.STATED) ? allStatedConcepts
				: allInferredConcepts;
		Long sctId = new Long(sctIdStr);
		// Do we know about this concept?
		Concept concept;
		if (!allConcepts.containsKey(sctId)) {
			concept = new Concept(sctId);
			if (fullyDefinedMap.containsKey(sctId) && fullyDefinedMap.get(sctId).equals(Boolean.TRUE)) {
				concept.setFullyDefined(true);
			}
			allConcepts.put(sctId, concept);
		} else {
			concept = allConcepts.get(sctId);
		}
		return concept;
	}

	public static Collection<Concept> getAllConcepts(CHARACTERISTIC characteristic) {
		Map<Long, Concept> allConcepts = characteristic.equals(Relationship.CHARACTERISTIC.STATED) ? allStatedConcepts
				: allInferredConcepts;
		return allConcepts.values();
	}

	private void setFullyDefined(boolean b) {
		this.isFullyDefined = b;
	}

	public boolean isFullyDefined() {
		return this.isFullyDefined;
	}

	public static void addFullyDefined(String sctIdStr) {
		fullyDefinedMap.put(Long.valueOf(sctIdStr), Boolean.TRUE);
	}

	public void addAttribute(Relationship r) {
		assert this.equals(r.getSourceConcept());

		// Is this an IS A relationship? Add as a parent if so
		if (r.isISA()) {
			parents.add(r.getDestinationConcept());
			// And tell that parent that it has a child
			r.getDestinationConcept().children.add(this);
		} else {
			// Resize groups if required
			for (int x = groups.size(); x <= r.getGroup(); x++) {
				groups.add(new RelationshipGroup(x));
			}
			groups.get(r.getGroup()).addAttribute(r);
		}
	}

	@Override
	public int compareTo(Concept other) {
		return this.sctId.compareTo(other.sctId);
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Concept) {
			Concept otherConcept = (Concept) other;
			return this.sctId.equals(otherConcept.sctId);
		}
		return false;
	}

	public Long getSctId() {
		return sctId;
	}

	public Set<Concept> getDescendents(int depth, boolean fullyDefinedOnly) {
		return populateDescendents(new HashSet<Concept>(), fullyDefinedOnly, depth);
	}

	private Set<Concept> populateDescendents(Set<Concept> allDescendents, boolean fullyDefinedOnly, int depth) {
		for (Concept thisChild : children) {
			if (!fullyDefinedOnly || (fullyDefinedOnly && thisChild.isFullyDefined)) {
				allDescendents.add(thisChild);
			}
			if (depth == DEPTH_NOT_SET || depth > 1) {
				int newDepth = depth == DEPTH_NOT_SET ? DEPTH_NOT_SET : depth - 1;
				thisChild.populateDescendents(allDescendents, fullyDefinedOnly, newDepth);
			}
		}
		return allDescendents;
	}

	public Set<Concept> getParents() {
		return parents;
	}

	public List<RelationshipGroup> getGroups() {
		return groups;
	}

	/**
	 * @return the sum of the hashes of the groupTypesUUIDs this should uniquely identify a model shape for a concept
	 * @throws UnsupportedEncodingException
	 */
	public GroupsHash getGroupsShapeHash() throws UnsupportedEncodingException {
		if (groupsHash == null) {
			long groupsHashLong = 0L;
			Set<GroupShape> contributingGroups = new HashSet<GroupShape>();
			for (RelationshipGroup g : groups) {
				groupsHashLong += g.getGroupBasicShape().hashCode();
				contributingGroups.add(g.getGroupBasicShape());
			}
			groupsHash = GroupsHash.get(Long.toString(Math.abs(groupsHashLong)));
			groupsHash.setHashStructure(contributingGroups);
		}
		return groupsHash;
	}

	public Concept getAncestor(int level) {
		// How far up the ancestry hierarchy are we working?
		Concept thisAncestor = this;
		while (level > 0) {
			level--;
			// Ensure that it only has one parent
			assert (thisAncestor.parents.size() == 1);
			thisAncestor = thisAncestor.parents.toArray(new Concept[] {})[0];
		}
		return thisAncestor;
	}

	public String toString() {
		return Long.toString(sctId);
	}

	public Collection<Relationship> getAllAttributes() {
		Set<Relationship> allAttributes = new HashSet<Relationship>();
		for (RelationshipGroup thisGroup : groups) {
			allAttributes.addAll(thisGroup.getAttributes());
		}
		return allAttributes;
	}

	public int getDepth() {
		return depth;
	}

	public void setDepth(int depth) {
		// We'll maintain the shortest possible path, so don't allow
		// depth to increase
		if (this.depth == DEPTH_NOT_SET || depth < this.depth) {
			this.depth = depth;
		}
	}

	private void populateAllAncestors(Set<Concept> ancestors, int depth) {
		for (Concept thisParent : parents) {
			ancestors.add(thisParent);
			if (depth == DEPTH_NOT_SET || depth > 1) {
				int newDepth = depth == DEPTH_NOT_SET ? DEPTH_NOT_SET : depth - 1;
				thisParent.populateAllAncestors(ancestors, newDepth);
			}
		}
	}

	public Set<Concept> getAncestors(int depth) {
		Set<Concept> allAncestorsAndSelf = new HashSet<Concept>();
		this.populateAllAncestors(allAncestorsAndSelf, depth);
		return allAncestorsAndSelf;
	}

	public Set<Concept> getAncestorsAndSelf(int depth) {
		Set<Concept> allAncestorsAndSelf = getAncestors(depth);
		allAncestorsAndSelf.add(this);
		return allAncestorsAndSelf;
	}

	public Set<Relationship> getGroup(int i) {
		return groups.size() > i ? groups.get(i).getAttributes() : new HashSet<Relationship>();
	}

}
