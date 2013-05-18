/** 
  * Orc's ReadJSON is too rudimentary and fails to parse oftenly.
  * This is a more robust JSON parser
  * 
  * @author clyfe
  */

package clyfe.spear

import orc.values.sites.{ TotalSite1, UntypedSite }
import orc.values.OrcRecord
import orc.error.runtime.SiteException
import scala.util.matching.Regex
import scala.util.parsing.json._

object ParseJSON extends Parser with TotalSite1 with UntypedSite {

  def eval(a: AnyRef): AnyRef = {
    a match {
      case text: String => {
		JSON.parseRaw(text) match {
		  case Some(e) => resolveType(e)
		  case None => throw new SiteException("JSON parsing failed.")
		}
      }
      case v => throw new SiteException("Value " + v + " has no JSON counterpart.")
    }
  }
  
  def resolveType(input: Any): AnyRef = input match {
    case JSONObject(data) => {
      val hash = data.transform { case (k,v) => resolveType(v) }
      OrcRecord(hash.asInstanceOf[Map[String, AnyRef]])
    }
    case JSONArray(data) => data.map(resolveType)
    case x => x.asInstanceOf[AnyRef]
  }
  
}
