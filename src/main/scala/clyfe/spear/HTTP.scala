/** 
  * orc.lib.web.HTTP changed so it fails with halt & signal instead exception.
  * This way we can retry using otherwise combinator.
  * 
  * @author clyfe
  */

package clyfe.spear

import orc.values.sites.{ TotalSite, Site0, Site1 }
import orc.values.OrcRecord
import orc.Handle
import orc.error.runtime.SiteException
import orc.values.Signal

import java.net.{ URLConnection, URL, URLEncoder }
import java.io.{ OutputStreamWriter, InputStreamReader }
import java.lang.StringBuilder
import java.net.URL

import scala.io.Source

/**
  *
  * @author dkitchin, Blake, clyfe
  */
object HTTP extends TotalSite {

  def evaluate(args: List[AnyRef]): AnyRef = {
    args match {
      case List(s: String) => {
        createHTTPInstance(new URL(s))
      }
      case List(s: String, OrcRecord(q)) => {
        val query = if (q.isEmpty) { "" } else { "?" + convertToQueryPairs(q) }
        createHTTPInstance(new URL(s + query))
      }
      case List(url: URL) => {
        createHTTPInstance(url)
      }
      case _ => throw new SiteException("Malformed arguments to HTTP. Arguments should be (String), (String, {..}), or (URL).")
    }
  }

  def convertToQueryPairs(entries: Map[String, AnyRef]): String = {
    val nameValuePairs =
      for ((name, v) <- entries; value = v.toString()) yield {
        URLEncoder.encode(name, "UTF-8") + "=" + URLEncoder.encode(value, "UTF-8")
      }
    nameValuePairs reduceLeft { _ + "&" + _ }
  }

  def createHTTPInstance(url: URL) = {
    new OrcRecord(List(
      ("get", HTTPGet(url)),
      ("post", HTTPPost(url)),
      ("url", url.toString())))
  }

  private def charEncodingFromContentType(contentType: java.lang.String): java.lang.String = {
    //TODO: Don't break if quoted parameter values are used (see RFC 2045 section 5.1)
    val contentTypeParams = contentType.split(";").map(_.trim()).toList.tail
    val charsetValues = List("ISO-8859-1") /* default per RFC 2616 section 3.7.1 */ ++ (
      for (param <- contentTypeParams if param.toLowerCase().startsWith("charset="))
        yield param.substring(8)
      )
    charsetValues.last
  }

  lazy val userAgent = "Orc/" + orc.Main.versionProperties.getProperty("orc.version") +
      " Java/" + java.lang.System.getProperty("java.version")

  case class HTTPGet(url: URL) extends Site0 {
    def call(h: Handle) {
      val getAction =
        new Runnable {
          def run() {
            try {
	            val conn = url.openConnection
	            conn.setConnectTimeout(10000)
	            conn.setReadTimeout(5000)
	            conn.setRequestProperty("User-Agent", userAgent)
	            conn.connect()
	
	            val contentType = conn.getContentType()
	            //TODO: Confirm our assumption that the content is a character stream
	            val charEncoding = charEncodingFromContentType(contentType)
	            val in = Source.fromInputStream(conn.getInputStream, charEncoding)
	            val result = in.mkString
	            in.close
	            h.publish(result)
            } catch {
            	case e => {
            	  //println(e
            	  h.halt
            	  h.publish(Signal)
            	}
            }
          }
          
        }
      (new Thread(getAction)).start()
    }
  }

  case class HTTPPost(url: URL) extends Site1 {
    def call(a: AnyRef, h: Handle) {
      val post = a.toString
      val postAction =
        new Runnable {
          def run() {
            try {
	            val conn = url.openConnection
	            conn.setConnectTimeout(10000)
	            conn.setReadTimeout(5000)
	            conn.setDoOutput(true)
	            conn.setRequestProperty("User-Agent", userAgent)
	            //FIXME: Set POSTed data's Content-Type correctly
	            conn.setRequestProperty("Content-Type", "text/plain; charset=UTF-8")
	            conn.connect()
	
	            val out = new OutputStreamWriter(conn.getOutputStream, "UTF-8")
	            out.write(post)
	            out.close
	
	            val contentType = conn.getContentType()
	            //TODO: Confirm our assumption that the content is a character stream
	            val charEncoding = charEncodingFromContentType(contentType)
	            val in = Source.fromInputStream(conn.getInputStream, charEncoding)
	            val result = in.mkString
	            in.close
	            h.publish(result)
            } catch {
            	case e => {
            	  //println(e
            	  h.halt
            	  h.publish(Signal)
            	}
            }
          }
        }
      (new Thread(postAction)).start()
    }
  }

}
