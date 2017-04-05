package de.bioforscher.singa.chemistry.parser.pdb.structures;

import de.bioforscher.singa.chemistry.parser.pdb.ligands.LigandParserService;
import de.bioforscher.singa.chemistry.parser.pdb.structures.tokens.*;
import de.bioforscher.singa.chemistry.physical.atoms.Atom;
import de.bioforscher.singa.chemistry.physical.branches.Chain;
import de.bioforscher.singa.chemistry.physical.branches.StructuralModel;
import de.bioforscher.singa.chemistry.physical.families.AminoAcidFamily;
import de.bioforscher.singa.chemistry.physical.families.LeafFactory;
import de.bioforscher.singa.chemistry.physical.families.NucleotideFamily;
import de.bioforscher.singa.chemistry.physical.leafes.AminoAcid;
import de.bioforscher.singa.chemistry.physical.leafes.LeafSubstructure;
import de.bioforscher.singa.chemistry.physical.leafes.Nucleotide;
import de.bioforscher.singa.chemistry.physical.model.LeafIdentifier;
import de.bioforscher.singa.chemistry.physical.model.Structure;
import de.bioforscher.singa.chemistry.physical.model.UniqueAtomIdentifer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author cl
 */
public class StructureCollector {

    private static final Logger logger = LoggerFactory.getLogger(StructureCollector.class);
    private static final String PDB_FETCH_URL = "https://files.rcsb.org/download/%s.pdb";

    private String currentPDB = "0000";
    private int currentModel = 0;

    private Map<UniqueAtomIdentifer, Atom> atoms;
    private Map<LeafIdentifier, String> leafNames;

    private ContentTreeNode contentTree;

    private StructureParser.Reducer reducer;
    private List<String> pdbLines;


    public StructureCollector() {
        this.atoms = new HashMap<>();
        this.leafNames = new TreeMap<>();
    }

    public StructureCollector(List<String> pdbLines, StructureParser.Reducer reducer) {
        this.reducer = reducer;
        this.pdbLines = pdbLines;
        this.atoms = new HashMap<>();
        this.leafNames = new TreeMap<>();
    }

    static Structure parse(List<String> pdbLines, StructureParser.Reducer reducer) {
        StructureCollector collector = new StructureCollector(pdbLines, reducer);
        collector.reduceLines();
        return collector.collectStructure();
    }

    private void reduceLines() {
        String firstLine = this.pdbLines.get(0);
        // parse meta information
        if (TitleToken.RECORD_PATTERN.matcher(firstLine).matches()) {
            this.currentPDB = TitleToken.ID_CODE.extract(firstLine);
        }
        if (this.reducer.parseMapping) {
            this.reducer.updatePdbIdentifer();
            this.reducer.updateChainIdentifier();
            this.reduceToChain(this.reducer.chainIdentifier);
            logger.info("Parsing structure {} chain {}", this.reducer.pdbIdentifier, this.reducer.chainIdentifier);
            return;
        }
        if (!this.reducer.allModels) {
            // parse only specific model
            // reduce lines to specific model
            this.reduceToModel(this.reducer.modelIdentifier);
        }
        if (!this.reducer.allChains) {
            // parse only specific chain
            // reduce lines to specific chain
            this.reduceToChain(this.reducer.chainIdentifier);
        }
    }

    private void reduceToModel(int modelIdentifier) {
        List<String> reducedList = new ArrayList<>();
        boolean collectLines = false;
        // for each line
        for (String currentLine : this.pdbLines) {
            // check if the correct model has begun
            if (ModelToken.RECORD_PATTERN.matcher(currentLine).matches()) {
                int currentModel = Integer.valueOf(ModelToken.MODEL_SERIAL.extract(currentLine));
                // turn on collection of lines
                if (currentModel == modelIdentifier) {
                    this.currentModel = currentModel;
                    collectLines = true;
                    continue;
                }
            }
            // check if the terminator is hit prevent further parsing
            if (collectLines && TerminatorTokens.MODEL_TERMINATOR.matcher(currentLine).matches()) {
                break;
            }
            // collect lines if we are in the correct model
            if (collectLines) {
                reducedList.add(currentLine);
            }
        }
        this.pdbLines = reducedList;
    }

    private void reduceToChain(String chainIdentifier) {
        List<String> reducedList = new ArrayList<>();
        // for each line
        for (String currentLine : this.pdbLines) {
            // check if this is a atom line
            if (AtomToken.RECORD_PATTERN.matcher(currentLine).matches()) {
                String currentChain = AtomToken.CHAIN_IDENTIFIER.extract(currentLine);
                // collect line if it has the correct chain
                if (currentChain.equals(chainIdentifier)) {
                    reducedList.add(currentLine);
                }
            }
            if (ModelToken.RECORD_PATTERN.matcher(currentLine).matches()) {
                // keel lines that indicate models
                reducedList.add(currentLine);
            }
        }
        this.pdbLines = reducedList;
    }

