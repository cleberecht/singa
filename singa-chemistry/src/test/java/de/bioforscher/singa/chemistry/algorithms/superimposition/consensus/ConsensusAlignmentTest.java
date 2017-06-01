package de.bioforscher.singa.chemistry.algorithms.superimposition.consensus;

import de.bioforscher.singa.chemistry.parser.pdb.structures.StructureParser;
import de.bioforscher.singa.chemistry.parser.pdb.structures.StructureParserOptions;
import de.bioforscher.singa.chemistry.physical.atoms.representations.RepresentationSchemeType;
import de.bioforscher.singa.chemistry.physical.branches.StructuralMotif;
import de.bioforscher.singa.chemistry.physical.families.AminoAcidFamily;
import de.bioforscher.singa.chemistry.physical.leaves.AminoAcid;
import de.bioforscher.singa.chemistry.physical.leaves.LeafSubstructure;
import de.bioforscher.singa.chemistry.physical.model.StructuralEntityFilter;
import de.bioforscher.singa.chemistry.physical.model.Structure;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

/**
 * @author fk
 */
public class ConsensusAlignmentTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    private List<StructuralMotif> input;

    @Before
    public void setUp() throws Exception {
        StructureParserOptions structureParserOptions = new StructureParserOptions();
        structureParserOptions.inferIdentifierFromFileName(true);
        this.input = Files.list(Paths.get("src/test/resources/consensus_alignment"))
                .map(path -> StructureParser.local()
                        .fileLocation(path.toString())
                        .everything()
                        .setOptions(structureParserOptions)
                        .parse())
                .map(Structure::getAllLeaves)
                .map(leaves -> StructuralMotif.fromLeaves(0, leaves))
                .collect(Collectors.toList());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailWithInputOfDifferentSize() {
        this.input.get(0).addSubstructure(new AminoAcid(0, AminoAcidFamily.ALANINE));
        new ConsensusAlignment(this.input);
    }

    @Test
    public void shouldCreateConsensusAlignment() throws IOException {
        ConsensusAlignment consensusAlignment = new ConsensusAlignment(this.input, 0.6);
        List<LeafSubstructure<?, ?>> consensusMotif = consensusAlignment.getTopConsensusTree().getRoot().getData().getStructuralMotif().getLeafSubstructures();
        consensusAlignment.writeClusters(this.folder.getRoot().toPath());
        assertEquals(this.input.size(), consensusAlignment.getTopConsensusTree().getLeafNodes().size());
    }

    @Test
    public void shouldCreateConsensusAlignmentWithRepresentationScheme() throws IOException {
        ConsensusAlignment consensusAlignment = new ConsensusAlignment(this.input, 0.2, true, StructuralEntityFilter.AtomFilter.isArbitrary(), RepresentationSchemeType.CB, true);
        List<LeafSubstructure<?, ?>> consensusMotif = consensusAlignment.getTopConsensusTree().getRoot().getData().getStructuralMotif().getLeafSubstructures();
        consensusAlignment.writeClusters(this.folder.getRoot().toPath());
        assertEquals(this.input.size(), consensusAlignment.getTopConsensusTree().getLeafNodes().size());
    }
}