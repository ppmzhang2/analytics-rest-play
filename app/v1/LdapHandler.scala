package v1

import java.util.Properties

import javax.naming.Context
import javax.naming.directory.{InitialDirContext, SearchControls}

import scala.util.{Failure, Success, Try}

class LdapHandler {
  private val ctxFactory = "com.sun.jndi.ldap.LdapCtxFactory"
  private val url = v1.LdapUrl
  private val auth = "simple"
  private val protocol = "ssl"

  def validate(username: String, password: String): Boolean = {
    getContext(username, password) match {
      case Success(_) => true
      case Failure(_) => false
    }
  }

  /**
    * Get the LDAP context
    *
    * @param username : user NT ID
    * @param password : PayPal PAZ OUD password
    *
    **/
  def getContext(username: String, password: String): Try[InitialDirContext] = {
    Try {
      val principal = s"uid=$username,ou=People,dc=paypalsupport,dc=com"
      val props = new Properties
      props.put(Context.INITIAL_CONTEXT_FACTORY, ctxFactory)
      props.put(Context.PROVIDER_URL, url)
      props.put(Context.SECURITY_AUTHENTICATION, auth)
      props.put(Context.SECURITY_PROTOCOL, protocol)
      props.put(Context.SECURITY_PRINCIPAL, principal)
      props.put(Context.SECURITY_CREDENTIALS, password)

      new InitialDirContext(props)
    }
  }

  def getName(ctx: InitialDirContext, name: String): String = {
    val ctrl = new SearchControls(SearchControls.SUBTREE_SCOPE,
      0, 0, Array("givenName", "sn"), false, false)

    val attrs = ctx.search("",
      s"(&(objectClass=person)(uid=$name))", ctrl)
      .next().getAttributes
    attrs.get("sn").get.toString + ", " + attrs.get("givenName").get.toString
  }

}
