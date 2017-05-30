package de.bioforscher.singa.chemistry.descriptive;

import de.bioforscher.singa.chemistry.descriptive.annotations.Annotatable;
import de.bioforscher.singa.chemistry.descriptive.annotations.Annotation;
import de.bioforscher.singa.chemistry.descriptive.annotations.AnnotationType;
import de.bioforscher.singa.chemistry.descriptive.features.diffusivity.Diffusivity;
import de.bioforscher.singa.chemistry.descriptive.features.molarmass.MolarMass;
import de.bioforscher.singa.chemistry.physical.model.Structure;
import de.bioforscher.singa.core.identifier.model.Identifiable;
import de.bioforscher.singa.core.identifier.model.Identifier;
import de.bioforscher.singa.core.utility.Nameable;
import de.bioforscher.singa.units.features.model.Feature;
import de.bioforscher.singa.units.features.model.FeatureContainer;
import de.bioforscher.singa.units.features.model.FeatureOrigin;
import de.bioforscher.singa.units.features.model.Featureable;
import tec.units.ri.quantity.Quantities;

import javax.measure.Quantity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static de.bioforscher.singa.chemistry.descriptive.features.molarmass.MolarMass.GRAM_PER_MOLE;

/**
 * Chemical Entity is an abstract class that provides the common features of all chemical substances on a descriptive
 * level. It does not contain the exact chemical structure, to handle chemical structures have a look at
 * {@link Structure Structure}. Each chemical entity should be identifiable by an
 * {@link Identifier}. Chemical entities can be annotated, posses a {@link MolarMass} and a name.
 *
 * @param <IdentifierType> The Type of the {@link Identifier}, that identifies this entity.
 * @author cl
 * @see <a href="https://de.wikipedia.org/wiki/Simplified_Molecular_Input_Line_Entry_Specification">Wikipedia:
 * SMILES</a>
 */
