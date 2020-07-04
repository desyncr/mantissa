package org.spaceroots.mantissa.optimization;

import org.spaceroots.mantissa.random.RandomVectorGenerator;
import org.spaceroots.mantissa.random.UncorrelatedRandomVectorGenerator;
import org.spaceroots.mantissa.random.CorrelatedRandomVectorGenerator;
import org.spaceroots.mantissa.random.UniformRandomGenerator;
import org.spaceroots.mantissa.random.VectorialSampleStatistics;
import org.spaceroots.mantissa.random.NotPositiveDefiniteMatrixException;

import java.util.Arrays;
import java.util.Comparator;

/** This class implements simplex-based direct search optimization
 * algorithms.

 * <p>Direct search method only use cost function values, they don't
 * need derivatives and don't either try to compute approximation of
 * the derivatives. According to a 1996 paper by Margaret H. Wright
 * (<a href="http://cm.bell-labs.com/cm/cs/doc/96/4-02.ps.gz">Direct
 * Search Methods: Once Scorned, Now Respectable</a>), they are used
 * when either the computation of the derivative is impossible (noisy
 * functions, unpredictable dicontinuities) or difficult (complexity,
 * computation cost). In the first cases, rather than an optimum, a
 * <em>not too bad</em> point is desired. In the latter cases, an
 * optimum is desired but cannot be reasonably found. In all cases
 * direct search methods can be useful.</p>

 * <p>Simplex-based direct search methods are based on comparison of
 * the cost function values at the vertices of a simplex (which is a
 * set of n+1 points in dimension n) that is updated by the algorithms
 * steps.</p>

 * <p>The instances can be built either in single-start or in
 * multi-start mode. Multi-start is a traditional way to try to avoid
 * beeing trapped in a local minimum and miss the global minimum of a
 * function. It can also be used to verify the convergence of an
 * algorithm. In multi-start mode, the {@link #minimizes(CostFunction,
 * int, ConvergenceChecker, double[], double[]) minimizes}
 * method returns the best minimum found after all starts, and the
 * {@link #getMinima getMinima} method can be used to retrieve all
 * minima from all starts (including the one already provided by the
 * {@link #minimizes(CostFunction, int, ConvergenceChecker, double[],
 * double[]) minimizes} method).</p>

 * <p>This class is the base class performing the boilerplate simplex
 * initialization and handling. The simplex update by itself is
 * performed by the derived classes according to the implemented
 * algorithms.</p>

 * @author Luc Maisonobe
 * @version $Id: DirectSearchOptimizer.java 1709 2006-12-03 21:16:50Z luc $
 * @see CostFunction
 * @see NelderMead
 * @see MultiDirectional
 */
public abstract class DirectSearchOptimizer {

  /** Simple constructor.
   */
  protected DirectSearchOptimizer() {
  }

  /** Minimizes a cost function.
   * <p>The initial simplex is built from two vertices that are
   * considered to represent two opposite vertices of a box parallel
   * to the canonical axes of the space. The simplex is the subset of
   * vertices encountered while going from vertexA to vertexB
   * travelling along the box edges only. This can be seen as a scaled
   * regular simplex using the projected separation between the given
   * points as the scaling factor along each coordinate axis.</p>
   * <p>The optimization is performed in single-start mode.</p>
   * @param f cost function
   * @param maxEvaluations maximal number of function calls for each
   * start (note that the number will be checked <em>after</em>
   * complete simplices have been evaluated, this means that in some
   * cases this number will be exceeded by a few units, depending on
   * the dimension of the problem)
   * @param checker object to use to check for convergence
   * @param vertexA first vertex
   * @param vertexB last vertex
   * @return the point/cost pairs giving the minimal cost
   * @exception CostException if the cost function throws one during
   * the search
   * @exception NoConvergenceException if none of the starts did
   * converge (it is not thrown if at least one start did converge)
   */
  public PointCostPair minimizes(CostFunction f, int maxEvaluations,
                                 ConvergenceChecker checker,
                                 double[] vertexA, double[] vertexB)
    throws CostException, NoConvergenceException {

    // set up optimizer
    buildSimplex(vertexA, vertexB);
    setSingleStart();

    // compute minimum
    return minimizes(f, maxEvaluations, checker);

  }

