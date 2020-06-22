package bio.singa.features.parameters;

import bio.singa.features.quantities.DynamicViscosity;
import bio.singa.features.quantities.MolarConcentration;
import bio.singa.features.units.UnitRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.units.indriya.quantity.Quantities;

import javax.measure.Quantity;
import javax.measure.quantity.Length;
import javax.measure.quantity.Temperature;

import static bio.singa.features.units.UnitProvider.PASCAL_SECOND;
import static tech.units.indriya.unit.MetricPrefix.MICRO;
import static tech.units.indriya.unit.MetricPrefix.MILLI;
import static tech.units.indriya.unit.Units.*;

public class Environment {

    /**
     * Standard system temperature [temperature] (293 K = 20 C)
     */
    public static final Quantity<Temperature> DEFAULT_SYSTEM_TEMPERATURE = Quantities.getQuantity(293.0, KELVIN);
    /**
     * Standard system macro viscosity [pressure per time] (1 mPa*s = 1cP = Viscosity of Water at 20 C)
     * as determined by Kalwarczyk, Tomasz, et al. "Comparative analysis of viscosity of complex liquids and cytoplasm
     * of mammalian cells at the nanoscale." Nano letters 11.5 (2011): 2157-2163.
     * Average of both cell types.
     */
    public static final Quantity<DynamicViscosity> DEFAULT_MACRO_VISCOSITY = Quantities.getQuantity(34.0, MILLI(PASCAL).multiply(SECOND).asType(DynamicViscosity.class));
    /**
     * Standard system matrix viscosity [pressure per time] (1 mPa*s = 1cP = Viscosity of Water at 20 C)
     * as determined by Kalwarczyk, Tomasz, et al. "Comparative analysis of viscosity of complex liquids and cytoplasm
     * of mammalian cells at the nanoscale." Nano letters 11.5 (2011): 2157-2163.
     */
    public static final Quantity<DynamicViscosity> DEFAULT_MATRIX_VISCOSITY = Quantities.getQuantity(1.0, MILLI(PASCAL).multiply(SECOND).asType(DynamicViscosity.class));
    /**
     * Standard system extend [length] (5 um)
     */
    public static final Quantity<Length> DEFAULT_SYSTEM_EXTEND = Quantities.getQuantity(1.0, MICRO(METRE));
    /**
     * Standard simulation extend [pseudo length] 500
     */
    public static final double DEFAULT_SIMULATION_EXTEND = 100;
    private static final Logger logger = LoggerFactory.getLogger(Environment.class);
    /**
     * The singleton instance.
     */
    private static Environment instance;

    /**
     * The global temperature of the simulation system.
     */
    private Quantity<Temperature> systemTemperature;

    /**
     * The viscosity experienced by large components
     */
    private Quantity<DynamicViscosity> macroViscosity;

    /**
     * The viscosity experienced by small components
     */
    private Quantity<DynamicViscosity> matrixViscosity;

    /**
     * An empty concentration quantity
     */
    private Quantity<MolarConcentration> emptyConcentration;

    /**
     * The extend of the actual system.
     */
    private Quantity<Length> systemExtend;

    /**
     * Multiply the scale by a simulation distance to get the system distance.
     */
    private Quantity<Length> systemScale;

    /**
     * The extend of the simulation.
     */
    private double simulationExtend;

    /**
     * Multiply the scale by a system distance to get the simulation distance.
     */
    private double simulationScale;

    private Environment() {
        systemExtend = DEFAULT_SYSTEM_EXTEND;
        simulationExtend = DEFAULT_SIMULATION_EXTEND;
        systemTemperature = DEFAULT_SYSTEM_TEMPERATURE;
        macroViscosity = DEFAULT_MACRO_VISCOSITY;
        matrixViscosity = DEFAULT_MATRIX_VISCOSITY;
        emptyConcentration = UnitRegistry.concentration(0.0);
        simulationScale = simulationExtend / systemExtend.getValue().doubleValue();
        systemScale = systemExtend.divide(simulationExtend);
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
        instance = new Environment();
    }

