package dev.jvmguard.connector.server

import dev.jvmguard.common.Loggers
import dev.jvmguard.common.helper.GroupHelper
import dev.jvmguard.data.config.LdapConfig
import dev.jvmguard.data.config.LdapUserMapping
import dev.jvmguard.data.user.User
import dev.jvmguard.data.user.UserType
import com.unboundid.ldap.sdk.Filter
import java.util.*
import javax.naming.Context
import javax.naming.NamingException
import javax.naming.directory.Attributes
import javax.naming.directory.SearchControls
import javax.naming.ldap.InitialLdapContext
import javax.naming.ldap.StartTlsRequest
import javax.naming.ldap.StartTlsResponse

// To generate a keystore for Apache DS, execute
// keytool -genkey -keyalg "RSA" -dname "cn=localhost, ou=ApacheDS, o=ASF, c=US" -alias dev -storepass secret -keystore dev.ks -validity 500
// Then start the jvmguard server with
// -Djavax.net.ssl.trustStore=<path to dev.ks>
// -Djavax.net.ssl.trustStorePassword=secret
//
// To debug SSL connections, add
// -Djavax.net.debug=ssl
object LdapHelper {

    private val DEBUG = java.lang.Boolean.getBoolean("jvmguard.debugLdap")

    private const val LDAP_ATTRIBUTE_CN = "cn"
    private const val LDAP_ATTRIBUTE_MAIL = "mail"
    private val SERVER_LOGGER = Loggers.SERVER

    fun validatePasswordLdap(password: String, user: User, ldapConfig: LdapConfig): Boolean {
        val ldapDn = user.ldapDn
        try {
            val useStartTls = ldapConfig.useStartTls
            val context = if (useStartTls) createLdapContext(ldapConfig) else createLdapContext(ldapConfig, ldapDn, password)
            try {
                val tlsResponse = if (useStartTls) createStartTlsResponse(context) else null

                if (useStartTls) {
                    context.addToEnvironment(Context.SECURITY_PRINCIPAL, ldapDn)
                    context.addToEnvironment(Context.SECURITY_CREDENTIALS, password)
                }

                val attributes = context.getAttributes(ldapDn, arrayOf(LDAP_ATTRIBUTE_CN, LDAP_ATTRIBUTE_MAIL))
                getAttribute(attributes, LDAP_ATTRIBUTE_CN)?.let { user.fullName = it }
                getAttribute(attributes, LDAP_ATTRIBUTE_MAIL)?.let { user.email = it }

                tlsResponse?.close()
            } finally {
                context.close()
            }
        } catch (e: Exception) {
            logException(e)
            return false
        }
        return true
    }

    fun findUser(loginName: String, ldapConfig: LdapConfig): User? {
        try {
            val context = if (ldapConfig.isAuthenticate) {
                createLdapContext(ldapConfig, ldapConfig.userName, ldapConfig.password)
            } else {
                createLdapContext(ldapConfig)
            }

            try {
                for (userMapping in ldapConfig.userMappings) {
                    val controls = SearchControls().apply {
                        returningAttributes = arrayOf("dn")
                        searchScope = SearchControls.SUBTREE_SCOPE
                    }

                    val resolvedSearchExpression = userMapping.userFilter.replace(LdapUserMapping.TOKEN_USER, Filter.encodeValue(loginName))
                    val answers = context.search(userMapping.searchBase, resolvedSearchExpression, controls)
                    if (answers.hasMore()) {
                        val result = answers.nextElement()
                        if (!answers.hasMore()) {
                            val user = User().apply {
                                this.loginName = loginName
                                userType = UserType.LDAP
                                ldapDn = result.nameInNamespace
                                accessLevel = userMapping.accessLevel
                                groupNames = arrayListOf(GroupHelper.ROOT_GROUP_ID)
                            }
                            if (DEBUG) {
                                SERVER_LOGGER.info("{} matched {}", userMapping, loginName)
                            }
                            return user
                        } else if (DEBUG) {
                            SERVER_LOGGER.info("{} has more than one match", userMapping)
                        }
                    } else {
                        SERVER_LOGGER.info("{} has no matches", userMapping)
                    }
                }
            } finally {
                context.close()
            }
        } catch (e: NamingException) {
            logException(e)
        }
        return null
    }

    private fun logException(e: Exception) {
        if (DEBUG) {
            SERVER_LOGGER.error("LDAP authentication failure", e)
        } else {
            SERVER_LOGGER.error("LDAP authentication failure: {}", e.message)
        }
    }

    private fun createLdapContext(ldapConfig: LdapConfig): InitialLdapContext = createLdapContext(ldapConfig, null, null)

    private fun createLdapContext(ldapConfig: LdapConfig, user: String?, password: String?): InitialLdapContext {
        val env = Properties()
        env[Context.INITIAL_CONTEXT_FACTORY] = "com.sun.jndi.ldap.LdapCtxFactory"
        env[Context.PROVIDER_URL] = ldapConfig.url
        env[Context.SECURITY_AUTHENTICATION] = "simple"
        if (user != null) {
            env[Context.SECURITY_PRINCIPAL] = user
            env[Context.SECURITY_CREDENTIALS] = password
        }
        return InitialLdapContext(env, null)
    }

    private fun createStartTlsResponse(context: InitialLdapContext): StartTlsResponse {
        val tlsResponse = context.extendedOperation(StartTlsRequest()) as StartTlsResponse
        tlsResponse.negotiate(null)
        return tlsResponse
    }

    private fun getAttribute(attributes: Attributes, attributeName: String): String? =
        attributes.get(attributeName)?.get()?.toString()
}
