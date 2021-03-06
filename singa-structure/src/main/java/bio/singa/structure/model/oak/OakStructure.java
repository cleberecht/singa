package bio.singa.structure.model.oak;

import bio.singa.mathematics.vectors.Vector3D;
import bio.singa.chemistry.model.elements.ElementProvider;
import bio.singa.structure.model.families.LigandFamily;
import bio.singa.features.identifiers.LeafIdentifier;
import bio.singa.features.identifiers.PDBIdentifier;
import bio.singa.features.identifiers.UniqueAtomIdentifer;
import bio.singa.structure.model.interfaces.*;

import java.util.*;

/**
 * @author cl
 */
public class OakStructure implements Structure {

    /**
     * The branches this structure contains.
     */
    private final TreeMap<Integer, OakModel> models;

    /**
     * The PDB identifier of the structure.
     */
    private String pdbIdentifier;

    /**
     * The title of the structure.
     */
    private String title;

    private int lastAddedAtomIdentifier;

    public OakStructure() {
        models = new TreeMap<>();
    }

    public OakStructure(OakStructure structure) {
        pdbIdentifier = structure.getPdbIdentifier();
        title = structure.title;
        models = new TreeMap<>();
        lastAddedAtomIdentifier = structure.lastAddedAtomIdentifier;
        for (OakModel model : structure.models.values()) {
            models.put(model.getModelIdentifier(), model.getCopy());
        }
    }

    @Override
    public String getPdbIdentifier() {
        return pdbIdentifier;
    }

    public void setPdbIdentifier(String pdbIdentifier) {
        if (PDBIdentifier.PATTERN.matcher(pdbIdentifier).matches() || pdbIdentifier.equals(LeafIdentifier.DEFAULT_PDB_IDENTIFIER)) {
            this.pdbIdentifier = pdbIdentifier.toLowerCase();
        } else {
            throw new IllegalArgumentException("The pdb identifier must match to the pdb identifier pattern.");
        }
    }

    @Override
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public List<Model> getAllModels() {
        return new ArrayList<>(models.values());
    }

    @Override
    public Set<Integer> getAllModelIdentifiers() {
        return new HashSet<>(models.keySet());
    }

    @Override
    public Model getFirstModel() {
        return models.firstEntry().getValue();
    }

    @Override
    public Optional<Model> getModel(int modelIdentifier) {
        if (models.containsKey(modelIdentifier)) {
            return Optional.of(models.get(modelIdentifier));
        }
        return Optional.empty();
    }

    @Override
    public void removeModel(int modelIdentifier) {
        models.remove(modelIdentifier);
    }

    public void addModel(OakModel model) {
        models.put(model.getModelIdentifier(), model);
    }

    @Override
    public Optional<Chain> getChain(int modelIdentifier, String chainIdentifier) {
        final Optional<Model> optionalModel = getModel(modelIdentifier);
        return optionalModel.flatMap(model -> model.getChain(chainIdentifier));
    }

    @Override
    public List<Chain> getAllChains() {
        List<Chain> allChains = new ArrayList<>();
        for (OakModel model : models.values()) {
            allChains.addAll(model.getAllChains());
        }
        return allChains;
    }

    @Override
    public Chain getFirstChain() {
        return getFirstModel().getFirstChain();
    }

    @Override
    public List<LeafSubstructure<?>> getAllLeafSubstructures() {
        List<LeafSubstructure<?>> allLeafSubstructures = new ArrayList<>();
        for (OakModel model : models.values()) {
            allLeafSubstructures.addAll(model.getAllLeafSubstructures());
        }
        return allLeafSubstructures;
    }

    @Override
    public Optional<LeafSubstructure<?>> getLeafSubstructure(LeafIdentifier leafIdentifier) {
        final Optional<Chain> chainOptional = getChain(leafIdentifier.getModelIdentifier(), leafIdentifier.getChainIdentifier());
        return chainOptional.flatMap(chain -> chain.getLeafSubstructure(leafIdentifier));
    }

    @Override
    public LeafSubstructure<?> getFirstLeafSubstructure() {
        return getFirstModel().getFirstChain().getFirstLeafSubstructure();
    }

    @Override
    public boolean removeLeafSubstructure(LeafIdentifier leafIdentifier) {
        final Optional<Chain> chain = getChain(leafIdentifier.getModelIdentifier(), leafIdentifier.getChainIdentifier());
        if (chain.isPresent()) {
            if (chain.get().getLeafSubstructure(leafIdentifier).isPresent()) {
                chain.get().removeLeafSubstructure(leafIdentifier);
                return true;
            }
        }
        getAllAtoms();
        return false;
    }