  /** Minimizes a cost function.
   * <p>The initial simplex is built from two vertices that are
   * considered to represent two opposite vertices of a box parallel
   * to the canonical axes of the space. The simplex is the subset of
   * vertices encountered while going from vertexA to vertexB
   * travelling along the box edges only. This can be seen as a scaled
   * regular simplex using the projected separation between the given
   * points as the scaling factor along each coordinate axis.</p>
   * <p>The optimization is performed in multi-start mode.</p>
   * @param f cost function
   * @param maxEvaluations maximal number of function calls for each
   * start (note that the number will be checked <em>after</em>
   * complete simplices have been evaluated, this means that in some
   * cases this number will be exceeded by a few units, depending on
   * the dimension of the problem)
   * @param checker object to use to check for convergence
   * @param vertexA first vertex
   * @param vertexB last vertex
   * @param starts number of starts to perform (including the
   * first one), multi-start is disabled if value is less than or
   * equal to 1
   * @param seed seed for the random vector generator (32 bits
   * integers array). If null, the current time will be used for the
   * generator initialization.
   * @return the point/cost pairs giving the minimal cost
   * @exception CostException if the cost function throws one during
   * the search
   * @exception NoConvergenceException if none of the starts did
   * converge (it is not thrown if at least one start did converge)
   */
  public PointCostPair minimizes(CostFunction f, int maxEvaluations,
                                 ConvergenceChecker checker,
                                 double[] vertexA, double[] vertexB,
                                 int starts, int[] seed)
    throws CostException, NoConvergenceException {

    // set up the simplex travelling around the box
    buildSimplex(vertexA, vertexB);

    // we consider the simplex could have been produced by a generator
    // having its mean value at the center of the box, the standard
    // deviation along each axe beeing the corresponding half size
    double[] mean              = new double[vertexA.length];
    double[] standardDeviation = new double[vertexA.length];
    for (int i = 0; i < vertexA.length; ++i) {
      mean[i]              = 0.5 * (vertexA[i] + vertexB[i]);
      standardDeviation[i] = 0.5 * Math.abs(vertexA[i] - vertexB[i]);
    }

    RandomVectorGenerator rvg =
      new UncorrelatedRandomVectorGenerator(mean, standardDeviation,
                                            new UniformRandomGenerator(seed));
    setMultiStart(starts, rvg);

    // compute minimum
    return minimizes(f, maxEvaluations, checker);

  }

  /** Minimizes a cost function.
   * <p>The simplex is built from all its vertices.</p>
   * <p>The optimization is performed in single-start mode.</p>
   * @param f cost function
   * @param maxEvaluations maximal number of function calls for each
   * start (note that the number will be checked <em>after</em>
   * complete simplices have been evaluated, this means that in some
   * cases this number will be exceeded by a few units, depending on
   * the dimension of the problem)
   * @param checker object to use to check for convergence
   * @param vertices array containing all vertices of the simplex
   * @return the point/cost pairs giving the minimal cost
   * @exception CostException if the cost function throws one during
   * the search
   * @exception NoConvergenceException if none of the starts did
   * converge (it is not thrown if at least one start did converge)
   */
  public PointCostPair minimizes(CostFunction f, int maxEvaluations,
                                 ConvergenceChecker checker,
                                 double[][] vertices)
    throws CostException, NoConvergenceException {

    // set up optimizer
    buildSimplex(vertices);
    setSingleStart();

    // compute minimum
    return minimizes(f, maxEvaluations, checker);

  }

