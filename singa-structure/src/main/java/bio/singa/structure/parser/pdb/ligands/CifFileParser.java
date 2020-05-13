package bio.singa.structure.parser.pdb.ligands;

import bio.singa.core.utility.Pair;
import bio.singa.mathematics.vectors.Vector3D;
import bio.singa.structure.elements.Element;
import bio.singa.structure.elements.ElementProvider;
import bio.singa.structure.model.families.AminoAcidFamily;
import bio.singa.structure.model.families.LigandFamily;
import bio.singa.structure.model.families.NucleotideFamily;
import bio.singa.structure.model.identifiers.LeafIdentifier;
import bio.singa.structure.model.interfaces.AminoAcid;
import bio.singa.structure.model.interfaces.LeafSubstructure;
import bio.singa.structure.model.interfaces.Nucleotide;
import bio.singa.structure.model.oak.*;
import bio.singa.structure.parser.pdb.structures.tokens.LeafSkeleton;

import java.util.*;

/**
 * @author cl
 */
public class CifFileParser {

    private static final int DEFAULT_VALUE_SPACING = 49;

    private final List<String> lines;
    private final List<String> atomLines;
    private final List<String> bondLines;
    private final Map<String, OakAtom> atoms;
    private final Map<Pair<String>, BondType> bonds;
    private String name;
    private String type;
    private String oneLetterCode;
    private String threeLetterCode;
    private String parent;
    private String inchi;

    private CifFileParser(List<String> lines) {
        this.lines = lines;
        atoms = new HashMap<>();
        bonds = new HashMap<>();
        bondLines = new ArrayList<>();
        atomLines = new ArrayList<>();
    }


    public static LeafSubstructure<?> parseLeafSubstructure(List<String> lines) {
        CifFileParser parser = new CifFileParser(lines);
        return parser.parseCompleteLeafSubstructure();
    }

    public static LeafSkeleton parseLeafSkeleton(List<String> lines) {
        CifFileParser parser = new CifFileParser(lines);
        return parser.parseLeafSkeleton();
    }

    /**
     * Extracts a value from a one line entry from a cif file. Trimming white spaces and removing double quotes.
     *
     * @param line the line to extract
     * @return The extracted value.
     */
    private static String extractValue(String line) {
        // assumes key-value pair per line, separated by white space characters
        // FIXME errors occurring because CIF files have been apparently been reformatted by RCSB, multi line entries might occur, see e.g. _chem_comp.name in http://files.rcsb.org/ligands/view/5MU.cif
        // FIXME DEFAULT_VALUE_SPACING concept does not work anymore
        String[] split = line.split("\\s+");
        if (split.length < 2) {
            return "";
        }
        return split[1];
    }

