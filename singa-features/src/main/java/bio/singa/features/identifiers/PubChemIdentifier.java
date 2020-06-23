package bio.singa.features.identifiers;

import bio.singa.features.identifiers.model.AbstractIdentifier;
import bio.singa.features.model.Evidence;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This identifier is used by the <a href="https://pubchem.ncbi.nlm.nih.gov/">PubChem Database</a> to identify small
 * molecules.
 * <p>
 * The identifier usually only contains numbers. Sometimes the number is preceded by "CID:". To make this identifier
 * more "identifiable" we require the suffix "CID:".
 *
 * @author cl
 */
public class PubChemIdentifier extends AbstractIdentifier {

    /**
     * The pattern to verify the identifier.
     */
    public static final Pattern PATTERN = Pattern.compile("CID:([\\d]+)");

    /**
     * Creates a new identifier.
     *
     * @param identifier The identifier.
     * @throws IllegalArgumentException If the identifier not valid.
     */
    public PubChemIdentifier(String identifier) throws IllegalArgumentException {
        super(identifier, PATTERN);
    }

    public PubChemIdentifier(String identifier, Evidence evidence) throws IllegalArgumentException {
        super(identifier, PATTERN, evidence);
    }

    /**
     * Returns the consecutive number without the "CID:" part.
     *
     * @return The consecutive number without the "CID:" part.
     */
    public int getConsecutiveNumber() {
        Matcher matcherCHEBI = PATTERN.matcher(getContent());
        if (matcherCHEBI.matches()) {
            return Integer.parseInt(matcherCHEBI.group(1));
        } else {
            // should no be possible
            throw new IllegalStateException("This identifier has been created with an unexpected pattern.");
        }
    }



}