  /** Minimizes a cost function.
   * <p>The simplex is built from all its vertices.</p>
   * <p>The optimization is performed in multi-start mode.</p>
   * @param f cost function
   * @param maxEvaluations maximal number of function calls for each
   * start (note that the number will be checked <em>after</em>
   * complete simplices have been evaluated, this means that in some
   * cases this number will be exceeded by a few units, depending on
   * the dimension of the problem)
   * @param checker object to use to check for convergence
   * @param vertices array containing all vertices of the simplex
   * @param starts number of starts to perform (including the
   * first one), multi-start is disabled if value is less than or
   * equal to 1
   * @param seed seed for the random vector generator (32 bits
   * integers array). If null, the current time will be used for the
   * generator initialization.
   * @return the point/cost pairs giving the minimal cost
   * @exception NotPositiveDefiniteMatrixException if the vertices
   * array is degenerated
   * @exception CostException if the cost function throws one during
   * the search
   * @exception NoConvergenceException if none of the starts did
   * converge (it is not thrown if at least one start did converge)
   */
  public PointCostPair minimizes(CostFunction f, int maxEvaluations,
                                 ConvergenceChecker checker,
                                 double[][] vertices,
                                 int starts, int[] seed)
    throws NotPositiveDefiniteMatrixException,
           CostException, NoConvergenceException {

    // store the points into the simplex
    buildSimplex(vertices);

    // compute the statistical properties of the simplex points
    VectorialSampleStatistics statistics = new VectorialSampleStatistics();
    for (int i = 0; i < vertices.length; ++i) {
      statistics.add(vertices[i]);
    }

    RandomVectorGenerator rvg =
      new CorrelatedRandomVectorGenerator(statistics.getMean(),
                                          statistics.getCovarianceMatrix(null),
                                          new UniformRandomGenerator(seed));
    setMultiStart(starts, rvg);

    // compute minimum
    return minimizes(f, maxEvaluations, checker);

  }

  /** Minimizes a cost function.
   * <p>The simplex is built randomly.</p>
   * <p>The optimization is performed in single-start mode.</p>
   * @param f cost function
   * @param maxEvaluations maximal number of function calls for each
   * start (note that the number will be checked <em>after</em>
   * complete simplices have been evaluated, this means that in some
   * cases this number will be exceeded by a few units, depending on
   * the dimension of the problem)
   * @param checker object to use to check for convergence
   * @param generator random vector generator
   * @return the point/cost pairs giving the minimal cost
   * @exception CostException if the cost function throws one during
   * the search
   * @exception NoConvergenceException if none of the starts did
   * converge (it is not thrown if at least one start did converge)
   */
  public PointCostPair minimizes(CostFunction f, int maxEvaluations,
                                 ConvergenceChecker checker,
                                 RandomVectorGenerator generator)
    throws CostException, NoConvergenceException {

    // set up optimizer
    buildSimplex(generator);
    setSingleStart();

    // compute minimum
    return minimizes(f, maxEvaluations, checker);

  }

  /** Minimizes a cost function.
   * <p>The simplex is built randomly.</p>
   * <p>The optimization is performed in multi-start mode.</p>
   * @param f cost function
   * @param maxEvaluations maximal number of function calls for each
   * start (note that the number will be checked <em>after</em>
   * complete simplices have been evaluated, this means that in some
   * cases this number will be exceeded by a few units, depending on
   * the dimension of the problem)
   * @param checker object to use to check for convergence
   * @param generator random vector generator
   * @param starts number of starts to perform (including the
   * first one), multi-start is disabled if value is less than or
   * equal to 1
   * @return the point/cost pairs giving the minimal cost
   * @exception CostException if the cost function throws one during
   * the search
   * @exception NoConvergenceException if none of the starts did
   * converge (it is not thrown if at least one start did converge)
   */
  public PointCostPair minimizes(CostFunction f, int maxEvaluations,
                                 ConvergenceChecker checker,
                                 RandomVectorGenerator generator,
                                 int starts)
    throws CostException, NoConvergenceException {

    // set up optimizer
    buildSimplex(generator);
    setMultiStart(starts, generator);

    // compute minimum
    return minimizes(f, maxEvaluations, checker);

  }

