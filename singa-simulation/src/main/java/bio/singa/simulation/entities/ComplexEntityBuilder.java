package bio.singa.simulation.entities;

import bio.singa.core.utility.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author cl
 */
public class ComplexEntityBuilder {

    public static CombineStep create() {
        return new ComplexEntityCombiner();
    }

    public interface CombineStep {
        CombineStep combine(ChemicalEntity first, ChemicalEntity second);
        CombineStep combine(ChemicalEntity first, ChemicalEntity second, BindingSite bindingSite);
        ComplexEntity build();
    }

    public static class ComplexEntityCombiner implements CombineStep {

        ComplexEntity complex;
        Map<BindingSite, Pair<ChemicalEntity>> bindingSiteMap;

        public ComplexEntityCombiner() {
            complex = new ComplexEntity();
            bindingSiteMap = new HashMap<>();
        }

        @Override
        public CombineStep combine(ChemicalEntity first, ChemicalEntity second) {
            BindingSite bindingSite = BindingSite.forPair(first, second);
            return combine(first, second, bindingSite);
        }

        @Override
        public CombineStep combine(ChemicalEntity first, ChemicalEntity second, BindingSite bindingSite) {
            bindingSiteMap.put(bindingSite, new Pair<>(first, second));
            return this;
        }

        @Override
        public ComplexEntity build() {
            // collect binding sites
            Map<ChemicalEntity, Set<BindingSite>> bindingSiteMapping = new HashMap<>();
            for (Map.Entry<BindingSite, Pair<ChemicalEntity>> entry : bindingSiteMap.entrySet()) {
                attachBindingSites(bindingSiteMapping, entry);
            }
            // connect
            bindingSiteMapping.forEach(complex::snapTo);
            complex.update();
            return complex;
        }
    }

    public static void attachBindingSites(Map<ChemicalEntity, Set<BindingSite>> bindingSiteMapping, Map.Entry<BindingSite, Pair<ChemicalEntity>> entry) {
        ChemicalEntity primaryEntity = entry.getValue().getFirst();
        ChemicalEntity secondaryEntity = entry.getValue().getSecond();
        BindingSite bindingSite = entry.getKey();

        if (!bindingSiteMapping.containsKey(primaryEntity)) {
            bindingSiteMapping.put(primaryEntity, new HashSet<>());
        }
        bindingSiteMapping.get(primaryEntity).add(bindingSite);

        if (!bindingSiteMapping.containsKey(secondaryEntity)) {
            bindingSiteMapping.put(secondaryEntity, new HashSet<>());
        }
        bindingSiteMapping.get(secondaryEntity).add(bindingSite);
    }
}
