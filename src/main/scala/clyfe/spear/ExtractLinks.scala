/**
  * Extracts links from a text and returns them as list.
  * 
  * @author clyfe
  */

package clyfe.spear

import orc.values.sites.{ TotalSite1, UntypedSite }
import orc.values.OrcRecord
import orc.error.runtime.SiteException
import scala.util.matching.Regex

object ExtractLinks extends TotalSite1 with UntypedSite {

  def eval(a: AnyRef): AnyRef = {
    a match {
      case text: String => {
		val pattern = new Regex("""https?://[\w!#$%&\'()*+,./:;=?@\[\]-]+(?<![!,.?;:"\'()-])""", "link")
		pattern.findAllIn(text).matchData.toList
      }
      case v => throw new SiteException("Value " + v + " has no JSON counterpart.")
    }
  }

}