public abstract class ChemicalEntity<IdentifierType extends Identifier> implements Identifiable<IdentifierType>,
        Nameable, Annotatable, Featureable {

    /**
     * The distinct {@link Identifier} by which this entity is identified.
     */
    private final IdentifierType identifier;

    /**
     * The name by which this entity is referenced.
     */
    private String name = "Unnamed chemical entity";

    /**
     * The molar mass of this entity.
     */
    private Quantity<MolarMass> molarMass;

    /**
     * All annotations of this entity.
     */
    private List<Annotation> annotations;

    private FeatureContainer container;

    private final Set<Class<? extends Feature>> availableFeatures;

    /**
     * Creates a new Chemical Entity with the given pdbIdentifier.
     *
     * @param identifier The pdbIdentifier.
     */
    protected ChemicalEntity(IdentifierType identifier) {
        this.identifier = identifier;
        this.annotations = new ArrayList<>();
        this.container = new FeatureContainer();
        this.availableFeatures = new HashSet<>();
        this.availableFeatures.add(Diffusivity.class);
        this.availableFeatures.add(MolarMass.class);
    }

    @Override
    public IdentifierType getIdentifier() {
        return this.identifier;
    }

    @Override
    public String getName() {
        return this.name;
    }

    /**
     * Sets the name.
     *
     * @param name The name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the {@link MolarMass}.
     *
     * @return The {@link MolarMass}.
     */
    public Quantity<MolarMass> getMolarMass() {
        return this.molarMass;
    }

    /**
     * Sets The {@link MolarMass} in {@link MolarMass#GRAM_PER_MOLE g/mol}.
     *
     * @param molarMass The {@link MolarMass} in {@link MolarMass#GRAM_PER_MOLE g/mol}.
     */
    public void setMolarMass(double molarMass) {
        this.molarMass = Quantities.getQuantity(molarMass, GRAM_PER_MOLE);
    }

    /**
     * Sets the {@link MolarMass}.
     *
     * @param molarMass The {@link MolarMass}.
     */
    public void setMolarMass(Quantity<MolarMass> molarMass) {
        this.molarMass = molarMass;
    }

    @Override
    public List<Annotation> getAnnotations() {
        return this.annotations;
    }

    /**
     * Adds an additional name as an annotation to this chemical entity.
     *
     * @param additionalName An alternative name.
     */
    public void addAdditionalName(String additionalName) {
        addAnnotation(new Annotation<>(AnnotationType.ADDITIONAL_NAME, additionalName));
    }

    /**
     * Gets all additional names for the Annotations as a List of Strings.
     *
     * @return All alternative names.
     */
    public List<String> getAdditionalNames() {
        return getContentOfAnnotations(String.class, AnnotationType.ADDITIONAL_NAME);
    }

    public void addAdditionalIdentifer(Identifier identifier) {
        addAnnotation(new Annotation<>(AnnotationType.ADDITIONAL_IDENTIFIER, identifier));
    }

    public List<Identifier> getAdditionalIdentifiers() {
        return getContentOfAnnotations(Identifier.class, AnnotationType.ADDITIONAL_IDENTIFIER);
    }

    @Override
    public <FeatureType extends Feature> FeatureType getFeature(Class<FeatureType> featureTypeClass) {
        return this.container.getFeature(featureTypeClass);
    }

    @Override
    public <FeatureType extends Feature> void setFeature(Class<FeatureType> featureTypeClass) {
        this.container.setFeature(featureTypeClass, this);
    }

    @Override
    public <FeatureType extends Feature> void setFeature(FeatureType feature) {
        this.container.setFeature(feature);
    }

    @Override
    public <FeatureType extends Feature> boolean hasFeature(Class<FeatureType> featureTypeClass) {
        return this.container.hasFeature(featureTypeClass);
    }

    @Override
    public Set<Class<? extends Feature>> getAvailableFeatures() {
        return availableFeatures;
    }

    public List<Identifier> getAllIdentifiers() {
        List<Identifier> identifiers = getAdditionalIdentifiers();
        identifiers.add(this.identifier);
        return identifiers;
    }

    @Override
    public String toString() {
        return "ChemicalEntity{" +
                "identifier=" + this.identifier +
                ", name='" + this.name + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChemicalEntity<?> that = (ChemicalEntity<?>) o;
        return this.identifier != null ? this.identifier.equals(that.identifier) : that.identifier == null;
    }

    @Override
    public int hashCode() {
        return this.identifier != null ? this.identifier.hashCode() : 0;
    }

    public static abstract class Builder<TopLevelType extends ChemicalEntity<?>, BuilderType extends Builder, IdentifierType extends Identifier> {

        TopLevelType topLevelObject;
        BuilderType builderObject;

        public Builder(IdentifierType identifier) {
            this.topLevelObject = createObject(identifier);
            this.builderObject = getBuilder();
        }

        protected abstract TopLevelType createObject(IdentifierType primaryIdentifer);

        protected abstract BuilderType getBuilder();

        public BuilderType name(String name) {
            this.topLevelObject.setName(name);
            return this.builderObject;
        }

        public BuilderType molarMass(Quantity<MolarMass> molarMass) {
            this.topLevelObject.setMolarMass(molarMass);
            this.topLevelObject.setFeature(new MolarMass(molarMass, FeatureOrigin.MANUALLY_ANNOTATED));
            return this.builderObject;
        }

        public BuilderType molarMass(double molarMass) {
            final Quantity<MolarMass> quantity = Quantities.getQuantity(molarMass, GRAM_PER_MOLE);
            this.topLevelObject.setMolarMass(quantity);
            this.topLevelObject.setFeature(new MolarMass(quantity, FeatureOrigin.MANUALLY_ANNOTATED));
            return this.builderObject;
        }

        public BuilderType additionalIdentifier(Identifier identifier) {
            this.topLevelObject.addAdditionalIdentifer(identifier);
            return this.builderObject;
        }

        public BuilderType annotation(Annotation annotation) {
            this.topLevelObject.addAnnotation(annotation);
            return this.builderObject;
        }

        public TopLevelType build() {
            return this.topLevelObject;
        }
    }

}