  /** Build a simplex from two extreme vertices.
   * <p>The two vertices are considered to represent two opposite
   * vertices of a box parallel to the canonical axes of the
   * space. The simplex is the subset of vertices encountered while
   * going from vertexA to vertexB travelling along the box edges
   * only. This can be seen as a scaled regular simplex using the
   * projected separation between the given points as the scaling
   * factor along each coordinate axis.</p>
   * @param vertexA first vertex
   * @param vertexB last vertex
   */
  private void buildSimplex(double[] vertexA, double[] vertexB) {

    int n = vertexA.length;
    simplex = new PointCostPair[n + 1];

    // set up the simplex travelling around the box
    for (int i = 0; i <= n; ++i) {
      double[] vertex = new double[n];
      if (i > 0) {
        System.arraycopy(vertexB, 0, vertex, 0, i);
      }
      if (i < n) {
        System.arraycopy(vertexA, i, vertex, i, n - i);
      }
      simplex[i] = new PointCostPair(vertex, Double.NaN);
    }

  }

  /** Build a simplex from all its points.
   * @param vertices array containing all vertices of the simplex
   */
  private void buildSimplex(double[][] vertices) {
    int n = vertices.length - 1;
    simplex = new PointCostPair[n + 1];
    for (int i = 0; i <= n; ++i) {
      simplex[i] = new PointCostPair(vertices[i], Double.NaN);
    }
  }

  /** Build a simplex randomly.
   * @param generator random vector generator
   */
  private void buildSimplex(RandomVectorGenerator generator) {

    // use first vector size to compute the number of points
    double[] vertex = generator.nextVector();
    int n = vertex.length;
    simplex = new PointCostPair[n + 1];
    simplex[0] = new PointCostPair(vertex, Double.NaN);

    // fill up the vertex
    for (int i = 1; i <= n; ++i) {
      simplex[i] = new PointCostPair(generator.nextVector(), Double.NaN);
    }

  }

  /** Set up single-start mode.
   */
  private void setSingleStart() {
    starts    = 1;
    generator = null;
    minima    = null;
  }

  /** Set up multi-start mode.
   * @param starts number of starts to perform (including the
   * first one), multi-start is disabled if value is less than or
   * equal to 1
   * @param generator random vector generator to use for restarts
   */
  public void setMultiStart(int starts, RandomVectorGenerator generator) {
    if (starts < 2) {
      this.starts    = 1;
      this.generator = null;
      minima         = null;
    } else {
      this.starts    = starts;
      this.generator = generator;
      minima         = null;
    }
  }

  /** Get all the minima found during the last call to {@link
   * #minimizes(CostFunction, int, ConvergenceChecker, double[], double[])
   * minimizes}.
   * <p>The optimizer stores all the minima found during a set of
   * restarts when multi-start mode is enabled. The {@link
   * #minimizes(CostFunction, int, ConvergenceChecker, double[], double[])
   * minimizes} method returns the best point only. This method
   * returns all the points found at the end of each starts, including
   * the best one already returned by the {@link #minimizes(CostFunction,
   * int, ConvergenceChecker, double[], double[]) minimizes} method.
   * The array as one element for each start as specified in the constructor
   * (it has one element only if optimizer has been set up for single-start).</p>
   * <p>The array containing the minima is ordered with the results
   * from the runs that did converge first, sorted from lowest to
   * highest minimum cost, and null elements corresponding to the runs
   * that did not converge (all elements will be null if the {@link
   * #minimizes(CostFunction, int, ConvergenceChecker, double[], double[])
   * minimizes} method throwed a {@link NoConvergenceException
   * NoConvergenceException}).</p>
   * @return array containing the minima, or null if {@link
   * #minimizes(CostFunction, int, ConvergenceChecker, double[], double[])
   * minimizes} has not been called
   */
  public PointCostPair[] getMinima() {
    return (PointCostPair[]) minima.clone();
  }

