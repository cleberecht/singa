package de.bioforscher.singa.chemistry.physical.model;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by leberech on 16/12/16.
 */
public class LeafIdentifiers {

    public static List<LeafIdentifier> of(String... identifers) {
        return Arrays.stream(identifers).map(LeafIdentifier::fromString).collect(Collectors.toList());
    }

}