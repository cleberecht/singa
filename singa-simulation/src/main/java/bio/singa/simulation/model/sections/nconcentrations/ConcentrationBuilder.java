package bio.singa.simulation.model.sections.nconcentrations;

import bio.singa.chemistry.entities.ChemicalEntity;
import bio.singa.chemistry.entities.simple.SmallMolecule;
import bio.singa.features.model.Evidence;
import bio.singa.features.quantities.MolarConcentration;
import bio.singa.features.units.UnitRegistry;
import bio.singa.mathematics.geometry.faces.Rectangle;
import bio.singa.mathematics.geometry.model.Polygon;
import bio.singa.mathematics.vectors.Vector2D;
import bio.singa.simulation.model.sections.CellRegion;
import bio.singa.simulation.model.sections.CellSubsection;
import bio.singa.simulation.model.sections.CellTopology;
import tech.units.indriya.ComparableQuantity;
import tech.units.indriya.quantity.Quantities;

import javax.measure.Quantity;
import javax.measure.Unit;
import javax.measure.quantity.Time;
import java.util.List;

import static bio.singa.features.units.UnitProvider.*;
import static bio.singa.features.units.UnitRegistry.humanReadable;
import static bio.singa.simulation.model.sections.CellSubsections.CYTOPLASM;
import static bio.singa.simulation.model.sections.CellSubsections.VESICLE_LUMEN;
import static tech.units.indriya.unit.MetricPrefix.MILLI;
import static tech.units.indriya.unit.Units.MINUTE;
import static tech.units.indriya.unit.Units.SECOND;

/**
 * @author cl
 */
public class ConcentrationBuilder {

    public static EntityStep create() {
        return new ConcentrationBuilderImpl();
    }

    public interface EntityStep {

        PoolStep entity(ChemicalEntity entity);

    }

    public interface PoolStep {

        ConcentrationStep subsection(CellSubsection subsection);

        ConcentrationStep topology(CellTopology topology);

    }

    public interface ConcentrationStep {

        AdditionalConditionsStep concentration(Quantity<MolarConcentration> concentration);

        AdditionalConditionsStep molecules(double numberOfMolecules);

        ConcentrationUnitStep concentrationValue(double value);

    }

    public interface AdditionalConditionsStep extends BuildStep {

        AdditionalConditionsStep updatableIdentifiers(List<String> identifiers);

        AdditionalConditionsStep updatableIdentifiers(String... identifiers);

        AdditionalConditionsStep updatableIdentifier(String identifier);

        AdditionalConditionsStep regions(List<CellRegion> regions);

        AdditionalConditionsStep regions(CellRegion... regions);

        AdditionalConditionsStep region(CellRegion region);

        AdditionalConditionsStep inArea(Polygon polygon);

        AdditionalConditionsStep inRegionArea(CellRegion region);

        AdditionalConditionsStep atTime(ComparableQuantity<Time> time);

        AdditionalConditionsStep fixed();

        AdditionalConditionsStep onlyNodes();

        AdditionalConditionsStep onlyVesicles();

        TimedUnitStep atTimeValue(double time);

    }

    public interface ConcentrationUnitStep {

        AdditionalConditionsStep unit(Unit<MolarConcentration> concentrationUnit);

        default AdditionalConditionsStep milliMolar() {
            return unit(MILLI_MOLE_PER_LITRE);
        }

        default AdditionalConditionsStep microMolar() {
            return unit(MICRO_MOLE_PER_LITRE);
        }

        default AdditionalConditionsStep nanoMolar() {
            return unit(NANO_MOLE_PER_LITRE);
        }

    }

    public interface TimedUnitStep {

        AdditionalConditionsStep timeUnit(Unit<Time> timeUnit);

        default AdditionalConditionsStep milliSeconds() {
            return timeUnit(MILLI(SECOND));
        }

        default AdditionalConditionsStep seconds() {
            return timeUnit(SECOND);
        }

        default AdditionalConditionsStep minutes() {
            return timeUnit(MINUTE);
        }

    }

    public interface BuildStep {

        BuildStep evidence(Evidence evidence);

        InitialConcentration build();

    }

    public static class ConcentrationBuilderImpl implements EntityStep, PoolStep, ConcentrationStep, ConcentrationUnitStep, AdditionalConditionsStep, TimedUnitStep {

        private InitialConcentration initialConcentration;
        private double concentrationValue;
        private double timeValue;

        public ConcentrationBuilderImpl() {
            initialConcentration = new InitialConcentration();
        }

        @Override
        public PoolStep entity(ChemicalEntity entity) {
            initialConcentration.setEntity(entity);
            return this;
        }

        @Override
        public ConcentrationStep subsection(CellSubsection subsection) {
            initialConcentration.addCondition(SectionCondition.forSection(subsection));
            return this;
        }

        @Override
        public ConcentrationStep topology(CellTopology topology) {
            initialConcentration.addCondition(TopologyCondition.isTopology(topology));
            return this;
        }