    public static Quantity<MolarConcentration> emptyConcentration() {
        return getInstance().emptyConcentration;
    }

    public static Quantity<Temperature> getTemperature() {
        return getInstance().systemTemperature;
    }

    public static void setTemperature(Quantity<Temperature> temperature) {
        logger.debug("Setting environmental temperature to {}.", temperature);
        getInstance().systemTemperature = temperature.to(KELVIN);
    }

    public static Quantity<DynamicViscosity> getMacroViscosity() {
        return getInstance().macroViscosity;
    }

    public static void setMacroViscosity(Quantity<DynamicViscosity> viscosity) {
        logger.debug("Setting environmental macro dynamic viscosity of to {}.", viscosity);
        getInstance().macroViscosity = viscosity.to(MILLI(PASCAL_SECOND));
    }

    public static Quantity<DynamicViscosity> getMatrixViscosity() {
        return getInstance().matrixViscosity;
    }

    public static void setMatrixViscosity(Quantity<DynamicViscosity> viscosity) {
        logger.debug("Setting environmental matrix dynamic viscosity of to {}.", viscosity);
        getInstance().matrixViscosity = viscosity.to(MILLI(PASCAL_SECOND));
    }

    public static void setNodeSpacingToDiameter(Quantity<Length> diameter, int spanningNodes) {
        logger.debug("Setting system diameter to {} using {} spanning nodes.", diameter, spanningNodes);
        UnitRegistry.setSpace(diameter.divide(spanningNodes));
    }

    public static Quantity<Length> getSystemExtend() {
        return getInstance().systemExtend;
    }

    public static void setSystemExtend(Quantity<Length> systemExtend) {
        getInstance().systemExtend = systemExtend;
        getInstance().setSystemAndSimulationScales();
    }

    public static double getSimulationExtend() {
        return getInstance().simulationExtend;
    }

    public static void setSimulationExtend(double simulationExtend) {
        getInstance().simulationExtend = simulationExtend;
        getInstance().setSystemAndSimulationScales();
    }

    public static Quantity<Length> getSystemScale() {
        return getInstance().systemScale;
    }

    public static double getSimulationScale() {
        return getInstance().simulationScale;
    }

    public static void updateScales() {
        getInstance().systemExtend = getInstance().systemExtend.to(UnitRegistry.getSpaceUnit());
        getInstance().setSystemAndSimulationScales();
    }

    public static Quantity<Length> convertSimulationToSystemScale(double simulationDistance) {
        if (getSystemScale() == null) {
            getInstance().setSystemAndSimulationScales();
        }
        return getInstance().systemScale.multiply(simulationDistance);
    }

    public static Quantity<Length> scaleSimulationToSystemScale(double simulationDistance) {
        return getInstance().systemScale.multiply(simulationDistance / UnitRegistry.getSpaceScale());
    }

    public static double convertSystemToSimulationScale(Quantity<Length> realDistance) {
        if (getInstance().simulationScale == 0.0) {
            getInstance().setSystemAndSimulationScales();
        }
        return realDistance.to(UnitRegistry.getSpaceUnit()).getValue().doubleValue() * getInstance().simulationScale;
    }

    public static double scaleSystemToSimulationScale(Quantity<Length> realDistance) {
        return realDistance.to(UnitRegistry.getSpaceUnit()).getValue().doubleValue() * getInstance().simulationScale * UnitRegistry.getSpaceScale();
    }

    public static String report() {
        return "Environment: \n" +
                "system extend = " + getInstance().systemExtend + "\n" +
                "simulation extend = " + getInstance().simulationExtend + "\n" +
                "system temperature = " + getInstance().systemTemperature + "\n" +
                "system viscosity = " + getInstance().macroViscosity + "\n";
    }

    private void setSystemAndSimulationScales() {
        getInstance().simulationScale = getInstance().simulationExtend / getInstance().systemExtend.getValue().doubleValue();
        getInstance().systemScale = getInstance().systemExtend.divide(getInstance().simulationExtend);
    }

}
