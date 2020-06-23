package bio.singa.chemistry.features.databases.unichem;

import bio.singa.core.parser.AbstractHTMLParser;
import bio.singa.features.identifiers.ChEBIIdentifier;
import bio.singa.features.identifiers.InChIKey;
import bio.singa.features.identifiers.PDBLigandIdentifier;
import bio.singa.features.identifiers.PubChemIdentifier;
import bio.singa.features.identifiers.model.Identifier;
import bio.singa.features.identifiers.model.IdentifierPatternRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author cl
 */
public class UniChemParser extends AbstractHTMLParser<List<Identifier>> {

    private static final Logger logger = LoggerFactory.getLogger(UniChemParser.class);
    private static final String UNICHEM_FETCH_URL = "https://www.ebi.ac.uk/unichem/rest/verbose_inchikey/%s";

    public UniChemParser(InChIKey inChIKey) {
        setResource(String.format(UNICHEM_FETCH_URL, inChIKey.getContent()));
    }

    public static List<Identifier> parse(InChIKey inChIKey) {
        UniChemParser parser = new UniChemParser(inChIKey);
        return parser.parse();
    }

    @Override
    public List<Identifier> parse() {
        fetchResource();
        try {
            try (InputStreamReader inputStreamReader = new InputStreamReader(getFetchResult())) {
                try (BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
                    return processLines(bufferedReader.lines().collect(Collectors.toList()));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not open input stream for uni chem resource.", e);
        }
    }

    private List<Identifier> processLines(List<String> lines) {
        boolean isChebi = false;
        boolean isPubChem = false;
        boolean isPdbLigand = false;
        List<Identifier> identifiers = new ArrayList<>();
        for (String line : lines) {
            if (line.equals("  name: chebi")) {
                isChebi = true;
            } else if (line.equals("  name: pubchem")) {
                isPubChem = true;
            } else if (line.equals("  name: pdb")) {
                isPdbLigand = true;
            } else if (isChebi && line.startsWith("    - ")) {
                isChebi = false;
                String chebiNumber = line.substring(5).trim();
                identifiers.add(new ChEBIIdentifier("CHEBI:" + chebiNumber));
            } else if (isPubChem && line.startsWith("    - ")) {
                isPubChem = false;
                String pubChemNumber = line.substring(5).trim();
                identifiers.add(new PubChemIdentifier("CID:" + pubChemNumber));
            }  else if (isPdbLigand && line.startsWith("    - ")) {
                isPdbLigand = false;
                String pdbLigandIdentifier = line.substring(5).trim();
                identifiers.add(new PDBLigandIdentifier(pdbLigandIdentifier));
            }
        }
        return identifiers;
    }


    public static PubChemIdentifier fetchPubChemIdentifier(InChIKey inChIKey) {
        return IdentifierPatternRegistry.find(PubChemIdentifier.class, UniChemParser.parse(inChIKey)).orElse(null);
    }
    public static PDBLigandIdentifier fetchPdbLigandIdentifier(InChIKey inChIKey) {
        return IdentifierPatternRegistry.find(PDBLigandIdentifier.class, UniChemParser.parse(inChIKey)).orElse(null);
    }


}
