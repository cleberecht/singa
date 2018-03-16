package de.bioforscher.singa.chemistry.descriptive.features.databases.ena;

import de.bioforscher.singa.core.biology.NucleotideSequence;
import de.bioforscher.singa.core.identifier.ENAAccessionNumber;
import org.junit.Test;

import static de.bioforscher.singa.core.identifier.ENAAccessionNumber.ExpressionType.GENOMIC_DNA;
import static org.junit.Assert.assertEquals;

/**
 * @author cl
 */
public class ENAParserServiceTest {

    @Test
    public void shouldParseNucleotideSequence() {
        final NucleotideSequence sequence = ENAParserService.parse(new ENAAccessionNumber("CAA37856.1", GENOMIC_DNA));
        assertEquals("atgcgtacagaatattgtggacagctccgtttgtcccacgtggggcagcaggtgactctgtgtggttgggtcaaccgtcgtcgtgatcttggtagcctgatcttcatcgatatgcgcgaccgcgaaggtatcgtgcaggtatttttcgatccggatcgtgcggacgcgttaaagctggcctctgaactgcgtaatgagttctgcattcaggtcacgggcaccgtacgtgcgcgtgacgaaaaaaatattaaccgcgatatggcgaccggcgaaatcgaagtgctggcgtcctcgctgactatcatcaaccgcgcagatgttctgccgcttgactctaaccacgtcaacaccgaagaagcgcgtctgaaataccgctacctcgacctgcgtcgtccggaaatggctcagcgcctgaaaacccgcgctaaaatcaccagcctggtgcgccgttttatggatgaccacggcttcctcgacatcgaaactccgatgctgaccaaagccacgccggaaggcgcgcgtgactacctggtgccttctcgtgtgcacaaaggtaaattctacgcactgccgcaatccccgcagttgttcaaacagctgctgatgatgtccggttttgaccgttactatcagatcgttaaatgcttccgtgacgaagacctgcgtgctgaccgtcagcctgaatttactcagatcgatgtggaaacttctttcatgaccgcgccgcaagtgcgtgaagtgatggaagcgctggtgcgtcatctgtggctggaagtgaagggtgtggatctgggcgatttcccggtaatgacctttgcggaagcagaacgccgttatggttctgataaaccggatctgcgtaacccgatggaactgactgacgttgctgatctgctgaaatctgttgagtttgctgtatttgcaggtccggcgaacgatccgaaaggtcgcgtagcggctctgcgcgttccgggcggcgcatcgctgacccgtaagcagatcgacgaatacggtaacttcgttaaaatctacggcgcgaaaggtctggcttacatcaaagttaacgaacgcgcgaaaggtctggaaggtatcaacagcccggtagcgaagttccttaatgcagaaatcatcgaagacatcctggatcgtactgccgcgcaagatggcgatatgattttcttcggtgccgacaacaagaaaattgttgccgacgcgatgggtgcactgcgcctgaaagtgggtaaagaccttggtctgaccgacgaaagcaaatgggcaccgctgtgggttatcgacttcccgatgtttgaagacgacggtgaaggcggcctgacggcaatgcaccatccgttcacctcaccgaaagatatgacggctgcagaactgaaagctgcaccggaaaatgcggtggcgaacgcttacgatatggtcatcaatggttacgaagtgggcggtggttcagtacgtatccataatggtgatatgcagcagacggtgtttggtattctgggtatcaacgaagaggaacagcgcgagaaattcggcttcctgctcgacgctctgaaatacggtactccgccgcacgcaggtctggcattcggtcttgaccgtctgaccatgctgctgaccggcaccgacaatatccgtgacgttatcgccttcccgaaaaccacggcggcagcgtgtctgatgactgaagcaccgagctttgctaacccgactgcactggctgagctgagcattcaggttgtgaagaaggctgagaataactga",sequence.getSequence());
        assertEquals("MRTEYCGQLRLSHVGQQVTLCGWVNRRRDLGSLIFIDMRDREGIVQVFFDPDRADALKLASELRNEFCIQVTGTVRARDEKNINRDMATGEIEVLASSLTIINRADVLPLDSNHVNTEEARLKYRYLDLRRPEMAQRLKTRAKITSLVRRFMDDHGFLDIETPMLTKATPEGARDYLVPSRVHKGKFYALPQSPQLFKQLLMMSGFDRYYQIVKCFRDEDLRADRQPEFTQIDVETSFMTAPQVREVMEALVRHLWLEVKGVDLGDFPVMTFAEAERRYGSDKPDLRNPMELTDVADLLKSVEFAVFAGPANDPKGRVAALRVPGGASLTRKQIDEYGNFVKIYGAKGLAYIKVNERAKGLEGINSPVAKFLNAEIIEDILDRTAAQDGDMIFFGADNKKIVADAMGALRLKVGKDLGLTDESKWAPLWVIDFPMFEDDGEGGLTAMHHPFTSPKDMTAAELKAAPENAVANAYDMVINGYEVGGGSVRIHNGDMQQTVFGILGINEEEQREKFGFLLDALKYGTPPHAGLAFGLDRLTMLLTGTDNIRDVIAFPKTTAAACLMTEAPSFANPTALAELSIQVVKKAENN",sequence.getTranslationSequence());
        assertEquals(11,sequence.getTranslationTable(),0);
    }


}