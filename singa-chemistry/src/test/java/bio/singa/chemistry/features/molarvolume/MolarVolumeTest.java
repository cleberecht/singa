package bio.singa.chemistry.features.molarvolume;

import bio.singa.chemistry.features.databases.chebi.ChEBIParserService;
import bio.singa.chemistry.simple.SmallMolecule;
import bio.singa.features.quantities.MolarVolume;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author cl
 */
class MolarVolumeTest {

    @Test
    void shouldUseChEBIToCalculateVolume() {
        // values from http://pubs.acs.org/doi/pdf/10.1021/ja00354a007

        SmallMolecule ammonia = ChEBIParserService.parse("CHEBI:16134");
        // Species propane = ChEBIParserService.parse("CHEBI:32879");
        // Species benzene = ChEBIParserService.parse("CHEBI:16716");
        // Species biphenyl = ChEBIParserService.parse("CHEBI:17097");

        final MolarVolume ammoniaFeature = ammonia.getFeature(MolarVolume.class);
        assertEquals(21.91, ammoniaFeature.getValue().doubleValue(), 2.0);

        // TODO no actual 3d structures are available from chebi
        // maybe try to go to pdb and fetch ligand information there
        // alternatively parse mole file
        // propane.setFeature(MolarVolume.class);
        // final MolarVolume propaneFeature = propane.getFeature(MolarVolume.class);
        // assertEquals(61.39, propaneFeature.getValue().doubleValue(), 5.0);
        //
        // benzene.setFeature(MolarVolume.class);
        // final MolarVolume benzeneFeature = benzene.getFeature(MolarVolume.class);
        // assertEquals(85.39, benzeneFeature.getValue().doubleValue(), 5.0);
        //
        // biphenyl.setFeature(MolarVolume.class);
        // final MolarVolume biphenylFeature = biphenyl.getFeature(MolarVolume.class);
        // assertEquals(157.1, biphenylFeature.getValue().doubleValue(), 5.0);

    }

}