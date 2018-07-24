package bio.singa.simulation.model;

import bio.singa.chemistry.entities.Enzyme;
import bio.singa.chemistry.entities.Protein;
import bio.singa.features.identifiers.UniProtIdentifier;

/**
 * @author cl
 */
public class AdenylCyclaseModelTest {

    public static void main(String[] args) {

        // Paper: Adenylate Cyclase 6 Determines cAMP Formation and Aquaporin-2 Phosphorylation and Trafficking in
        // Inner Medulla

        // adenylate cyclase 6
        Enzyme vasopressinReceptor = new Enzyme.Builder("ADCY6")
                .additionalIdentifier(new UniProtIdentifier("O43306"))
                .build();

        // is activated by g-protein subunit alpha
        Protein gProteinAlpha = new Protein.Builder("G(A)")
                .additionalIdentifier(new UniProtIdentifier("P63092"))
                .build();

    }

}