    @Override
    public Optional<Atom> getAtom(Integer atomIdentifier) {
        for (LeafSubstructure leafSubstructure : getAllLeafSubstructures()) {
            final Optional<Atom> atom = leafSubstructure.getAtom(atomIdentifier);
            if (atom.isPresent()) {
                return atom;
            }
        }
        return Optional.empty();
    }

    public Optional<Map.Entry<UniqueAtomIdentifer, Atom>> getUniqueAtomEntry(int atomSerial) {
        for (Model model : getAllModels()) {
            for (Chain chain : model.getAllChains()) {
                for (LeafSubstructure leafSubstructure : chain.getAllLeafSubstructures()) {
                    for (Atom atom : leafSubstructure.getAllAtoms()) {
                        if (atom.getAtomIdentifier().equals(atomSerial)) {
                            UniqueAtomIdentifer identifier = new UniqueAtomIdentifer(pdbIdentifier, model.getModelIdentifier(),
                                    chain.getChainIdentifier(), leafSubstructure.getIdentifier().getSerial(), leafSubstructure.getIdentifier().getInsertionCode(),
                                    atomSerial);
                            return Optional.of(new AbstractMap.SimpleEntry<>(identifier, atom));
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    public Optional<Map.Entry<UniqueAtomIdentifer, Atom>> getAtomByCoordinate(Vector3D coordinate, double eps) {
        for (Model model : getAllModels()) {
            for (Chain chain : model.getAllChains()) {
                for (LeafSubstructure leafSubstructure : chain.getAllLeafSubstructures()) {
                    for (Atom atom : leafSubstructure.getAllAtoms()) {
                        if (atom.getPosition().almostEqual(coordinate, eps)) {
                            UniqueAtomIdentifer identifier = new UniqueAtomIdentifer(pdbIdentifier, model.getModelIdentifier(),
                                    chain.getChainIdentifier(), leafSubstructure.getIdentifier().getSerial(), leafSubstructure.getIdentifier().getInsertionCode(),
                                    atom.getAtomIdentifier());
                            return Optional.of(new AbstractMap.SimpleEntry<>(identifier, atom));
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }



    /**
     * Adds an {@link Atom} to the {@link Structure}
     * FIXME: atom serial overflow may happen (if exceeds 9999)
     *
     * @param chainIdentifier The identifier of the {@link Chain} to which it should be added.
     * @param threeLetterCode The three-letter code of the associated {@link LeafSubstructure}.
     * @param position The position of the {@link Atom}.
     */
    public void addAtom(String chainIdentifier, String threeLetterCode, Vector3D position) {
        Optional<Chain> optionalChain = getFirstModel().getChain(chainIdentifier);
        if (optionalChain.isPresent()) {
            OakChain chain = (OakChain) optionalChain.get();
            OakLigand leafSubstructure = new OakLigand(chain.getNextLeafIdentifier(), new LigandFamily(threeLetterCode));
            lastAddedAtomIdentifier++;
            leafSubstructure.addAtom(new OakAtom(lastAddedAtomIdentifier, ElementProvider.UNKOWN, "CA", position));
            chain.addLeafSubstructure(leafSubstructure);
        } else {
            throw new IllegalStateException("Unable to add atom to chain " + chainIdentifier + ", chain could not be found.");
        }
    }

    @Override
    public void removeAtom(Integer atomIdentifier) {
        for (LeafSubstructure leafSubstructure : getAllLeafSubstructures()) {
            final Optional<Atom> atom = leafSubstructure.getAtom(atomIdentifier);
            if (atom.isPresent()) {
                leafSubstructure.removeAtom(atomIdentifier);
                return;
            }
        }
    }


    public int getLastAddedAtomIdentifier() {
        return lastAddedAtomIdentifier;
    }

    public void setLastAddedAtomIdentifier(int lastAddedAtomIdentifier) {
        this.lastAddedAtomIdentifier = lastAddedAtomIdentifier;
    }

    @Override
    public Structure getCopy() {
        return new OakStructure(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OakStructure that = (OakStructure) o;

        return pdbIdentifier != null ? pdbIdentifier.equals(that.pdbIdentifier) : that.pdbIdentifier == null;
    }

    @Override
    public int hashCode() {
        return pdbIdentifier != null ? pdbIdentifier.hashCode() : 0;
    }

    @Override
    public String toString() {
        return flatToString();
    }

}