        @Override
        public AdditionalConditionsStep concentration(Quantity<MolarConcentration> concentration) {
            initialConcentration.setConcentration(concentration);
            return this;
        }

        @Override
        public AdditionalConditionsStep molecules(double numberOfMolecules) {
            double concentration = MolarConcentration.moleculesToConcentration(numberOfMolecules);
            return concentration(humanReadable(UnitRegistry.concentration(concentration)));
        }

        @Override
        public ConcentrationUnitStep concentrationValue(double value) {
            concentrationValue = value;
            return this;
        }

        @Override
        public AdditionalConditionsStep unit(Unit<MolarConcentration> concentrationUnit) {
            return concentration(humanReadable(UnitRegistry.concentration(concentrationValue, concentrationUnit)));
        }

        @Override
        public AdditionalConditionsStep updatableIdentifiers(List<String> identifiers) {
            initialConcentration.addCondition(NodeIdentifierCondition.forIdentifiers(identifiers));
            return this;
        }

        @Override
        public AdditionalConditionsStep updatableIdentifiers(String... identifiers) {
            initialConcentration.addCondition(NodeIdentifierCondition.forIdentifiers(identifiers));
            return this;
        }

        @Override
        public AdditionalConditionsStep updatableIdentifier(String identifier) {
            initialConcentration.addCondition(NodeIdentifierCondition.forIdentifiers(identifier));
            return this;
        }

        @Override
        public AdditionalConditionsStep regions(List<CellRegion> regions) {
            initialConcentration.addCondition(RegionCondition.forRegions(regions));
            return this;
        }

        @Override
        public AdditionalConditionsStep regions(CellRegion... regions) {
            initialConcentration.addCondition(RegionCondition.forRegions(regions));
            return this;
        }

        @Override
        public AdditionalConditionsStep region(CellRegion region) {
            initialConcentration.addCondition(RegionCondition.forRegions(region));
            return this;
        }

        @Override
        public AdditionalConditionsStep inArea(Polygon polygon) {
            initialConcentration.addCondition(AreaCondition.inPolygon(polygon));
            return this;
        }

        @Override
        public AdditionalConditionsStep inRegionArea(CellRegion region) {
            initialConcentration.addCondition(AreaCondition.forRegion(region));
            return this;
        }

        @Override
        public AdditionalConditionsStep atTime(ComparableQuantity<Time> time) {
            initialConcentration.setTime(time);
            return this;
        }

        @Override
        public AdditionalConditionsStep fixed() {
            initialConcentration.setFix(true);
            return this;
        }

        @Override
        public AdditionalConditionsStep onlyNodes() {
            initialConcentration.addCondition(NodeTypeCondition.isNode());
            return this;
        }

        @Override
        public AdditionalConditionsStep onlyVesicles() {
            initialConcentration.addCondition(NodeTypeCondition.isVesicle());
            return this;
        }

        @Override
        public TimedUnitStep atTimeValue(double time) {
            timeValue = time;
            return this;
        }

        @Override
        public AdditionalConditionsStep timeUnit(Unit<Time> timeUnit) {
            return atTime(Quantities.getQuantity(timeValue, timeUnit));
        }

        @Override
        public BuildStep evidence(Evidence evidence) {
            initialConcentration.setEvidence(evidence);
            return this;
        }

        @Override
        public InitialConcentration build() {
            return initialConcentration;
        }
    }

    public static void main(String[] args) {

        SmallMolecule entity = SmallMolecule.create("001").build();

        InitialConcentration sectionConcentration = ConcentrationBuilder.create()
                .entity(entity)
                .subsection(VESICLE_LUMEN)
                .concentrationValue(10)
                .microMolar()
                .build();

        System.out.println(sectionConcentration);

        InitialConcentration fixedConcentration = ConcentrationBuilder.create()
                .entity(entity)
                .subsection(CYTOPLASM)
                .concentrationValue(1)
                .nanoMolar()
                .fixed()
                .build();

        System.out.println(fixedConcentration);

        InitialConcentration timedConcentration = ConcentrationBuilder.create()
                .entity(entity)
                .subsection(CYTOPLASM)
                .molecules(200)
                .atTimeValue(5)
                .seconds()
                .build();

        System.out.println(timedConcentration);

        InitialConcentration identifierConcentration = ConcentrationBuilder.create()
                .entity(entity)
                .subsection(CYTOPLASM)
                .concentrationValue(100)
                .milliMolar()
                .updatableIdentifier("n(9,1)")
                .build();

        System.out.println(identifierConcentration);

        InitialConcentration areaConcentration = ConcentrationBuilder.create()
                .entity(entity)
                .topology(CellTopology.INNER)
                .concentrationValue(0.5)
                .microMolar()
                .onlyNodes()
                .inArea(new Rectangle(new Vector2D(0, 0), new Vector2D(100, 100)))
                .build();

        System.out.println(areaConcentration);

    }

}