    private Structure collectStructure() {
        logger.debug("Collecting content from {} PDB lines", this.pdbLines.size());
        collectAtomInformation();
        createContentTree();

        Structure structure = new Structure();
        structure.setPdbIdentifier(this.contentTree.getIdentifier());

        logger.debug("creating structure");
        int chainGraphId = 0;
        for (ContentTreeNode modelNode : this.contentTree.getNodesFromLevel(ContentTreeNode.StructureLevel.MODEL)) {
            logger.debug("collecting chains for model {}", modelNode.getIdentifier());
            StructuralModel model = new StructuralModel(Integer.valueOf(modelNode.getIdentifier()));
            for (ContentTreeNode chainNode : modelNode.getNodesFromLevel(ContentTreeNode.StructureLevel.CHAIN)) {
                logger.trace("collecting leafs for chain {}", chainNode.getIdentifier());
                Chain chain = new Chain(chainGraphId++);
                chain.setChainIdentifier(chainNode.getIdentifier());
                for (ContentTreeNode leafNode : chainNode.getNodesFromLevel(ContentTreeNode.StructureLevel.LEAF)) {
                    LeafSubstructure<?, ?> leafSubstructure = assignLeaf(leafNode, Integer.valueOf(modelNode.getIdentifier()), chainNode.getIdentifier());
                    chain.addSubstructure(leafSubstructure);
                }
                model.addSubstructure(chain);
            }
            structure.addSubstructure(model);
        }
        structure.getAllChains().forEach(Chain::connectChainBackbone);
        return structure;
    }

    private void collectAtomInformation() {
        logger.debug("collecting information about atoms from {} PDB lines", this.pdbLines.size());
        for (String currentLine : this.pdbLines) {
            if (AtomToken.RECORD_PATTERN.matcher(currentLine).matches()) {
                UniqueAtomIdentifer identifier = createUniqueAtomIdentifier(currentLine);
                this.atoms.put(identifier, AtomToken.assembleAtom(currentLine));
                this.leafNames.put(new LeafIdentifier(identifier.getPdbIdentifier(), identifier.getModelIdentifier(), identifier.getChainIdentifier(), identifier.getLeafIdentifer()), AtomToken.RESIDUE_NAME.extract(currentLine));
            } else if (ModelToken.RECORD_PATTERN.matcher(currentLine).matches()) {
                this.currentModel = Integer.valueOf(ModelToken.MODEL_SERIAL.extract(currentLine));
            }
        }
    }

    private void createContentTree() {
        logger.debug("creating content tree");
        this.contentTree = new ContentTreeNode(this.currentPDB, ContentTreeNode.StructureLevel.STRUCTURE);
        this.atoms.forEach((identifer, atom) -> this.contentTree.appendAtom(atom, identifer));
    }

    private UniqueAtomIdentifer createUniqueAtomIdentifier(String atomLine) {
        int atomSerial = Integer.valueOf(AtomToken.ATOM_SERIAL.extract(atomLine));
        String chain = AtomToken.CHAIN_IDENTIFIER.extract(atomLine);
        int leaf = Integer.valueOf(AtomToken.RESIDUE_SERIAL.extract(atomLine));
        return new UniqueAtomIdentifer(this.currentPDB, this.currentModel, chain, leaf, atomSerial);
    }

    private LeafSubstructure<?, ?> assignLeaf(ContentTreeNode leafNode, int modelIdentifier, String chainIdentifer) {
        // generate leaf pdbIdentifier
        LeafIdentifier leafIdentifier = new LeafIdentifier(this.currentPDB, modelIdentifier, chainIdentifer, Integer.valueOf(leafNode.getIdentifier()));
        // get leaf name for leaf identifer
        String leafName = this.leafNames.get(leafIdentifier);
        // get atoms of this leaf
        Map<String, Atom> atoms = leafNode.getAtomMap();
        // log it
        logger.trace("creating leaf {}:{} for chain {}", leafNode.getIdentifier(), leafName, chainIdentifer);
        // find most suitable implementation
        if (isPlainAminoAcid(leafName)) {
            AminoAcidFamily family = AminoAcidFamily.getAminoAcidTypeByThreeLetterCode(leafName).get();
            return createAminoAcid(leafIdentifier, family, atoms);
        }
        if (isPlainNucleotide(leafName)) {
            NucleotideFamily family = NucleotideFamily.getNucleotideByThreeLetterCode(leafName).get();
            return createNucleotide(leafIdentifier, family, atoms);
        }
        return createLeafWithAdditionalInformation(leafIdentifier, leafName, atoms);
    }

    private boolean isPlainAminoAcid(String leafName) {
        return AminoAcidFamily.getAminoAcidTypeByThreeLetterCode(leafName).isPresent();
    }

    private boolean isPlainNucleotide(String leafName) {
        return NucleotideFamily.getNucleotideByThreeLetterCode(leafName).isPresent();
    }

    private AminoAcid createAminoAcid(LeafIdentifier identifier, AminoAcidFamily family, Map<String, Atom> atoms) {
        return LeafFactory.createAminoAcidFromAtoms(identifier, family, atoms);
    }

    private Nucleotide createNucleotide(LeafIdentifier identifier, NucleotideFamily family, Map<String, Atom> atoms) {
        return LeafFactory.createNucleotideFromAtoms(identifier, family, atoms);
    }

    private LeafSubstructure<?, ?> createLeafWithAdditionalInformation(LeafIdentifier identifier, String leafName, Map<String, Atom> atoms) {
        LeafSkeleton leafSkeleton;
        if (!this.reducer.skeletons.containsKey(leafName)) {
            leafSkeleton = LigandParserService.parseLeafSkeleton(leafName);
            this.reducer.skeletons.put(leafName, leafSkeleton);
        } else {
            leafSkeleton = this.reducer.skeletons.get(leafName);
        }
        return leafSkeleton.toRealLeafSubStructure(identifier, atoms);
    }


}