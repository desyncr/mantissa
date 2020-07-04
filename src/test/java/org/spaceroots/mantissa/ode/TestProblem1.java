package org.spaceroots.mantissa.ode;

/**
 * This class is used in the junit tests for the ODE integrators.

 * <p>This specific problem is the following differential equation :
 * <pre>
 *    y' = -y
 * </pre>
 * the solution of this equation is a simple exponential function :
 * <pre>
 *   y (t) = y (t0) exp (t0-t)
 * </pre>
 * </p>

 */
class TestProblem1
  extends TestProblemAbstract {

  /** theoretical state */
  private double[] y;

  /**
   * Simple constructor.
   */
  public TestProblem1() {
    super();
    double[] y0 = { 1.0, 0.1 };
    setInitialConditions(0.0, y0);
    setFinalConditions(4.0);
    double[] errorScale = { 1.0, 1.0 };
    setErrorScale(errorScale);
    y = new double[y0.length];
  }
 
  /**
   * Copy constructor.
   * @param problem problem to copy
   */
  public TestProblem1(TestProblem1 problem) {
    super(problem);
    y = (double[]) problem.y.clone();
  }

  /**
   * Clone operation.
   * @return a copy of the instance
   */
  public Object clone() {
    return new TestProblem1(this);
  }

  public void doComputeDerivatives(double t, double[] y, double[] yDot) {

    // compute the derivatives
    for (int i = 0; i < n; ++i)
      yDot[i] = -y[i];

  }

  public double[] computeTheoreticalState(double t) {
    double c = Math.exp (t0 - t);
    for (int i = 0; i < n; ++i) {
      y[i] = c * y0[i];
    }
    return y;
  }

}
