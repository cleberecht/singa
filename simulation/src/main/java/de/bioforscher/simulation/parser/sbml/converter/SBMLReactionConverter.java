package de.bioforscher.simulation.parser.sbml.converter;

import de.bioforscher.chemistry.descriptive.ChemicalEntity;
import de.bioforscher.simulation.model.parameters.SimulationParameter;
import de.bioforscher.simulation.modules.reactions.implementations.DynamicReaction;
import de.bioforscher.simulation.modules.reactions.implementations.kineticLaws.implementations.DynamicKineticLaw;
import de.bioforscher.simulation.modules.reactions.model.CatalyticReactant;
import de.bioforscher.simulation.modules.reactions.model.ReactantRole;
import de.bioforscher.simulation.modules.reactions.model.StoichiometricReactant;
import de.bioforscher.simulation.parser.sbml.FunctionReference;
import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.ModifierSpeciesReference;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SpeciesReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.measure.Unit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Converts JSBML Reactions to SiNGA Reactions
 * @author cl
 */
public class SBMLReactionConverter {

    private static final Logger logger = LoggerFactory.getLogger(SBMLReactionConverter.class);

    private Map<String, Unit<?>> units;
    private Map<String, ChemicalEntity> entities;
    private Map<String, FunctionReference> functions;
    private Map<String, SimulationParameter<?>> globalParameters;

    private SBMLKineticLawConverter kineticLawConverter;
    private DynamicReaction currentReaction;

    public SBMLReactionConverter(Map<String, Unit<?>> units, Map<String, ChemicalEntity> entities, Map<String, FunctionReference> functions, Map<String, SimulationParameter<?>> globalParameters) {
        this.units = units;
        this.entities = entities;
        this.functions = functions;
        this.globalParameters = globalParameters;
        this.kineticLawConverter = new SBMLKineticLawConverter(this.units, this.functions, this.globalParameters);
    }

    public List<DynamicReaction> convertReactions(ListOf<Reaction> sbmlReactions) {
        List<DynamicReaction> reactions = new ArrayList<>();
        for (Reaction reaction: sbmlReactions) {
            reactions.add(convertReaction(reaction));
        }
        return reactions;
    }

    public DynamicReaction convertReaction(Reaction reaction) {
        logger.debug("Parsing Reaction {} ...", reaction.getName());
        DynamicKineticLaw kineticLaw = this.kineticLawConverter.convertKineticLaw(reaction.getKineticLaw());
        this.currentReaction = new DynamicReaction(kineticLaw);
        assignSubstrates(reaction.getListOfReactants());
        assignProducts(reaction.getListOfProducts());
        assignModifiers(reaction.getListOfModifiers());
        logger.debug("Parsed Reaction:{}", this.currentReaction.getDisplayString());
        return this.currentReaction;
    }

    private void assignSubstrates(ListOf<SpeciesReference> substrates) {
        for (SpeciesReference reference : substrates) {
            logger.debug("Assigning Chemical Entity {} as substrate.", reference.getSpecies());
            String identifier = reference.getSpecies();
            this.currentReaction.getKineticLaw().referenceChemicalEntityToParameter(identifier, this.entities.get(identifier));
            this.currentReaction.getStoichiometricReactants().add(new StoichiometricReactant(this.entities.get(identifier), ReactantRole.DECREASING, reference.getStoichiometry()));
        }
    }

    private void assignProducts(ListOf<SpeciesReference> products) {
        for (SpeciesReference reference : products) {
            logger.debug("Assigning Chemical Entity {} as product.", reference.getSpecies());
            String identifier = reference.getSpecies();
            this.currentReaction.getKineticLaw().referenceChemicalEntityToParameter(identifier, this.entities.get(identifier));
            this.currentReaction.getStoichiometricReactants().add(new StoichiometricReactant(this.entities.get(identifier), ReactantRole.INCREASING, reference.getStoichiometry()));
        }
    }

    private void assignModifiers(ListOf<ModifierSpeciesReference> modifiers) {
        for (ModifierSpeciesReference reference : modifiers) {
            logger.debug("Assigning Chemical Entity {} as catalytic modifier.", reference.getSpecies());
            String identifier = reference.getSpecies();
            this.currentReaction.getKineticLaw().referenceChemicalEntityToParameter(identifier, this.entities.get(identifier));
            this.currentReaction.getCatalyticReactants().add(new CatalyticReactant(this.entities.get(identifier), ReactantRole.INCREASING));
        }
    }


}