    /**
     * Collect all relevant lines that are later required for extracting information.
     *
     * @param skipAtoms True, if no atom lines should be parsed.
     */
    private void collectLines(boolean skipAtoms) {
        boolean bondSection = false;
        boolean atomSection = false;
        boolean descriptorSection = false;

        // extract information
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            // extract information
            extractInformation(line);
            // signifies start of bond section
            if (line.startsWith("_chem_comp_bond.pdbx_ordinal")) {
                bondSection = true;
                continue;
            }
            if (bondSection) {
                // extract bonds connecting the given atoms
                if (line.startsWith("#")) {
                    bondSection = false;
                    continue;
                }
                bondLines.add(line);
            }
            if (!skipAtoms) {
                if (line.startsWith("_chem_comp_atom.pdbx_ordinal")) {
                    atomSection = true;
                    continue;
                }
                if (atomSection) {
                    // extract bonds connecting the given atoms
                    if (line.startsWith("#")) {
                        atomSection = false;
                        continue;
                    }
                    atomLines.add(line);
                }
            }
            // chemical identifier section
            if (line.startsWith("_pdbx_chem_comp_descriptor.descriptor")) {
                descriptorSection = true;
            } else if (descriptorSection) {
                if (line.startsWith("#")) {
                    descriptorSection = false;
                    continue;
                }
                String[] splitLine = line.split("\\s+");
                if (splitLine.length == 1) {
                    continue;
                }
                if (splitLine[1].equals("InChI")) {
                    // multi line InChI detected
                    if (splitLine.length == 4) {
                        List<String> multiLineInchi = lines.subList(i + 1, lines.size());
                        Iterator<String> iterator = multiLineInchi.iterator();
                        StringJoiner assembledInchi = new StringJoiner("");
                        while (iterator.hasNext()) {
                            String inchiLine = iterator.next().trim();
                            if (inchiLine.startsWith("\"")) {
                                // 1 line complete InChI read
                                if (inchiLine.endsWith("\"")) {
                                    inchi = inchiLine.replace("\"", "");
                                    break;
                                } else {
                                    // TODO check whether this occurs at any point in the whole PDB
                                    // start of multiline InChI
                                    assembledInchi.add(inchiLine.replace("\"", ""));
                                }
                            } else if (inchiLine.endsWith("\"")) {
                                inchi = assembledInchi.toString();
                            }
                        }
                    } else {
                        // single line InChI detected
                        inchi = splitLine[4].replace("\"", "");
                    }
                }
            }
        }
    }

    /**
     * Extracts and creates atoms from the extracted lines.
     */
    private void extractAtoms() {
        for (String line : atomLines) {
            String[] splitLine = line.split("\\s+");
            /// 1 = atom name, 3 = element, 9 = x coordinate, 10 = y coordinate, 11 = z coordinates, 17 = identifer
            int identifier = Integer.parseInt(splitLine[17]);
            Element element = ElementProvider.getElementBySymbol(splitLine[3]).orElse(ElementProvider.UNKOWN);
            String atomName = splitLine[1].replace("\"", "");
            Vector3D coordinates = new Vector3D(Double.parseDouble(splitLine[9]), Double.parseDouble(splitLine[10]), Double.parseDouble(splitLine[11]));
            OakAtom atom = new OakAtom(identifier, element, atomName, coordinates);
            atoms.put(atomName, atom);
        }
    }

    /**
     * Extracts the information required to create bonds.
     */
    private void extractBonds() {
        // for each of the collected bond lines
        for (String line : bondLines) {
            String[] splitLine = line.split("\\s+");
            // 1 = first atom, 2 = second atom, 3 = bond type
            bonds.put(new Pair<>(splitLine[1].replace("\"", ""), splitLine[2].replace("\"", "")),
                    BondType.getBondTypeByCifName(splitLine[3]).orElse(BondType.SINGLE_BOND));
        }
    }

    /**
     * Extracts information about the ligand.
     *
     * @param line The line.
     */
    private void extractInformation(String line) {
        // extract compound name
        if (line.startsWith("_chem_comp.name")) {
            name = extractValue(line);
        }
        // extract compound type
        if (line.startsWith("_chem_comp.type")) {
            type = extractValue(line);
        }
        // extract one letter code
        if (line.startsWith("_chem_comp.one_letter_code")) {
            oneLetterCode = extractValue(line);
        }
        // extract three letter code
        if (line.startsWith("_chem_comp.three_letter_code")) {
            threeLetterCode = extractValue(line);
        }
        // extract parent
        if (line.startsWith("_chem_comp.mon_nstd_parent_comp_id")) {
            parent = extractValue(line);
        }
    }

    /**
     * Parses a leaf from scratch using only information provided in the cif file.
     *
     * @return A leaf.
     */
    private OakLeafSubstructure<?> parseCompleteLeafSubstructure() {
        collectLines(false);
        extractAtoms();
        extractBonds();
        return createLeafSubstructure(LeafIdentifier.fromSimpleString("A-1"));
    }

    /**
     * Creates a complete {@link LeafSubstructure} using the information collected until the call of this method.
     *
     * @param leafIdentifier The identifier this leaf should have.
     * @return A complete {@link LeafSubstructure} using the information collected until the call of this method.
     */
    private OakLeafSubstructure<?> createLeafSubstructure(LeafIdentifier leafIdentifier) {
        OakLeafSubstructure<?> leafSubstructure;
        if (isNucleotide()) {
            // check for nucleotides
            Optional<NucleotideFamily> nucleotideFamily = NucleotideFamily.getNucleotideByThreeLetterCode(parent);
            leafSubstructure = nucleotideFamily.map(nucleotideFamily1 -> new OakNucleotide(leafIdentifier, nucleotideFamily1, threeLetterCode))
                    .orElseGet(() -> new OakNucleotide(leafIdentifier, NucleotideFamily.getNucleotideByThreeLetterCode(threeLetterCode).orElseThrow(() ->
                            new IllegalArgumentException("Could not create Nucleotide with three letter code" + threeLetterCode))));

        } else if (isAminoAcid()) {
            // check for amino acids
            Optional<AminoAcidFamily> aminoAcidFamily = AminoAcidFamily.getAminoAcidTypeByThreeLetterCode(parent);
            leafSubstructure = aminoAcidFamily.map(aminoAcidFamily1 -> new OakAminoAcid(leafIdentifier, aminoAcidFamily1, threeLetterCode))
                    .orElseGet(() -> new OakAminoAcid(leafIdentifier, AminoAcidFamily.getAminoAcidTypeByThreeLetterCode(threeLetterCode).orElseThrow(() ->
                            new IllegalArgumentException("Could not create Nucleotide with three letter code" + threeLetterCode))));
        } else {
            // else this is a ligand
            OakLigand OakLigand = new OakLigand(leafIdentifier, new LigandFamily(oneLetterCode, threeLetterCode));
            OakLigand.setName(name);
            leafSubstructure = OakLigand;
        }
        atoms.values().forEach(leafSubstructure::addAtom);
        connectAtoms(leafSubstructure);
        return leafSubstructure;
    }

    /**
     * Creates a leaf skeleton to be used to create complete leafs from.
     *
     * @return A leaf skeleton.
     */
    private LeafSkeleton parseLeafSkeleton() {
        collectLines(true);
        extractBonds();
        return createLeafSkeleton();
    }

    /**
     * Creates a leaf skeleton only containing the information required to build a new leaf.
     *
     * @return A leaf skeleton.
     */
    private LeafSkeleton createLeafSkeleton() {
        LeafSkeleton.AssignedFamily assignedFamily;
        if (isNucleotide()) {
            // check for nucleotides
            if (!parent.equals("?")) {
                assignedFamily = LeafSkeleton.AssignedFamily.MODIFIED_NUCLEOTIDE;
            } else {
                // TODO fix this fallback solution
                assignedFamily = LeafSkeleton.AssignedFamily.LIGAND;
            }
        } else if (isAminoAcid()) {
            // check for amino acids
            assignedFamily = LeafSkeleton.AssignedFamily.MODIFIED_AMINO_ACID;
        } else {
            // else this is a ligand
            assignedFamily = LeafSkeleton.AssignedFamily.LIGAND;
        }
        LeafSkeleton leafSkeleton = new LeafSkeleton(threeLetterCode, parent, assignedFamily, bonds);
        leafSkeleton.setInchi(inchi);
        return leafSkeleton;
    }

    /**
     * Returns whether this molecule can be considered as a {@link Nucleotide}. This checks if the type is either {@code
     * RNA LINKING} or {@code DNA LINKING}.
     *
     * @return True if the entity is a nucleotide.
     */
    private boolean isNucleotide() {
        return type.equalsIgnoreCase("RNA LINKING") || type.equalsIgnoreCase("DNA LINKING");
    }

    /**
     * Returns whether this molecule can be considered as a {@link AminoAcid}. This checks if the type is {@code
     * L-PEPTIDE LINKING} and a valid parent is specified.
     *
     * @return True if entity is amino acid.
     */
    private boolean isAminoAcid() {
        return type.equalsIgnoreCase("L-PEPTIDE LINKING"); // && AminoAcidFamily.getAminoAcidTypeByThreeLetterCode(this.parent).isPresent();
    }

    /**
     * Connects the atoms in the leaf as specified in the bonds map.
     *
     * @param leafWithAtoms The leaf to connect.
     */
    private void connectAtoms(OakLeafSubstructure<?> leafWithAtoms) {
        int bondCounter = 0;
        for (Map.Entry<Pair<String>, BondType> bond : bonds.entrySet()) {
            OakBond oakBond = new OakBond(bondCounter++, bond.getValue());
            leafWithAtoms.addBondBetween(oakBond, atoms.get(bond.getKey().getFirst()),
                    atoms.get(bond.getKey().getSecond()));
        }
    }

}
