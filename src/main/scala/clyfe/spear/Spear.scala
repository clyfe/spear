/**
  * SPEAR Algorithm in Scala, made for Orc
  * http://www.michael-noll.com/projects/spear-algorithm/
  * 
  * @author clyfe
  */

package clyfe.spear

import orc.values.sites.{ TotalSite1, UntypedSite }
import orc.values.OrcRecord
import orc.values.OrcTuple
import orc.error.runtime.SiteException
import scala.collection.mutable.HashMap
import cern.colt.function.tdouble.IntIntDoubleFunction
import cern.colt.matrix.tdouble.DoubleFactory2D
import cern.colt.matrix.tdouble.DoubleFactory1D
import cern.colt.matrix.tdouble.algo.DenseDoubleAlgebra
import java.util.Date

object SqrtCreditScore extends IntIntDoubleFunction {
  override def apply(first: Int, second: Int, third: Double): Double = Math.sqrt(third)
}

object Spear extends TotalSite1 with UntypedSite {

  def eval(a: AnyRef): AnyRef = {
    a match {
      case list: List[_] => {
        val activities = list.asInstanceOf[List[OrcRecord]].map(_.entries)
        run(activities)
      }
      case v => throw new SiteException("Value " + v + " must be a list of records")
    }
  }
  
  def run(activities: List[Map[String, AnyRef]]) = {
    val users = activities.map(_("user")).distinct
    val links = activities.map(_("link")).distinct

    // adjacency
    var A = DoubleFactory2D.sparse.make(users.length, links.length, 0)

    // populate
    var numActions = HashMap[String, Int]()
    for (activity <- activities) {
      val link = activity("link").toString
      numActions(link) = numActions.getOrElse(link, 0) + 1
    }

    var currentNumUsers = new HashMap[String, Int]() { override def default(key: String) = 0 }
    var currentScore = new HashMap[String, Int]()
    var prevTimestampOfDocs = new HashMap[String, Date]() { override def default(key: String) = null }

    for (activity <- activities) {
      val timestamp = activity("timestamp").asInstanceOf[Date]
      val link = activity("link").toString
      val user = activity("user").toString
      if (timestamp equals prevTimestampOfDocs(link))
        A.set(users.indexOf(user), links.indexOf(link), currentScore(link))
      else {
        currentScore(link) = numActions(link) - currentNumUsers(link)
        prevTimestampOfDocs(link) = timestamp
        A.set(users.indexOf(user), links.indexOf(link), currentScore(link))
        currentNumUsers(link) += 1
      }
    }

    // credit scores
    A.forEachNonZero(SqrtCreditScore)

    // expertise for users
    var E = DoubleFactory1D.dense.make(users.length, 1)

    // quality for documents
    var Q = DoubleFactory1D.dense.make(links.length, 1)

    val iterations = 250

    for (i <- 1 to iterations) {
      E = DenseDoubleAlgebra.DEFAULT.mult(A, Q)
      Q = DenseDoubleAlgebra.DEFAULT.mult(DenseDoubleAlgebra.DEFAULT.transpose(A), E)
      E.normalize
      Q.normalize
    }

    val expertise = for (i <- E.toArray.indices) yield (E.get(i), i)
    val quality = for (i <- Q.toArray.indices) yield (Q.get(i), i)

    val usersRanked = expertise.sortBy(_._1).map(i => users(i._2)).reverse.toList
    val linksRanked = quality.sortBy(_._1).map(i => links(i._2)).reverse.toList
    
    OrcTuple(List(usersRanked, linksRanked))
  }

}
