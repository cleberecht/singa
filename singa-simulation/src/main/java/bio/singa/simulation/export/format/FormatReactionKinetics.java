package bio.singa.simulation.export.format;

import bio.singa.simulation.model.modules.concentration.imlementations.reactions.Reaction;
import bio.singa.simulation.model.modules.concentration.imlementations.reactions.behaviors.reactants.Reactant;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author cl
 */
public class FormatReactionKinetics {

    private static final String michaelisMentenTemplate = "$\\frac{k_{\\text{cat}} \\cdot [\\text{%s}] \\cdot [\\text{%s}]}{K_m \\cdot [\\text{%s}]}$";

    private static String formatReactants(Stream<Reactant> reactants) {
        return reactants.map(FormatReactionKinetics::formatReactant)
                .collect(Collectors.joining(" \\cdot "));
    }

    private static String formatReactant(Reactant reactant) {
        return "[\\text{" + reactant.getEntity().getIdentifier().getContent() + "}]";
    }

    private static String formatSectionReactants(Stream<Reactant> reactants) {
        return reactants.map(FormatReactionKinetics::formatSectionReactant)
                .collect(Collectors.joining(" \\cdot "));
    }

    private static String formatSectionReactant(Reactant reactant) {
        String section = null;
        switch (reactant.getPreferredTopology()) {
            case INNER:
                section = "i";
                break;
            case OUTER:
                section = "o";
                break;
            case MEMBRANE:
                section = "m";
                break;
        }
        return "\\text{[" + reactant.getEntity().getIdentifier().getContent() + "]}_" + section;
    }

    public static String formatTex(Reaction reaction) {

        // throw new IllegalArgumentException("The reaction class " + reaction.getClass() + " lacks an implemented latex format.");
        return "";
    }

//    public static String formatTex(MichaelisMentenReaction reaction) {
//        String substrate = reaction.getSubstrateEntities().iterator().next().getIdentifier().getContent();
//        String enzyme = reaction.getEnzyme().getIdentifier().getContent();
//        return String.format(michaelisMentenTemplate, enzyme, substrate, substrate);
//    }

//    public static String formatTex(NthOrderReaction reaction) {
//        String substrates = formatReactants(reaction.getStoichiometricReactants().stream().filter(Reactant::isSubstrate));
//        return "$k \\cdot " + substrates + "$";
//    }

//    public static String formatTex(ReversibleReaction reaction) {
//        String substrates = formatReactants(reaction.getStoichiometricReactants().stream().filter(Reactant::isSubstrate));
//        String products = formatReactants(reaction.getStoichiometricReactants().stream().filter(Reactant::isProduct));
//        return "$k_{1} \\cdot " + substrates + " - k_{-1} \\cdot " + products + "$";
//    }

//    public static String formatTex(SectionDependentReaction reaction) {
//        String substrates = formatSectionReactants(reaction.getSubstrates().stream());
//        String products = formatSectionReactants(reaction.getProducts().stream());
//        return "$k_{1} \\cdot " + substrates + " - k_{-1} \\cdot " + products + "$";
//    }

//    public static String formatTex(DynamicReaction reaction) {
//        return "not supported";
//    }

}
