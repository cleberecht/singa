package bio.singa.simulation.model.agents.pointlike;

import bio.singa.chemistry.features.ChemistryFeatureContainer;
import bio.singa.features.quantities.PixelDiffusivity;
import bio.singa.features.model.Evidence;
import bio.singa.features.model.Feature;
import bio.singa.features.model.FeatureContainer;
import bio.singa.features.model.Featureable;
import bio.singa.features.parameters.Environment;
import bio.singa.features.quantities.Geometry;
import bio.singa.features.units.UnitRegistry;
import bio.singa.mathematics.geometry.faces.Circle;
import bio.singa.mathematics.vectors.Vector2D;
import bio.singa.simulation.model.agents.linelike.LineLikeAgent;
import bio.singa.simulation.model.graphs.AutomatonNode;
import bio.singa.simulation.model.modules.concentration.ConcentrationDelta;
import bio.singa.simulation.model.modules.concentration.ConcentrationDeltaManager;
import bio.singa.simulation.model.modules.displacement.DisplacementBasedModule;
import bio.singa.simulation.model.modules.displacement.DisplacementDelta;
import bio.singa.simulation.model.modules.displacement.DisplacementDeltaManager;
import bio.singa.simulation.model.sections.CellRegion;
import bio.singa.simulation.model.sections.CellSubsection;
import bio.singa.simulation.model.sections.CellTopology;
import bio.singa.simulation.model.sections.ConcentrationContainer;
import bio.singa.simulation.model.simulation.Updatable;

import javax.measure.Quantity;
import javax.measure.quantity.Area;
import javax.measure.quantity.Length;
import javax.measure.quantity.Volume;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static bio.singa.features.quantities.Diffusivity.SQUARE_MICROMETRE_PER_SECOND;
import static bio.singa.features.model.Evidence.SourceType.LITERATURE;
import static bio.singa.simulation.model.sections.CellRegions.VESICLE_REGION;

/**
 * @author cl
 */
public class Vesicle implements Updatable, Featureable {

    public static final Evidence ROTHMAN2016 = new Evidence(LITERATURE, "Rothman 2016 ", "Rothman, Jason Seth, et al. \"Physical determinants of vesicle mobility and supply at a central synapse.\" Elife 5 (2016): e15133.");
    public static final Evidence HOLT2004 = new Evidence(LITERATURE, "Holt 2004", "Holt, Matthew, et al. \"High mobility of vesicles supports continuous exocytosis at a ribbon synapse.\" Current Biology 14.3 (2004): 173-183.");
    protected static final Set<Class<? extends Feature>> availableFeatures = new HashSet<>();
    public static AtomicInteger vesicleCounter = new AtomicInteger();
    // Originally 1.5e-2
    public static PixelDiffusivity DEFAULT_VESICLE_DIFFUSIVITY = PixelDiffusivity.of(1.5e-6, SQUARE_MICROMETRE_PER_SECOND)
            .comment("diffusivity of macroscopic entities")
            .evidence(ROTHMAN2016, HOLT2004)
            .build();

    static {
        availableFeatures.add(PixelDiffusivity.class);
    }

    protected FeatureContainer features;
    private String identifier;
    private Quantity<Length> radius;
    private Quantity<Area> area;
    private Quantity<Volume> volume;
    private ConcentrationDeltaManager concentrationManager;
    private DisplacementDeltaManager displacementManager;

    private CellRegion region;
    private Map<AutomatonNode, Double> associatedNodes;

    private String state;
    private String targetDirection;
    private LineLikeAgent attachedFilament;
    private ListIterator<Vector2D> segmentIterator;

    public Vesicle(CellRegion region, Vector2D position, Quantity<Length> radius) {
        identifier = generateIdentifier();
        this.region = region;
        features = new ChemistryFeatureContainer();
        setRadius(radius);
        concentrationManager = new ConcentrationDeltaManager(region.setUpConcentrationContainer());
        displacementManager = new DisplacementDeltaManager(position);
        associatedNodes = new HashMap<>();
        state = VesicleStateRegistry.UNATTACHED;
        setFeature(DEFAULT_VESICLE_DIFFUSIVITY);
    }

    public Vesicle(Vector2D position, Quantity<Length> radius) {
        this(VESICLE_REGION, position, radius);
    }

