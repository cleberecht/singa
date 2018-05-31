package de.bioforscher.singa.features.parameters;

import de.bioforscher.singa.features.model.QuantityFormatter;
import de.bioforscher.singa.features.quantities.DynamicViscosity;
import de.bioforscher.singa.features.quantities.MolarConcentration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tec.uom.se.function.MultiplyConverter;
import tec.uom.se.quantity.Quantities;
import tec.uom.se.unit.TransformedUnit;

import javax.measure.Quantity;
import javax.measure.Unit;
import javax.measure.quantity.*;
import java.text.DecimalFormat;
import java.util.Observable;
import java.util.Observer;

import static de.bioforscher.singa.features.units.UnitProvider.MOLE_PER_LITRE;
import static de.bioforscher.singa.features.units.UnitProvider.PASCAL_SECOND;
import static tec.uom.se.unit.MetricPrefix.*;
import static tec.uom.se.unit.Units.*;

public class Environment extends Observable {

    private static final Logger logger = LoggerFactory.getLogger(Environment.class);

    public static final QuantityFormatter<Time> TIME_FORMATTER = new QuantityFormatter<>(SECOND, true);

    private static final DecimalFormat DELTA_VALUE_FORMATTER = new DecimalFormat("0.####E00");
    public static final QuantityFormatter<MolarConcentration> DELTA_FORMATTER = new QuantityFormatter<>(DELTA_VALUE_FORMATTER, MOLE_PER_LITRE, false);

    /**
     * Standard node distance [length] (100 nm)
     */
    public static final Quantity<Length> DEFAULT_NODE_DISTANCE = Quantities.getQuantity(100.0, NANO(METRE));

    /**
     * Standard time step size [time] (1 us)
     */
    public static final Quantity<Time> DEFAULT_TIME_STEP = Quantities.getQuantity(1.0, MICRO(SECOND));

    /**
     * Standard system temperature [temperature] (293 K = 20 C)
     */
    public static final Quantity<Temperature> DEFAULT_TEMPERATURE = Quantities.getQuantity(293.0, KELVIN);

    /**
     * Standard system viscosity [pressure per time] (1 mPa*s = 1cP = Viscosity of Water at 20 C)
     */
    public static final Quantity<DynamicViscosity> DEFAULT_VISCOSITY = Quantities.getQuantity(1.0, MILLI(PASCAL_SECOND));

    /**
     * Standard system extend [length] (5 um)
     */
    public static final Quantity<Length> DEFAULT_SYSTEM_EXTEND = Quantities.getQuantity(5.0, MICRO(METRE));

    /**
     * Standard simulation extend [pseudo length] 500
     */
    public static final double DEFAULT_SIMULATION_EXTEND = 500;

    private static Environment instance;

    private Quantity<Length> systemExtend;
    private Quantity<Length> systemScale;

    private double simulationExtend;
    private double simulationScale;

    private Quantity<Length> nodeDistance;
    private Quantity<Time> timeStep;
    private Quantity<Temperature> systemTemperature;
    private Quantity<DynamicViscosity> systemViscosity;

    private Unit<MolarConcentration> transformedMolarConcentration;
    private Quantity<MolarConcentration> emptyConcentration = Quantities.getQuantity(0.0, MOLE_PER_LITRE);

    private Quantity<Volume> subsectionVolume;
    private Unit<Volume> transformedVolume;
    private Unit<Area> transformedArea;
    private Unit<Length> transformedLength;


    private Environment() {
        nodeDistance = DEFAULT_NODE_DISTANCE;
        timeStep = DEFAULT_TIME_STEP;
        systemTemperature = DEFAULT_TEMPERATURE;
        systemViscosity = DEFAULT_VISCOSITY;
        systemExtend = DEFAULT_SYSTEM_EXTEND;
        simulationExtend = DEFAULT_SIMULATION_EXTEND;
        transformedMolarConcentration = MOLE_PER_LITRE;
        transformedVolume = CUBIC_METRE;
        transformedArea = SQUARE_METRE;
        transformedLength = METRE;

        setSystemAnsSimulationScales();
        setChanged();
        notifyObservers();
    }

    private static Environment getInstance() {
        if (instance == null) {
            synchronized (Environment.class) {
                instance = new Environment();
            }
        }
        return instance;
    }

    public static void reset() {
        getInstance().nodeDistance = DEFAULT_NODE_DISTANCE;
        getInstance().timeStep = DEFAULT_TIME_STEP;
        getInstance().systemTemperature = DEFAULT_TEMPERATURE;
        getInstance().systemViscosity = DEFAULT_VISCOSITY;
        getInstance().systemExtend = DEFAULT_SYSTEM_EXTEND;
        getInstance().simulationExtend = DEFAULT_SIMULATION_EXTEND;
        getInstance().transformedMolarConcentration = MOLE_PER_LITRE;
        getInstance().transformedVolume = CUBIC_METRE;
        getInstance().transformedArea = SQUARE_METRE;
        getInstance().transformedLength = METRE;

        getInstance().setSystemAnsSimulationScales();
        getInstance().setChanged();
        getInstance().notifyObservers();
    }

    public static Quantity<Length> getNodeDistance() {
        return instance.nodeDistance;
    }

    public static void setNodeDistance(Quantity<Length> nodeDistance) {
        logger.debug("Setting node distance to {}.", nodeDistance);
        getInstance().nodeDistance = nodeDistance;
        getInstance().setTransformedMolarConcentrationUnit();
        getInstance().transformSpaceScales();
        getInstance().emptyConcentration = Quantities.getQuantity(0.0, getTransformedMolarConcentration());
        getInstance().setChanged();
        getInstance().notifyObservers();
    }

