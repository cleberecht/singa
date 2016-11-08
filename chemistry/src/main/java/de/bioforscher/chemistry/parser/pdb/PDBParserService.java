package de.bioforscher.chemistry.parser.pdb;

import de.bioforscher.chemistry.physical.model.SubStructure;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.util.stream.Collectors;

/**
 * Created by Christoph on 30/10/2016.
 */
public class PDBParserService {

    public static final String PDB_FETCH_URL = "https://files.rcsb.org/download/%s.pdb";

    public static SubStructure parseProteinById(String pdbId) throws IOException {
        return parsePDBFile(new URL(String.format(PDB_FETCH_URL, pdbId)).openStream());
    }

    public static SubStructure parsePDBFile(String filepath) throws IOException {
        return parsePDBFile(new File(filepath));
    }

    public static SubStructure parsePDBFile(File pdbFile) throws IOException {
        return parsePDBFile(Files.newInputStream(pdbFile.toPath()));
    }

    public  static SubStructure parsePDBFile(InputStream inputStream) throws IOException {
        return StructureAssembler.assembleStructure(new BufferedReader(new InputStreamReader(inputStream))
                .lines()
                .collect(Collectors.toList()));
    }

}
