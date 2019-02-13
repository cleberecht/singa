package bio.singa.chemistry.entities;

import bio.singa.features.identifiers.SimpleStringIdentifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author cl
 */
class SpeciesTest {

    @Test
    void shouldCreateSpeciesWithStringIdentifier() {
        SmallMolecule methanol = SmallMolecule.create("CHEBI:123").build();
        assertEquals(methanol.getIdentifier().getContent(), "CHEBI:123");
    }

    @Test
    void shouldCreateSpeciesWithIdentifier() {
        SmallMolecule methanol = SmallMolecule.create(new SimpleStringIdentifier("CHEBI:123")).build();
        assertEquals(methanol.getIdentifier().getContent(), "CHEBI:123");
    }

    @Test
    void shouldBeIdenticalEnzymes() {
        Enzyme gtpAse1 = new Enzyme.Builder("A2BC19").build();
        Enzyme gtpAse2 = new Enzyme.Builder("A2BC19").build();

        assertEquals(gtpAse1, gtpAse2);
    }

}