  /** Minimizes a cost function.
   * @param f cost function
   * @param maxEvaluations maximal number of function calls for each
   * start (note that the number will be checked <em>after</em>
   * complete simplices have been evaluated, this means that in some
   * cases this number will be exceeded by a few units, depending on
   * the dimension of the problem)
   * @param checker object to use to check for convergence
   * @return the point/cost pairs giving the minimal cost
   * @exception CostException if the cost function throws one during
   * the search
   * @exception NoConvergenceException if none of the starts did
   * converge (it is not thrown if at least one start did converge)
   */
  private PointCostPair minimizes(CostFunction f, int maxEvaluations,
                                  ConvergenceChecker checker)
    throws CostException, NoConvergenceException {

    this.f = f;
    minima = new PointCostPair[starts];
    
    // multi-start loop
    for (int i = 0; i < starts; ++i) {

      evaluations = 0;
      evaluateSimplex();

      for (boolean loop = true; loop;) {
        if (checker.converged(simplex)) {
          // we have found a minimum
          minima[i] = simplex[0];
          loop = false;
        } else if (evaluations >= maxEvaluations) {
          // this start did not converge, try a new one
          minima[i] = null;
          loop = false;
        } else {
          iterateSimplex();
        }
      }

      if (i < (starts - 1)) {
        // restart
        buildSimplex(generator);
      }

    }

    // sort the minima from lowest cost to highest cost, followed by
    // null elements
    Arrays.sort(minima, pointCostPairComparator);

    // return the found point given the lowest cost
    if (minima[0] == null) {
      throw new NoConvergenceException("none of the {0} start points"
                                       + " lead to convergence",
                                       new String[] {
                                         Integer.toString(starts)
                                       });
    }
    return minima[0];

  }

  /** Compute the next simplex of the algorithm.
   */
  protected abstract void iterateSimplex()
    throws CostException;

  /** Evaluate the cost on one point.
   * <p>A side effect of this method is to count the number of
   * function evaluations</p>
   * @param x point on which the cost function should be evaluated
   * @return cost at the given point
   * @exception CostException if no cost can be computed for the parameters
   */
  protected double evaluateCost(double[] x)
    throws CostException {
    evaluations++;
    return f.cost(x);
  }

  /** Evaluate all the non-evaluated points of the simplex.
   * @exception CostException if no cost can be computed for the parameters
   */
  protected void evaluateSimplex()
    throws CostException {

    // evaluate the cost at all non-evaluated simplex points
    for (int i = 0; i < simplex.length; ++i) {
      PointCostPair pair = simplex[i];
      if (Double.isNaN(pair.cost)) {
        simplex[i] = new PointCostPair(pair.point, evaluateCost(pair.point));
      }
    }

    // sort the simplex from lowest cost to highest cost
    Arrays.sort(simplex, pointCostPairComparator);

  }

  /** Replace the worst point of the simplex by a new point.
   * @param pointCostPair point to insert
   */
  protected void replaceWorstPoint(PointCostPair pointCostPair) {
    int n = simplex.length - 1;
    for (int i = 0; i < n; ++i) {
      if (simplex[i].cost > pointCostPair.cost) {
        PointCostPair tmp = simplex[i];
        simplex[i]        = pointCostPair;
        pointCostPair     = tmp;
      }
    }
    simplex[n] = pointCostPair;
  }

  /** Comparator for {@link PointCostPair PointCostPair} objects. */
  private static Comparator pointCostPairComparator = new Comparator() {
      public int compare(Object o1, Object o2) {
        if (o1 == null) {
          return (o2 == null) ? 0 : +1;
        } else if (o2 == null) {
          return -1;
        } else {
          double cost1 = ((PointCostPair) o1).cost;
          double cost2 = ((PointCostPair) o2).cost;
          return (cost1 < cost2) ? -1 : ((o1 == o2) ? 0 : +1);
        }
      }
    };

  /** Simplex. */
  protected PointCostPair[] simplex;

  /** Cost function. */
  private CostFunction f;

  /** Number of evaluations already performed. */
  private int evaluations;

  /** Number of starts to go. */
  private int starts;

  /** Random generator for multi-start. */
  private RandomVectorGenerator generator;

  /** Found minima. */
  private PointCostPair[] minima;

}