    private static String generateIdentifier() {
        return "v" + vesicleCounter.getAndIncrement();
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public String getStringIdentifier() {
        return identifier;
    }

    @Override
    public Vector2D getPosition() {
        return displacementManager.getCurrentPosition();
    }

    public Quantity<Length> getRadius() {
        return radius;
    }

    public void setRadius(Quantity<Length> radius) {
        this.radius = radius.to(UnitRegistry.getSpaceUnit());
        area = Geometry.calculateArea(this.radius);
        volume = Geometry.calculateVolume(this.radius);
    }

    public Quantity<Area> getArea() {
        return area;
    }

    public Quantity<Volume> getVolume() {
        return volume;
    }

    public Vector2D getNextPosition() {
        return displacementManager.getNextPosition();
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public LineLikeAgent getAttachedFilament() {
        return attachedFilament;
    }

    public void setAttachedFilament(LineLikeAgent attachedFilament) {
        this.attachedFilament = attachedFilament;
    }

    public String getTargetDirection() {
        return targetDirection;
    }

    public void setTargetDirection(String targetDirection) {
        this.targetDirection = targetDirection;
    }

    public ListIterator<Vector2D> getSegmentIterator() {
        return segmentIterator;
    }

    public void setSegmentIterator(ListIterator<Vector2D> segmentIterator) {
        this.segmentIterator = segmentIterator;
    }

    public CellRegion getRegion() {
        return region;
    }

    public void setRegion(CellRegion region) {
        this.region = region;
    }

    public void clearAttachmentInformation() {
        setState(VesicleStateRegistry.UNATTACHED);
        setAttachedFilament(null);
        setTargetDirection(null);
        setSegmentIterator(null);
    }

    public void addPotentialSpatialDelta(DisplacementDelta spatialDelta) {
        displacementManager.addPotentialDisplacementDelta(spatialDelta);
    }

    public Optional<DisplacementDelta> getSpatialDelta(DisplacementBasedModule module) {
        return displacementManager.getPotentialDisplacementDelta(module);
    }

    public Map<AutomatonNode, Double> getAssociatedNodes() {
        return associatedNodes;
    }

    public void addAssociatedNode(AutomatonNode node, double relativeArea) {
        associatedNodes.put(node, relativeArea);
    }

    public void clearAssociatedNodes() {
        getConcentrationContainer().removeSubsection(CellTopology.INNER);
        associatedNodes.clear();
    }

    public Vector2D calculateTotalDisplacement() {
        return displacementManager.calculateTotalDisplacement();
    }

    public void clearPotentialDisplacementDeltas() {
        displacementManager.clearPotentialDisplacementDeltas();
    }

    public void resetNextPosition() {
        displacementManager.resetNextPosition();
    }

    public void updatePosition() {
        displacementManager.updatePosition();
    }

    @Override
    public ConcentrationDeltaManager getConcentrationManager() {
        return concentrationManager;
    }

    /**
     * Returns {@code true} if this node is observed.
     *
     * @return {@code true} if this node is observed.
     */
    public boolean isObserved() {
        return concentrationManager.isObserved();
    }

    /**
     * Sets the observed state of this node.
     *
     * @param isObserved {@code true} if this node is observed.
     */
    public void setObserved(boolean isObserved) {
        concentrationManager.setObserved(isObserved);
    }

    @Override
    public ConcentrationContainer getConcentrationContainer() {
        return concentrationManager.getConcentrationContainer();
    }

    @Override
    public CellRegion getCellRegion() {
        return region;
    }

    @Override
    public Set<CellSubsection> getAllReferencedSections() {
        return concentrationManager.getConcentrationContainer().getReferencedSubsections();
    }

    @Override
    public void addPotentialDelta(ConcentrationDelta delta) {
        concentrationManager.addPotentialDelta(delta);
    }

    public Circle getCircleRepresentation() {
        return new Circle(displacementManager.getCurrentPosition(), Environment.convertSystemToSimulationScale(radius));
    }

    @Override
    public Collection<Feature<?>> getFeatures() {
        return features.getAllFeatures();
    }

    @Override
    public <FeatureType extends Feature> FeatureType getFeature(Class<FeatureType> featureTypeClass) {
        if (!features.hasFeature(featureTypeClass)) {
            setFeature(featureTypeClass);
        }
        return features.getFeature(featureTypeClass);
    }

    @Override
    public <FeatureType extends Feature> void setFeature(Class<FeatureType> featureTypeClass) {
        features.setFeature(featureTypeClass, this);
    }

    @Override
    public <FeatureType extends Feature> void setFeature(FeatureType feature) {
        features.setFeature(feature);
    }

    @Override
    public <FeatureType extends Feature> boolean hasFeature(Class<FeatureType> featureTypeClass) {
        return features.hasFeature(featureTypeClass);
    }

    @Override
    public Set<Class<? extends Feature>> getAvailableFeatures() {
        return availableFeatures;
    }

    @Override
    public String toString() {
        return identifier + " radius = " + radius + " " + " position = " + displacementManager.getCurrentPosition();
    }

}