    public static Quantity<MolarConcentration> emptyConcentration() {
        return getInstance().emptyConcentration;
    }

    private void setTransformedMolarConcentrationUnit() {
        final Unit<Length> nodeDistanceUnit = nodeDistance.getUnit();
        final Unit<MolarConcentration> transformedUnit = MOLE.divide(nodeDistanceUnit.pow(3)).asType(MolarConcentration.class);
        if (nodeDistance.getValue().doubleValue() == 1.0) {
            transformedMolarConcentration = transformedUnit;
        } else {
            transformedMolarConcentration = new TransformedUnit<>(transformedUnit, new MultiplyConverter(Math.pow(nodeDistance.getValue().doubleValue(), 3)));
        }
    }

    public static Unit<MolarConcentration> getTransformedMolarConcentration() {
        return getInstance().transformedMolarConcentration;
    }

    public static Quantity<MolarConcentration> transformToVolume(Quantity<MolarConcentration> concentration, Quantity<Volume> volume) {
        final Unit<Volume> volumeUnit = volume.getUnit();
        final Unit<MolarConcentration> transformedUnit = MOLE.divide(volumeUnit).asType(MolarConcentration.class);
        if (volume.getValue().doubleValue() == 1.0) {
            return concentration.to(transformedUnit);
        } else {
            return concentration.to(new TransformedUnit<>(transformedUnit, new MultiplyConverter(volume.getValue().doubleValue())));
        }
    }

    public void transformSpaceScales() {
        // base length unit
        final Unit<Length> lengthUnit = nodeDistance.getUnit();
        // base area unit
        final Unit<Area> areaUnit = lengthUnit.pow(2).asType(Area.class);
        // base volume unit
        final Unit<Volume> volumeUnit = lengthUnit.pow(3).asType(Volume.class);
        // transform with multiplier if necessary
        if (nodeDistance.getValue().doubleValue() == 1.0) {
            transformedLength = lengthUnit;
            transformedArea = areaUnit;
            transformedVolume = volumeUnit;
            subsectionVolume = Quantities.getQuantity(1.0, volumeUnit);
        } else {
            transformedLength = new TransformedUnit<>(lengthUnit, new MultiplyConverter(nodeDistance.getValue().doubleValue()));
            transformedArea = new TransformedUnit<>(areaUnit, new MultiplyConverter(Math.pow(nodeDistance.getValue().doubleValue(), 2)));
            transformedVolume = new TransformedUnit<>(volumeUnit, new MultiplyConverter(Math.pow(nodeDistance.getValue().doubleValue(), 3)));
        }
    }

    public static Quantity<Volume> getSubsectionVolume() {
        return getInstance().subsectionVolume;
    }

    public static Unit<Area> getTransformedArea() {
        return getInstance().transformedArea;
    }

    public static Unit<Length> getTransformedLength() {
        return getInstance().transformedLength;
    }

    public static Unit<Volume> getTransformedVolume() {
        return getInstance().transformedVolume;
    }

    public static Quantity<Temperature> getTemperature() {
        return getInstance().systemTemperature;
    }

    public static void setTemperature(Quantity<Temperature> temperature) {
        logger.debug("Setting environmental temperature to {}.", temperature);
        getInstance().systemTemperature = temperature.to(KELVIN);
    }

    public static Quantity<DynamicViscosity> getViscosity() {
        return getInstance().systemViscosity;
    }

    public static void setSystemViscosity(Quantity<DynamicViscosity> viscosity) {
        logger.debug("Setting environmental dynamic viscosity of to {}.", viscosity);
        getInstance().systemViscosity = viscosity.to(MILLI(PASCAL_SECOND));
    }

    public static Quantity<Time> getTimeStep() {
        return getInstance().timeStep;
    }

    public static void setTimeStep(Quantity<Time> timeStep) {
        getInstance().timeStep = timeStep;
    }

    public static void setNodeSpacingToDiameter(Quantity<Length> diameter, int spanningNodes) {
        logger.debug("Setting system diameter to {} using {} spanning nodes.", diameter, spanningNodes);
        setNodeDistance(diameter.divide(spanningNodes));
    }

    public static Quantity<Length> getSystemExtend() {
        return getInstance().systemExtend;
    }

    public static void setSystemExtend(Quantity<Length> systemExtend) {
        getInstance().systemExtend = systemExtend;
        getInstance().setSystemAnsSimulationScales();
    }

    public static double getSimulationExtend() {
        return getInstance().simulationExtend;
    }

    public static void setSimulationExtend(double simulationExtend) {
        getInstance().simulationExtend = simulationExtend;
        getInstance().setSystemAnsSimulationScales();
    }

    public static Quantity<Length> getSystemScale() {
        return getInstance().systemScale;
    }

    public static double getSimulationScale() {
        return getInstance().simulationScale;
    }

    private void setSystemAnsSimulationScales() {
        simulationScale = simulationExtend / systemExtend.getValue().doubleValue();
        systemScale = systemExtend.divide(simulationExtend);
    }

    public static Quantity<Length> convertSimulationToSystemScale(double simulationDistance) {
        return getInstance().systemScale.multiply(simulationDistance);
    }

    public static double convertSystemToSimulationScale(Quantity<Length> realDistance) {
        return realDistance.to(getInstance().systemExtend.getUnit()).getValue().doubleValue() * getInstance().simulationScale;
    }

    public static void attachObserver(Observer observer) {
        getInstance().addObserver(observer);
    }

}
